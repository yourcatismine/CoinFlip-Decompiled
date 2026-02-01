package com.kstudio.ultracoinflip.gui.cache;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.inventory.ItemStack;

public class PlayerHeadCache {
   private static final int MAX_CACHE_SIZE = 1000;
   private static final long CACHE_EXPIRY_MS = 300000L;
   private final ConcurrentMap<String, PlayerHeadCache.CachedHead> cache = new ConcurrentHashMap<>();

   private static int hashLore(List<?> lore) {
      return lore != null && !lore.isEmpty() ? lore.toString().hashCode() : 0;
   }

   public ItemStack getCachedHead(UUID playerUuid, String base64, String displayName, List<?> lore, Boolean glowing, Integer customModelData) {
      if (playerUuid != null) {
         return null;
      } else {
         String key = new PlayerHeadCache.CacheKey(playerUuid, base64, displayName, hashLore(lore), glowing, customModelData).toString();
         PlayerHeadCache.CachedHead cached = this.cache.get(key);
         if (cached != null) {
            if (cached.isExpired()) {
               this.cache.remove(key);
               return null;
            } else {
               return cached.getHead().clone();
            }
         } else {
            return null;
         }
      }
   }

   public void cacheHead(UUID playerUuid, String base64, String displayName, List<?> lore, Boolean glowing, Integer customModelData, ItemStack head) {
      if (playerUuid == null) {
         if (this.cache.size() >= 1000) {
            this.cleanupExpired();
            if (this.cache.size() >= 1000) {
               int toRemove = 100;
               this.cache
                  .entrySet()
                  .stream()
                  .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
                  .limit(toRemove)
                  .forEach(e -> this.cache.remove(e.getKey()));
            }
         }

         String key = new PlayerHeadCache.CacheKey(playerUuid, base64, displayName, hashLore(lore), glowing, customModelData).toString();
         this.cache.put(key, new PlayerHeadCache.CachedHead(head.clone()));
      }
   }

   public void cleanupExpired() {
      this.cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
   }

   public void clear() {
      this.cache.clear();
   }

   public int size() {
      return this.cache.size();
   }

   private static class CacheKey {
      private final String key;

      public CacheKey(UUID playerUuid, String base64, String displayName, int loreHash, Boolean glowing, Integer customModelData) {
         StringBuilder sb = new StringBuilder();
         if (playerUuid != null) {
            sb.append("player:").append(playerUuid.toString());
         }

         if (base64 != null && !base64.isEmpty()) {
            sb.append(":base64:").append(base64.hashCode());
         }

         if (displayName != null) {
            sb.append(":name:").append(displayName.hashCode());
         }

         sb.append(":lore:").append(loreHash);
         if (glowing != null) {
            sb.append(":glow:").append(glowing);
         }

         if (customModelData != null) {
            sb.append(":model:").append(customModelData);
         }

         this.key = sb.toString();
      }

      @Override
      public String toString() {
         return this.key;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else if (o != null && this.getClass() == o.getClass()) {
            PlayerHeadCache.CacheKey cacheKey = (PlayerHeadCache.CacheKey)o;
            return this.key.equals(cacheKey.key);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return this.key.hashCode();
      }
   }

   private static class CachedHead {
      private final ItemStack head;
      private final long timestamp;

      public CachedHead(ItemStack head) {
         this.head = head;
         this.timestamp = System.currentTimeMillis();
      }

      public ItemStack getHead() {
         return this.head;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() - this.timestamp > 300000L;
      }
   }
}
