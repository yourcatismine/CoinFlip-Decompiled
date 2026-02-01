package com.kstudio.ultracoinflip.gui.cache;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;

public class InventoryUpdateBatcher {
   private static final long BATCH_DELAY_MS = 50L;
   private final KStudio plugin;
   private final Set<Player> pendingUpdates = ConcurrentHashMap.newKeySet();
   private final AtomicBoolean scheduled = new AtomicBoolean(false);

   public InventoryUpdateBatcher(KStudio plugin) {
      this.plugin = plugin;
   }

   public void scheduleUpdate(Player player) {
      if (player != null && player.isOnline()) {
         this.pendingUpdates.add(player);
         if (this.scheduled.compareAndSet(false, true)) {
            FoliaScheduler.runTaskLater(this.plugin, player, () -> {
               this.flushUpdates();
               this.scheduled.set(false);
            }, 1L);
         }
      }
   }

   public void flushUpdates() {
      Set<Player> toUpdate = new HashSet<>(this.pendingUpdates);
      this.pendingUpdates.clear();

      for (Player player : toUpdate) {
         if (player != null && player.isOnline()) {
            try {
               player.updateInventory();
            } catch (Exception var5) {
               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin
                     .getDebugManager()
                     .verbose(DebugManager.Category.GUI, "Error updating inventory for " + player.getName() + ": " + var5.getMessage());
               }
            }
         }
      }
   }

   public void clear() {
      this.pendingUpdates.clear();
      this.scheduled.set(false);
   }
}
