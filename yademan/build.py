#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Yademan — Luxury Persian daily-reminder app UI kit builder.
Builds 8 self-contained RTL screens, renders hi-res PNG screenshots,
a showcase page, design-system source files and a final zip.
"""
import base64, json, zipfile
from pathlib import Path

ROOT = Path('/home/user/yademan')
FONTS = ROOT / 'assets' / 'fonts'
(ROOT / 'screens').mkdir(parents=True, exist_ok=True)
(ROOT / 'screenshots').mkdir(parents=True, exist_ok=True)
(ROOT / 'design-system' / 'icons').mkdir(parents=True, exist_ok=True)

def b64(p: Path) -> str:
    return base64.b64encode(p.read_bytes()).decode()

F = {
    'ar400': b64(FONTS / 'vazirmatn-400.woff2'),
    'ar500': b64(FONTS / 'vazirmatn-500.woff2'),
    'ar700': b64(FONTS / 'vazirmatn-700.woff2'),
    'ar800': b64(FONTS / 'vazirmatn-800.woff2'),
    'la400': b64(FONTS / 'vazirmatn-latin-400.woff2'),
    'la700': b64(FONTS / 'vazirmatn-latin-700.woff2'),
}

FONT_CSS = """
@font-face{font-family:'Vazirmatn';font-weight:400;font-display:swap;src:url(data:font/woff2;base64,__AR400__) format('woff2');unicode-range:U+0600-06FF,U+2000-206F}
@font-face{font-family:'Vazirmatn';font-weight:500;font-display:swap;src:url(data:font/woff2;base64,__AR500__) format('woff2');unicode-range:U+0600-06FF,U+2000-206F}
@font-face{font-family:'Vazirmatn';font-weight:700;font-display:swap;src:url(data:font/woff2;base64,__AR700__) format('woff2');unicode-range:U+0600-06FF,U+2000-206F}
@font-face{font-family:'Vazirmatn';font-weight:800;font-display:swap;src:url(data:font/woff2;base64,__AR800__) format('woff2');unicode-range:U+0600-06FF,U+2000-206F}
@font-face{font-family:'Vazirmatn';font-weight:400;font-display:swap;src:url(data:font/woff2;base64,__LA400__) format('woff2');unicode-range:U+0000-00FF,U+2010-2014}
@font-face{font-family:'Vazirmatn';font-weight:700;font-display:swap;src:url(data:font/woff2;base64,__LA700__) format('woff2');unicode-range:U+0000-00FF,U+2010-2014}
""".replace('__AR400__', F['ar400']).replace('__AR500__', F['ar500']).replace(
 '__AR700__', F['ar700']).replace('__AR800__', F['ar800']).replace(
 '__LA400__', F['la400']).replace('__LA700__', F['la700'])

TOKENS = """:root{
--bg-0:#05070F;--bg-1:#0B0F1E;--bg-2:#131A30;--bg-3:#1A2240;
--surface:rgba(255,255,255,.055);--surface-2:rgba(255,255,255,.09);
--stroke:rgba(255,255,255,.11);--stroke-soft:rgba(255,255,255,.07);
--gold-1:#F7DE8B;--gold-2:#DDAF4B;--gold-3:#9C741F;--gold-solid:#D9B45B;
--gold-grad:linear-gradient(135deg,#F7DE8B 0%,#DDAF4B 48%,#A87F22 100%);
--ink-1:#F7F4EC;--ink-2:#B9BED2;--ink-3:#7E8399;
--c-gold:#E8B84B;--c-violet:#9D8CFF;--c-cyan:#4CC3FF;--c-rose:#FF7A9E;--c-green:#3ED598;--c-amber:#FFB84D;
--danger:#FF6B81;--success:#3ED598;
--r-lg:26px;--r-md:20px;--r-sm:14px;
--shadow-gold:0 14px 34px -8px rgba(221,175,75,.45);
--font:'Vazirmatn',Tahoma,sans-serif;
}"""

CSS = """
*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
html,body{height:100%}
body{margin:0;display:flex;align-items:center;justify-content:center;min-height:100vh;background:#04060D;font-family:var(--font);-webkit-font-smoothing:antialiased}
.screen{position:relative;width:390px;height:844px;overflow:hidden;flex:none;border-radius:48px;color:var(--ink-1);
background:radial-gradient(120% 55% at 85% -8%,rgba(221,175,75,.16),transparent 60%),radial-gradient(90% 45% at 8% 108%,rgba(157,140,255,.14),transparent 60%),linear-gradient(180deg,#0D1326 0%,#0B0F1E 45%,#070B16 100%);
border:1px solid rgba(255,255,255,.13);box-shadow:0 40px 90px -20px rgba(0,0,0,.8);isolation:isolate}
.screen::before{content:'';position:absolute;top:0;left:8%;right:8%;height:1px;background:linear-gradient(90deg,transparent,rgba(247,222,139,.7),transparent);z-index:5}
.orb{position:absolute;border-radius:50%;filter:blur(70px);z-index:-1;pointer-events:none}
.orb-a{width:260px;height:260px;top:-90px;left:-70px;background:rgba(221,175,75,.13)}
.orb-b{width:300px;height:300px;bottom:-60px;right:-100px;background:rgba(157,140,255,.12)}
.statusbar{position:relative;display:flex;justify-content:space-between;align-items:center;padding:20px 30px 4px;font-size:14.5px;font-weight:700;letter-spacing:.2px}
.island{position:absolute;top:15px;left:50%;transform:translateX(-50%);width:112px;height:31px;background:#000;border-radius:20px;box-shadow:inset 0 0 0 1px rgba(255,255,255,.06)}
.island::after{content:'';position:absolute;left:22px;top:50%;transform:translateY(-50%);width:9px;height:9px;border-radius:50%;background:radial-gradient(circle at 35% 35%,#2b3350,#000)}
.sb-ics{display:flex;align-items:center;gap:7px}
.content{padding:6px 20px 0}
.appbar{display:flex;align-items:center;gap:12px;margin-top:4px}
.avatar{width:50px;height:50px;border-radius:18px;background:var(--gold-grad);display:grid;place-items:center;font-size:22px;font-weight:800;color:#241A05;box-shadow:var(--shadow-gold);flex:none}
.avatar-ring{padding:2px;border-radius:20px;background:linear-gradient(135deg,rgba(247,222,139,.8),rgba(247,222,139,.05));flex:none}
.grow{flex:1;min-width:0}
.hello{font-size:12px;color:var(--ink-2);font-weight:500}
.name{font-size:19px;font-weight:800;margin-top:1px}
.name .gold{background:var(--gold-grad);-webkit-background-clip:text;background-clip:text;color:transparent}
.icon-btn{position:relative;width:46px;height:46px;border-radius:16px;background:var(--surface);border:1px solid var(--stroke);display:grid;place-items:center;color:var(--ink-1);flex:none}
.icon-btn .dot{position:absolute;top:11px;left:12px;width:9px;height:9px;border-radius:50%;background:var(--danger);border:2px solid #0B0F1E}
.h1{font-size:21px;font-weight:800}
.sub{font-size:12.5px;color:var(--ink-2);margin-top:3px}
.hero{margin-top:14px;background:linear-gradient(155deg,rgba(247,222,139,.15),rgba(221,175,75,.04) 55%,rgba(157,140,255,.08));border:1px solid rgba(221,175,75,.32);border-radius:var(--r-lg);padding:12px 16px;display:flex;gap:14px;align-items:center;position:relative;overflow:hidden}
.hero::after{content:'';position:absolute;top:-40px;left:-40px;width:130px;height:130px;border-radius:50%;background:radial-gradient(circle,rgba(247,222,139,.25),transparent 70%)}
.ring{position:relative;width:80px;height:80px;flex:none}
.ring svg{transform:rotate(-90deg)}
.ring .pct{position:absolute;inset:0;display:grid;place-items:center;font-size:16px;font-weight:800}
.hero-t{font-size:15px;font-weight:800}
.hero-s{font-size:11.5px;color:var(--ink-2);margin-top:4px;line-height:1.9}
.btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;border:none;cursor:pointer;font-family:var(--font);font-weight:700;border-radius:16px}
.btn-gold{background:var(--gold-grad);color:#241A05;box-shadow:var(--shadow-gold)}
.btn-sm{font-size:12.5px;padding:9px 18px;border-radius:13px;margin-top:9px}
.btn-block{width:100%;font-size:15px;padding:16px}
.btn-ghost{background:var(--surface);border:1px solid var(--stroke);color:var(--ink-1);font-size:12.5px;padding:9px 16px;border-radius:13px}
.sec{display:flex;justify-content:space-between;align-items:center;margin:14px 2px 9px}
.sec-t{font-size:15px;font-weight:800}
.sec-l{font-size:12px;color:var(--gold-solid);font-weight:700}
.qa{display:grid;grid-template-columns:repeat(4,1fr);gap:10px}
.qa-it{background:var(--surface);border:1px solid var(--stroke);border-radius:19px;padding:10px 2px 9px;text-align:center}
.qa-ic{width:40px;height:40px;border-radius:14px;margin:0 auto 7px;display:grid;place-items:center}
.qa-tx{font-size:11px;font-weight:700}
.task{display:flex;align-items:center;gap:11px;background:var(--surface);border:1px solid var(--stroke);border-radius:19px;padding:10px 13px;margin-bottom:8px}
.t-ic{width:44px;height:44px;border-radius:15px;display:grid;place-items:center;flex:none}
.t-body{flex:1;min-width:0}
.t-title{font-size:13.5px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.t-sub{font-size:11px;color:var(--ink-2);margin-top:3px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.t-time{font-size:11px;font-weight:700;color:var(--gold-1);background:rgba(221,175,75,.12);border:1px solid rgba(221,175,75,.25);padding:5px 10px;border-radius:10px;white-space:nowrap}
.check{width:27px;height:27px;border-radius:50%;border:1.6px solid rgba(255,255,255,.32);flex:none;display:grid;place-items:center;color:#241A05}
.check.on{background:var(--gold-grad);border:none;box-shadow:0 4px 14px rgba(221,175,75,.4)}
.c-gold{background:rgba(232,184,75,.14);color:var(--c-gold)}
.c-violet{background:rgba(157,140,255,.14);color:var(--c-violet)}
.c-cyan{background:rgba(76,195,255,.13);color:var(--c-cyan)}
.c-rose{background:rgba(255,122,158,.13);color:var(--c-rose)}
.c-green{background:rgba(62,213,152,.12);color:var(--c-green)}
.c-amber{background:rgba(255,184,77,.13);color:var(--c-amber)}
.summary{background:linear-gradient(155deg,rgba(247,222,139,.16),rgba(221,175,75,.03) 60%);border:1px solid rgba(221,175,75,.32);border-radius:var(--r-lg);padding:13px 16px;margin-top:14px;position:relative;overflow:hidden}
.summary::after{content:'';position:absolute;top:-50px;right:-50px;width:150px;height:150px;border-radius:50%;background:radial-gradient(circle,rgba(247,222,139,.22),transparent 70%)}
.sum-lb{font-size:12px;color:var(--ink-2)}
.sum-vl{font-size:23px;font-weight:800;margin-top:5px}
.sum-vl small{font-size:12.5px;font-weight:500;color:var(--ink-2)}
.pbar{height:9px;border-radius:6px;background:rgba(255,255,255,.09);margin-top:10px;overflow:hidden}
.pbar i{display:block;height:100%;border-radius:6px;background:var(--gold-grad);box-shadow:0 0 12px rgba(221,175,75,.5)}
.sum-row{display:flex;justify-content:space-between;align-items:center;margin-top:7px;font-size:11.5px;color:var(--ink-2)}
.tabs{display:flex;background:rgba(255,255,255,.05);border:1px solid var(--stroke);border-radius:16px;padding:4px;margin-top:11px}
.tab{flex:1;text-align:center;font-size:13px;font-weight:700;color:var(--ink-3);padding:8px;border-radius:12px}
.tab.on{background:var(--gold-grad);color:#241A05;box-shadow:var(--shadow-gold)}
.tab small{font-weight:500;opacity:.75}
.bill{display:flex;align-items:center;gap:11px;background:var(--surface);border:1px solid var(--stroke);border-radius:19px;padding:10px 13px;margin-top:7px}
.bill-amt{font-size:13.5px;font-weight:800;white-space:nowrap}
.chip{font-size:10.5px;font-weight:700;padding:4px 11px;border-radius:20px;white-space:nowrap}
.chip.warn{background:rgba(255,184,77,.14);color:var(--c-amber);border:1px solid rgba(255,184,77,.3)}
.chip.danger{background:rgba(255,107,129,.13);color:var(--danger);border:1px solid rgba(255,107,129,.3)}
.chip.info{background:rgba(76,195,255,.12);color:var(--c-cyan);border:1px solid rgba(76,195,255,.3)}
.chip.ok{background:rgba(62,213,152,.12);color:var(--success);border:1px solid rgba(62,213,152,.3)}
.chip.gold{background:rgba(221,175,75,.13);color:var(--gold-1);border:1px solid rgba(221,175,75,.32)}
.bill-col{display:flex;flex-direction:column;align-items:flex-end;gap:6px}
.pay{font-size:11px;font-weight:800;padding:6px 15px;border-radius:11px}
.week{display:flex;gap:7px;margin-top:14px}
.day{flex:1;text-align:center;padding:9px 0 8px;border-radius:16px;background:var(--surface);border:1px solid var(--stroke)}
.day .dw{font-size:10px;color:var(--ink-3);font-weight:700}
.day .dn{font-size:15px;font-weight:800;margin-top:3px}
.day .ev{display:flex;gap:3px;justify-content:center;margin-top:5px;height:5px}
.day .ev i{width:5px;height:5px;border-radius:50%}
.day.today{background:var(--gold-grad);border:none;box-shadow:var(--shadow-gold)}
.day.today .dw,.day.today .dn{color:#241A05}
.tl{margin-top:4px}
.tl-it{display:flex;gap:12px}
.tl-tm{font-size:12px;font-weight:800;color:var(--ink-2);width:44px;text-align:left;padding-top:15px;flex:none}
.tl-ln{display:flex;flex-direction:column;align-items:center;flex:none}
.tl-dot{width:13px;height:13px;border-radius:50%;margin-top:17px;box-shadow:0 0 0 4px rgba(255,255,255,.06)}
.tl-bar{width:2px;flex:1;background:rgba(255,255,255,.1);border-radius:2px;margin:4px 0}
.tl-it:last-child .tl-bar{display:none}
.tl-card{flex:1;background:var(--surface);border:1px solid var(--stroke);border-radius:18px;padding:11px 13px;margin:6px 0 12px;display:flex;align-items:center;gap:10px}
.med{display:flex;align-items:center;gap:11px;background:var(--surface);border:1px solid var(--stroke);border-radius:19px;padding:11px 13px;margin-top:9px}
.med-btn{font-size:11.5px;font-weight:800;padding:9px 15px;border-radius:12px;white-space:nowrap}
.ad-card{background:linear-gradient(155deg,rgba(62,213,152,.1),rgba(62,213,152,.02));border:1px solid rgba(62,213,152,.25);border-radius:var(--r-lg);padding:15px 16px;margin-top:14px}
.ad-top{display:flex;justify-content:space-between;align-items:center}
.ad-vl{font-size:22px;font-weight:800;color:var(--c-green)}
.ad-days{display:flex;gap:8px;margin-top:12px}
.ad-d{flex:1;text-align:center}
.ad-d i{display:grid;place-items:center;width:34px;height:34px;margin:0 auto;border-radius:50%;background:rgba(62,213,152,.13);border:1px solid rgba(62,213,152,.3);color:var(--c-green);font-style:normal}
.ad-d.off i{background:rgba(255,255,255,.05);border-color:var(--stroke);color:var(--ink-3)}
.ad-d span{font-size:10px;color:var(--ink-3);display:block;margin-top:5px}
.addbar{display:flex;gap:9px;margin-top:14px}
.input{flex:1;background:rgba(255,255,255,.05);border:1px solid var(--stroke);border-radius:16px;padding:12px 16px;font-family:var(--font);font-size:13.5px;color:var(--ink-1);outline:none;min-width:0}
.input::placeholder{color:var(--ink-3)}
.addbtn{width:52px;border-radius:16px;flex:none;font-size:22px}
.fchips{display:flex;gap:8px;margin-top:12px}
.fchip{font-size:11.5px;font-weight:700;padding:8px 15px;border-radius:13px;background:var(--surface);border:1px solid var(--stroke);color:var(--ink-2)}
.fchip.on{background:rgba(221,175,75,.14);border-color:rgba(221,175,75,.4);color:var(--gold-1)}
.shop{display:flex;align-items:center;gap:11px;background:var(--surface);border:1px solid var(--stroke);border-radius:17px;padding:10px 13px;margin-top:8px}
.shop.done{opacity:.55}
.shop.done .t-title{text-decoration:line-through}
.sq{width:26px;height:26px;border-radius:9px;border:1.6px solid rgba(255,255,255,.32);flex:none;display:grid;place-items:center;color:#241A05}
.sq.on{background:var(--gold-grad);border:none}
.shop-pr{font-size:11.5px;color:var(--ink-2);white-space:nowrap}
.cartbar{position:absolute;bottom:104px;left:20px;right:20px;background:rgba(18,23,42,.92);backdrop-filter:blur(16px);border:1px solid rgba(221,175,75,.3);border-radius:20px;padding:12px 16px;display:flex;align-items:center;gap:10px;box-shadow:0 16px 40px rgba(0,0,0,.5)}
.note{background:var(--surface);border:1px solid var(--stroke);border-radius:19px;padding:13px 14px;margin-top:9px;display:flex;gap:11px}
.note.pin{background:linear-gradient(155deg,rgba(247,222,139,.13),rgba(221,175,75,.03));border-color:rgba(221,175,75,.35)}
.tag{width:5px;border-radius:3px;flex:none}
.mini{display:flex;align-items:center;gap:7px;font-size:11.5px;color:var(--ink-2);margin-top:7px}
.mini .sq{width:19px;height:19px;border-radius:7px}
.mini.done span{text-decoration:line-through;opacity:.6}
.flabel{font-size:12.5px;font-weight:800;margin:10px 2px 6px;color:var(--ink-1)}
.tgrid{display:grid;grid-template-columns:repeat(3,1fr);gap:8px}
.tg{background:var(--surface);border:1.5px solid var(--stroke);border-radius:16px;padding:8px 4px;text-align:center;color:var(--ink-2)}
.tg .qa-ic{width:32px;height:32px;border-radius:11px}
.tg.sel{border-color:rgba(221,175,75,.6);background:rgba(221,175,75,.09);color:var(--ink-1);box-shadow:0 0 0 1px rgba(221,175,75,.3)}
.tg-tx{font-size:11px;font-weight:700}
.frow{display:flex;gap:9px}
.frow .input{text-align:center;font-weight:700}
.seg{display:flex;background:rgba(255,255,255,.05);border:1px solid var(--stroke);border-radius:15px;padding:4px;gap:4px}
.seg-it{flex:1;text-align:center;font-size:12px;font-weight:700;color:var(--ink-3);padding:8px 2px;border-radius:11px}
.seg-it.on{background:var(--gold-grad);color:#241A05}
.notif{display:flex;gap:11px;background:var(--surface);border:1px solid var(--stroke);border-radius:19px;padding:10px 13px;margin-top:8px;align-items:flex-start}
.notif.read{opacity:.6}
.ndot{width:8px;height:8px;border-radius:50%;background:var(--gold-2);margin-top:7px;flex:none;box-shadow:0 0 10px rgba(221,175,75,.8)}
.ntime{font-size:10.5px;color:var(--ink-3);margin-top:4px}
.setrow{display:flex;align-items:center;gap:11px;background:var(--surface);border:1px solid var(--stroke);border-radius:17px;padding:10px 14px;margin-top:7px}
.tgl{width:48px;height:29px;border-radius:16px;background:rgba(255,255,255,.12);position:relative;flex:none;transition:.2s}
.tgl::after{content:'';position:absolute;top:3px;right:3px;width:23px;height:23px;border-radius:50%;background:#fff;transition:.2s}
.tgl.on{background:var(--gold-grad)}
.tgl.on::after{transform:translateX(-19px)}
.bottomnav{position:absolute;bottom:0;left:0;right:0;padding:8px 16px 20px;background:linear-gradient(180deg,transparent 0%,rgba(5,7,13,.88) 38%);z-index:5}
.navbar{display:flex;background:rgba(17,22,40,.88);backdrop-filter:blur(22px);-webkit-backdrop-filter:blur(22px);border:1px solid var(--stroke);border-radius:26px;padding:9px 4px 7px;align-items:flex-end;justify-content:space-around;box-shadow:0 18px 44px rgba(0,0,0,.55)}
.nav-it{display:flex;flex-direction:column;align-items:center;gap:4px;font-size:10px;font-weight:700;color:var(--ink-3);min-width:58px;padding:3px 0}
.nav-it.on{color:var(--gold-1)}
.nav-it.on .nv-ic{background:rgba(221,175,75,.15);border-color:rgba(221,175,75,.35)}
.nv-ic{width:42px;height:30px;border-radius:12px;display:grid;place-items:center;border:1px solid transparent}
.fab{width:58px;height:58px;border-radius:21px;background:var(--gold-grad);display:grid;place-items:center;color:#241A05;margin:-32px 2px 0;box-shadow:var(--shadow-gold);border:4px solid #0C1226}
.home-ind{width:118px;height:5px;border-radius:3px;background:rgba(255,255,255,.4);margin:9px auto 0}
.savewrap{position:absolute;bottom:104px;left:20px;right:20px;z-index:5}
.row{display:flex;align-items:center;gap:10px}
.mt6{margin-top:6px}.gold-tx{color:var(--gold-1)}
"""

# ---------------- icons ----------------
P = {
 'home': '<path d="M4 11l8-7 8 7v9a1.5 1.5 0 0 1-1.5 1.5H14v-6h-4v6H5.5A1.5 1.5 0 0 1 4 20z"/>',
 'bell': '<path d="M6 9.5a6 6 0 1 1 12 0c0 4.5 1.8 5.8 1.8 5.8H4.2S6 14 6 9.5"/><path d="M10 19.5a2.2 2.2 0 0 0 4 0"/>',
 'cart': '<path d="M3 4.5h2.2l2.3 11.4a1.2 1.2 0 0 0 1.2 1h8.1a1.2 1.2 0 0 0 1.2-1L20 8H6"/><circle cx="9.5" cy="20" r="1.4"/><circle cx="17" cy="20" r="1.4"/>',
 'user': '<circle cx="12" cy="8" r="4"/><path d="M4.5 20.5c1.4-3.8 4.4-5.5 7.5-5.5s6.1 1.7 7.5 5.5"/>',
 'plus': '<path d="M12 5.5v13M5.5 12h13"/>',
 'check': '<path d="M4.5 12.5l5 5L19.5 7"/>',
 'chev': '<path d="M14 6l-6 6 6 6"/>',
 'card': '<rect x="3" y="6" width="18" height="13" rx="2.5"/><path d="M3 10.5h18M7 15h4"/>',
 'receipt': '<path d="M6 3.5h12V21l-2-1.4-2 1.4-2-1.4L10 21l-2-1.4L6 21z"/><path d="M9 8.5h6M9 12.5h6"/>',
 'cal': '<rect x="4" y="5.5" width="16" height="15" rx="2.5"/><path d="M4 10.5h16M8.5 3v4M15.5 3v4"/>',
 'clock': '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
 'pill': '<rect x="3" y="8.5" width="18" height="7.5" rx="3.75"/><path d="M12 8.5v7.5"/>',
 'note': '<path d="M6 3.5h8.5L19 8v11.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 19.5v-14A1.5 1.5 0 0 1 6 4z"/><path d="M14 3.5V8.5h5M9 13h6M9 16.5h4"/>',
 'gear': '<circle cx="12" cy="12" r="3.2"/><path d="M19 12a7 7 0 0 0-.14-1.4l2-1.55-2-3.46-2.36.95a7 7 0 0 0-2.42-1.4L13.7 1.6h-3.4l-.38 2.54a7 7 0 0 0-2.42 1.4l-2.36-.95-2 3.46 2 1.55a7 7 0 0 0 0 2.8l-2 1.55 2 3.46 2.36-.95a7 7 0 0 0 2.42 1.4l.38 2.54h3.4l.38-2.54a7 7 0 0 0 2.42-1.4l2.36.95 2-3.46-2-1.55c.1-.46.14-.93.14-1.4z"/>',
 'search': '<circle cx="11" cy="11" r="6.5"/><path d="M16 16l4.5 4.5"/>',
 'star': '<path d="M12 3.5l2.6 5.4 5.9.8-4.3 4.1 1 5.8-5.2-2.8-5.2 2.8 1-5.8L3.5 9.7l5.9-.8z"/>',
 'sound': '<path d="M4 10v4h3.5L12 17.5v-11L7.5 10z"/><path d="M15.5 9.5a3.5 3.5 0 0 1 0 5M18 7a7 7 0 0 1 0 10"/>',
 'moon': '<path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z"/>',
 'trash': '<path d="M4.5 7h15M9.5 7V4.5h5V7M6.5 7l.9 13.5h9.2L17.5 7M10 11v6M14 11v6"/>',
 'pin': '<path d="M12 21s-6.8-6-6.8-10.8a6.8 6.8 0 0 1 13.6 0C18.8 15 12 21 12 21z"/><circle cx="12" cy="10" r="2.4"/>',
 'repeat': '<path d="M20 12a8 8 0 0 0-13.6-5.6L4 8.8M4 12a8 8 0 0 0 13.6 5.6l2.4-2.4"/><path d="M4 4.5v4.3h4.3M20 19.5v-4.3h-4.3"/>',
 'wallet': '<path d="M4 7.5A1.5 1.5 0 0 1 5.5 6h13A1.5 1.5 0 0 1 20 7.5V18a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18z"/><path d="M4 9h16M20 13.5h-5a1.5 1.5 0 0 0 0 3h5"/><circle cx="16.5" cy="15" r=".8" fill="currentColor"/>',
 'chart': '<path d="M4 20V10M10 20V4M16 20v-8M20.5 20H2.5"/>',
 'shield': '<path d="M12 3l7 2.8v6.1c0 4.4-2.9 7.3-7 9.1-4.1-1.8-7-4.7-7-9.1V5.8z"/><path d="M9 11.8l2.2 2.2 4-4.2"/>',
 'x': '<path d="M6 6l12 12M18 6L6 18"/>',
 'filter': '<path d="M4 6.5h16M7 12h10M10 17.5h4"/>',
 'spark': '<path d="M12 3.5l1.7 5 5 1.7-5 1.7-1.7 5-1.7-5-5-1.7 5-1.7z"/><path d="M18.5 15.5l.9 2.6 2.6.9-2.6.9-.9 2.6-.9-2.6-2.6-.9 2.6-.9z"/>',
 'signal': '<path d="M5 19v-3M10 19v-6M15 19V9M20 19V5"/>',
}
def ic(name, size=22, sw=1.8):
    return f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round">{P[name]}</svg>'

BATTERY = '<svg width="25" height="12" viewBox="0 0 25 12"><rect x=".7" y=".7" width="20" height="10.6" rx="3" fill="none" stroke="currentColor" opacity=".4"/><rect x="2.4" y="2.4" width="15" height="7.2" rx="1.8" fill="currentColor"/><path d="M23 4v4a2 2 0 0 0 0-4z" fill="currentColor" opacity=".4"/></svg>'
WIFI = '<svg width="16" height="12" viewBox="0 0 16 12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><path d="M1.5 4.5a10 10 0 0 1 13 0M4.5 7.5a6 6 0 0 1 7 0M8 10.5h.01"/></svg>'

def statusbar():
    return f'''<div class="statusbar"><div class="island"></div><span>۹:۴۱</span>
<span class="sb-ics">{ic('signal',16,1.8)}{WIFI}{BATTERY}</span></div>'''

def bottomnav(active):
    def it(key, icon, label):
        on = ' on' if key == active else ''
        return f'<div class="nav-it{on}"><span class="nv-ic">{ic(icon,22,1.8)}</span><span>{label}</span></div>'
    fab_on = ' style="box-shadow:0 0 0 3px rgba(221,175,75,.45),var(--shadow-gold)"' if active == 'add' else ''
    return f'''<div class="bottomnav"><div class="navbar">{it('home','home','خانه')}{it('rem','bell','یادآوری‌ها')}
<div class="fab"{fab_on}>{ic('plus',26,2.4)}</div>{it('shop','cart','خرید')}{it('me','user','من')}</div><div class="home-ind"></div></div>'''

def shell(sid, title, body, nav='home'):
    return f'''<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="UTF-8">
<meta name="viewport" content="width=390, initial-scale=1"><title>{title} | یادمان</title>
<style>{TOKENS}{FONT_CSS}{CSS}</style></head>
<body><div class="screen" id="{sid}"><div class="orb orb-a"></div><div class="orb orb-b"></div>
{statusbar()}<div class="content">{body}</div>{bottomnav(nav)}</div></body></html>'''

# ---------------- screens ----------------
def s_home():
    qa = ''.join([
        f'<div class="qa-it"><div class="qa-ic c-{c}">{ic(i,21,1.8)}</div><div class="qa-tx">{t}</div></div>'
        for i,c,t in [('card','gold','اقساط'),('receipt','violet','چک‌ها'),('cal','cyan','قرارها'),('pill','rose','داروها')]])
    def task(i,c,t,s,tm,done=False):
        ck = f'<div class="check on">{ic("check",14,2.6)}</div>' if done else '<div class="check"></div>'
        return f'<div class="task"><div class="t-ic c-{c}">{ic(i,22,1.8)}</div><div class="t-body"><div class="t-title">{t}</div><div class="t-sub">{s}</div></div><span class="t-time">{tm}</span>{ck}</div>'
    tasks = (task('pill','rose','قرص ویتامین D','بعد از ناهار • ۱ عدد','۱۳:۰۰',True)
        + task('card','gold','قسط وام مسکن','۴٬۲۰۰٬۰۰۰ تومان • بانک ملت','امروز')
        + task('cal','cyan','جلسه با تیم طراحی','دفتر مرکزی • طبقه ۴','۱۶:۰۰')
        + task('cart','green','خرید هفتگی خانه','۷ از ۱۲ قلم خریداری شد','۱۸:۳۰'))
    return f'''<div class="appbar"><div class="avatar-ring"><div class="avatar">س</div></div>
<div class="grow"><div class="hello">جمعه ۲۷ شهریور ۱۴۰۵</div><div class="name">سلام، <span class="gold">سارا</span></div></div>
<div class="icon-btn">{ic('bell',22,1.8)}<span class="dot"></span></div></div>
<div class="hero"><div class="ring"><svg width="80" height="80" viewBox="0 0 86 86"><circle cx="43" cy="43" r="37" fill="none" stroke="rgba(255,255,255,.12)" stroke-width="9"/><circle cx="43" cy="43" r="37" fill="none" stroke="url(#gg)" stroke-width="9" stroke-linecap="round" stroke-dasharray="232.5" stroke-dashoffset="88" /><defs><linearGradient id="gg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F7DE8B"/><stop offset="1" stop-color="#DDAF4B"/></linearGradient></defs></svg><div class="pct">۶۲٪</div></div>
<div class="grow"><div class="hero-t">پیشرفت امروز</div><div class="hero-s">۵ از ۸ کار انجام شد<br>۱ سررسید مهم باقی مانده</div><button class="btn btn-gold btn-sm">مشاهده برنامه</button></div></div>
<div class="sec"><span class="sec-t">دسترسی سریع</span><span class="sec-l">همه</span></div>
<div class="qa">{qa}</div>
<div class="sec"><span class="sec-t">کارهای امروز</span><span class="sec-l">تقویم</span></div>{tasks}'''

def s_bills():
    def bill(i,c,t,s,amt,chip,cls,pay=False):
        b = f'<button class="btn btn-gold pay">پرداخت</button>' if pay else ''
        return f'''<div class="bill"><div class="t-ic c-{c}">{ic(i,22,1.8)}</div>
<div class="t-body"><div class="t-title">{t}</div><div class="t-sub">{s}</div></div>
<div class="bill-col"><span class="bill-amt">{amt}</span><span class="chip {cls}">{chip}</span>{b}</div></div>'''
    return f'''<div class="appbar"><div class="grow"><div class="h1">اقساط و چک‌ها</div><div class="sub">شهریور ۱۴۰۵ • ۵ مورد فعال</div></div>
<div class="icon-btn">{ic('filter',21,1.8)}</div></div>
<div class="summary"><div class="sum-lb">مانده پرداخت این ماه</div>
<div class="sum-vl">۱۲٬۴۵۰٬۰۰۰ <small>تومان</small></div>
<div class="pbar"><i style="width:68%"></i></div>
<div class="sum-row"><span>۶۸٪ پرداخت شده</span><span class="chip warn">۳ سررسید نزدیک</span></div></div>
<div class="tabs"><div class="tab on">اقساط <small>(۳)</small></div><div class="tab">چک‌ها <small>(۲)</small></div></div>
{bill('card','gold','قسط وام مسکن','بانک ملت • هر ماه ۵م','۴٬۲۰۰٬۰۰۰','۳ روز مانده','warn',True)}
{bill('card','gold','قسط خودرو','لیزینگ پارسیان • هر ماه ۱۲م','۲٬۸۵۰٬۰۰۰','سررسید فردا','danger',True)}
{bill('receipt','violet','چک شماره ۱۲۴۵','حساب جاری • وجه حامل','۵٬۰۰۰٬۰۰۰','۷ روز مانده','info')}
{bill('receipt','violet','چک شماره ۱۲۴۴','حساب جاری • پاس شد','۳٬۴۰۰٬۰۰۰','پرداخت شد','ok')}'''

def s_cal():
    days = [('ش','۲۱',0),('ی','۲۲',1),('د','۲۳',0),('س','۲۴',2),('چ','۲۵',1),('پ','۲۶',0),('ج','۲۷',3)]
    wk = ''
    for dw,dn,ev in days:
        dots = '<i style="background:#4CC3FF"></i>'*(1 if ev>=1 else 0)+'<i style="background:#E8B84B"></i>'*(1 if ev>=2 else 0)+'<i style="background:#FF7A9E"></i>'*(1 if ev>=3 else 0)
        cls = ' today' if dn=='۲۷' else ''
        wk += f'<div class="day{cls}"><div class="dw">{dw}</div><div class="dn">{dn}</div><div class="ev">{dots}</div></div>'
    def ev(tm,color,t,s,i):
        return f'''<div class="tl-it"><div class="tl-tm">{tm}</div><div class="tl-ln"><div class="tl-dot" style="background:{color}"></div><div class="tl-bar"></div></div>
<div class="tl-card"><div class="t-ic" style="background:{color}22;color:{color}">{ic(i,20,1.8)}</div>
<div class="t-body"><div class="t-title">{t}</div><div class="t-sub">{s}</div></div></div></div>'''
    return f'''<div class="appbar"><div class="grow"><div class="h1">شهریور ۱۴۰۵</div><div class="sub">جمعه ۲۷ شهریور • ۴ رویداد</div></div>
<div class="icon-btn">{ic('plus',22,2.2)}</div></div>
<div class="week">{wk}</div>
<div class="sec"><span class="sec-t">برنامه امروز</span><span class="chip gold">۴ رویداد</span></div>
<div class="tl">{ev('۱۰:۰۰','#4CC3FF','جلسه با تیم طراحی','دفتر مرکزی • طبقه ۴ • ۱ ساعت','cal')}
{ev('۱۳:۳۰','#3ED598','ناهار با خانواده','رستوران شاندیز • سعادت‌آباد','pin')}
{ev('۱۷:۰۰','#9D8CFF','دندانپزشک','کلینیک لبخند • دکتر محمدی','clock')}
{ev('۲۰:۰۰','#E8B84B','تماس تصویری با مادر','یادآوری ۱۵ دقیقه قبل','bell')}
{ev('۲۲:۰۰','#FF7A9E','مطالعه قبل از خواب','کتاب اثر مرکب • ۳۰ دقیقه','star')}</div>'''

def s_med():
    def med(name,dose,tm,cls,btxt):
        return f'''<div class="med"><div class="t-ic c-rose">{ic('pill',22,1.8)}</div>
<div class="t-body"><div class="t-title">{name}</div><div class="t-sub">{dose}</div></div>
<span class="t-time">{tm}</span><button class="btn {cls} med-btn">{btxt}</button></div>'''
    dd = ''.join([f'<div class="ad-d{" off" if d in ("پ","ج") else ""}"><i>{ic("check",15,2.6) if d not in ("پ","ج") else ""}</i><span>{d}</span></div>' for d in ['ش','ی','د','س','چ','پ','ج']])
    return f'''<div class="appbar"><div class="grow"><div class="h1">یادآوری دارو</div><div class="sub">۴ دارو برای امروز تجویز شده</div></div>
<div class="icon-btn">{ic('chart',21,1.8)}</div></div>
<div class="ad-card"><div class="ad-top"><div><div class="sum-lb">پایبندی این هفته</div><div class="ad-vl">۸۵٪ عالیه!</div></div><span class="chip ok">۶ روز متوالی</span></div>
<div class="ad-days">{dd}</div></div>
<div class="sec"><span class="sec-t">داروهای امروز</span><span class="sec-l">ویرایش</span></div>
{med('متفورمین ۵۰۰','۱ عدد • بعد از صبحانه','۰۸:۰۰','btn-ghost','مصرف شد ✓')}
{med('ویتامین D','۱ عدد • بعد از ناهار','۱۳:۰۰','btn-gold','مصرف شد؟')}
{med('کلسیم + زینک','۱ عدد • همراه ناهار','۱۳:۳۰','btn-ghost','یادآوری')}
{med('امگا ۳','۱ عدد • همراه شام','۲۰:۳۰','btn-ghost','یادآوری')}
{med('منیزیم','۱ عدد • قبل از خواب','۲۳:۰۰','btn-ghost','یادآوری')}'''

def s_shop():
    def row(name,qty,price='',done=False):
        sq = f'<div class="sq on">{ic("check",14,2.8)}</div>' if done else '<div class="sq"></div>'
        cls = ' done' if done else ''
        pr = f'<span class="shop-pr">{price}</span>' if price else ''
        return f'<div class="shop{cls}">{sq}<div class="t-body"><div class="t-title">{name}</div><div class="t-sub">{qty}</div></div>{pr}</div>'
    return f'''<div class="appbar"><div class="grow"><div class="h1">لیست خرید</div><div class="sub">خرید هفتگی خانه • ۷ از ۱۲ قلم</div></div>
<div class="icon-btn">{ic('trash',20,1.8)}</div></div>
<div class="pbar" style="margin-top:14px"><i style="width:58%"></i></div>
<div class="addbar"><input class="input" placeholder="قلم جدید اضافه کن…  مثلاً: پنیر"><button class="btn btn-gold addbtn">+</button></div>
<div class="fchips"><span class="fchip on">همه ۱۲</span><span class="fchip">سوپرمارکت ۵</span><span class="fchip">میوه ۴</span><span class="fchip">لبنیات ۳</span></div>
<div style="margin-top:6px">{row('شیر کم‌چرب','۲ عدد • لبنیات','',True)}{row('نان سنگک','۴ عدد • نانوایی','',True)}
{row('تخم‌مرغ','۱ شانه • سوپرمارکت','۸۵٬۰۰۰')}{row('گوجه‌فرنگی','۲ کیلو • میوه و سبزی','۹۶٬۰۰۰')}
{row('سینه مرغ','۱ کیلو • پروتئینی','۲۱۰٬۰۰۰')}</div>
<div class="cartbar"><div class="t-ic c-green">{ic('cart',22,1.8)}</div>
<div class="grow"><div class="t-title">جمع سبد خرید</div><div class="t-sub">۳۹۱٬۰۰۰ تومان • ۵ قلم</div></div>
<button class="btn btn-gold" style="font-size:12px;padding:10px 20px;border-radius:13px">اتمام خرید</button></div>'''

def s_notes():
    def mini(t,done=False):
        sq = f'<div class="sq on">{ic("check",12,3)}</div>' if done else '<div class="sq"></div>'
        return f'<div class="mini{" done" if done else ""}">{sq}<span>{t}</span></div>'
    return f'''<div class="appbar"><div class="grow"><div class="h1">دفترچه یادداشت</div><div class="sub">۶ یادداشت • ۲ سنجاق شده</div></div>
<div class="icon-btn">{ic('search',21,1.8)}</div></div>
<div class="note pin"><div class="tag" style="background:var(--gold-2)"></div><div class="grow">
<div class="row"><div class="t-title grow">تماس با بانک برای وام</div><span class="chip gold">سنجاق شده</span></div>
{mini('پرسیدن مدارک لازم',True)}{mini('گرفتن نوبت حضوری',True)}{mini('مقایسه سود بانک‌ها')}</div></div>
<div class="note"><div class="tag" style="background:var(--c-cyan)"></div><div class="grow">
<div class="t-title">ایده‌های سفر شمال</div><div class="t-sub">۴ یادداشت • ۲ روز پیش</div>
{mini('رزرو کلبه جنگلی',True)}{mini('لیست وسایل کمپ')}</div></div>
<div class="note"><div class="tag" style="background:var(--c-rose)"></div><div class="grow">
<div class="t-title">کارهای مهم هفته</div><div class="t-sub">۳ مورد • دیروز</div>
{mini('تمدید بیمه خودرو')}{mini('پرداخت قبض برق',True)}</div></div>
<div class="note"><div class="tag" style="background:var(--c-green)"></div><div class="grow">
<div class="t-title">لیست مهمانی جمعه شب</div><div class="t-sub">۶ قلم • امروز</div></div></div>
<div class="note"><div class="tag" style="background:var(--c-amber)"></div><div class="grow"><div class="t-title">جملات انگیزشی روزانه</div><div class="t-sub">۱۲ یادداشت • هفته پیش</div></div></div>
<div class="savewrap"><button class="btn btn-gold btn-block">+ یادداشت جدید</button></div>'''

def s_add():
    tg = ''.join([f'<div class="tg{" sel" if t=="قرار" else ""}"><div class="qa-ic c-{c}">{ic(i,20,1.8)}</div><div class="tg-tx">{t}</div></div>'
        for i,c,t in [('card','gold','قسط'),('receipt','violet','چک'),('cal','cyan','قرار'),('pill','rose','دارو'),('cart','green','خرید'),('star','amber','سایر')]])
    return f'''<div class="appbar"><div class="icon-btn">{ic('x',20,2.2)}</div>
<div class="grow" style="text-align:center"><div class="h1" style="font-size:18px">یادآوری جدید</div></div>
<div style="width:46px"></div></div>
<div class="flabel">نوع یادآوری</div><div class="tgrid">{tg}</div>
<div class="flabel">عنوان</div><input class="input" style="width:100%" value="جلسه با تیم طراحی">
<div class="frow" style="margin-top:9px"><input class="input" value="جمعه ۲۷ شهریور"><input class="input" value="۱۶:۰۰"></div>
<div class="flabel">تکرار</div><div class="seg"><div class="seg-it on">یک‌بار</div><div class="seg-it">روزانه</div><div class="seg-it">هفتگی</div><div class="seg-it">ماهانه</div></div>
<div class="flabel">یادآوری قبل از موعد</div><div class="seg"><div class="seg-it on">۱۵ دقیقه</div><div class="seg-it">۱ ساعت</div><div class="seg-it">۱ روز</div></div>
<div class="flabel">اولویت</div><div class="seg"><div class="seg-it">کم</div><div class="seg-it on">متوسط</div><div class="seg-it">زیاد</div></div>
<div class="savewrap"><button class="btn btn-gold btn-block">✓ &nbsp;ذخیره یادآوری</button></div>'''

def s_notif():
    def nt(i,c,t,d,tm,unread=True):
        return f'''<div class="notif{" read" if not unread else ""}"><div class="t-ic c-{c}">{ic(i,21,1.8)}</div>
<div class="grow"><div class="t-title">{t}</div><div class="t-sub">{d}</div><div class="ntime">{tm}</div></div>
{'<span class="ndot"></span>' if unread else ''}</div>'''
    def st(i,c,t,on=True):
        return f'''<div class="setrow"><div class="t-ic c-{c}" style="width:40px;height:40px">{ic(i,19,1.8)}</div>
<div class="t-body"><div class="t-title" style="font-size:13px">{t}</div></div><div class="tgl{" on" if on else ""}"></div></div>'''
    return f'''<div class="appbar"><div class="grow"><div class="h1">اعلان‌ها</div><div class="sub">۳ اعلان خوانده نشده</div></div>
<div class="icon-btn">{ic('gear',21,1.7)}</div></div>
{nt('card','gold','سررسید قسط نزدیکه!','قسط خودرو فردا سررسید می‌شود','۱۰ دقیقه پیش')}
{nt('pill','rose','وقت دارویته','ویتامین D را بعد از ناهار بخور','۱ ساعت پیش')}
{nt('receipt','violet','یادآوری چک','چک ۱۲۴۵ تا ۷ روز دیگر پاس می‌شود','۳ ساعت پیش')}
{nt('cal','cyan','قرار امروز','جلسه با تیم طراحی ساعت ۱۶:۰۰','دیروز',False)}
<div class="sec"><span class="sec-t">تنظیمات یادآوری</span></div>
{st('spark','gold','یادآوری هوشمند',True)}{st('sound','cyan','صدای اعلان',True)}
{st('moon','violet','مزاحم نشوید ۲۳ تا ۷',False)}'''

SCREENS = [
    ('01-home', 'خانه و داشبورد', s_home(), 'home'),
    ('02-bills', 'اقساط و چک‌ها', s_bills(), 'rem'),
    ('03-calendar', 'قرارها و تقویم', s_cal(), 'rem'),
    ('04-medicine', 'یادآوری دارو', s_med(), 'rem'),
    ('05-shopping', 'لیست خرید', s_shop(), 'shop'),
    ('06-notes', 'دفترچه یادداشت', s_notes(), 'me'),
    ('07-add', 'یادآوری جدید', s_add(), 'add'),
    ('08-notifications', 'اعلان‌ها و تنظیمات', s_notif(), 'rem'),
]

# ---------------- build ----------------
for sid, title, body, nav in SCREENS:
    (ROOT / 'screens' / f'{sid}.html').write_text(shell(sid, title, body, nav), encoding='utf-8')
print('screens written:', len(SCREENS))

# design-system sources
DS = ROOT / 'design-system'
(DS / 'design-tokens.css').write_text(
    "/* Yademan design tokens — font files in ../assets/fonts */\n"
    "@font-face{font-family:'Vazirmatn';font-weight:400;font-display:swap;src:url('../assets/fonts/vazirmatn-400.woff2') format('woff2')}\n"
    "@font-face{font-family:'Vazirmatn';font-weight:500;font-display:swap;src:url('../assets/fonts/vazirmatn-500.woff2') format('woff2')}\n"
    "@font-face{font-family:'Vazirmatn';font-weight:700;font-display:swap;src:url('../assets/fonts/vazirmatn-700.woff2') format('woff2')}\n"
    "@font-face{font-family:'Vazirmatn';font-weight:800;font-display:swap;src:url('../assets/fonts/vazirmatn-800.woff2') format('woff2')}\n\n"
    + TOKENS + "\n", encoding='utf-8')
(DS / 'components.css').write_text("/* Yademan components (requires design-tokens.css) */\n" + CSS + "\n", encoding='utf-8')

COLORS = {
  "bg": {"bg0": "#05070F", "bg1": "#0B0F1E", "bg2": "#131A30", "bg3": "#1A2240"},
  "gold": {"gold1": "#F7DE8B", "gold2": "#DDAF4B", "gold3": "#9C741F", "solid": "#D9B45B",
           "gradient": "linear-gradient(135deg,#F7DE8B 0%,#DDAF4B 48%,#A87F22 100%)"},
  "ink": {"primary": "#F7F4EC", "secondary": "#B9BED2", "muted": "#7E8399"},
  "category": {"installment": "#E8B84B", "check": "#9D8CFF", "meeting": "#4CC3FF",
               "medicine": "#FF7A9E", "shopping": "#3ED598", "note": "#FFB84D"},
  "status": {"danger": "#FF6B81", "success": "#3ED598", "warning": "#FFB84D", "info": "#4CC3FF"},
  "surface": {"surface": "rgba(255,255,255,0.055)", "surface2": "rgba(255,255,255,0.09)",
              "stroke": "rgba(255,255,255,0.11)"},
  "radius": {"lg": 26, "md": 20, "sm": 14},
  "font": {"family": "Vazirmatn", "weights": [400, 500, 700, 800]}
}
(DS / 'colors.json').write_text(json.dumps(COLORS, ensure_ascii=False, indent=2), encoding='utf-8')

def hex8(h):
    h = h.lstrip('#'); return 'FF' + h.upper()
axml = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
for group in [COLORS['bg'], COLORS['gold'], COLORS['ink'], COLORS['category'], COLORS['status']]:
    for k, v in group.items():
        if isinstance(v, str) and v.startswith('#'):
            axml.append(f'    <color name="yademan_{k}">#{hex8(v)}</color>')
axml.append('</resources>')
(DS / 'colors-android.xml').write_text('\n'.join(axml) + '\n', encoding='utf-8')

swift = ['import SwiftUI', '', 'extension Color {']
for group, prefix in [(COLORS['bg'], 'bg'), (COLORS['gold'], 'gold'), (COLORS['ink'], 'ink'),
                      (COLORS['category'], 'cat'), (COLORS['status'], 'st')]:
    for k, v in group.items():
        if isinstance(v, str) and v.startswith('#'):
            h = v.lstrip('#'); r, g, b = int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)
            swift.append(f'    static let yademan{prefix.capitalize()}{k.capitalize()} = Color(red: {r}/255, green: {g}/255, blue: {b}/255)')
swift.append('}')
(DS / 'Colors-swift.swift').write_text('\n'.join(swift) + '\n', encoding='utf-8')

kt = ['import androidx.compose.ui.graphics.Color', '', 'object YademanColors {']
for group, prefix in [(COLORS['bg'], 'Bg'), (COLORS['gold'], 'Gold'), (COLORS['ink'], 'Ink'),
                      (COLORS['category'], ''), (COLORS['status'], '')]:
    for k, v in group.items():
        if isinstance(v, str) and v.startswith('#'):
            kt.append(f'    val {prefix}{k.capitalize()} = Color(0xFF{v.lstrip("#").upper()})')
kt.append('}')
(DS / 'YademanColors-compose.kt').write_text('\n'.join(kt) + '\n', encoding='utf-8')

for name, path in P.items():
    (DS / 'icons' / f'{name}.svg').write_text(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">{path}</svg>',
        encoding='utf-8')
print('design-system written')

# ---------------- screenshots ----------------
from playwright.sync_api import sync_playwright
import tempfile
tmp = Path(tempfile.mkdtemp())
with sync_playwright() as p:
    b = p.chromium.launch()
    for sid, title, _, _ in SCREENS:
        uri = (ROOT / 'screens' / f'{sid}.html').as_uri()
        pg = b.new_page(viewport={'width': 390, 'height': 844}, device_scale_factor=3,
                        is_mobile=True, has_touch=True, color_scheme='dark')
        pg.goto(uri)
        pg.evaluate('document.fonts.ready')
        pg.wait_for_timeout(500)
        pg.locator('.screen').screenshot(path=str(ROOT / 'screenshots' / f'{sid}.png'))
        ov = pg.evaluate('''() => { const s = document.querySelector('.screen');
            const r = s.getBoundingClientRect(); let max = 0;
            s.querySelectorAll('.content > *').forEach(e => { const b = e.getBoundingClientRect();
              if (!e.className.includes || (!String(e.className).includes('savewrap') && !String(e.className).includes('cartbar'))) max = Math.max(max, b.bottom - r.top); });
            const bar = s.querySelector('.savewrap,.cartbar');
            const bartop = bar ? Math.round(bar.getBoundingClientRect().top - r.top) : 0;
            return Math.round(max) + '/' + bartop; }''')
        print(sid, 'flow-bottom/bar-top =', ov)
        pg.close()
        pg2 = b.new_page(viewport={'width': 390, 'height': 844}, device_scale_factor=2,
                         is_mobile=True, has_touch=True, color_scheme='dark')
        pg2.goto(uri)
        pg2.evaluate('document.fonts.ready')
        pg2.wait_for_timeout(500)
        pg2.locator('.screen').screenshot(path=str(tmp / f'{sid}.jpg'), type='jpeg', quality=82)
        pg2.close()
    b.close()

# ---------------- showcase ----------------
cards = ''
for sid, title, _, _ in SCREENS:
    jpg = b64(tmp / f'{sid}.jpg')
    cards += f'''<figure class="ph"><img src="data:image/jpeg;base64,{jpg}" alt="{title}">
<figcaption><span>{title}</span><a href="screens/{sid}.html" download>دانلود صفحه</a></figcaption></figure>'''
swatches = ''
for label, group in [('پس‌زمینه', COLORS['bg']), ('طلایی', COLORS['gold']), ('متن', COLORS['ink']),
                     ('دسته‌بندی', COLORS['category']), ('وضعیت', COLORS['status'])]:
    items = ''.join([f'<div class="sw"><i style="background:{v}"></i><div><b>{k}</b><span dir="ltr">{v}</span></div></div>'
                     for k, v in group.items() if isinstance(v, str) and v.startswith('#')])
    swatches += f'<h3>{label}</h3><div class="swrow">{items}</div>'
(ROOT / 'showcase.html').write_text(f'''<!DOCTYPE html><html lang="fa" dir="rtl"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><title>یادمان — گالری طرح‌ها</title>
<style>{TOKENS}{FONT_CSS}
*{{margin:0;box-sizing:border-box}}body{{background:#04060D;color:var(--ink-1);font-family:var(--font);padding:48px 24px}}
.wrap{{max-width:1200px;margin:0 auto}}.brand{{display:flex;align-items:center;gap:14px}}
.logo{{width:58px;height:58px;border-radius:19px;background:var(--gold-grad);display:grid;place-items:center;color:#241A05;box-shadow:var(--shadow-gold)}}
h1{{font-size:30px;font-weight:800}}p.sub{{color:var(--ink-2);margin-top:8px;line-height:2}}
.grid{{display:grid;grid-template-columns:repeat(auto-fill,minmax(230px,1fr));gap:26px;margin-top:36px}}
.ph{{background:rgba(255,255,255,.03);border:1px solid var(--stroke);border-radius:26px;padding:16px}}
.ph img{{width:100%;border-radius:22px;display:block;box-shadow:0 24px 60px rgba(0,0,0,.6)}}
.ph figcaption{{display:flex;justify-content:space-between;align-items:center;margin-top:12px;font-size:13px;font-weight:700}}
.ph a{{font-size:11.5px;color:var(--gold-1);text-decoration:none;border:1px solid rgba(221,175,75,.4);padding:6px 13px;border-radius:10px}}
h2{{font-size:22px;margin:54px 0 6px}}h3{{font-size:15px;margin:22px 0 10px;color:var(--gold-1)}}
.swrow{{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:10px}}
.sw{{display:flex;gap:10px;align-items:center;background:rgba(255,255,255,.04);border:1px solid var(--stroke);border-radius:14px;padding:9px 12px}}
.sw i{{width:34px;height:34px;border-radius:11px;flex:none;border:1px solid rgba(255,255,255,.15)}}
.sw b{{display:block;font-size:12px}}.sw span{{font-size:11px;color:var(--ink-2)}}
.dl{{display:inline-block;margin-top:30px;background:var(--gold-grad);color:#241A05;font-weight:800;padding:15px 34px;border-radius:16px;text-decoration:none;box-shadow:var(--shadow-gold)}}
.note{{margin-top:14px;color:var(--ink-2);font-size:13px;line-height:2}}
</style></head><body><div class="wrap">
<div class="brand"><div class="logo"><svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">{P['bell']}</svg></div>
<div><h1>یادمان <span style="font-size:15px;color:var(--gold-1)">YADEMAN</span></h1><p class="sub">کیت رابط کاربری لاکچری اپ یادآور روزانه — فارسی و راست‌چین • تم تیره + طلایی • ۸ صفحه موبایل (۳۹۰×۸۴۴)</p></div></div>
<div class="grid">{cards}</div>
<h2>پالت رنگی</h2><p class="sub">تمام کدهای رنگ داخل فایل زیپ هم هستند: colors.json ،colors-android.xml ،Colors-swift.swift ،YademanColors-compose.kt و design-tokens.css</p>
{swatches}
<div><span class="dl">فایل زیپ کامل داخل همین پوشه است: yademan-ui-kit.zip</span></div>
<p class="note">فونت: وزیرمتن (اوزان ۴۰۰/۵۰۰/۷۰۰/۸۰۰) — آیکن‌ها: استروک ۱.۸ با گوشه‌های گرد — شعاع گوشه‌ها: ۲۶ / ۲۰ / ۱۴</p>
</div></body></html>''', encoding='utf-8')
print('showcase written')

# ---------------- readme ----------------
(ROOT / 'README.md').write_text('''# یادمان (Yademan) — کیت رابط کاربری اپ یادآور روزانه

کیت لاکچری، مدرن و فارسی (راست‌چین) برای اپلیکیشن یادآوری کارهای روزمره:
اقساط، چک، قرارها، دارو، لیست خرید و دفترچه یادداشت.

## محتویات
| مسیر | توضیح |
|---|---|
| `screenshots/` | ۸ اسکرین‌شات PNG با کیفیت بالا (۳۹۰×۸۴۴ با مقیاس ۳x) |
| `screens/` | سورس هر ۸ صفحه (HTML تک‌فایل، آفلاین، فونت امبد شده) |
| `showcase.html` | گالری همه صفحات + پالت رنگی (تک‌فایل آفلاین) |
| `design-system/design-tokens.css` | متغیرهای رنگ، فونت، شعاع گوشه |
| `design-system/components.css` | استایل همه کامپوننت‌ها |
| `design-system/colors.json` | توکن‌های رنگ (برای هر پلتفرم) |
| `design-system/colors-android.xml` | رنگ‌ها برای Android |
| `design-system/Colors-swift.swift` | رنگ‌ها برای iOS |
| `design-system/YademanColors-compose.kt` | رنگ‌ها برای Jetpack Compose |
| `design-system/icons/` | ۲۳ آیکن SVG استروک |
| `assets/fonts/` | فونت وزیرمتن woff2 (اوزان ۴۰۰/۵۰۰/۷۰۰/۸۰۰ + لاتین) |

## صفحات
۱. خانه و داشبورد (`01-home`) — ۲. اقساط و چک‌ها (`02-bills`) — ۳. قرارها و تقویم (`03-calendar`) — ۴. یادآوری دارو (`04-medicine`) — ۵. لیست خرید (`05-shopping`) — ۶. دفترچه یادداشت (`06-notes`) — ۷. یادآوری جدید (`07-add`) — ۸. اعلان‌ها و تنظیمات (`08-notifications`)

## پالت رنگی
| نقش | نام | کد |
|---|---|---|
| پس‌زمینه تیره | bg-1 | `#0B0F1E` |
| پس‌زمینه عمیق | bg-0 | `#05070F` |
| طلایی روشن | gold-1 | `#F7DE8B` |
| طلایی اصلی | gold-2 | `#DDAF4B` |
| طلایی تیره | gold-3 | `#9C741F` |
| گرادیان طلایی | — | `linear-gradient(135deg,#F7DE8B,#DDAF4B 48%,#A87F22)` |
| متن اصلی | ink-1 | `#F7F4EC` |
| متن ثانویه | ink-2 | `#B9BED2` |
| متن کم‌رنگ | ink-3 | `#7E8399` |
| اقساط | installment | `#E8B84B` |
| چک | check | `#9D8CFF` |
| قرار | meeting | `#4CC3FF` |
| دارو | medicine | `#FF7A9E` |
| خرید | shopping | `#3ED598` |
| یادداشت | note | `#FFB84D` |
| خطر | danger | `#FF6B81` |

## استفاده سریع (وب)
```html
<link rel="stylesheet" href="design-system/design-tokens.css">
<link rel="stylesheet" href="design-system/components.css">
```
سپس از کلاس‌ها (`hero` ،`task` ،`btn-gold` ،`bottomnav` و…) استفاده کنید.

## مشخصات طراحی
- ابعاد موبایل: ۳۹۰×۸۴۴ • فونت: وزیرمتن • جهت: RTL • آیکن: استروک ۱.۸ گرد
''', encoding='utf-8')

# ---------------- zip ----------------
zpath = Path('/home/user/yademan-ui-kit.zip')
if zpath.exists(): zpath.unlink()
with zipfile.ZipFile(zpath, 'w', zipfile.ZIP_DEFLATED) as z:
    for p in sorted(ROOT.rglob('*')):
        if p.is_file() and p.suffix != '.py' and '__pycache__' not in str(p):
            z.write(p, 'yademan/' + str(p.relative_to(ROOT)))
print('zip:', zpath, round(zpath.stat().st_size / 1024 / 1024, 2), 'MB')
print('DONE')
