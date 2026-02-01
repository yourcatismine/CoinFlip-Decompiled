package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.FileConfiguration;

public class PerformanceOptimizer {
   private static final Map<String, String> messageCache = new ConcurrentHashMap<>();
   private static final Map<String, String> configStringCache = new ConcurrentHashMap<>();
   private static final Map<String, Integer> configIntCache = new ConcurrentHashMap<>();
   private static final Map<String, Boolean> configBooleanCache = new ConcurrentHashMap<>();
   private static final Map<String, Double> configDoubleCache = new ConcurrentHashMap<>();
   private final KStudio plugin;
   private volatile long lastCacheClear = System.currentTimeMillis();
   private static final long CACHE_CLEAR_INTERVAL = 300000L;

   public PerformanceOptimizer(KStudio plugin) {
      this.plugin = plugin;
   }

   public String getCachedMessage(String key) {
      this.clearCacheIfNeeded();
      return messageCache.computeIfAbsent(key, k -> {
         String message = this.plugin.getConfigManager().getMessages().getString(k, k);
         return message != null ? message : k;
      });
   }

   public String getCachedConfigString(String key, String defaultValue) {
      this.clearCacheIfNeeded();
      String cacheKey = key + ":" + defaultValue;
      return configStringCache.computeIfAbsent(cacheKey, k -> {
         FileConfiguration config = this.plugin.getGUIConfig();
         return config != null ? config.getString(key, defaultValue) : defaultValue;
      });
   }

   public int getCachedConfigInt(String key, int defaultValue) {
      this.clearCacheIfNeeded();
      String cacheKey = key + ":" + defaultValue;
      return configIntCache.computeIfAbsent(cacheKey, k -> {
         FileConfiguration config = this.plugin.getGUIConfig();
         return config != null ? config.getInt(key, defaultValue) : defaultValue;
      });
   }

   public boolean getCachedConfigBoolean(String key, boolean defaultValue) {
      this.clearCacheIfNeeded();
      String cacheKey = key + ":" + defaultValue;
      return configBooleanCache.computeIfAbsent(cacheKey, k -> {
         FileConfiguration config = this.plugin.getGUIConfig();
         return config != null ? config.getBoolean(key, defaultValue) : defaultValue;
      });
   }

   public double getCachedConfigDouble(String key, double defaultValue) {
      this.clearCacheIfNeeded();
      String cacheKey = key + ":" + defaultValue;
      return configDoubleCache.computeIfAbsent(cacheKey, k -> {
         FileConfiguration config = this.plugin.getGUIConfig();
         return config != null ? config.getDouble(key, defaultValue) : defaultValue;
      });
   }

   public void clearCache() {
      messageCache.clear();
      configStringCache.clear();
      configIntCache.clear();
      configBooleanCache.clear();
      configDoubleCache.clear();
      this.lastCacheClear = System.currentTimeMillis();
   }

   private void clearCacheIfNeeded() {
      long now = System.currentTimeMillis();
      if (now - this.lastCacheClear > 300000L) {
         this.clearCache();
      }
   }
}
