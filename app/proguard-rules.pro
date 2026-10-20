# yt-dlp / ffmpeg native & java wrappers
-keep class com.yausername.** { *; }
-keep interface com.yausername.** { *; }
-dontwarn com.yausername.**

-keep class io.github.junkfood02.** { *; }
-keep interface io.github.junkfood02.** { *; }
-dontwarn io.github.junkfood02.**

# Jackson JSON parsing used by youtubedl-android VideoInfo mapper
-keep class com.fasterxml.jackson.** { *; }
-keep interface com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

# App database entities
-keep class com.kmb.downloader.data.** { *; }
