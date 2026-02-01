package com.kstudio.ultracoinflip.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.CoinFlipLog;
import com.kstudio.ultracoinflip.data.PlayerSettings;
import com.kstudio.ultracoinflip.data.PlayerStats;
import com.kstudio.ultracoinflip.libs.hikaricp.java8.HikariConfig;
import com.kstudio.ultracoinflip.libs.hikaricp.java8.HikariDataSource;
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
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class MySQLDatabaseManager implements DatabaseManager {
   private final KStudio plugin;
   private HikariDataSource dataSource;
   private final String host;
   private final int port;
   private final String database;
   private final String username;
   private final String password;
   private final boolean ssl;
   private final int poolSize;
   private final long connectionTimeout;
   private final long idleTimeout;
   private final long maxLifetime;
   private final long leakDetectionThreshold;
   private final Object dbLock = new Object();
   private static final Gson GSON = new Gson();

   public MySQLDatabaseManager(KStudio plugin) {
      this.plugin = plugin;
      this.host = plugin.getConfig().getString("database.mysql.host", "localhost");
      this.port = plugin.getConfig().getInt("database.mysql.port", 3306);
      this.database = plugin.getConfig().getString("database.mysql.database", "ultracoinflip");
      this.username = plugin.getConfig().getString("database.mysql.username", "root");
      this.password = plugin.getConfig().getString("database.mysql.password", "password");
      this.ssl = plugin.getConfig().getBoolean("database.mysql.ssl", false);
      this.poolSize = plugin.getConfig().getInt("database.mysql.pool-size", 10);
      this.connectionTimeout = plugin.getConfig().getLong("database.mysql.connection-timeout", 10000L);
      this.idleTimeout = plugin.getConfig().getLong("database.mysql.idle-timeout", 600000L);
      this.maxLifetime = plugin.getConfig().getLong("database.mysql.max-lifetime", 1800000L);
      this.leakDetectionThreshold = plugin.getConfig().getLong("database.mysql.leak-detection-threshold", 60000L);
   }

   @Override
   public void initialize() throws Exception {
      this.plugin.getLogger().info("Attempting to connect to MySQL database using HikariCP...");
      this.plugin.getLogger().info("Host: " + this.host + ":" + this.port);
      this.plugin.getLogger().info("Database: " + this.database);
      this.plugin.getLogger().info("Username: " + this.username);
      this.plugin.getLogger().info("Pool size: " + this.poolSize);
      String baseUrl = "jdbc:mysql://"
         + this.host
         + ":"
         + this.port
         + "?useSSL="
         + this.ssl
         + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=utf8";

      try {
         try {
            Connection tempConnection = DriverManager.getConnection(baseUrl, this.username, this.password);

            try {
               try {
                  Statement stmt = tempConnection.createStatement();

                  try {
                     stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + this.database + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                     this.plugin.getLogger().info("Database '" + this.database + "' ready (created or already exists)");
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
               } catch (SQLException var14) {
                  if (var14.getMessage() != null && var14.getMessage().contains("Access denied")) {
                     this.plugin.getLogger().warning("Cannot create database (access denied). Assuming database already exists.");
                  } else {
                     this.plugin.getLogger().warning("Could not create database: " + var14.getMessage() + ". Assuming database already exists.");
                  }
               }
            } catch (Throwable var15) {
               if (tempConnection != null) {
                  try {
                     tempConnection.close();
                  } catch (Throwable var9) {
                     var15.addSuppressed(var9);
                  }
               }

               throw var15;
            }

            if (tempConnection != null) {
               tempConnection.close();
            }
         } catch (SQLException var16) {
            this.plugin.getLogger().info("Could not connect without database, will try direct connection to database...");
         }

         HikariConfig config = new HikariConfig();
         config.setJdbcUrl("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database);
         config.setUsername(this.username);
         config.setPassword(this.password);
         config.setMaximumPoolSize(this.poolSize);
         config.setMinimumIdle(Math.max(1, this.poolSize / 2));
         config.setConnectionTimeout(this.connectionTimeout);
         config.setIdleTimeout(this.idleTimeout);
         config.setMaxLifetime(this.maxLifetime);
         config.setLeakDetectionThreshold(this.leakDetectionThreshold);
         config.addDataSourceProperty("cachePrepStmts", "true");
         config.addDataSourceProperty("prepStmtCacheSize", "250");
         config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
         config.addDataSourceProperty("useServerPrepStmts", "true");
         config.addDataSourceProperty("useLocalSessionState", "true");
         config.addDataSourceProperty("rewriteBatchedStatements", "true");
         config.addDataSourceProperty("cacheResultSetMetadata", "true");
         config.addDataSourceProperty("cacheServerConfiguration", "true");
         config.addDataSourceProperty("elideSetAutoCommits", "true");
         config.addDataSourceProperty("maintainTimeStats", "false");
         config.addDataSourceProperty("useSSL", String.valueOf(this.ssl));
         config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
         config.addDataSourceProperty("serverTimezone", "UTC");
         config.addDataSourceProperty("useUnicode", "true");
         config.addDataSourceProperty("characterEncoding", "utf8");
         this.dataSource = new HikariDataSource(config);
         Connection testConnection = this.dataSource.getConnection();

         try {
            Statement stmt = testConnection.createStatement();

            try {
               stmt.executeQuery("SELECT 1").close();
            } catch (Throwable var11) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var12) {
            if (testConnection != null) {
               try {
                  testConnection.close();
               } catch (Throwable var7) {
                  var12.addSuppressed(var7);
               }
            }

            throw var12;
         }

         if (testConnection != null) {
            testConnection.close();
         }

         this.createTables();
         this.plugin.getLogger().info("MySQL database connected successfully using HikariCP connection pool!");
      } catch (Exception var17) {
         this.plugin.getLogger().severe("Failed to connect to MySQL database!");
         Throwable rootCause = var17;

         while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
         }

         String errorMsg = rootCause.getMessage();
         if (errorMsg == null) {
            errorMsg = var17.getMessage();
         }

         if (errorMsg == null) {
            errorMsg = "";
         }

         this.plugin.getLogger().severe("Error: " + errorMsg);
         if (!errorMsg.isEmpty()) {
            if (errorMsg.contains("Connection timed out") || errorMsg.contains("Communications link failure")) {
               this.plugin.getLogger().severe("Connection timeout - Please check:");
               this.plugin.getLogger().severe("  1. MySQL server is running");
               this.plugin.getLogger().severe("  2. Host and port are correct (current: " + this.host + ":" + this.port + ")");
               this.plugin.getLogger().severe("  3. Firewall allows connections on port " + this.port);
               this.plugin.getLogger().severe("  4. MySQL server allows remote connections (if not localhost)");
            } else if (errorMsg.contains("Access denied")) {
               if (errorMsg.contains("to database")) {
                  this.plugin.getLogger().severe("═══════════════════════════════════════════════════════════");
                  this.plugin.getLogger().severe("Access denied to database '" + this.database + "'!");
                  this.plugin.getLogger().severe("═══════════════════════════════════════════════════════════");
                  this.plugin.getLogger().severe("");
                  this.plugin.getLogger().severe("Possible causes:");
                  this.plugin.getLogger().severe("  1. Database name is incorrect in config.yml");
                  this.plugin.getLogger().severe("     Current database name: " + this.database);
                  this.plugin.getLogger().severe("     Please verify the correct database name from your hosting panel");
                  this.plugin.getLogger().severe("");
                  this.plugin.getLogger().severe("  2. User '" + this.username + "' does not have permission to access this database");
                  this.plugin.getLogger().severe("");
                  this.plugin.getLogger().severe("Solutions:");
                  this.plugin.getLogger().severe("  Option A: Update database name in config.yml:");
                  this.plugin.getLogger().severe("    database.mysql.database: <correct_database_name>");
                  this.plugin.getLogger().severe("");
                  this.plugin.getLogger().severe("  Option B: Grant permissions (if you have admin access):");
                  this.plugin.getLogger().severe("    GRANT ALL PRIVILEGES ON " + this.database + ".* TO '" + this.username + "'@'%';");
                  this.plugin.getLogger().severe("    FLUSH PRIVILEGES;");
                  this.plugin.getLogger().severe("═══════════════════════════════════════════════════════════");
               } else {
                  this.plugin.getLogger().severe("Access denied - Authentication failed!");
                  this.plugin.getLogger().severe("Please check:");
                  this.plugin.getLogger().severe("  1. Username is correct (current: " + this.username + ")");
                  this.plugin.getLogger().severe("  2. Password is correct");
                  this.plugin.getLogger().severe("  3. User exists in MySQL");
               }
            } else if (errorMsg.contains("Unknown database")) {
               this.plugin.getLogger().severe("Database '" + this.database + "' does not exist!");
               this.plugin.getLogger().severe("");
               this.plugin.getLogger().severe("To fix this, run these SQL commands in MySQL:");
               this.plugin.getLogger().severe("  CREATE DATABASE " + this.database + ";");
               this.plugin.getLogger().severe("  GRANT ALL PRIVILEGES ON " + this.database + ".* TO '" + this.username + "'@'%';");
               this.plugin.getLogger().severe("  FLUSH PRIVILEGES;");
            } else if (errorMsg.contains("Unsupported character encoding")) {
               this.plugin.getLogger().severe("Character encoding error - This should not happen with current configuration.");
               this.plugin.getLogger().severe("Please report this error to the plugin developer.");
            }
         }

         throw new Exception("Failed to initialize MySQL database connection pool", var17);
      }
   }

   private void createTables() throws SQLException {
      String createStatsTable = "CREATE TABLE IF NOT EXISTS player_stats (uuid VARCHAR(36) PRIMARY KEY, wins INT DEFAULT 0, defeats INT DEFAULT 0, profit_money DOUBLE DEFAULT 0, profit_playerpoints DOUBLE DEFAULT 0, profit_tokenmanager DOUBLE DEFAULT 0, profit_beasttokens DOUBLE DEFAULT 0, loss_money DOUBLE DEFAULT 0, loss_playerpoints DOUBLE DEFAULT 0, loss_tokenmanager DOUBLE DEFAULT 0, loss_beasttokens DOUBLE DEFAULT 0, wins_money INT DEFAULT 0, defeats_money INT DEFAULT 0, wins_playerpoints INT DEFAULT 0, defeats_playerpoints INT DEFAULT 0, wins_tokenmanager INT DEFAULT 0, defeats_tokenmanager INT DEFAULT 0, wins_beasttokens INT DEFAULT 0, defeats_beasttokens INT DEFAULT 0, wins_placeholder TEXT DEFAULT '{}', defeats_placeholder TEXT DEFAULT '{}', winstreak INT DEFAULT 0) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
      String createBackupsTable = "CREATE TABLE IF NOT EXISTS backups (uuid VARCHAR(36) PRIMARY KEY, currency_type VARCHAR(50) NOT NULL, amount DOUBLE NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
      String createWaitingBackupsTable = "CREATE TABLE IF NOT EXISTS waiting_backups (game_id VARCHAR(36) PRIMARY KEY, owner_uuid VARCHAR(36) NOT NULL, currency_type VARCHAR(50) NOT NULL, currency_id VARCHAR(100), amount DOUBLE NOT NULL, created_at BIGINT NOT NULL, INDEX idx_waiting_backups_owner (owner_uuid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
      String createLogsTable = "CREATE TABLE IF NOT EXISTS coinflip_logs (id BIGINT AUTO_INCREMENT PRIMARY KEY, player1_uuid VARCHAR(36) NOT NULL, player1_name VARCHAR(16) NOT NULL, player2_uuid VARCHAR(36) NOT NULL, player2_name VARCHAR(16) NOT NULL, winner_uuid VARCHAR(36) NOT NULL, winner_name VARCHAR(16) NOT NULL, loser_uuid VARCHAR(36) NOT NULL, loser_name VARCHAR(16) NOT NULL, currency_type VARCHAR(50) NOT NULL, currency_id VARCHAR(100), amount DOUBLE NOT NULL, total_pot DOUBLE NOT NULL, tax_rate DOUBLE NOT NULL, tax_amount DOUBLE NOT NULL, winner_amount DOUBLE NOT NULL, timestamp BIGINT NOT NULL, game_type VARCHAR(10) DEFAULT 'PLAYER', INDEX idx_logs_timestamp (timestamp DESC), INDEX idx_logs_player1 (player1_uuid), INDEX idx_logs_player2 (player2_uuid), INDEX idx_logs_winner (winner_uuid)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
      String createSettingsTable = "CREATE TABLE IF NOT EXISTS player_settings (uuid VARCHAR(36) PRIMARY KEY, settings TEXT DEFAULT '{}') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
      Connection connection = this.dataSource.getConnection();

      try {
         Statement stmt = connection.createStatement();

         try {
            stmt.execute(createStatsTable);
            stmt.execute(createBackupsTable);
            stmt.execute(createLogsTable);
            stmt.execute(createWaitingBackupsTable);
            stmt.execute(createSettingsTable);

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN profit_playerpoints DOUBLE DEFAULT 0");
            } catch (SQLException var29) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN loss_playerpoints DOUBLE DEFAULT 0");
            } catch (SQLException var28) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN profit_tokenmanager DOUBLE DEFAULT 0");
            } catch (SQLException var27) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN loss_tokenmanager DOUBLE DEFAULT 0");
            } catch (SQLException var26) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN profit_beasttokens DOUBLE DEFAULT 0");
            } catch (SQLException var25) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN loss_beasttokens DOUBLE DEFAULT 0");
            } catch (SQLException var24) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_money INT DEFAULT 0");
            } catch (SQLException var23) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_money INT DEFAULT 0");
            } catch (SQLException var22) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_playerpoints INT DEFAULT 0");
            } catch (SQLException var21) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_playerpoints INT DEFAULT 0");
            } catch (SQLException var20) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_tokenmanager INT DEFAULT 0");
            } catch (SQLException var19) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_tokenmanager INT DEFAULT 0");
            } catch (SQLException var18) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_beasttokens INT DEFAULT 0");
            } catch (SQLException var17) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_beasttokens INT DEFAULT 0");
            } catch (SQLException var16) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN wins_placeholder TEXT DEFAULT '{}'");
            } catch (SQLException var15) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN defeats_placeholder TEXT DEFAULT '{}'");
            } catch (SQLException var14) {
            }

            try {
               stmt.execute("ALTER TABLE player_stats ADD COLUMN winstreak INT DEFAULT 0");
            } catch (SQLException var13) {
            }

            try {
               stmt.execute("ALTER TABLE coinflip_logs ADD COLUMN game_type VARCHAR(10) DEFAULT 'PLAYER'");
            } catch (SQLException var12) {
            }
         } catch (Throwable var30) {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (Throwable var11) {
                  var30.addSuppressed(var11);
               }
            }

            throw var30;
         }

         if (stmt != null) {
            stmt.close();
         }
      } catch (Throwable var31) {
         if (connection != null) {
            try {
               connection.close();
            } catch (Throwable var10) {
               var31.addSuppressed(var10);
            }
         }

         throw var31;
      }

      if (connection != null) {
         connection.close();
      }
   }

   @Override
   public void close() throws Exception {
      if (this.dataSource != null && !this.dataSource.isClosed()) {
         this.dataSource.close();
         this.plugin.getLogger().info("HikariCP connection pool closed.");
      }
   }

   @Override
   public Connection getConnection() throws Exception {
      synchronized (this.dbLock) {
         if (this.dataSource != null && !this.dataSource.isClosed()) {
            return this.dataSource.getConnection();
         } else {
            throw new SQLException("Database connection pool is not initialized. Call initialize() first.");
         }
      }
   }

   @Override
   public PlayerStats loadPlayerStats(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "SELECT * FROM player_stats WHERE uuid = ?";
         Connection connection = this.getConnection();

         PlayerStats var37;
         label222: {
            try {
               PreparedStatement stmt;
               label224: {
                  stmt = connection.prepareStatement(sql);

                  try {
                     stmt.setString(1, uuid.toString());
                     ResultSet rs = stmt.executeQuery();

                     label205: {
                        try {
                           if (rs.next()) {
                              PlayerStats stats = new PlayerStats();
                              stats.setWins(rs.getInt("wins"));
                              stats.setDefeats(rs.getInt("defeats"));
                              stats.setProfitMoney(rs.getDouble("profit_money"));

                              try {
                                 stats.setProfitPlayerPoints(rs.getDouble("profit_playerpoints"));
                              } catch (SQLException var31) {
                                 stats.setProfitPlayerPoints(0.0);
                              }

                              stats.setLossMoney(rs.getDouble("loss_money"));

                              try {
                                 stats.setLossPlayerPoints(rs.getDouble("loss_playerpoints"));
                              } catch (SQLException var30) {
                                 stats.setLossPlayerPoints(0.0);
                              }

                              try {
                                 stats.setProfitTokenManager(rs.getDouble("profit_tokenmanager"));
                              } catch (SQLException var29) {
                                 stats.setProfitTokenManager(0.0);
                              }

                              try {
                                 stats.setProfitBeastTokens(rs.getDouble("profit_beasttokens"));
                              } catch (SQLException var28) {
                                 stats.setProfitBeastTokens(0.0);
                              }

                              try {
                                 stats.setLossTokenManager(rs.getDouble("loss_tokenmanager"));
                              } catch (SQLException var27) {
                                 stats.setLossTokenManager(0.0);
                              }

                              try {
                                 stats.setLossBeastTokens(rs.getDouble("loss_beasttokens"));
                              } catch (SQLException var26) {
                                 stats.setLossBeastTokens(0.0);
                              }

                              try {
                                 stats.setWinsMoney(rs.getInt("wins_money"));
                              } catch (SQLException var25) {
                                 stats.setWinsMoney(0);
                              }

                              try {
                                 stats.setDefeatsMoney(rs.getInt("defeats_money"));
                              } catch (SQLException var24) {
                                 stats.setDefeatsMoney(0);
                              }

                              try {
                                 stats.setWinsPlayerPoints(rs.getInt("wins_playerpoints"));
                              } catch (SQLException var23) {
                                 stats.setWinsPlayerPoints(0);
                              }

                              try {
                                 stats.setDefeatsPlayerPoints(rs.getInt("defeats_playerpoints"));
                              } catch (SQLException var22) {
                                 stats.setDefeatsPlayerPoints(0);
                              }

                              try {
                                 stats.setWinsTokenManager(rs.getInt("wins_tokenmanager"));
                              } catch (SQLException var21) {
                                 stats.setWinsTokenManager(0);
                              }

                              try {
                                 stats.setDefeatsTokenManager(rs.getInt("defeats_tokenmanager"));
                              } catch (SQLException var20) {
                                 stats.setDefeatsTokenManager(0);
                              }

                              try {
                                 stats.setWinsBeastTokens(rs.getInt("wins_beasttokens"));
                              } catch (SQLException var19) {
                                 stats.setWinsBeastTokens(0);
                              }

                              try {
                                 stats.setDefeatsBeastTokens(rs.getInt("defeats_beasttokens"));
                              } catch (SQLException var18) {
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
                              } catch (Exception var17) {
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
                              } catch (Exception var16) {
                                 stats.setDefeatsPlaceholder(new HashMap<>());
                              }

                              try {
                                 stats.setWinstreak(rs.getInt("winstreak"));
                              } catch (SQLException var15) {
                                 stats.setWinstreak(0);
                              }

                              var37 = stats;
                              break label205;
                           }
                        } catch (Throwable var32) {
                           if (rs != null) {
                              try {
                                 rs.close();
                              } catch (Throwable var14) {
                                 var32.addSuppressed(var14);
                              }
                           }

                           throw var32;
                        }

                        if (rs != null) {
                           rs.close();
                        }
                        break label224;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                  } catch (Throwable var33) {
                     if (stmt != null) {
                        try {
                           stmt.close();
                        } catch (Throwable var13) {
                           var33.addSuppressed(var13);
                        }
                     }

                     throw var33;
                  }

                  if (stmt != null) {
                     stmt.close();
                  }
                  break label222;
               }

               if (stmt != null) {
                  stmt.close();
               }
            } catch (Throwable var34) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var12) {
                     var34.addSuppressed(var12);
                  }
               }

               throw var34;
            }

            if (connection != null) {
               connection.close();
            }

            return new PlayerStats();
         }

         if (connection != null) {
            connection.close();
         }

         return var37;
      }
   }

   @Override
   public void savePlayerStats(UUID uuid, PlayerStats stats) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT INTO player_stats (uuid, wins, defeats, profit_money, profit_playerpoints, profit_tokenmanager, profit_beasttokens, loss_money, loss_playerpoints, loss_tokenmanager, loss_beasttokens, wins_money, defeats_money, wins_playerpoints, defeats_playerpoints, wins_tokenmanager, defeats_tokenmanager, wins_beasttokens, defeats_beasttokens, wins_placeholder, defeats_placeholder, winstreak) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE wins = VALUES(wins), defeats = VALUES(defeats), profit_money = VALUES(profit_money), profit_playerpoints = VALUES(profit_playerpoints), profit_tokenmanager = VALUES(profit_tokenmanager), profit_beasttokens = VALUES(profit_beasttokens), loss_money = VALUES(loss_money), loss_playerpoints = VALUES(loss_playerpoints), loss_tokenmanager = VALUES(loss_tokenmanager), loss_beasttokens = VALUES(loss_beasttokens), wins_money = VALUES(wins_money), defeats_money = VALUES(defeats_money), wins_playerpoints = VALUES(wins_playerpoints), defeats_playerpoints = VALUES(defeats_playerpoints), wins_tokenmanager = VALUES(wins_tokenmanager), defeats_tokenmanager = VALUES(defeats_tokenmanager), wins_beasttokens = VALUES(wins_beasttokens), defeats_beasttokens = VALUES(defeats_beasttokens), wins_placeholder = VALUES(wins_placeholder), defeats_placeholder = VALUES(defeats_placeholder), winstreak = VALUES(winstreak)";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
         } catch (Throwable var13) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public void saveBackup(UUID uuid, String currencyType, double amount) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT INTO backups (uuid, currency_type, amount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE currency_type = VALUES(currency_type), amount = VALUES(amount)";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setString(1, uuid.toString());
               stmt.setString(2, currencyType);
               stmt.setDouble(3, amount);
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
         } catch (Throwable var15) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var12) {
                  var15.addSuppressed(var12);
               }
            }

            throw var15;
         }

         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public DatabaseManager.BackupData loadBackup(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "SELECT * FROM backups WHERE uuid = ?";
         Connection connection = this.getConnection();

         DatabaseManager.BackupData var7;
         label104: {
            try {
               PreparedStatement stmt;
               label106: {
                  stmt = connection.prepareStatement(sql);

                  try {
                     stmt.setString(1, uuid.toString());
                     ResultSet rs = stmt.executeQuery();

                     label87: {
                        try {
                           if (rs.next()) {
                              var7 = new DatabaseManager.BackupData(rs.getString("currency_type"), rs.getDouble("amount"));
                              break label87;
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
                        break label106;
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
                  break label104;
               }

               if (stmt != null) {
                  stmt.close();
               }
            } catch (Throwable var15) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var10) {
                     var15.addSuppressed(var10);
                  }
               }

               throw var15;
            }

            if (connection != null) {
               connection.close();
            }

            return null;
         }

         if (connection != null) {
            connection.close();
         }

         return var7;
      }
   }

   @Override
   public void removeBackup(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String sql = "DELETE FROM backups WHERE uuid = ?";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setString(1, uuid.toString());
               stmt.executeUpdate();
            } catch (Throwable var11) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var12) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public List<UUID> getAllBackups() throws Exception {
      synchronized (this.dbLock) {
         List<UUID> backups = new ArrayList<>();
         String sql = "SELECT uuid FROM backups";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     try {
                        backups.add(UUID.fromString(rs.getString("uuid")));
                     } catch (IllegalArgumentException var13) {
                        this.plugin.getLogger().warning("Invalid UUID format in backups table: " + rs.getString("uuid"));
                     }
                  }
               } catch (Throwable var14) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var12) {
                        var14.addSuppressed(var12);
                     }
                  }

                  throw var14;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var15) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var11) {
                     var15.addSuppressed(var11);
                  }
               }

               throw var15;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var16) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var10) {
                  var16.addSuppressed(var10);
               }
            }

            throw var16;
         }

         if (connection != null) {
            connection.close();
         }

         return backups;
      }
   }

   @Override
   public void saveCoinFlipLog(CoinFlipLog log) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT INTO coinflip_logs (player1_uuid, player1_name, player2_uuid, player2_name, winner_uuid, winner_name, loser_uuid, loser_name, currency_type, currency_id, amount, total_pot, tax_rate, tax_amount, winner_amount, timestamp, game_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
            } catch (Throwable var11) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var12) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (connection != null) {
            connection.close();
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

         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
         } catch (Throwable var17) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var12) {
                  var17.addSuppressed(var12);
               }
            }

            throw var17;
         }

         if (connection != null) {
            connection.close();
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

         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               if (limit > 0) {
                  stmt.setInt(1, limit);
               }

               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     logs.add(this.resultSetToLog(rs));
                  }
               } catch (Throwable var14) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var15) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }
               }

               throw var15;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var16) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var11) {
                  var16.addSuppressed(var11);
               }
            }

            throw var16;
         }

         if (connection != null) {
            connection.close();
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

         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
               } catch (Throwable var18) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var19) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }
               }

               throw var19;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var20) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var15) {
                  var20.addSuppressed(var15);
               }
            }

            throw var20;
         }

         if (connection != null) {
            connection.close();
         }

         return logs;
      }
   }

   @Override
   public void saveWaitingGameBackup(UUID gameId, UUID ownerUuid, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) throws Exception {
      synchronized (this.dbLock) {
         String sql = "INSERT INTO waiting_backups (game_id, owner_uuid, currency_type, currency_id, amount, created_at) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE owner_uuid = VALUES(owner_uuid), currency_type = VALUES(currency_type), currency_id = VALUES(currency_id), amount = VALUES(amount), created_at = VALUES(created_at)";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
            } catch (Throwable var16) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var17) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var14) {
                  var17.addSuppressed(var14);
               }
            }

            throw var17;
         }

         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public void removeWaitingGameBackup(UUID gameId) throws Exception {
      synchronized (this.dbLock) {
         String sql = "DELETE FROM waiting_backups WHERE game_id = ?";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setString(1, gameId.toString());
               stmt.executeUpdate();
            } catch (Throwable var11) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var12) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (connection != null) {
            connection.close();
         }
      }
   }

   @Override
   public List<DatabaseManager.WaitingGameBackup> loadWaitingGameBackups(UUID ownerUuid) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.WaitingGameBackup> backups = new ArrayList<>();
         String sql = "SELECT * FROM waiting_backups WHERE owner_uuid = ? ORDER BY created_at ASC";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
               } catch (Throwable var14) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var15) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }
               }

               throw var15;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var16) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var11) {
                  var16.addSuppressed(var11);
               }
            }

            throw var16;
         }

         if (connection != null) {
            connection.close();
         }

         return backups;
      }
   }

   @Override
   public List<DatabaseManager.WaitingGameBackup> loadAllWaitingGameBackups() throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.WaitingGameBackup> backups = new ArrayList<>();
         String sql = "SELECT * FROM waiting_backups ORDER BY created_at ASC";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     DatabaseManager.WaitingGameBackup backup = this.mapWaitingBackup(rs);
                     if (backup != null) {
                        backups.add(backup);
                     }
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
         } catch (Throwable var15) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var10) {
                  var15.addSuppressed(var10);
               }
            }

            throw var15;
         }

         if (connection != null) {
            connection.close();
         }

         return backups;
      }
   }

   @Override
   public int deleteOldLogs(long olderThan) throws Exception {
      synchronized (this.dbLock) {
         String sql = "DELETE FROM coinflip_logs WHERE timestamp < ?";
         Connection connection = this.getConnection();

         int var7;
         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setLong(1, olderThan);
               var7 = stmt.executeUpdate();
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
         } catch (Throwable var13) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (connection != null) {
            connection.close();
         }

         return var7;
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
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setInt(1, limit);
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     try {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        String playerName = this.getPlayerName(uuid);
                        entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getInt("wins")));
                     } catch (IllegalArgumentException var14) {
                        this.plugin.getLogger().warning("Invalid UUID format in player_stats table: " + rs.getString("uuid"));
                     }
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
         } catch (Throwable var17) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var11) {
                  var17.addSuppressed(var11);
               }
            }

            throw var17;
         }

         if (connection != null) {
            connection.close();
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
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setInt(1, limit);
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     UUID uuid = UUID.fromString(rs.getString("uuid"));
                     String playerName = this.getPlayerName(uuid);
                     entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("net_profit")));
                  }
               } catch (Throwable var18) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var19) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }
               }

               throw var19;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var20) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var15) {
                  var20.addSuppressed(var15);
               }
            }

            throw var20;
         }

         if (connection != null) {
            connection.close();
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
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setInt(1, limit);
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     UUID uuid = UUID.fromString(rs.getString("uuid"));
                     String playerName = this.getPlayerName(uuid);
                     entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("net_profit")));
                  }
               } catch (Throwable var18) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var19) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }
               }

               throw var19;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var20) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var15) {
                  var20.addSuppressed(var15);
               }
            }

            throw var20;
         }

         if (connection != null) {
            connection.close();
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

         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

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
                     UUID uuid = UUID.fromString(rs.getString("uuid"));
                     String playerName = rs.getString("player_name");
                     if (playerName == null || playerName.isEmpty()) {
                        playerName = this.getPlayerName(uuid);
                     }

                     entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getDouble("max_win")));
                  }
               } catch (Throwable var17) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
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
                  } catch (Throwable var15) {
                     var18.addSuppressed(var15);
                  }
               }

               throw var18;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var19) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var14) {
                  var19.addSuppressed(var14);
               }
            }

            throw var19;
         }

         if (connection != null) {
            connection.close();
         }

         return entries;
      }
   }

   private List<DatabaseManager.LeaderboardEntry> getTopPlayersByProfitFromLogs(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         if (currencyId != null) {
            String sql = "SELECT COALESCE(winner_stats.uuid, loser_stats.uuid) as uuid, COALESCE(winner_stats.player_name, loser_stats.player_name) as player_name, COALESCE(winner_stats.total_won, 0) - COALESCE(loser_stats.total_lost, 0) as net_profit FROM (  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won   FROM coinflip_logs   WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL))   AND (game_type = 'PLAYER' OR game_type IS NULL)   GROUP BY winner_uuid, winner_name) winner_stats FULL OUTER JOIN (  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost   FROM coinflip_logs   WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL))   AND (game_type = 'PLAYER' OR game_type IS NULL)   GROUP BY loser_uuid, loser_name) loser_stats ON winner_stats.uuid = loser_stats.uuid WHERE (COALESCE(winner_stats.total_won, 0) - COALESCE(loser_stats.total_lost, 0)) > 0 ORDER BY net_profit DESC LIMIT ?";
         } else {
            String sql = "SELECT COALESCE(winner_stats.uuid, loser_stats.uuid) as uuid, COALESCE(winner_stats.player_name, loser_stats.player_name) as player_name, COALESCE(winner_stats.total_won, 0) - COALESCE(loser_stats.total_lost, 0) as net_profit FROM (  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won   FROM coinflip_logs   WHERE currency_type = ? AND currency_id IS NULL   AND (game_type = 'PLAYER' OR game_type IS NULL)   GROUP BY winner_uuid, winner_name) winner_stats FULL OUTER JOIN (  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost   FROM coinflip_logs   WHERE currency_type = ? AND currency_id IS NULL   AND (game_type = 'PLAYER' OR game_type IS NULL)   GROUP BY loser_uuid, loser_name) loser_stats ON winner_stats.uuid = loser_stats.uuid WHERE (COALESCE(winner_stats.total_won, 0) - COALESCE(loser_stats.total_lost, 0)) > 0 ORDER BY net_profit DESC LIMIT ?";
         }

         StringBuilder sqlBuilder = new StringBuilder();
         if (currencyId != null) {
            sqlBuilder.append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ")
               .append("LEFT JOIN (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ON w.uuid = l.uuid ")
               .append("UNION ")
               .append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ")
               .append("LEFT JOIN (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit DESC LIMIT ?");
         } else {
            sqlBuilder.append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ")
               .append("LEFT JOIN (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ON w.uuid = l.uuid ")
               .append("UNION ")
               .append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ")
               .append("LEFT JOIN (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit DESC LIMIT ?");
         }

         String var23 = sqlBuilder.toString();
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(var23);

            try {
               int paramIndex = 1;
               if (currencyId != null) {
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
               } else {
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyType.name());
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
               } catch (Throwable var18) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var19) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }
               }

               throw var19;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var20) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var15) {
                  var20.addSuppressed(var15);
               }
            }

            throw var20;
         }

         if (connection != null) {
            connection.close();
         }

         return entries;
      }
   }

   private List<DatabaseManager.LeaderboardEntry> getTopPlayersByWorstProfitFromLogs(CoinFlipGame.CurrencyType currencyType, String currencyId, int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         StringBuilder sqlBuilder = new StringBuilder();
         if (currencyId != null) {
            sqlBuilder.append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ")
               .append("LEFT JOIN (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ON w.uuid = l.uuid ")
               .append("WHERE (COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0)) < 0 ")
               .append("UNION ")
               .append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ")
               .append("LEFT JOIN (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND (currency_id = ? OR (currency_id IS NULL AND ? IS NULL)) ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit ASC LIMIT ?");
         } else {
            sqlBuilder.append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ")
               .append("LEFT JOIN (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ON w.uuid = l.uuid ")
               .append("WHERE (COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0)) < 0 ")
               .append("UNION ")
               .append("SELECT ")
               .append("COALESCE(w.uuid, l.uuid) as uuid, ")
               .append("COALESCE(w.player_name, l.player_name) as player_name, ")
               .append("COALESCE(w.total_won, 0) - COALESCE(l.total_lost, 0) as net_profit ")
               .append("FROM (")
               .append("  SELECT loser_uuid as uuid, loser_name as player_name, SUM(amount) as total_lost ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY loser_uuid, loser_name")
               .append(") l ")
               .append("LEFT JOIN (")
               .append("  SELECT winner_uuid as uuid, winner_name as player_name, SUM(winner_amount) as total_won ")
               .append("  FROM coinflip_logs ")
               .append("  WHERE currency_type = ? AND currency_id IS NULL ")
               .append("  AND (game_type = 'PLAYER' OR game_type IS NULL) ")
               .append("  GROUP BY winner_uuid, winner_name")
               .append(") w ON l.uuid = w.uuid ")
               .append("WHERE w.uuid IS NULL ")
               .append("ORDER BY net_profit ASC LIMIT ?");
         }

         String sql = sqlBuilder.toString();
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               int paramIndex = 1;
               if (currencyId != null) {
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyId);
                  stmt.setString(paramIndex++, currencyId);
               } else {
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyType.name());
                  stmt.setString(paramIndex++, currencyType.name());
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
               } catch (Throwable var18) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var17) {
                        var18.addSuppressed(var17);
                     }
                  }

                  throw var18;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var19) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var16) {
                     var19.addSuppressed(var16);
                  }
               }

               throw var19;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var20) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var15) {
                  var20.addSuppressed(var15);
               }
            }

            throw var20;
         }

         if (connection != null) {
            connection.close();
         }

         return entries;
      }
   }

   @Override
   public List<DatabaseManager.LeaderboardEntry> getTopPlayersByWinstreak(int limit) throws Exception {
      synchronized (this.dbLock) {
         List<DatabaseManager.LeaderboardEntry> entries = new ArrayList<>();
         String sql = "SELECT uuid, winstreak FROM player_stats WHERE winstreak > 0 ORDER BY winstreak DESC LIMIT ?";
         Connection connection = this.getConnection();

         try {
            PreparedStatement stmt = connection.prepareStatement(sql);

            try {
               stmt.setInt(1, limit);
               ResultSet rs = stmt.executeQuery();

               try {
                  while (rs.next()) {
                     UUID uuid = UUID.fromString(rs.getString("uuid"));
                     String playerName = this.getPlayerName(uuid);
                     entries.add(new DatabaseManager.LeaderboardEntry(uuid, playerName, rs.getInt("winstreak")));
                  }
               } catch (Throwable var14) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var15) {
               if (stmt != null) {
                  try {
                     stmt.close();
                  } catch (Throwable var12) {
                     var15.addSuppressed(var12);
                  }
               }

               throw var15;
            }

            if (stmt != null) {
               stmt.close();
            }
         } catch (Throwable var16) {
            if (connection != null) {
               try {
                  connection.close();
               } catch (Throwable var11) {
                  var16.addSuppressed(var11);
               }
            }

            throw var16;
         }

         if (connection != null) {
            connection.close();
         }

         return entries;
      }
   }

   @Override
   public DatabaseManager.BotGameStats getPlayerBotStats(UUID uuid) throws Exception {
      synchronized (this.dbLock) {
         String uuidStr = uuid.toString();
         String sql = "SELECT SUM(CASE WHEN winner_uuid = ? THEN 1 ELSE 0 END) as wins, COUNT(*) as total_games, COALESCE(SUM(CASE WHEN winner_uuid = ? THEN winner_amount ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN loser_uuid = ? THEN amount ELSE 0 END), 0) as profit FROM coinflip_logs WHERE game_type = 'HOUSE' AND (player1_uuid = ? OR player2_uuid = ?)";
         Connection connection = this.getConnection();

         DatabaseManager.BotGameStats var12;
         label104: {
            try {
               PreparedStatement stmt;
               label106: {
                  stmt = connection.prepareStatement(sql);

                  try {
                     stmt.setString(1, uuidStr);
                     stmt.setString(2, uuidStr);
                     stmt.setString(3, uuidStr);
                     stmt.setString(4, uuidStr);
                     stmt.setString(5, uuidStr);
                     ResultSet rs = stmt.executeQuery();

                     label87: {
                        try {
                           if (rs.next()) {
                              int wins = rs.getInt("wins");
                              int totalGames = rs.getInt("total_games");
                              double profit = rs.getDouble("profit");
                              var12 = new DatabaseManager.BotGameStats(wins, totalGames, profit);
                              break label87;
                           }
                        } catch (Throwable var17) {
                           if (rs != null) {
                              try {
                                 rs.close();
                              } catch (Throwable var16) {
                                 var17.addSuppressed(var16);
                              }
                           }

                           throw var17;
                        }

                        if (rs != null) {
                           rs.close();
                        }
                        break label106;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                  } catch (Throwable var18) {
                     if (stmt != null) {
                        try {
                           stmt.close();
                        } catch (Throwable var15) {
                           var18.addSuppressed(var15);
                        }
                     }

                     throw var18;
                  }

                  if (stmt != null) {
                     stmt.close();
                  }
                  break label104;
               }

               if (stmt != null) {
                  stmt.close();
               }
            } catch (Throwable var19) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var14) {
                     var19.addSuppressed(var14);
                  }
               }

               throw var19;
            }

            if (connection != null) {
               connection.close();
            }

            return new DatabaseManager.BotGameStats(0, 0, 0.0);
         }

         if (connection != null) {
            connection.close();
         }

         return var12;
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
         Connection connection = this.getConnection();

         PlayerSettings var11;
         label117: {
            try {
               PreparedStatement stmt;
               label119: {
                  stmt = connection.prepareStatement(sql);

                  try {
                     stmt.setString(1, uuid.toString());
                     ResultSet rs = stmt.executeQuery();

                     label100: {
                        try {
                           if (rs.next()) {
                              String settingsJson = rs.getString("settings");
                              if (settingsJson != null && !settingsJson.trim().isEmpty()) {
                                 try {
                                    Type type = (new TypeToken<Map<String, Boolean>>() {}).getType();
                                    Map<String, Boolean> settingsMap = GSON.fromJson(settingsJson, type);
                                    PlayerSettings playerSettings = new PlayerSettings();
                                    if (settingsMap != null) {
                                       playerSettings.setSettings(settingsMap);
                                    }

                                    var11 = playerSettings;
                                    break label100;
                                 } catch (Exception var16) {
                                    this.plugin.getLogger().warning("Failed to parse player settings JSON for " + uuid + ": " + var16.getMessage());
                                 }
                              }
                           }
                        } catch (Throwable var17) {
                           if (rs != null) {
                              try {
                                 rs.close();
                              } catch (Throwable var15) {
                                 var17.addSuppressed(var15);
                              }
                           }

                           throw var17;
                        }

                        if (rs != null) {
                           rs.close();
                        }
                        break label119;
                     }

                     if (rs != null) {
                        rs.close();
                     }
                  } catch (Throwable var18) {
                     if (stmt != null) {
                        try {
                           stmt.close();
                        } catch (Throwable var14) {
                           var18.addSuppressed(var14);
                        }
                     }

                     throw var18;
                  }

                  if (stmt != null) {
                     stmt.close();
                  }
                  break label117;
               }

               if (stmt != null) {
                  stmt.close();
               }
            } catch (Throwable var19) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var13) {
                     var19.addSuppressed(var13);
                  }
               }

               throw var19;
            }

            if (connection != null) {
               connection.close();
            }

            return new PlayerSettings();
         }

         if (connection != null) {
            connection.close();
         }

         return var11;
      }
   }

   @Override
   public void savePlayerSettings(UUID uuid, PlayerSettings settings) throws Exception {
      synchronized (this.dbLock) {
         if (settings != null && !settings.isDefault()) {
            String sql = "INSERT INTO player_settings (uuid, settings) VALUES (?, ?) ON DUPLICATE KEY UPDATE settings = VALUES(settings)";
            Connection connection = this.getConnection();

            try {
               PreparedStatement stmt = connection.prepareStatement(sql);

               try {
                  stmt.setString(1, uuid.toString());
                  String settingsJson = GSON.toJson(settings.getSettings());
                  stmt.setString(2, settingsJson);
                  stmt.executeUpdate();
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
            } catch (Throwable var16) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var10) {
                     var16.addSuppressed(var10);
                  }
               }

               throw var16;
            }

            if (connection != null) {
               connection.close();
            }
         } else {
            String deleteSql = "DELETE FROM player_settings WHERE uuid = ?";
            Connection connection = this.getConnection();

            try {
               PreparedStatement stmt = connection.prepareStatement(deleteSql);

               try {
                  stmt.setString(1, uuid.toString());
                  stmt.executeUpdate();
               } catch (Throwable var15) {
                  if (stmt != null) {
                     try {
                        stmt.close();
                     } catch (Throwable var13) {
                        var15.addSuppressed(var13);
                     }
                  }

                  throw var15;
               }

               if (stmt != null) {
                  stmt.close();
               }
            } catch (Throwable var17) {
               if (connection != null) {
                  try {
                     connection.close();
                  } catch (Throwable var12) {
                     var17.addSuppressed(var12);
                  }
               }

               throw var17;
            }

            if (connection != null) {
               connection.close();
            }
         }
      }
   }
}
