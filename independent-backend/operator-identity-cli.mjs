import pg from 'pg';
import {enrollDemo,deactivateDemo} from './operator-identity.mjs';
let pool;
try{
 if(process.env.MANO_OPERATOR_SYNTHETIC_ONLY!=='true')throw new Error('Synthetic acknowledgment required');
 const url=new URL(process.env.MANO_OPERATOR_DATABASE_URL||'invalid:');
 if(!['postgres:','postgresql:'].includes(url.protocol)||url.pathname!=='/mano_stage'||!url.password||url.search||url.hash||url.username==='mano_stage_app')throw new Error('Operator database required');
 const mode=process.env.MANO_OPERATOR_DB_TLS||'verify-full';
 if(mode!=='verify-full'&&!(mode==='private-test'&&['mano-db','127.0.0.1','localhost'].includes(url.hostname)))throw new Error('Explicit transport policy required');
 pool=new pg.Pool({connectionString:url.href,ssl:mode==='private-test'?false:{rejectUnauthorized:true,...(process.env.MANO_OPERATOR_DB_CA?{ca:process.env.MANO_OPERATOR_DB_CA}:{})},max:1,connectionTimeoutMillis:3000,statement_timeout:5000});
 const [action,handle,role,pharmacyId,...rest]=process.argv.slice(2);
 if(rest.length)throw new Error('Invalid arguments');
 let result;
 if(action==='enroll')result=await enrollDemo(pool,{syntheticOnly:true,handle,role,...(pharmacyId?{pharmacyId}:{})});
 else if(action==='deactivate'&&role===undefined)result=await deactivateDemo(pool,{syntheticOnly:true,handle});
 else throw new Error('Invalid action');
 console.log(JSON.stringify(result));
}catch{console.error('SYNTHETIC_IDENTITY_OPERATION_FAILED');process.exitCode=1;}
finally{if(pool)await pool.end();}
