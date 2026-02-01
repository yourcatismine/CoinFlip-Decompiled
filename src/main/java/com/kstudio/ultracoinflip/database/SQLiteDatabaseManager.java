package com.kstudio.ultracoinflip.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.CoinFlipLog;
import com.kstudio.ultracoinflip.data.PlayerSettings;
import com.kstudio.ultracoinflip.data.PlayerStats;
import java.io.File;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class SQLiteDatabaseManager implements DatabaseManager {
   private final KStudio plugin;
   private Connection connection;
   private final Object dbLock = new Object();
   private static final Gson GSON = new Gson();

   @Override
   public void initialize() throws Exception {
      File dataFolder = this.plugin.getDataFolder();
      if (!dataFolder.exists()) {
         dataFolder.mkdirs();
      }

      File dbFile = new File(dataFolder, "ultracoinflip.db");
      String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
      this.connection = DriverManager.getConnection(url);
      this.createTables();
      this.plugin.getLogger().info("SQLite database connected!");
   }

   private void createTables() throws SQLException {
      String createStatsTable = "CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, wins INTEGER DEFAULT 0, defeats INTEGER DEFAULT 0, profit_money REAL DEFAULT 0, profit_playerpoints REAL DEFAULT 0, profit_tokenmanager REAL DEFAULT 0, profit_beasttokens REAL DEFAULT 0, loss_money REAL DEFAULT 0, loss_playerpoints REAL DEFAULT 0, loss_tokenmanager REAL DEFAULT 0, loss_beasttokens REAL DEFAULT 0, wins_money INTEGER DEFAULT 0, defeats_money INTEGER DEFAULT 0, wins_playerpoints INTEGER DEFAULT 0, defeats_playerpoints INTEGER DEFAULT 0, wins_tokenmanager INTEGER DEFAULT 0, defeats_tokenmanager INTEGER DEFAULT 0, wins_beasttokens INTEGER DEFAULT 0, defeats_beasttokens INTEGER DEFAULT 0, wins_placeholder TEXT DEFAULT '{}', defeats_placeholder TEXT DEFAULT '{}', winstreak INTEGER DEFAULT 0)";
      String createBackupsTable = "CREATE TABLE IF NOT EXISTS backups (uuid VARCHAR(36) PRIMARY KEY, currency_type VARCHAR(50) NOT NULL, amount REAL NOT NULL)";
      String createWaitingBackupsTable = "CREATE TABLE IF NOT EXISTS waiting_backups (game_id VARCHAR(36) PRIMARY KEY, owner_uuid VARCHAR(36) NOT NULL, currency_type VARCHAR(50) NOT NULL, currency_id VARCHAR(100), amount REAL NOT NULL, created_at BIGINT NOT NULL)";
      String createLogsTable = "CREATE TABLE IF NOT EXISTS coinflip_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, player1_uuid VARCHAR(36) NOT NULL, player1_name VARCHAR(16) NOT NULL, player2_uuid VARCHAR(36) NOT NULL, player2_name VARCHAR(16) NOT NULL, winner_uuid VARCHAR(36) NOT NULL, winner_name VARCHAR(16) NOT NULL, loser_uuid VARCHAR(36) NOT NULL, loser_name VARCHAR(16) NOT NULL, currency_type VARCHAR(50) NOT NULL, currency_id VARCHAR(100), amount REAL NOT NULL, total_pot REAL NOT NULL, tax_rate REAL NOT NULL, tax_amount REAL NOT NULL, winner_amount REAL NOT NULL, timestamp BIGINT NOT NULL, game_type VARCHAR(10) DEFAULT 'PLAYER')";
      String createSettingsTable = "CREATE TABLE IF NOT EXISTS player_settings (uuid VARCHAR(36) PRIMARY KEY, settings TEXT DEFAULT '{}')";
      Statement stmt = this.connection.createStatement();

      try {
         stmt.execute(createStatsTable);
         stmt.execute(createBackupsTable);
         stmt.execute(createLogsTable);
         stmt.execute(createWaitingBackupsTable);
         stmt.execute(createSettingsTable);

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON coinflip_logs(timestamp DESC)");
         } catch (SQLException var34) {
         }

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_player1 ON coinflip_logs(player1_uuid)");
         } catch (SQLException var33) {
         }

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_player2 ON coinflip_logs(player2_uuid)");
         } catch (SQLException var32) {
         }

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_winner ON coinflip_logs(winner_uuid)");
         } catch (SQLException var31) {
         }

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_waiting_backups_owner ON waiting_backups(owner_uuid)");
         } catch (SQLException var30) {
         }

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_currency_winner ON coinflip_logs(currency_type, currency_id, winner_uuid, winner_amount)");
         } catch (SQLException var29) {
         }

         try {
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_logs_currency_loser ON coinflip_logs(currency_type, currency_id, loser_uuid, amount)");
         } catch (SQLException var28) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN profit_playerpoints REAL DEFAULT 0");
         } catch (SQLException var27) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN loss_playerpoints REAL DEFAULT 0");
         } catch (SQLException var26) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN profit_tokenmanager REAL DEFAULT 0");
         } catch (SQLException var25) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN loss_tokenmanager REAL DEFAULT 0");
         } catch (SQLException var24) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN profit_beasttokens REAL DEFAULT 0");
         } catch (SQLException var23) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN loss_beasttokens REAL DEFAULT 0");
         } catch (SQLException var22) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_money INTEGER DEFAULT 0");
         } catch (SQLException var21) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_money INTEGER DEFAULT 0");
         } catch (SQLException var20) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_playerpoints INTEGER DEFAULT 0");
         } catch (SQLException var19) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_playerpoints INTEGER DEFAULT 0");
         } catch (SQLException var18) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_tokenmanager INTEGER DEFAULT 0");
         } catch (SQLException var17) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_tokenmanager INTEGER DEFAULT 0");
         } catch (SQLException var16) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_beasttokens INTEGER DEFAULT 0");
         } catch (SQLException var15) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_beasttokens INTEGER DEFAULT 0");
         } catch (SQLException var14) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_placeholder TEXT DEFAULT '{}'");
         } catch (SQLException var13) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_placeholder TEXT DEFAULT '{}'");
         } catch (SQLException var12) {
         }

         try {
            stmt.execute("ALTER TABLE player_stats ADD COLUMN winstreak INTEGER DEFAULT 0");
         } catch (SQLException var11) {
         }

         try {
            stmt.execute("ALTER TABLE coinflip_logs ADD COLUMN game_type VARCHAR(10) DEFAULT 'PLAYER'");
         } catch (SQLException var10) {
         }
      } catch (Throwable var35) {
         if (stmt != null) {
            try {
               stmt.close();
            } catch (Throwable var9) {
               var35.addSuppressed(var9);
            }
         }

         throw var35;
      }

      if (stmt != null) {
         stmt.close();
      }
   }

   @Override
   public void close() throws Exception {
      if (this.connection != null && !this.connection.isClosed()) {
         this.connection.close();
      }
   }

   @Override
   public Connection getConnection() throws Exception {
      synchronized (this.dbLock) {
         if (this.connection == null || this.connection.isClosed()) {
            this.initialize();
         }

         return this.connection;
      }
   }

   @Override
   public PlayerStats loadPlayerStats(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "SELECT * FROM player_stats WHERE uuid = ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         PlayerStats var34;
         label194: {
            try {
               stmt.setString(1, uuid.toString());
               ResultSet rs = stmt.executeQuery();

               label196: {
                  try {
                     if (!rs.next()) {
                        break label196;
                     }

                     PlayerStats stats = new PlayerStats();
                     stats.setWins(rs.getInt("wins"));
                     stats.setDefeats(rs.getInt("defeats"));
                     stats.setProfitMoney(rs.getDouble("profit_money"));

                     try {
                        stats.setProfitPlayerPoints(rs.getDouble("profit_playerpoints"));
                     } catch (SQLException var29) {
                        stats.setProfitPlayerPoints(0.0);
                     }

                     stats.setLossMoney(rs.getDouble("loss_money"));

                     try {
                        stats.setLossPlayerPoints(rs.getDouble("loss_playerpoints"));
                     } catch (SQLException var28) {
                        stats.setLossPlayerPoints(0.0);
                     }

                     try {
                        stats.setProfitTokenManager(rs.getDouble("profit_tokenmanager"));
                     } catch (SQLException var27) {
                        stats.setProfitTokenManager(0.0);
                     }

                     try {
                        stats.setProfitBeastTokens(rs.getDouble("profit_beasttokens"));
                     } catch (SQLException var26) {
                        stats.setProfitBeastTokens(0.0);
                     }

                     try {
                        stats.setLossTokenManager(rs.getDouble("loss_tokenmanager"));
                     } catch (SQLException var25) {
                        stats.setLossTokenManager(0.0);
                     }

                     try {
                        stats.setLossBeastTokens(rs.getDouble("loss_beasttokens"));
                     } catch (SQLException var24) {
                        stats.setLossBeastTokens(0.0);
                     }

                     try {
                        stats.setWinsMoney(rs.getInt("wins_money"));
                     } catch (SQLException var23) {
                        stats.setWinsMoney(0);
                     }

                     try {
                        stats.setDefeatsMoney(rs.getInt("defeats_money"));
                     } catch (SQLException var22) {
                        stats.setDefeatsMoney(0);
                     }

                     try {
                        stats.setWinsPlayerPoints(rs.getInt("wins_playerpoints"));
                     } catch (SQLException var21) {
                        stats.setWinsPlayerPoints(0);
                     }

                     try {
                        stats.setDefeatsPlayerPoints(rs.getInt("defeats_playerpoints"));
                     } catch (SQLException var20) {
                        stats.setDefeatsPlayerPoints(0);
                     }

                     try {
                        stats.setWinsTokenManager(rs.getInt("wins_tokenmanager"));
                     } catch (SQLException var19) {
                        stats.setWinsTokenManager(0);
                     }

                     try {
                        stats.setDefeatsTokenManager(rs.getInt("defeats_tokenmanager"));
                     } catch (SQLException var18) {
                        stats.setDefeatsTokenManager(0);
                     }

                     try {
                        stats.setWinsBeastTokens(rs.getInt("wins_beasttokens"));
                     } catch (SQLException var17) {
                        stats.setWinsBeastTokens(0);
                     }

                     try {
                        stats.setDefeatsBeastTokens(rs.getInt("defeats_beasttokens"));
                     } catch (SQLException var16) {
                        stats.setDefeatsBeastTokens(0);
                     }

                     try {
                        String winsPlaceholderJson = rs.getString("wins_placeholder");
                        if (winsPlaceholderJson != null && !winsPlaceholderJson.isEmpty()) {
                           Type type = (new TypeToken<Map<String, Integer>>() {}).getType();
                           Map<String, Integer> winsMap = GSON.fromJson(winsPlaceholderJson, type);
                           if (winsMap != null) {
                              stats.setWinsPlaceholder(winsMap);
                           }
                        }
                     } catch (Exception var15) {
                        stats.setWinsPlaceholder(new HashMap<>());
                     }

                     try {
                        String defeatsPlaceholderJson = rs.getString("defeats_placeholder");
                        if (defeatsPlaceholderJson != null && !defeatsPlaceholderJson.isEmpty()) {
                           Type type = (new TypeToken<Map<String, Integer>>() {}).getType();
                           Map<String, Integer> defeatsMap = GSON.fromJson(defeatsPlaceholderJson, type);
                           if (defeatsMap != null) {
                              stats.setDefeatsPlaceholder(defeatsMap);
                           }
                        }
                     } catch (Exception var14) {
                        stats.setDefeatsPlaceholder(new HashMap<>());
                     }

                     try {
                        stats.setWinstreak(rs.getInt("winstreak"));
                     } catch (SQLException var13) {
                        stats.setWinstreak(0);
                     }

                     var34 = stats;
                  } catch (Throwable var30) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var12) {
                           var30.addSuppressed(var12);
                        }
                     }

                     throw var30;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label194;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var31) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var11) {
                     var31.addSuppressed(var11);
                  }
               }

               throw var31;
            }

            if (stmt != null) {
               stmt.close();
            }

            return new PlayerStats();
         }

         if (stmt != null) {
            stmt.close();
         }

         return var34;
      }
   }

   @Override
   public void savePlayerStats(UUID uuid, PlayerStats stats) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT OR REPLACE INTO player_stats (uuid, wins, defeats, profit_money, profit_playerpoints, profit_tokenmanager, profit_beasttokens, loss_money, loss_playerpoints, loss_tokenmanager, loss_beasttokens, wins_money, defeats_money, wins_playerpoints, defeats_playerpoints, wins_tokenmanager, defeats_tokenmanager, wins_beasttokens, defeats_beasttokens, wins_placeholder, defeats_placeholder, winstreak) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, stats.getWins());
            stmt.setInt(3, stats.getDefeats());
            stmt.setDouble(4, stats.getProfitMoney());
            stmt.setDouble(5, stats.getProfitPlayerPoints());
            stmt.setDouble(6, stats.getProfitTokenManager());
            stmt.setDouble(7, stats.getProfitBeastTokens());
            stmt.setDouble(8, stats.getLossMoney());
            stmt.setDouble(9, stats.getLossPlayerPoints());
            stmt.setDouble(10, stats.getLossTokenManager());
            stmt.setDouble(11, stats.getLossBeastTokens());
            stmt.setInt(12, stats.getWinsMoney());
            stmt.setInt(13, stats.getDefeatsMoney());
            stmt.setInt(14, stats.getWinsPlayerPoints());
            stmt.setInt(15, stats.getDefeatsPlayerPoints());
            stmt.setInt(16, stats.getWinsTokenManager());
            stmt.setInt(17, stats.getDefeatsTokenManager());
            stmt.setInt(18, stats.getWinsBeastTokens());
            stmt.setInt(19, stats.getDefeatsBeastTokens());
            stmt.setString(20, GSON.toJson(stats.getWinsPlaceholder() != null ? stats.getWinsPlaceholder() : new HashMap()));
            stmt.setString(21, GSON.toJson(stats.getDefeatsPlaceholder() != null ? stats.getDefeatsPlaceholder() : new HashMap()));
            stmt.setInt(22, stats.getWinstreak());
            stmt.executeUpdate();
         } catch (Throwable var10) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (stmt != null) {
            stmt.close();
         }
      }
   }

   @Override
   public void saveBackup(UUID uuid, String currencyType, double amount) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT OR REPLACE INTO backups (uuid, currency_type, amount) VALUES (?, ?, ?)";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, currencyType);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
         } catch (Throwable var12) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (stmt != null) {
            stmt.close();
         }
      }
   }

   @Override
   public DatabaseManager.BackupData loadBackup(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "SELECT * FROM backups WHERE uuid = ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         DatabaseManager.BackupData var6;
         label76: {
            try {
               stmt.setString(1, uuid.toString());
               ResultSet rs = stmt.executeQuery();

               label78: {
                  try {
                     if (!rs.next()) {
                        break label78;
                     }

                     var6 = new DatabaseManager.BackupData(rs.getString("currency_type"), rs.getDouble("amount"));
                  } catch (Throwable var11) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label76;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var12) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (stmt != null) {
               stmt.close();
            }

            return null;
         }

         if (stmt != null) {
            stmt.close();
         }

         return var6;
      }
   }

   @Override
   public void removeBackup(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "DELETE FROM backups WHERE uuid = ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
         } catch (Throwable var9) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stmt != null) {
            stmt.close();
         }
      }
   }

   @Override
   public List<UUID> getAllBackups() throws Exception {
      synchronized (this.dbLock) {
         List<UUID> backups = new ArrayList<>();
         String sql = "SELECT uuid FROM backups";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  try {
                     backups.add(UUID.fromString(rs.getString("uuid")));
                  } catch (IllegalArgumentException var11) {
                     this.plugin.getLogger().warning("Invalid UUID format in backups table: " + rs.getString("uuid"));
                  }
               }
            } catch (Throwable var12) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }
               }

               throw var12;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var13) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var9) {
                  var13.addSuppressed(var9);
               }
            }

            throw var13;
         }

         if (stmt != null) {
            stmt.close();
         }

         return backups;
      }
   }

   @Override
   public void saveCoinFlipLog(CoinFlipLog log) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT INTO coinflip_logs (player1_uuid, player1_name, player2_uuid, player2_name, winner_uuid, winner_name, loser_uuid, loser_name, currency_type, currency_id, amount, total_pot, tax_rate, tax_amount, winner_amount, timestamp, game_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, log.getPlayer1UUID().toString());
            stmt.setString(2, log.getPlayer1Name());
            stmt.setString(3, log.getPlayer2UUID().toString());
            stmt.setString(4, log.getPlayer2Name());
            stmt.setString(5, log.getWinnerUUID().toString());
            stmt.setString(6, log.getWinnerName());
            stmt.setString(7, log.getLoserUUID().toString());
            stmt.setString(8, log.getLoserName());
            stmt.setString(9, log.getCurrencyType().name());
            stmt.setString(10, log.getCurrencyId());
            stmt.setDouble(11, log.getAmount());
            stmt.setDouble(12, log.getTotalPot());
            stmt.setDouble(13, log.getTaxRate());
            stmt.setDouble(14, log.getTaxAmount());
            stmt.setDouble(15, log.getWinnerAmount());
            stmt.setLong(16, log.getTimestamp());
            CoinFlipLog.GameType gameType = log.getGameType();
            if (gameType == null) {
               gameType = CoinFlipLog.GameType.PLAYER;
            }

            stmt.setString(17, gameType.name());
            stmt.executeUpdate();
         } catch (Throwable var9) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stmt != null) {
            stmt.close();
         }
      }
   }

   @Override
   public List<CoinFlipLog> getPlayerLogs(UUID uuid, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<CoinFlipLog> logs = new ArrayList<>();
         String sql = "SELECT * FROM coinflip_logs WHERE player1_uuid = ? OR player2_uuid = ? ORDER BY timestamp DESC";
         if (limit > 0) {
            sql = sql + " LIMIT ?";
         }

         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, uuid.toString());
            if (limit > 0) {
               stmt.setInt(3, limit);
            }

            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  logs.add(this.resultSetToLog(rs));
               }
            } catch (Throwable var13) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }
               }

               throw var13;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var14) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var11) {
                  var14.addSuppressed(var11);
               }
            }

            throw var14;
         }

         if (stmt != null) {
            stmt.close();
         }

         return logs;
      }
   }

   @Override
   public List<CoinFlipLog> getAllLogs(int limit) throws Exception {
      synchronized (this.dbLock) {
         List<CoinFlipLog> logs = new ArrayList<>();
         String sql = "SELECT * FROM coinflip_logs ORDER BY timestamp DESC";
         if (limit > 0) {
            sql = sql + " LIMIT ?";
         }

         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            if (limit > 0) {
               stmt.setInt(1, limit);
            }

            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  logs.add(this.resultSetToLog(rs));
               }
            } catch (Throwable var12) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var13) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (stmt != null) {
            stmt.close();
         }

         return logs;
      }
   }

   @Override
   public List<CoinFlipLog> getLogsByTimeRange(long startTime, long endTime, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<CoinFlipLog> logs = new ArrayList<>();
         String sql = "SELECT * FROM coinflip_logs WHERE timestamp >= ? AND timestamp <= ? ORDER BY timestamp DESC";
         if (limit > 0) {
            sql = sql + " LIMIT ?";
         }

         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setLong(1, startTime);
            stmt.setLong(2, endTime);
            if (limit > 0) {
               stmt.setInt(3, limit);
            }

            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  logs.add(this.resultSetToLog(rs));
               }
            } catch (Throwable var16) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var17) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (stmt != null) {
            stmt.close();
         }

         return logs;
      }
   }

   @Override
   public void saveWaitingGameBackup(UUID gameId, UUID ownerUuid, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT OR REPLACE INTO waiting_backups (game_id, owner_uuid, currency_type, currency_id, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, gameId.toString());
            stmt.setString(2, ownerUuid.toString());
            stmt.setString(3, currencyType.name());
            if (currencyId != null) {
               stmt.setString(4, currencyId);
            } else {
               stmt.setNull(4, 12);
            }

            stmt.setDouble(5, amount);
            stmt.setLong(6, System.currentTimeMillis());
            stmt.executeUpdate();
         } catch (Throwable var14) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }
            }

            throw var14;
         }

         if (stmt != null) {
            stmt.close();
         }
      }
   }

   @Override
   public void removeWaitingGameBackup(UUID gameId) throws Exception {
      synchronized (this.dbLock) {
         String sql = "DELETE FROM waiting_backups WHERE game_id = ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, gameId.toString());
            stmt.executeUpdate();
         } catch (Throwable var9) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (stmt != null) {
            stmt.close();
         }
      }
   }

   @Override
   public List<DatabaseManager.WaitingGameBackup> loadWaitingGameBackups(UUID ownerUuid) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.WaitingGameBackup> backups = new ArrayList<>();
         String sql = "SELECT * FROM waiting_backups WHERE owner_uuid = ? ORDER BY created_at ASC";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setString(1, ownerUuid.toString());
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  DatabaseManager.WaitingGameBackup backup = this.mapWaitingBackup(rs);
                  if (backup != null) {
                     backups.add(backup);
                  }
               }
            } catch (Throwable var12) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var13) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (stmt != null) {
            stmt.close();
         }

         return backups;
      }
   }

   @Override
   public List<DatabaseManager.WaitingGameBackup> loadAllWaitingGameBackups() throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.WaitingGameBackup> backups = new ArrayList<>();
         String sql = "SELECT * FROM waiting_backups ORDER BY created_at ASC";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  DatabaseManager.WaitingGameBackup backup = this.mapWaitingBackup(rs);
                  if (backup != null) {
                     backups.add(backup);
                  }
               }
            } catch (Throwable var11) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var12) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (stmt != null) {
            stmt.close();
         }

         return backups;
      }
   }

   @Override
   public int deleteOldLogs(long olderThan) throws Exception {
      synchronized (this.dbLock) {
         String sql = "DELETE FROM coinflip_logs WHERE timestamp < ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         int var6;
         try {
            stmt.setLong(1, olderThan);
            var6 = stmt.executeUpdate();
         } catch (Throwable var10) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (stmt != null) {
            stmt.close();
         }

         return var6;
      }
   }

   private DatabaseManager.WaitingGameBackup mapWaitingBackup(ResultSet rs) throws SQLException {
      String currencyTypeString = rs.getString("currency_type");

      CoinFlipGame.CurrencyType currencyType;
      try {
         currencyType = CoinFlipGame.CurrencyType.valueOf(currencyTypeString);
      } catch (IllegalArgumentException var5) {
         currencyType = CoinFlipGame.CurrencyType.MONEY;
      }

      String currencyId = rs.getString("currency_id");
      if (currencyId != null && currencyId.trim().isEmpty()) {
         currencyId = null;
      }

      return new DatabaseManager.WaitingGameBackup(
         UUID.fromString(rs.getString("game_id")),
         UUID.fromString(rs.getString("owner_uuid")),
         currencyType,
         currencyId,
         rs.getDouble("amount"),
         rs.getLong("created_at")
      );
   }

   private CoinFlipLog resultSetToLog(ResultSet rs) throws SQLException {
      CoinFlipLog.GameType gameType = CoinFlipLog.GameType.PLAYER;

      try {
         String gameTypeStr = rs.getString("game_type");
         if (gameTypeStr != null && !gameTypeStr.isEmpty()) {
            gameType = CoinFlipLog.GameType.valueOf(gameTypeStr);
         }
      } catch (SQLException var4) {
         gameType = CoinFlipLog.GameType.PLAYER;
      }

      return new CoinFlipLog(
         rs.getLong("id"),
         UUID.fromString(rs.getString("player1_uuid")),
         rs.getString("player1_name"),
         UUID.fromString(rs.getString("player2_uuid")),
         rs.getString("player2_name"),
         UUID.fromString(rs.getString("winner_uuid")),
         rs.getString("winner_name"),
         UUID.fromString(rs.getString("loser_uuid")),
         rs.getString("loser_name"),
         CoinFlipGame.CurrencyType.valueOf(rs.getString("currency_type")),
         rs.getString("currency_id"),
         rs.getDouble("amount"),
         rs.getDouble("total_pot"),
         rs.getDouble("tax_rate"),
         rs.getDouble("tax_amount"),
         rs.getDouble("winner_amount"),
         rs.getLong("timestamp"),
         gameType
      );
   }

   @Override
   public List<DatabaseManager.LeaderboardEntry> getTopPlayersByWins(int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String sql = "SELECT uuid, wins FROM player_stats WHERE wins > 0 ORDER BY wins DESC LIMIT ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  try {
                     UUID uuid = UUID.fromString(rs.getString("uuid"));
                     String playerName = this.getPlayerName(uuid);
                     entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getInt("wins")));
                  } catch (IllegalArgumentException var12) {
                     this.plugin.getLogger().warning("Invalid UUID format in player_stats table: " + rs.getString("uuid"));
                  }
               }
            } catch (Throwable var13) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var11) {
                     var13.addSuppressed(var11);
                  }
               }

               throw var13;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var14) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var10) {
                  var14.addSuppressed(var10);
               }
            }

            throw var14;
         }

         if (stmt != null) {
            stmt.close();
         }

         return entries;
      }
   }

   @Override
   public List<DatabaseManager.LeaderboardEntry> getTopPlayersByProfit(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String profitColumn;
         String lossColumn;
         switch (currencyType) {
            case MONEY:
               profitColumn = "profit_money";
               lossColumn = "loss_money";
               break;
            case PLAYERPOINTS:
               profitColumn = "profit_playerpoints";
               lossColumn = "loss_playerpoints";
               break;
            case TOKENMANAGER:
               profitColumn = "profit_tokenmanager";
               lossColumn = "loss_tokenmanager";
               break;
            case BEASTTOKENS:
               profitColumn = "profit_beasttokens";
               lossColumn = "loss_beasttokens";
               break;
            default:
               return this.getTopPlayersByProfitFromLogs(currencyType, currencyId, limit);
         }

         String sql = "SELECT uuid, ("
            + profitColumn
            + " - "
            + lossColumn
            + ") as net_profit FROM player_stats WHERE ("
            + profitColumn
            + " - "
            + lossColumn
            + ") > 0 ORDER BY net_profit DESC LIMIT ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  UUID uuid = UUID.fromString(rs.getString("uuid"));
                  String playerName = this.getPlayerName(uuid);
                  entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("net_profit")));
               }
            } catch (Throwable var16) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var17) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (stmt != null) {
            stmt.close();
         }

         return entries;
      }
   }

   @Override
   public List<DatabaseManager.LeaderboardEntry> getTopPlayersByWorstProfit(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String profitColumn;
         String lossColumn;
         switch (currencyType) {
            case MONEY:
               profitColumn = "profit_money";
               lossColumn = "loss_money";
               break;
            case PLAYERPOINTS:
               profitColumn = "profit_playerpoints";
               lossColumn = "loss_playerpoints";
               break;
            case TOKENMANAGER:
               profitColumn = "profit_tokenmanager";
               lossColumn = "loss_tokenmanager";
               break;
            case BEASTTOKENS:
               profitColumn = "profit_beasttokens";
               lossColumn = "loss_beasttokens";
               break;
            default:
               return this.getTopPlayersByWorstProfitFromLogs(currencyType, currencyId, limit);
         }

         String sql = "SELECT uuid, ("
            + profitColumn
            + " - "
            + lossColumn
            + ") as net_profit FROM player_stats WHERE ("
            + profitColumn
            + " - "
            + lossColumn
            + ") < 0 ORDER BY net_profit ASC LIMIT ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  UUID uuid = UUID.fromString(rs.getString("uuid"));
                  String playerName = this.getPlayerName(uuid);
                  entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("net_profit")));
               }
            } catch (Throwable var16) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var17) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (stmt != null) {
            stmt.close();
         }

         return entries;
      }
   }

   @Override
   public List<DatabaseManager.LeaderboardEntry> getTopPlayersByLargestWin(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String sql;
         if (currencyId != null) {
            sql = "SELECT winner_uuid as uuid, winner_name as player_name, MAX(winner_amount) as max_win FROM coinflip_logs WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) AND (game_type = 'PLAYER' OR game_type IS NULL) GROUP BY winner_uuid, winner_name ORDER BY max_win DESC LIMIT ?";
         } else {
            sql = "SELECT winner_uuid as uuid, winner_name as player_name, MAX(winner_amount) as max_win FROM coinflip_logs WHERE currency_type = ? AND currency_id IS NULL AND (game_type = 'PLAYER' OR game_type IS NULL) GROUP BY winner_uuid, winner_name ORDER BY max_win DESC LIMIT ?";
         }

         try {
            PreparedStatement stmt = this.getConnection().prepareStatement(sql);

            try {
               int paramIndex = 1;
               stmt.setString(paramIndex++, currencyType.name());
               if (currencyId != null) {
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
               }

               stmt.setInt(paramIndex, limit);
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String playerName = rs.getString("player_name");
                        if (playerName == null || playerName.isEmpty()) {
                           playerName = this.getPlayerName(uuid);
                        }

                        entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("max_win")));
                     } catch (IllegalArgumentException var15) {
                        this.plugin.getLogger().warning("Skipping leaderboard entry with invalid UUID: " + var15.getMessage());
                     } catch (SQLException var16) {
                        this.plugin.getLogger().warning("Error reading leaderboard entry: " + var16.getMessage());
                     }
                  }
               } catch (Throwable var17) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var14) {
                        var17.addSuppressed(var14);
                     }
                  }

                  throw var17;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var18) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var13) {
                     var18.addSuppressed(var13);
                  }
               }

               throw var18;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (SQLException var19) {
            this.plugin.getLogger().severe("Database error in getTopPlayersByLargestWin: " + var19.getMessage());
            throw new Exception("Failed to retrieve leaderboard data", var19);
         }

         return entries;
      }
   }

   private List<DatabaseManager.LeaderboardEntry> getTopPlayersByProfitFromLogs(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String sql = "SELECT COALESCE(winner_stats.uuid, loser_stats.uuid) as uuid, COALESCE(winner_stats.player_name, loser_stats.player_name) as player_name, COALESCE(winner_stats.total_won, 0) - COALESCE(loser_stats.total_lost, 0) as net_profit FROM (  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won   FROM coinflip_logs   WHERE currency_type = ? "
            + (currencyId != null ? "AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) " : "")
            + "  GROUP BY winner_uuid, winner_name) winner_stats FULL OUTER JOIN (  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost   FROM coinflip_logs   WHERE currency_type = ? "
            + (currencyId != null ? "AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) " : "")
            + "  GROUP BY loser_uuid, loser_name) loser_stats ON winner_stats.uuid = loser_stats.uuid WHERE (COALESCE(winner_stats.total_won, 0) - COALESCE(loser_stats.total_lost, 0)) > 0 ORDER BY net_profit DESC LIMIT ?";
         StringBuilder sqlBuilder = new StringBuilder();
         if (currencyId != null) {
            sqlBuilder.append("WITH winner_totals AS (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append("), ")
               .append("loser_totals AS (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM winner_totals w ")
               .append("LEFT JOIN loser_totals l ON w.uuid = l.uuid ")
               .append("UNION ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM loser_totals l ")
               .append("LEFT JOIN winner_totals w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit DESC LIMIT ?");
         } else {
            sqlBuilder.append("WITH winner_totals AS (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append("), ")
               .append("loser_totals AS (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM winner_totals w ")
               .append("LEFT JOIN loser_totals l ON w.uuid = l.uuid ")
               .append("UNION ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM loser_totals l ")
               .append("LEFT JOIN winner_totals w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit DESC LIMIT ?");
         }

         sql = sqlBuilder.toString();
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            int paramIndex = 1;
            stmt.setString(paramIndex++, currencyType.name());
            if (currencyId != null) {
               stmt.setString(paramIndex++, currencyId);
               stmt.setString(paramIndex++, currencyId);
            }

            stmt.setString(paramIndex++, currencyType.name());
            if (currencyId != null) {
               stmt.setString(paramIndex++, currencyId);
               stmt.setString(paramIndex++, currencyId);
            }

            stmt.setInt(paramIndex, limit);
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  UUID uuid = UUID.fromString(rs.getString("uuid"));
                  String playerName = rs.getString("player_name");
                  if (playerName == null || playerName.isEmpty()) {
                     playerName = this.getPlayerName(uuid);
                  }

                  entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("net_profit")));
               }
            } catch (Throwable var16) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var17) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (stmt != null) {
            stmt.close();
         }

         return entries;
      }
   }

   private List<DatabaseManager.LeaderboardEntry> getTopPlayersByWorstProfitFromLogs(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         StringBuilder sqlBuilder = new StringBuilder();
         if (currencyId != null) {
            sqlBuilder.append("WITH winner_totals AS (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append("), ")
               .append("loser_totals AS (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM winner_totals w ")
               .append("LEFT JOIN loser_totals l ON w.uuid = l.uuid ")
               .append("WHERE (COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0)) < 0 ")
               .append("UNION ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM loser_totals l ")
               .append("LEFT JOIN winner_totals w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit ASC LIMIT ?");
         } else {
            sqlBuilder.append("WITH winner_totals AS (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append("), ")
               .append("loser_totals AS (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM winner_totals w ")
               .append("LEFT JOIN loser_totals l ON w.uuid = l.uuid ")
               .append("WHERE (COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0)) < 0 ")
               .append("UNION ")
               .append("SELECT ")
               .append("  COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("  COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("  COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM loser_totals l ")
               .append("LEFT JOIN winner_totals w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit ASC LIMIT ?");
         }

         String sql = sqlBuilder.toString();
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            int paramIndex = 1;
            stmt.setString(paramIndex++, currencyType.name());
            if (currencyId != null) {
               stmt.setString(paramIndex++, currencyId);
               stmt.setString(paramIndex++, currencyId);
            }

            stmt.setString(paramIndex++, currencyType.name());
            if (currencyId != null) {
               stmt.setString(paramIndex++, currencyId);
               stmt.setString(paramIndex++, currencyId);
            }

            stmt.setInt(paramIndex, limit);
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  UUID uuid = UUID.fromString(rs.getString("uuid"));
                  String playerName = rs.getString("player_name");
                  if (playerName == null || playerName.isEmpty()) {
                     playerName = this.getPlayerName(uuid);
                  }

                  entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("net_profit")));
               }
            } catch (Throwable var16) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var17) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (stmt != null) {
            stmt.close();
         }

         return entries;
      }
   }

   @Override
   public List<DatabaseManager.LeaderboardEntry> getTopPlayersByWinstreak(int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String sql = "SELECT uuid, winstreak FROM player_stats WHERE winstreak > 0 ORDER BY winstreak DESC LIMIT ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         try {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            try {
               while (rs.next()) {
                  UUID uuid = UUID.fromString(rs.getString("uuid"));
                  String playerName = this.getPlayerName(uuid);
                  entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getInt("winstreak")));
               }
            } catch (Throwable var12) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var13) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (stmt != null) {
            stmt.close();
         }

         return entries;
      }
   }

   @Override
   public DatabaseManager.BotGameStats getPlayerBotStats(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String uuidStr = uuid.toString();
         String sql = "SELECT SUM(CASE WHEN winner_uuid = ? THEN 1 ELSE 0 END) as wins, COUNT(*) as total_games, COALESCE(SUM(CASE WHEN winner_uuid = ? THEN winner_amount ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN loser_uuid = ? THEN amount ELSE 0 END), 0) as profit FROM coinflip_logs WHERE game_type = 'HOUSE' AND (player1_uuid = ? OR player2_uuid = ?)";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         DatabaseManager.BotGameStats var11;
         label76: {
            try {
               stmt.setString(1, uuidStr);
               stmt.setString(2, uuidStr);
               stmt.setString(3, uuidStr);
               stmt.setString(4, uuidStr);
               stmt.setString(5, uuidStr);
               ResultSet rs = stmt.executeQuery();

               label78: {
                  try {
                     if (!rs.next()) {
                        break label78;
                     }

                     int wins = rs.getInt("wins");
                     int totalGames = rs.getInt("total_games");
                     double profit = rs.getDouble("profit");
                     var11 = new DatabaseManager.BotGameStats(wins, totalGames, profit);
                  } catch (Throwable var15) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var14) {
                           var15.addSuppressed(var14);
                        }
                     }

                     throw var15;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label76;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var16) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var13) {
                     var16.addSuppressed(var13);
                  }
               }

               throw var16;
            }

            if (stmt != null) {
               stmt.close();
            }

            return new DatabaseManager.BotGameStats(0, 0, 0.0);
         }

         if (stmt != null) {
            stmt.close();
         }

         return var11;
      }
   }

   private String getPlayerName(UUID uuid) {
      try {
         OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
         if (offlinePlayer != null && offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
         }
      } catch (Exception var5) {
      }

      try {
         List<CoinFlipLog> logs = this.getPlayerLogs(uuid, 1);
         if (!logs.isEmpty()) {
            CoinFlipLog log = logs.get(0);
            if (log.getPlayer1UUID().equals(uuid)) {
               return log.getPlayer1Name();
            }

            if (log.getPlayer2UUID().equals(uuid)) {
               return log.getPlayer2Name();
            }
         }
      } catch (Exception var4) {
      }

      return "Unknown";
   }

   @Override
   public PlayerSettings loadPlayerSettings(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "SELECT settings FROM player_settings WHERE uuid = ?";
         PreparedStatement stmt = this.getConnection().prepareStatement(sql);

         PlayerSettings var10;
         label91: {
            try {
               ResultSet rs;
               label93: {
                  stmt.setString(1, uuid.toString());
                  rs = stmt.executeQuery();

                  try {
                     if (!rs.next()) {
                        break label93;
                     }

                     String settingsJson = rs.getString("settings");
                     if (settingsJson == null || settingsJson.trim().isEmpty()) {
                        break label93;
                     }

                     try {
                        Type type = (new TypeToken<Map<String, Boolean>>() {}).getType();
                        Map<String, Boolean> settingsMap = GSON.fromJson(settingsJson, type);
                        PlayerSettings playerSettings = new PlayerSettings();
                        if (settingsMap != null) {
                           playerSettings.setSettings(settingsMap);
                        }

                        var10 = playerSettings;
                     } catch (Exception var14) {
                        this.plugin.getLogger().warning("Failed to parse player settings JSON for " + uuid + ": " + var14.getMessage());
                        break label93;
                     }
                  } catch (Throwable var15) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var13) {
                           var15.addSuppressed(var13);
                        }
                     }

                     throw var15;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label91;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var16) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var12) {
                     var16.addSuppressed(var12);
                  }
               }

               throw var16;
            }

            if (stmt != null) {
               stmt.close();
            }

            return new PlayerSettings();
         }

         if (stmt != null) {
            stmt.close();
         }

         return var10;
      }
   }

   @Override
   public void savePlayerSettings(UUID uuid, PlayerSettings settings) throws Exception {
      synchronized (this.dbLock) {
         if (settings != null && !settings.isDefault()) {
            String sql = "INSERT OR REPLACE INTO player_settings (uuid, settings) VALUES (?, ?)";
            PreparedStatement stmt = this.getConnection().prepareStatement(sql);

            try {
               stmt.setString(1, uuid.toString());
               String settingsJson = GSON.toJson(settings.getSettings());
               stmt.setString(2, settingsJson);
               stmt.executeUpdate();
            } catch (Throwable var11) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var9) {
                     var11.addSuppressed(var9);
                  }
               }

               throw var11;
            }

            if (stmt != null) {
               stmt.close();
            }
         } else {
            String deleteSql = "DELETE FROM player_settings WHERE uuid = ?";
            PreparedStatement stmt = this.getConnection().prepareStatement(deleteSql);

            try {
               stmt.setString(1, uuid.toString());
               stmt.executeUpdate();
            } catch (Throwable var12) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var10) {
                     var12.addSuppressed(var10);
                  }
               }

               throw var12;
            }

            if (stmt != null) {
               stmt.close();
            }
         }
      }
   }

   @Generated
   public SQLiteDatabaseManager(KStudio plugin) {
      this.plugin = plugin;
   }
}
