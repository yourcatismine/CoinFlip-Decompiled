package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import java.util.logging.Logger;
import org.bukkit.Bukkit;

public class ErrorHandler {
   private static final String DISCORD_SUPPORT = "https://discord.gg/GGDxDnpnDP";
   private static final String PLUGIN_PAGE = "https://www.spigotmc.org/resources/ultracoinflip.130124/";
   private static final String DEVELOPER_NAME = "hiiamken";

   public static void checkServerCompatibility(KStudio plugin) {
      Logger logger = plugin.getLogger();
      String serverVersion = Bukkit.getVersion();
      String serverName = Bukkit.getName();
      boolean isPaper = serverName.toLowerCase().contains("paper");
      boolean isSpigot = serverName.toLowerCase().contains("spigot");
      boolean isCraftBukkit = serverName.toLowerCase().contains("craftbukkit");
      boolean isFolia = serverName.toLowerCase().contains("folia");
      boolean isPurpur = serverName.toLowerCase().contains("purpur");
      boolean isPufferfish = serverName.toLowerCase().contains("pufferfish");
      boolean hasIssues = false;
      StringBuilder issueMessage = new StringBuilder();
      boolean isLegacy = VersionDetector.isLegacy();
      String warningIcon = isLegacy ? "[!]" : "⚠";
      String detectedVersion = VersionDetector.getServerVersion();
      if ("unknown".equals(detectedVersion)) {
         hasIssues = true;
         issueMessage.append("  ").append(warningIcon).append(" Could not detect server version properly.\n");
      } else {
         try {
            String[] parts = detectedVersion.split("\\.");
            if (parts.length >= 2) {
               int major = Integer.parseInt(parts[0]);
               int minor = Integer.parseInt(parts[1]);
               if (major == 1 && minor < 8) {
                  hasIssues = true;
                  issueMessage.append("  ")
                     .append(warningIcon)
                     .append(" Detected version ")
                     .append(detectedVersion)
                     .append(" - Minimum supported version is 1.8.8\n");
               }

               if (major == 1 && minor > 21) {
                  hasIssues = true;
                  issueMessage.append("  ")
                     .append(warningIcon)
                     .append(" Detected version ")
                     .append(detectedVersion)
                     .append(" - This version may not be fully tested. Latest tested: 1.21.11\n");
               } else if (major == 1 && minor == 21) {
                  try {
                     if (parts.length >= 3) {
                        int patch = Integer.parseInt(parts[2]);
                        if (patch > 11) {
                           hasIssues = true;
                           issueMessage.append("  ")
                              .append(warningIcon)
                              .append(" Detected version ")
                              .append(detectedVersion)
                              .append(" - This version may not be fully tested. Latest tested: 1.21.11\n");
                        }
                     }
                  } catch (Exception var19) {
                  }
               }
            }
         } catch (Exception var20) {
         }
      }

      logger.info("Server Information:");
      logger.info("  Server: " + serverName);
      logger.info("  Version: " + serverVersion);
      logger.info("  Detected Version: " + detectedVersion);
      logger.info("  Java Version: " + VersionDetector.getJavaVersion());
      if (hasIssues) {
         String separator = isLegacy
            ? "==============================================================="
            : "═══════════════════════════════════════════════════════════";
         logger.warning(separator);
         logger.warning(warningIcon + " COMPATIBILITY WARNING DETECTED " + warningIcon);
         logger.warning(separator);
         logger.warning(issueMessage.toString());
         logger.warning("If you encounter any issues, please:");
         logger.warning("  1. Report the issue on our Discord: https://discord.gg/GGDxDnpnDP");
         logger.warning("  2. Include server version, Java version, and error logs");
         logger.warning("  3. Developer (hiiamken) will fix it immediately!");
         logger.warning("  4. We provide fast support and quick fixes!");
         logger.warning(separator);
      } else {
         String checkmark = isLegacy ? "[OK]" : "✓";
         logger.info(checkmark + " Server compatibility check passed");
      }
   }

   public static void handleUnexpectedError(KStudio plugin, Exception e, String context) {
      Logger logger = plugin.getLogger();
      boolean isLegacy = VersionDetector.isLegacy();
      String separator = isLegacy
         ? "==============================================================="
         : "═══════════════════════════════════════════════════════════";
      String warningIcon = isLegacy ? "[!]" : "⚠";
      logger.severe(separator);
      logger.severe(warningIcon + " UNEXPECTED ERROR DETECTED " + warningIcon);
      logger.severe(separator);
      logger.severe("Context: " + context);
      logger.severe("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      logger.severe("");
      logger.severe("Server Information:");
      logger.severe("  Server: " + Bukkit.getName());
      logger.severe("  Version: " + Bukkit.getVersion());
      logger.severe("  Detected Version: " + VersionDetector.getServerVersion());
      logger.severe("  Java Version: " + VersionDetector.getJavaVersion());
      logger.severe("");
      String bullet = isLegacy ? "-" : "•";
      String phoneIcon = isLegacy ? "[!]" : "\ud83d\udcde";
      logger.severe("This error might be caused by:");
      logger.severe("  " + bullet + " Unsupported server fork or version");
      logger.severe("  " + bullet + " Incompatible plugin combination");
      logger.severe("  " + bullet + " Corrupted configuration files");
      logger.severe("");
      logger.severe(phoneIcon + " GET HELP IMMEDIATELY:");
      logger.severe("  Discord Support: https://discord.gg/GGDxDnpnDP");
      logger.severe("  Plugin Page: https://www.spigotmc.org/resources/ultracoinflip.130124/");
      logger.severe("  Developer: hiiamken");
      logger.severe("  We will fix this issue ASAP! Fast support guaranteed!");
      logger.severe("");
      logger.severe("Please provide the following information when reporting:");
      logger.severe("  1. Full error stack trace (see below)");
      logger.severe("  2. Server version and fork");
      logger.severe("  3. Java version");
      logger.severe("  4. List of other plugins installed");
      logger.severe("  5. Steps to reproduce the error");
      logger.severe(separator);
      e.printStackTrace();
   }

   public static void handleEnableError(KStudio plugin, Exception e) {
      handleUnexpectedError(plugin, e, "Plugin Enable");
      Logger logger = plugin.getLogger();
      boolean isLegacy = VersionDetector.isLegacy();
      String warningIcon = isLegacy ? "[!]" : "⚠";
      logger.severe("");
      logger.severe(warningIcon + " Plugin failed to enable properly!");
      logger.severe("Some features may not work correctly.");
      logger.severe("Please check the error above and report it to our Discord.");
   }

   public static void handleDisableError(KStudio plugin, Exception e) {
      handleUnexpectedError(plugin, e, "Plugin Disable");
   }

   public static void handleDatabaseError(KStudio plugin, Exception e, String operation) {
      Logger logger = plugin.getLogger();
      boolean isLegacy = VersionDetector.isLegacy();
      String separator = isLegacy
         ? "==============================================================="
         : "═══════════════════════════════════════════════════════════";
      String warningIcon = isLegacy ? "[!]" : "⚠";
      String bullet = isLegacy ? "-" : "•";
      logger.severe(separator);
      logger.severe(warningIcon + " DATABASE ERROR " + warningIcon);
      logger.severe(separator);
      logger.severe("Operation: " + operation);
      logger.severe("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      logger.severe("");
      logger.severe("This might be caused by:");
      logger.severe("  " + bullet + " Database connection issues");
      logger.severe("  " + bullet + " Corrupted database file");
      logger.severe("  " + bullet + " Insufficient permissions");
      logger.severe("");
      logger.severe("Need help? Contact us:");
      logger.severe("  Discord: https://discord.gg/GGDxDnpnDP");
      logger.severe(separator);
      e.printStackTrace();
   }

   public static void handleConfigError(KStudio plugin, Exception e, String configFile) {
      Logger logger = plugin.getLogger();
      boolean isLegacy = VersionDetector.isLegacy();
      String separator = isLegacy
         ? "==============================================================="
         : "═══════════════════════════════════════════════════════════";
      String warningIcon = isLegacy ? "[!]" : "⚠";
      logger.warning(separator);
      logger.warning(warningIcon + " CONFIGURATION ERROR " + warningIcon);
      logger.warning(separator);
      logger.warning("Config File: " + configFile);
      logger.warning("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      logger.warning("");
      logger.warning("The plugin will use default values.");
      logger.warning("If this persists, please:");
      logger.warning("  1. Check your " + configFile + " file for syntax errors");
      logger.warning("  2. Delete the file to regenerate it with defaults");
      logger.warning("  3. Contact us on Discord if issue persists: https://discord.gg/GGDxDnpnDP");
      logger.warning(separator);
   }

   public static void setupGlobalExceptionHandler(KStudio plugin) {
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
         if (throwable instanceof Exception) {
            handleUnexpectedError(plugin, (Exception)throwable, "Uncaught Exception in thread: " + thread.getName());
         }
      });
   }
}
