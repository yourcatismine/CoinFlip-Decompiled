package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import java.lang.reflect.Method;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class TokenManagerCurrencyHandler implements CurrencyHandler {
   private final KStudio plugin;
   private final String unit;
   private final String displayName;
   private Object tokenManager;
   private boolean initialized = false;
   private Method getTokensMethod;
   private Method addTokensMethod;
   private Method removeTokensMethod;

   private void initialize() {
      if (!this.initialized) {
         try {
            Plugin tokenManagerPlugin = Bukkit.getServer().getPluginManager().getPlugin("TokenManager");
            if (tokenManagerPlugin == null) {
               this.initialized = true;
               return;
            }

            this.tokenManager = tokenManagerPlugin;
            Class<?> tokenManagerClass = this.tokenManager.getClass();
            this.getTokensMethod = tokenManagerClass.getMethod("getTokens", Player.class);
            this.addTokensMethod = tokenManagerClass.getMethod("addTokens", Player.class, int.class);
            this.removeTokensMethod = tokenManagerClass.getMethod("removeTokens", Player.class, int.class);
            this.initialized = true;
            this.plugin.getLogger().info("TokenManager API initialized successfully!");
         } catch (Exception var3) {
            this.plugin.getLogger().warning("Failed to initialize TokenManager API: " + var3.getMessage());
            this.initialized = true;
         }
      }
   }

   @Override
   public boolean isAvailable() {
      this.initialize();
      return this.tokenManager != null && this.getTokensMethod != null && this.addTokensMethod != null && this.removeTokensMethod != null;
   }

   @Override
   public double getBalance(Player player) {
      if (!this.isAvailable()) {
         return 0.0;
      } else {
         try {
            Object result = this.getTokensMethod.invoke(this.tokenManager, player);
            if (result instanceof Integer) {
               return ((Integer)result).doubleValue();
            } else {
               return result instanceof Number ? ((Number)result).doubleValue() : 0.0;
            }
         } catch (Exception var3) {
            this.plugin.getLogger().warning("Failed to get TokenManager balance for " + player.getName() + ": " + var3.getMessage());
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
            int tokens = (int)amount;
            this.removeTokensMethod.invoke(this.tokenManager, player, tokens);
            return true;
         } catch (Exception var5) {
            this.plugin.getLogger().warning("Failed to withdraw TokenManager tokens from " + player.getName() + ": " + var5.getMessage());
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
            int tokens = (int)amount;
            this.addTokensMethod.invoke(this.tokenManager, player, tokens);
            return true;
         } catch (Exception var5) {
            this.plugin.getLogger().warning("Failed to deposit TokenManager tokens to " + player.getName() + ": " + var5.getMessage());
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
   public TokenManagerCurrencyHandler(KStudio plugin, String unit, String displayName) {
      this.plugin = plugin;
      this.unit = unit;
      this.displayName = displayName;
   }
}
