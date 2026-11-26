// Desktop presentation only; native Android and narrow browsers retain original layout.
if (!window.QPetiscosNative) {
const deskMedia=matchMedia('(min-width:768px)');
const deskOn=()=>!window.QPetiscosNative&&deskMedia.matches;
const deskRoot=document.documentElement;
const deskNav=document.createElement('aside');deskNav.id='desktopNav';deskNav.setAttribute('aria-label','Navegação desktop');
const deskLinks=[['agenda','Agenda','calendar'],['calendar','Calendário','calendar'],['events','Eventos','list'],['notes','Notas internas','note'],['statistics','Estatísticas','stats'],['history','Histórico','history'],['settings','Definições','user']];
deskNav.innerHTML='<button id="deskCollapse" aria-label="Recolher menu" aria-expanded="true">'+qi('chevronDown')+'</button><button class="deskBrandButton" title="Agenda de hoje" aria-label="Agenda de hoje"><img class="deskBrand" alt="QPetiscos"></button>'+deskLinks.map(([key,label,glyph])=>`<button data-desk-tab="${key}" title="${label}">${key==='agenda'||key==='calendar'||key==='events'?document.querySelector('#bottomNav [data-tab="'+key+'"]').querySelector('svg').outerHTML:mgIcon(glyph)}<span class="deskLabel">${label}</span></button>`).join('')+'<div id="deskFooter"></div>';
document.querySelector('main').prepend(deskNav);
const deskNew=document.createElement('button');deskNew.id='desktopNew';deskNew.innerHTML=qi('plus')+' Novo evento';deskNew.onclick=()=>$('add').click();$('listPanel').querySelector('header').append(deskNew);
function deskCollapse(v){deskRoot.classList.toggle('deskCollapsed',v);$('deskCollapse').setAttribute('aria-expanded',String(!v));$('deskCollapse').setAttribute('aria-label',v?'Expandir menu':'Recolher menu');$('deskCollapse').innerHTML='<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M9 4v16M'+(v?'13 9l3 3-3 3':'16 9l-3 3 3 3')+'"/></svg>';}
deskCollapse(qpStorage.getItem('deskCollapsed')==='1');$('deskCollapse').onclick=()=>{const v=!deskRoot.classList.contains('deskCollapsed');qpStorage.setItem('deskCollapsed',v?'1':'0');deskCollapse(v);};
deskNav.querySelector('.deskBrandButton').onclick=()=>{selected=today();setTab('agenda');};
deskNav.onclick=e=>{const t=e.target.closest('[data-desk-tab]')?.dataset.deskTab;if(!t)return;closePopups();if(t==='notes')$('moreNotes').click();else if(t==='statistics')$('moreStats').click();else if(t==='history')$('moreHistory').click();else setTab(t);};
let deskSort='date',deskDirection=1;
function deskTable(list){const cols=[['date','Data'],['time','Hora'],['kind','Tipo'],['name','Nome / Pedido'],['people','Pessoas'],['notes','Notas'],['done','Estado']];list.sort((a,b)=>deskDirection*(deskSort==='people'||deskSort==='done'?Number(a[deskSort]||0)-Number(b[deskSort]||0):String(a[deskSort]||'').localeCompare(String(b[deskSort]||''),'pt-PT'))||a.date.localeCompare(b.date)||a.time.localeCompare(b.time));return '<div class="deskTableWrap"><table class="deskTable"><thead><tr>'+cols.map(([k,l])=>`<th scope="col" ${deskSort===k?'aria-sort="'+(deskDirection===1?'ascending':'descending')+'"':''}><button data-desk-sort="${k}">${l}${deskSort===k?(deskDirection===1?' ↑':' ↓'):''}</button></th>`).join('')+'<th>Ações</th></tr></thead><tbody>'+list.map(e=>{const temp=document.createElement('template');temp.innerHTML=card(e,true);const actions=temp.content.querySelector('.actions').outerHTML;return `<tr data-event="${esc(e.id)}" class="${e.done?'done':''}" style="--stripe:var(--${e.kind==='reserva'?'res':'take'})"><td>${esc(parse(e.date).toLocaleDateString('pt-PT'))}</td><td>${esc(e.time)}</td><td>${e.kind==='reserva'?'Reserva':'Takeaway'}</td><td><strong>${esc(e.name)}</strong></td><td>${e.kind==='reserva'?esc(e.people||''):'−'}</td><td>${esc(e.notes||'')}</td><td>${e.done?'Concluído':'Pendente'}</td><td>${actions}</td></tr>`}).join('')+'</tbody></table></div>';}
$('events').addEventListener('click',e=>{const k=e.target.closest('[data-desk-sort]')?.dataset.deskSort;if(!k)return;if(k===deskSort)deskDirection*=-1;else{deskSort=k;deskDirection=1;}render();},true);
// Reuse the already-filtered list; desktop never renders the mobile list first.
qpViewRenderer=function(list,currentTab){
 if(!deskOn()||!['agenda','calendar','events'].includes(currentTab))return mobileRenderList(list,currentTab);
 if(currentTab==='events')return list.length?deskTable([...list]):'';
 return '<div class="deskServices">'+[['Almoço','sun',list.filter(e=>e.time<'15:00')],['Jantar','moon',list.filter(e=>e.time>='15:00')]].map(([label,icon,items])=>`<section class="deskService"><h3>${qi(icon)} ${label} <small>${items.length} eventos · ${items.reduce((a,e)=>a+(e.kind==='reserva'?Number(e.people||0):0),0)} pessoas</small></h3>${items.length?items.map(e=>card(e)).join(''):emptyState('Sem eventos','Neste serviço')}</section>`).join('')+'</div>';
};
// Keep the existing Total / Reservas / Takeaway filters and add a read-only people KPI.
function deskMetrics(){
 const host=$('filters');host.classList.toggle('deskMetricGrid',deskOn()&&!searching&&['agenda','calendar'].includes(tab));
 if(!host.classList.contains('deskMetricGrid'))return;
 const meta={all:['calendar','Total'],reserva:['calendar','Reservas'],takeaway:['bag','Takeaways']};
 for(const b of host.querySelectorAll('[data-filter]')){
   const [glyph,label]=meta[b.dataset.filter],value=b.querySelector('b')?.textContent||'0';
   b.innerHTML=`${qi(glyph)}<span class="deskMetricValue">${esc(value)}</span><span>${label}</span>`;
   b.setAttribute('aria-label',`${label}: ${value}`);
 }
 const value=!dataLoaded&&$('syncBanner').dataset.state==='error'?'–':events.filter(e=>e.date===selected&&e.kind==='reserva').reduce((n,e)=>n+Number(e.people||0),0);
 const people=document.createElement('div');people.className='deskPeopleMetric';people.innerHTML=`${qi('users')}<span class="deskMetricValue">${esc(value)}</span><span>Pessoas previstas</span>`;
 host.insertBefore(people,host.querySelector('[data-filter="takeaway"]'));$('peopleSummary').hidden=true;
}
const deskRenderOriginal=render;
render=function(){
 deskRoot.classList.toggle('qpDesktop',deskOn());deskRenderOriginal();deskMetrics();if(!deskOn())return;
 deskNav.querySelector('.deskBrand').src=$('headerLogo').getAttribute('src')||'';
 deskNav.querySelectorAll('[data-desk-tab]').forEach(b=>{if(b.dataset.deskTab===tab&&!searching)b.setAttribute('aria-current','page');else b.removeAttribute('aria-current')});
 $('deskFooter').textContent=$('moreVersion').textContent||$('appVersion').textContent;
 deskNew.disabled=qpReadOnly();deskNew.title=qpReadOnly()?'Ativa o modo Edição nas Definições':'';
};
const deskThemeOriginal=applyTheme;applyTheme=function(){deskThemeOriginal();deskNav.querySelector('.deskBrand').src=$('headerLogo').getAttribute('src')||'';};
const deskBetaRefresh=window.qpBetaModeRefresh;window.qpBetaModeRefresh=function(){deskBetaRefresh();deskNew.disabled=qpReadOnly();deskNew.title=qpReadOnly()?'Ativa o modo Edição nas Definições':'';$('deskFooter').textContent=$('moreVersion').textContent||$('appVersion').textContent;};
new MutationObserver(()=>{$('deskFooter').textContent=$('moreVersion').textContent;}).observe($('moreVersion'),{childList:true,characterData:true,subtree:true});
deskMedia.addEventListener('change',()=>{render();if(tab==='statistics')statsRender();});deskRoot.classList.toggle('qpDesktop',deskOn());


}
