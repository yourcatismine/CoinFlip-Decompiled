package com.kstudio.ultracoinflip.listeners;

import com.kstudio.ultracoinflip.KStudio;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;

public class VaultEconomyListener implements Listener {
   private final KStudio plugin;
   private boolean economyInitialized = false;

   public VaultEconomyListener(KStudio plugin) {
      this.plugin = plugin;
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onServiceRegister(ServiceRegisterEvent event) {
      if (!this.economyInitialized && event.getProvider().getService().equals(Economy.class)) {
         this.plugin.getColorLogger().info("     " + this.plugin.getColorLogger().brightGreen("Economy provider detected! Initializing currency system..."));
         this.plugin.initializeEconomySystem();
         this.economyInitialized = true;
      }
   }
}
