package com.kstudio.ultracoinflip.currency;

import org.bukkit.entity.Player;

public interface CurrencyHandler {
   boolean isAvailable();

   double getBalance(Player var1);

   boolean hasBalance(Player var1, double var2);

   boolean withdraw(Player var1, double var2);

   boolean deposit(Player var1, double var2);

   String getUnit();

   String getDisplayName();
}
