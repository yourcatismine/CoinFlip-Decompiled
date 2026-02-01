package com.kstudio.ultracoinflip.database;

import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.CoinFlipLog;
import com.kstudio.ultracoinflip.data.PlayerSettings;
import com.kstudio.ultracoinflip.data.PlayerStats;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

public interface DatabaseManager {
   void initialize() throws Exception;

   void close() throws Exception;

   Connection getConnection() throws Exception;

   PlayerStats loadPlayerStats(UUID var1) throws Exception;

   void savePlayerStats(UUID var1, PlayerStats var2) throws Exception;

   void saveBackup(UUID var1, String var2, double var3) throws Exception;

   DatabaseManager.BackupData loadBackup(UUID var1) throws Exception;

   void removeBackup(UUID var1) throws Exception;

   List<UUID> getAllBackups() throws Exception;

   void saveCoinFlipLog(CoinFlipLog var1) throws Exception;

   List<CoinFlipLog> getPlayerLogs(UUID var1, int var2) throws Exception;

   List<CoinFlipLog> getAllLogs(int var1) throws Exception;

   List<CoinFlipLog> getLogsByTimeRange(long var1, long var3, int var5) throws Exception;

   void saveWaitingGameBackup(UUID var1, UUID var2, CoinFlipGame.CurrencyType var3, String var4, double var5) throws Exception;

   void removeWaitingGameBackup(UUID var1) throws Exception;

   List<DatabaseManager.WaitingGameBackup> loadWaitingGameBackups(UUID var1) throws Exception;

   List<DatabaseManager.WaitingGameBackup> loadAllWaitingGameBackups() throws Exception;

   int deleteOldLogs(long var1) throws Exception;

   List<DatabaseManager.LeaderboardEntry> getTopPlayersByWins(int var1) throws Exception;

   List<DatabaseManager.LeaderboardEntry> getTopPlayersByProfit(CoinFlipGame.CurrencyType var1, String var2, int var3) throws Exception;

   List<DatabaseManager.LeaderboardEntry> getTopPlayersByLargestWin(CoinFlipGame.CurrencyType var1, String var2, int var3) throws Exception;

   List<DatabaseManager.LeaderboardEntry> getTopPlayersByWorstProfit(CoinFlipGame.CurrencyType var1, String var2, int var3) throws Exception;

   List<DatabaseManager.LeaderboardEntry> getTopPlayersByWinstreak(int var1) throws Exception;

   DatabaseManager.BotGameStats getPlayerBotStats(UUID var1) throws Exception;

   PlayerSettings loadPlayerSettings(UUID var1) throws Exception;

   void savePlayerSettings(UUID var1, PlayerSettings var2) throws Exception;

   public static class BackupData {
      private final String currencyType;
      private final double amount;

      public BackupData(String currencyType, double amount) {
         this.currencyType = currencyType;
         this.amount = amount;
      }

      public String getCurrencyType() {
         return this.currencyType;
      }

      public double getAmount() {
         return this.amount;
      }
   }

   public static class BotGameStats {
      private final int wins;
      private final int totalGames;
      private final double profit;

      public BotGameStats(int wins, int totalGames, double profit) {
         this.wins = wins;
         this.totalGames = totalGames;
         this.profit = profit;
      }

      public int getWins() {
         return this.wins;
      }

      public int getTotalGames() {
         return this.totalGames;
      }

      public double getProfit() {
         return this.profit;
      }

      public double getWinRate() {
         return this.totalGames == 0 ? 0.0 : this.wins * 100.0 / this.totalGames;
      }
   }

   public static class LeaderboardEntry {
      private final UUID uuid;
      private final String playerName;
      private final double value;

      public LeaderboardEntry(UUID uuid, String playerName, double value) {
         this.uuid = uuid;
         this.playerName = playerName;
         this.value = value;
      }

      public UUID getUuid() {
         return this.uuid;
      }

      public String getPlayerName() {
         return this.playerName;
      }

      public double getValue() {
         return this.value;
      }
   }

   public static class WaitingGameBackup {
      private final UUID gameId;
      private final UUID ownerUuid;
      private final CoinFlipGame.CurrencyType currencyType;
      private final String currencyId;
      private final double amount;
      private final long createdAt;

      public WaitingGameBackup(UUID gameId, UUID ownerUuid, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount, long createdAt) {
         this.gameId = gameId;
         this.ownerUuid = ownerUuid;
         this.currencyType = currencyType;
         this.currencyId = currencyId;
         this.amount = amount;
         this.createdAt = createdAt;
      }

      public UUID getGameId() {
         return this.gameId;
      }

      public UUID getOwnerUuid() {
         return this.ownerUuid;
      }

      public CoinFlipGame.CurrencyType getCurrencyType() {
         return this.currencyType;
      }

      public String getCurrencyId() {
         return this.currencyId;
      }

      public double getAmount() {
         return this.amount;
      }

      public long getCreatedAt() {
         return this.createdAt;
      }
   }
}
