package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import lombok.Generated;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public class VaultCurrencyHandler implements CurrencyHandler {
   private final KStudio plugin;
   private final String unit;
   private final String displayName;

   @Override
   public boolean isAvailable() {
      return this.plugin.getEconomy() != null;
   }

   @Override
   public double getBalance(Player player) {
      return !this.isAvailable() ? 0.0 : this.plugin.getEconomy().getBalance(player);
   }

   @Override
   public boolean hasBalance(Player player, double amount) {
      return !this.isAvailable() ? false : this.plugin.getEconomy().has(player, amount);
   }

   @Override
   public boolean withdraw(Player player, double amount) {
      if (!this.isAvailable()) {
         return false;
      } else {
         Economy economy = this.plugin.getEconomy();
         return economy.has(player, amount) ? economy.withdrawPlayer(player, amount).transactionSuccess() : false;
      }
   }

   @Override
   public boolean deposit(Player player, double amount) {
      return !this.isAvailable() ? false : this.plugin.getEconomy().depositPlayer(player, amount).transactionSuccess();
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
   public VaultCurrencyHandler(KStudio plugin, String unit, String displayName) {
      this.plugin = plugin;
      this.unit = unit;
      this.displayName = displayName;
   }
}
