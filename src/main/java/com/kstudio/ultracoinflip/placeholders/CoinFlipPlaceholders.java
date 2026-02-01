package com.kstudio.ultracoinflip.placeholders;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.PlayerStats;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CoinFlipPlaceholders extends PlaceholderExpansion {
   private final KStudio plugin;
   private final DecimalFormat decimalFormat;
   private final DecimalFormat percentageFormat;
   private final Map<String, CoinFlipPlaceholders.CacheEntry> leaderboardCache = new ConcurrentHashMap<>();
   private static final long CACHE_TTL_MS = 30000L;

   public CoinFlipPlaceholders(KStudio plugin) {
      this.plugin = plugin;
      this.decimalFormat = new DecimalFormat("#.##");
      this.percentageFormat = new DecimalFormat("#.##");
   }

   @NotNull
   public String getIdentifier() {
      return "coinflip";
   }

   @NotNull
   public String getAuthor() {
      return this.plugin.getDescription().getAuthors().toString();
   }

   @NotNull
   public String getVersion() {
      return this.plugin.getDescription().getVersion();
   }

   public boolean persist() {
      return true;
   }

   public String onRequest(OfflinePlayer player, @NotNull String params) {
      if (player == null) {
         return "";
      } else {
         PlayerStats stats = this.plugin.getCoinFlipManager().getStats(player.getUniqueId());
         if (params.equalsIgnoreCase("wins")) {
            return String.valueOf(stats.getWins());
         } else if (params.equalsIgnoreCase("losses") || params.equalsIgnoreCase("defeats")) {
            return String.valueOf(stats.getDefeats());
         } else if (params.equalsIgnoreCase("total_games") || params.equalsIgnoreCase("games")) {
            return String.valueOf(stats.getTotalGames());
         } else if (params.equalsIgnoreCase("winstreak")) {
            return String.valueOf(stats.getWinstreak());
         } else if (params.equalsIgnoreCase("win_percentage") || params.equalsIgnoreCase("winrate")) {
            return this.percentageFormat.format(stats.getWinPercentage());
         } else if (params.equalsIgnoreCase("win_percentage_formatted")) {
            return this.percentageFormat.format(stats.getWinPercentage()) + "%";
         } else if (params.equalsIgnoreCase("winrate_money") || params.equalsIgnoreCase("win_percentage_money")) {
            return this.percentageFormat.format(stats.getWinPercentageMoney());
         } else if (params.equalsIgnoreCase("winrate_money_formatted") || params.equalsIgnoreCase("win_percentage_money_formatted")) {
            return this.percentageFormat.format(stats.getWinPercentageMoney()) + "%";
         } else if (params.equalsIgnoreCase("winrate_playerpoints") || params.equalsIgnoreCase("win_percentage_playerpoints")) {
            return this.percentageFormat.format(stats.getWinPercentagePlayerPoints());
         } else if (params.equalsIgnoreCase("winrate_playerpoints_formatted") || params.equalsIgnoreCase("win_percentage_playerpoints_formatted")) {
            return this.percentageFormat.format(stats.getWinPercentagePlayerPoints()) + "%";
         } else if (params.equalsIgnoreCase("winrate_tokenmanager") || params.equalsIgnoreCase("win_percentage_tokenmanager")) {
            return this.percentageFormat.format(stats.getWinPercentageTokenManager());
         } else if (params.equalsIgnoreCase("winrate_tokenmanager_formatted") || params.equalsIgnoreCase("win_percentage_tokenmanager_formatted")) {
            return this.percentageFormat.format(stats.getWinPercentageTokenManager()) + "%";
         } else if (params.equalsIgnoreCase("winrate_beasttokens") || params.equalsIgnoreCase("win_percentage_beasttokens")) {
            return this.percentageFormat.format(stats.getWinPercentageBeastTokens());
         } else if (params.equalsIgnoreCase("winrate_beasttokens_formatted") || params.equalsIgnoreCase("win_percentage_beasttokens_formatted")) {
            return this.percentageFormat.format(stats.getWinPercentageBeastTokens()) + "%";
         } else if (params.equalsIgnoreCase("profit_money") || params.equalsIgnoreCase("profit_m")) {
            return this.decimalFormat.format(stats.getProfitMoney());
         } else if (params.equalsIgnoreCase("loss_money") || params.equalsIgnoreCase("loss_m")) {
            return this.decimalFormat.format(stats.getLossMoney());
         } else if (params.equalsIgnoreCase("net_profit_money") || params.equalsIgnoreCase("net_m")) {
            return this.decimalFormat.format(stats.getNetProfitMoney());
         } else if (params.equalsIgnoreCase("profit_money_formatted") || params.equalsIgnoreCase("profit_m_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getProfitMoney());
         } else if (params.equalsIgnoreCase("loss_money_formatted") || params.equalsIgnoreCase("loss_m_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getLossMoney());
         } else if (params.equalsIgnoreCase("net_profit_money_formatted")
            || params.equalsIgnoreCase("net_m_formatted")
            || params.equalsIgnoreCase("net_profitt")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getNetProfitMoney());
         } else if (params.equalsIgnoreCase("profit_playerpoints") || params.equalsIgnoreCase("profit_pp")) {
            return this.decimalFormat.format(stats.getProfitPlayerPoints());
         } else if (params.equalsIgnoreCase("loss_playerpoints") || params.equalsIgnoreCase("loss_pp")) {
            return this.decimalFormat.format(stats.getLossPlayerPoints());
         } else if (params.equalsIgnoreCase("net_profit_playerpoints") || params.equalsIgnoreCase("net_pp")) {
            return this.decimalFormat.format(stats.getNetProfitPlayerPoints());
         } else if (params.equalsIgnoreCase("profit_playerpoints_formatted") || params.equalsIgnoreCase("profit_pp_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getProfitPlayerPoints());
         } else if (params.equalsIgnoreCase("loss_playerpoints_formatted") || params.equalsIgnoreCase("loss_pp_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getLossPlayerPoints());
         } else if (params.equalsIgnoreCase("net_profit_playerpoints_formatted") || params.equalsIgnoreCase("net_pp_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getNetProfitPlayerPoints());
         } else if (params.equalsIgnoreCase("profit_tokenmanager") || params.equalsIgnoreCase("profit_tm")) {
            return this.decimalFormat.format(stats.getProfitTokenManager());
         } else if (params.equalsIgnoreCase("loss_tokenmanager") || params.equalsIgnoreCase("loss_tm")) {
            return this.decimalFormat.format(stats.getLossTokenManager());
         } else if (params.equalsIgnoreCase("net_profit_tokenmanager") || params.equalsIgnoreCase("net_tm")) {
            return this.decimalFormat.format(stats.getNetProfitTokenManager());
         } else if (params.equalsIgnoreCase("profit_tokenmanager_formatted") || params.equalsIgnoreCase("profit_tm_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getProfitTokenManager());
         } else if (params.equalsIgnoreCase("loss_tokenmanager_formatted") || params.equalsIgnoreCase("loss_tm_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getLossTokenManager());
         } else if (params.equalsIgnoreCase("net_profit_tokenmanager_formatted") || params.equalsIgnoreCase("net_tm_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getNetProfitTokenManager());
         } else if (params.equalsIgnoreCase("profit_beasttokens") || params.equalsIgnoreCase("profit_bt")) {
            return this.decimalFormat.format(stats.getProfitBeastTokens());
         } else if (params.equalsIgnoreCase("loss_beasttokens") || params.equalsIgnoreCase("loss_bt")) {
            return this.decimalFormat.format(stats.getLossBeastTokens());
         } else if (params.equalsIgnoreCase("net_profit_beasttokens") || params.equalsIgnoreCase("net_bt")) {
            return this.decimalFormat.format(stats.getNetProfitBeastTokens());
         } else if (params.equalsIgnoreCase("profit_beasttokens_formatted") || params.equalsIgnoreCase("profit_bt_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getProfitBeastTokens());
         } else if (params.equalsIgnoreCase("loss_beasttokens_formatted") || params.equalsIgnoreCase("loss_bt_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getLossBeastTokens());
         } else if (params.equalsIgnoreCase("net_profit_beasttokens_formatted") || params.equalsIgnoreCase("net_bt_formatted")) {
            return this.plugin.getGuiHelper().formatAmount(stats.getNetProfitBeastTokens());
         } else if (params.equalsIgnoreCase("total_profit")) {
            return this.decimalFormat
               .format(stats.getProfitMoney() + stats.getProfitPlayerPoints() + stats.getProfitTokenManager() + stats.getProfitBeastTokens());
         } else if (params.equalsIgnoreCase("total_loss")) {
            return this.decimalFormat.format(stats.getLossMoney() + stats.getLossPlayerPoints() + stats.getLossTokenManager() + stats.getLossBeastTokens());
         } else if (params.equalsIgnoreCase("total_net")) {
            return this.decimalFormat
               .format(stats.getNetProfitMoney() + stats.getNetProfitPlayerPoints() + stats.getNetProfitTokenManager() + stats.getNetProfitBeastTokens());
         } else {
            if (params.startsWith("coinsengine_") && params.endsWith("_unit")) {
               String currencyId = params.substring("coinsengine_".length(), params.length() - "_unit".length());
               if (this.plugin.getCurrencyManager().isCoinsEngineCurrencyEnabled(currencyId)) {
                  return this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.COINSENGINE, currencyId);
               }
            }

            if (params.startsWith("coinsengine_") && params.endsWith("_display")) {
               String currencyId = params.substring("coinsengine_".length(), params.length() - "_display".length());
               if (this.plugin.getCurrencyManager().isCoinsEngineCurrencyEnabled(currencyId)) {
                  return this.plugin.getCurrencyManager().getDisplayName(CoinFlipGame.CurrencyType.COINSENGINE, currencyId);
               }
            }

            if (params.startsWith("profit_coinsengine_") || params.startsWith("loss_coinsengine_") || params.startsWith("net_profit_coinsengine_")) {
               return "0";
            } else if (params.startsWith("profit_coinsengine_") && params.endsWith("_formatted")) {
               return "0";
            } else if (params.startsWith("loss_coinsengine_") && params.endsWith("_formatted")) {
               return "0";
            } else if (params.startsWith("net_profit_coinsengine_") && params.endsWith("_formatted")) {
               return "0";
            } else {
               if (params.startsWith("placeholder_") && params.endsWith("_unit")) {
                  String currencyId = params.substring("placeholder_".length(), params.length() - "_unit".length());
                  if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                     return this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyId);
                  }
               }

               if (params.startsWith("placeholder_") && params.endsWith("_display")) {
                  String currencyId = params.substring("placeholder_".length(), params.length() - "_display".length());
                  if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                     return this.plugin.getCurrencyManager().getDisplayName(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyId);
                  }
               }

               if (params.startsWith("placeholder_")) {
                  String remaining = params.substring("placeholder_".length());
                  if (remaining.endsWith("_winrate")) {
                     String currencyId = remaining.substring(0, remaining.length() - "_winrate".length());
                     if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                        return "0";
                     }
                  } else if (remaining.endsWith("_win_percentage")) {
                     String currencyId = remaining.substring(0, remaining.length() - "_win_percentage".length());
                     if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                        double winrate = stats.getWinPercentagePlaceholder(currencyId);
                        return String.format("%.2f", winrate);
                     }
                  } else if (remaining.endsWith("_winrate_formatted")) {
                     String currencyId = remaining.substring(0, remaining.length() - "_winrate_formatted".length());
                     if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                        double winrate = stats.getWinPercentagePlaceholder(currencyId);
                        return String.format("%.2f%%", winrate);
                     }
                  } else if (remaining.endsWith("_win_percentage_formatted")) {
                     String currencyId = remaining.substring(0, remaining.length() - "_win_percentage_formatted".length());
                     if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                        double winrate = stats.getWinPercentagePlaceholder(currencyId);
                        return String.format("%.2f%%", winrate);
                     }
                  }
               }

               if (!params.startsWith("profit_placeholder_") && !params.startsWith("loss_placeholder_") && !params.startsWith("net_profit_placeholder_")) {
                  if (params.startsWith("profit_placeholder_") && params.endsWith("_formatted")) {
                     return "0";
                  } else if (params.startsWith("loss_placeholder_") && params.endsWith("_formatted")) {
                     return "0";
                  } else if (params.startsWith("net_profit_placeholder_") && params.endsWith("_formatted")) {
                     return "0";
                  } else {
                     if (params.startsWith("winrate_placeholder_")) {
                        String currencyId = params.substring("winrate_placeholder_".length());
                        if (currencyId.endsWith("_formatted")) {
                           currencyId = currencyId.substring(0, currencyId.length() - "_formatted".length());
                           if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                              double winrate = stats.getWinPercentagePlaceholder(currencyId);
                              return String.format("%.2f%%", winrate);
                           }
                        } else if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                           double winrate = stats.getWinPercentagePlaceholder(currencyId);
                           return String.format("%.2f", winrate);
                        }
                     }

                     if (params.startsWith("win_percentage_placeholder_")) {
                        String currencyId = params.substring("win_percentage_placeholder_".length());
                        if (currencyId.endsWith("_formatted")) {
                           currencyId = currencyId.substring(0, currencyId.length() - "_formatted".length());
                           if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                              double winrate = stats.getWinPercentagePlaceholder(currencyId);
                              return String.format("%.2f%%", winrate);
                           }
                        } else if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
                           double winrate = stats.getWinPercentagePlaceholder(currencyId);
                           return String.format("%.2f", winrate);
                        }
                     }

                     for (String currencyId : this.plugin.getCurrencyManager().getEnabledPlaceholderCurrencyIds()) {
                        if (params.equalsIgnoreCase(currencyId + "_unit")) {
                           return this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyId);
                        }

                        if (params.equalsIgnoreCase(currencyId + "_display")) {
                           return this.plugin.getCurrencyManager().getDisplayName(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyId);
                        }

                        if (!params.equalsIgnoreCase("profit_" + currencyId)
                           && !params.equalsIgnoreCase("loss_" + currencyId)
                           && !params.equalsIgnoreCase("net_profit_" + currencyId)) {
                           if (!params.equalsIgnoreCase("profit_" + currencyId + "_formatted")
                              && !params.equalsIgnoreCase("loss_" + currencyId + "_formatted")
                              && !params.equalsIgnoreCase("net_profit_" + currencyId + "_formatted")) {
                              if (!params.equalsIgnoreCase("winrate_" + currencyId) && !params.equalsIgnoreCase("win_percentage_" + currencyId)) {
                                 if (!params.equalsIgnoreCase("winrate_" + currencyId + "_formatted")
                                    && !params.equalsIgnoreCase("win_percentage_" + currencyId + "_formatted")) {
                                    continue;
                                 }

                                 double winrate = stats.getWinPercentagePlaceholder(currencyId);
                                 return String.format("%.2f%%", winrate);
                              }

                              double winrate = stats.getWinPercentagePlaceholder(currencyId);
                              return String.format("%.2f", winrate);
                           }

                           return "0";
                        }

                        return "0";
                     }

                     if (params.startsWith("top_")) {
                        String result = this.handleTopPlaceholder(params);
                        if (result != null) {
                           return result;
                        }
                     }

                     return null;
                  }
               } else {
                  return "0";
               }
            }
         }
      }
   }

   private String handleTopPlaceholder(String params) {
      try {
         String remaining = params.substring(4);
         int underscoreIndex = remaining.indexOf(95);
         if (underscoreIndex == -1) {
            return null;
         } else {
            int rank;
            try {
               rank = Integer.parseInt(remaining.substring(0, underscoreIndex));
            } catch (NumberFormatException var14) {
               return null;
            }

            if (rank < 1) {
               return null;
            } else {
               remaining = remaining.substring(underscoreIndex + 1);
               String filterType = null;
               boolean needsCurrency = true;
               if (remaining.startsWith("wins_")) {
                  filterType = "WINS";
                  needsCurrency = false;
                  remaining = remaining.substring(5);
               } else if (remaining.startsWith("profit_")) {
                  filterType = "PROFIT";
                  remaining = remaining.substring(7);
               } else if (remaining.startsWith("largest-win_")) {
                  filterType = "LARGEST_WIN";
                  remaining = remaining.substring(12);
               } else if (remaining.startsWith("largest_win_")) {
                  filterType = "LARGEST_WIN";
                  remaining = remaining.substring(11);
               } else if (remaining.startsWith("worst-profit_")) {
                  filterType = "WORST_PROFIT";
                  remaining = remaining.substring(13);
               } else if (remaining.startsWith("worst_profit_")) {
                  filterType = "WORST_PROFIT";
                  remaining = remaining.substring(12);
               } else {
                  if (!remaining.startsWith("winstreak_")) {
                     return null;
                  }

                  filterType = "WINSTREAK";
                  needsCurrency = false;
                  remaining = remaining.substring(10);
               }

               CoinFlipGame.CurrencyType currencyType = null;
               String currencyId = null;
               if (needsCurrency) {
                  if (remaining.startsWith("money_")) {
                     currencyType = CoinFlipGame.CurrencyType.MONEY;
                     remaining = remaining.substring(6);
                  } else if (remaining.startsWith("playerpoints_")) {
                     currencyType = CoinFlipGame.CurrencyType.PLAYERPOINTS;
                     remaining = remaining.substring(13);
                  } else if (remaining.startsWith("tokenmanager_")) {
                     currencyType = CoinFlipGame.CurrencyType.TOKENMANAGER;
                     remaining = remaining.substring(13);
                  } else if (remaining.startsWith("beasttokens_")) {
                     currencyType = CoinFlipGame.CurrencyType.BEASTTOKENS;
                     remaining = remaining.substring(12);
                  } else if (remaining.startsWith("coinsengine_")) {
                     currencyType = CoinFlipGame.CurrencyType.COINSENGINE;
                     remaining = remaining.substring(12);
                     int nextUnderscore = remaining.indexOf(95);
                     if (nextUnderscore == -1) {
                        return null;
                     }

                     currencyId = remaining.substring(0, nextUnderscore);
                     remaining = remaining.substring(nextUnderscore + 1);
                  } else if (remaining.startsWith("placeholder_")) {
                     currencyType = CoinFlipGame.CurrencyType.PLACEHOLDER;
                     remaining = remaining.substring(11);
                     int nextUnderscore = remaining.indexOf(95);
                     if (nextUnderscore == -1) {
                        return null;
                     }

                     currencyId = remaining.substring(0, nextUnderscore);
                     remaining = remaining.substring(nextUnderscore + 1);
                  } else {
                     int nextUnderscore = remaining.indexOf(95);
                     if (nextUnderscore == -1) {
                        return null;
                     }

                     String possibleCurrencyId = remaining.substring(0, nextUnderscore);
                     if (this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(possibleCurrencyId)) {
                        currencyType = CoinFlipGame.CurrencyType.PLACEHOLDER;
                        currencyId = possibleCurrencyId;
                        remaining = remaining.substring(nextUnderscore + 1);
                     } else {
                        if (!this.plugin.getCurrencyManager().isCoinsEngineCurrencyEnabled(possibleCurrencyId)) {
                           return null;
                        }

                        currencyType = CoinFlipGame.CurrencyType.COINSENGINE;
                        currencyId = possibleCurrencyId;
                        remaining = remaining.substring(nextUnderscore + 1);
                     }
                  }
               }

               boolean wantName = remaining.equals("name");
               boolean wantValue = remaining.equals("value");
               boolean wantValueFormatted = remaining.equals("value_formatted");
               if (!wantName && !wantValue && !wantValueFormatted) {
                  return null;
               } else {
                  List<DatabaseManager.LeaderboardEntry> entries = this.getLeaderboardEntries(filterType, currencyType, currencyId);
                  if (entries != null && !entries.isEmpty() && rank <= entries.size()) {
                     DatabaseManager.LeaderboardEntry entry = entries.get(rank - 1);
                     if (wantName) {
                        return entry.getPlayerName() != null ? entry.getPlayerName() : "Unknown";
                     } else {
                        return wantValueFormatted ? this.plugin.getGuiHelper().formatAmount(entry.getValue()) : this.decimalFormat.format(entry.getValue());
                     }
                  } else {
                     return wantName ? "N/A" : "0";
                  }
               }
            }
         }
      } catch (Exception var15) {
         this.plugin.getLogger().warning("Error parsing top placeholder: " + params + " - " + var15.getMessage());
         return null;
      }
   }

   private List<DatabaseManager.LeaderboardEntry> getLeaderboardEntries(String filterType, CoinFlipGame.CurrencyType currencyType, String currencyId) {
      StringBuilder keyBuilder = new StringBuilder(filterType.length() + currencyType.name().length() + (currencyId != null ? currencyId.length() : 4) + 10);
      keyBuilder.append(filterType).append("_").append(currencyType.name()).append("_");
      keyBuilder.append(currencyId != null ? currencyId : "null");
      String cacheKey = keyBuilder.toString();
      CoinFlipPlaceholders.CacheEntry cached = this.leaderboardCache.get(cacheKey);
      if (cached != null && !cached.isExpired()) {
         return cached.entries;
      } else {
         try {
            new ArrayList();
            int placeholderLimit = 10000;
            List e;
            switch (filterType) {
               case "WINS":
                  e = this.plugin.getDatabaseManager().getTopPlayersByWins(placeholderLimit);
                  break;
               case "PROFIT":
                  e = this.plugin.getDatabaseManager().getTopPlayersByProfit(currencyType, currencyId, placeholderLimit);
                  break;
               case "LARGEST_WIN":
                  e = this.plugin.getDatabaseManager().getTopPlayersByLargestWin(currencyType, currencyId, placeholderLimit);
                  break;
               case "WORST_PROFIT":
                  e = this.plugin.getDatabaseManager().getTopPlayersByWorstProfit(currencyType, currencyId, placeholderLimit);
                  break;
               case "WINSTREAK":
                  e = this.plugin.getDatabaseManager().getTopPlayersByWinstreak(placeholderLimit);
                  break;
               default:
                  return new ArrayList<>();
            }

            this.leaderboardCache.put(cacheKey, new CoinFlipPlaceholders.CacheEntry(e));
            return e;
         } catch (Exception var11) {
            this.plugin.getLogger().warning("Error fetching leaderboard entries: " + var11.getMessage());
            return new ArrayList<>();
         }
      }
   }

   private static class CacheEntry {
      final long timestamp = System.currentTimeMillis();
      final List<DatabaseManager.LeaderboardEntry> entries;

      CacheEntry(List<DatabaseManager.LeaderboardEntry> entries) {
         this.entries = entries;
      }

      boolean isExpired() {
         return System.currentTimeMillis() - this.timestamp > 30000L;
      }
   }
}
