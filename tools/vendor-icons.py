"""Vendor a pinned official Lucide static package for Web and Android builds."""
import json, pathlib, sys, tarfile, xml.etree.ElementTree as ET, html, shutil
root=pathlib.Path(__file__).resolve().parent.parent
aliases=json.loads((root/'tools/icon-map.json').read_text())
ns='http://schemas.android.com/apk/res/android'
ET.register_namespace('android',ns)
def tag(e): return e.tag.rsplit('}',1)[-1]
def attr(e, name, default='0'): return float(e.get(name,default))
def path_data(e):
 t=tag(e)
 if t=='path': return e.attrib['d']
 if t=='circle':
  x,y,r=[attr(e,k) for k in ('cx','cy','r')];return f'M{x-r} {y}a{r} {r} 0 1 0 {2*r} 0a{r} {r} 0 1 0 {-2*r} 0Z'
 if t=='line': return f'M{e.attrib["x1"]} {e.attrib["y1"]}L{e.attrib["x2"]} {e.attrib["y2"]}'
 if t in ('polyline','polygon'): return 'M'+e.attrib['points']+('Z' if t=='polygon' else '')
 if t=='rect':
  x,y,w,h=[attr(e,k) for k in ('x','y','width','height')];rx=min(attr(e,'rx'),w/2,h/2);return f'M{x+rx} {y}H{x+w-rx}Q{x+w} {y} {x+w} {y+rx}V{y+h-rx}Q{x+w} {y+h} {x+w-rx} {y+h}H{x+rx}Q{x} {y+h} {x} {y+h-rx}V{y+rx}Q{x} {y} {x+rx} {y}Z'
 if t=='ellipse':
  x,y,rx,ry=[attr(e,k) for k in ('cx','cy','rx','ry')];return f'M{x-rx} {y}a{rx} {ry} 0 1 0 {2*rx} 0a{rx} {ry} 0 1 0 {-2*rx} 0Z'
 raise ValueError('Unsupported SVG element: '+t)
with tarfile.open(sys.argv[1],'r:gz') as tf:
 def read(name): return tf.extractfile(name).read()
 icons={k:ET.fromstring(read(f'package/icons/{v}.svg')) for k,v in aliases.items()}
 def markup(e): return '<'+tag(e)+' '+ ' '.join(f'{k}="{html.escape(v,quote=True)}"' for k,v in e.attrib.items())+'/>'
 data={k:''.join(markup(e) for e in v if tag(e) in ('path','circle','rect','line','polyline','polygon','ellipse')) for k,v in icons.items()}
 if not all(data.values()): raise ValueError('Empty icon')
 (root/'assets/vendor/qp-icons.js').write_text('/* Lucide Static 0.468.0; see LUCIDE-LICENSE.txt */\nwindow.QP_ICON_PATHS='+json.dumps(data)+';\nwindow.QP_ICON_SOURCE="lucide-0.468.0";\n')
 license_names=[m.name for m in tf.getmembers() if m.name.lower() in ('package/license','package/license.md','package/license.txt')]
 if not license_names: raise ValueError('Missing Lucide license')
 (root/'assets/vendor/LUCIDE-LICENSE.txt').write_bytes(read(license_names[0]))
 native={'widget_calendar':'calendarPlain','widget_collapse':'chevronUp','widget_expand':'chevronDown','widget_moon':'moon','widget_next':'chevron','widget_pending':'pending','widget_plus':'plus','widget_prev':'prev','widget_refresh':'refresh','widget_sun':'sun','widget_trash':'trash','widget_users':'users','widget_warning':'warning','widget_wifi':'wifi','push_alarm':'alarm','push_bag':'bag','push_check':'success','push_clock':'clock','push_edit':'edit','push_note':'note','push_undo':'undo','push_user':'user'}
 for filename,key in native.items():
  a=lambda k:'{'+ns+'}'+k
  vector=ET.Element('vector',{a('width'):'24dp',a('height'):'24dp',a('viewportWidth'):'24',a('viewportHeight'):'24'})
  for e in icons[key]:
   if tag(e) not in ('path','circle','rect','line','polyline','polygon','ellipse'):continue
   ET.SubElement(vector,'path',{a('pathData'):path_data(e),a('fillColor'):'@android:color/transparent',a('strokeColor'):'#ffffff',a('strokeWidth'):'2',a('strokeLineCap'):'round',a('strokeLineJoin'):'round'})
  ET.ElementTree(vector).write(root/f'android-app/app/src/main/res/drawable/ic_{filename}.xml',encoding='utf-8',xml_declaration=True)
 print(f'Vendored {len(data)} official Lucide glyphs and {len(native)} Android vectors.')
