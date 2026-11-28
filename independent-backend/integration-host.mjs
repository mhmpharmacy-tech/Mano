import http from 'node:http';
import {once} from 'node:events';
import {pathToFileURL} from 'node:url';
import pg from 'pg';
import {createHandler} from './server.mjs';
import {demoAuth} from './demo-auth.mjs';
import {databaseOptions,sameToken,tokenPattern} from './policy.mjs';

// Internal operator transport only. Never expose this listener via public ingress.
export function createIntegrationHost(pool,env,deliver){
  if(env.MANO_INTEGRATION_ENABLED!=='true'||env.MANO_INDEPENDENT_STAGE_ENABLED!=='true'||
     !tokenPattern.test(env.MANO_STAGE_ACCESS_TOKEN||'')||
     !tokenPattern.test(env.MANO_INTEGRATION_OPERATOR_TOKEN||'')||
     env.MANO_INTEGRATION_OPERATOR_TOKEN===env.MANO_STAGE_ACCESS_TOKEN)throw new Error('Integration disabled or invalid gates');
  const handler=createHandler(pool,env);
  const auth=env.MANO_STAGE_OTP_KEY!==env.MANO_INTEGRATION_OPERATOR_TOKEN?demoAuth(pool,env,deliver):null;
  return http.createServer({requestTimeout:10000,headersTimeout:5000,maxHeaderSize:8192},(req,res)=>{
    const reject=()=>{res.writeHead(404,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end('{"code":"NOT_FOUND"}');};
    const host=req.headers.host||'';
    if(env.MANO_INTEGRATION_ENABLED!=='true'||env.MANO_INDEPENDENT_STAGE_ENABLED!=='true'||!/^127\.0\.0\.1:\d+$/.test(host)||req.headers.origin||req.headers['sec-fetch-site']||
       !sameToken(req.headers['x-mano-operator-token'],env.MANO_INTEGRATION_OPERATOR_TOKEN)||
       req.headers['x-mano-stage-token']||!['GET','POST','DELETE'].includes(req.method))return reject();
    const p=req.url||'';
    if(p==='/internal/mano/v1/auth/issue'||p==='/internal/mano/v1/auth/verify'){
      if(!auth||env.MANO_STAGE_DEMO_AUTH_ENABLED!=='true'||req.method!=='POST'||req.headers.authorization)return reject();
      void handleDemoAuth(req,res,auth,p);return;
    }
    if(!/^\/internal\/mano\/v1\/(?:profiles(?:\/[a-fA-F0-9-]{36}\/shares(?:\/[a-fA-F0-9-]{36})?)?|session\/logout)$/.test(p))return reject();
    req.url=p.slice('/internal/mano'.length);
    req.headers['x-mano-stage-token']=env.MANO_STAGE_ACCESS_TOKEN;
    delete req.headers['x-mano-operator-token'];
    void handler(req,res);
  });
}
async function handleDemoAuth(req,res,auth,path){
  const send=(status,body)=>{res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(body));};
  let body;
  try{
    if(!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type']||''))throw new Error();
    const chunks=[];let size=0;
    for await(const chunk of req){size+=chunk.length;if(size>1024)throw new Error();chunks.push(chunk);}
    body=JSON.parse(Buffer.concat(chunks).toString('utf8'));
    if(!body||typeof body!=='object'||Array.isArray(body))throw new Error();
  }catch{return send(400,{code:'INVALID_BODY'});}
  try{
    if(path.endsWith('/issue')){
      if(Object.keys(body).join(',')!=='handle'||typeof body.handle!=='string'||!/^demo-[a-z0-9]{1,40}$/.test(body.handle))return send(400,{code:'INVALID_BODY'});
      return send(202,await auth.issue(body.handle));
    }
    if(Object.keys(body).sort().join(',')!=='challengeId,code'||typeof body.challengeId!=='string'||typeof body.code!=='string')return send(400,{code:'INVALID_BODY'});
    const result=await auth.verify(body.challengeId,body.code);
    return result?send(200,result):send(401,{code:'AUTH_REQUIRED'});
  }catch{return send(503,{code:'STAGE_UNAVAILABLE'});}
}
export async function startIntegration(env=process.env,deliver){
  // Without independently verified encrypted transport, require an explicit synthetic-only acknowledgement.
  if(env.MANO_INTEGRATION_SYNTHETIC_ONLY!=='true')throw new Error('Synthetic mode required');
  const pool=new pg.Pool(databaseOptions(env));
  pool.on('error',()=>console.error('INTEGRATION_POOL_ERROR'));
  let server;
  try{
    server=createIntegrationHost(pool,env,deliver);
    const {rows:[ready]}=await pool.query(`SELECT current_database()='mano_stage' AND current_user='mano_stage_app'
      AND r.rolcanlogin AND NOT(r.rolsuper OR r.rolbypassrls OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication)
      AND NOT EXISTS(SELECT 1 FROM pg_auth_members m WHERE m.member=r.oid)
      AND (SELECT count(*)=2 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname='mano_stage' AND c.relname IN ('profiles','shares') AND c.relrowsecurity AND c.relowner<>r.oid)
      AND NOT has_table_privilege(current_user,'mano_stage.sessions','SELECT')
      AND NOT has_table_privilege(current_user,'mano_stage.audit_events','SELECT')
      AS ok, COALESCE((SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()),false) AS encrypted
      FROM pg_roles r WHERE r.rolname=current_user`);
    if(!ready?.ok||(env.MANO_STAGE_DB_TLS==='verify-full'&&!ready.encrypted))throw new Error('Integration readiness failed');
    await pool.query('SELECT id FROM mano_stage.actor() LIMIT 0');
    const port=Number(env.MANO_INTEGRATION_PORT||'3101');
    if(!Number.isInteger(port)||(port!==0&&port<1024)||port>65535||port===3000)throw new Error('Invalid internal port');
    server.listen(port,'127.0.0.1');await once(server,'listening');
    return {server,close:async()=>{if(server.listening)await new Promise(resolve=>server.close(resolve));await pool.end();}};
  }catch(e){if(server?.listening)server.close();await pool.end();throw e;}
}
if(process.argv[1]&&import.meta.url===pathToFileURL(process.argv[1]).href){
  try{
    const running=await startIntegration();
    console.log('MANO synthetic integration listening on loopback');
    let stopping=false;
    for(const signal of ['SIGTERM','SIGINT'])process.once(signal,async()=>{
      if(stopping)return;stopping=true;
      const limit=setTimeout(()=>process.exit(1),12000);limit.unref();
      try{await running.close();}catch{process.exitCode=1;}finally{clearTimeout(limit);}
    });
  }catch{console.error('INTEGRATION_STARTUP_FAILED');process.exitCode=1;}
}
