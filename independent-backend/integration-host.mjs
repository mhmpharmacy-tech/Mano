import http from 'node:http';
import {once} from 'node:events';
import {pathToFileURL} from 'node:url';
import pg from 'pg';
import {createHandler} from './server.mjs';
import {databaseOptions,sameToken,tokenPattern} from './policy.mjs';

// Internal operator transport only. Never expose this listener via public ingress.
export function createIntegrationHost(pool,env){
  if(env.MANO_INTEGRATION_ENABLED!=='true'||env.MANO_INDEPENDENT_STAGE_ENABLED!=='true'||
     !tokenPattern.test(env.MANO_STAGE_ACCESS_TOKEN||'')||
     !tokenPattern.test(env.MANO_INTEGRATION_OPERATOR_TOKEN||'')||
     env.MANO_INTEGRATION_OPERATOR_TOKEN===env.MANO_STAGE_ACCESS_TOKEN)throw new Error('Integration disabled or invalid gates');
  const handler=createHandler(pool,env);
  return http.createServer({requestTimeout:10000,headersTimeout:5000,maxHeaderSize:8192},(req,res)=>{
    const reject=()=>{res.writeHead(404,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end('{"code":"NOT_FOUND"}');};
    const host=req.headers.host||'';
    if(!/^127\.0\.0\.1:\d+$/.test(host)||req.headers.origin||req.headers['sec-fetch-site']||
       !sameToken(req.headers['x-mano-operator-token'],env.MANO_INTEGRATION_OPERATOR_TOKEN)||
       req.headers['x-mano-stage-token']||!['GET','POST','DELETE'].includes(req.method))return reject();
    const p=req.url||'';
    if(!/^\/internal\/mano\/v1\/(?:profiles(?:\/[a-fA-F0-9-]{36}\/shares(?:\/[a-fA-F0-9-]{36})?)?|session\/logout)$/.test(p))return reject();
    req.url=p.slice('/internal/mano'.length);
    req.headers['x-mano-stage-token']=env.MANO_STAGE_ACCESS_TOKEN;
    delete req.headers['x-mano-operator-token'];
    void handler(req,res);
  });
}
export async function startIntegration(env=process.env){
  // Without independently verified encrypted transport, require an explicit synthetic-only acknowledgement.
  if(env.MANO_INTEGRATION_SYNTHETIC_ONLY!=='true')throw new Error('Synthetic mode required');
  const pool=new pg.Pool(databaseOptions(env));
  pool.on('error',()=>console.error('INTEGRATION_POOL_ERROR'));
  let server;
  try{
    server=createIntegrationHost(pool,env);
    await pool.query('SELECT id FROM mano_stage.profiles LIMIT 0');
    await pool.query('SELECT id FROM mano_stage.actor() LIMIT 0');
    const port=Number(env.MANO_INTEGRATION_PORT||'3101');
    if(!Number.isInteger(port)||port<1024||port>65535||port===3000)throw new Error('Invalid internal port');
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
