package com.kstudio.ultracoinflip.refund;

import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RefundLimiter {
   private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
   private final Map<UUID, Long> gameProcessing = new ConcurrentHashMap<>();
   private final Map<UUID, Long> playersInTransaction = new ConcurrentHashMap<>();
   private final AtomicBoolean globalLock = new AtomicBoolean(false);
   private final AtomicBoolean reloading = new AtomicBoolean(false);
   private final AtomicInteger activeRefundCount = new AtomicInteger(0);
   private long cooldownMs = 3000L;
   private static final long MAX_PROCESSING_TIME_MS = 300000L;

   public void setCooldownMs(long cooldownMs) {
      this.cooldownMs = cooldownMs;
   }

   public long getCooldownMs() {
      return this.cooldownMs;
   }

   public boolean isOnCooldown(UUID playerUuid) {
      if (playerUuid == null) {
         return false;
      } else {
         Long lastRefund = this.playerCooldowns.get(playerUuid);
         return lastRefund == null ? false : System.currentTimeMillis() - lastRefund < this.cooldownMs;
      }
   }

   public int getRemainingCooldownSeconds(UUID playerUuid) {
      if (playerUuid == null) {
         return 0;
      } else {
         Long lastRefund = this.playerCooldowns.get(playerUuid);
         if (lastRefund == null) {
            return 0;
         } else {
            long remaining = this.cooldownMs - (System.currentTimeMillis() - lastRefund);
            return remaining > 0L ? (int)Math.ceil(remaining / 1000.0) : 0;
         }
      }
   }

   public void updateCooldown(UUID playerUuid) {
      if (playerUuid != null) {
         this.playerCooldowns.put(playerUuid, System.currentTimeMillis());
      }
   }

   public boolean tryLockGame(UUID gameId) {
      if (gameId == null) {
         return false;
      } else {
         Long existing = this.gameProcessing.putIfAbsent(gameId, System.currentTimeMillis());
         return existing == null;
      }
   }

   public void unlockGame(UUID gameId) {
      if (gameId != null) {
         this.gameProcessing.remove(gameId);
      }
   }

   public boolean isGameLocked(UUID gameId) {
      return gameId != null && this.gameProcessing.containsKey(gameId);
   }

   public boolean tryStartTransaction(UUID playerUuid) {
      if (playerUuid == null) {
         return false;
      } else if (this.globalLock.get() || this.reloading.get()) {
         return false;
      } else if (this.isOnCooldown(playerUuid)) {
         return false;
      } else {
         Long existing = this.playersInTransaction.putIfAbsent(playerUuid, System.currentTimeMillis());
         if (existing != null) {
            return false;
         } else {
            this.activeRefundCount.incrementAndGet();
            return true;
         }
      }
   }

   public void endTransaction(UUID playerUuid, boolean updateCooldown) {
      if (playerUuid != null) {
         this.playersInTransaction.remove(playerUuid);
         this.activeRefundCount.decrementAndGet();
         if (updateCooldown) {
            this.updateCooldown(playerUuid);
         }
      }
   }

   public boolean isInTransaction(UUID playerUuid) {
      return playerUuid != null && this.playersInTransaction.containsKey(playerUuid);
   }

   public boolean acquireGlobalLock() {
      return this.globalLock.compareAndSet(false, true);
   }

   public void releaseGlobalLock() {
      this.globalLock.set(false);
   }

   public boolean isGlobalLocked() {
      return this.globalLock.get();
   }

   public void setReloading(boolean reloading) {
      this.reloading.set(reloading);
   }

   public boolean isReloading() {
      return this.reloading.get();
   }

   public int getActiveRefundCount() {
      return this.activeRefundCount.get();
   }

   public int getPlayersInTransactionCount() {
      return this.playersInTransaction.size();
   }

   public int getGamesProcessingCount() {
      return this.gameProcessing.size();
   }

   public boolean isRefundInProgress() {
      return this.activeRefundCount.get() > 0 || !this.playersInTransaction.isEmpty() || !this.gameProcessing.isEmpty();
   }

   public int cleanup() {
      int cleaned = 0;
      long now = System.currentTimeMillis();
      long cutoffTime = now - 300000L;
      long cooldownCutoff = now - this.cooldownMs * 2L;

      for (Entry<UUID, Long> entry : this.gameProcessing.entrySet()) {
         if (entry.getValue() < cutoffTime) {
            this.gameProcessing.remove(entry.getKey());
            cleaned++;
         }
      }

      for (Entry<UUID, Long> entryx : this.playersInTransaction.entrySet()) {
         if (entryx.getValue() < cutoffTime) {
            this.playersInTransaction.remove(entryx.getKey());
            this.activeRefundCount.decrementAndGet();
            cleaned++;
         }
      }

      for (Entry<UUID, Long> entryxx : this.playerCooldowns.entrySet()) {
         if (entryxx.getValue() < cooldownCutoff) {
            this.playerCooldowns.remove(entryxx.getKey());
            cleaned++;
         }
      }

      return cleaned;
   }

   public void reset() {
      this.playerCooldowns.clear();
      this.gameProcessing.clear();
      this.playersInTransaction.clear();
      this.globalLock.set(false);
      this.reloading.set(false);
      this.activeRefundCount.set(0);
   }
}
