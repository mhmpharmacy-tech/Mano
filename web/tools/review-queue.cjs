"use strict";
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");
const root = path.resolve(__dirname, "..");
const digest = value => crypto.createHash("sha256").update(value).digest("hex");

function buildReviewQueue(directory = root) {
  const read = name => fs.readFileSync(path.join(directory, name));
  const registry = JSON.parse(read("clinical-content-status.json"));
  const pages = ["index.html", "staff-demo.html", ...registry.draftArticles.map(a => a.path), ...registry.draftModules.map(a => a.path)];
  const dependencies = ["clinical-content-status.json", "logic.js", "app.js", "modules.js", "staff-demo.js", "styles.css", "modules.css", "articles.css", "staff-demo.css"];
  const assets = fs.readdirSync(path.join(directory,"assets")).map(name => "assets/" + name);
  const hashes = Object.fromEntries([...new Set([...pages, ...dependencies, ...assets])].sort().map(name => {
    if (!/^(?:assets\/)?[a-zA-Z0-9_.-]+$/.test(name) || !fs.lstatSync(path.join(directory,name)).isFile()) throw Error("Unexpected review asset");
    return [name, digest(read(name))];
  }));
  const title = file => {
    const match = read(file).toString("utf8").match(/<title>([^<]+)<\/title>/);
    if (!match) throw Error("Missing title: " + file);
    return match[1];
  };
  const sources = (claims) => claims.map(c => {
    const s = registry.sourceRegister[c.source];
    if (!s || !/^https:\/\//.test(s.url)) throw Error("Missing registered source");
    return {id:c.source, publisher:s.publisher, url:s.url, topic:c.topic||"", locator:c.locator||s.locator||"Check full relevant source", coverage:s.coverage||"Access scope not recorded; verify full source", verifiedOn:s.verifiedOn||null};
  });
  const items = [
    {id:"home:whole", group:"home", title:"کل صفحه اصلی و هشدارها", page:"index.html", refs:[], local:"همه مطالب، منطق هشدار و ادعاهای خدمت را مرور کنید."},
    ...registry.contentSections.map(s=>({id:"home:"+s.id, group:"home", title:({screening:"علائم هشدار",medicine:"ایمنی دارو",skin:"پوست",nutrition:"تغذیه و رشد",faq:"پرسش‌های متداول"})[s.id]||s.id, page:"index.html#"+s.id, refs:sources(s.references.map(source=>({source}))), local:"تطبیق متن و وضعیت ارجاع با شرایط ایران."})),
    ...registry.draftArticles.map(a=>({id:"article:"+a.path,group:"article",title:title(a.path),page:a.path,refs:sources(a.claims),local:a.localReviewRequired||"ترجمه، ایمنی و تطبیق با ایران را بررسی کنید."})),
  ];
  for(const m of registry.draftModules){
    items.push({id:"module:"+m.path,group:"module",title:title(m.path),page:m.path,refs:[],local:"علاوه بر تک‌تک مدخل‌ها، کل صفحه، عملکرد و محدودیت‌های ابزار را بازبینی کنید."});
    for(const e of m.entries)items.push({id:"entry:"+m.path+":"+e.id,group:"entry",title:title(m.path)+" / "+e.id,page:m.path,refs:sources(e.claims),local:"سن، هشدار، محدودیت کاربرد و برچسب ایرانی در صورت ارتباط."});
  }
  items.push({id:"staff:demo",group:"staff",title:"پنل آزمایشی داروساز",page:"staff-demo.html",refs:[],local:"ایمنی مسیر تصمیم، محدودیت داده ساختگی و نبود سرویس واقعی."});
  if(new Set(items.map(i=>i.id)).size!==items.length) throw Error("Duplicate item");
  return {schemaVersion:1,snapshot:digest(JSON.stringify(hashes)),hashes,items};
}
module.exports = {buildReviewQueue};
