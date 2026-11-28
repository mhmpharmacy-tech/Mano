// Internal test harness contract only. No public sign-up, SMS or persistent credentials.
// The operator supplies the transport and authenticated fixture-role binding.
export type DemoAuthTransport = (request: {
  path: '/internal/mano/v1/auth/issue' | '/internal/mano/v1/auth/verify';
  body: Record<string,string>; signal: AbortSignal;
}) => Promise<{status:number;data:unknown}>;
const handlePattern=/^demo-[a-z0-9]{1,40}$/;
const uuidPattern=/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
const record=(v:unknown):v is Record<string,unknown>=>!!v&&typeof v==='object'&&!Array.isArray(v);
export class IndependentDemoAuth {
 private generation=0;
 private controller:AbortController|null=null;
 constructor(private readonly enabled:boolean,private readonly transport:DemoAuthTransport){}
 cancel(){this.generation++;this.controller?.abort();this.controller=null;}
 private async request(path:Parameters<DemoAuthTransport>[0]['path'],body:Record<string,string>,status:number){
   if(!this.enabled)throw new Error('STAGE_DISABLED');
   this.cancel();
   const generation=this.generation;
   const controller=new AbortController();this.controller=controller;
   const timer=setTimeout(()=>controller.abort(),8000);
   try{
     const r=await this.transport({path,body,signal:controller.signal});
     if(generation!==this.generation||controller.signal.aborted||r.status!==status)throw new Error('AUTH_NOT_CONFIRMED');
     return r.data;
   }finally{
     clearTimeout(timer);
     if(this.controller===controller)this.controller=null;
   }
 }
 async issue(handle:string){
   if(!handlePattern.test(handle))throw new Error('SYNTHETIC_ONLY');
   const r=await this.request('/internal/mano/v1/auth/issue',{handle},202);
   if(!record(r)||typeof r.challengeId!=='string'||!uuidPattern.test(r.challengeId)||Object.keys(r).length!==1)throw new Error('INVALID_RESPONSE');
   return r.challengeId;
 }
 async verify(challengeId:string,code:string){
   if(!uuidPattern.test(challengeId)||!/^\d{6}$/.test(code))throw new Error('INVALID_CHALLENGE');
   const r=await this.request('/internal/mano/v1/auth/verify',{challengeId,code},200);
   if(!record(r)||typeof r.token!=='string'||! /^[A-Za-z0-9_-]{43}$/.test(r.token)||r.expiresIn!==1800)throw new Error('INVALID_RESPONSE');
   return r.token;
 }
}
