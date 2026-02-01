package com.kstudio.ultracoinflip.listeners;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.security.ExploitDetector;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
   private final KStudio plugin;

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      this.plugin.getCoinFlipManager().restoreBackup(player);
      this.plugin.getCoinFlipManager().restoreWaitingGameBackups(player);
      FoliaScheduler.runTaskLater(this.plugin, player, () -> this.plugin.sendPendingUpdateMessages(player), 20L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      boolean cancelGameOnDisconnect = this.plugin.getCoinFlipManager().isCancelGameOnDisconnect();
      boolean refundOnDisconnect = this.plugin.getCoinFlipManager().isRefundOnDisconnect();
      boolean keepOnDisconnect = this.plugin.getCoinFlipManager().isKeepCoinflipOnDisconnect();
      if (this.plugin.getCoinFlipManager().isInRollingGame(uuid)) {
         if (cancelGameOnDisconnect) {
            this.plugin.getCoinFlipManager().refundRollingGame(uuid);
         }
      } else {
         if (this.plugin.getCoinFlipManager().hasActiveGame(uuid)) {
            if (keepOnDisconnect) {
               return;
            }

            if (refundOnDisconnect) {
               this.plugin.getCoinFlipManager().refundAllGames(player);
            } else {
               this.plugin.getCoinFlipManager().cancelGameWithoutRefund(player);
            }
         }

         if (this.plugin.getPlayerSettingsManager() != null) {
            this.plugin.getPlayerSettingsManager().clearCache(uuid);
         }

         ExploitDetector detector = this.plugin.getExploitDetector();
         if (detector != null) {
            detector.clearPlayerData(uuid);
         }

         if (this.plugin.getBettingLimitManager() != null) {
            this.plugin.getBettingLimitManager().clearPlayerData(uuid);
         }
      }
   }

   @Generated
   public PlayerListener(KStudio plugin) {
      this.plugin = plugin;
   }
}
