package com.kstudio.ultracoinflip.data;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StatsBatchQueue {
   private final KStudio plugin;
   private final DatabaseManager databaseManager;
   private final Map<UUID, PlayerStats> pendingSaves = new ConcurrentHashMap<>();
   private static final int MAX_BATCH_SIZE = 10;
   private static final long FLUSH_INTERVAL_MS = 5000L;
   private final AtomicBoolean isFlushing = new AtomicBoolean(false);
   private final AtomicInteger pendingCount = new AtomicInteger(0);
   private volatile long lastFlushTime = System.currentTimeMillis();

   public StatsBatchQueue(KStudio plugin, DatabaseManager databaseManager) {
      this.plugin = plugin;
      this.databaseManager = databaseManager;
   }

   public void queueSave(UUID uuid, PlayerStats stats) {
      if (uuid != null && stats != null) {
         this.pendingSaves.put(uuid, stats);
         int count = this.pendingCount.incrementAndGet();
         if (count >= 10) {
            this.flushBatch(false);
         }
      }
   }

   public void flushBatch(boolean forced) {
      if (forced || this.isFlushing.compareAndSet(false, true)) {
         try {
            Map<UUID, PlayerStats> toSave;
            synchronized (this.pendingSaves) {
               if (this.pendingSaves.isEmpty()) {
                  return;
               }

               toSave = new ConcurrentHashMap<>(this.pendingSaves);
               this.pendingSaves.clear();
               this.pendingCount.set(0);
            }

            if (!toSave.isEmpty()) {
               int successCount = 0;
               int failCount = 0;

               for (Entry<UUID, PlayerStats> entry : toSave.entrySet()) {
                  UUID uuid = entry.getKey();
                  PlayerStats stats = entry.getValue();

                  try {
                     this.databaseManager.savePlayerStats(uuid, stats);
                     successCount++;
                  } catch (Exception var17) {
                     failCount++;
                     this.plugin.getLogger().warning("Failed to save stats for " + uuid + " in batch: " + var17.getMessage());

                     try {
                        Thread.sleep(10L);
                        this.databaseManager.savePlayerStats(uuid, stats);
                        successCount++;
                        failCount--;
                        this.plugin.getLogger().info("Retry successful for " + uuid);
                     } catch (Exception var16) {
                        this.plugin.getLogger().severe("Retry also failed for " + uuid + ": " + var16.getMessage());
                     }
                  }
               }

               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.DATABASE)) {
                  this.plugin
                     .getDebugManager()
                     .verbose(DebugManager.Category.DATABASE, String.format("Batch flush: %d saved, %d failed", successCount, failCount));
               }

               this.lastFlushTime = System.currentTimeMillis();
            }
         } finally {
            this.isFlushing.set(false);
         }
      }
   }

   public void checkAutoFlush() {
      long now = System.currentTimeMillis();
      if (now - this.lastFlushTime >= 5000L && !this.pendingSaves.isEmpty()) {
         this.flushBatch(false);
      }
   }

   public void forceFlushAll() {
      this.flushBatch(true);
   }

   public int getQueueSize() {
      return this.pendingCount.get();
   }
}
