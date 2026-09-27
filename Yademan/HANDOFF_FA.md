# گزارش تحویل پروژه «یادمان» — v1.2.0

> تغییرات v1.2 (سرعت، اشتراک‌ها، تعداد دفعات تکرار) در **CHANGELOG_v1.2_FA.md** آمده است.

> تغییرات v1.1 نسبت به v1.0 در **CHANGELOG_v1.1_FA.md** آمده است.

> این سند برای توسعه‌دهنده یا هوش مصنوعی بعدی نوشته شده تا بدون حدس‌زدن بفهمد پروژه در چه وضعی است، چه چیزی عوض شده و قراردادها چیست.

---

## ۱) خلاصه
نسخه‌های قبلی (phase1..5 و Complete) **بیلد نمی‌شدند** و بخش‌های اصلی‌شان ظاهری بود. این نسخه روی پایه خوب قبلی (تم، رنگ‌ها، کامپوننت‌ها، JalaliConverter، PersianDigits) ساخته شده و **لایه داده، سینک، نوتیفیکیشن، ۸ صفحه اصلی + صفحات جانبی و ناوبری از نو نوشته شده‌اند**.

- پکیج: `com.hossein.yademan` | minSdk 24 | compile/target 35
- Kotlin 2.0.21 + Compose BOM 2024.12.01 + Material3 (تم سفارشی)
- AGP 8.7.3 | Gradle 8.9 | JDK 17
- Room 2.6.1 (KSP) | WorkManager 2.10 | OkHttp 4.12 + Gson | security-crypto | core-splashscreen
- DI دستی: `ServiceLocator` (object singleton)
- بدون `java.time` → بدون نیاز به desugaring روی API 24

⚠️ **مهم:** این پروژه در محیطی ساخته شد که Android SDK نداشت؛ پس `assembleDebug` واقعاً اجرا نشده. کد با بررسی ایستا (تطابق پارامترها، وجود همه ارجاع‌ها، تعادل پرانتزها، اسمارت‌کست‌ها) چک شده، ولی اولین بیلد ممکن است یکی‌دو خطای جزئی بدهد. در آن صورت فقط متن خطا را به AI بدهید.

---

## ۲) باگ‌های نسخه قبلی که رفع شد
| مشکل | وضعیت قبل | اصلاح |
|---|---|---|
| `libs.versions.toml` | در Complete نبود؛ در phase5 کلید `ksp` تکراری و retrofit زیر `[plugins]` | بازنویسی کامل |
| provider در Manifest | `androidx.work.impl.WorkManagerInitializer` (وجود ندارد → کرش) | حذف شد؛ WorkManager با androidx.startup خودکار |
| فونت‌ها | فایل‌های ۰ بایتی | وزیرمتن واقعی (۴۰۰/۵۰۰/۷۰۰/۸۰۰) از فونت متغیر ساخته شد (مجوز OFL) |
| زمان آلارم | `الان + offset` (due_at نادیده) | `وقوع بعدی − offset` |
| لغو آلارم | Intent بدون action → هیچ‌وقت لغو نمی‌شد | data URI یکتا + FLAG_NO_CREATE |
| `reminderId` در Receiver | تعریف‌نشده (خطای کامپایل) | بازنویسی Receiver با goAsync |
| `j2d` private ولی بیرون استفاده شده | خطای کامپایل | public شد |
| NavGraph قدیمی | پارامترهای ناموجود پاس می‌داد | NavGraph واحد جدید |
| state در صفحات | `mutableStateOf` بدون remember، بدون Flow | ViewModel + StateFlow از Room Flow |
| AddReminder | ذخیره نمی‌کرد | فرم کامل + ذخیره + آلارم |
| حذف | حذف واقعی → هرگز سینک نمی‌شد | soft-delete با `local_deleted` |
| version | موقع سینک زیاد می‌شد | موقع تغییر محلی زیاد می‌شود |
| pull | تغییرات محلی را رونویسی می‌کرد و زنجیره سینک می‌ساخت | قانون version + بدون سینک مجدد |
| last_sync | ساعت گوشی | مقدار برگشتی سرور |
| لاگین/یادداشت‌ها/تقویم | از هیچ جا قابل دسترسی نبودند | همه در ناوبری |

---

## ۳) ساختار فایل‌ها
```
app/src/main/java/com/hossein/yademan/
├── MainActivity.kt            اسپلش، edge-to-edge، RTL، باز کردن اعلان‌ها از نوتیفیکیشن
├── YademanApp.kt              init: DI، کانال‌ها، Seed، بازچینش آلارم، Workerها
├── ServiceLocator.kt          DI دستی + appScope
├── data/
│   ├── Reminder.kt            Entity + SyncStatus
│   ├── Category.kt, NotificationLog.kt, SyncMeta.kt
│   ├── ReminderDao.kt, CategoryDao.kt, NotificationLogDao.kt, SyncMetaDao.kt  (Flow + suspend)
│   ├── AppDatabase.kt         نسخه ۲، fallbackToDestructiveMigration
│   ├── ReminderMeta.kt        ReminderType + قرارداد meta_json (پایین‌تر)
│   └── Seed.kt                داده دمو با گارد seeded_v1
├── repository/
│   ├── ReminderRepository.kt  قوانین version/sync_status/soft-delete + آلارم + سینک debounced
│   ├── CategoryRepository.kt, NotificationRepository.kt
│   └── SyncRepository.kt      key-value روی sync_meta + MetaKeys
├── services/
│   ├── ApiClient.kt           OkHttp، همه endpointها، ApiException
│   ├── JsonExt.kt             خواندن امن JSON (رشته/عدد/JSON-درون-رشته)
│   ├── RecordMapper.kt        Entity ↔ رکورد سینک + نگاشت record_type
│   ├── AuthManager.kt         EncryptedSharedPreferences (+fallback اگر Keystore خراب بود)
│   ├── SyncManager.kt         push → conflicts → pull، با Mutex
│   ├── SyncWorker.kt          دوره‌ای ۱۵ دقیقه + debounced + شرط شبکه
│   ├── AlarmScheduler.kt      AlarmManager دقیق/غیردقیق، follow-up، تست
│   ├── NotificationHelper.kt  کانال‌ها، ساخت نوتیفیکیشن با دکمه «انجام شد»
│   ├── ReminderReceiver.kt    زنگ آلارم + دکمه انجام شد (goAsync)
│   ├── BootReceiver.kt        ریبوت/آپدیت/تغییر ساعت/تغییر مجوز
│   └── DailyAlarmRescheduleWorker.kt  هر ۱۲ ساعت بازچینش
├── ui/theme/                  Color.kt, Type.kt, Theme.kt (از نسخه قبل، سالم)
├── ui/components/             AppCard, IconTile, Chip, SegmentedRow, ToggleRow(GoldSwitch),
│                              ProgressRing, LinearProgress, BottomNavBar(+برچسب), TimelineDot,
│                              EmptyState, Icons, Buttons, ScreenScaffold, YTextField, CheckCircle, Common
├── ui/screens/
│   ├── UiCommon.kt            TimeTicker، BillsLogic، reminderSubtitle
│   ├── HomeScreen.kt          ۱) خانه
│   ├── BillsScreen.kt         ۲) اقساط و چک‌ها
│   ├── CalendarScreen.kt      ۳) قرارها و تقویم
│   ├── MedicineScreen.kt      ۴) داروها
│   ├── ShoppingScreen.kt      ۵) لیست خرید (تب)
│   ├── NotesScreen.kt         ۶) یادداشت‌ها + NoteEditScreen.kt (ویرایشگر)
│   ├── AddReminderScreen.kt   ۷) یادآوری جدید
│   ├── NotificationsScreen.kt ۸) اعلان‌ها + تنظیمات
│   ├── RemindersScreen.kt     تب «یادآوری‌ها» (دسترسی به همه بخش‌ها + لیست امروز/آینده/انجام‌شده)
│   ├── ProfileScreen.kt       تب «من» (حساب، سینک، سلامت یادآورها، اعلان آزمایشی)
│   └── LoginScreen.kt         ورود/ثبت‌نام
├── ui/navigation/NavGraph.kt
└── utils/ JalaliConverter.kt, PersianDigits.kt, DateUtils.kt, Recurrence.kt
```

---

## ۴) قرارداد داده (مهم‌ترین بخش برای AI بعدی)

### ۴-۱) همه انواع در جدول `reminders`
نوع در `meta_json.type` است: `installment | check | meeting | medicine | shopping | note | other`.
دسته پیش‌فرض: `cat_installment, cat_check, cat_meeting, cat_medicine, cat_shopping, cat_note`.

### ۴-۲) ساختار `meta_json`
```jsonc
// همه
{ "type": "...", "done_dates": ["2026-09-27", ...] }   // فقط برای تکراری‌ها؛ یک‌بارها از is_done
// installment / check
{ "bank": "بانک ملت", "check_number": "۴۸۲۱۰۶" }
// meeting / other
{ "location": "دفتر مرکزی" }
// medicine
{ "dose": "۵۰۰ میلی‌گرم • بعد از صبحانه" }   // «مصرف امروز» = done_dates شامل امروز
// shopping   (قیمت در amount_toman)
{ "group": "لبنیات", "qty": "۲ بطری" }
// note       (متن آزاد در ستون note)
{ "pinned": true, "items": [ { "text": "...", "done": false } ] }
```
- کلید روز = تاریخ **میلادی محلی** `yyyy-MM-dd` (نه شمسی) تا با منطقه زمانی پایدار باشد.
- «انجام شد» برای تکراری فقط همان روز را علامت می‌زند (`ReminderMeta.withDoneOn`).
- برای قسط ماهانه، «پرداخت» وقوعِ همان ماه را در done_dates ثبت می‌کند.

### ۴-۳) موتور تکرار (`utils/Recurrence.kt`)
- `daily` هر N روز | `weekly` هر N هفته (با `weekdays_mask`، بیت ۰=شنبه) | `monthly` هر N **ماه شمسی** همان روز (اگر ماه کوتاه‌تر بود آخرین روز) | `yearly` شمسی | `none`.
- ساعت وقوع = ساعت/دقیقه `due_at` به وقت محلی. `end_at` رعایت می‌شود.
- `occurrenceOnDay(r, day)` و `nextOccurrence(r, from, skipDone)` مبنای همه صفحات و آلارم‌ها هستند.

### ۴-۴) قوانین سینک در Repository
- ایجاد: `version=1`, `local_new`
- ویرایش: `version+1`, `updated_at=now`, `local_changed` (اگر هنوز local_new است همان می‌ماند)
- حذف: اگر local_new → حذف واقعی؛ وگرنه `is_deleted=1, local_deleted, version+1`
- بعد از هر تغییر: `AlarmScheduler.schedule` + `SyncWorker.requestSoon` (debounce ۴ ثانیه، REPLACE)

---

## ۵) سینک با بک‌اند (بدون تغییر سرور)
Base: `https://kafezare.ir/wp-json/lifeplanner/v1/` — همه POST، توکن داخل بدنه.

**نگاشت record_type:** یادداشت→`note` | قرار→`event` | قلم خرید→`task` | قسط/چک/دارو/سایر→`reminder` | دسته→`category`.
`record_data` = **کل ردیف** با نام ستون‌های snake_case (هیچ فیلدی گم نمی‌شود). نوع دقیق در `record_data.meta_json.type` حفظ می‌شود.

**هر رکورد push:**
```json
{ "id": "...", "record_type": "reminder", "record_data": { ...همه ستون‌ها... },
  "version": 3, "updated_at": "2026-09-27T06:00:00Z", "device_id": "android-...", "is_deleted": false }
```
**چرخه:** push (dirty + deleted) → رسیدگی به conflicts → pull از last_sync → ذخیره `last_sync` برگشتی سرور → بازچینش آلارم‌ها.
**تعارض:** version بزرگ‌تر برنده؛ برابر → سرور. درخواست resolve: `{token, conflicts:[{id, resolution:"client|server", winner, record?}]}`. اگر سرور نسخه خودش را همراه تعارض نفرستد، یک pull کامل انجام می‌شود.
**مقاومت در برابر فرمت سرور:** پارسر هم `id` و هم `record_id`، هم `is_deleted` و هم `deleted`، و `record_data` به صورت شیء یا رشته JSON را می‌پذیرد. conflicts می‌تواند آرایه شیء، آرایه id یا شیء باشد.
**اولین last_sync:** `"1970-01-01 00:00:00"` (ثابت `SyncManager.INITIAL_LAST_SYNC`).

> ⚠️ **فرض‌هایی که باید با کد پلاگین وردپرس تطبیق داده شوند:** نام دقیق فیلدهای رکورد در push، فرمت conflicts و بدنه resolve، و فرمت last_sync. اگر فرق داشت فقط `RecordMapper.kt` و `SyncManager.kt` تغییر می‌کنند.

**WorkManager:** دوره‌ای ۱۵ دقیقه + هنگام استارت + debounced بعد از تغییر، همه با `NetworkType.CONNECTED` → در حالت هواپیما منتظر می‌ماند و بعد از اتصال خودکار سینک می‌کند. خطای 401/403 → توقف تا ورود دوباره.

---

## ۶) نوتیفیکیشن (باز / بسته / صفحه خاموش / Doze)
1. `AlarmScheduler.schedule`: فقط **وقوع بعدی** هر یادآور؛ زمان زنگ = وقوع − `alarm_offset_minutes` (اگر گذشته ولی وقوع نرسیده، فوری).
2. `setExactAndAllowWhileIdle` (در Doze هم اجرا می‌شود)؛ بدون مجوز دقیق → `setAndAllowWhileIdle`.
3. مجوزها: `SCHEDULE_EXACT_ALARM` با `maxSdkVersion=32` + `USE_EXACT_ALARM` برای ۱۳+ (الگوی رسمی گوگل). ⚠️ برای انتشار در Google Play، USE_EXACT_ALARM فقط برای اپ‌هایی که کار اصلی‌شان آلارم/یادآور است مجاز است؛ اگر رد شد، آن خط را حذف و maxSdkVersion را از SCHEDULE_EXACT_ALARM بردارید.
4. `ReminderReceiver` با `goAsync`: نمایش + ثبت در `notification_log` + ثبت `fired_{id}` (جلوگیری از اعلان تکراری همان وقوع) + زمان‌بندی وقوع بعدی.
5. **یادآوری هوشمند:** اگر تا ۱۰ دقیقه بعد از موعد انجام نشد، دوباره اعلان می‌دهد.
6. **صدا خاموش / مزاحم نشو (۲۳ تا ۷):** اعلان روی کانال بی‌صدا.
7. دکمه «انجام شد / مصرف شد / پرداخت شد» روی خود نوتیفیکیشن.
8. `BootReceiver`: BOOT_COMPLETED، MY_PACKAGE_REPLACED، TIME_SET، TIMEZONE_CHANGED، تغییر مجوز آلارم دقیق، QUICKBOOT (exported=true چون همه protected broadcast هستند).
9. `DailyAlarmRescheduleWorker` هر ۱۲ ساعت + بازچینش در هر استارت و بعد از هر سینک.
10. مجوز `POST_NOTIFICATIONS` در اولین اجرا پرسیده می‌شود. تب «من» → «سلامت یادآورها» وضعیت اعلان، آلارم دقیق و **معافیت باتری** را نشان می‌دهد و دکمه اصلاح دارد (برای شیائومی/سامسونگ/هواوی حیاتی است).
11. دکمه «ارسال اعلان آزمایشی (۱۰ ثانیه دیگر)» برای تست با اپ بسته.

کانال‌ها: `yademan_reminders` (HIGH، با صدا) و `yademan_reminders_silent`.

---

## ۷) صفحات و ناوبری
نوار پایین: خانه | یادآوری‌ها | FAB طلایی (+) | خرید | من — تب فعال: پیله طلایی + برچسب طلایی.

- **خانه:** آواتار + «سلام، {نام}» + تاریخ شمسی + زنگوله با نقطه unread؛ حلقه پیشرفت امروز؛ «Z سررسید مهم» (قسط/چک پرداخت‌نشده تا ۷ روز + کارهای اولویت‌بالا)؛ هاب ۱۵ خدمت با بج زنده؛ «کارهای امروز» با چک (داروی روزانه → done_dates امروز، بقیه → is_done).
- **اقساط و چک‌ها:** مانده این ماه، درصد پرداخت، سررسید نزدیک، تب‌ها، وضعیت (N روز مانده/فردا/امروز/گذشته/پرداخت شد)، «پرداخت» و «برگرداندن»، فیلتر پرداخت‌نشده‌ها، نگه‌داشتن = حذف.
- **تقویم:** نوار هفته شنبه تا جمعه با نقطه‌های رنگی، ورق زدن هفته، «برو به امروز»، تایم‌لاین با خط اتصال، + با تاریخ روز انتخابی.
- **داروها:** پایداری ۷ روز با متن (عالیه/خوبه/ادامه بده)، روزهای متوالی، ۷ دایره هفته، دکمه‌ها: «مصرف شد ✓» خاکستری (برگرداندن) / «مصرف شد؟» طلایی / «مصرف کردم» (ثبت مصرف زودتر از موعد).
- **خرید:** پیشرفت، افزودن قلم («شیر ۴۵۰۰۰» → قیمت خودکار)، فیلتر گروه‌ها، تیک، جمع سبد، «اتمام خرید»، سطل = حذف انجام‌شده‌ها.
- **یادداشت‌ها:** نوار رنگی ۸dp، سنجاق، چک‌لیست قابل تیک، جست‌وجو، ویرایشگر کامل (رنگ، سنجاق، موارد).
- **یادآوری جدید:** گرید ۶ نوع، عنوان، مبلغ (قسط/چک/خرید)، فیلد جزئیات (بانک/دوز/مکان/گروه)، stepper روز/ساعت/دقیقه±۵، تکرار، یادآوری قبل، اولویت. پیش‌فرض هوشمند هر نوع.
- **اعلان‌ها:** نقطه طلایی unread، زمان نسبی، لمس = خوانده، «خواندن همه»، ۳ سوییچ ذخیره در sync_meta.

همه اعداد با `fa()` / `PersianDigits` فارسی و همه تاریخ‌ها شمسی‌اند.

---

## ۸) Seed اولین اجرا
۶ دسته | ۲ قسط ماهانه + ۱ چک | ۳ قرار | ۵ داروی روزانه (فقط متفورمین امروز مصرف شده؛ سابقه ۶ روز گذشته برای آمار) | ۱۲ قلم خرید در ۶ گروه (۵ تا خریده) | ۶ یادداشت (۲ سنجاق) | ۱ یادآور سایر. نام نمایشی پیش‌فرض «حسین» (از تب «من» قابل تغییر).

---

## ۹) بیلد و اجرا
1. پوشه `Yademan` را در Android Studio (Ladybug یا جدیدتر) باز کنید. JDK 17.
2. Gradle Sync. فایل `gradle-wrapper.jar` داخل بسته نیست (باینری است)؛ Android Studio با `gradle-wrapper.properties` خودش Gradle 8.9 را می‌گیرد. برای خط فرمان یک‌بار اجرا کنید: `gradle wrapper --gradle-version 8.9`.
3. `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## ۱۰) چک‌لیست تست
- [ ] اجرا روی API 34 بدون کرش؛ seed نمایش داده شود
- [ ] تب «من» → اعلان آزمایشی → اپ را از Recents ببندید → بعد از ۱۰ ثانیه اعلان بیاید
- [ ] یادآور با ساعت ۲ دقیقه بعد و «یادآوری قبل» ۱۵ دقیقه → فوراً یا در موعد اعلان (چون offset گذشته)
- [ ] ریبوت گوشی → آلارم‌ها سر جایشان
- [ ] ورود/ثبت‌نام → دیدن رکوردها در wp-admin
- [ ] حالت هواپیما → تغییر → قطع هواپیما → سینک خودکار
- [ ] مقایسه پهلو‌به‌پهلو با screenshots مخزن

## ۱۱) محدودیت‌ها و کارهای بعدی
- **تطبیق پیکسلی با مخزن:** به GitHub دسترسی نبود؛ ظاهر بر پایه توکن‌های طراحی و کامپوننت‌هایی است که نسخه قبلی از `home.html`/`common.css` مخزن استخراج کرده بود. ۸ صفحه باید با اسکرین‌شات‌ها مقایسه و فاصله‌ها ریزتنظیم شوند.
- **خانه:** مخزن (طبق کد قبلی) هاب ۱۵ خدمت دارد و پرامپ «۴ کارت دسترسی سریع». طبق قانون «مخزن برنده است» هاب نگه داشته شد. اگر اسکرین‌شات ۴ کارت نشان می‌دهد، `hubDefs` در `HomeScreen.kt` را به ۴ آیتم کاهش دهید.
- کاشی‌های هاب بدون صفحه اختصاصی (قبوض، بیمه، تولد و …) فرم «یادآوری جدید» را با نوع «سایر» و عنوان پیش‌پرشده باز می‌کنند.
- `USE_EXACT_ALARM` در Google Play فقط برای اپ‌های ساعت/تقویم/یادآور مجاز است؛ اگر رد شد حذفش کنید (SCHEDULE_EXACT_ALARM + درخواست کاربر کافی است).
- `EncryptedSharedPreferences` در نسخه‌های جدید deprecated است ولی کار می‌کند.
- ویرایش یادآورهای موجود (غیر یادداشت) هنوز صفحه ندارد؛ فقط انجام/پرداخت/حذف (نگه‌داشتن روی کارت).
- `sms_reminder` / `email_reminder` ذخیره و سینک می‌شوند ولی ارسالشان سمت سرور است.
