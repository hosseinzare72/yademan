# یادمان (Yademan) — کیت رابط کاربری اپ یادآور روزانه

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
