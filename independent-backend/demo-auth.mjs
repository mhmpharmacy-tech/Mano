import {createHmac,randomInt,randomBytes,randomUUID} from 'node:crypto';
import {digest,tokenPattern,uuidPattern} from './policy.mjs';
// Only an injected, operator-controlled synthetic delivery sink; never logs or returns an OTP.
export function demoAuth(pool,env,deliver) {
  const key=env.MANO_STAGE_OTP_KEY;
  if(env.MANO_STAGE_DEMO_AUTH_ENABLED!=='true'||!tokenPattern.test(key||'')||key===env.MANO_STAGE_ACCESS_TOKEN||typeof deliver!=='function') return null;
  const proof=(id,code)=>createHmac('sha256',Buffer.from(key,'base64url')).update(id+':'+code).digest('hex');
  return {
    async issue(handle){
      if(typeof handle!=='string'||!/^demo-[a-z0-9-]{1,40}$/.test(handle)) throw new Error('INVALID_HANDLE');
      const id=randomUUID(),code=String(randomInt(0,1000000)).padStart(6,'0');
      const {rows}=await pool.query('SELECT mano_stage.issue_demo_challenge($1,$2,$3) AS ok',[handle,id,proof(id,code)]);
      if(rows[0].ok) await deliver({handle,id,code});
      return {challengeId:id}; // same response for unknown or throttled handle
    },
    async verify(id,code){
      if(typeof id!=='string'||!uuidPattern.test(id)||typeof code!=='string'||!/^\d{6}$/.test(code)) return null;
      const secret=randomBytes(32).toString('base64url');
      const {rows}=await pool.query('SELECT mano_stage.verify_demo_challenge($1,$2,$3) AS ok',[id,proof(id,code),digest(secret)]);
      return rows[0].ok?{token:secret,expiresIn:1800}:null;
    }
  };
}
