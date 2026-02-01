package com.kstudio.ultracoinflip.data;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.currency.CurrencySettings;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class HouseCoinFlipManager {
   private final KStudio plugin;
   private final Map<UUID, Long> lastPlayTime = new ConcurrentHashMap<>();
   private final Map<UUID, Map<String, Integer>> dailyGameCounts = new ConcurrentHashMap<>();
   private final Map<UUID, HouseCoinFlipManager.PendingHouseGame> pendingGames = new ConcurrentHashMap<>();

   public HouseCoinFlipManager(KStudio plugin) {
      this.plugin = plugin;
   }

   public boolean isEnabled() {
      return this.plugin.getConfig().getBoolean("house.enabled", true);
   }

   public String getSubcommandName() {
      return this.plugin.getConfig().getString("house.subcommand", "bot");
   }

   public String validateHouseGame(Player player, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) {
      if (!this.isEnabled()) {
         return this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("house.disabled");
      } else if (!player.hasPermission("ultracoinflip.house.use")) {
         return this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("house.no-permission");
      } else if (!this.plugin.getCurrencyManager().isCurrencyEnabled(currencyType, currencyId)) {
         return this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.currency-disabled");
      } else {
         CurrencySettings currencySettings = this.plugin.getCurrencyManager().getCurrencySettings(currencyType, currencyId);
         if (currencySettings == null) {
            return this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.currency-disabled");
         } else {
            double minBet = this.parseAmount(this.plugin.getConfig().getString("house.bet.min", "1k"));
            double maxBet = this.parseAmount(this.plugin.getConfig().getString("house.bet.max", "10M"));
            if (amount < minBet) {
               String message = this.plugin.getMessage("house.min-bet");
               String formattedAmount = this.plugin.getGuiHelper().formatAmount(minBet, currencyId);
               message = this.formatMessage(message, "amount", formattedAmount);
               return this.plugin.getMessage("prefix") + " " + message;
            } else if (maxBet > 0.0 && amount > maxBet) {
               String message = this.plugin.getMessage("house.max-bet");
               String formattedAmount = this.plugin.getGuiHelper().formatAmount(maxBet, currencyId);
               message = this.formatMessage(message, "amount", formattedAmount);
               return this.plugin.getMessage("prefix") + " " + message;
            } else {
               double currencyMinBid = currencySettings.getMinBid();
               if (amount < currencyMinBid) {
                  String message = this.plugin.getMessage("command.min-bid");
                  String formattedAmount = this.plugin.getGuiHelper().formatAmount(currencyMinBid, currencyId);
                  message = this.formatMessage(message, "amount", formattedAmount);
                  return this.plugin.getMessage("prefix") + " " + message;
               } else {
                  double currencyMaxBid = currencySettings.getMaxBid();
                  if (currencyMaxBid > 0.0 && amount > currencyMaxBid) {
                     String message = this.plugin.getMessage("command.max-bid");
                     String formattedAmount = this.plugin.getGuiHelper().formatAmount(currencyMaxBid, currencyId);
                     message = this.formatMessage(message, "amount", formattedAmount);
                     return this.plugin.getMessage("prefix") + " " + message;
                  } else if (!this.plugin.getCurrencyManager().hasBalanceWithReserve(player, currencyType, currencyId, amount)) {
                     boolean isReserveIssue = this.plugin.getCurrencyManager().isReserveBalanceIssue(player, currencyType, currencyId, amount);
                     Map<String, String> placeholders = new HashMap<>();
                     String messageKey;
                     if (isReserveIssue) {
                        double currentBalance = this.plugin.getCurrencyManager().getBalance(player, currencyType, currencyId);
                        double minReserve = this.plugin.getCurrencyManager().getMinReserveBalance(currencyType, currencyId);
                        double maxAllowedBet = Math.max(0.0, currentBalance - minReserve);
                        String formattedReserve = this.plugin.getGuiHelper().formatAmount(minReserve, currencyId);
                        String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, currencyId);
                        String formattedMaxBet = this.plugin.getGuiHelper().formatAmount(maxAllowedBet, currencyId);
                        if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
                           messageKey = "command.not-enough-money-reserve";
                        } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                           messageKey = "command.not-enough-playerpoints-reserve";
                        } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                           messageKey = "command.not-enough-tokenmanager-reserve";
                        } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                           messageKey = "command.not-enough-beasttokens-reserve";
                        } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                           messageKey = "command.not-enough-coinsengine-reserve";
                        } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                           messageKey = "command.not-enough-placeholder-reserve";
                        } else {
                           messageKey = "command.not-enough-money-reserve";
                        }

                        placeholders.put("reserve", formattedReserve);
                        placeholders.put("balance", formattedBalance);
                        placeholders.put("max_bet", formattedMaxBet);
                     } else {
                        if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
                           messageKey = "command.not-enough-money";
                        } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                           messageKey = "command.not-enough-playerpoints";
                        } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                           messageKey = "command.not-enough-tokenmanager";
                        } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                           messageKey = "command.not-enough-beasttokens";
                        } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                           messageKey = "command.not-enough-coinsengine";
                        } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                           messageKey = "command.not-enough-placeholder";
                        } else {
                           messageKey = "command.not-enough-money";
                        }

                        double currentBalance = this.plugin.getCurrencyManager().getBalance(player, currencyType, currencyId);
                        String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, currencyId);
                        String formattedAmount = this.plugin.getGuiHelper().formatAmount(amount, currencyId);
                        placeholders.put("amount", formattedAmount);
                        placeholders.put("balance", formattedBalance);
                     }

                     String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                     return this.formatMessage(message, placeholders);
                  } else {
                     if (!player.hasPermission("ultracoinflip.house.bypass.delay")) {
                        long delaySeconds = this.plugin.getConfig().getLong("house.limits.delay-between-games", 10L);
                        if (delaySeconds > 0L) {
                           Long lastPlay = this.lastPlayTime.get(player.getUniqueId());
                           if (lastPlay != null) {
                              long timeSinceLastPlay = (System.currentTimeMillis() - lastPlay) / 1000L;
                              if (timeSinceLastPlay < delaySeconds) {
                                 long remaining = delaySeconds - timeSinceLastPlay;
                                 String message = this.plugin.getMessage("house.delay-remaining");
                                 message = this.formatMessage(message, "time", String.valueOf(remaining));
                                 return this.plugin.getMessage("prefix") + " " + message;
                              }
                           }
                        }
                     }

                     if (!player.hasPermission("ultracoinflip.house.bypass.limit")) {
                        int maxGamesPerDay = this.plugin.getConfig().getInt("house.limits.max-games-per-day", 50);
                        if (maxGamesPerDay > 0) {
                           int todayCount = this.getTodayGameCount(player.getUniqueId());
                           if (todayCount >= maxGamesPerDay) {
                              String message = this.plugin.getMessage("house.daily-limit-reached");
                              message = this.formatMessage(message, "limit", String.valueOf(maxGamesPerDay));
                              return this.plugin.getMessage("prefix") + " " + message;
                           }
                        }
                     }

                     return null;
                  }
               }
            }
         }
      }
   }

   private int getTodayGameCount(UUID playerUuid) {
      String today = this.getTodayString();
      Map<String, Integer> playerCounts = this.dailyGameCounts.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
      playerCounts.entrySet().removeIf(entry -> !entry.getKey().equals(today) && !entry.getKey().equals(this.getYesterdayString()));
      return playerCounts.getOrDefault(today, 0);
   }

   private void incrementTodayGameCount(UUID playerUuid) {
      String today = this.getTodayString();
      Map<String, Integer> playerCounts = this.dailyGameCounts.computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>());
      playerCounts.put(today, playerCounts.getOrDefault(today, 0) + 1);
   }

   private String getTodayString() {
      Calendar cal = Calendar.getInstance();
      return String.format("%04d-%02d-%02d", cal.get(1), cal.get(2) + 1, cal.get(5));
   }

   private String getYesterdayString() {
      Calendar cal = Calendar.getInstance();
      cal.add(5, -1);
      return String.format("%04d-%02d-%02d", cal.get(1), cal.get(2) + 1, cal.get(5));
   }

   private String formatMessage(String message, String key, String value) {
      if (message == null) {
         return "";
      } else {
         message = message.replace("<" + key + ">", value);
         return message.replace("%" + key + "%", value);
      }
   }

   private String formatMessage(String message, Map<String, String> placeholders) {
      if (message == null) {
         return "";
      } else if (placeholders != null && !placeholders.isEmpty()) {
         for (Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            message = message.replace("<" + key + ">", value);
            message = message.replace("%" + key + "%", value);
         }

         return message;
      } else {
         return message;
      }
   }

   private double parseAmount(String amountStr) {
      if (amountStr != null && !amountStr.isEmpty()) {
         amountStr = amountStr.trim().toUpperCase();

         try {
            return Double.parseDouble(amountStr);
         } catch (NumberFormatException var5) {
            if (amountStr.endsWith("K")) {
               double value = Double.parseDouble(amountStr.substring(0, amountStr.length() - 1));
               return value * 1000.0;
            } else if (amountStr.endsWith("M")) {
               double value = Double.parseDouble(amountStr.substring(0, amountStr.length() - 1));
               return value * 1000000.0;
            } else if (amountStr.endsWith("B")) {
               double value = Double.parseDouble(amountStr.substring(0, amountStr.length() - 1));
               return value * 1.0E9;
            } else if (amountStr.endsWith("T")) {
               double value = Double.parseDouble(amountStr.substring(0, amountStr.length() - 1));
               return value * 1.0E12;
            } else {
               return 0.0;
            }
         }
      } else {
         return 0.0;
      }
   }

   public void recordHouseGamePlay(UUID playerUuid) {
      this.lastPlayTime.put(playerUuid, System.currentTimeMillis());
      this.incrementTodayGameCount(playerUuid);
   }

   public void addPendingGame(UUID playerUuid, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) {
      this.pendingGames.put(playerUuid, new HouseCoinFlipManager.PendingHouseGame(playerUuid, currencyType, currencyId, amount, System.currentTimeMillis()));
   }

   public void removePendingGame(UUID playerUuid) {
      this.pendingGames.remove(playerUuid);
   }

   public Collection<HouseCoinFlipManager.PendingHouseGame> getPendingGames() {
      return new ArrayList<>(this.pendingGames.values());
   }

   public void clearPendingGames() {
      this.pendingGames.clear();
   }

   public static class PendingHouseGame {
      private final UUID playerUuid;
      private final CoinFlipGame.CurrencyType currencyType;
      private final String currencyId;
      private final double amount;
      private final long timestamp;

      public PendingHouseGame(UUID playerUuid, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount, long timestamp) {
         this.playerUuid = playerUuid;
         this.currencyType = currencyType;
         this.currencyId = currencyId;
         this.amount = amount;
         this.timestamp = timestamp;
      }

      public UUID getPlayerUuid() {
         return this.playerUuid;
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

      public long getTimestamp() {
         return this.timestamp;
      }
   }
}
