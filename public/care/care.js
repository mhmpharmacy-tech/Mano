import {initializeApp} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js";
import {getAuth,GoogleAuthProvider,signInWithPopup,onAuthStateChanged,signOut} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js";
import {initializeAppCheck,ReCaptchaEnterpriseProvider} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-app-check.js";
import {getFunctions,httpsCallable} from "https://www.gstatic.com/firebasejs/10.14.1/firebase-functions.js";
const $=id=>document.getElementById(id);
const labels={send:"ارسال کارت",open:"مشاهدهٔ خانواده",dose:"ثبت مصرف",question:"پرسش خانواده",reply:"پاسخ داروساز",checkin:"گزارش پیگیری",close:"پایان پیگیری"};
function node(parent,tag,text){const el=document.createElement(tag);el.textContent=text;parent.append(el);return el}
try {
  const config=await (await fetch("/__/firebase/init.json")).json();
  const app=initializeApp(config);
  const {siteKey}=await (await fetch("/care/app-check.json")).json();
  initializeAppCheck(app,{provider:new ReCaptchaEnterpriseProvider(siteKey),isTokenAutoRefreshEnabled:true});
  const auth=getAuth(app), f=getFunctions(app,"us-central1");
  const call=async(name,data)=>(await httpsCallable(f,name)(data)).data;
  $("login").onclick=()=>signInWithPopup(auth,new GoogleAuthProvider()).catch(()=>{$("status").textContent="ورود انجام نشد."});
  $("logout").onclick=()=>signOut(auth);
  let revision=0;
  async function load(){
    const epoch=++revision,user=auth.currentUser;
    $("cards").replaceChildren(); $("login").hidden=!!user; $("logout").hidden=$("refresh").hidden=!user;
    $("status").textContent=user?"در حال دریافت…":"با حساب ثبت‌شده در مانو وارد شوید.";
    if(!user)return;
    try {
      const {claims}=await user.getIdTokenResult();
      const staff=claims.manoStaffVerified===true && claims.manoStaffRole==="pharmacist";
      const rows=await call(staff?"getPharmacyInbox":"listParentConsultations",staff?{pharmacyId:claims.manoPharmacyId,limit:100}:{limit:50});
      if(epoch!==revision)return;
      $("status").textContent="";
      for(const row of rows.filter(r=>staff||r.careCard)){
        const section=node($("cards"),"article","");
        const card=row.careCard;
        node(section,"h2",card?card.child+" · "+card.medicine:"پروندهٔ "+row.caseId);
        node(section,"p",row.questionSummaryFa);
        function action(kind,inputs={}){
          const button=node(section,"button",kind==="send"?"تأیید و ارسال به خانواده":labels[kind]);
          let requestId=crypto.randomUUID();
          button.onclick=async()=>{
            const values=Object.fromEntries(Object.entries(inputs).map(([k,v])=>[k,v.value.trim()]));
            if(Object.values(values).some(v=>!v)){ $("status").textContent="لطفاً همهٔ فیلدها را تکمیل کنید.";return }
            button.disabled=true;
            try{await call("updateCareCard",{pharmacyId:row.pharmacyId,caseId:row.caseId,action:kind,requestId,confirmed:kind==="send",...values});if(epoch===revision)await load()}
            catch{button.disabled=false;$("status").textContent="ثبت تأیید نشد. دوباره تلاش کنید."}
          };
        }
        function input(title){const label=node(section,"label",title);return node(label,"textarea","")}
        if(!card){action("send",{child:input("نام کودک"),medicine:input("نام فرآورده"),strength:input("غلظت یا قدرت"),instructions:input("دستور تأییدشده"),followUp:input("موعد پیگیری")});continue}
        const details=node(section,"details","");node(details,"summary","مشاهدهٔ کارت");
        node(details,"p",[card.strength,card.instructions,"پیگیری در "+card.followUp].join("\n"));
        details.ontoggle=async()=>{if(details.open&&!staff&&!card.openedAt){try{await call("updateCareCard",{pharmacyId:row.pharmacyId,caseId:row.caseId,requestId:crypto.randomUUID(),action:"open"})}catch{$("status").textContent="ثبت مشاهده تأیید نشد."}}};
        for(const e of card.events)node(details,"p",labels[e.action]+" · "+new Date(e.at).toLocaleString("fa-IR")+" "+e.text);
        if(card.closedAt)continue;
        if(staff){action("reply",{text:input("پاسخ به خانواده")});action("close")}
        else{action("dose");action("question",{text:input("سؤال دربارهٔ دارو")});action("checkin",{text:input("نتیجهٔ پیگیری")})}
      }
      if(!$("cards").children.length)$("status").textContent="هنوز کارتی برای نمایش وجود ندارد.";
    } catch {if(epoch===revision)$("status").textContent="دریافت پرونده‌ها ممکن نشد."}
  }
  $("refresh").onclick=load;onAuthStateChanged(auth,load);
} catch {$("login").disabled=true;$("status").textContent="اتصال امن کارت مراقبت هنوز در این سایت تنظیم نشده است."}
