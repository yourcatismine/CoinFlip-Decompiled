package com.kstudio.ultracoinflip.gui.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PlaceholderMapPool {
   private static final int MAX_POOL_SIZE = 50;
   private static final int INITIAL_MAP_CAPACITY = 16;
   private final Queue<Map<String, String>> pool = new ConcurrentLinkedQueue<>();

   public Map<String, String> borrow() {
      Map<String, String> map = this.pool.poll();
      if (map != null) {
         map.clear();
         return map;
      } else {
         return new HashMap<>(16);
      }
   }

   public void returnToPool(Map<String, String> map) {
      if (map != null) {
         if (this.pool.size() < 50) {
            map.clear();
            this.pool.offer(map);
         }
      }
   }

   public static Map<String, String> singleEntry(String key, String value) {
      Map<String, String> map = new HashMap<>(2);
      map.put(key, value);
      return map;
   }

   public static Map<String, String> twoEntries(String key1, String value1, String key2, String value2) {
      Map<String, String> map = new HashMap<>(4);
      map.put(key1, value1);
      map.put(key2, value2);
      return map;
   }

   public void clear() {
      this.pool.clear();
   }

   public int size() {
      return this.pool.size();
   }
}
