package com.kstudio.ultracoinflip.refund;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class TransactionLogger {
   private final KStudio plugin;
   private final File logFile;
   private final BlockingQueue<String> logQueue;
   private final SimpleDateFormat dateFormat;
   private volatile boolean running;
   private Thread writerThread;
   private volatile boolean consoleLoggingEnabled;
   private static final String LOG_FORMAT = "[%s] [TXN] %s | Player=%s | GameID=%s | Amount=%.2f | Currency=%s/%s";
   private static final String ROLLING_LOG_FORMAT = "[%s] [TXN-ROLL] %s | Player1=%s | Player2=%s | Amount=%.2f | Currency=%s/%s";

   public TransactionLogger(KStudio plugin) {
      this.plugin = plugin;
      this.logFile = new File(plugin.getDataFolder(), "transactions.log");
      this.logQueue = new LinkedBlockingQueue<>();
      this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      this.running = true;
      this.consoleLoggingEnabled = plugin.getConfig().getBoolean("transaction-logging.console", false);

      try {
         if (!this.logFile.exists()) {
            this.logFile.getParentFile().mkdirs();
            this.logFile.createNewFile();
         }
      } catch (IOException var3) {
         plugin.getLogger().warning("Failed to create transaction log file: " + var3.getMessage());
      }

      this.startWriterThread();
   }

   private void startWriterThread() {
      this.writerThread = new Thread(() -> {
         while (this.running || !this.logQueue.isEmpty()) {
            try {
               String message = this.logQueue.poll(TimeUnit.SECONDS.toMillis(1L), TimeUnit.MILLISECONDS);
               if (message != null) {
                  this.writeToFile(message);
               }
            } catch (InterruptedException var2) {
               Thread.currentThread().interrupt();
               break;
            }
         }

         String message;
         while ((message = this.logQueue.poll()) != null) {
            this.writeToFile(message);
         }
      }, "CoinFlip-TransactionLogger");
      this.writerThread.setDaemon(true);
      this.writerThread.start();
   }

   private void writeToFile(String message) {
      try {
         PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(this.logFile, true)));

         try {
            writer.println(message);
         } catch (Throwable var6) {
            try {
               writer.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }

            throw var6;
         }

         writer.close();
      } catch (IOException var7) {
         this.plugin.getLogger().log(Level.WARNING, "Failed to write to transaction log", (Throwable) var7);
      }
   }

   public void logRefund(
         String playerName, UUID playerUuid, UUID gameId, double amount, CoinFlipGame.CurrencyType currencyType,
         String currencyId, RefundResult result) {
      String timestamp = this.dateFormat.format(new Date());
      String message = String.format(
            "[%s] [TXN] %s | Player=%s | GameID=%s | Amount=%.2f | Currency=%s/%s",
            timestamp,
            result.getDisplayName(),
            playerName,
            gameId,
            amount,
            currencyType,
            currencyId != null ? currencyId : "default");
      if (this.consoleLoggingEnabled) {
         String consoleMsg = String.format(
               "[TXN] %s | %s | %.2f %s/%s | %s", result.getDisplayName(), playerName, amount, currencyType,
               currencyId != null ? currencyId : "default", gameId);
         if (result.isSuccess()) {
            this.plugin.getLogger().info(consoleMsg);
         } else if (result != RefundResult.ROLLBACK && result != RefundResult.FAILED) {
            this.plugin.getLogger().info(consoleMsg);
         } else {
            this.plugin.getLogger().warning(consoleMsg);
         }
      }

      this.logQueue.offer(message);
   }

   public void logRollingRefund(
         String player1Name,
         UUID player1Uuid,
         String player2Name,
         UUID player2Uuid,
         double amount,
         CoinFlipGame.CurrencyType currencyType,
         String currencyId,
         RefundResult result) {
      String timestamp = this.dateFormat.format(new Date());
      String message = String.format(
            "[%s] [TXN-ROLL] %s | Player1=%s | Player2=%s | Amount=%.2f | Currency=%s/%s",
            timestamp,
            result.getDisplayName(),
            player1Name,
            player2Name,
            amount,
            currencyType,
            currencyId != null ? currencyId : "default");
      if (this.consoleLoggingEnabled) {
         String consoleMsg = String.format(
               "[TXN-ROLL] %s | %s vs %s | %.2f %s/%s",
               result.getDisplayName(),
               player1Name,
               player2Name,
               amount,
               currencyType,
               currencyId != null ? currencyId : "default");
         if (result.isSuccess()) {
            this.plugin.getLogger().info(consoleMsg);
         } else {
            this.plugin.getLogger().warning(consoleMsg);
         }
      }

      this.logQueue.offer(message);
   }

   public void logBlocked(String playerName, RefundResult reason, String details) {
      String timestamp = this.dateFormat.format(new Date());
      String message = String.format("[%s] [TXN-BLOCKED] %s | Player=%s | Details=%s", timestamp,
            reason.getDisplayName(), playerName, details);
      if (this.consoleLoggingEnabled) {
         this.plugin.getLogger()
               .info(String.format("[TXN-BLOCKED] %s | %s | %s", reason.getDisplayName(), playerName, details));
      }

      this.logQueue.offer(message);
   }

   public void logWarning(String message) {
      String timestamp = this.dateFormat.format(new Date());
      String fullMessage = String.format("[%s] [TXN-WARN] %s", timestamp, message);
      if (this.consoleLoggingEnabled) {
         this.plugin.getLogger().warning("[TXN-WARN] " + message);
      }

      this.logQueue.offer(fullMessage);
   }

   public void shutdown() {
      this.running = false;
      if (this.writerThread != null) {
         this.writerThread.interrupt();

         try {
            this.writerThread.join(5000L);
         } catch (InterruptedException var2) {
            Thread.currentThread().interrupt();
         }
      }

      String message;
      while ((message = this.logQueue.poll()) != null) {
         this.writeToFile(message);
      }
   }

   public int getPendingCount() {
      return this.logQueue.size();
   }

   public void reloadConfig() {
      this.consoleLoggingEnabled = this.plugin.getConfig().getBoolean("transaction-logging.console", false);
   }
}
