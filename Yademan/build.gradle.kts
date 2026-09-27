// مسیر: Yademan/build.gradle.kts
// فایل بیلد سطح پروژه؛ فقط پلاگین‌ها را معرفی می‌کند
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
