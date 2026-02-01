package com.kstudio.ultracoinflip.gui.cache;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.entity.Player;

public class ClickCooldownManager {
   private static final long DEFAULT_COOLDOWN_MS = 100L;
   private final ConcurrentMap<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
   private final long cooldownMs;

   public ClickCooldownManager(long cooldownMs) {
      this.cooldownMs = cooldownMs > 0L ? cooldownMs : 100L;
   }

   public ClickCooldownManager() {
      this(100L);
   }

   public boolean canClick(Player player) {
      if (player == null) {
         return false;
      } else {
         UUID uuid = player.getUniqueId();
         Long lastClick = this.lastClickTime.get(uuid);
         if (lastClick == null) {
            this.lastClickTime.put(uuid, System.currentTimeMillis());
            return true;
         } else {
            long timeSinceLastClick = System.currentTimeMillis() - lastClick;
            if (timeSinceLastClick >= this.cooldownMs) {
               this.lastClickTime.put(uuid, System.currentTimeMillis());
               return true;
            } else {
               return false;
            }
         }
      }
   }

   public void clearCooldown(Player player) {
      if (player != null) {
         this.lastClickTime.remove(player.getUniqueId());
      }
   }

   public void clearAll() {
      this.lastClickTime.clear();
   }

   public long getRemainingCooldown(Player player) {
      if (player == null) {
         return 0L;
      } else {
         UUID uuid = player.getUniqueId();
         Long lastClick = this.lastClickTime.get(uuid);
         if (lastClick == null) {
            return 0L;
         } else {
            long timeSinceLastClick = System.currentTimeMillis() - lastClick;
            long remaining = this.cooldownMs - timeSinceLastClick;
            return remaining > 0L ? remaining : 0L;
         }
      }
   }
}
