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
COMMIT;
