package com.kstudio.ultracoinflip.data;

import com.kstudio.ultracoinflip.KStudio;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.entity.Player;

public class BettingLimitManager {
   private final KStudio plugin;
   private final ConcurrentMap<UUID, ConcurrentMap<String, BettingLimitManager.BettingData>> playerBettingData = new ConcurrentHashMap<>();
   private static final String BYPASS_PERMISSION = "ultracoinflip.bypass.bettinglimit";

   public BettingLimitManager(KStudio plugin) {
      this.plugin = plugin;
   }

   public boolean isEnabled() {
      return this.plugin.getConfig().getBoolean("betting-limits.enabled", false);
   }

   public BettingLimitManager.LimitCheckResult canPlayerBet(Player player, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) {
      if (!this.isEnabled()) {
         return null;
      } else if (player.hasPermission("ultracoinflip.bypass.bettinglimit")) {
         return null;
      } else {
         String currencyKey = this.getCurrencyKey(currencyType, currencyId);
         String configPath = this.getConfigPath(currencyType, currencyId);
         if (!this.plugin.getConfig().contains(configPath)) {
            return null;
         } else {
            BettingLimitManager.BettingData data = this.getOrCreateBettingData(player.getUniqueId(), currencyKey);
            data.checkAndResetIfNeeded();
            double dailyLimit = this.plugin.getConfig().getDouble(configPath + ".daily-limit", -1.0);
            if (dailyLimit > 0.0) {
               double newDailyTotal = data.dailyTotal + amount;
               if (newDailyTotal > dailyLimit) {
                  double remaining = Math.max(0.0, dailyLimit - data.dailyTotal);
                  return new BettingLimitManager.LimitCheckResult(
                     BettingLimitManager.LimitType.DAILY, dailyLimit, data.dailyTotal, remaining, currencyType, currencyId
                  );
               }
            }

            double weeklyLimit = this.plugin.getConfig().getDouble(configPath + ".weekly-limit", -1.0);
            if (weeklyLimit > 0.0) {
               double newWeeklyTotal = data.weeklyTotal + amount;
               if (newWeeklyTotal > weeklyLimit) {
                  double remaining = Math.max(0.0, weeklyLimit - data.weeklyTotal);
                  return new BettingLimitManager.LimitCheckResult(
                     BettingLimitManager.LimitType.WEEKLY, weeklyLimit, data.weeklyTotal, remaining, currencyType, currencyId
                  );
               }
            }

            return null;
         }
      }
   }

   public void recordBet(Player player, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) {
      if (this.isEnabled()) {
         if (!player.hasPermission("ultracoinflip.bypass.bettinglimit")) {
            String currencyKey = this.getCurrencyKey(currencyType, currencyId);
            BettingLimitManager.BettingData data = this.getOrCreateBettingData(player.getUniqueId(), currencyKey);
            data.checkAndResetIfNeeded();
            data.dailyTotal += amount;
            data.weeklyTotal += amount;
         }
      }
   }

   public double getRemainingDailyLimit(Player player, CoinFlipGame.CurrencyType currencyType, String currencyId) {
      if (this.isEnabled() && !player.hasPermission("ultracoinflip.bypass.bettinglimit")) {
         String currencyKey = this.getCurrencyKey(currencyType, currencyId);
         String configPath = this.getConfigPath(currencyType, currencyId);
         double dailyLimit = this.plugin.getConfig().getDouble(configPath + ".daily-limit", -1.0);
         if (dailyLimit <= 0.0) {
            return -1.0;
         } else {
            BettingLimitManager.BettingData data = this.getOrCreateBettingData(player.getUniqueId(), currencyKey);
            data.checkAndResetIfNeeded();
            return Math.max(0.0, dailyLimit - data.dailyTotal);
         }
      } else {
         return -1.0;
      }
   }

   public double getRemainingWeeklyLimit(Player player, CoinFlipGame.CurrencyType currencyType, String currencyId) {
      if (this.isEnabled() && !player.hasPermission("ultracoinflip.bypass.bettinglimit")) {
         String currencyKey = this.getCurrencyKey(currencyType, currencyId);
         String configPath = this.getConfigPath(currencyType, currencyId);
         double weeklyLimit = this.plugin.getConfig().getDouble(configPath + ".weekly-limit", -1.0);
         if (weeklyLimit <= 0.0) {
            return -1.0;
         } else {
            BettingLimitManager.BettingData data = this.getOrCreateBettingData(player.getUniqueId(), currencyKey);
            data.checkAndResetIfNeeded();
            return Math.max(0.0, weeklyLimit - data.weeklyTotal);
         }
      } else {
         return -1.0;
      }
   }

   public void clearPlayerData(UUID playerUuid) {
      this.playerBettingData.remove(playerUuid);
   }

   public void clearAll() {
      this.playerBettingData.clear();
   }

   private BettingLimitManager.BettingData getOrCreateBettingData(UUID playerUuid, String currencyKey) {
      return this.playerBettingData
         .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
         .computeIfAbsent(currencyKey, k -> new BettingLimitManager.BettingData());
   }

   private String getCurrencyKey(CoinFlipGame.CurrencyType type, String id) {
      return type != CoinFlipGame.CurrencyType.COINSENGINE && type != CoinFlipGame.CurrencyType.PLACEHOLDER
         ? type.name().toLowerCase()
         : type.name().toLowerCase() + "_" + (id != null ? id : "default");
   }

   private String getConfigPath(CoinFlipGame.CurrencyType type, String id) {
      switch (type) {
         case MONEY:
            return "betting-limits.currencies.money";
         case PLAYERPOINTS:
            return "betting-limits.currencies.playerpoints";
         case TOKENMANAGER:
            return "betting-limits.currencies.tokenmanager";
         case BEASTTOKENS:
            return "betting-limits.currencies.beasttokens";
         case COINSENGINE:
            return "betting-limits.currencies.coinsengine." + (id != null ? id : "default");
         case PLACEHOLDER:
            return "betting-limits.currencies.placeholder." + (id != null ? id : "default");
         default:
            return "betting-limits.currencies.default";
      }
   }

   private static class BettingData {
      double dailyTotal = 0.0;
      double weeklyTotal = 0.0;
      LocalDate dailyResetDate = LocalDate.now();
      LocalDate weeklyResetDate = getWeekStartDate();

      private BettingData() {
      }

      private static LocalDate getWeekStartDate() {
         return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      }

      void checkAndResetIfNeeded() {
         LocalDate today = LocalDate.now();
         LocalDate currentWeekStart = getWeekStartDate();
         if (!today.equals(this.dailyResetDate)) {
            this.dailyTotal = 0.0;
            this.dailyResetDate = today;
         }

         if (!currentWeekStart.equals(this.weeklyResetDate)) {
            this.weeklyTotal = 0.0;
            this.weeklyResetDate = currentWeekStart;
         }
      }
   }

   public static class LimitCheckResult {
      private final BettingLimitManager.LimitType limitType;
      private final double limit;
      private final double currentTotal;
      private final double remaining;
      private final CoinFlipGame.CurrencyType currencyType;
      private final String currencyId;

      public LimitCheckResult(
         BettingLimitManager.LimitType limitType,
         double limit,
         double currentTotal,
         double remaining,
         CoinFlipGame.CurrencyType currencyType,
         String currencyId
      ) {
         this.limitType = limitType;
         this.limit = limit;
         this.currentTotal = currentTotal;
         this.remaining = remaining;
         this.currencyType = currencyType;
         this.currencyId = currencyId;
      }

      public BettingLimitManager.LimitType getLimitType() {
         return this.limitType;
      }

      public double getLimit() {
         return this.limit;
      }

      public double getCurrentTotal() {
         return this.currentTotal;
      }

      public double getRemaining() {
         return this.remaining;
      }

      public CoinFlipGame.CurrencyType getCurrencyType() {
         return this.currencyType;
      }

      public String getCurrencyId() {
         return this.currencyId;
      }

      public String getMessageKey() {
         return this.limitType == BettingLimitManager.LimitType.DAILY ? "betting-limit-daily-exceeded" : "betting-limit-weekly-exceeded";
      }
   }

   public static enum LimitType {
      DAILY,
      WEEKLY;
   }
}
