package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TaxRateCalculator {
   private final KStudio plugin;
   private static final String BYPASS_TAX_PERMISSION = "ultracoinflip.bypass.tax";

   public TaxRateCalculator(KStudio plugin) {
      this.plugin = plugin;
   }

   public double calculateTaxRate(Player player, double amount, CoinFlipGame.CurrencyType currencyType, String currencyId) {
      if (this.hasBypassTaxPermission(player)) {
         return 0.0;
      } else if (amount < 0.0) {
         this.plugin.getLogger().warning("Invalid amount for tax calculation: " + amount + ". Using default tax rate.");
         return 0.1;
      } else {
         CurrencySettings settings = this.plugin.getCurrencyManager().getCurrencySettings(currencyType, currencyId);
         if (settings == null) {
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin
                  .getDebugManager()
                  .info(
                     DebugManager.Category.CURRENCY, "TaxRateCalculator: CurrencySettings is null for " + currencyType + ":" + currencyId + ". Tax disabled."
                  );
            }

            return 0.0;
         } else {
            boolean taxEnabled = settings.isTaxEnabled();
            if (!taxEnabled) {
               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  this.plugin
                     .getDebugManager()
                     .info(
                        DebugManager.Category.CURRENCY,
                        "TaxRateCalculator: Tax is disabled for " + currencyType + ":" + currencyId + ". Returning 0.0 tax rate."
                     );
               }

               return 0.0;
            } else {
               TaxRateConfig config = this.getTaxRateConfig(settings);
               boolean debugEnabled = this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY);
               if (config == null) {
                  if (debugEnabled) {
                     this.plugin
                        .getLogger()
                        .info(
                           "TaxRateCalculator: Using old tax rate system for "
                              + currencyType
                              + " (dynamic tax config not found). Tax rate: "
                              + settings.getTaxRate()
                        );
                  }

                  return settings.getTaxRate();
               } else {
                  double taxRate = config.getBaseTaxRate();
                  taxRate = this.applyTierTaxRate(config, amount, taxRate);
                  if (debugEnabled) {
                     if (taxRate != taxRate) {
                        this.plugin
                           .getDebugManager()
                           .info(
                              DebugManager.Category.CURRENCY,
                              "TaxRateCalculator: Applied tier tax rate for amount "
                                 + amount
                                 + " ("
                                 + currencyType
                                 + "). Base: "
                                 + taxRate
                                 + ", Final: "
                                 + taxRate
                           );
                     } else {
                        this.plugin
                           .getDebugManager()
                           .info(
                              DebugManager.Category.CURRENCY,
                              "TaxRateCalculator: Using base tax rate for amount " + amount + " (" + currencyType + "). Rate: " + taxRate
                           );
                     }
                  }

                  return Math.max(0.0, Math.min(1.0, taxRate));
               }
            }
         }
      }
   }

   private boolean hasBypassTaxPermission(Player player) {
      return player != null && player.hasPermission("ultracoinflip.bypass.tax");
   }

   public boolean hasBypassTaxPermission(UUID playerUuid) {
      if (playerUuid == null) {
         return false;
      } else {
         Player player = Bukkit.getPlayer(playerUuid);
         return player != null ? player.hasPermission("ultracoinflip.bypass.tax") : false;
      }
   }

   private TaxRateConfig getTaxRateConfig(CurrencySettings settings) {
      return settings == null ? null : settings.getTaxRateConfig();
   }

   /** @deprecated */
   private TaxRateConfig getTaxRateConfig(CoinFlipGame.CurrencyType currencyType, String currencyId) {
      CurrencySettings settings = this.plugin.getCurrencyManager().getCurrencySettings(currencyType, currencyId);
      return this.getTaxRateConfig(settings);
   }

   private double applyTierTaxRate(TaxRateConfig config, double amount, double currentTaxRate) {
      List<TaxRateConfig.Tier> tiers = config.getTiers();
      if (tiers != null && !tiers.isEmpty()) {
         TaxRateConfig.Tier lastMatchingTier = null;

         for (TaxRateConfig.Tier tier : tiers) {
            if (tier != null && tier.matches(amount)) {
               lastMatchingTier = tier;
            }
         }

         if (lastMatchingTier != null) {
            double tierTaxRate = lastMatchingTier.getTaxRate();
            return Math.max(0.0, Math.min(1.0, tierTaxRate));
         } else {
            return currentTaxRate;
         }
      } else {
         return currentTaxRate;
      }
   }

   public TaxRateCalculator.TaxBreakdown getTaxBreakdown(Player player, double amount, CoinFlipGame.CurrencyType currencyType, String currencyId) {
      double taxRate = this.calculateTaxRate(player, amount, currencyType, currencyId);
      double totalPot = amount * 2.0;
      double tax = Math.round(totalPot * taxRate * 100.0) / 100.0;
      double taxedAmount = Math.round(totalPot * (1.0 - taxRate) * 100.0) / 100.0;
      return new TaxRateCalculator.TaxBreakdown(taxRate, tax, taxedAmount, totalPot);
   }

   public static class TaxBreakdown {
      private final double taxRate;
      private final double taxAmount;
      private final double taxedAmount;
      private final double totalPot;

      public TaxBreakdown(double taxRate, double taxAmount, double taxedAmount, double totalPot) {
         this.taxRate = taxRate;
         this.taxAmount = taxAmount;
         this.taxedAmount = taxedAmount;
         this.totalPot = totalPot;
      }

      public double getTaxRate() {
         return this.taxRate;
      }

      public double getTaxAmount() {
         return this.taxAmount;
      }

      public double getTaxedAmount() {
         return this.taxedAmount;
      }

      public double getTotalPot() {
         return this.totalPot;
      }

      public double getTaxRatePercent() {
         return this.taxRate * 100.0;
      }
   }
}
