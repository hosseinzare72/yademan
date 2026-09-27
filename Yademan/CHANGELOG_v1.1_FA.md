# یادمان v1.1.0 — گزارش Pre-build Static Analysis

محیط بررسی Android SDK و کامپایلر Kotlin نداشت؛ پس همه‌چیز **ایستا** چک شد: همه ۶۶ فایل Kotlin خط‌به‌خط،
تطابق نسخه‌ها، importها، امضاهای Dao/Repository، پارامترهای NavGraph، وجود همه ارجاع‌ها (YIcons/YColors/YText/YBrush/YRadius/MetaKeys/R.*)،
Manifest، ServiceLocator، تعادل پرانتزها و اعتبار XML/فونت/PNG.

## ۱) خطاهای کامپایل (قطعی) — رفع شد
| # | فایل | مشکل | اصلاح |
|---|---|---|---|
| 1 | `ui/navigation/NavGraph.kt` | `import androidx.navigation.graph.findStartDestination` وجود ندارد | `import androidx.navigation.NavGraph.Companion.findStartDestination` |
| 2 | `BillsScreen`, `RemindersScreen`, `ShoppingScreen`, `NotesScreen`, `NoteEditScreen` | `rememberSaveable` استفاده شده ولی import نشده (در `androidx.compose.runtime.*` نیست) | `import androidx.compose.runtime.saveable.rememberSaveable` |

## ۲) ریسک کامپایل (دفاعی) — رفع شد
| # | فایل | مشکل | اصلاح |
|---|---|---|---|
| 3 | `ui/screens/UiCommon.kt` | `private fun Int.fa()` هم‌نام با `utils.fa` در همان پکیجِ صفحات (سایه‌اندازی روی star-import) | حذف و `import com.hossein.yademan.utils.fa` |
| 4 | `BillsScreen.kt` | متغیر محلی `items` هم‌نام تابع `items()` در LazyColumn | تغییر نام به `visibleBills` |
| 5 | `data/Seed.kt` | پارامتر lambda با نام صریح `it` | تغییر نام به `item` |

## ۳) باگ‌های منطقی/زمان اجرا — رفع شد
| # | فایل | مشکل | اصلاح |
|---|---|---|---|
| 6 | `services/SyncManager.kt` | تعارض (conflict) روی **دسته‌ها** به‌عنوان Reminder ذخیره می‌شد → رکورد خراب در لیست یادآورها | رسیدگی جدا به دسته‌ها با `categoryFrom`/`categoryRecord` |
| 7 | `ui/components/YTextField.kt` | `KeyboardActions(onAny=…)` همیشه ست می‌شد → دکمه Next کیبورد در لاگین کار نمی‌کرد | فقط وقتی `onImeAction` داریم؛ وگرنه `KeyboardActions.Default` |
| 8 | `AddReminderScreen.kt` | دو بار لمس «ذخیره» = دو یادآور تکراری + دو popBackStack | گارد `saving` + غیرفعال شدن دکمه |
| 9 | `NoteEditScreen.kt` | همان مشکل برای ذخیره/حذف یادداشت (popBackStack دوم صفحه قبلی را هم می‌بست) | گارد `busy` |
| 10 | `LoginScreen.kt` | ارسال دوباره با Enter کیبورد حین loading | `if (loading) return` |
| 11 | `MainActivity.kt` | با چرخش صفحه دوباره به صفحه اعلان‌ها پرت می‌شد | خواندن extra فقط وقتی `savedInstanceState == null` |
| 12 | `services/AuthManager.kt` | race در ساخت `deviceId` (دو thread → دو شناسه متفاوت) | `@Synchronized get()` |
| 13 | `repository/SyncRepository.kt` | هر نوشتن در sync_meta (مثل `fired_*`) همه observerها را بی‌دلیل rebuild می‌کرد | `distinctUntilChanged()` |
| 14 | `MedicineScreen.kt` | دکمه «یادآوری» در واقع دارو را «مصرف‌شده» ثبت می‌کرد (برچسب گمراه‌کننده) | برچسب «مصرف کردم» |

## ۴) Manifest و منابع
| # | مورد | اصلاح |
|---|---|---|
| 15 | `SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` با هم | `maxSdkVersion="32"` روی SCHEDULE_EXACT_ALARM (الگوی رسمی گوگل) |
| 16 | `allowBackup=true` + EncryptedSharedPreferences → بعد از restore روی گوشی جدید توکن قابل رمزگشایی نیست | `res/xml/backup_rules.xml` و `data_extraction_rules.xml` (حذف prefs توکن از بکاپ) |
| 17 | ReminderReceiver (exported=false) و BootReceiver (exported=true، همه اکشن‌ها) | بررسی شد؛ سالم |

## ۵) Gradle
- `libs.versions.toml` با AGP 8.7.3 / Gradle 8.9 / JDK 17 / compileSdk 35 **سازگار** است:
  core-ktx 1.15 و work 2.10 به compileSdk 35 نیاز دارند ✓، Compose BOM 2024.12.01 ✓، navigation 2.8.5 ✓، lifecycle 2.8.7 ✓.
- KSP = `2.0.21-1.0.28` دقیقاً با Kotlin `2.0.21` جفت است ✓ (نسخه‌بندی KSP = `<kotlin>-<ksp>`).
- `gradle.properties`: `ksp.useKSP2=false` صریح شد (Room 2.6.1 با KSP2 مشکلات شناخته‌شده دارد).
- `versionCode 2` / `versionName 1.1.0`.

## ۶) بررسی شد و سالم بود
- همه امضاهای Dao: `Flow<…>` برای observe، `suspend` برای نوشتن/خواندن یک‌باره، return typeها ✓ — Repositoryها دقیقاً همان‌ها را forward می‌کنند.
- NavGraph: پارامترهای هر ۱۲ مقصد با امضای composableها یکی است؛ آرگومان‌های `tab/id/type/title/day` با نوع و default درست.
- همه ۴۵ آیکن، رنگ‌ها، سبک‌های متن، MetaKeys و `R.font.*`/`R.drawable.*` وجود دارند؛ فونت‌ها TTF معتبر و آیکن‌ها PNG معتبرند.
- ServiceLocator: همه ref‌ها (`db, auth, reminders, categories, notifications, meta, appScope, app`) تعریف و استفاده‌شان درست است.

## ۷) هنوز باز (نیاز به تصمیم شما، کد تغییر نکرد)
- **Gradle wrapper**: `gradlew`، `gradlew.bat` و `gradle-wrapper.jar` در بسته نیستند. Android Studio مشکلی ندارد؛ برای خط فرمان یک‌بار `gradle wrapper --gradle-version 8.9`.
- **داده دمو و سینک**: رکوردهای Seed با `local_new` ساخته می‌شوند، پس بعد از اولین ورود به حساب **به سرور push می‌شوند**. اگر نمی‌خواهید، قبل از انتشار Seed را خاموش یا با `synced` بسازید.
- **Google Play**: `USE_EXACT_ALARM` بررسی سیاستی دارد (فقط اپ‌های آلارم/یادآور).
