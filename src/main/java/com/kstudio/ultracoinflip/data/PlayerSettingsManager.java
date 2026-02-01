package com.kstudio.ultracoinflip.data;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Generated;
import org.bukkit.entity.Player;

public class PlayerSettingsManager {
   private final KStudio plugin;
   private final DatabaseManager databaseManager;
   private final Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();
   private final Object cacheLock = new Object();

   public PlayerSettingsManager(KStudio plugin) {
      this.plugin = plugin;
      this.databaseManager = plugin.getDatabaseManager();
   }

   public PlayerSettings getPlayerSettings(UUID uuid) {
      synchronized (this.cacheLock) {
         PlayerSettings cached = this.settingsCache.get(uuid);
         if (cached != null) {
            return cached;
         } else {
            PlayerSettings defaultSettings = new PlayerSettings();
            this.settingsCache.put(uuid, defaultSettings);
            if (this.databaseManager != null) {
               FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
                  try {
                     PlayerSettings loaded = this.databaseManager.loadPlayerSettings(uuid);
                     synchronized (this.cacheLock) {
                        this.settingsCache.put(uuid, loaded);
                     }
                  } catch (Exception var6) {
                     this.plugin.getLogger().warning("Failed to load player settings for " + uuid + ": " + var6.getMessage());
                  }
               });
            }

            return defaultSettings;
         }
      }
   }

   public PlayerSettings loadPlayerSettingsSync(UUID uuid) {
      synchronized (this.cacheLock) {
         PlayerSettings cached = this.settingsCache.get(uuid);
         if (cached != null && !cached.isDefault()) {
            return cached;
         } else {
            label35:
            if (this.databaseManager != null) {
               PlayerSettings var10000;
               try {
                  PlayerSettings loaded = this.databaseManager.loadPlayerSettings(uuid);
                  this.settingsCache.put(uuid, loaded);
                  var10000 = loaded;
               } catch (Exception var6) {
                  this.plugin.getLogger().warning("Failed to load player settings for " + uuid + ": " + var6.getMessage());
                  break label35;
               }

               return var10000;
            }

            if (cached != null) {
               return cached;
            } else {
               PlayerSettings defaultSettings = new PlayerSettings();
               this.settingsCache.put(uuid, defaultSettings);
               return defaultSettings;
            }
         }
      }
   }

   public void savePlayerSettings(UUID uuid, PlayerSettings settings) {
      synchronized (this.cacheLock) {
         this.settingsCache.put(uuid, settings);
      }

      if (this.databaseManager != null) {
         FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
            try {
               this.databaseManager.savePlayerSettings(uuid, settings);
            } catch (Exception var4) {
               this.plugin.getLogger().warning("Failed to save player settings for " + uuid + ": " + var4.getMessage());
            }
         });
      }
   }

   public boolean getSetting(UUID uuid, String key) {
      return this.getPlayerSettings(uuid).getSetting(key);
   }

   public void setSetting(UUID uuid, String key, boolean value) {
      PlayerSettings settings = this.loadPlayerSettingsSync(uuid);
      settings.setSetting(key, value);
      this.savePlayerSettings(uuid, settings);
   }

   public boolean toggleSetting(UUID uuid, String key) {
      PlayerSettings settings = this.loadPlayerSettingsSync(uuid);
      boolean newValue = settings.toggleSetting(key);
      this.savePlayerSettings(uuid, settings);
      return newValue;
   }

   public void clearCache(UUID uuid) {
      synchronized (this.cacheLock) {
         this.settingsCache.remove(uuid);
      }
   }

   public void clearAllCache() {
      synchronized (this.cacheLock) {
         this.settingsCache.clear();
      }
   }

   public boolean isSettingEnabled(Player player, String settingKey) {
      if (player == null) {
         return true;
      } else if (player.hasPermission("ultracoinflip.settings.bypass")) {
         return true;
      } else {
         String bypassPermission = "ultracoinflip.settings.bypass." + settingKey;
         return player.hasPermission(bypassPermission) ? true : this.getSetting(player.getUniqueId(), settingKey);
      }
   }

   public boolean isSettingEnabled(UUID uuid, String settingKey) {
      return this.getSetting(uuid, settingKey);
   }

   @Generated
   public Object getCacheLock() {
      return this.cacheLock;
   }
}
