-- Synthetic staging only; apply AFTER schema.sql as schema owner, once.
BEGIN;
CREATE TABLE mano_stage.care_cards (
 id uuid PRIMARY KEY, profile_id uuid NOT NULL REFERENCES mano_stage.profiles(id),
 pharmacy_id uuid NOT NULL REFERENCES mano_stage.pharmacies(id),
 author_id uuid NOT NULL REFERENCES mano_stage.accounts(id),
 template text NOT NULL CHECK(template='demo-care-v1'),
 state text NOT NULL DEFAULT 'issued' CHECK(state IN ('issued','acknowledged','followup_requested','closed')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ON mano_stage.care_cards(profile_id,pharmacy_id);
CREATE TABLE mano_stage.care_events (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 card_id uuid NOT NULL REFERENCES mano_stage.care_cards(id),
 actor_id uuid NOT NULL REFERENCES mano_stage.accounts(id),
 state text NOT NULL CHECK(state IN ('issued','acknowledged','followup_requested','closed')),
 occurred_at timestamptz NOT NULL DEFAULT now()
);
ALTER TABLE mano_stage.care_cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE mano_stage.care_events ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON mano_stage.care_cards,mano_stage.care_events FROM PUBLIC,mano_stage_app;
-- Pharmacy-scoped access, not merely access to the same child through a different pharmacy.
CREATE FUNCTION mano_stage.care_staff(pid uuid,fid uuid,aid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $care$
 SELECT EXISTS(SELECT 1 FROM mano_stage.shares s JOIN mano_stage.memberships m ON m.pharmacy_id=s.pharmacy_id
 JOIN mano_stage.pharmacies f ON f.id=s.pharmacy_id
 WHERE s.profile_id=pid AND s.pharmacy_id=fid AND s.revoked_at IS NULL AND m.account_id=aid AND m.active AND f.active)
$care$;
CREATE FUNCTION mano_stage.list_care(pid uuid) RETURNS TABLE(id uuid,pharmacy_id uuid,template text,state text)
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $care$
 SELECT c.id,c.pharmacy_id,c.template,c.state FROM mano_stage.care_cards c CROSS JOIN mano_stage.actor() a
 WHERE c.profile_id=pid AND ((a.role='parent' AND mano_stage.owns_profile(pid))
 OR (a.role='pharmacist' AND mano_stage.care_staff(pid,c.pharmacy_id,a.id)))
 ORDER BY c.created_at DESC,c.id LIMIT 100
$care$;
CREATE FUNCTION mano_stage.issue_care(cid uuid,pid uuid,fid uuid) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $care$
DECLARE a record;
BEGIN
 SELECT * INTO a FROM mano_stage.actor();
 IF NOT FOUND OR a.role<>'pharmacist' THEN RETURN false; END IF;
 -- Lock the consent and membership rows so concurrent revocation serializes with issuance.
 PERFORM 1 FROM mano_stage.shares s JOIN mano_stage.memberships m ON m.pharmacy_id=s.pharmacy_id
 JOIN mano_stage.pharmacies f ON f.id=s.pharmacy_id
 WHERE s.profile_id=pid AND s.pharmacy_id=fid AND s.revoked_at IS NULL AND m.account_id=a.id AND m.active AND f.active
 FOR SHARE OF s,m,f;
 IF NOT FOUND THEN RETURN false; END IF;
 INSERT INTO mano_stage.care_cards(id,profile_id,pharmacy_id,author_id,template) VALUES(cid,pid,fid,a.id,'demo-care-v1');
 INSERT INTO mano_stage.care_events(card_id,actor_id,state) VALUES(cid,a.id,'issued');
 RETURN true;
END; $care$;
CREATE FUNCTION mano_stage.transition_care(cid uuid,pid uuid,next_state text) RETURNS boolean
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog,mano_stage AS $care$
DECLARE a record; c mano_stage.care_cards%ROWTYPE;
BEGIN
 SELECT * INTO a FROM mano_stage.actor();
 IF NOT FOUND OR next_state IS NULL THEN RETURN false; END IF;
 SELECT * INTO c FROM mano_stage.care_cards WHERE id=cid AND profile_id=pid FOR UPDATE;
 IF NOT FOUND THEN RETURN false; END IF;
 IF a.role='parent' THEN
  IF NOT mano_stage.owns_profile(pid) OR NOT ((c.state='issued' AND next_state='acknowledged')
    OR (c.state='acknowledged' AND next_state='followup_requested')) THEN RETURN false; END IF;
 ELSIF a.role='pharmacist' THEN
  IF c.state<>'followup_requested' OR next_state<>'closed' THEN RETURN false; END IF;
  PERFORM 1 FROM mano_stage.shares s JOIN mano_stage.memberships m ON m.pharmacy_id=s.pharmacy_id
  JOIN mano_stage.pharmacies f ON f.id=s.pharmacy_id
  WHERE s.profile_id=pid AND s.pharmacy_id=c.pharmacy_id AND s.revoked_at IS NULL AND m.account_id=a.id AND m.active AND f.active
  FOR SHARE OF s,m,f;
  IF NOT FOUND THEN RETURN false; END IF;
 ELSE RETURN false;
 END IF;
 UPDATE mano_stage.care_cards SET state=next_state,updated_at=now() WHERE id=cid;
 INSERT INTO mano_stage.care_events(card_id,actor_id,state) VALUES(cid,a.id,next_state);
 RETURN true;
END; $care$;
REVOKE ALL ON FUNCTION mano_stage.care_staff(uuid,uuid,uuid),mano_stage.list_care(uuid),mano_stage.issue_care(uuid,uuid,uuid),mano_stage.transition_care(uuid,uuid,text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION mano_stage.list_care(uuid),mano_stage.issue_care(uuid,uuid,uuid),mano_stage.transition_care(uuid,uuid,text) TO mano_stage_app;
COMMIT;
