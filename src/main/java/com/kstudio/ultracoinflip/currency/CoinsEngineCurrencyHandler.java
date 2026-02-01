package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.lang.reflect.Method;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CoinsEngineCurrencyHandler implements CurrencyHandler {
   private final KStudio plugin;
   private final String currencyId;
   private final String unit;
   private final String displayName;
   private Object currency;
   private boolean initialized = false;
   private Method getBalanceMethod;
   private Method addBalanceMethod;
   private Method removeBalanceMethod;

   private void initialize() {
      if (!this.initialized) {
         try {
            Plugin coinsEnginePlugin = Bukkit.getServer().getPluginManager().getPlugin("CoinsEngine");
            if (coinsEnginePlugin == null) {
               this.initialized = true;
               return;
            }

            Class<?> apiClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
            Method isLoadedMethod = apiClass.getMethod("isLoaded");
            Boolean isLoaded = (Boolean)isLoadedMethod.invoke(null);
            if (!isLoaded) {
               this.plugin.getLogger().warning("CoinsEngine API is not loaded yet!");
               this.initialized = true;
               return;
            }

            Method getCurrencyMethod = apiClass.getMethod("getCurrency", String.class);
            this.currency = getCurrencyMethod.invoke(null, this.currencyId);
            if (this.currency == null) {
               this.plugin.getLogger().warning("CoinsEngine currency with ID '" + this.currencyId + "' not found!");
               this.initialized = true;
               return;
            }

            Class<?> currencyInterface = null;

            try {
               currencyInterface = Class.forName("su.nightexpress.coinsengine.api.currency.Currency");
            } catch (ClassNotFoundException var14) {
               currencyInterface = this.currency.getClass();
            }

            Method[] allMethods = apiClass.getMethods();

            for (Method method : allMethods) {
               if (method.getName().equals("getBalance")
                  && method.getParameterCount() == 2
                  && method.getParameterTypes()[0] == Player.class
                  && currencyInterface.isAssignableFrom(method.getParameterTypes()[1])) {
                  this.getBalanceMethod = method;
                  break;
               }
            }

            for (Method methodx : allMethods) {
               if (methodx.getName().equals("addBalance")
                  && methodx.getParameterCount() == 3
                  && methodx.getParameterTypes()[0] == Player.class
                  && currencyInterface.isAssignableFrom(methodx.getParameterTypes()[1])
                  && methodx.getParameterTypes()[2] == double.class) {
                  this.addBalanceMethod = methodx;
                  break;
               }
            }

            for (Method methodxx : allMethods) {
               if (methodxx.getName().equals("removeBalance")
                  && methodxx.getParameterCount() == 3
                  && methodxx.getParameterTypes()[0] == Player.class
                  && currencyInterface.isAssignableFrom(methodxx.getParameterTypes()[1])
                  && methodxx.getParameterTypes()[2] == double.class) {
                  this.removeBalanceMethod = methodxx;
                  break;
               }
            }

            if (this.getBalanceMethod == null || this.addBalanceMethod == null || this.removeBalanceMethod == null) {
               Class<?> currencyClass = this.currency.getClass();

               try {
                  if (this.getBalanceMethod == null) {
                     this.getBalanceMethod = apiClass.getMethod("getBalance", Player.class, currencyClass);
                  }

                  if (this.addBalanceMethod == null) {
                     this.addBalanceMethod = apiClass.getMethod("addBalance", Player.class, currencyClass, double.class);
                  }

                  if (this.removeBalanceMethod == null) {
                     this.removeBalanceMethod = apiClass.getMethod("removeBalance", Player.class, currencyClass, double.class);
                  }
               } catch (NoSuchMethodException var15) {
                  for (Method methodxxx : allMethods) {
                     if (this.getBalanceMethod == null
                        && methodxxx.getName().equals("getBalance")
                        && methodxxx.getParameterCount() == 2
                        && methodxxx.getParameterTypes()[0] == Player.class
                        && currencyClass.isAssignableFrom(methodxxx.getParameterTypes()[1])) {
                        this.getBalanceMethod = methodxxx;
                     }

                     if (this.addBalanceMethod == null
                        && methodxxx.getName().equals("addBalance")
                        && methodxxx.getParameterCount() == 3
                        && methodxxx.getParameterTypes()[0] == Player.class
                        && currencyClass.isAssignableFrom(methodxxx.getParameterTypes()[1])
                        && methodxxx.getParameterTypes()[2] == double.class) {
                        this.addBalanceMethod = methodxxx;
                     }

                     if (this.removeBalanceMethod == null
                        && methodxxx.getName().equals("removeBalance")
                        && methodxxx.getParameterCount() == 3
                        && methodxxx.getParameterTypes()[0] == Player.class
                        && currencyClass.isAssignableFrom(methodxxx.getParameterTypes()[1])
                        && methodxxx.getParameterTypes()[2] == double.class) {
                        this.removeBalanceMethod = methodxxx;
                     }
                  }
               }
            }

            if (this.getBalanceMethod == null || this.addBalanceMethod == null || this.removeBalanceMethod == null) {
               throw new NoSuchMethodException(
                  "Could not find CoinsEngine API methods. Expected: CoinsEngineAPI.getBalance(Player, Currency), CoinsEngineAPI.addBalance(Player, Currency, double), CoinsEngineAPI.removeBalance(Player, Currency, double). Currency class: "
                     + this.currency.getClass().getName()
                     + ". Please check CoinsEngine v2.6.0 API documentation."
               );
            }

            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin.getLogger().info("CoinsEngine API methods found for currency '" + this.currencyId + "'");
            }

            this.initialized = true;
            this.plugin.getLogger().info("CoinsEngine currency '" + this.currencyId + "' initialized successfully!");
         } catch (Exception var16) {
            this.plugin.getLogger().warning("Failed to initialize CoinsEngine currency '" + this.currencyId + "': " + var16.getMessage());
            var16.printStackTrace();
            this.initialized = true;
         }
      }
   }

   @Override
   public boolean isAvailable() {
      this.initialize();
      return this.currency != null && this.getBalanceMethod != null && this.addBalanceMethod != null && this.removeBalanceMethod != null;
   }

   @Override
   public double getBalance(Player player) {
      if (!this.isAvailable()) {
         return 0.0;
      } else {
         try {
            Object result = this.getBalanceMethod.invoke(null, player, this.currency);
            if (result instanceof Double) {
               return (Double)result;
            } else {
               return result instanceof Number ? ((Number)result).doubleValue() : 0.0;
            }
         } catch (Exception var3) {
            this.plugin
               .getLogger()
               .warning("Failed to get CoinsEngine balance for " + player.getName() + " (currency: " + this.currencyId + "): " + var3.getMessage());
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
            this.removeBalanceMethod.invoke(null, player, this.currency, amount);
            return true;
         } catch (Exception var5) {
            this.plugin
               .getLogger()
               .warning("Failed to withdraw CoinsEngine balance from " + player.getName() + " (currency: " + this.currencyId + "): " + var5.getMessage());
            var5.printStackTrace();
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
            this.addBalanceMethod.invoke(null, player, this.currency, amount);
            return true;
         } catch (Exception var5) {
            this.plugin
               .getLogger()
               .warning("Failed to deposit CoinsEngine balance to " + player.getName() + " (currency: " + this.currencyId + "): " + var5.getMessage());
            var5.printStackTrace();
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

   public String getCurrencyId() {
      return this.currencyId;
   }

   public Object getCurrency() {
      if (!this.initialized) {
         this.initialize();
      }

      return this.currency;
   }

   @Generated
   public CoinsEngineCurrencyHandler(KStudio plugin, String currencyId, String unit, String displayName) {
      this.plugin = plugin;
      this.currencyId = currencyId;
      this.unit = unit;
      this.displayName = displayName;
   }
}
