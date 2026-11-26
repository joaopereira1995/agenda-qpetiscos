const qpReportIcon='<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M8 17v-3m4 3v-6m4 6v-4"/>';
function qpIcon(name){const paths=window.QP_ICON_PATHS||{};return `<svg data-qp-icon="${name}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${(name==='report'?qpReportIcon:paths[name])||paths.info||''}</svg>`;}
document.querySelectorAll('svg[data-qp-static]').forEach(el=>{el.outerHTML=qpIcon(el.dataset.qpStatic)});
