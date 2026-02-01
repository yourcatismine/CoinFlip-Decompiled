package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import lombok.Generated;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderCurrencyHandler implements CurrencyHandler {
   private final KStudio plugin;
   private final String currencyId;
   private final String placeholder;
   private final String unit;
   private final String displayName;
   private final String giveCommand;
   private final String removeCommand;

   public String getCurrencyId() {
      return this.currencyId;
   }

   @Override
   public boolean isAvailable() {
      return this.plugin.isPlaceholderAPI();
   }

   @Override
   public double getBalance(Player player) {
      if (!this.isAvailable()) {
         return 0.0;
      } else {
         try {
            String value = PlaceholderAPI.setPlaceholders(player, this.placeholder);
            value = value.replaceAll("[^0-9.-]", "");
            if (!value.isEmpty() && !value.equals("-") && !value.equals(".")) {
               double balance = Double.parseDouble(value);
               return Math.max(0.0, balance);
            } else {
               this.plugin
                  .getLogger()
                  .warning(
                     "Placeholder '"
                        + this.placeholder
                        + "' returned invalid value for "
                        + player.getName()
                        + ": "
                        + PlaceholderAPI.setPlaceholders(player, this.placeholder)
                  );
               return 0.0;
            }
         } catch (NumberFormatException var5) {
            this.plugin
               .getLogger()
               .warning(
                  "Failed to parse placeholder balance for "
                     + player.getName()
                     + " using "
                     + this.placeholder
                     + ". Raw value: "
                     + PlaceholderAPI.setPlaceholders(player, this.placeholder)
               );
            return 0.0;
         } catch (Exception var6) {
            this.plugin
               .getLogger()
               .warning("Failed to get placeholder balance for " + player.getName() + " using " + this.placeholder + ": " + var6.getMessage());
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
      } else if (!this.hasBalance(player, amount)) {
         return false;
      } else {
         try {
            String command = this.removeCommand
               .replace("{player}", player.getName())
               .replace("{amount}", String.format("%.2f", amount).replaceAll("\\.?0+$", ""));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return true;
         } catch (Exception var5) {
            this.plugin
               .getLogger()
               .warning(
                  "Failed to withdraw currency from "
                     + player.getName()
                     + " (currency: "
                     + this.currencyId
                     + ") using command: "
                     + this.removeCommand
                     + " - "
                     + var5.getMessage()
               );
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
            String command = this.giveCommand
               .replace("{player}", player.getName())
               .replace("{amount}", String.format("%.2f", amount).replaceAll("\\.?0+$", ""));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return true;
         } catch (Exception var5) {
            this.plugin
               .getLogger()
               .warning(
                  "Failed to deposit currency to "
                     + player.getName()
                     + " (currency: "
                     + this.currencyId
                     + ") using command: "
                     + this.giveCommand
                     + " - "
                     + var5.getMessage()
               );
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
   public PlaceholderCurrencyHandler(
      KStudio plugin, String currencyId, String placeholder, String unit, String displayName, String giveCommand, String removeCommand
   ) {
      this.plugin = plugin;
      this.currencyId = currencyId;
      this.placeholder = placeholder;
      this.unit = unit;
      this.displayName = displayName;
      this.giveCommand = giveCommand;
      this.removeCommand = removeCommand;
   }
}
