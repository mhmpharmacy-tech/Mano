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
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, mano_stage AS $
  SELECT a.id,a.role FROM mano_stage.sessions s JOIN mano_stage.accounts a ON a.id=s.account_id
  WHERE s.token_hash=current_setting('mano_stage.session_digest',true)
    AND s.revoked_at IS NULL AND s.expires_at>statement_timestamp() AND a.active
$;
CREATE FUNCTION mano_stage.can_read_profile(pid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, mano_stage AS $
 SELECT EXISTS(SELECT 1 FROM mano_stage.actor() a JOIN mano_stage.profiles p ON p.id=pid
 WHERE (a.role='parent' AND p.owner_id=a.id) OR
 (a.role='pharmacist' AND EXISTS(SELECT 1 FROM mano_stage.shares s
 JOIN mano_stage.memberships m ON m.pharmacy_id=s.pharmacy_id
 JOIN mano_stage.pharmacies f ON f.id=s.pharmacy_id
 WHERE s.profile_id=p.id AND s.revoked_at IS NULL AND m.account_id=a.id AND m.active AND f.active)))
$;
CREATE FUNCTION mano_stage.owns_profile(pid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = pg_catalog, mano_stage AS $
 SELECT EXISTS(SELECT 1 FROM mano_stage.profiles p JOIN mano_stage.actor() a ON a.id=p.owner_id
 WHERE p.id=pid AND a.role='parent')
$;
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
COMMIT;
