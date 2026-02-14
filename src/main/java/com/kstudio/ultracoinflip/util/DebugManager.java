package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;

public class DebugManager {
   private final KStudio plugin;
   private final ColorLogger colorLogger;
   private boolean enabled = false;
   private DebugManager.Level minLevel = DebugManager.Level.INFO;
   private boolean stackTrace = false;
   private boolean fileLogging = false;
   private boolean performanceTracking = false;
   private File logFile;
   private PrintWriter fileWriter;
   private final Map<DebugManager.Category, Boolean> categoryEnabled = new ConcurrentHashMap<>();
   private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
   private final Map<String, Long> performanceStartTimes = new ConcurrentHashMap<>();
   private final Map<String, List<Long>> performanceMeasurements = new ConcurrentHashMap<>();

   public DebugManager(KStudio plugin) {
      this.plugin = plugin;
      this.colorLogger = plugin.getColorLogger();
      this.loadConfig();
   }

   public void loadConfig() {
      FileConfiguration config = this.plugin.getConfig();
      if (config != null) {
         this.enabled = config.getBoolean("debug.enabled", false);
         String levelStr = config.getString("debug.level", "INFO");
         this.minLevel = DebugManager.Level.fromString(levelStr);
         this.stackTrace = config.getBoolean("debug.stack-trace", false);
         this.fileLogging = config.getBoolean("debug.file-logging", false);
         this.performanceTracking = config.getBoolean("debug.performance-tracking", false);

         for (DebugManager.Category category : DebugManager.Category.values()) {
            boolean categoryEnabled = config.getBoolean("debug.categories." + category.getConfigKey(), this.enabled);
            this.categoryEnabled.put(category, categoryEnabled);
         }

         if (this.fileLogging && this.enabled) {
            this.setupFileLogging();
         } else if (this.fileWriter != null) {
            this.closeFileLogging();
         }

         if (!this.performanceTracking) {
            this.performanceMeasurements.clear();
            this.performanceStartTimes.clear();
         }
      }
   }

   private void setupFileLogging() {
      try {
         File logsFolder = new File(this.plugin.getDataFolder(), "debug-logs");
         if (!logsFolder.exists()) {
            logsFolder.mkdirs();
         }

         String fileName = "debug-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
               + ".log";
         this.logFile = new File(logsFolder, fileName);
         this.fileWriter = new PrintWriter(new FileWriter(this.logFile, true));
         this.fileWriter.println("==========================================");
         this.fileWriter.println("Debug Log Started: " + LocalDateTime.now().format(this.dateTimeFormatter));
         this.fileWriter.println("Plugin: CoinFlip v" + this.plugin.getPluginVersion());
         this.fileWriter.println("==========================================");
         this.fileWriter.flush();
         if (this.isEnabled()) {
            this.info(DebugManager.Category.GENERAL, "File logging enabled: " + this.logFile.getAbsolutePath());
         }
      } catch (IOException var3) {
         this.plugin.getLogger().warning("Failed to setup debug file logging: " + var3.getMessage());
         this.fileLogging = false;
      }
   }

   private void closeFileLogging() {
      if (this.fileWriter != null) {
         this.fileWriter.println("==========================================");
         this.fileWriter.println("Debug Log Ended: " + LocalDateTime.now().format(this.dateTimeFormatter));
         this.fileWriter.println("==========================================");
         this.fileWriter.close();
         this.fileWriter = null;
      }
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public boolean isCategoryEnabled(DebugManager.Category category) {
      return this.enabled && this.categoryEnabled.getOrDefault(category, false);
   }

   private boolean shouldLog(DebugManager.Level level) {
      return this.enabled && level.getPriority() >= this.minLevel.getPriority();
   }

   public void debug(DebugManager.Category category, DebugManager.Level level, String message) {
      if (this.isCategoryEnabled(category) && this.shouldLog(level)) {
         String timestamp = LocalDateTime.now().format(this.dateTimeFormatter);
         String formattedMessage = String.format("[DEBUG] [%s] [%s] %s", category.getDisplayName(),
               level.getDisplayName(), message);
         this.logToConsole(category, level, formattedMessage);
         if (this.fileLogging && this.fileWriter != null) {
            this.fileWriter.println("[" + timestamp + "] " + formattedMessage);
            this.fileWriter.flush();
         }
      }
   }

   private void logToConsole(DebugManager.Category category, DebugManager.Level level, String message) {
      switch (level) {
         case VERBOSE:
            this.plugin.getLogger().info(this.colorLogger.gray(message));
            break;
         case INFO:
            this.plugin.getLogger().info(this.colorLogger.brightCyan(message));
            break;
         case WARNING:
            this.plugin.getLogger().warning(this.colorLogger.brightYellow(message));
            break;
         case ERROR:
            this.plugin.getLogger().severe(this.colorLogger.brightRed(message));
            break;
         default:
            this.plugin.getLogger().info(message);
      }
   }

   public void debug(DebugManager.Category category, DebugManager.Level level, String message, Throwable throwable) {
      this.debug(category, level, message);
      if (this.stackTrace && throwable != null) {
         String stackTrace = this.getStackTrace(throwable);
         String stackTraceMessage = "Stack trace: " + stackTrace;
         if (this.fileLogging && this.fileWriter != null) {
            this.fileWriter.println(stackTraceMessage);
            this.printStackTraceToFile(throwable);
            this.fileWriter.flush();
         } else {
            throwable.printStackTrace();
         }
      }
   }

   private String getStackTrace(Throwable throwable) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      throwable.printStackTrace(pw);
      return sw.toString();
   }

   private void printStackTraceToFile(Throwable throwable) {
      if (this.fileWriter != null) {
         throwable.printStackTrace(this.fileWriter);
      }
   }

   public void verbose(DebugManager.Category category, String message) {
      this.debug(category, DebugManager.Level.VERBOSE, message);
   }

   public void info(DebugManager.Category category, String message) {
      this.debug(category, DebugManager.Level.INFO, message);
   }

   public void warning(DebugManager.Category category, String message) {
      this.debug(category, DebugManager.Level.WARNING, message);
   }

   public void error(DebugManager.Category category, String message) {
      this.debug(category, DebugManager.Level.ERROR, message);
   }

   public void verbose(DebugManager.Category category, String message, Throwable throwable) {
      this.debug(category, DebugManager.Level.VERBOSE, message, throwable);
   }

   public void info(DebugManager.Category category, String message, Throwable throwable) {
      this.debug(category, DebugManager.Level.INFO, message, throwable);
   }

   public void warning(DebugManager.Category category, String message, Throwable throwable) {
      this.debug(category, DebugManager.Level.WARNING, message, throwable);
   }

   public void error(DebugManager.Category category, String message, Throwable throwable) {
      this.debug(category, DebugManager.Level.ERROR, message, throwable);
   }

   public void startPerformanceTracking(String operation) {
      if (this.performanceTracking && this.enabled) {
         this.performanceStartTimes.put(operation, System.currentTimeMillis());
      }
   }

   public void endPerformanceTracking(String operation) {
      if (this.performanceTracking && this.enabled) {
         Long startTime = this.performanceStartTimes.remove(operation);
         if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            this.performanceMeasurements.computeIfAbsent(operation, k -> new ArrayList<>()).add(duration);
            if (this.isCategoryEnabled(DebugManager.Category.PERFORMANCE)) {
               this.info(DebugManager.Category.PERFORMANCE,
                     String.format("Performance: %s took %d ms", operation, duration));
            }
         }
      }
   }

   public Map<String, DebugManager.PerformanceStats> getPerformanceStats() {
      Map<String, DebugManager.PerformanceStats> stats = new HashMap<>();

      for (Entry<String, List<Long>> entry : this.performanceMeasurements.entrySet()) {
         String operation = entry.getKey();
         List<Long> measurements = entry.getValue();
         if (!measurements.isEmpty()) {
            long min = Collections.min(measurements);
            long max = Collections.max(measurements);
            long sum = measurements.stream().mapToLong(Long::longValue).sum();
            long avg = sum / measurements.size();
            stats.put(operation, new DebugManager.PerformanceStats(operation, min, max, avg, measurements.size()));
         }
      }

      return stats;
   }

   public void printPerformanceStats() {
      if (this.performanceTracking && this.enabled) {
         Map<String, DebugManager.PerformanceStats> stats = this.getPerformanceStats();
         if (stats.isEmpty()) {
            this.info(DebugManager.Category.PERFORMANCE, "No performance data collected.");
         } else {
            this.info(DebugManager.Category.PERFORMANCE, "==========================================");
            this.info(DebugManager.Category.PERFORMANCE, "Performance Statistics:");
            this.info(DebugManager.Category.PERFORMANCE, "==========================================");

            for (DebugManager.PerformanceStats stat : stats.values()) {
               this.info(
                     DebugManager.Category.PERFORMANCE,
                     String.format("%s: min=%dms, max=%dms, avg=%dms, count=%d", stat.getOperation(), stat.getMin(),
                           stat.getMax(), stat.getAvg(), stat.getCount()));
            }

            this.info(DebugManager.Category.PERFORMANCE, "==========================================");
         }
      }
   }

   public void clearPerformanceStats() {
      this.performanceMeasurements.clear();
      this.performanceStartTimes.clear();
   }

   public void shutdown() {
      if (this.performanceTracking && this.enabled) {
         this.printPerformanceStats();
      }

      this.closeFileLogging();
   }

   @Deprecated
   public boolean isDebugEnabled() {
      return this.isEnabled();
   }

   public static enum Category {
      GENERAL("General", "general"),
      CONFIG("Config", "config"),
      DATABASE("Database", "database"),
      CURRENCY("Currency", "currency"),
      GAME("Game", "game"),
      GUI("GUI", "gui"),
      DISCORD("Discord", "discord"),
      COMMAND("Command", "command"),
      PERFORMANCE("Performance", "performance"),
      EVENT("Event", "event");

      private final String displayName;
      private final String configKey;

      private Category(String displayName, String configKey) {
         this.displayName = displayName;
         this.configKey = configKey;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public String getConfigKey() {
         return this.configKey;
      }
   }

   public static enum Level {
      VERBOSE(0, "VERBOSE"),
      INFO(1, "INFO"),
      WARNING(2, "WARNING"),
      ERROR(3, "ERROR");

      private final int priority;
      private final String displayName;

      private Level(int priority, String displayName) {
         this.priority = priority;
         this.displayName = displayName;
      }

      public int getPriority() {
         return this.priority;
      }

      public String getDisplayName() {
         return this.displayName;
      }

      public static DebugManager.Level fromString(String level) {
         try {
            return valueOf(level.toUpperCase());
         } catch (IllegalArgumentException var2) {
            return INFO;
         }
      }
   }

   public static class PerformanceStats {
      private final String operation;
      private final long min;
      private final long max;
      private final long avg;
      private final int count;

      public PerformanceStats(String operation, long min, long max, long avg, int count) {
         this.operation = operation;
         this.min = min;
         this.max = max;
         this.avg = avg;
         this.count = count;
      }

      public String getOperation() {
         return this.operation;
      }

      public long getMin() {
         return this.min;
      }

      public long getMax() {
         return this.max;
      }

      public long getAvg() {
         return this.avg;
      }

      public int getCount() {
         return this.count;
      }
   }
}
