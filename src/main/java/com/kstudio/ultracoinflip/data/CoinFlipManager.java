package com.kstudio.ultracoinflip.data;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.InventoryHandler;
import com.kstudio.ultracoinflip.gui.impl.CoinFlipListGUI;
import com.kstudio.ultracoinflip.gui.impl.CoinFlipRollGUI;
import com.kstudio.ultracoinflip.refund.RefundLimiter;
import com.kstudio.ultracoinflip.refund.RefundResult;
import com.kstudio.ultracoinflip.refund.TransactionLogger;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.LegacyCompatibility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class CoinFlipManager {
   private final KStudio plugin;
   private final DatabaseManager databaseManager;
   private final Map<UUID, CoinFlipGame> activeGamesById = new ConcurrentHashMap<>();
   private final Map<UUID, Set<UUID>> hostGameIndex = new ConcurrentHashMap<>();
   private final Map<UUID, PlayerStats> playerStatsCache = new ConcurrentHashMap<>();
   private final Map<UUID, UUID> activeRollingGames = new ConcurrentHashMap<>();
   private final Map<UUID, Long> rollingGameStartTimes = new ConcurrentHashMap<>();
   private final Map<UUID, Map<UUID, DatabaseManager.WaitingGameBackup>> pendingWaitingRefunds = new ConcurrentHashMap<>();
   private final Object statsLock = new Object();
   private final Object rollingGamesLock = new Object();
   private final Set<UUID> gamesBeingRefunded = ConcurrentHashMap.newKeySet();
   private final Set<UUID> playersInRefundTransaction = ConcurrentHashMap.newKeySet();
   private final Map<UUID, Long> refundCooldowns = new ConcurrentHashMap<>();
   private static final long REFUND_COOLDOWN_MS = 3000L;
   private final Map<UUID, ReentrantLock> refundLocks = new ConcurrentHashMap<>();
   private final Set<UUID> rollingGamesBeingRefunded = ConcurrentHashMap.newKeySet();
   private final Map<String, Integer> consecutiveWinsMap = new ConcurrentHashMap<>();
   private final Object consecutiveWinsLock = new Object();
   private StatsBatchQueue statsBatchQueue;
   private boolean batchModeEnabled = true;
   private volatile boolean cachedRefundOnDisconnect;
   private volatile boolean cachedRefundOnReconnectAfterShutdown;
   private volatile boolean cachedCancelGameOnDisconnect;
   private volatile boolean cachedKeepCoinflipOnDisconnect;
   private volatile boolean cachedAllowCloseDuringAnimation;
   private final AtomicInteger gamesListVersion = new AtomicInteger(0);
   private volatile List<CoinFlipGame> cachedGamesList = null;
   private volatile int cachedGamesListVersion = -1;
   private final Object gamesListCacheLock = new Object();

   public CoinFlipManager(KStudio plugin, DatabaseManager databaseManager) {
      this.plugin = plugin;
      this.databaseManager = databaseManager;
      this.cacheConfigValues();

      try {
         boolean enabled = plugin.getConfigManager().getConfig().getBoolean("performance.batch-stats-save.enabled",
               true);
         this.batchModeEnabled = enabled;
         if (this.batchModeEnabled) {
            this.statsBatchQueue = new StatsBatchQueue(plugin, databaseManager);
            FoliaScheduler.runTaskTimer(plugin, () -> {
               if (this.statsBatchQueue != null) {
                  this.statsBatchQueue.checkAutoFlush();
               }
            }, 40L, 40L);
            if (plugin.getDebugManager() != null
                  && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.DATABASE)) {
               plugin.getDebugManager().info(DebugManager.Category.DATABASE,
                     "Stats batch queue enabled for optimized database performance");
            }
         }
      } catch (Exception var4) {
         plugin.getLogger().warning("Failed to initialize batch queue, using individual saves: " + var4.getMessage());
         this.batchModeEnabled = false;
      }

      FoliaScheduler.runTaskTimer(plugin, () -> {
         int removed = 0;
         Iterator<Entry<UUID, Map<UUID, DatabaseManager.WaitingGameBackup>>> it = this.pendingWaitingRefunds.entrySet()
               .iterator();

         while (it.hasNext()) {
            Entry<UUID, Map<UUID, DatabaseManager.WaitingGameBackup>> entry = it.next();
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
               it.remove();
               removed++;
            }
         }

         if (removed > 0 && plugin.getDebugManager() != null
               && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.DATABASE)) {
            plugin.getDebugManager().info(DebugManager.Category.DATABASE,
                  "Cleaned up " + removed + " empty pending refund entries");
         }

         long cutoffTime = System.currentTimeMillis() - 600000L;
         this.refundCooldowns.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
         this.refundLocks.entrySet().removeIf(entry -> !entry.getValue().isLocked());
      }, 36000L, 36000L);

      // Stale rolling games cleanup task (every 5 minutes)
      FoliaScheduler.runTaskTimer(plugin, () -> {
         long now = System.currentTimeMillis();
         long staleCutoff = now - 300000L; // 5 minutes
         int cleaned = 0;

         synchronized (this.rollingGamesLock) {
            Iterator<Entry<UUID, Long>> it = this.rollingGameStartTimes.entrySet().iterator();
            while (it.hasNext()) {
               Entry<UUID, Long> entry = it.next();
               if (entry.getValue() < staleCutoff) {
                  UUID playerUUID = entry.getKey();
                  UUID partnerUUID = this.activeRollingGames.get(playerUUID);

                  this.activeRollingGames.remove(playerUUID);
                  if (partnerUUID != null) {
                     this.activeRollingGames.remove(partnerUUID);
                     this.rollingGameStartTimes.remove(partnerUUID);
                  }
                  it.remove();
                  cleaned++;
               }
            }
         }

         if (cleaned > 0 && plugin.getDebugManager() != null
               && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GAME)) {
            plugin.getDebugManager().info(DebugManager.Category.GAME,
                  "Cleaned up " + cleaned + " stale rolling games (active for >5m)");
         }
      }, 6000L, 6000L);
   }

   private void cacheConfigValues() {
      this.cachedRefundOnDisconnect = this.plugin.getConfig().getBoolean("game-behavior.refund-on-disconnect", false);
      this.cachedRefundOnReconnectAfterShutdown = this.plugin.getConfig()
            .getBoolean("game-behavior.refund-on-reconnect-after-shutdown", true);
      this.cachedCancelGameOnDisconnect = this.plugin.getConfig().getBoolean("game-behavior.cancel-game-on-disconnect",
            false);
      this.cachedKeepCoinflipOnDisconnect = this.plugin.getConfig()
            .getBoolean("game-behavior.keep-coinflip-on-disconnect", false);
      this.cachedAllowCloseDuringAnimation = this.plugin.getConfig()
            .getBoolean("game-behavior.allow-close-during-animation", true);
   }

   public void reloadConfigCache() {
      this.cacheConfigValues();
   }

   public boolean isCancelGameOnDisconnect() {
      return this.cachedCancelGameOnDisconnect;
   }

   public boolean isRefundOnDisconnect() {
      return this.cachedRefundOnDisconnect;
   }

   public boolean isKeepCoinflipOnDisconnect() {
      return this.cachedKeepCoinflipOnDisconnect;
   }

   public boolean isAllowCloseDuringAnimation() {
      return this.cachedAllowCloseDuringAnimation;
   }

   public void createGame(Player player, CoinFlipGame.CurrencyType type, double amount) {
      this.createGame(player, type, null, amount);
   }

   public void createGame(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      this.createGame(player, type, currencyId, amount, null);
   }

   public void createGame(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount,
         Boolean headsChoice) {
      CoinFlipGame game = new CoinFlipGame(UUID.randomUUID(), player, type, currencyId, amount,
            System.currentTimeMillis(), headsChoice);
      this.addActiveGame(game);
      if (this.plugin.getDebugManager() != null
            && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GAME)) {
         String currencyInfo = currencyId != null ? currencyId : type.name();
         String headsTailsInfo = headsChoice != null ? (headsChoice ? "Heads" : "Tails") : "None";
         this.plugin
               .getDebugManager()
               .info(
                     DebugManager.Category.GAME,
                     String.format("Game created: Player=%s, Currency=%s, Amount=%.2f, Choice=%s", player.getName(),
                           currencyInfo, amount, headsTailsInfo));
      }

      CoinFlipListGUI.refreshAllViewers(this.plugin);
   }

   private void updateStatsForCurrency(PlayerStats stats, CoinFlipGame.CurrencyType currencyType, String currencyId,
         boolean isWin, double amount) {
      if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
         if (isWin) {
            stats.setProfitMoney(stats.getProfitMoney() + Math.max(0.0, amount));
            stats.setWinsMoney(stats.getWinsMoney() + 1);
         } else {
            stats.setLossMoney(stats.getLossMoney() + amount);
            stats.setDefeatsMoney(stats.getDefeatsMoney() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
         if (isWin) {
            stats.setProfitPlayerPoints(stats.getProfitPlayerPoints() + Math.max(0.0, amount));
            stats.setWinsPlayerPoints(stats.getWinsPlayerPoints() + 1);
         } else {
            stats.setLossPlayerPoints(stats.getLossPlayerPoints() + amount);
            stats.setDefeatsPlayerPoints(stats.getDefeatsPlayerPoints() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
         if (isWin) {
            stats.setProfitTokenManager(stats.getProfitTokenManager() + Math.max(0.0, amount));
            stats.setWinsTokenManager(stats.getWinsTokenManager() + 1);
         } else {
            stats.setLossTokenManager(stats.getLossTokenManager() + amount);
            stats.setDefeatsTokenManager(stats.getDefeatsTokenManager() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
         if (isWin) {
            stats.setProfitBeastTokens(stats.getProfitBeastTokens() + Math.max(0.0, amount));
            stats.setWinsBeastTokens(stats.getWinsBeastTokens() + 1);
         } else {
            stats.setLossBeastTokens(stats.getLossBeastTokens() + amount);
            stats.setDefeatsBeastTokens(stats.getDefeatsBeastTokens() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER && currencyId != null) {
         if (isWin) {
            stats.incrementWinsPlaceholder(currencyId);
         } else {
            stats.incrementDefeatsPlaceholder(currencyId);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE && currencyId != null) {
         if (isWin) {
            stats.incrementWinsPlaceholder(currencyId);
         } else {
            stats.incrementDefeatsPlaceholder(currencyId);
         }
      }
   }

   private void addActiveGame(CoinFlipGame game) {
      this.activeGamesById.put(game.getGameId(), game);
      UUID hostUuid = game.getHostUuid();
      if (hostUuid == null && game.getHost() != null) {
         hostUuid = game.getHost().getUniqueId();
      }

      if (hostUuid != null) {
         this.hostGameIndex.computeIfAbsent(hostUuid, id -> ConcurrentHashMap.newKeySet()).add(game.getGameId());
         this.invalidateGamesCache();
         this.persistWaitingGame(game);
      }
   }

   public CoinFlipGame removeGame(UUID gameId) {
      CoinFlipGame game = this.activeGamesById.remove(gameId);
      if (game == null) {
         return null;
      } else {
         UUID hostUuid = game.getHostUuid();
         if (hostUuid == null && game.getHost() != null) {
            hostUuid = game.getHost().getUniqueId();
         }

         if (hostUuid != null) {
            Set<UUID> hostGames = this.hostGameIndex.get(hostUuid);
            if (hostGames != null) {
               hostGames.remove(gameId);
               if (hostGames.isEmpty()) {
                  this.hostGameIndex.remove(hostUuid);
               }
            }
         }

         this.invalidateGamesCache();
         if (this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GAME)) {
            String playerName;
            if (game.getHost() != null) {
               playerName = game.getHost().getName();
            } else if (hostUuid != null) {
               playerName = hostUuid.toString();
            } else {
               playerName = gameId.toString();
            }

            this.plugin.getDebugManager().verbose(DebugManager.Category.GAME,
                  String.format("Game removed: Player=%s", playerName));
         }

         CoinFlipListGUI.refreshAllViewers(this.plugin);
         return game;
      }
   }

   public CoinFlipGame takeGameForRoll(UUID gameId) {
      CoinFlipGame game = this.removeGame(gameId);
      if (game != null) {
         this.deleteWaitingBackup(gameId);
      }

      return game;
   }

   public void restoreGameAfterFailedJoin(CoinFlipGame game) {
      if (game != null) {
         this.addActiveGame(game);
         this.plugin
               .getLogger()
               .info(
                     String.format(
                           "[JOIN ROLLBACK] Restored game %s after failed join. Host: %s, Amount: %.2f",
                           game.getGameId(),
                           game.getHost() != null ? game.getHost().getName() : game.getHostUuid(),
                           game.getAmount()));
         if (this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GAME)) {
            this.plugin
                  .getDebugManager()
                  .info(
                        DebugManager.Category.GAME,
                        String.format(
                              "Game restored after failed join: GameId=%s, Host=%s", game.getGameId(),
                              game.getHost() != null ? game.getHost().getName() : "unknown"));
         }
      }
   }

   public void initializeDatabase() {
      if (this.plugin.getDebugManager() != null) {
         this.plugin.getDebugManager().startPerformanceTracking("database_initialize");
      }

      try {
         this.databaseManager.initialize();
         this.loadBackups();
         this.loadWaitingGameBackups();
         this.plugin.getLogger().info("Database initialized successfully!");
         if (this.plugin.getDebugManager() != null) {
            this.plugin.getDebugManager().endPerformanceTracking("database_initialize");
            this.plugin.getDebugManager().info(DebugManager.Category.DATABASE, "Database initialized successfully");
         }
      } catch (Exception var2) {
         this.plugin.getLogger().severe("Failed to initialize database: " + var2.getMessage());
         if (this.plugin.getDebugManager() != null) {
            this.plugin.getDebugManager().error(DebugManager.Category.DATABASE,
                  "Failed to initialize database: " + var2.getMessage(), var2);
         } else {
            var2.printStackTrace();
         }
      }
   }

   public CoinFlipGame getGame(UUID uuid) {
      Set<UUID> games = this.hostGameIndex.get(uuid);
      if (games != null && !games.isEmpty()) {
         UUID firstGameId = null;

         try {
            firstGameId = games.iterator().next();
         } catch (NoSuchElementException var7) {
            return null;
         }

         if (firstGameId == null) {
            return null;
         } else {
            CoinFlipGame game = this.activeGamesById.get(firstGameId);
            if (game != null) {
               return game;
            } else {
               games = this.hostGameIndex.get(uuid);
               if (games != null && !games.isEmpty()) {
                  for (UUID gameId : games) {
                     game = this.activeGamesById.get(gameId);
                     if (game != null) {
                        return game;
                     }
                  }
               }

               return null;
            }
         }
      } else {
         return null;
      }
   }

   public CoinFlipGame getGameById(UUID gameId) {
      return gameId == null ? null : this.activeGamesById.get(gameId);
   }

   public List<CoinFlipGame> getAllGames() {
      int currentVersion = this.gamesListVersion.get();
      if (this.cachedGamesList != null && this.cachedGamesListVersion == currentVersion) {
         return this.cachedGamesList;
      } else {
         Set<UUID> rollingGameUuids;
         synchronized (this.rollingGamesLock) {
            rollingGameUuids = new HashSet<>(this.activeRollingGames.keySet());
         }

         synchronized (this.gamesListCacheLock) {
            if (this.cachedGamesList != null && this.cachedGamesListVersion == this.gamesListVersion.get()) {
               return this.cachedGamesList;
            } else {
               List<CoinFlipGame> newList = new ArrayList<>();

               for (CoinFlipGame game : this.activeGamesById.values()) {
                  if (game != null) {
                     Player host = game.getHost();
                     if (host == null || !rollingGameUuids.contains(host.getUniqueId())) {
                        newList.add(game);
                     }
                  }
               }

               newList.sort(Comparator.comparingLong(CoinFlipGame::getCreatedAt));
               this.cachedGamesList = Collections.unmodifiableList(newList);
               this.cachedGamesListVersion = this.gamesListVersion.get();
               return this.cachedGamesList;
            }
         }
      }
   }

   private void invalidateGamesCache() {
      this.gamesListVersion.incrementAndGet();
   }

   public int getGamesListVersion() {
      return this.gamesListVersion.get();
   }

   public PlayerStats getStats(UUID uuid) {
      PlayerStats cached = this.playerStatsCache.get(uuid);
      if (cached != null) {
         return this.createStatsCopy(cached);
      } else {
         synchronized (this.statsLock) {
            cached = this.playerStatsCache.get(uuid);
            if (cached != null) {
               return this.createStatsCopy(cached);
            } else {
               PlayerStats defaultStats = new PlayerStats();
               this.playerStatsCache.put(uuid, defaultStats);
               FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
                  try {
                     PlayerStats loadedStats = this.databaseManager.loadPlayerStats(uuid);
                     if (loadedStats != null) {
                        synchronized (this.statsLock) {
                           this.playerStatsCache.put(uuid, loadedStats);
                        }
                     }
                  } catch (Exception var6) {
                     this.plugin.getLogger().warning("Failed to load stats for " + uuid + ": " + var6.getMessage());
                  }
               });
               return this.createStatsCopy(defaultStats);
            }
         }
      }
   }

   private PlayerStats createStatsCopy(PlayerStats source) {
      return source == null ? new PlayerStats() : new PlayerStats(source);
   }

   public void saveStats(UUID uuid, PlayerStats stats) {
      PlayerStats cached = this.createStatsCopy(stats);
      synchronized (this.statsLock) {
         this.playerStatsCache.put(uuid, cached);
      }

      if (this.plugin.getDebugManager() != null
            && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.DATABASE)) {
         this.plugin
               .getDebugManager()
               .verbose(
                     DebugManager.Category.DATABASE,
                     String.format("Saving stats for player: %s (Wins: %d, Defeats: %d)", uuid, stats.getWins(),
                           stats.getDefeats()));
      }

      if (this.batchModeEnabled && this.statsBatchQueue != null) {
         this.statsBatchQueue.queueSave(uuid, cached);
      } else {
         FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
            try {
               if (this.plugin.getDebugManager() != null) {
                  this.plugin.getDebugManager().startPerformanceTracking("save_player_stats");
               }

               this.databaseManager.savePlayerStats(uuid, cached);
               if (this.plugin.getDebugManager() != null) {
                  this.plugin.getDebugManager().endPerformanceTracking("save_player_stats");
               }
            } catch (Exception var4) {
               this.plugin.getLogger().severe("Failed to save stats for " + uuid + ": " + var4.getMessage());
               if (this.plugin.getDebugManager() != null) {
                  this.plugin.getDebugManager().error(DebugManager.Category.DATABASE,
                        "Failed to save stats for " + uuid + ": " + var4.getMessage(), var4);
               } else {
                  var4.printStackTrace();
               }
            }
         });
      }
   }

   public boolean hasActiveGame(UUID uuid) {
      Set<UUID> games = this.hostGameIndex.get(uuid);
      return games != null && !games.isEmpty();
   }

   public int getActiveGameCount(UUID uuid) {
      Set<UUID> games = this.hostGameIndex.get(uuid);
      return games != null ? games.size() : 0;
   }

   public boolean canCreateMoreGames(Player player) {
      int maxGames = this.getMaxAllowedGames(player);
      return maxGames < 0 ? true : this.getActiveGameCount(player.getUniqueId()) < maxGames;
   }

   public int getMaxAllowedGames(Player player) {
      if (player == null) {
         return 1;
      } else {
         FileConfiguration config = this.plugin.getConfig();
         boolean enabled = config.getBoolean("game-behavior.multiple-games.enabled", false);
         if (!enabled) {
            return 1;
         } else {
            int defaultLimit = config.getInt("game-behavior.multiple-games.default-limit", 1);
            int resolvedLimit = Math.max(1, defaultLimit);
            List<Map<?, ?>> entries = config.getMapList("game-behavior.multiple-games.permission-limits");
            if (entries != null) {
               for (Map<?, ?> entry : entries) {
                  if (entry != null) {
                     Object permObj = entry.get("permission");
                     if (permObj instanceof String) {
                        String permission = ((String) permObj).trim();
                        if (!permission.isEmpty() && player.hasPermission(permission)) {
                           int permLimit = resolvedLimit;
                           Object limitObj = entry.get("limit");
                           if (limitObj instanceof Number) {
                              permLimit = ((Number) limitObj).intValue();
                           } else if (limitObj instanceof String) {
                              try {
                                 permLimit = Integer.parseInt(((String) limitObj).trim());
                              } catch (NumberFormatException var14) {
                              }
                           }

                           if (permLimit < 0) {
                              return -1;
                           }

                           resolvedLimit = Math.max(resolvedLimit, permLimit);
                        }
                     }
                  }
               }
            }

            return resolvedLimit;
         }
      }
   }

   public boolean isOnRefundCooldown(UUID playerUuid) {
      if (playerUuid == null) {
         return false;
      } else {
         Long lastRefund = this.refundCooldowns.get(playerUuid);
         return lastRefund == null ? false : System.currentTimeMillis() - lastRefund < 3000L;
      }
   }

   public int getRemainingCooldown(UUID playerUuid) {
      if (playerUuid == null) {
         return 0;
      } else {
         Long lastRefund = this.refundCooldowns.get(playerUuid);
         if (lastRefund == null) {
            return 0;
         } else {
            long remaining = 3000L - (System.currentTimeMillis() - lastRefund);
            return remaining > 0L ? (int) Math.ceil(remaining / 1000.0) : 0;
         }
      }
   }

   private ReentrantLock getRefundLock(UUID gameId) {
      return this.refundLocks.computeIfAbsent(gameId, id -> new ReentrantLock());
   }

   public boolean refundGame(Player player, UUID gameId) {
      if (player != null && gameId != null) {
         RefundLimiter limiter = this.plugin.getRefundLimiter();
         TransactionLogger txLogger = this.plugin.getTransactionLogger();
         if (limiter == null || !limiter.isGlobalLocked() && !limiter.isReloading()) {
            ReentrantLock lock = this.getRefundLock(gameId);
            if (!lock.tryLock()) {
               if (txLogger != null) {
                  txLogger.logBlocked(player.getName(), RefundResult.BLOCKED,
                        "Game " + gameId + " already being processed");
               }

               return false;
            } else {
               CoinFlipGame game = null;
               UUID hostUuid = null;

               boolean e;
               try {
                  if (!this.gamesBeingRefunded.add(gameId)) {
                     if (txLogger != null) {
                        txLogger.logBlocked(player.getName(), RefundResult.BLOCKED,
                              "Duplicate refund attempt for game " + gameId);
                     }

                     return false;
                  }

                  if (limiter != null && !limiter.tryLockGame(gameId)) {
                     this.gamesBeingRefunded.remove(gameId);
                     if (txLogger != null) {
                        txLogger.logBlocked(player.getName(), RefundResult.BLOCKED,
                              "Limiter lock failed for game " + gameId);
                     }

                     return false;
                  }

                  try {
                     game = this.activeGamesById.remove(gameId);
                     if (game == null) {
                        if (txLogger != null) {
                           txLogger.logRefund(
                                 player.getName(), player.getUniqueId(), gameId, 0.0, CoinFlipGame.CurrencyType.MONEY,
                                 null, RefundResult.NOT_FOUND);
                        }

                        return false;
                     }

                     hostUuid = game.getHostUuid();
                     if (hostUuid != null) {
                        Set<UUID> hostGames = this.hostGameIndex.get(hostUuid);
                        if (hostGames != null) {
                           hostGames.remove(gameId);
                           if (hostGames.isEmpty()) {
                              this.hostGameIndex.remove(hostUuid);
                           }
                        }
                     }

                     this.invalidateGamesCache();
                     if (this.plugin.getDebugManager() != null
                           && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GAME)) {
                        this.plugin
                              .getDebugManager()
                              .info(
                                    DebugManager.Category.GAME,
                                    String.format(
                                          "[REFUND] Player=%s, GameID=%s, Amount=%.2f, Currency=%s", player.getName(),
                                          gameId, game.getAmount(), game.getCurrencyType()));
                     }

                     boolean depositSuccess = false;

                     try {
                        depositSuccess = this.plugin.getCurrencyManager().deposit(player, game.getCurrencyType(),
                              game.getCurrencyId(), game.getAmount());
                     } catch (Exception var21) {
                        this.plugin.getLogger().severe("[REFUND ERROR] Deposit failed for player " + player.getName()
                              + ": " + var21.getMessage());
                        depositSuccess = false;
                     }

                     if (depositSuccess) {
                        this.deleteWaitingBackup(gameId);
                        this.removePendingWaitingRefund(player.getUniqueId(), gameId);
                        this.refundCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                        if (limiter != null) {
                           limiter.updateCooldown(player.getUniqueId());
                        }

                        if (txLogger != null) {
                           txLogger.logRefund(
                                 player.getName(),
                                 player.getUniqueId(),
                                 gameId,
                                 game.getAmount(),
                                 game.getCurrencyType(),
                                 game.getCurrencyId(),
                                 RefundResult.SUCCESS);
                        }

                        return true;
                     }

                     this.activeGamesById.put(gameId, game);
                     if (hostUuid != null) {
                        this.hostGameIndex.computeIfAbsent(hostUuid, k -> ConcurrentHashMap.newKeySet()).add(gameId);
                     }

                     this.invalidateGamesCache();

                     try {
                        this.persistWaitingGame(game);
                     } catch (Exception var20) {
                        this.plugin.getLogger()
                              .severe("[REFUND ROLLBACK] Failed to persist backup: " + var20.getMessage());
                     }

                     if (txLogger != null) {
                        txLogger.logRefund(
                              player.getName(),
                              player.getUniqueId(),
                              gameId,
                              game.getAmount(),
                              game.getCurrencyType(),
                              game.getCurrencyId(),
                              RefundResult.ROLLBACK);
                     }

                     e = false;
                  } finally {
                     this.gamesBeingRefunded.remove(gameId);
                     if (limiter != null) {
                        limiter.unlockGame(gameId);
                     }
                  }
               } finally {
                  lock.unlock();
                  this.refundLocks.remove(gameId);
               }

               return e;
            }
         } else {
            if (txLogger != null) {
               txLogger.logBlocked(player.getName(), RefundResult.BLOCKED, "Global lock or reload in progress");
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public int refundAllGames(Player player) {
      if (player == null) {
         return 0;
      } else {
         UUID uuid = player.getUniqueId();
         RefundLimiter limiter = this.plugin.getRefundLimiter();
         TransactionLogger txLogger = this.plugin.getTransactionLogger();
         if (limiter == null || !limiter.isGlobalLocked() && !limiter.isReloading()) {
            boolean onCooldown = limiter != null ? limiter.isOnCooldown(uuid) : this.isOnRefundCooldown(uuid);
            if (onCooldown) {
               int remaining = limiter != null ? limiter.getRemainingCooldownSeconds(uuid)
                     : this.getRemainingCooldown(uuid);
               if (txLogger != null) {
                  txLogger.logBlocked(player.getName(), RefundResult.COOLDOWN,
                        "Cooldown: " + remaining + "s remaining");
               }

               return -1;
            } else {
               boolean transactionStarted = false;
               if (limiter != null) {
                  if (!limiter.tryStartTransaction(uuid)) {
                     if (txLogger != null) {
                        txLogger.logBlocked(player.getName(), RefundResult.BLOCKED,
                              "Concurrent transaction in progress");
                     }

                     return -2;
                  }

                  transactionStarted = true;
               } else if (!this.playersInRefundTransaction.add(uuid)) {
                  return -2;
               }

               int var17;
               try {
                  Set<UUID> games = this.hostGameIndex.get(uuid);
                  if (games == null || games.isEmpty()) {
                     return 0;
                  }

                  List<UUID> ids = new ArrayList<>(games);
                  int refunded = 0;

                  for (UUID gameId : ids) {
                     if (this.refundGame(player, gameId)) {
                        refunded++;
                     }
                  }

                  var17 = refunded;
               } finally {
                  if (limiter != null && transactionStarted) {
                     limiter.endTransaction(uuid, true);
                  } else {
                     this.playersInRefundTransaction.remove(uuid);
                  }
               }

               return var17;
            }
         } else {
            if (txLogger != null) {
               txLogger.logBlocked(player.getName(), RefundResult.BLOCKED, "Global lock or reload in progress");
            }

            return -3;
         }
      }
   }

   public int getActiveGameCount() {
      return this.activeGamesById.size();
   }

   public int getRollingGameCount() {
      synchronized (this.rollingGamesLock) {
         return this.activeRollingGames.size() / 2;
      }
   }

   public int getPlayersInTransactionCount() {
      RefundLimiter limiter = this.plugin.getRefundLimiter();
      return limiter != null ? limiter.getPlayersInTransactionCount() : this.playersInRefundTransaction.size();
   }

   public boolean isRefundInProgress() {
      RefundLimiter limiter = this.plugin.getRefundLimiter();
      return limiter != null ? limiter.isRefundInProgress()
            : !this.playersInRefundTransaction.isEmpty() || !this.gamesBeingRefunded.isEmpty();
   }

   public void cancelGameWithoutRefund(Player player) {
      if (player != null) {
         UUID uuid = player.getUniqueId();
         Set<UUID> games = this.hostGameIndex.get(uuid);
         if (games != null && !games.isEmpty()) {
            if (this.plugin.getDebugManager() != null
                  && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GAME)) {
               this.plugin
                     .getDebugManager()
                     .info(DebugManager.Category.GAME, String.format(
                           "Cancelling %d game(s) without refund for player %s", games.size(), player.getName()));
            }

            for (UUID gameId : new ArrayList<>(games)) {
               CoinFlipGame game = this.activeGamesById.get(gameId);
               if (game != null) {
                  this.removeGame(gameId);
                  this.queueWaitingRefund(game);
               }
            }
         }
      }
   }

   public void registerRollingGame(UUID player1UUID, UUID player2UUID, double amount,
         CoinFlipGame.CurrencyType currencyType, String currencyId) {
      synchronized (this.rollingGamesLock) {
         this.saveBackup(player1UUID, currencyType, currencyId, amount);
         UUID botUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
         if (!botUUID.equals(player2UUID)) {
            this.saveBackup(player2UUID, currencyType, currencyId, amount);
         }

         this.activeRollingGames.put(player1UUID, player2UUID);
         this.rollingGameStartTimes.put(player1UUID, System.currentTimeMillis());
         if (!botUUID.equals(player2UUID)) {
            this.activeRollingGames.put(player2UUID, player1UUID);
            this.rollingGameStartTimes.put(player2UUID, System.currentTimeMillis());
         }
      }
   }

   public void unregisterRollingGame(UUID player1UUID, UUID player2UUID) {
      synchronized (this.rollingGamesLock) {
         boolean player1InGame = this.activeRollingGames.containsKey(player1UUID);
         boolean player2InGame = this.activeRollingGames.containsKey(player2UUID);
         if (player1InGame || player2InGame) {
            this.activeRollingGames.remove(player1UUID);
            this.activeRollingGames.remove(player2UUID);
            this.rollingGameStartTimes.remove(player1UUID);
            this.rollingGameStartTimes.remove(player2UUID);
         }
      }
   }

   public boolean isInRollingGame(UUID uuid) {
      synchronized (this.rollingGamesLock) {
         return this.activeRollingGames.containsKey(uuid);
      }
   }

   public UUID getRollingGamePartner(UUID uuid) {
      synchronized (this.rollingGamesLock) {
         return this.activeRollingGames.get(uuid);
      }
   }

   public void refundRollingGame(UUID disconnectedPlayerUUID) {
      if (!this.rollingGamesBeingRefunded.add(disconnectedPlayerUUID)) {
         this.plugin.getLogger()
               .info("[ANTI-DUPE] Rolling game refund already in progress for " + disconnectedPlayerUUID);
      } else {
         UUID partnerUUID = null;
         UUID botUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

         try {
            synchronized (this.rollingGamesLock) {
               partnerUUID = this.activeRollingGames.get(disconnectedPlayerUUID);
               if (partnerUUID == null) {
                  return;
               }

               if (!botUUID.equals(partnerUUID) && !this.rollingGamesBeingRefunded.add(partnerUUID)) {
                  this.plugin.getLogger()
                        .info("[ANTI-DUPE] Rolling game already being refunded by partner " + partnerUUID);
                  return;
               }

               this.activeRollingGames.remove(disconnectedPlayerUUID);
               this.rollingGameStartTimes.remove(disconnectedPlayerUUID);
               if (!botUUID.equals(partnerUUID)) {
                  this.activeRollingGames.remove(partnerUUID);
                  this.rollingGameStartTimes.remove(partnerUUID);
               }
            }

            boolean refundOnDisconnect = this.cachedRefundOnDisconnect;

            try {
               DatabaseManager.BackupData backup1 = this.databaseManager.loadBackup(disconnectedPlayerUUID);
               DatabaseManager.BackupData backup2 = this.databaseManager.loadBackup(partnerUUID);
               this.plugin
                     .getLogger()
                     .info(
                           String.format(
                                 "[Rolling Game Refund] Player1=%s, Player2=%s, RefundOnDisconnect=%s",
                                 disconnectedPlayerUUID, partnerUUID, refundOnDisconnect));
               if (backup1 != null) {
                  Player player1 = Bukkit.getPlayer(disconnectedPlayerUUID);
                  if (player1 != null && player1.isOnline() && refundOnDisconnect) {
                     this.refundPlayerFromBackup(player1, backup1);
                     this.removeBackup(disconnectedPlayerUUID);
                     this.plugin.getLogger().info("[Rolling Game Refund] Refunded player1: " + disconnectedPlayerUUID);
                  }
               }

               if (backup2 != null) {
                  if (!botUUID.equals(partnerUUID)) {
                     Player player2 = Bukkit.getPlayer(partnerUUID);
                     if (player2 != null && player2.isOnline() && refundOnDisconnect) {
                        this.refundPlayerFromBackup(player2, backup2);
                        this.removeBackup(partnerUUID);
                        this.plugin.getLogger().info("[Rolling Game Refund] Refunded player2: " + partnerUUID);
                     }
                  } else {
                     this.removeBackup(partnerUUID);
                     this.plugin.getLogger()
                           .info("[Rolling Game Refund] Removed bot backup (no refund needed): " + partnerUUID);
                  }
               }

               this.plugin
                     .getLogger()
                     .info("Refunded rolling game for players " + disconnectedPlayerUUID + " and " + partnerUUID
                           + " due to player disconnect");
            } catch (Exception var12) {
               this.plugin.getLogger()
                     .severe("Failed to refund rolling game for " + disconnectedPlayerUUID + ": " + var12.getMessage());
               var12.printStackTrace();
            }
         } finally {
            this.rollingGamesBeingRefunded.remove(disconnectedPlayerUUID);
            if (partnerUUID != null && !botUUID.equals(partnerUUID)) {
               this.rollingGamesBeingRefunded.remove(partnerUUID);
            }
         }
      }
   }

   public int cleanupRollingGamesOnReload() {
      int cleanedCount = 0;
      synchronized (this.rollingGamesLock) {
         if (this.activeRollingGames.isEmpty()) {
            return 0;
         } else {
            Set<UUID> processedUuids = new HashSet<>(this.activeRollingGames.size() / 2 + 1);
            List<Entry<UUID, UUID>> gamesToCleanup = new ArrayList<>();

            for (Entry<UUID, UUID> entry : this.activeRollingGames.entrySet()) {
               UUID player1UUID = entry.getKey();
               UUID player2UUID = entry.getValue();
               if (!processedUuids.contains(player1UUID) && !processedUuids.contains(player2UUID)) {
                  processedUuids.add(player1UUID);
                  processedUuids.add(player2UUID);
                  gamesToCleanup.add(entry);
               }
            }

            for (Entry<UUID, UUID> entryx : gamesToCleanup) {
               UUID player1UUID = entryx.getKey();
               UUID player2UUID = entryx.getValue();
               if (player1UUID != null && player2UUID != null) {
                  try {
                     Player player1 = Bukkit.getPlayer(player1UUID);
                     Player player2 = Bukkit.getPlayer(player2UUID);
                     if (player1 != null && player1.isOnline()) {
                        try {
                           Inventory topInv = GUIHelper.getTopInventorySafely(player1);
                           if (topInv != null) {
                              InventoryHandler handler = this.plugin.getGuiManager().getHandler(topInv);
                              if (handler instanceof CoinFlipRollGUI) {
                                 player1.closeInventory();
                              }
                           }
                        } catch (Exception var15) {
                        }
                     }

                     if (player2 != null && player2.isOnline()) {
                        try {
                           Inventory topInv = GUIHelper.getTopInventorySafely(player2);
                           if (topInv != null) {
                              InventoryHandler handler = this.plugin.getGuiManager().getHandler(topInv);
                              if (handler instanceof CoinFlipRollGUI) {
                                 player2.closeInventory();
                              }
                           }
                        } catch (Exception var16) {
                        }
                     }

                     boolean refundOnDisconnect = this.cachedRefundOnDisconnect;
                     DatabaseManager.BackupData backup1 = this.databaseManager.loadBackup(player1UUID);
                     DatabaseManager.BackupData backup2 = this.databaseManager.loadBackup(player2UUID);
                     if (backup1 != null && player1 != null && player1.isOnline() && refundOnDisconnect) {
                        this.refundPlayerFromBackup(player1, backup1);
                        this.removeBackup(player1UUID);
                     }

                     if (backup2 != null && player2 != null && player2.isOnline() && refundOnDisconnect) {
                        this.refundPlayerFromBackup(player2, backup2);
                        this.removeBackup(player2UUID);
                     }

                     this.activeRollingGames.remove(player1UUID);
                     this.activeRollingGames.remove(player2UUID);
                     cleanedCount++;
                  } catch (Exception var17) {
                     this.plugin
                           .getLogger()
                           .warning("Failed to cleanup rolling game for players " + player1UUID + " and " + player2UUID
                                 + ": " + var17.getMessage());
                     this.activeRollingGames.remove(player1UUID);
                     this.activeRollingGames.remove(player2UUID);
                  }
               } else {
                  this.plugin.getLogger().warning("Skipping cleanup for game with null UUID(s): player1=" + player1UUID
                        + ", player2=" + player2UUID);
               }
            }

            this.invalidateGamesCache();
            return cleanedCount;
         }
      }
   }

   private void refundPlayerFromBackup(Player player, DatabaseManager.BackupData backup) {
      String currencyTypeString = backup.getCurrencyType();
      String currencyId = null;

      CoinFlipGame.CurrencyType type;
      try {
         if (currencyTypeString.contains(":")) {
            String[] parts = currencyTypeString.split(":", 2);
            type = CoinFlipGame.CurrencyType.valueOf(parts[0]);
            currencyId = parts[1];
         } else {
            type = CoinFlipGame.CurrencyType.valueOf(currencyTypeString);
         }
      } catch (IllegalArgumentException var9) {
         if (!currencyTypeString.equals("ORBS") && !currencyTypeString.startsWith("ORBS:")) {
            this.plugin.getLogger()
                  .warning("Unknown currency type in backup for " + player.getUniqueId() + ": " + currencyTypeString);
            return;
         }

         if (currencyTypeString.contains(":")) {
            String[] parts = currencyTypeString.split(":", 2);
            currencyId = parts[1];
         } else {
            currencyId = "orbs";
         }

         if (!this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyId)) {
            this.plugin
                  .getLogger()
                  .warning(
                        "Legacy ORBS backup found for " + player.getUniqueId() + " but placeholder currency '"
                              + currencyId + "' is not enabled. Skipping refund...");
            return;
         }

         type = CoinFlipGame.CurrencyType.PLACEHOLDER;
      }

      this.plugin.getCurrencyManager().deposit(player, type, currencyId, backup.getAmount());
      String unit = this.plugin.getCurrencyManager().getUnit(type, currencyId);
      String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.refunded");
      Map<String, String> placeholders = new HashMap<>();
      placeholders.put("amount", this.plugin.getGuiHelper().formatAmount(backup.getAmount()));
      placeholders.put("symbol", unit);
      this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
   }

   public void refundAllGames() {
      boolean refundOnDisconnect = this.cachedRefundOnDisconnect;
      boolean refundOnReconnectAfterShutdown = this.cachedRefundOnReconnectAfterShutdown;
      boolean skipRefundOnStop = refundOnReconnectAfterShutdown && refundOnDisconnect;
      if (skipRefundOnStop) {
         this.plugin.getLogger().info(
               "Refunds will be processed when players reconnect (refund-on-reconnect-after-shutdown is enabled)...");
      } else {
         this.plugin.getLogger().info("Refunding all active coinflip games due to server shutdown...");
      }

      int refundedOnline = 0;
      int refundedOffline = 0;
      int totalBackups = 0;

      try {
         for (CoinFlipGame game : new ArrayList<>(this.activeGamesById.values())) {
            if (game != null) {
               Player host = game.getHost();
               UUID hostUuid = host != null ? host.getUniqueId() : null;
               Player onlinePlayer = hostUuid != null ? Bukkit.getPlayer(hostUuid) : null;
               if (onlinePlayer != null && onlinePlayer.isOnline() && !skipRefundOnStop) {
                  this.plugin.getCurrencyManager().deposit(onlinePlayer, game.getCurrencyType(), game.getCurrencyId(),
                        game.getAmount());
                  refundedOnline++;
                  this.deleteWaitingBackup(game.getGameId());
                  this.plugin.getLogger().info(
                        "Refunded active game for online player: " + onlinePlayer.getName() + " (" + hostUuid + ")");
               } else {
                  refundedOffline++;
                  this.queueWaitingRefund(game);
                  if (skipRefundOnStop) {
                     this.plugin.getLogger().info("Active game for player will be restored on rejoin: "
                           + (hostUuid != null ? hostUuid : "unknown"));
                  } else {
                     this.plugin.getLogger().info("Active game for offline player will be restored on rejoin: "
                           + (hostUuid != null ? hostUuid : "unknown"));
                  }
               }

               this.removeGame(game.getGameId());
            }
         }

         synchronized (this.rollingGamesLock) {
            Set<UUID> processedUuids = new HashSet<>(this.activeRollingGames.size() / 2 + 1);

            for (Entry<UUID, UUID> entry : this.activeRollingGames.entrySet()) {
               UUID player1UUID = entry.getKey();
               UUID player2UUID = entry.getValue();
               if (!processedUuids.contains(player1UUID) && !processedUuids.contains(player2UUID)) {
                  processedUuids.add(player1UUID);
                  processedUuids.add(player2UUID);

                  try {
                     DatabaseManager.BackupData backup1 = this.databaseManager.loadBackup(player1UUID);
                     DatabaseManager.BackupData backup2 = this.databaseManager.loadBackup(player2UUID);
                     if (backup1 != null) {
                        Player player1 = Bukkit.getPlayer(player1UUID);
                        if (player1 != null && player1.isOnline() && !skipRefundOnStop) {
                           this.refundPlayerFromBackup(player1, backup1);
                           this.removeBackup(player1UUID);
                           refundedOnline++;
                           this.plugin.getLogger().info("Refunded rolling game for online player: " + player1.getName()
                                 + " (" + player1UUID + ")");
                        } else {
                           refundedOffline++;
                           if (skipRefundOnStop) {
                              this.plugin.getLogger()
                                    .info("Rolling game for player will be restored on rejoin: " + player1UUID);
                           } else {
                              this.plugin.getLogger()
                                    .info("Rolling game for offline player will be restored on rejoin: " + player1UUID);
                           }
                        }
                     }

                     if (backup2 != null) {
                        Player player2 = Bukkit.getPlayer(player2UUID);
                        if (player2 != null && player2.isOnline() && !skipRefundOnStop) {
                           this.refundPlayerFromBackup(player2, backup2);
                           this.removeBackup(player2UUID);
                           refundedOnline++;
                           this.plugin.getLogger().info("Refunded rolling game for online player: " + player2.getName()
                                 + " (" + player2UUID + ")");
                        } else {
                           refundedOffline++;
                           if (skipRefundOnStop) {
                              this.plugin.getLogger()
                                    .info("Rolling game for player will be restored on rejoin: " + player2UUID);
                           } else {
                              this.plugin.getLogger()
                                    .info("Rolling game for offline player will be restored on rejoin: " + player2UUID);
                           }
                        }
                     }
                  } catch (Exception var18) {
                     this.plugin
                           .getLogger()
                           .warning("Failed to refund rolling game for players " + player1UUID + " and " + player2UUID
                                 + ": " + var18.getMessage());
                  }
               }
            }

            this.activeRollingGames.clear();
            this.invalidateGamesCache();
         }

         List<UUID> backupUuids = this.databaseManager.getAllBackups();
         totalBackups = backupUuids.size();

         for (UUID uuid : backupUuids) {
            try {
               DatabaseManager.BackupData backup = this.databaseManager.loadBackup(uuid);
               if (backup != null) {
                  Player player = Bukkit.getPlayer(uuid);
                  if (player != null && player.isOnline() && !skipRefundOnStop) {
                     this.refundPlayerFromBackup(player, backup);
                     this.removeBackup(uuid);
                     refundedOnline++;
                     this.plugin.getLogger()
                           .info("Refunded backup for online player: " + player.getName() + " (" + uuid + ")");
                  } else {
                     refundedOffline++;
                  }
               }
            } catch (Exception var17) {
               this.plugin.getLogger().warning("Failed to process backup for " + uuid + ": " + var17.getMessage());
            }
         }

         this.activeGamesById.clear();
         this.hostGameIndex.clear();
         this.invalidateGamesCache();
         this.plugin
               .getLogger()
               .info(
                     "Refund process completed. Online players refunded: "
                           + refundedOnline
                           + ", Offline players (will be restored on rejoin): "
                           + refundedOffline
                           + ", Total backups: "
                           + totalBackups);
      } catch (Exception var20) {
         this.plugin.getLogger().severe("Failed to refund all games: " + var20.getMessage());
         var20.printStackTrace();
      }
   }

   private void persistWaitingGame(CoinFlipGame game) {
      if (game != null && game.getHostUuid() != null) {
         Runnable task = () -> {
            try {
               this.databaseManager.saveWaitingGameBackup(game.getGameId(), game.getHostUuid(), game.getCurrencyType(),
                     game.getCurrencyId(), game.getAmount());
            } catch (Exception var3) {
               this.plugin.getLogger().warning(
                     "Failed to save waiting game backup for " + game.getHostUuid() + ": " + var3.getMessage());
            }
         };
         if (!LegacyCompatibility.isServerStopping() && this.plugin.getServer().isPrimaryThread()) {
            FoliaScheduler.runTaskAsynchronously(this.plugin, task);
         } else {
            task.run();
         }
      }
   }

   private void deleteWaitingBackup(UUID gameId) {
      if (gameId != null) {
         Runnable task = () -> {
            try {
               this.databaseManager.removeWaitingGameBackup(gameId);
            } catch (Exception var3) {
               this.plugin.getLogger()
                     .warning("Failed to remove waiting game backup for " + gameId + ": " + var3.getMessage());
            }
         };
         if (!LegacyCompatibility.isServerStopping() && this.plugin.getServer().isPrimaryThread()) {
            FoliaScheduler.runTaskAsynchronously(this.plugin, task);
         } else {
            task.run();
         }
      }
   }

   private void queueWaitingRefund(CoinFlipGame game) {
      if (game != null && game.getHostUuid() != null) {
         DatabaseManager.WaitingGameBackup backup = new DatabaseManager.WaitingGameBackup(
               game.getGameId(), game.getHostUuid(), game.getCurrencyType(), game.getCurrencyId(), game.getAmount(),
               System.currentTimeMillis());
         this.pendingWaitingRefunds.computeIfAbsent(game.getHostUuid(), id -> new ConcurrentHashMap<>())
               .put(game.getGameId(), backup);
      }
   }

   private void removePendingWaitingRefund(UUID ownerUuid, UUID gameId) {
      if (ownerUuid != null && gameId != null) {
         Map<UUID, DatabaseManager.WaitingGameBackup> backups = this.pendingWaitingRefunds.get(ownerUuid);
         if (backups != null) {
            backups.remove(gameId);
            if (backups.isEmpty()) {
               this.pendingWaitingRefunds.remove(ownerUuid);
            }
         }
      }
   }

   private void loadWaitingGameBackups() {
      try {
         for (DatabaseManager.WaitingGameBackup backup : this.databaseManager.loadAllWaitingGameBackups()) {
            this.pendingWaitingRefunds.computeIfAbsent(backup.getOwnerUuid(), id -> new ConcurrentHashMap<>())
                  .put(backup.getGameId(), backup);
         }

         for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != null && online.isOnline()) {
               this.restoreWaitingGameBackups(online);
            }
         }
      } catch (Exception var4) {
         this.plugin.getLogger().warning("Failed to load waiting game backups: " + var4.getMessage());
      }
   }

   public void restoreWaitingGameBackups(Player player) {
      if (player != null && player.isOnline()) {
         UUID uuid = player.getUniqueId();
         FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
            List<DatabaseManager.WaitingGameBackup> backups = this.collectWaitingBackups(uuid);
            if (!backups.isEmpty()) {
               FoliaScheduler.runTask(this.plugin, player, () -> {
                  if (!player.isOnline()) {
                     for (DatabaseManager.WaitingGameBackup backup : backups) {
                        this.pendingWaitingRefunds.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>())
                              .put(backup.getGameId(), backup);
                     }
                  } else {
                     for (DatabaseManager.WaitingGameBackup backup : backups) {
                        this.plugin.getCurrencyManager().deposit(player, backup.getCurrencyType(),
                              backup.getCurrencyId(), backup.getAmount());
                        this.deleteWaitingBackup(backup.getGameId());
                     }

                     this.pendingWaitingRefunds.remove(uuid);
                     this.plugin.getLogger()
                           .info("Restored " + backups.size() + " waiting coinflip(s) for " + player.getName());
                  }
               });
            }
         });
      }
   }

   private List<DatabaseManager.WaitingGameBackup> collectWaitingBackups(UUID ownerUuid) {
      List<DatabaseManager.WaitingGameBackup> backups = new ArrayList<>();
      Map<UUID, DatabaseManager.WaitingGameBackup> pending = this.pendingWaitingRefunds.remove(ownerUuid);
      if (pending != null) {
         backups.addAll(pending.values());
      }

      if (backups.isEmpty()) {
         try {
            backups.addAll(this.databaseManager.loadWaitingGameBackups(ownerUuid));
         } catch (Exception var5) {
            this.plugin.getLogger()
                  .warning("Failed to load waiting backups for " + ownerUuid + ": " + var5.getMessage());
         }
      }

      return backups;
   }

   private void saveBackup(UUID uuid, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      if (!LegacyCompatibility.isServerStopping() && this.plugin.getServer().isPrimaryThread()) {
         FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
            try {
               String currencyTypeString = type.name();
               if (currencyId != null && (type == CoinFlipGame.CurrencyType.COINSENGINE
                     || type == CoinFlipGame.CurrencyType.PLACEHOLDER)) {
                  currencyTypeString = type.name() + ":" + currencyId;
               }

               this.databaseManager.saveBackup(uuid, currencyTypeString, amount);
            } catch (Exception var7x) {
               this.plugin.getLogger().severe("Failed to save backup for " + uuid + ": " + var7x.getMessage());
               var7x.printStackTrace();
            }
         });
      } else {
         try {
            String currencyTypeString = type.name();
            if (currencyId != null
                  && (type == CoinFlipGame.CurrencyType.COINSENGINE || type == CoinFlipGame.CurrencyType.PLACEHOLDER)) {
               currencyTypeString = type.name() + ":" + currencyId;
            }

            this.databaseManager.saveBackup(uuid, currencyTypeString, amount);
         } catch (Exception var7) {
            this.plugin.getLogger().severe("Failed to save backup for " + uuid + ": " + var7.getMessage());
            var7.printStackTrace();
         }
      }
   }

   public void removeBackupForPlayer(UUID uuid) {
      this.removeBackup(uuid);
   }

   private void removeBackup(UUID uuid) {
      if (!LegacyCompatibility.isServerStopping() && this.plugin.getServer().isPrimaryThread()) {
         FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
            try {
               this.databaseManager.removeBackup(uuid);
            } catch (Exception var3x) {
               this.plugin.getLogger().severe("Failed to remove backup for " + uuid + ": " + var3x.getMessage());
               var3x.printStackTrace();
            }
         });
      } else {
         try {
            this.databaseManager.removeBackup(uuid);
         } catch (Exception var3) {
            this.plugin.getLogger().severe("Failed to remove backup for " + uuid + ": " + var3.getMessage());
            var3.printStackTrace();
         }
      }
   }

   public void saveBackupForRefund(UUID uuid, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      this.saveBackup(uuid, type, currencyId, amount);
   }

   public void loadBackups() {
   }

   public void restoreBackup(Player player) {
      if (player != null && player.isOnline()) {
         UUID uuid = player.getUniqueId();
         FoliaScheduler.runTaskAsynchronously(
               this.plugin,
               () -> {
                  try {
                     DatabaseManager.BackupData backup = this.databaseManager.loadBackup(uuid);
                     if (backup == null) {
                        return;
                     }

                     FoliaScheduler.runTask(
                           this.plugin,
                           () -> {
                              Player onlinePlayer = Bukkit.getPlayer(uuid);
                              if (onlinePlayer != null && onlinePlayer.isOnline()) {
                                 FoliaScheduler.runTask(
                                       this.plugin,
                                       onlinePlayer,
                                       () -> {
                                          if (onlinePlayer.isOnline()) {
                                             String currencyTypeString = backup.getCurrencyType();
                                             String currencyId = null;
                                             CoinFlipGame.CurrencyType type;
                                             if (currencyTypeString.contains(":")) {
                                                String[] parts = currencyTypeString.split(":", 2);

                                                try {
                                                   type = CoinFlipGame.CurrencyType.valueOf(parts[0]);
                                                   currencyId = parts[1];
                                                } catch (IllegalArgumentException var10) {
                                                   if (!parts[0].equals("ORBS")) {
                                                      this.plugin.getLogger()
                                                            .warning("Unknown currency type in backup: " + parts[0]);
                                                      this.removeBackup(uuid);
                                                      return;
                                                   }

                                                   if (!this.plugin.getCurrencyManager()
                                                         .isPlaceholderCurrencyEnabled("orbs")) {
                                                      this.plugin
                                                            .getLogger()
                                                            .warning("Legacy ORBS backup found for " + uuid
                                                                  + " but no placeholder currency 'orbs' is enabled. Skipping...");
                                                      this.removeBackup(uuid);
                                                      return;
                                                   }

                                                   type = CoinFlipGame.CurrencyType.PLACEHOLDER;
                                                   currencyId = "orbs";
                                                }
                                             } else {
                                                try {
                                                   type = CoinFlipGame.CurrencyType.valueOf(currencyTypeString);
                                                } catch (IllegalArgumentException var11) {
                                                   if (!currencyTypeString.equals("ORBS")) {
                                                      this.plugin.getLogger().warning(
                                                            "Unknown currency type in backup: " + currencyTypeString);
                                                      this.removeBackup(uuid);
                                                      return;
                                                   }

                                                   if (!this.plugin.getCurrencyManager()
                                                         .isPlaceholderCurrencyEnabled("orbs")) {
                                                      this.plugin
                                                            .getLogger()
                                                            .warning("Legacy ORBS backup found for " + uuid
                                                                  + " but no placeholder currency 'orbs' is enabled. Skipping...");
                                                      this.removeBackup(uuid);
                                                      return;
                                                   }

                                                   type = CoinFlipGame.CurrencyType.PLACEHOLDER;
                                                   currencyId = "orbs";
                                                }
                                             }

                                             this.plugin.getCurrencyManager().deposit(onlinePlayer, type, currencyId,
                                                   backup.getAmount());
                                             String unit = this.plugin.getCurrencyManager().getUnit(type, currencyId);
                                             String message = this.plugin.getMessage("prefix") + " "
                                                   + this.plugin.getMessage("game.refunded");
                                             Map<String, String> placeholders = new HashMap<>();
                                             placeholders.put("amount",
                                                   this.plugin.getGuiHelper().formatAmount(backup.getAmount()));
                                             placeholders.put("symbol", unit);
                                             this.plugin.getAdventureHelper().sendMessage(onlinePlayer, message,
                                                   placeholders);
                                             this.removeBackup(uuid);
                                          }
                                       });
                              } else {
                                 this.plugin
                                       .getLogger()
                                       .info("Player " + uuid
                                             + " went offline before backup could be restored. Backup will be restored on next join.");
                              }
                           });
                  } catch (Exception var4) {
                     this.plugin.getLogger().severe("Failed to restore backup for " + uuid + ": " + var4.getMessage());
                     var4.printStackTrace();
                  }
               });
      }
   }

   private String createConsecutiveWinsKey(UUID uuid1, UUID uuid2) {
      return uuid1.compareTo(uuid2) < 0 ? uuid1.toString() + "_" + uuid2.toString()
            : uuid2.toString() + "_" + uuid1.toString();
   }

   public int getConsecutiveWins(UUID winnerUUID, UUID loserUUID) {
      if (winnerUUID != null && loserUUID != null) {
         synchronized (this.consecutiveWinsLock) {
            String key = this.createConsecutiveWinsKey(winnerUUID, loserUUID);
            return this.consecutiveWinsMap.getOrDefault(key, 0);
         }
      } else {
         return 0;
      }
   }

   public int incrementConsecutiveWins(UUID winnerUUID, UUID loserUUID) {
      if (winnerUUID != null && loserUUID != null) {
         synchronized (this.consecutiveWinsLock) {
            String key = this.createConsecutiveWinsKey(winnerUUID, loserUUID);
            int currentWins = this.consecutiveWinsMap.getOrDefault(key, 0);
            if (currentWins >= Integer.MAX_VALUE) {
               return currentWins;
            } else {
               int newWins = currentWins + 1;
               this.consecutiveWinsMap.put(key, newWins);
               return newWins;
            }
         }
      } else {
         return 0;
      }
   }

   public void resetConsecutiveWins(UUID newWinnerUUID, UUID newLoserUUID) {
      if (newWinnerUUID != null && newLoserUUID != null) {
         synchronized (this.consecutiveWinsLock) {
            String key = this.createConsecutiveWinsKey(newWinnerUUID, newLoserUUID);
            this.consecutiveWinsMap.remove(key);
         }
      }
   }

   public void closeDatabase() {
      if (this.batchModeEnabled && this.statsBatchQueue != null) {
         this.plugin.getLogger().info("Flushing stats batch queue...");
         this.statsBatchQueue.forceFlushAll();
      }

      Map<UUID, PlayerStats> cacheSnapshot;
      synchronized (this.statsLock) {
         cacheSnapshot = new HashMap<>(this.playerStatsCache);
      }

      try {
         this.plugin.getLogger().info("Saving player stats to database...");
         int saved = 0;

         for (Entry<UUID, PlayerStats> entry : cacheSnapshot.entrySet()) {
            try {
               this.databaseManager.savePlayerStats(entry.getKey(), entry.getValue());
               saved++;
            } catch (Exception var6) {
               this.plugin.getLogger()
                     .warning("Failed to save stats for " + entry.getKey() + " on shutdown: " + var6.getMessage());
            }
         }

         this.plugin.getLogger().info("Saved " + saved + " player stats to database.");
         this.databaseManager.close();
      } catch (Exception var8) {
         this.plugin.getLogger().severe("Failed to close database: " + var8.getMessage());
         var8.printStackTrace();
      }
   }
}
