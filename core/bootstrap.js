// Load desktop presentation only in a browser. Android always uses baseline mobile.
(async()=>{
 if(!window.QPetiscosNative){
   const desktopViewport=matchMedia('(min-width:768px)');let pending=null;
   const ensureDesktop=()=>{
     if(!desktopViewport.matches)return Promise.resolve();
     if(!pending)pending=(async()=>{
       const css=document.createElement('link');css.rel='stylesheet';css.href='desktop/desktop.css';
       const styleReady=new Promise((resolve,reject)=>{css.onload=resolve;css.onerror=reject;});
       document.head.append(css);
       try{
         await styleReady;
         await new Promise((resolve,reject)=>{const script=document.createElement('script');script.src='desktop/desktop.js';script.onload=resolve;script.onerror=reject;document.head.append(script);});
       }catch(e){css.remove();pending=null;console.error('Desktop presentation unavailable; using mobile layout.',e);}
     })();
     return pending;
   };
   desktopViewport.addEventListener('change',async()=>{if(desktopViewport.matches){await ensureDesktop();if(!document.querySelector('main').hidden)render();}});
   await ensureDesktop();
 }
 await (async()=>{try{if(!db)throw new Error('Não foi possível carregar o Supabase. Verifica a Internet.');const data=check(await db.auth.getSession());if(data.session)await setup(data.session.user);else{$('login').hidden=false;ready()}}catch(e){$('login').hidden=false;$('error').textContent=error(e);ready()}})();
})();
