-- Apply once, only to a dedicated EMPTY synthetic staging database.
-- Operator must create the non-owner LOGIN role mano_stage_app beforehand.
BEGIN;
CREATE SCHEMA mano_stage;
CREATE TABLE mano_stage.accounts (
  id uuid PRIMARY KEY, role text NOT NULL CHECK (role IN ('parent','pharmacist')),
  active boolean NOT NULL DEFAULT true, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE mano_stage.sessions (
  token_hash text PRIMARY KEY CHECK (token_hash ~ '^[a-f0-9]{64}$'),
  account_id uuid NOT NULL REFERENCES mano_stage.accounts(id),
  expires_at timestamptz NOT NULL, revoked_at timestamptz
);
CREATE INDEX ON mano_stage.sessions(account_id);
CREATE TABLE mano_stage.pharmacies (
  id uuid PRIMARY KEY, active boolean NOT NULL DEFAULT true
);
CREATE TABLE mano_stage.memberships (
  account_id uuid REFERENCES mano_stage.accounts(id),
  pharmacy_id uuid REFERENCES mano_stage.pharmacies(id),
  active boolean NOT NULL DEFAULT false, PRIMARY KEY(account_id,pharmacy_id)
);
CREATE TABLE mano_stage.profiles (
  id uuid PRIMARY KEY, owner_id uuid NOT NULL REFERENCES mano_stage.accounts(id),
  alias text NOT NULL CHECK (alias ~ '^demo-[a-z0-9-]{1,40}$'),
  age_months integer NOT NULL CHECK (age_months BETWEEN 0 AND 215),
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON mano_stage.profiles(owner_id);
CREATE TABLE mano_stage.shares (
  profile_id uuid REFERENCES mano_stage.profiles(id),
  pharmacy_id uuid REFERENCES mano_stage.pharmacies(id),
  consent_version text NOT NULL CHECK (consent_version='synthetic-v1'),
  granted_at timestamptz NOT NULL DEFAULT now(), revoked_at timestamptz,
  PRIMARY KEY(profile_id,pharmacy_id)
);
REVOKE ALL ON SCHEMA mano_stage FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA mano_stage FROM PUBLIC;
GRANT USAGE ON SCHEMA mano_stage TO mano_stage_app;
GRANT SELECT ON mano_stage.accounts, mano_stage.sessions, mano_stage.pharmacies,
  mano_stage.memberships, mano_stage.profiles, mano_stage.shares TO mano_stage_app;
GRANT INSERT ON mano_stage.profiles, mano_stage.shares TO mano_stage_app;
GRANT UPDATE (revoked_at,granted_at) ON mano_stage.shares TO mano_stage_app;
-- Resolve the request identity from the session digest, not a caller-supplied role.
-- SECURITY DEFINER helpers are owned by the schema migration owner, not the runtime role.
CREATE FUNCTION mano_stage.actor() RETURNS TABLE(id uuid, role text)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, mano_stage AS $mano$
  SELECT a.id,a.role FROM mano_stage.sessions s JOIN mano_stage.accounts a ON a.id=s.account_id
  WHERE s.token_hash=current_setting('mano_stage.session_digest',true)
    AND s.revoked_at IS NULL AND s.expires_at>statement_timestamp() AND a.active
$mano$;
CREATE FUNCTION mano_stage.can_read_profile(pid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, mano_stage AS $mano$
 SELECT EXISTS(SELECT 1 FROM mano_stage.actor() a JOIN mano_stage.profiles p ON p.id=pid
 WHERE (a.role='parent' AND p.owner_id=a.id) OR
 (a.role='pharmacist' AND EXISTS(SELECT 1 FROM mano_stage.shares s
 JOIN mano_stage.memberships m ON m.pharmacy_id=s.pharmacy_id
 JOIN mano_stage.pharmacies f ON f.id=s.pharmacy_id
 WHERE s.profile_id=p.id AND s.revoked_at IS NULL AND m.account_id=a.id AND m.active AND f.active)))
$mano$;
CREATE FUNCTION mano_stage.owns_profile(pid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, mano_stage AS $mano$
 SELECT EXISTS(SELECT 1 FROM mano_stage.profiles p JOIN mano_stage.actor() a ON a.id=p.owner_id
 WHERE p.id=pid AND a.role='parent')
$mano$;
REVOKE ALL ON FUNCTION mano_stage.actor(), mano_stage.can_read_profile(uuid), mano_stage.owns_profile(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION mano_stage.actor(), mano_stage.can_read_profile(uuid), mano_stage.owns_profile(uuid) TO mano_stage_app;
REVOKE SELECT ON mano_stage.sessions, mano_stage.accounts, mano_stage.memberships FROM mano_stage_app;
ALTER TABLE mano_stage.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE mano_stage.shares ENABLE ROW LEVEL SECURITY;
CREATE POLICY profile_read ON mano_stage.profiles FOR SELECT TO mano_stage_app
 USING (mano_stage.can_read_profile(id));
CREATE POLICY profile_insert ON mano_stage.profiles FOR INSERT TO mano_stage_app
 WITH CHECK (EXISTS(SELECT 1 FROM mano_stage.actor() a WHERE a.id=owner_id AND a.role='parent'));
CREATE POLICY share_read ON mano_stage.shares FOR SELECT TO mano_stage_app
 USING (mano_stage.owns_profile(profile_id));
CREATE POLICY share_insert ON mano_stage.shares FOR INSERT TO mano_stage_app
 WITH CHECK (mano_stage.owns_profile(profile_id) AND EXISTS(SELECT 1 FROM mano_stage.pharmacies f WHERE f.id=pharmacy_id AND f.active));
CREATE POLICY share_update ON mano_stage.shares FOR UPDATE TO mano_stage_app
 USING (mano_stage.owns_profile(profile_id)) WITH CHECK (mano_stage.owns_profile(profile_id));
-- Schema owner intentionally bypasses RLS for controlled admin/provisioning only.
-- Runtime role must never be owner, member of owner role or BYPASSRLS.
-- Synthetic-only authentication: pre-enrolled identifiers, no open signup or SMS delivery.
CREATE TABLE mano_stage.demo_identity (
  handle text PRIMARY KEY CHECK(handle ~ '^demo-[a-z0-9]{1,40}
),
  account_id uuid UNIQUE NOT NULL REFERENCES mano_stage.accounts(id)
);
CREATE TABLE mano_stage.demo_challenge (
  handle text PRIMARY KEY REFERENCES mano_stage.demo_identity(handle),
  challenge_id uuid UNIQUE NOT NULL, proof_hash text NOT NULL CHECK(proof_hash ~ '^[a-f0-9]{64}
),
  issued_at timestamptz NOT NULL DEFAULT now(), expires_at timestamptz NOT NULL,
  attempts integer NOT NULL DEFAULT 0 CHECK(attempts BETWEEN 0 AND 5),
  consumed boolean NOT NULL DEFAULT false
);
CREATE FUNCTION mano_stage.issue_demo_challenge(h text,c uuid,proof text) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $mano$
BEGIN
 IF NOT EXISTS (SELECT 1 FROM mano_stage.demo_identity i JOIN mano_stage.accounts a ON a.id=i.account_id WHERE i.handle=h AND a.active) THEN RETURN false; END IF;
 INSERT INTO mano_stage.demo_challenge(handle,challenge_id,proof_hash,expires_at)
 VALUES(h,c,proof,now()+interval '5 minutes')
 ON CONFLICT(handle) DO UPDATE SET challenge_id=c,proof_hash=proof,issued_at=now(),
 expires_at=now()+interval '5 minutes',attempts=0,consumed=false
 WHERE mano_stage.demo_challenge.issued_at < now()-interval '60 seconds';
 RETURN FOUND;
END $mano$;
CREATE FUNCTION mano_stage.verify_demo_challenge(c uuid,proof text,new_session text) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $mano$
DECLARE d mano_stage.demo_challenge%ROWTYPE; aid uuid;
BEGIN
 SELECT * INTO d FROM mano_stage.demo_challenge WHERE challenge_id=c FOR UPDATE;
 IF NOT FOUND OR d.consumed OR d.attempts>=5 OR d.expires_at<=clock_timestamp() THEN RETURN false; END IF;
 UPDATE mano_stage.demo_challenge SET attempts=attempts+1 WHERE challenge_id=c;
 IF d.proof_hash<>proof THEN RETURN false; END IF;
 SELECT a.id INTO aid FROM mano_stage.demo_identity i JOIN mano_stage.accounts a ON a.id=i.account_id WHERE i.handle=d.handle AND a.active;
 IF aid IS NULL THEN RETURN false; END IF;
 UPDATE mano_stage.demo_challenge SET consumed=true WHERE challenge_id=c;
 INSERT INTO mano_stage.sessions(token_hash,account_id,expires_at) VALUES(new_session,aid,now()+interval '30 minutes');
 RETURN true;
END $mano$;
CREATE FUNCTION mano_stage.revoke_session() RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $mano$
 UPDATE mano_stage.sessions SET revoked_at=now()
 WHERE token_hash=current_setting('mano_stage.session_digest',true) AND revoked_at IS NULL
$mano$;
REVOKE ALL ON FUNCTION mano_stage.issue_demo_challenge(text,uuid,text),mano_stage.verify_demo_challenge(uuid,text,text),mano_stage.revoke_session() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION mano_stage.issue_demo_challenge(text,uuid,text),mano_stage.verify_demo_challenge(uuid,text,text),mano_stage.revoke_session() TO mano_stage_app;
REVOKE ALL ON mano_stage.demo_identity,mano_stage.demo_challenge FROM PUBLIC;
CREATE TABLE mano_stage.audit_events (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 actor_id uuid NOT NULL REFERENCES mano_stage.accounts(id),
 action text NOT NULL CHECK(action IN ('profile.list','profile.create','share.grant','share.revoke','session.logout')),
 occurred_at timestamptz NOT NULL DEFAULT now()
);
CREATE FUNCTION mano_stage.record_audit(event_action text) RETURNS void
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $mano$
 INSERT INTO mano_stage.audit_events(actor_id,action) SELECT id,event_action FROM mano_stage.actor()
$mano$;
REVOKE ALL ON mano_stage.audit_events FROM PUBLIC;
REVOKE ALL ON FUNCTION mano_stage.record_audit(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION mano_stage.record_audit(text) TO mano_stage_app;
COMMIT;
