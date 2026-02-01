package com.kstudio.ultracoinflip.gui.cache;

import com.kstudio.ultracoinflip.data.CoinFlipGame;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FilteredGamesCache {
   private static final long CACHE_EXPIRY_MS = 5000L;
   private final ConcurrentMap<String, FilteredGamesCache.CacheEntry> cache = new ConcurrentHashMap<>();
   private static final Comparator<CoinFlipGame> AMOUNT_ASC_COMPARATOR = (g1, g2) -> g1 != null && g2 != null
      ? Double.compare(g1.getAmount(), g2.getAmount())
      : 0;
   private static final Comparator<CoinFlipGame> AMOUNT_DESC_COMPARATOR = (g1, g2) -> g1 != null && g2 != null
      ? Double.compare(g2.getAmount(), g1.getAmount())
      : 0;

   public List<CoinFlipGame> getFilteredGames(List<CoinFlipGame> games, String filterType, int gamesVersion) {
      if (games != null && !games.isEmpty()) {
         String safeFilterType = filterType != null ? filterType : "NONE";
         FilteredGamesCache.CacheEntry cached = this.cache.get(safeFilterType);
         if (cached != null && cached.isValid(gamesVersion)) {
            return cached.filteredGames;
         } else {
            List<CoinFlipGame> filteredGames = this.applyFilterSort(games, safeFilterType);
            this.cache.put(safeFilterType, new FilteredGamesCache.CacheEntry(gamesVersion, filteredGames));
            return filteredGames;
         }
      } else {
         return games;
      }
   }

   private List<CoinFlipGame> applyFilterSort(List<CoinFlipGame> games, String filterType) {
      List<CoinFlipGame> result = new ArrayList<>(games);
      switch (filterType) {
         case "TIME_NEWEST":
            Collections.reverse(result);
            break;
         case "AMOUNT_ASCENDING":
            result.sort(AMOUNT_ASC_COMPARATOR);
            break;
         case "AMOUNT_DESCENDING":
            result.sort(AMOUNT_DESC_COMPARATOR);
         case "NONE":
      }

      return Collections.unmodifiableList(result);
   }

   public void clear() {
      this.cache.clear();
   }

   public void clearFilter(String filterType) {
      if (filterType != null) {
         this.cache.remove(filterType);
      }
   }

   public int size() {
      return this.cache.size();
   }

   private static class CacheEntry {
      final int gamesVersion;
      final long timestamp;
      final List<CoinFlipGame> filteredGames;

      CacheEntry(int gamesVersion, List<CoinFlipGame> filteredGames) {
         this.gamesVersion = gamesVersion;
         this.timestamp = System.currentTimeMillis();
         this.filteredGames = filteredGames;
      }

      boolean isValid(int currentVersion) {
         return this.gamesVersion == currentVersion && System.currentTimeMillis() - this.timestamp < 5000L;
      }
   }
}
