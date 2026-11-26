function qpIcon(name){const paths=window.QP_ICON_PATHS||{};return `<svg data-qp-icon="${name}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false">${paths[name]||paths.info||''}</svg>`;}
document.querySelectorAll('svg[data-qp-static]').forEach(el=>{el.outerHTML=qpIcon(el.dataset.qpStatic)});
