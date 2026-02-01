package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SoundHelper {
   private final KStudio plugin;

   public SoundHelper(KStudio plugin) {
      this.plugin = plugin;
   }

   public void playSound(Player player, String soundKey) {
      if (player != null && player.isOnline()) {
         FileConfiguration soundsConfig = this.plugin.getConfigManager().getSoundsConfig();
         boolean enabled = soundsConfig.getBoolean(soundKey + ".enabled", true);
         if (enabled) {
            String soundName = soundsConfig.getString(soundKey + ".sound", "");
            if (soundName != null && !soundName.isEmpty()) {
               float volume = (float)soundsConfig.getDouble(soundKey + ".volume", 1.0);
               float pitch = (float)soundsConfig.getDouble(soundKey + ".pitch", 1.0);
               volume = Math.max(0.0F, Math.min(1.0F, volume));
               pitch = Math.max(0.5F, Math.min(2.0F, pitch));
               this.plugin.getAdventureHelper().playSound(player, soundName, volume, pitch);
            } else {
               this.plugin.getLogger().warning("Sound '" + soundKey + "' is enabled but sound name is not configured!");
            }
         }
      }
   }

   public boolean isSoundEnabled(String soundKey) {
      FileConfiguration soundsConfig = this.plugin.getConfigManager().getSoundsConfig();
      return soundsConfig.getBoolean(soundKey + ".enabled", true);
   }
}
