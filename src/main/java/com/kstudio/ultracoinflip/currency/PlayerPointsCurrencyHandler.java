package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import java.lang.reflect.Method;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerPointsCurrencyHandler implements CurrencyHandler {
   private final KStudio plugin;
   private final String unit;
   private final String displayName;
   private Object api;
   private boolean initialized = false;
   private Method lookMethod;
   private Method takeMethod;
   private Method giveMethod;

   private void initialize() {
      if (!this.initialized) {
         try {
            Plugin playerPointsPlugin = Bukkit.getServer().getPluginManager().getPlugin("PlayerPoints");
            if (playerPointsPlugin == null) {
               this.initialized = true;
               return;
            }

            Method getAPIMethod = playerPointsPlugin.getClass().getMethod("getAPI");
            this.api = getAPIMethod.invoke(playerPointsPlugin);
            if (this.api != null) {
               Class<?> apiClass = this.api.getClass();
               this.lookMethod = apiClass.getMethod("look", UUID.class);
               this.takeMethod = apiClass.getMethod("take", UUID.class, int.class);
               this.giveMethod = apiClass.getMethod("give", UUID.class, int.class);
               this.initialized = true;
               this.plugin.getLogger().info("PlayerPoints API initialized successfully!");
            }
         } catch (Exception var4) {
            this.plugin.getLogger().warning("Failed to initialize PlayerPoints API: " + var4.getMessage());
            this.initialized = true;
         }
      }
   }

   @Override
   public boolean isAvailable() {
      this.initialize();
      return this.api != null && this.lookMethod != null && this.takeMethod != null && this.giveMethod != null;
   }

   @Override
   public double getBalance(Player player) {
      if (!this.isAvailable()) {
         return 0.0;
      } else {
         try {
            Object result = this.lookMethod.invoke(this.api, player.getUniqueId());
            if (result instanceof Integer) {
               return ((Integer)result).doubleValue();
            } else {
               return result instanceof Number ? ((Number)result).doubleValue() : 0.0;
            }
         } catch (Exception var3) {
            this.plugin.getLogger().warning("Failed to get PlayerPoints balance for " + player.getName() + ": " + var3.getMessage());
            return 0.0;
         }
      }
   }

   @Override
   public boolean hasBalance(Player player, double amount) {
      return !this.isAvailable() ? false : this.getBalance(player) >= amount;
   }

   @Override
   public boolean withdraw(Player player, double amount) {
      if (!this.isAvailable()) {
         return false;
      } else {
         try {
            int points = (int)amount;
            Object result = this.takeMethod.invoke(this.api, player.getUniqueId(), points);
            return result instanceof Boolean ? (Boolean)result : true;
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to withdraw PlayerPoints from " + player.getName() + ": " + var6.getMessage());
            return false;
         }
      }
   }

   @Override
   public boolean deposit(Player player, double amount) {
      if (!this.isAvailable()) {
         return false;
      } else {
         try {
            int points = (int)amount;
            Object result = this.giveMethod.invoke(this.api, player.getUniqueId(), points);
            return result instanceof Boolean ? (Boolean)result : true;
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to deposit PlayerPoints to " + player.getName() + ": " + var6.getMessage());
            return false;
         }
      }
   }

   @Override
   public String getUnit() {
      return this.unit;
   }

   @Override
   public String getDisplayName() {
      return this.displayName;
   }

   @Generated
   public PlayerPointsCurrencyHandler(KStudio plugin, String unit, String displayName) {
      this.plugin = plugin;
      this.unit = unit;
      this.displayName = displayName;
   }
}
