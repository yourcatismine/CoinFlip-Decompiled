package co.aikar.commands;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

class ACFBukkitListener implements Listener {
   private BukkitCommandManager manager;
   private final Plugin plugin;

   public ACFBukkitListener(BukkitCommandManager manager, Plugin plugin) {
      this.manager = manager;
      this.plugin = plugin;
   }

   @EventHandler
   public void onPluginDisable(PluginDisableEvent event) {
      if (this.plugin.getName().equalsIgnoreCase(event.getPlugin().getName())) {
         this.manager.unregisterCommands();
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      if (this.manager.autoDetectFromClient) {
         this.manager.readPlayerLocale(player);
         this.manager.getScheduler().createDelayedTask(this.plugin, () -> this.manager.readPlayerLocale(player), 20L);
      } else {
         this.manager.setIssuerLocale(player, this.manager.getLocales().getDefaultLocale());
         this.manager.notifyLocaleChange(this.manager.getCommandIssuer(player), null, this.manager.getLocales().getDefaultLocale());
      }
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent quitEvent) {
      UUID playerUniqueId = quitEvent.getPlayer().getUniqueId();
      this.manager.issuersLocale.remove(playerUniqueId);
      this.manager.issuersLocaleString.remove(playerUniqueId);
   }
}
