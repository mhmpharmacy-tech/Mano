import pg from 'pg';
import {databaseOptions} from './policy.mjs';
// Read-only, no data values or connection details in output.
let pool;
try {
  pool=new pg.Pool(databaseOptions(process.env));
  const {rows:[r]}=await pool.query(`SELECT current_database()='mano_stage' AS database_ok,
    current_user='mano_stage_app' AS role_ok,
    NOT (r.rolsuper OR r.rolbypassrls OR r.rolcreaterole OR r.rolcreatedb) AS privileges_ok,
    EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='mano_stage' AND c.relname='profiles' AND c.relrowsecurity AND c.relowner<>r.oid) AS profiles_rls,
    EXISTS(SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='mano_stage' AND c.relname='shares' AND c.relrowsecurity AND c.relowner<>r.oid) AS shares_rls,
    NOT has_table_privilege(current_user,'mano_stage.sessions','SELECT') AS session_private,
    NOT has_table_privilege(current_user,'mano_stage.audit_events','SELECT') AS audit_private
    FROM pg_roles r WHERE r.rolname=current_user`);
  const ok=!!r&&Object.values(r).every(v=>v===true);
  console.log(JSON.stringify({ok,checks:r??{},syntheticOnly:true}));
  if(!ok)process.exitCode=1;
} catch {console.error('STAGE_PREFLIGHT_FAILED');process.exitCode=1;}
finally {if(pool)await pool.end();}
