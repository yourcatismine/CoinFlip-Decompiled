package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VersionDetector {
   private static final String SERVER_VERSION = detectServerVersion();
   private static final int JAVA_VERSION = detectJavaVersion();
   private static final boolean IS_LEGACY = isLegacyVersion();
   private static final boolean ADVENTURE_NATIVE = detectAdventureNative();

   private static String detectServerVersion() {
      try {
         String version = Bukkit.getVersion();
         Pattern pattern = Pattern.compile("(\\d+\\.\\d+(\\.\\d+)?)");
         Matcher matcher = pattern.matcher(version);
         if (matcher.find()) {
            return matcher.group(1);
         }
      } catch (Exception var3) {
      }

      return "unknown";
   }

   private static int detectJavaVersion() {
      String version = System.getProperty("java.version");
      if (version == null) {
         return 8;
      } else if (version.startsWith("1.")) {
         if (version.startsWith("1.8")) {
            return 8;
         } else {
            return version.startsWith("1.7") ? 7 : 8;
         }
      } else {
         try {
            int dotIndex = version.indexOf(46);
            return dotIndex > 0 ? Integer.parseInt(version.substring(0, dotIndex)) : Integer.parseInt(version);
         } catch (NumberFormatException var2) {
            return 8;
         }
      }
   }

   private static boolean isLegacyVersion() {
      try {
         String[] parts = SERVER_VERSION.split("\\.");
         if (parts.length >= 2) {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            if (major == 1) {
               return minor >= 8 && minor <= 12;
            }
         }
      } catch (Exception var3) {
      }

      return false;
   }

   private static boolean detectAdventureNative() {
      if (IS_LEGACY) {
         return false;
      } else {
         try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Player.class.getMethod("sendMessage", componentClass);
            String[] parts = SERVER_VERSION.split("\\.");
            if (parts.length >= 2) {
               int major = Integer.parseInt(parts[0]);
               int minor = Integer.parseInt(parts[1]);
               if (major == 1 && minor >= 16) {
                  return true;
               }
            }

            return false;
         } catch (Exception var4) {
            return false;
         }
      }
   }

   public static String getServerVersion() {
      return SERVER_VERSION;
   }

   public static int getJavaVersion() {
      return JAVA_VERSION;
   }

   public static boolean isLegacy() {
      return IS_LEGACY;
   }

   public static boolean isAdventureNative() {
      return ADVENTURE_NATIVE;
   }

   public static boolean isVersionAtLeast(int major, int minor) {
      try {
         String[] parts = SERVER_VERSION.split("\\.");
         if (parts.length >= 2) {
            int serverMajor = Integer.parseInt(parts[0]);
            int serverMinor = Integer.parseInt(parts[1]);
            if (serverMajor > major) {
               return true;
            }

            if (serverMajor == major) {
               return serverMinor >= minor;
            }
         }

         return false;
      } catch (Exception var5) {
         return true;
      }
   }

   public static void logVersionInfo(KStudio plugin) {
      plugin.getLogger().info("Server Version: " + SERVER_VERSION);
      plugin.getLogger().info("Java Version: " + JAVA_VERSION);
      plugin.getLogger().info("Legacy Mode: " + (IS_LEGACY ? "Yes (1.8.x-1.12.x)" : "No"));
      plugin.getLogger().info("Adventure Native: " + (ADVENTURE_NATIVE ? "Yes" : "No (using fallback)"));
   }
}
