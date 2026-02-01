package com.kstudio.ultracoinflip.data;

import java.util.HashMap;
import java.util.Map;
import lombok.Generated;

public class PlayerSettings {
   private Map<String, Boolean> settings = new HashMap<>();

   public boolean getSetting(String key) {
      return this.settings.getOrDefault(key, true);
   }

   public void setSetting(String key, boolean value) {
      this.settings.put(key, value);
   }

   public boolean toggleSetting(String key) {
      boolean newValue = !this.getSetting(key);
      this.setSetting(key, newValue);
      return newValue;
   }

   public boolean isDefault() {
      return this.settings.isEmpty();
   }

   @Generated
   public Map<String, Boolean> getSettings() {
      return this.settings;
   }

   @Generated
   public void setSettings(Map<String, Boolean> settings) {
      this.settings = settings;
   }
}
