# Keep Room Database and Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Android Telecom Services and Activities
-keep class com.example.telephony.** { *; }
-keep class com.example.domain.model.** { *; }
