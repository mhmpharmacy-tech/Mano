// Internal preview only. No deployment, patient data, or external requests.
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const http=require('node:http');
const fs=require('node:fs/promises');
const path=require('node:path');
const root=path.resolve(__dirname,'../web');
const pages=['index','mano-drug','mano-growth','mano-skin','mano-nutrition','mano-care','mano-connect'];
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript','.css':'text/css','.png':'image/png','.svg':'image/svg+xml','.ttf':'font/ttf'};
const server=http.createServer(async(req,res)=>{
 try{
  const file=path.resolve(root,'.'+new URL(req.url,'http://localhost').pathname);
  if(!file.startsWith(root+path.sep)){res.writeHead(403).end();return;}
  const bytes=await fs.readFile(file);
  res.writeHead(200,{'Content-Type':types[path.extname(file)]||'application/octet-stream'}).end(bytes);
 }catch{res.writeHead(404).end();}
});
(async()=>{
 let browser;let failures=0;let passed=0;
 try{
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const origin='http://127.0.0.1:'+server.address().port;
  browser=await chromium.launch({headless:true});
  await fs.mkdir('brand-screenshots',{recursive:true});
  for(const width of [320,390,768,1440]){
   const context=await browser.newContext({viewport:{width,height:900},reducedMotion:'reduce'});
   await context.route('**/*',route=>route.request().url().startsWith(origin+'/')?route.continue():route.abort());
   for(const name of pages){
    const page=await context.newPage();const errors=[];
    page.on('pageerror',e=>errors.push(e.message));
    try{
     await page.goto(origin+'/'+name+'.html',{waitUntil:'load'});
     await page.evaluate(()=>document.fonts.ready);
     const img=page.locator('img[data-mano-state]').first();
     await img.evaluate(el=>el.decode());
     await page.screenshot({path:'brand-screenshots/'+name+'-'+width+'.png',fullPage:true,animations:'disabled'});
     assert.equal(await page.locator('html').getAttribute('dir'),'rtl');
     assert.match(await page.locator('meta[name=robots]').getAttribute('content'),/noindex/);
     assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'horizontal document overflow');
     assert.ok(await img.isVisible(),'mascot not visible');
     assert.ok(await page.evaluate(()=>Boolean(window.ManoCharacter)),'character engine unavailable');
     assert.deepEqual(errors,[]);
     console.log('PASS',name,width);passed++;
    }catch(e){console.error('FAIL',name,width,e.message);failures++;}
    finally{await page.close();}
   }
   const nav=await context.newPage();
   try{
    await nav.goto(origin+'/index.html');
    const menu=nav.locator('#menu-button');
    if(await menu.isVisible()) await menu.click();
    const link=nav.locator('.site-header nav a[href="#modules"]:visible');
    assert.equal(await link.count(),1);
    await link.click();
    assert.equal(new URL(nav.url()).hash,'#modules');
    if(await menu.isVisible()) assert.equal(await menu.getAttribute('aria-expanded'),'false');
    const cards=nav.locator('.home-module-card');
    assert.equal(await cards.count(),6);
    await cards.first().click();
    await nav.waitForURL('**/mano-drug.html');
    assert.equal(await nav.locator('.mano-world-grid a[aria-current="page"]').count(),1);
    console.log('PASS module navigation',width);passed++;
   }catch(e){console.error('FAIL module navigation',width,e.message);failures++;}
   finally{await nav.close();}
   const safety=await context.newPage();
   try{
    await safety.goto(origin+'/index.html');
    await safety.locator('#child-age').selectOption('older');
    await safety.locator('input[value=breath]').check();
    await safety.locator('#warning-submit').click();
    await safety.locator('html[data-mano-quiet]').waitFor();
    assert.equal(await safety.locator('#screening-result').getAttribute('role'),'alert');
    assert.ok(await safety.locator('#screening-result a[href="tel:115"]').isVisible());
    await safety.screenshot({path:'brand-screenshots/safety-'+width+'.png',fullPage:true,animations:'disabled'});
    console.log('PASS safety quiet mode',width);passed++;
   }catch(e){console.error('FAIL safety quiet mode',width,e.message);failures++;}
   finally{await safety.close();}
   await context.close();
  }
 }finally{if(browser)await browser.close();await new Promise(resolve=>server.close(resolve));}
 console.log({passed,failures});if(failures)process.exitCode=1;
})().catch(e=>{console.error(e);process.exitCode=1;});
