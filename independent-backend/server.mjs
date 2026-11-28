import http from 'node:http';
import { randomUUID } from 'node:crypto';
import { pathToFileURL } from 'node:url';
import pg from 'pg';
import { bearer, databaseOptions, digest, sameToken, tokenPattern, uuidPattern, validProfile, validShare } from './policy.mjs';

function send(res,status,body) {
  res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store',
    'X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'});
  res.end(JSON.stringify(body));
}
async function readBody(req) {
  if (!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type'] || '')) throw new Error('body');
  const parts=[]; let size=0;
  for await(const part of req) {size+=part.length; if(size>4096) throw new Error('body'); parts.push(part);}
  return JSON.parse(Buffer.concat(parts).toString('utf8'));
}
export function createHandler(pool,env) {
  return async (req,res) => {
    const reply=(status,body)=>send(res,status,body);
    if (env.MANO_INDEPENDENT_STAGE_ENABLED !== 'true' ||
        !sameToken(req.headers['x-mano-stage-token'],env.MANO_STAGE_ACCESS_TOKEN)) {
      return reply(404,{code:'NOT_FOUND'});
    }
    const token=bearer(req.headers.authorization);
    if (!token) return reply(401,{code:'AUTH_REQUIRED'});
    let client;
    try {
      // Parse and bound untrusted bodies before reserving a database connection.
      const path = req.url || '';
      const share = /^\/v1\/profiles\/([^/]+)\/shares$/.exec(path);
      const revoke = /^\/v1\/profiles\/([^/]+)\/shares\/([^/]+)$/.exec(path);
      let body;
      if (req.method==='POST' && (path==='/v1/profiles'||share)) {
        try {body=await readBody(req);} catch {return reply(400,{code:'INVALID_BODY'});}
        if (path==='/v1/profiles' ? !validProfile(body) : !validShare(body)) return reply(400,{code:'INVALID_BODY'});
      }
      client=await pool.connect();
      await client.query('BEGIN');
      // Identity is resolved from a revocable server-side session, never caller claims.
      const actor=(await client.query(`SELECT a.id,a.role FROM mano_stage.sessions s
        JOIN mano_stage.accounts a ON a.id=s.account_id
        WHERE s.token_hash=$1 AND s.revoked_at IS NULL AND s.expires_at>now() AND a.active`,[digest(token)])).rows[0];
      let result;
      if (!actor) result=[401,{code:'AUTH_REQUIRED'}];
      else if(req.method==='GET' && path==='/v1/profiles') {
        const {rows}=await client.query(`SELECT p.id,p.alias,p.age_months FROM mano_stage.profiles p
          WHERE ($2='parent' AND p.owner_id=$1) OR ($2='pharmacist' AND EXISTS (
            SELECT 1 FROM mano_stage.shares s JOIN mano_stage.memberships m ON m.pharmacy_id=s.pharmacy_id
            JOIN mano_stage.pharmacies f ON f.id=s.pharmacy_id
            WHERE s.profile_id=p.id AND s.revoked_at IS NULL AND m.account_id=$1 AND m.active AND f.active))
          ORDER BY p.id LIMIT 100`,[actor.id,actor.role]);
        result=[200,{profiles:rows}];
      } else if (req.method==='POST' && path==='/v1/profiles') {
        if(actor.role!=='parent') result=[403,{code:'FORBIDDEN'}];
        else {
          const id=randomUUID();
          await client.query('INSERT INTO mano_stage.profiles(id,owner_id,alias,age_months) VALUES($1,$2,$3,$4)',[id,actor.id,body.alias,body.ageMonths]);
          result=[201,{id}];
        }
      } else if(req.method==='POST' && share && uuidPattern.test(share[1])) {
        if(actor.role!=='parent') result=[403,{code:'FORBIDDEN'}];
        else {
          const q=await client.query(`INSERT INTO mano_stage.shares(profile_id,pharmacy_id,consent_version)
            SELECT p.id,f.id,'synthetic-v1' FROM mano_stage.profiles p CROSS JOIN mano_stage.pharmacies f
            WHERE p.id=$1 AND p.owner_id=$2 AND f.id=$3 AND f.active
            ON CONFLICT(profile_id,pharmacy_id) DO UPDATE SET revoked_at=NULL,granted_at=now()
            RETURNING profile_id`,[share[1],actor.id,body.pharmacyId]);
          result=q.rowCount?[200,{ok:true}]:[404,{code:'NOT_FOUND'}];
        }
      } else if(req.method==='DELETE' && revoke && uuidPattern.test(revoke[1]) && uuidPattern.test(revoke[2])) {
        if(actor.role!=='parent') result=[403,{code:'FORBIDDEN'}];
        else {
          const q=await client.query(`UPDATE mano_stage.shares s SET revoked_at=now()
            FROM mano_stage.profiles p WHERE s.profile_id=p.id AND p.id=$1 AND p.owner_id=$2 AND s.pharmacy_id=$3
            RETURNING s.profile_id`,[revoke[1],actor.id,revoke[2]]);
          result=q.rowCount?[200,{ok:true}]:[404,{code:'NOT_FOUND'}];
        }
      } else result=[404,{code:'NOT_FOUND'}];
      await client.query('COMMIT');
      return reply(...result);
    } catch {
      if(client) {try {await client.query('ROLLBACK');}catch {client.release(true);client=undefined;}}
      // No identifiers, request bodies, tokens, connection URLs, or raw errors in responses/logs.
      return reply(503,{code:'STAGE_UNAVAILABLE'});
    } finally {if(client) client.release();}
  };
}
export function createStageServer(pool,env=process.env) {
  return http.createServer({requestTimeout:10000,headersTimeout:5000,maxHeaderSize:8192},createHandler(pool,env));
}
if(process.argv[1] && import.meta.url===pathToFileURL(process.argv[1]).href) {
  try {
    if(process.env.MANO_INDEPENDENT_STAGE_ENABLED!=='true'||!tokenPattern.test(process.env.MANO_STAGE_ACCESS_TOKEN||'')) throw new Error('Disabled');
    const pool=new pg.Pool(databaseOptions(process.env));
    pool.on('error',()=>console.error('STAGE_POOL_ERROR'));
    await pool.query('SELECT 1 FROM mano_stage.accounts LIMIT 0');
    const server=createStageServer(pool);
    server.listen(3100,'127.0.0.1',()=>console.log('Synthetic stage listening on loopback:3100'));
    for(const signal of ['SIGINT','SIGTERM']) process.once(signal,()=>{server.close(()=>{void pool.end();});});
  } catch {console.error('STAGE_STARTUP_FAILED');process.exitCode=1;}
}
