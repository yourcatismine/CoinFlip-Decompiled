package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BeastTokensCurrencyHandler implements CurrencyHandler {
   private final KStudio plugin;
   private final String unit;
   private final String displayName;
   private Object tokensManager;
   private Method getTokensMethod;
   private Method addTokensMethod;
   private Method removeTokensMethod;
   private boolean initialized = false;

   public BeastTokensCurrencyHandler(KStudio plugin, String unit, String displayName) {
      this.plugin = plugin;
      this.unit = unit;
      this.displayName = displayName;
   }

   private void initialize() {
      if (!this.initialized) {
         try {
            Plugin beastTokens = Bukkit.getPluginManager().getPlugin("BeastTokens");
            if (beastTokens != null && beastTokens.isEnabled()) {
               Class<?> apiClass = Class.forName("me.mraxetv.beasttokens.api.BeastTokensAPI");
               Method getTokensManagerMethod = apiClass.getMethod("getTokensManager");
               this.tokensManager = getTokensManagerMethod.invoke(null);
               if (this.tokensManager == null) {
                  this.plugin.getLogger().warning("BeastTokens plugin returned a null tokens manager. BeastTokens currency will be disabled.");
               } else {
                  Class<?> managerClass = Class.forName("me.mraxetv.beasttokens.api.handlers.BTTokensManager");
                  this.getTokensMethod = managerClass.getMethod("getTokens", Player.class);
                  this.addTokensMethod = managerClass.getMethod("addTokens", Player.class, double.class);
                  this.removeTokensMethod = managerClass.getMethod("removeTokens", Player.class, double.class);
                  this.plugin.getLogger().info("BeastTokens integration initialized successfully (using classes provided by the BeastTokens plugin).");
               }

               return;
            }

            this.initialized = true;
         } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException var8) {
            this.plugin.getLogger().warning("Failed to initialize BeastTokens integration: " + var8.getMessage());
            this.tokensManager = null;
            this.getTokensMethod = null;
            this.addTokensMethod = null;
            this.removeTokensMethod = null;
            return;
         } finally {
            this.initialized = true;
         }
      }
   }

   @Override
   public boolean isAvailable() {
      this.initialize();
      return this.tokensManager != null && this.getTokensMethod != null && this.addTokensMethod != null && this.removeTokensMethod != null;
   }

   @Override
   public double getBalance(Player player) {
      if (!this.isAvailable()) {
         return 0.0;
      } else {
         try {
            Object result = this.getTokensMethod.invoke(this.tokensManager, player);
            return result instanceof Number ? ((Number)result).doubleValue() : 0.0;
         } catch (Exception var3) {
            this.plugin.getLogger().warning("Failed to get BeastTokens balance for " + player.getName() + ": " + var3.getMessage());
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
            this.removeTokensMethod.invoke(this.tokensManager, player, amount);
            return true;
         } catch (Exception var5) {
            this.plugin.getLogger().warning("Failed to withdraw BeastTokens from " + player.getName() + ": " + var5.getMessage());
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
            this.addTokensMethod.invoke(this.tokensManager, player, amount);
            return true;
         } catch (Exception var5) {
            this.plugin.getLogger().warning("Failed to deposit BeastTokens to " + player.getName() + ": " + var5.getMessage());
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
}
