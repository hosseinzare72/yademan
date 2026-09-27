# مسیر: Yademan/app/proguard-rules.pro
# v1.2: minify در release روشن است.
# Entityهای Room و مدل‌های داده (Gson فقط با JsonObject کار می‌کند، ولی برای اطمینان نگه می‌داریم)
-keep class com.hossein.yademan.data.** { *; }
# Receiverها و Workerها از Manifest/WorkManager با reflection ساخته می‌شوند
-keep class com.hossein.yademan.services.** extends android.content.BroadcastReceiver { *; }
-keep class com.hossein.yademan.services.** extends androidx.work.ListenableWorker { <init>(...); }
# Gson
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-dontwarn com.google.gson.**
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# security-crypto (Tink)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
