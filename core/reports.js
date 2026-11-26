// Read-only report foundation; shared by browser and bundled Android WebView.
let reportPeriod='week';
function reportBounds(mode,day,from,to){
 const d=parse(day),y=d.getFullYear(),m=d.getMonth();
 switch(mode){
 case 'today':return [day,day];
 case 'yesterday':return [addDays(day,-1),addDays(day,-1)];
 case 'week':{const start=addDays(day,-(d.getDay()+6)%7);return [start,addDays(start,6)];}
 case 'month':return [iso(new Date(y,m,1,12)),iso(new Date(y,m+1,0,12))];
 case 'year':return [`${y}-01-01`,`${y}-12-31`];
 case 'custom':return from&&to&&from<=to?[from,to]:null;
 default:return null;
 }
}
function reportFilter(rows,bounds,kind,status){return rows.filter(e=>['reserva','takeaway'].includes(e.kind)&&e.date>=bounds[0]&&e.date<=bounds[1]&&(kind==='all'||e.kind===kind)&&(status==='all'||(status==='done'?e.done:!e.done))).sort((a,b)=>a.date.localeCompare(b.date)||a.time.localeCompare(b.time));}
function renderReports(){
 const bounds=reportBounds(reportPeriod,localDay(),$('reportFrom').value,$('reportTo').value);
 $('reportDates').hidden=reportPeriod!=='custom';
 $('reportPeriods').querySelectorAll('button').forEach(b=>{const active=b.dataset.reportPeriod===reportPeriod;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active));});
 $('reportRange').textContent=bounds?bounds.map(fmtDate).join(' a '):'';
 const content=$('reportContent'),message=$('reportMessage');content.replaceChildren();
 if(!bounds){message.textContent='Seleciona um intervalo de datas válido.';return;}
 const sync=$('syncBanner').dataset.state;
 if(!dataLoaded){message.textContent=sync==='error'?'Não foi possível consultar os eventos.':'A carregar eventos…';return;}
 const rows=reportFilter(events,bounds,$('reportKind').value,$('reportStatus').value);
 message.textContent=(sync==='error'?'Sem ligação. Últimos dados sincronizados. ':'')+(rows.length?`${rows.length} ${rows.length===1?'evento':'eventos'} no intervalo.`:'Sem eventos para os filtros selecionados.');
 // Beta.1 validates the filters against the existing synchronized dataset.
 // Aggregation, charts and detailed listing are intentionally deferred to beta.2.
 content.innerHTML='<div class="reportPreview"><article class="mgCard"><h3>Estatísticas</h3><p class="muted">Indicadores disponíveis na beta.2.</p></article><article class="mgCard"><h3>Gráficos</h3><p class="muted">Distribuição por dia e serviço na beta.2.</p></article><article class="mgCard"><h3>Listagem de eventos</h3><p class="muted">Listagem detalhada disponível na beta.2.</p></article></div>';
}
$('reportPeriods').onclick=e=>{const b=e.target.closest('[data-report-period]');if(!b)return;reportPeriod=b.dataset.reportPeriod;if(reportPeriod==='custom'&&!$('reportFrom').value&&!$('reportTo').value){$('reportFrom').value=localDay();$('reportTo').value=localDay();}renderReports();};
for(const id of ['reportKind','reportStatus','reportFrom','reportTo'])$(id).addEventListener('change',renderReports);
$('reportReset').onclick=()=>{reportPeriod='week';$('reportKind').value='all';$('reportStatus').value='all';$('reportFrom').value='';$('reportTo').value='';renderReports();};
$('reportBack').onclick=goHome;
const reportPreviousBack=window.qpBack;window.qpBack=()=>{if(reportPreviousBack())return true;if(tab==='reports'){goHome();return true;}return false;};
new MutationObserver(()=>{if(tab==='reports'&&!searching)renderReports();}).observe($('syncBanner'),{attributes:true,attributeFilter:['data-state']});
