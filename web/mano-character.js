/* Presentation only. Never evaluates clinical data. */
(function(root,factory){
 const api=factory();
 if(typeof module==="object" && module.exports) module.exports=api;
 else {
  root.ManoCharacter=api;
  const init=()=>root.document.querySelectorAll('img[data-mano-state]').forEach(img=>api.apply(img,img.getAttribute('data-mano-state')));
  if(root.document.readyState==="loading") root.document.addEventListener("DOMContentLoaded",init,{once:true});
  else init();
 }
})(typeof globalThis!=="undefined"?globalThis:this,function(){
 "use strict";
 const states=Object.freeze({welcome:"mano_pose_welcome.png",listen:"mano_pose_listen.png",medicine:"mascot_medicine.png",skin:"mascot_skin.png",alert:"mano_pose_listen.png"});
 function apply(img,state){
  if(!img || typeof img.setAttribute!=="function" || !Object.hasOwn(states,state)) return false;
  img.setAttribute("src","./assets/"+states[state]);
  img.setAttribute("data-mano-state",state);
  return true;
 }
 return Object.freeze({states,apply});
});
