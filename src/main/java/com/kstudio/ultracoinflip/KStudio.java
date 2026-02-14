package com.kstudio.ultracoinflip;

import co.aikar.commands.PaperCommandManager;
import com.kstudio.ultracoinflip.adventure.AdventureHelper;
import com.kstudio.ultracoinflip.commands.ACFCompletions;
import com.kstudio.ultracoinflip.commands.CoinFlipCommand;
import com.kstudio.ultracoinflip.config.ConfigManager;
import com.kstudio.ultracoinflip.currency.CurrencyManager;
import com.kstudio.ultracoinflip.currency.TaxRateCalculator;
import com.kstudio.ultracoinflip.data.BettingLimitManager;
import com.kstudio.ultracoinflip.data.CoinFlipManager;
import com.kstudio.ultracoinflip.data.PlayerSettingsManager;
import com.kstudio.ultracoinflip.database.DatabaseFactory;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import com.kstudio.ultracoinflip.discord.DiscordWebhookHandler;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.GUIListener;
import com.kstudio.ultracoinflip.gui.GUIManager;
import com.kstudio.ultracoinflip.gui.cache.FilteredGamesCache;
import com.kstudio.ultracoinflip.gui.cache.InventoryUpdateBatcher;
import com.kstudio.ultracoinflip.gui.cache.ItemStackPool;
import com.kstudio.ultracoinflip.gui.cache.PlaceholderMapPool;
import com.kstudio.ultracoinflip.listeners.PlayerListener;
import com.kstudio.ultracoinflip.listeners.VaultEconomyListener;
import com.kstudio.ultracoinflip.placeholders.CoinFlipPlaceholders;
import com.kstudio.ultracoinflip.refund.RefundLimiter;
import com.kstudio.ultracoinflip.refund.TransactionLogger;
import com.kstudio.ultracoinflip.security.ExploitDetector;
import com.kstudio.ultracoinflip.util.AnvilInputManager;
import com.kstudio.ultracoinflip.util.ChatInputManager;
import com.kstudio.ultracoinflip.util.ColorLogger;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.ErrorHandler;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.SoundHelper;
import com.kstudio.ultracoinflip.util.UltraCoinFlipMetrics;
import com.kstudio.ultracoinflip.util.UpdateChecker;
import com.kstudio.ultracoinflip.util.VersionDetector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Generated;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class KStudio extends JavaPlugin {
   private static KStudio instance;
   private ColorLogger colorLogger;
   private DebugManager debugManager;
   private AdventureHelper adventureHelper;
   private GUIHelper guiHelper;
   private InventoryUpdateBatcher inventoryUpdateBatcher;
   private FilteredGamesCache filteredGamesCache;
   private ItemStackPool itemStackPool;
   private PlaceholderMapPool placeholderMapPool;
   private ConfigManager configManager;
   private CurrencyManager currencyManager;
   private TaxRateCalculator taxRateCalculator;
   private GUIManager guiManager;
   private CoinFlipManager coinFlipManager;
   private PlayerSettingsManager playerSettingsManager;
   private BettingLimitManager bettingLimitManager;
   private DatabaseManager databaseManager;
   private RefundLimiter refundLimiter;
   private TransactionLogger transactionLogger;
   private ExploitDetector exploitDetector;
   private Economy economy;
   private boolean placeholderAPI;
   private PaperCommandManager commandManager;
   private DiscordWebhookHandler discordWebhookHandler;
   private SoundHelper soundHelper;
   private ChatInputManager chatInputManager;
   private AnvilInputManager anvilInputManager;
   private UltraCoinFlipMetrics metrics;
   private String pluginVersion;
   private List<String> pendingUpdateMessages = new ArrayList<>();
   private volatile boolean cachedUpdateCheckerEnabled = true;
   private volatile boolean cachedNotifyInGame = true;
   private volatile String cachedNotifyPermission = "ultracoinflip.admin";

   public void onEnable() {
      instance = this;

      try {
         java.lang.reflect.Method getPluginMetaMethod = this.getClass().getMethod("getPluginMeta");
         Object meta = getPluginMetaMethod.invoke(this);
         java.lang.reflect.Method getVersionMethod = meta.getClass().getMethod("getVersion");
         this.pluginVersion = (String) getVersionMethod.invoke(meta);
      } catch (Exception | NoSuchMethodError var9) {
         String version = this.getDescription().getVersion();
         this.pluginVersion = version;
      }

      this.colorLogger = new ColorLogger(this);

      try {
         ErrorHandler.setupGlobalExceptionHandler(this);
      } catch (Exception var8) {
         this.getLogger().warning("Failed to setup global exception handler: " + var8.getMessage());
      }

      try {
         ErrorHandler.checkServerCompatibility(this);
      } catch (Exception var7) {
         this.getLogger().warning("Failed to check server compatibility: " + var7.getMessage());
      }

      VersionDetector.logVersionInfo(this);
      this.printStartupBanner();
      this.adventureHelper = new AdventureHelper(this);
      this.guiHelper = new GUIHelper(this);
      this.inventoryUpdateBatcher = new InventoryUpdateBatcher(this);
      this.filteredGamesCache = new FilteredGamesCache();
      this.itemStackPool = new ItemStackPool();
      this.placeholderMapPool = new PlaceholderMapPool();

      try {
         this.configManager = new ConfigManager(this);
         this.cacheUpdateCheckerConfig();
      } catch (Exception var6) {
         ErrorHandler.handleConfigError(this, var6, "config.yml");

         try {
            this.configManager = new ConfigManager(this);
         } catch (Exception var5) {
            ErrorHandler.handleEnableError(this, var5);
            this.getServer().getPluginManager().disablePlugin(this);
            return;
         }
      }

      this.debugManager = new DebugManager(this);
      this.debugManager.info(DebugManager.Category.GENERAL, "Debug system initialized");
      Plugin placeholderAPIPlugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
      this.placeholderAPI = placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled();
      if (this.placeholderAPI) {
         new CoinFlipPlaceholders(this).register();
      }

      if (!this.setupEconomy()) {
         this.colorLogger.warning("No economy plugin hooked to Vault yet.");
         this.colorLogger.info("     " + this.colorLogger.yellow("Waiting for economy provider to register..."));
         this.getServer().getPluginManager().registerEvents(new VaultEconomyListener(this), this);
         this.currencyManager = null;
      } else {
         this.colorLogger.info("     " + this.colorLogger.brightGreen("Vault economy ready!"));
         this.currencyManager = new CurrencyManager(this);
      }

      this.checkPluginDependencies();
      if (this.currencyManager != null) {
         this.taxRateCalculator = new TaxRateCalculator(this);
      }

      this.refundLimiter = new RefundLimiter();
      this.transactionLogger = new TransactionLogger(this);
      this.exploitDetector = new ExploitDetector(this);

      try {
         this.databaseManager = DatabaseFactory.createDatabaseManager(this);
         this.coinFlipManager = new CoinFlipManager(this, this.databaseManager);
         this.coinFlipManager.initializeDatabase();
      } catch (Exception var4) {
         ErrorHandler.handleDatabaseError(this, var4, "Database Initialization");
         this.colorLogger.error("Failed to initialize database! Plugin will be disabled.");
         this.getServer().getPluginManager().disablePlugin(this);
         return;
      }

      this.playerSettingsManager = new PlayerSettingsManager(this);
      this.bettingLimitManager = new BettingLimitManager(this);
      this.guiManager = new GUIManager();
      this.guiManager.setLogger(this.getLogger());
      this.guiManager.setPlugin(this);
      this.discordWebhookHandler = new DiscordWebhookHandler(this);
      this.soundHelper = new SoundHelper(this);
      this.chatInputManager = new ChatInputManager(this);
      this.anvilInputManager = new AnvilInputManager(this);

      try {
         this.commandManager = new PaperCommandManager(this);
         ACFCompletions.registerCompletions(this, this.commandManager.getCommandCompletions());
         CoinFlipCommand command = new CoinFlipCommand(this);
         this.commandManager.registerCommand(command);
      } catch (Exception var3) {
         this.colorLogger.error("Failed to initialize ACF Command Manager! Plugin will be disabled.");
         var3.printStackTrace();
         this.getServer().getPluginManager().disablePlugin(this);
         return;
      }

      Bukkit.getPluginManager().registerEvents(new GUIListener(this.guiManager), this);
      Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
      if (FoliaScheduler.isFolia()) {
         this.colorLogger.info(
               this.colorLogger.brightPurple("Folia detected!") + this.colorLogger.gray(" Using Folia schedulers."));
      }

      // this.checkForUpdates();
      this.metrics = new UltraCoinFlipMetrics(this);
      this.metrics.initialize();
      this.printCompletionMessage();
   }

   public void onDisable() {
      try {
         if (this.colorLogger != null) {
            boolean isLegacy = VersionDetector.isLegacy();
            String separator = isLegacy
                  ? "========================================================================"
                  : "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
            this.colorLogger.warning(separator);
            this.colorLogger
                  .warning("  " + this.colorLogger.bold("CoinFlip v" + this.pluginVersion) + " "
                        + this.colorLogger.brightYellow("is shutting down..."));
            this.colorLogger.warning(separator);
         }

         if (this.refundLimiter != null) {
            this.refundLimiter.acquireGlobalLock();
         }

         if (this.coinFlipManager != null) {
            try {
               this.coinFlipManager.refundAllGames();
               this.coinFlipManager.closeDatabase();
            } catch (Exception var19) {
               ErrorHandler.handleDisableError(this, var19);
            }
         }

         if (this.transactionLogger != null) {
            try {
               this.transactionLogger.shutdown();
            } catch (Exception var17) {
               ErrorHandler.handleDisableError(this, var17);
            }
         }

         if (this.exploitDetector != null) {
            try {
               this.exploitDetector.shutdown();
            } catch (Exception var16) {
               ErrorHandler.handleDisableError(this, var16);
            }
         }

         if (this.refundLimiter != null) {
            this.refundLimiter.reset();
         }

         if (this.debugManager != null) {
            try {
               this.debugManager.shutdown();
            } catch (Exception var15) {
               ErrorHandler.handleDisableError(this, var15);
            }
         }
      } catch (Exception var21) {
         ErrorHandler.handleDisableError(this, var21);
      } finally {
         if (this.colorLogger == null) {
            this.getLogger().info("CoinFlip has been disabled!");
         }
      }
   }

   private void printStartupBanner() {
      this.colorLogger.info(this.colorLogger.brightYellow(""));
      this.colorLogger.info(this.colorLogger.brightYellow("  ᴄᴏɪɴꜰʟɪᴘ " + this.pluginVersion));
      this.colorLogger.info(this.colorLogger.brightYellow(""));
   }

   public void cacheUpdateCheckerConfig() {
      this.cachedUpdateCheckerEnabled = this.configManager.getConfig().getBoolean("update-checker.enabled", true);
      this.cachedNotifyInGame = this.configManager.getConfig().getBoolean("update-checker.notify-in-game", true);
      this.cachedNotifyPermission = this.configManager.getConfig().getString("update-checker.notify-permission",
            "ultracoinflip.admin");
   }

   private void checkForUpdates() {
      if (this.cachedUpdateCheckerEnabled) {
         new UpdateChecker(this, 130124)
               .getVersion(
                     version -> {
                        String currentVersion = this.pluginVersion;
                        boolean notifyInGame = this.cachedNotifyInGame;
                        String notifyPermission = this.cachedNotifyPermission;
                        if (currentVersion.equals(version)) {
                           String latestMsg = this.configManager.getMessage("update.latest-version")
                                 .replace("<version>", version);
                           String consoleMsg = this.colorLogger.translateConsoleColors(latestMsg);
                           this.colorLogger.info("  " + consoleMsg);
                           synchronized (this.pendingUpdateMessages) {
                              this.pendingUpdateMessages.clear();
                           }
                        } else {
                           String newVersionMsg = this.configManager.getMessage("update.new-version-available");
                           String currentVersionMsg = this.configManager.getMessage("update.current-version")
                                 .replace("<version>", currentVersion);
                           String latestVersionMsg = this.configManager.getMessage("update.latest-version-label")
                                 .replace("<version>", version);
                           String downloadMsg = this.configManager.getMessage("update.download-link");
                           this.colorLogger.warning("  " + ColorLogger.stripColorCodes(newVersionMsg));
                           this.colorLogger.warning("     " + ColorLogger.stripColorCodes(currentVersionMsg));
                           this.colorLogger.warning("     " + ColorLogger.stripColorCodes(latestVersionMsg));
                           this.colorLogger.warning("     " + ColorLogger.stripColorCodes(downloadMsg));
                           if (notifyInGame) {
                              String prefix = this.configManager.getMessage("prefix");
                              String message = this.configManager
                                    .getMessage("update.in-game-new-version")
                                    .replace("<current>", currentVersion)
                                    .replace("<latest>", version);
                              String inGameNewVersionMsg = prefix + "<reset> " + message;
                              synchronized (this.pendingUpdateMessages) {
                                 this.pendingUpdateMessages.clear();
                                 this.pendingUpdateMessages.add(inGameNewVersionMsg);
                              }

                              FoliaScheduler.runTask(this,
                                    () -> this.sendUpdateMessageToPlayers(inGameNewVersionMsg, notifyPermission));
                           }
                        }
                     });
      }
   }

   private void sendUpdateMessageToPlayers(String message, String permission) {
      if (this.adventureHelper == null) {
         this.getLogger().warning("Cannot send update messages: AdventureHelper is not initialized");
      } else {
         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline() && (player.isOp() || player.hasPermission(permission))) {
               this.adventureHelper.sendMessage(player, message);
            }
         }
      }
   }

   public void sendPendingUpdateMessages(Player player) {
      if (player != null && player.isOnline()) {
         boolean notifyInGame = this.cachedNotifyInGame;
         if (notifyInGame) {
            String notifyPermission = this.cachedNotifyPermission;
            if (player.isOp() || player.hasPermission(notifyPermission)) {
               synchronized (this.pendingUpdateMessages) {
                  for (String message : this.pendingUpdateMessages) {
                     this.adventureHelper.sendMessage(player, message);
                  }
               }
            }
         }
      }
   }

   private void printCompletionMessage() {
      String version = this.pluginVersion;
      boolean isLegacy = VersionDetector.isLegacy();
      String separator = isLegacy
            ? "========================================================================"
            : "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
      String chatIcon = isLegacy ? "[!]" : "\ud83d\udcac";
      String starIcon = isLegacy ? "[*]" : "⭐";
      this.colorLogger.success(separator);
      this.colorLogger.success("  " + this.colorLogger.bold("CoinFlip v" + version) + " "
            + this.colorLogger.brightGreen("enabled successfully!"));
      this.colorLogger.success(separator);
      this.colorLogger
            .info("  " + this.colorLogger.white("Developed by") + " " + this.colorLogger.brightPurple("KStudio"));
      this.colorLogger.info("  " + this.colorLogger.white("Support:") + "     "
            + this.colorLogger.brightCyan("https://discord.gg/GGDxDnpnDP"));
      this.colorLogger.info("");
      this.colorLogger
            .info(
                  "  "
                        + this.colorLogger.brightYellow(chatIcon)
                        + "  "
                        + this.colorLogger
                              .white("Found a bug or have suggestions? Join our Discord for quick support!"));
      this.colorLogger
            .info(
                  "  "
                        + this.colorLogger.brightYellow(starIcon)
                        + "  "
                        + this.colorLogger.white("Enjoying the plugin? Please leave a 5-star review to help us grow!"));
   }

   private void checkPluginDependencies() {
      boolean isLegacy = VersionDetector.isLegacy();
      String arrow = isLegacy ? "->" : "→";
      this.colorLogger
            .info("  " + this.colorLogger.brightCyan(arrow) + " Vault:          " + this.getPluginStatus("Vault"));
      this.colorLogger.info(
            "  " + this.colorLogger.brightCyan(arrow) + " PlaceholderAPI: " + this.getPluginStatus("PlaceholderAPI"));
      this.colorLogger.info(
            "  " + this.colorLogger.brightCyan(arrow) + " PlayerPoints:   " + this.getPluginStatus("PlayerPoints"));
      this.colorLogger.info(
            "  " + this.colorLogger.brightCyan(arrow) + " TokenManager:   " + this.getPluginStatus("TokenManager"));
      this.colorLogger.info(
            "  " + this.colorLogger.brightCyan(arrow) + " CoinsEngine:    " + this.getPluginStatus("CoinsEngine"));
      this.colorLogger.info(
            "  " + this.colorLogger.brightCyan(arrow) + " BeastTokens:    " + this.getPluginStatus("BeastTokens"));
   }

   private String getPluginStatus(String pluginName) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
      if (plugin != null) {
         String version;
         if (plugin instanceof JavaPlugin) {
            try {
               java.lang.reflect.Method getPluginMetaMethod = plugin.getClass().getMethod("getPluginMeta");
               Object meta = getPluginMetaMethod.invoke(plugin);
               java.lang.reflect.Method getVersionMethod = meta.getClass().getMethod("getVersion");
               version = (String) getVersionMethod.invoke(meta);
            } catch (Exception | NoSuchMethodError var5) {
               version = plugin.getDescription().getVersion();
            }
         } else {
            version = plugin.getDescription().getVersion();
         }

         return plugin.isEnabled()
               ? this.colorLogger.brightGreen("Found ") + this.colorLogger.gray("(v" + version + ")")
               : this.colorLogger.brightYellow("Disabled ") + this.colorLogger.gray("(v" + version + ")");
      } else {
         return this.colorLogger.gray("Not installed");
      }
   }

   private boolean setupEconomy() {
      if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
         this.colorLogger.error("Vault plugin is not installed!");
         return false;
      } else {
         RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
         if (rsp == null) {
            return false;
         } else {
            this.economy = (Economy) rsp.getProvider();
            if (this.economy == null) {
               this.colorLogger.error("Failed to get economy provider from Vault!");
               return false;
            } else {
               return true;
            }
         }
      }
   }

   public String getMessage(String key) {
      return this.configManager.getMessage(key);
   }

   public String getGUIMessage(String key) {
      return this.configManager.getGUIConfigString(key);
   }

   public FileConfiguration getGUIConfig() {
      return this.configManager.getGUIConfig();
   }

   public FileConfiguration getSoundsConfig() {
      return this.configManager.getSoundsConfig();
   }

   public FileConfiguration getConfig() {
      return this.configManager.getConfig();
   }

   public ChatInputManager getChatInputManager() {
      return this.chatInputManager;
   }

   public AnvilInputManager getAnvilInputManager() {
      return this.anvilInputManager;
   }

   public void initializeEconomySystem() {
      if (this.currencyManager == null) {
         if (this.setupEconomy()) {
            this.colorLogger
                  .info("     " + this.colorLogger.brightGreen("Vault economy ready (from ServiceRegisterEvent)!"));
            this.currencyManager = new CurrencyManager(this);
            this.taxRateCalculator = new TaxRateCalculator(this);
            this.colorLogger.info("     " + this.colorLogger.brightGreen("Currency system initialized successfully!"));
         }
      }
   }

   public boolean isPlaceholderAPI() {
      if (!this.placeholderAPI) {
         return false;
      } else {
         Plugin plugin = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
         return plugin != null && plugin.isEnabled();
      }
   }

   @Generated
   public ColorLogger getColorLogger() {
      return this.colorLogger;
   }

   @Generated
   public DebugManager getDebugManager() {
      return this.debugManager;
   }

   @Generated
   public AdventureHelper getAdventureHelper() {
      return this.adventureHelper;
   }

   @Generated
   public GUIHelper getGuiHelper() {
      return this.guiHelper;
   }

   @Generated
   public InventoryUpdateBatcher getInventoryUpdateBatcher() {
      return this.inventoryUpdateBatcher;
   }

   @Generated
   public FilteredGamesCache getFilteredGamesCache() {
      return this.filteredGamesCache;
   }

   @Generated
   public ItemStackPool getItemStackPool() {
      return this.itemStackPool;
   }

   @Generated
   public PlaceholderMapPool getPlaceholderMapPool() {
      return this.placeholderMapPool;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   @Generated
   public CurrencyManager getCurrencyManager() {
      return this.currencyManager;
   }

   @Generated
   public TaxRateCalculator getTaxRateCalculator() {
      return this.taxRateCalculator;
   }

   @Generated
   public GUIManager getGuiManager() {
      return this.guiManager;
   }

   @Generated
   public CoinFlipManager getCoinFlipManager() {
      return this.coinFlipManager;
   }

   @Generated
   public PlayerSettingsManager getPlayerSettingsManager() {
      return this.playerSettingsManager;
   }

   @Generated
   public BettingLimitManager getBettingLimitManager() {
      return this.bettingLimitManager;
   }

   @Generated
   public DatabaseManager getDatabaseManager() {
      return this.databaseManager;
   }

   @Generated
   public RefundLimiter getRefundLimiter() {
      return this.refundLimiter;
   }

   @Generated
   public TransactionLogger getTransactionLogger() {
      return this.transactionLogger;
   }

   @Generated
   public ExploitDetector getExploitDetector() {
      return this.exploitDetector;
   }

   @Generated
   public Economy getEconomy() {
      return this.economy;
   }

   @Generated
   public PaperCommandManager getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public DiscordWebhookHandler getDiscordWebhookHandler() {
      return this.discordWebhookHandler;
   }

   @Generated
   public SoundHelper getSoundHelper() {
      return this.soundHelper;
   }

   @Generated
   public UltraCoinFlipMetrics getMetrics() {
      return this.metrics;
   }

   @Generated
   public String getPluginVersion() {
      return this.pluginVersion;
   }

   @Generated
   public List<String> getPendingUpdateMessages() {
      return this.pendingUpdateMessages;
   }

   @Generated
   public boolean isCachedUpdateCheckerEnabled() {
      return this.cachedUpdateCheckerEnabled;
   }

   @Generated
   public boolean isCachedNotifyInGame() {
      return this.cachedNotifyInGame;
   }

   @Generated
   public String getCachedNotifyPermission() {
      return this.cachedNotifyPermission;
   }

   @Generated
   public static KStudio getInstance() {
      return instance;
   }
}
