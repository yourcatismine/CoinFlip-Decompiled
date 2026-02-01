package com.kstudio.ultracoinflip.gui.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.inventory.ItemStack;

public class ItemStackPool {
   private static final int MAX_POOL_SIZE = 100;
   private final ConcurrentMap<String, ItemStack> fillerPool = new ConcurrentHashMap<>();
   private final ConcurrentMap<String, ItemStack> itemPool = new ConcurrentHashMap<>();

   public ItemStack getFillerItem(String materialName, String displayName) {
      String cacheKey = materialName + ":" + (displayName != null ? displayName.hashCode() : 0);
      ItemStack template = this.fillerPool.get(cacheKey);
      return template != null ? template.clone() : null;
   }

   public void poolFillerItem(String materialName, String displayName, ItemStack template) {
      if (template != null) {
         if (this.fillerPool.size() < 100) {
            String cacheKey = materialName + ":" + (displayName != null ? displayName.hashCode() : 0);
            this.fillerPool.putIfAbsent(cacheKey, template.clone());
         }
      }
   }

   public ItemStack getPooledItem(String cacheKey) {
      ItemStack template = this.itemPool.get(cacheKey);
      return template != null ? template.clone() : null;
   }

   public void poolItem(String cacheKey, ItemStack template) {
      if (template != null && cacheKey != null) {
         if (this.itemPool.size() < 100) {
            this.itemPool.putIfAbsent(cacheKey, template.clone());
         }
      }
   }

   public void clear() {
      this.fillerPool.clear();
      this.itemPool.clear();
   }

   public int size() {
      return this.fillerPool.size() + this.itemPool.size();
   }
}
