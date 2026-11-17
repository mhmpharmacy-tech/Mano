"use strict";
/** Build only a static internal-review artifact. Never a clinical release. */
const fs=require("node:fs");
const path=require("node:path");
const crypto=require("node:crypto");
const root=path.resolve(__dirname,"..");
const {buildReviewQueue}=require("./review-queue.cjs");
const ASSETS=["img_nutrition_banner.jpg","img_symptom_care_banner.jpg","mano_icon_fg.png","mano_pose_listen.png","mano_pose_welcome.png","mascot_medicine.png","mascot_skin.png","vazirmatn.ttf"];
const BASE=["index.html","staff-demo.html","styles.css","articles.css","modules.css","staff-demo.css","logic.js","app.js","modules.js","staff-demo.js","clinical-content-status.json","clinical-review.html","review.css","review.js"];
function buildPreview(dest, source=root){
 const m=JSON.parse(fs.readFileSync(path.join(source,"clinical-content-status.json"),"utf8"));
 if(m.status!=="NOT_APPROVED_FOR_PUBLIC_CLINICAL_RELEASE"||m.reviewedAt!==null)throw Error("Internal preview state required");
 const articles=m.draftArticles||[],modules=m.draftModules||[];
 if(articles.length!==8||modules.length!==6)throw Error("Unexpected registry size");
 const pages=[...articles,...modules].map(x=>{
  if(!/^[a-z0-9-]+\.html$/.test(x.path)||x.reviewStatus!=="NEEDS_CLINICAL_REVIEW"||x.humanReviewer!==null||x.clinicalUseEnabled===true)throw Error("Invalid draft: "+x.path);
  return x.path;
 });
 const names=[...BASE,...pages,...ASSETS.map(x=>"assets/"+x)];
 if(new Set(names).size!==names.length)throw Error("Duplicate artifact paths");
 const allowed=new Set([...names,"review-data.js"]);
 for(const name of names){
  if(!fs.lstatSync(path.join(source,name)).isFile())throw Error("Invalid file "+name);
  if(!name.endsWith(".html"))continue;
  const html=fs.readFileSync(path.join(source,name),"utf8");
  if(!html.includes('name="robots" content="noindex,nofollow"'))throw Error("Missing noindex: "+name);
  for(const x of html.matchAll(/(?:href|src)="\.\/([^"#?]+)(?:#[^"]*)?"/g)){
   if(!allowed.has(x[1]))throw Error("Unpackaged local link: "+name+" -> "+x[1]);
  }
 }
 if(fs.existsSync(dest))throw Error("Output must be a new directory");
 fs.mkdirSync(path.join(dest,"assets"),{recursive:true});
 const checksums={};
 for(const name of names){
  const bytes=fs.readFileSync(path.join(source,name));
  fs.writeFileSync(path.join(dest,name),bytes);
  checksums[name]=crypto.createHash("sha256").update(bytes).digest("hex");
 }
 const queue=buildReviewQueue(source);
 const reviewData="window.MANO_REVIEW = "+JSON.stringify(queue)+";\n";
 fs.writeFileSync(path.join(dest,"review-data.js"),reviewData);
 checksums["review-data.js"]=crypto.createHash("sha256").update(reviewData).digest("hex");
 fs.writeFileSync(path.join(dest,"PREVIEW_MANIFEST.json"),JSON.stringify({status:"INTERNAL_REVIEW_ONLY",clinicalServiceEnabled:false,articleCount:articles.length,moduleCount:modules.length,reviewItems:queue.items.length,reviewSnapshot:queue.snapshot,sha256:checksums},null,2)+"\n");
 fs.writeFileSync(path.join(dest,"START_HERE_FA.txt"),"مانو کیدز — نسخه آزمایشی داخلی، نه محصول بالینی\n\n۱. تمام فایل‌ها را از ZIP خارج کنید.\n۲. index.html را با مرورگر باز کنید.\n۳. شش ماژول و هشت مقاله قابل مرور هستند.\n۴. staff-demo.html فقط پنل آموزشی داروساز با داده ساختگی است.\n۵. clinical-review.html میز کار داخلی بازبینی است.\n\nفرم‌های آموزشی هیچ پرونده‌ای ارسال نمی‌کنند. اطلاعات واقعی کودک وارد نکنید. تأیید بالینی، محاسبه دوز، تفسیر رشد، تشخیص تصویر و مشاوره واقعی فعال نیستند. پیوند منابع برای مشاهده به اینترنت نیاز دارند. روی دامنه عمومی منتشر نکنید. فایل خصوصی واقعی یا کلید سرویس در این بسته قرار ندارد.\n");
 return {files:names.length+1,articleCount:articles.length,moduleCount:modules.length};
}
module.exports={buildPreview};
if(require.main===module){if(!process.argv[2])throw Error("Specify a new output directory");console.log(JSON.stringify(buildPreview(path.resolve(process.argv[2]))))}
