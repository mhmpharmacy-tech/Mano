import http from 'node:http';
import {once} from 'node:events';
import {randomBytes} from 'node:crypto';
import {sameToken,tokenPattern} from './policy.mjs';

// Operator-only local browser harness, NOT a deployable public BFF.
// The test runner injects a short-lived HttpOnly cookie; there is no cookie issuance route.
export async function startBrowserBridge({upstreamPort,operatorToken,syntheticOnly}){
 if(syntheticOnly!==true||!Number.isInteger(upstreamPort)||upstreamPort<1024||upstreamPort>65535||!tokenPattern.test(operatorToken||''))throw new Error('Invalid bridge configuration');
 const cookie=randomBytes(32).toString('base64url');
 const expiry=Date.now()+15*60*1000;
 let origin='',requests=0,active=0;
 const server=http.createServer({requestTimeout:10000,headersTimeout:5000,maxHeaderSize:8192},async(req,res)=>{
  const send=(status,data)=>{res.writeHead(status,{'content-type':'application/json','cache-control':'no-store','x-content-type-options':'nosniff','referrer-policy':'no-referrer','content-security-policy':"default-src 'none'; frame-ancestors 'none'"});res.end(JSON.stringify(data));};
  const notFound=()=>send(404,{code:'NOT_FOUND'});
  const path=req.url||'';
  const cookies=(req.headers.cookie||'').split(';').map(v=>v.trim()).filter(v=>v.startsWith('mano_harness='));
  if(req.socket.remoteAddress!=='127.0.0.1'||req.headers.host!==origin.slice(7)||Date.now()>=expiry||
     cookies.length!==1||!sameToken(cookies[0].slice('mano_harness='.length),cookie)||
     req.headers['x-mano-browser-test']!=='1'||req.headers['x-mano-operator-token']||req.headers['x-mano-stage-token']||
     (req.headers.origin&&req.headers.origin!==origin)||
     (req.headers['sec-fetch-site']&&req.headers['sec-fetch-site']!=='same-origin')||
     !['GET','POST','DELETE'].includes(req.method))return notFound();
  if(req.method!=='GET'&&req.headers.origin!==origin)return notFound();
  if(!/^\/internal\/mano\/v1\/(?:profiles(?:\/[a-fA-F0-9-]{36}\/shares(?:\/[a-fA-F0-9-]{36})?)?|session\/logout|auth\/(?:issue|verify))$/.test(path))return notFound();
  if(++requests>200||active>=4)return send(429,{code:'RATE_LIMITED'});
  active++;
  try{
   let body;
   if(req.method==='POST'&&path!=='/internal/mano/v1/session/logout'){
    if(!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type']||''))return send(400,{code:'INVALID_BODY'});
    const chunks=[];let n=0;
    for await(const part of req){n+=part.length;if(n>4096)return send(413,{code:'INVALID_BODY'});chunks.push(part);}
    body=Buffer.concat(chunks);
   }
   const response=await fetch('http://127.0.0.1:'+upstreamPort+path,{
    method:req.method,redirect:'error',signal:AbortSignal.timeout(8000),
    headers:{'x-mano-operator-token':operatorToken,
     ...(req.headers.authorization?{authorization:req.headers.authorization}:{}),
     ...(body?{'content-type':'application/json'}:{})},...(body?{body}:{})});
   // Only JSON from fixed routes; never relay arbitrary response headers.
   return send(response.status,await response.json());
  }catch{return send(503,{code:'STAGE_UNAVAILABLE'});}
  finally{active--;}
 });
 server.listen(0,'127.0.0.1');await once(server,'listening');
 origin='http://127.0.0.1:'+server.address().port;
 return {origin,cookie,close:()=>new Promise(resolve=>server.close(resolve))};
}
