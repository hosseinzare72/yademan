import asyncio, pathlib
from playwright.async_api import async_playwright

ROOT = pathlib.Path(__file__).parent
OUT = ROOT / "shots"
OUT.mkdir(exist_ok=True)

PAGES = [
    ("home",          "01-home"),
    ("installments",  "02-installments"),
    ("checks",        "03-checks"),
    ("medicine",      "04-medicine"),
    ("new-reminder",  "05-new-reminder"),
    ("shopping",      "06-shopping"),
    ("notes",         "07-notes"),
    ("notifications", "08-notifications"),
    ("paywall",       "09-paywall"),
    ("calendar",      "10-calendar"),
    ("birthdays",     "11-birthdays"),
    ("travel",        "12-travel"),
    ("palette",       "13-palette"),
]

async def main():
    async with async_playwright() as p:
        b = await p.chromium.launch(args=["--force-color-profile=srgb","--hide-scrollbars"])
        pg = await b.new_page(viewport={"width":390,"height":844}, device_scale_factor=3)
        for src, name in PAGES:
            url = (ROOT / "screens" / f"{src}.html").as_uri()
            await pg.goto(url)
            await pg.evaluate("document.fonts.ready.then(()=>true)")
            await pg.wait_for_timeout(300)
            await pg.screenshot(path=str(OUT / f"{name}.png"))
            print("shot", name)
        await b.close()

asyncio.run(main())
