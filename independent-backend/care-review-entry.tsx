// CI-only entry; excluded from the normal Vite application and Docker runtime.
import {createRoot} from 'react-dom/client';
import {IndependentStageClient} from '../src/online/independentStageClient.ts';
import {privateBrowserTransport} from '../src/online/independentBrowserTransport.ts';
import {IndependentCarePreview} from '../src/screens/IndependentCarePreview.tsx';
const element=document.getElementById('care-review-root');
if(!element)throw new Error('Missing private review root');
const root=createRoot(element);
let client:IndependentStageClient|undefined;
let epoch=0;
export function mountReview(role:'parent'|'pharmacist',session:string,pharmacyId:string){
 client?.clearSession();
 client=new IndependentStageClient(true,privateBrowserTransport(true,true));
 client.setSession(session);
 root.render(<IndependentCarePreview enabled client={client} role={role} pharmacyId={pharmacyId} sessionEpoch={++epoch}/>);
}
export function unmountReview(){
 client?.clearSession();client=undefined;root.unmount();
}
