package com.kstudio.ultracoinflip.currency;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.config.ConfigUpdater;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.security.ExploitDetector;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CurrencyManager {
   private final KStudio plugin;
   private final ConfigUpdater configUpdater;
   private final Map<CoinFlipGame.CurrencyType, CurrencyHandler> handlers = new HashMap<>();
   private final Map<CoinFlipGame.CurrencyType, Boolean> enabledCurrencies = new HashMap<>();
   private final Map<String, CoinsEngineCurrencyHandler> coinsEngineHandlers = new HashMap<>();
   private final Map<String, Boolean> enabledCoinsEngineCurrencies = new HashMap<>();
   private final Map<String, PlaceholderCurrencyHandler> placeholderHandlers = new HashMap<>();
   private final Map<String, Boolean> enabledPlaceholderCurrencies = new HashMap<>();
   private final Map<CoinFlipGame.CurrencyType, CurrencySettings> currencySettings = new HashMap<>();
   private final Map<String, CurrencySettings> coinsEngineCurrencySettings = new HashMap<>();
   private final Map<String, CurrencySettings> placeholderCurrencySettings = new HashMap<>();
   private final Map<String, CurrencyManager.CurrencyInfo> syntaxCommandMap = new HashMap<>();
   private List<String> currencyErrors = new ArrayList<>();

   public CurrencyManager(KStudio plugin) {
      this.plugin = plugin;
      this.configUpdater = new ConfigUpdater(plugin);
      if (plugin.getDebugManager() != null) {
         plugin.getDebugManager().startPerformanceTracking("currency_load");
      }

      this.loadCurrencies();
      this.printCurrencyErrors();
      if (plugin.getDebugManager() != null) {
         plugin.getDebugManager().endPerformanceTracking("currency_load");
         plugin.getDebugManager()
            .info(
               DebugManager.Category.CURRENCY,
               String.format("Loaded %d currency handlers", this.handlers.size() + this.coinsEngineHandlers.size() + this.placeholderHandlers.size())
            );
      }
   }

   public void reload() {
      this.plugin.getLogger().info("Reloading currency configurations...");
      this.currencyErrors.clear();
      this.handlers.clear();
      this.enabledCurrencies.clear();
      this.coinsEngineHandlers.clear();
      this.enabledCoinsEngineCurrencies.clear();
      this.placeholderHandlers.clear();
      this.enabledPlaceholderCurrencies.clear();
      this.currencySettings.clear();
      this.coinsEngineCurrencySettings.clear();
      this.placeholderCurrencySettings.clear();
      this.syntaxCommandMap.clear();
      this.loadCurrencies();
      this.printCurrencyErrors();
   }

   private FileConfiguration loadCurrencyConfig(String fileName) {
      File currenciesFolder = new File(this.plugin.getDataFolder(), "currencies");
      if (!currenciesFolder.exists()) {
         currenciesFolder.mkdirs();
      }

      File currencyFile = new File(currenciesFolder, fileName);
      String resourcePath = "currencies/" + fileName;

      try {
         this.configUpdater.updateConfig(currencyFile, resourcePath, "config-version", true);
      } catch (Exception var25) {
         if (this.plugin.getConfigManager().isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] ConfigUpdater failed for " + fileName + ": " + var25.getMessage());
         }
      }

      boolean fileWasCreated = false;
      if (!currencyFile.exists()) {
         try {
            this.plugin.saveResource(resourcePath, false);
            fileWasCreated = true;
            this.plugin.getLogger().info("Created default currency config: " + fileName);
         } catch (Exception var23) {
            this.plugin.getLogger().warning("Could not save default currency config " + fileName + " from resources: " + var23.getMessage());
         }
      }

      FileConfiguration config = YamlConfiguration.loadConfiguration(currencyFile);
      FileConfiguration defConfig = null;
      InputStream defConfigStream = null;

      try {
         defConfigStream = this.plugin.getResource(resourcePath);
         if (defConfigStream != null) {
            defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            config.setDefaults(defConfig);
         } else {
            this.plugin.getLogger().warning("Default resource not found for currency config: " + resourcePath + ". File may be missing from plugin JAR.");
         }
      } catch (Exception var22) {
         this.plugin.getLogger().warning("Error loading defaults for currency config " + fileName + ": " + var22.getMessage());
      } finally {
         if (defConfigStream != null) {
            try {
               defConfigStream.close();
            } catch (Exception var20) {
            }
         }
      }

      if (fileWasCreated && defConfig != null) {
         boolean needsSave = false;

         for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
               config.set(key, defConfig.get(key));
               needsSave = true;
            }
         }

         if (needsSave) {
            try {
               config.save(currencyFile);
               this.plugin.getLogger().info("Initialized currency config " + fileName + " with all default values");
            } catch (Exception var21) {
               this.plugin.getLogger().warning("Failed to save initialized currency config " + fileName + ": " + var21.getMessage());
            }
         }
      }

      return config;
   }

   private CurrencySettings loadCurrencySettings(FileConfiguration config, String prefix, String currencyIdentifier) {
      String basePath = prefix != null && !prefix.isEmpty() ? prefix + "." : "";
      boolean broadcastEnabled = config.getBoolean(basePath + "broadcast-enabled", true);
      double minBroadcastAmount = config.getDouble(basePath + "min-broadcast-amount", 100.0);
      double minBid = config.getDouble(basePath + "min-bid", 1.0);
      double maxBid = config.getDouble(basePath + "max-bid", -1.0);
      double minReserveBalance = config.getDouble(basePath + "min-reserve-balance", 0.0);
      boolean taxEnabled = config.getBoolean(basePath + "tax-enabled", true);
      double taxRate = config.getDouble(basePath + "tax-rate", 0.1);
      if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
         String taxEnabledPath = basePath + "tax-enabled";
         Object rawValue = config.get(taxEnabledPath);
         this.plugin
            .getLogger()
            .info(
               "Currency "
                  + currencyIdentifier
                  + ": tax-enabled loaded as "
                  + taxEnabled
                  + " (raw config value: "
                  + rawValue
                  + ", type: "
                  + (rawValue != null ? rawValue.getClass().getSimpleName() : "null")
                  + ")"
            );
      }

      String syntaxCommandPath = basePath + "syntax-command";
      String syntaxCommand = config.getString(syntaxCommandPath);
      if (syntaxCommand == null || syntaxCommand.trim().isEmpty()) {
         Configuration defaults = config.getDefaults();
         if (defaults != null) {
            String defaultValue = defaults.getString(syntaxCommandPath);
            if (defaultValue != null && !defaultValue.trim().isEmpty()) {
               syntaxCommand = defaultValue;
               config.set(syntaxCommandPath, defaultValue);
            }
         }
      }

      String errorPrefix = currencyIdentifier != null ? currencyIdentifier + ": " : "";
      if (minBid < 0.0) {
         this.currencyErrors.add(errorPrefix + "min-bid must be >= 0 in currency config! Using default: 1.0");
         minBid = 1.0;
      }

      if (maxBid != -1.0 && maxBid < minBid) {
         this.currencyErrors.add(errorPrefix + "max-bid (" + maxBid + ") must be >= min-bid (" + minBid + ") in currency config! Ignoring max-bid.");
         maxBid = -1.0;
      }

      if (taxRate < 0.0 || taxRate > 1.0) {
         this.currencyErrors.add(errorPrefix + "tax-rate must be between 0.0 and 1.0 in currency config! Using default: 0.1");
         taxRate = 0.1;
      }

      if (minBroadcastAmount < 0.0) {
         this.currencyErrors.add(errorPrefix + "min-broadcast-amount must be >= 0 in currency config! Using default: 100.0");
         minBroadcastAmount = 100.0;
      }

      if (minReserveBalance < 0.0) {
         this.currencyErrors.add(errorPrefix + "min-reserve-balance must be >= 0 in currency config! Using default: 0.0");
         minReserveBalance = 0.0;
      }

      if (syntaxCommand != null && !syntaxCommand.trim().isEmpty()) {
         syntaxCommand = syntaxCommand.trim().toLowerCase();
         if (syntaxCommand.contains(" ") || syntaxCommand.contains(":") || syntaxCommand.contains(".")) {
            this.currencyErrors
               .add(
                  errorPrefix
                     + "syntax-command '"
                     + syntaxCommand
                     + "' contains invalid characters (spaces, colons, or dots)! Using as-is, but this may cause issues."
               );
         }
      } else {
         this.currencyErrors
            .add(
               errorPrefix
                  + "syntax-command is missing or empty in currency config! This currency will not appear in command completion. Please set a syntax-command (e.g., 'money', 'point', 'token')."
            );
         syntaxCommand = "";
      }

      CurrencySettings settings = new CurrencySettings(broadcastEnabled, minBroadcastAmount, minBid, maxBid, taxEnabled, taxRate, syntaxCommand);
      settings.setMinReserveBalance(minReserveBalance);
      boolean dynamicTaxEnabled = config.getBoolean(basePath + "dynamic-tax-enabled", false);
      if (dynamicTaxEnabled) {
         String taxRateConfigPath = basePath + "tax-rate-config";
         if (config.contains(taxRateConfigPath)) {
            TaxRateConfig taxRateConfig = this.loadTaxRateConfig(config, taxRateConfigPath, errorPrefix);
            if (taxRateConfig != null) {
               settings.setTaxRateConfig(taxRateConfig);
               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  this.plugin
                     .getLogger()
                     .info(
                        "Dynamic tax rate system enabled for "
                           + currencyIdentifier
                           + ". Base tax rate: "
                           + taxRateConfig.getBaseTaxRate()
                           + ", Tiers: "
                           + (taxRateConfig.getTiers() != null ? taxRateConfig.getTiers().size() : 0)
                     );
               }
            } else {
               this.currencyErrors.add(errorPrefix + "Failed to load tax-rate-config! Dynamic tax rate will be disabled. Check console for errors.");
            }
         } else {
            this.currencyErrors.add(errorPrefix + "dynamic-tax-enabled is true but tax-rate-config is missing! Dynamic tax rate will be disabled.");
         }
      }

      CurrencyRestrictions restrictions = this.loadCurrencyRestrictions(config, basePath, errorPrefix);
      settings.setRestrictions(restrictions);
      return settings;
   }

   private CurrencyRestrictions loadCurrencyRestrictions(FileConfiguration config, String basePath, String errorPrefix) {
      String restrictionsPath = basePath + "restrictions";
      if (!config.contains(restrictionsPath)) {
         return new CurrencyRestrictions();
      } else {
         boolean restrictionsEnabled = config.getBoolean(restrictionsPath + ".enabled", false);
         if (!restrictionsEnabled) {
            return new CurrencyRestrictions();
         } else {
            List<String> allowedWorlds = new ArrayList<>();
            if (config.contains(restrictionsPath + ".allowed-worlds")) {
               List<String> rawAllowedWorlds = config.getStringList(restrictionsPath + ".allowed-worlds");
               if (rawAllowedWorlds != null) {
                  allowedWorlds = rawAllowedWorlds.stream().filter(s -> s != null && !s.trim().isEmpty()).map(String::trim).collect(Collectors.toList());
               }
            }

            List<String> blockedWorlds = new ArrayList<>();
            if (config.contains(restrictionsPath + ".blocked-worlds")) {
               List<String> rawBlockedWorlds = config.getStringList(restrictionsPath + ".blocked-worlds");
               if (rawBlockedWorlds != null) {
                  blockedWorlds = rawBlockedWorlds.stream().filter(s -> s != null && !s.trim().isEmpty()).map(String::trim).collect(Collectors.toList());
               }
            }

            List<String> requiredPermissions = new ArrayList<>();
            if (config.contains(restrictionsPath + ".required-permissions")) {
               List<String> rawRequiredPermissions = config.getStringList(restrictionsPath + ".required-permissions");
               if (rawRequiredPermissions != null) {
                  requiredPermissions = rawRequiredPermissions.stream()
                     .filter(s -> s != null && !s.trim().isEmpty())
                     .map(String::trim)
                     .collect(Collectors.toList());
               }
            }

            if (!allowedWorlds.isEmpty() && !blockedWorlds.isEmpty()) {
               for (String blockedWorld : blockedWorlds) {
                  if (allowedWorlds.contains(blockedWorld)) {
                     this.plugin
                        .getLogger()
                        .warning(
                           errorPrefix
                              + "World '"
                              + blockedWorld
                              + "' is in both allowed-worlds and blocked-worlds. Blocked takes priority (world will be blocked)."
                        );
                     if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                        this.plugin.getLogger().info(errorPrefix + "This is intentional behavior - blocked-worlds always takes priority over allowed-worlds.");
                     }
                  }
               }
            }

            CurrencyRestrictions restrictions = new CurrencyRestrictions(allowedWorlds, blockedWorlds, requiredPermissions, restrictionsEnabled);
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin
                  .getLogger()
                  .info(
                     errorPrefix
                        + "Currency restrictions loaded: enabled="
                        + restrictionsEnabled
                        + ", allowed-worlds="
                        + allowedWorlds.size()
                        + ", blocked-worlds="
                        + blockedWorlds.size()
                        + ", required-permissions="
                        + requiredPermissions.size()
                  );
            }

            return restrictions;
         }
      }
   }

   private TaxRateConfig loadTaxRateConfig(FileConfiguration config, String basePath, String errorPrefix) {
      try {
         TaxRateConfig taxRateConfig = new TaxRateConfig();
         double baseTaxRate = config.getDouble(basePath + ".base-tax-rate", 0.1);
         double originalBaseTaxRate = baseTaxRate;
         if (baseTaxRate > 1.0 && baseTaxRate <= 100.0) {
            baseTaxRate /= 100.0;
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin
                  .getLogger()
                  .warning(
                     errorPrefix
                        + "tax-rate-config.base-tax-rate was "
                        + originalBaseTaxRate
                        + " (percentage format). Converted to "
                        + baseTaxRate
                        + " (decimal format). Please use decimal format (0.0-1.0) in future."
                  );
            }
         } else if (baseTaxRate < 0.0 || baseTaxRate > 1.0) {
            this.plugin
               .getLogger()
               .warning(
                  errorPrefix
                     + "tax-rate-config.base-tax-rate must be between 0.0 and 1.0 (or 0-100 as percentage)! Found: "
                     + baseTaxRate
                     + ". Using default: 0.1"
               );
            baseTaxRate = 0.1;
         }

         baseTaxRate = Math.max(0.0, Math.min(1.0, baseTaxRate));
         taxRateConfig.setBaseTaxRate(baseTaxRate);
         if (config.contains(basePath + ".tiers")) {
            List<?> tiersList = config.getList(basePath + ".tiers");
            if (tiersList != null) {
               for (Object tierObj : tiersList) {
                  if (tierObj instanceof Map) {
                     Map<String, Object> tierMap = (Map<String, Object>)tierObj;
                     double minAmount = ((Number)tierMap.getOrDefault("min-amount", 0)).doubleValue();
                     double maxAmount = tierMap.containsKey("max-amount") && tierMap.get("max-amount") != null
                        ? ((Number)tierMap.get("max-amount")).doubleValue()
                        : -1.0;
                     double tierTaxRate = ((Number)tierMap.getOrDefault("tax-rate", 0.1)).doubleValue();
                     double originalTierTaxRate = tierTaxRate;
                     if (tierTaxRate > 1.0 && tierTaxRate <= 100.0) {
                        tierTaxRate /= 100.0;
                        if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                           this.plugin
                              .getLogger()
                              .warning(
                                 errorPrefix
                                    + "tax-rate-config.tiers: tax-rate was "
                                    + originalTierTaxRate
                                    + " (percentage format). Converted to "
                                    + tierTaxRate
                                    + " (decimal format). Please use decimal format (0.0-1.0) in future."
                              );
                        }
                     } else if (tierTaxRate < 0.0 || tierTaxRate > 1.0) {
                        this.plugin
                           .getLogger()
                           .warning(
                              errorPrefix
                                 + "tax-rate-config.tiers: tax-rate must be between 0.0 and 1.0 (or 0-100 as percentage)! Found: "
                                 + tierTaxRate
                                 + ". Skipping tier."
                           );
                        continue;
                     }

                     tierTaxRate = Math.max(0.0, Math.min(1.0, tierTaxRate));
                     if (maxAmount != -1.0 && maxAmount <= minAmount) {
                        this.currencyErrors
                           .add(
                              errorPrefix
                                 + "tax-rate-config.tiers: max-amount ("
                                 + maxAmount
                                 + ") must be greater than min-amount ("
                                 + minAmount
                                 + ")! Skipping tier."
                           );
                     } else if (minAmount < 0.0) {
                        this.currencyErrors.add(errorPrefix + "tax-rate-config.tiers: min-amount must be >= 0! Skipping tier.");
                     } else {
                        taxRateConfig.getTiers().add(new TaxRateConfig.Tier(minAmount, maxAmount, tierTaxRate));
                     }
                  }
               }
            }
         }

         return taxRateConfig;
      } catch (Exception var21) {
         this.currencyErrors.add(errorPrefix + "Error loading tax-rate-config: " + var21.getMessage() + ". Using default tax rate.");
         this.plugin.getLogger().warning("Error loading tax-rate-config: " + var21.getMessage());
         var21.printStackTrace();
         return null;
      }
   }

   private void loadCurrencies() {
      FileConfiguration vaultConfig = this.loadCurrencyConfig("vault.yml");
      boolean vaultEnabled = vaultConfig.getBoolean("enabled", true);
      if (vaultEnabled) {
         String unit = vaultConfig.getString("unit", "$");
         String displayName = vaultConfig.getString("display-name", "Money");
         if (unit == null || unit.isEmpty()) {
            unit = vaultConfig.getString("symbol", "$");
            if (unit != null && !unit.isEmpty()) {
               this.currencyErrors.add("Vault currency: 'symbol' is deprecated, please use 'unit' instead!");
            } else {
               this.currencyErrors.add("Vault currency: unit is missing or empty! Using default: '$'");
               unit = "$";
            }
         }

         if (displayName == null || displayName.isEmpty()) {
            this.currencyErrors.add("Vault currency: display-name is missing or empty! Using default: 'Money'");
            displayName = "Money";
         }

         VaultCurrencyHandler vaultHandler = new VaultCurrencyHandler(this.plugin, unit, displayName);
         if (vaultHandler.isAvailable()) {
            this.handlers.put(CoinFlipGame.CurrencyType.MONEY, vaultHandler);
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.MONEY, true);
            CurrencySettings settings = this.loadCurrencySettings(vaultConfig, "", "Vault currency");
            this.currencySettings.put(CoinFlipGame.CurrencyType.MONEY, settings);
            if (settings.getSyntaxCommand() != null && !settings.getSyntaxCommand().isEmpty()) {
               this.syntaxCommandMap.put(settings.getSyntaxCommand(), new CurrencyManager.CurrencyInfo(CoinFlipGame.CurrencyType.MONEY, null));
            }

            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin.getLogger().info("Vault currency enabled with unit: " + unit);
            }
         } else {
            this.currencyErrors
               .add(
                  "Vault currency is enabled in config but Vault plugin is not installed or no economy plugin is hooked! Please install Vault and an economy plugin (e.g., EssentialsX, CMI)."
               );
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.MONEY, false);
         }
      } else {
         this.enabledCurrencies.put(CoinFlipGame.CurrencyType.MONEY, false);
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
            this.plugin.getLogger().info("Vault currency is disabled in config.");
         }
      }

      FileConfiguration playerPointsConfig = this.loadCurrencyConfig("playerpoints.yml");
      boolean playerPointsEnabled = playerPointsConfig.getBoolean("enabled", false);
      if (playerPointsEnabled) {
         String unitx = playerPointsConfig.getString("unit", "Points");
         String displayNamex = playerPointsConfig.getString("display-name", "PlayerPoints");
         if (unitx == null || unitx.isEmpty()) {
            unitx = playerPointsConfig.getString("symbol", "Points");
            if (unitx != null && !unitx.isEmpty()) {
               this.currencyErrors.add("PlayerPoints currency: 'symbol' is deprecated, please use 'unit' instead!");
            } else {
               this.currencyErrors.add("PlayerPoints currency: unit is missing or empty! Using default: 'Points'");
               unitx = "Points";
            }
         }

         if (displayNamex == null || displayNamex.isEmpty()) {
            this.currencyErrors.add("PlayerPoints currency: display-name is missing or empty! Using default: 'PlayerPoints'");
            displayNamex = "PlayerPoints";
         }

         PlayerPointsCurrencyHandler ppHandler = new PlayerPointsCurrencyHandler(this.plugin, unitx, displayNamex);
         if (ppHandler.isAvailable()) {
            this.handlers.put(CoinFlipGame.CurrencyType.PLAYERPOINTS, ppHandler);
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.PLAYERPOINTS, true);
            CurrencySettings settingsx = this.loadCurrencySettings(playerPointsConfig, "", "PlayerPoints currency");
            this.currencySettings.put(CoinFlipGame.CurrencyType.PLAYERPOINTS, settingsx);
            if (settingsx.getSyntaxCommand() != null && !settingsx.getSyntaxCommand().isEmpty()) {
               this.syntaxCommandMap.put(settingsx.getSyntaxCommand(), new CurrencyManager.CurrencyInfo(CoinFlipGame.CurrencyType.PLAYERPOINTS, null));
            }

            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin.getLogger().info("PlayerPoints currency enabled with unit: " + unitx);
            }
         } else {
            this.currencyErrors.add("PlayerPoints currency is enabled in config but PlayerPoints plugin is not installed! Please install PlayerPoints plugin.");
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.PLAYERPOINTS, false);
         }
      } else {
         this.enabledCurrencies.put(CoinFlipGame.CurrencyType.PLAYERPOINTS, false);
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
            this.plugin.getLogger().info("PlayerPoints currency is disabled in config.");
         }
      }

      FileConfiguration tokenManagerConfig = this.loadCurrencyConfig("tokenmanager.yml");
      boolean tokenManagerEnabled = tokenManagerConfig.getBoolean("enabled", false);
      if (tokenManagerEnabled) {
         String unitxx = tokenManagerConfig.getString("unit", "Tokens");
         String displayNamexx = tokenManagerConfig.getString("display-name", "Tokens");
         if (unitxx == null || unitxx.isEmpty()) {
            unitxx = tokenManagerConfig.getString("symbol", "Tokens");
            if (unitxx != null && !unitxx.isEmpty()) {
               this.currencyErrors.add("TokenManager currency: 'symbol' is deprecated, please use 'unit' instead!");
            } else {
               this.currencyErrors.add("TokenManager currency: unit is missing or empty! Using default: 'Tokens'");
               unitxx = "Tokens";
            }
         }

         if (displayNamexx == null || displayNamexx.isEmpty()) {
            this.currencyErrors.add("TokenManager currency: display-name is missing or empty! Using default: 'Tokens'");
            displayNamexx = "Tokens";
         }

         TokenManagerCurrencyHandler tokenManagerHandler = new TokenManagerCurrencyHandler(this.plugin, unitxx, displayNamexx);
         if (tokenManagerHandler.isAvailable()) {
            this.handlers.put(CoinFlipGame.CurrencyType.TOKENMANAGER, tokenManagerHandler);
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.TOKENMANAGER, true);
            CurrencySettings settingsxx = this.loadCurrencySettings(tokenManagerConfig, "", "TokenManager currency");
            this.currencySettings.put(CoinFlipGame.CurrencyType.TOKENMANAGER, settingsxx);
            if (settingsxx.getSyntaxCommand() != null && !settingsxx.getSyntaxCommand().isEmpty()) {
               this.syntaxCommandMap.put(settingsxx.getSyntaxCommand(), new CurrencyManager.CurrencyInfo(CoinFlipGame.CurrencyType.TOKENMANAGER, null));
            }

            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin.getLogger().info("TokenManager currency enabled with unit: " + unitxx);
            }
         } else {
            this.currencyErrors.add("TokenManager currency is enabled in config but TokenManager plugin is not installed! Please install TokenManager plugin.");
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.TOKENMANAGER, false);
         }
      } else {
         this.enabledCurrencies.put(CoinFlipGame.CurrencyType.TOKENMANAGER, false);
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
            this.plugin.getLogger().info("TokenManager currency is disabled in config.");
         }
      }

      FileConfiguration beastTokensConfig = this.loadCurrencyConfig("beasttokens.yml");
      boolean beastTokensEnabled = beastTokensConfig.getBoolean("enabled", false);
      if (beastTokensEnabled) {
         String unitxxx = beastTokensConfig.getString("unit", "Tokens");
         String displayNamexxx = beastTokensConfig.getString("display-name", "BeastTokens");
         if (unitxxx == null || unitxxx.isEmpty()) {
            unitxxx = beastTokensConfig.getString("symbol", "Tokens");
            if (unitxxx != null && !unitxxx.isEmpty()) {
               this.currencyErrors.add("BeastTokens currency: 'symbol' is deprecated, please use 'unit' instead!");
            } else {
               this.currencyErrors.add("BeastTokens currency: unit is missing or empty! Using default: 'Tokens'");
               unitxxx = "Tokens";
            }
         }

         if (displayNamexxx == null || displayNamexxx.isEmpty()) {
            this.currencyErrors.add("BeastTokens currency: display-name is missing or empty! Using default: 'BeastTokens'");
            displayNamexxx = "BeastTokens";
         }

         BeastTokensCurrencyHandler beastTokensHandler = new BeastTokensCurrencyHandler(this.plugin, unitxxx, displayNamexxx);
         if (beastTokensHandler.isAvailable()) {
            this.handlers.put(CoinFlipGame.CurrencyType.BEASTTOKENS, beastTokensHandler);
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.BEASTTOKENS, true);
            CurrencySettings settingsxxx = this.loadCurrencySettings(beastTokensConfig, "", "BeastTokens currency");
            this.currencySettings.put(CoinFlipGame.CurrencyType.BEASTTOKENS, settingsxxx);
            if (settingsxxx.getSyntaxCommand() != null && !settingsxxx.getSyntaxCommand().isEmpty()) {
               this.syntaxCommandMap.put(settingsxxx.getSyntaxCommand(), new CurrencyManager.CurrencyInfo(CoinFlipGame.CurrencyType.BEASTTOKENS, null));
            }

            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin.getLogger().info("BeastTokens currency enabled with unit: " + unitxxx);
            }
         } else {
            this.currencyErrors
               .add(
                  "BeastTokens currency is enabled in config but the BeastTokens plugin is not available! Please install BeastTokens (no separate API download needed)."
               );
            this.enabledCurrencies.put(CoinFlipGame.CurrencyType.BEASTTOKENS, false);
         }
      } else {
         this.enabledCurrencies.put(CoinFlipGame.CurrencyType.BEASTTOKENS, false);
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
            this.plugin.getLogger().info("BeastTokens currency is disabled in config.");
         }
      }

      FileConfiguration coinsEngineConfig = this.loadCurrencyConfig("coinsengine.yml");
      if (coinsEngineConfig.contains("dynamic-tax-enabled") || coinsEngineConfig.contains("tax-rate-config")) {
         this.migrateCoinsEngineDynamicTax(coinsEngineConfig);
      }

      if (coinsEngineConfig.contains("currencies")) {
         ConfigurationSection currenciesSection = coinsEngineConfig.getConfigurationSection("currencies");
         if (currenciesSection != null) {
            for (String currencyId : currenciesSection.getKeys(false)) {
               if (currencyId == null || currencyId.isEmpty()) {
                  this.currencyErrors.add("CoinsEngine currency: Found currency with empty ID! Skipping...");
               } else if (!currencyId.contains(" ") && !currencyId.contains(":") && !currencyId.contains(".")) {
                  String path = "currencies." + currencyId;
                  boolean enabled = coinsEngineConfig.getBoolean(path + ".enabled", false);
                  if (enabled) {
                     String unitxxxx = coinsEngineConfig.getString(path + ".unit", "Coins");
                     String displayNamexxxx = coinsEngineConfig.getString(path + ".display-name", currencyId);
                     if (unitxxxx == null || unitxxxx.isEmpty()) {
                        unitxxxx = coinsEngineConfig.getString(path + ".symbol", "Coins");
                        if (unitxxxx != null && !unitxxxx.isEmpty()) {
                           this.currencyErrors.add("CoinsEngine currency '" + currencyId + "': 'symbol' is deprecated, please use 'unit' instead!");
                        } else {
                           this.currencyErrors.add("CoinsEngine currency '" + currencyId + "': unit is missing or empty! Using default: 'Coins'");
                           unitxxxx = "Coins";
                        }
                     }

                     if (displayNamexxxx == null || displayNamexxxx.isEmpty()) {
                        this.currencyErrors
                           .add("CoinsEngine currency '" + currencyId + "': display-name is missing or empty! Using currency ID as display name.");
                        displayNamexxxx = currencyId;
                     }

                     CoinsEngineCurrencyHandler handler = new CoinsEngineCurrencyHandler(this.plugin, currencyId, unitxxxx, displayNamexxxx);
                     CurrencySettings settingsxxxx = this.loadCurrencySettings(coinsEngineConfig, path, "CoinsEngine currency '" + currencyId + "'");
                     this.coinsEngineCurrencySettings.put(currencyId, settingsxxxx);
                     if (settingsxxxx.getSyntaxCommand() != null && !settingsxxxx.getSyntaxCommand().isEmpty()) {
                        this.syntaxCommandMap
                           .put(settingsxxxx.getSyntaxCommand(), new CurrencyManager.CurrencyInfo(CoinFlipGame.CurrencyType.COINSENGINE, currencyId));
                     }

                     this.enabledCoinsEngineCurrencies.put(currencyId, true);
                     if (handler.isAvailable()) {
                        this.coinsEngineHandlers.put(currencyId, handler);
                        if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                           this.plugin.getLogger().info("CoinsEngine currency '" + currencyId + "' enabled with unit: " + unitxxxx);
                        }
                     } else {
                        Plugin coinsEnginePlugin = Bukkit.getServer().getPluginManager().getPlugin("CoinsEngine");
                        if (coinsEnginePlugin == null) {
                           this.currencyErrors
                              .add(
                                 "CoinsEngine currency '"
                                    + currencyId
                                    + "' is enabled in config but CoinsEngine plugin is not installed! Please install CoinsEngine plugin."
                              );
                        } else {
                           try {
                              Class<?> apiClass = Class.forName("su.nightexpress.coinsengine.api.CoinsEngineAPI");
                              Method isLoadedMethod = apiClass.getMethod("isLoaded");
                              Boolean isLoaded = (Boolean)isLoadedMethod.invoke(null);
                              if (!isLoaded) {
                                 this.currencyErrors
                                    .add(
                                       "CoinsEngine currency '"
                                          + currencyId
                                          + "' is enabled in config but CoinsEngine API is not loaded yet! This may be a timing issue - try reloading the plugin after CoinsEngine has fully loaded."
                                    );
                              } else {
                                 Method getCurrencyMethod = apiClass.getMethod("getCurrency", String.class);
                                 Object currency = getCurrencyMethod.invoke(null, currencyId);
                                 if (currency == null) {
                                    this.currencyErrors
                                       .add(
                                          "CoinsEngine currency '"
                                             + currencyId
                                             + "' is enabled in config but the currency does not exist in CoinsEngine! Please check that the currency ID '"
                                             + currencyId
                                             + "' is correct. You can check available currencies in CoinsEngine's configuration."
                                       );
                                 } else {
                                    this.currencyErrors
                                       .add(
                                          "CoinsEngine currency '"
                                             + currencyId
                                             + "' is enabled in config but failed to initialize! Please check the console for more details."
                                       );
                                 }
                              }
                           } catch (Exception var25) {
                              this.currencyErrors
                                 .add(
                                    "CoinsEngine currency '"
                                       + currencyId
                                       + "' is enabled in config but an error occurred while verifying the currency: "
                                       + var25.getMessage()
                                       + ". Please check that CoinsEngine is installed and the currency ID is correct."
                                 );
                           }
                        }
                     }
                  } else {
                     this.enabledCoinsEngineCurrencies.put(currencyId, false);
                  }
               } else {
                  this.currencyErrors
                     .add(
                        "CoinsEngine currency ID '"
                           + currencyId
                           + "' contains invalid characters (spaces, colons, or dots)! Currency ID should match the CoinsEngine currency ID exactly. Skipping..."
                     );
                  this.enabledCoinsEngineCurrencies.put(currencyId, false);
               }
            }
         }
      }

      boolean anyCoinsEngineEnabled = this.enabledCoinsEngineCurrencies.values().stream().anyMatch(Boolean::booleanValue);
      this.enabledCurrencies.put(CoinFlipGame.CurrencyType.COINSENGINE, anyCoinsEngineEnabled);
      if (!anyCoinsEngineEnabled
         && coinsEngineConfig.contains("currencies")
         && this.plugin.getDebugManager() != null
         && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
         this.plugin.getLogger().info("No CoinsEngine currencies are enabled in config.");
      }

      this.ensureCoinsEngineCurrencyFields(coinsEngineConfig);
      FileConfiguration placeholderConfig = this.loadCurrencyConfig("customplaceholder.yml");
      if (placeholderConfig.contains("dynamic-tax-enabled") || placeholderConfig.contains("tax-rate-config")) {
         this.migratePlaceholderDynamicTax(placeholderConfig);
      }

      if (placeholderConfig.contains("currencies")) {
         ConfigurationSection currenciesSection = placeholderConfig.getConfigurationSection("currencies");
         if (currenciesSection != null) {
            for (String currencyIdx : currenciesSection.getKeys(false)) {
               if (currencyIdx == null || currencyIdx.isEmpty()) {
                  this.currencyErrors.add("Placeholder currency: Found currency with empty ID! Skipping...");
               } else if (!currencyIdx.contains(" ") && !currencyIdx.contains(":") && !currencyIdx.contains(".")) {
                  String path = "currencies." + currencyIdx;
                  boolean enabled = placeholderConfig.getBoolean(path + ".enabled", false);
                  if (enabled) {
                     String placeholder = placeholderConfig.getString(path + ".placeholder", "");
                     String unitxxxxx = placeholderConfig.getString(path + ".unit", "Coins");
                     String displayNamexxxxx = placeholderConfig.getString(path + ".display-name", currencyIdx);
                     String giveCommand = placeholderConfig.getString(path + ".give-command", "");
                     String removeCommand = placeholderConfig.getString(path + ".remove-command", "");
                     if (placeholder != null && !placeholder.isEmpty()) {
                        if (!placeholder.startsWith("%") || !placeholder.endsWith("%")) {
                           this.currencyErrors
                              .add(
                                 "Placeholder currency '"
                                    + currencyIdx
                                    + "': placeholder '"
                                    + placeholder
                                    + "' does not appear to be a valid PlaceholderAPI placeholder! Placeholders should start and end with '%' (e.g., %plugin_balance%)."
                              );
                        }

                        if (unitxxxxx == null || unitxxxxx.isEmpty()) {
                           unitxxxxx = placeholderConfig.getString(path + ".symbol", "Coins");
                           if (unitxxxxx != null && !unitxxxxx.isEmpty()) {
                              this.currencyErrors.add("Placeholder currency '" + currencyIdx + "': 'symbol' is deprecated, please use 'unit' instead!");
                           } else {
                              this.currencyErrors.add("Placeholder currency '" + currencyIdx + "': unit is missing or empty! Using default: 'Coins'");
                              unitxxxxx = "Coins";
                           }
                        }

                        if (displayNamexxxxx == null || displayNamexxxxx.isEmpty()) {
                           this.currencyErrors
                              .add("Placeholder currency '" + currencyIdx + "': display-name is missing or empty! Using currency ID as display name.");
                           displayNamexxxxx = currencyIdx;
                        }

                        if (giveCommand != null && !giveCommand.isEmpty()) {
                           if (!giveCommand.contains("{player}") || !giveCommand.contains("{amount}")) {
                              this.currencyErrors
                                 .add(
                                    "Placeholder currency '"
                                       + currencyIdx
                                       + "': give-command '"
                                       + giveCommand
                                       + "' is missing required placeholders! It should contain {player} and {amount} (e.g., 'coins give {player} {amount}')."
                                 );
                           }

                           if (removeCommand != null && !removeCommand.isEmpty()) {
                              if (!removeCommand.contains("{player}") || !removeCommand.contains("{amount}")) {
                                 this.currencyErrors
                                    .add(
                                       "Placeholder currency '"
                                          + currencyIdx
                                          + "': remove-command '"
                                          + removeCommand
                                          + "' is missing required placeholders! It should contain {player} and {amount} (e.g., 'coins take {player} {amount}')."
                                    );
                              }

                              PlaceholderCurrencyHandler handlerx = new PlaceholderCurrencyHandler(
                                 this.plugin, currencyIdx, placeholder, unitxxxxx, displayNamexxxxx, giveCommand, removeCommand
                              );
                              CurrencySettings settingsxxxxx = this.loadCurrencySettings(placeholderConfig, path, "Placeholder currency '" + currencyIdx + "'");
                              this.placeholderCurrencySettings.put(currencyIdx, settingsxxxxx);
                              if (settingsxxxxx.getSyntaxCommand() != null && !settingsxxxxx.getSyntaxCommand().isEmpty()) {
                                 this.syntaxCommandMap
                                    .put(settingsxxxxx.getSyntaxCommand(), new CurrencyManager.CurrencyInfo(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyIdx));
                              }

                              this.enabledPlaceholderCurrencies.put(currencyIdx, true);
                              if (handlerx.isAvailable()) {
                                 this.placeholderHandlers.put(currencyIdx, handlerx);
                                 if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                                    this.plugin
                                       .getLogger()
                                       .info("Placeholder currency '" + currencyIdx + "' enabled with placeholder: " + placeholder + ", unit: " + unitxxxxx);
                                 }
                              } else {
                                 this.currencyErrors
                                    .add(
                                       "Placeholder currency '"
                                          + currencyIdx
                                          + "' is enabled in config but PlaceholderAPI plugin is not installed! Please install PlaceholderAPI plugin."
                                    );
                              }
                           } else {
                              this.currencyErrors
                                 .add(
                                    "Placeholder currency '"
                                       + currencyIdx
                                       + "': remove-command is missing or empty! This currency will be disabled. Please set a command to remove currency (e.g., 'coins take {player} {amount}')."
                                 );
                              this.enabledPlaceholderCurrencies.put(currencyIdx, false);
                           }
                        } else {
                           this.currencyErrors
                              .add(
                                 "Placeholder currency '"
                                    + currencyIdx
                                    + "': give-command is missing or empty! This currency will be disabled. Please set a command to give currency (e.g., 'coins give {player} {amount}')."
                              );
                           this.enabledPlaceholderCurrencies.put(currencyIdx, false);
                        }
                     } else {
                        this.currencyErrors
                           .add(
                              "Placeholder currency '"
                                 + currencyIdx
                                 + "': placeholder is missing or empty! This currency will be disabled. Please set a valid PlaceholderAPI placeholder (e.g., %plugin_balance%)."
                           );
                        this.enabledPlaceholderCurrencies.put(currencyIdx, false);
                     }
                  } else {
                     this.enabledPlaceholderCurrencies.put(currencyIdx, false);
                  }
               } else {
                  this.currencyErrors
                     .add(
                        "Placeholder currency ID '"
                           + currencyIdx
                           + "' contains invalid characters (spaces, colons, or dots)! Currency ID should only contain letters, numbers, and underscores. Skipping..."
                     );
                  this.enabledPlaceholderCurrencies.put(currencyIdx, false);
               }
            }
         }
      }

      boolean anyPlaceholderEnabled = this.enabledPlaceholderCurrencies.values().stream().anyMatch(Boolean::booleanValue);
      this.enabledCurrencies.put(CoinFlipGame.CurrencyType.PLACEHOLDER, anyPlaceholderEnabled);
      if (!anyPlaceholderEnabled
         && placeholderConfig.contains("currencies")
         && this.plugin.getDebugManager() != null
         && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
         this.plugin.getLogger().info("No Placeholder currencies are enabled in config.");
      }

      this.ensurePlaceholderCurrencyFields(placeholderConfig);
      boolean anyCurrencyEnabled = this.enabledCurrencies.values().stream().anyMatch(Boolean::booleanValue);
      if (!anyCurrencyEnabled) {
         this.currencyErrors
            .add(
               "WARNING: No currencies are enabled! Players will not be able to create coinflip games. Please enable at least one currency in currency configuration files."
            );
      }
   }

   public CurrencySettings getCurrencySettings(CoinFlipGame.CurrencyType type, String currencyId) {
      if (type == CoinFlipGame.CurrencyType.COINSENGINE && currencyId != null) {
         return this.coinsEngineCurrencySettings.getOrDefault(currencyId, new CurrencySettings());
      } else {
         return type == CoinFlipGame.CurrencyType.PLACEHOLDER && currencyId != null
            ? this.placeholderCurrencySettings.getOrDefault(currencyId, new CurrencySettings())
            : this.currencySettings.getOrDefault(type, new CurrencySettings());
      }
   }

   private void printCurrencyErrors() {
      if (!this.currencyErrors.isEmpty()) {
         this.plugin.getLogger().warning("==========================================");
         this.plugin.getLogger().warning("CURRENCY CONFIGURATION ERRORS DETECTED");
         this.plugin.getLogger().warning("==========================================");

         for (String error : this.currencyErrors) {
            this.plugin.getLogger().warning("  [ERROR] " + error);
         }

         this.plugin.getLogger().warning("");
         this.plugin.getLogger().warning("Please fix these errors in your currency configuration files.");
         this.plugin.getLogger().warning("Some currencies may be disabled until the issues are resolved.");
         this.plugin.getLogger().warning("==========================================");
      }
   }

   public CurrencyHandler getHandler(CoinFlipGame.CurrencyType type) {
      return this.handlers.get(type);
   }

   public CoinsEngineCurrencyHandler getCoinsEngineHandler(String currencyId) {
      return this.coinsEngineHandlers.get(currencyId);
   }

   public PlaceholderCurrencyHandler getPlaceholderHandler(String currencyId) {
      return this.placeholderHandlers.get(currencyId);
   }

   public CurrencyHandler getHandler(CoinFlipGame.CurrencyType type, String currencyId) {
      if (type == CoinFlipGame.CurrencyType.COINSENGINE && currencyId != null) {
         return this.getCoinsEngineHandler(currencyId);
      } else {
         return (CurrencyHandler)(type == CoinFlipGame.CurrencyType.PLACEHOLDER && currencyId != null
            ? this.getPlaceholderHandler(currencyId)
            : this.getHandler(type));
      }
   }

   public boolean isCurrencyEnabled(CoinFlipGame.CurrencyType type) {
      return this.enabledCurrencies.getOrDefault(type, false);
   }

   public boolean isCoinsEngineCurrencyEnabled(String currencyId) {
      return this.enabledCoinsEngineCurrencies.getOrDefault(currencyId, false);
   }

   public boolean isPlaceholderCurrencyEnabled(String currencyId) {
      return this.enabledPlaceholderCurrencies.getOrDefault(currencyId, false);
   }

   public boolean isCurrencyEnabled(CoinFlipGame.CurrencyType type, String currencyId) {
      if (type == CoinFlipGame.CurrencyType.COINSENGINE && currencyId != null) {
         return this.isCoinsEngineCurrencyEnabled(currencyId);
      } else {
         return type == CoinFlipGame.CurrencyType.PLACEHOLDER && currencyId != null
            ? this.isPlaceholderCurrencyEnabled(currencyId)
            : this.isCurrencyEnabled(type);
      }
   }

   public double getBalance(Player player, CoinFlipGame.CurrencyType type) {
      return this.getBalance(player, type, null);
   }

   public double getBalance(Player player, CoinFlipGame.CurrencyType type, String currencyId) {
      if (player == null) {
         return 0.0;
      } else {
         CurrencyHandler handler = this.getHandler(type, currencyId);
         return handler == null ? 0.0 : handler.getBalance(player);
      }
   }

   public boolean hasBalance(Player player, CoinFlipGame.CurrencyType type, double amount) {
      return this.hasBalance(player, type, null, amount);
   }

   public boolean hasBalance(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      if (player == null || !player.isOnline()) {
         return false;
      } else if (amount < 0.0) {
         return false;
      } else {
         CurrencyHandler handler = this.getHandler(type, currencyId);
         return handler == null ? false : handler.hasBalance(player, amount);
      }
   }

   public boolean hasBalanceWithReserve(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      if (player == null) {
         return false;
      } else {
         CurrencyHandler handler = this.getHandler(type, currencyId);
         if (handler == null) {
            return false;
         } else {
            CurrencySettings settings = this.getCurrencySettings(type, currencyId);
            double minReserveBalance = settings.getMinReserveBalance();
            if (minReserveBalance <= 0.0) {
               return handler.hasBalance(player, amount);
            } else {
               double balance = handler.getBalance(player);
               return balance >= amount && balance - amount >= minReserveBalance;
            }
         }
      }
   }

   public boolean isReserveBalanceIssue(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      if (player == null) {
         return false;
      } else {
         CurrencyHandler handler = this.getHandler(type, currencyId);
         if (handler == null) {
            return false;
         } else {
            CurrencySettings settings = this.getCurrencySettings(type, currencyId);
            double minReserveBalance = settings.getMinReserveBalance();
            if (minReserveBalance <= 0.0) {
               return false;
            } else {
               double balance = handler.getBalance(player);
               return balance >= amount && balance - amount < minReserveBalance;
            }
         }
      }
   }

   public double getMinReserveBalance(CoinFlipGame.CurrencyType type, String currencyId) {
      CurrencySettings settings = this.getCurrencySettings(type, currencyId);
      return settings != null ? settings.getMinReserveBalance() : 0.0;
   }

   public boolean withdraw(Player player, CoinFlipGame.CurrencyType type, double amount) {
      return this.withdraw(player, type, null, amount);
   }

   public boolean withdraw(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      if (player != null && player.isOnline()) {
         if (amount <= 0.0) {
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin
                  .getDebugManager()
                  .warning(DebugManager.Category.CURRENCY, String.format("Withdraw failed: Invalid amount %.2f for player %s", amount, player.getName()));
            }

            return false;
         } else {
            CurrencyHandler handler = this.getHandler(type, currencyId);
            if (handler == null) {
               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  this.plugin
                     .getDebugManager()
                     .warning(
                        DebugManager.Category.CURRENCY,
                        String.format("Withdraw failed: No handler found for type=%s, currencyId=%s, player=%s", type, currencyId, player.getName())
                     );
               }

               return false;
            } else {
               double balanceBefore = handler.getBalance(player);
               boolean result = handler.withdraw(player, amount);
               if (result) {
                  double balanceAfter = handler.getBalance(player);
                  double actualDeduction = balanceBefore - balanceAfter;
                  if (actualDeduction < amount - 0.01) {
                     ExploitDetector detector = this.plugin.getExploitDetector();
                     if (detector != null && detector.isEnabled()) {
                        String details = String.format("Before=%.2f, After=%.2f, Deducted=%.2f", balanceBefore, balanceAfter, actualDeduction);
                        detector.reportCurrency(
                           player, ExploitDetector.ExploitType.WITHDRAW_VERIFICATION_FAILED, type, currencyId, amount, actualDeduction, details
                        );
                     }

                     if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                        this.plugin
                           .getDebugManager()
                           .warning(
                              DebugManager.Category.CURRENCY,
                              String.format(
                                 "Withdraw verification failed: Player=%s, Currency=%s, Amount=%.2f, BalanceBefore=%.2f, BalanceAfter=%.2f, ActualDeduction=%.2f",
                                 player.getName(),
                                 currencyId != null ? currencyId : type.name(),
                                 amount,
                                 balanceBefore,
                                 balanceAfter,
                                 actualDeduction
                              )
                           );
                     }

                     return false;
                  }
               }

               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  String currencyInfo = currencyId != null ? currencyId : type.name();
                  this.plugin
                     .getDebugManager()
                     .info(
                        DebugManager.Category.CURRENCY,
                        String.format("Withdraw: Player=%s, Currency=%s, Amount=%.2f, Success=%s", player.getName(), currencyInfo, amount, result)
                     );
               }

               return result;
            }
         }
      } else {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
            this.plugin.getDebugManager().warning(DebugManager.Category.CURRENCY, "Withdraw failed: Player is null or offline");
         }

         return false;
      }
   }

   public boolean deposit(Player player, CoinFlipGame.CurrencyType type, double amount) {
      return this.deposit(player, type, null, amount);
   }

   public boolean deposit(Player player, CoinFlipGame.CurrencyType type, String currencyId, double amount) {
      if (player != null && player.isOnline()) {
         if (amount <= 0.0) {
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
               this.plugin
                  .getDebugManager()
                  .warning(DebugManager.Category.CURRENCY, String.format("Deposit failed: Invalid amount %.2f for player %s", amount, player.getName()));
            }

            return false;
         } else {
            CurrencyHandler handler = this.getHandler(type, currencyId);
            if (handler == null) {
               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  this.plugin
                     .getDebugManager()
                     .warning(
                        DebugManager.Category.CURRENCY,
                        String.format("Deposit failed: No handler found for type=%s, currencyId=%s, player=%s", type, currencyId, player.getName())
                     );
               }

               return false;
            } else {
               double balanceBefore = handler.getBalance(player);
               boolean result = handler.deposit(player, amount);
               if (result) {
                  double balanceAfter = handler.getBalance(player);
                  double actualIncrease = balanceAfter - balanceBefore;
                  if (actualIncrease < amount - 0.01) {
                     ExploitDetector detector = this.plugin.getExploitDetector();
                     if (detector != null && detector.isEnabled()) {
                        String details = String.format("Before=%.2f, After=%.2f, Increased=%.2f", balanceBefore, balanceAfter, actualIncrease);
                        detector.reportCurrency(
                           player, ExploitDetector.ExploitType.DEPOSIT_VERIFICATION_FAILED, type, currencyId, amount, actualIncrease, details
                        );
                     }

                     if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                        this.plugin
                           .getDebugManager()
                           .warning(
                              DebugManager.Category.CURRENCY,
                              String.format(
                                 "Deposit verification warning: Player=%s, Currency=%s, Amount=%.2f, BalanceBefore=%.2f, BalanceAfter=%.2f, ActualIncrease=%.2f",
                                 player.getName(),
                                 currencyId != null ? currencyId : type.name(),
                                 amount,
                                 balanceBefore,
                                 balanceAfter,
                                 actualIncrease
                              )
                           );
                     }
                  }
               }

               if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  String currencyInfo = currencyId != null ? currencyId : type.name();
                  this.plugin
                     .getDebugManager()
                     .info(
                        DebugManager.Category.CURRENCY,
                        String.format("Deposit: Player=%s, Currency=%s, Amount=%.2f, Success=%s", player.getName(), currencyInfo, amount, result)
                     );
               }

               return result;
            }
         }
      } else {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
            this.plugin.getDebugManager().warning(DebugManager.Category.CURRENCY, "Deposit failed: Player is null or offline");
         }

         return false;
      }
   }

   public String getUnit(CoinFlipGame.CurrencyType type) {
      return this.getUnit(type, null);
   }

   public String getUnit(CoinFlipGame.CurrencyType type, String currencyId) {
      CurrencyHandler handler = this.getHandler(type, currencyId);
      return handler == null ? "" : handler.getUnit();
   }

   @Deprecated
   public String getSymbol(CoinFlipGame.CurrencyType type) {
      return this.getUnit(type);
   }

   @Deprecated
   public String getSymbol(CoinFlipGame.CurrencyType type, String currencyId) {
      return this.getUnit(type, currencyId);
   }

   public String getDisplayName(CoinFlipGame.CurrencyType type) {
      return this.getDisplayName(type, null);
   }

   public String getDisplayName(CoinFlipGame.CurrencyType type, String currencyId) {
      CurrencyHandler handler = this.getHandler(type, currencyId);
      return handler == null ? type.name() : handler.getDisplayName();
   }

   public Set<String> getEnabledCoinsEngineCurrencyIds() {
      return this.enabledCoinsEngineCurrencies.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toSet());
   }

   public Set<String> getEnabledPlaceholderCurrencyIds() {
      return this.enabledPlaceholderCurrencies.entrySet().stream().filter(Entry::getValue).map(Entry::getKey).collect(Collectors.toSet());
   }

   public String getSyntaxCommand(CoinFlipGame.CurrencyType type, String currencyId) {
      CurrencySettings settings = this.getCurrencySettings(type, currencyId);
      return settings != null ? settings.getSyntaxCommand() : "";
   }

   public CurrencyManager.CurrencyInfo parseCurrencyFromSyntaxCommand(String syntaxCommand) {
      if (syntaxCommand != null && !syntaxCommand.trim().isEmpty()) {
         String normalized = syntaxCommand.trim().toLowerCase();
         return this.syntaxCommandMap.get(normalized);
      } else {
         return null;
      }
   }

   public List<String> getAllSyntaxCommands() {
      return new ArrayList<>(this.syntaxCommandMap.keySet());
   }

   public boolean canPlayerUseCurrency(Player player, CoinFlipGame.CurrencyType type, String currencyId) {
      if (player == null) {
         return false;
      } else {
         CurrencySettings settings = this.getCurrencySettings(type, currencyId);
         if (settings == null) {
            return true;
         } else {
            CurrencyRestrictions restrictions = settings.getRestrictions();
            return restrictions != null && restrictions.isRestrictionsEnabled() ? restrictions.canPlayerUse(player) : true;
         }
      }
   }

   public boolean canPlayersGambleTogether(Player player1, Player player2, CoinFlipGame.CurrencyType type, String currencyId) {
      return player1 != null && player2 != null
         ? this.canPlayerUseCurrency(player1, type, currencyId) && this.canPlayerUseCurrency(player2, type, currencyId)
         : false;
   }

   public String getRestrictionReason(Player player, CoinFlipGame.CurrencyType type, String currencyId) {
      if (player == null) {
         return null;
      } else {
         CurrencySettings settings = this.getCurrencySettings(type, currencyId);
         if (settings == null) {
            return null;
         } else {
            CurrencyRestrictions restrictions = settings.getRestrictions();
            if (restrictions != null && restrictions.isRestrictionsEnabled()) {
               World world = player.getWorld();
               if (world == null) {
                  return null;
               } else {
                  String worldName = world.getName();
                  if (worldName != null && !worldName.isEmpty()) {
                     if (!restrictions.isWorldAllowed(worldName)) {
                        List<String> blockedWorlds = restrictions.getBlockedWorlds();
                        List<String> allowedWorlds = restrictions.getAllowedWorlds();
                        if (blockedWorlds != null && blockedWorlds.contains(worldName)) {
                           String message = this.plugin.getMessage("restriction.world-blocked");
                           if (message != null && !message.isEmpty()) {
                              StringBuilder sb = new StringBuilder(message.length() + worldName.length() * 2);
                              sb.append(message);

                              int index;
                              while ((index = sb.indexOf("<world>")) != -1) {
                                 sb.replace(index, index + 7, worldName);
                              }

                              while ((index = sb.indexOf("%world%")) != -1) {
                                 sb.replace(index, index + 8, worldName);
                              }

                              return sb.toString();
                           }
                        } else if (allowedWorlds != null && !allowedWorlds.isEmpty()) {
                           String worldsList = String.join(", ", allowedWorlds);
                           String message = this.plugin.getMessage("restriction.world-allowed-only");
                           if (message != null && !message.isEmpty()) {
                              StringBuilder sb = new StringBuilder(message.length() + worldsList.length() * 2);
                              sb.append(message);

                              int index;
                              while ((index = sb.indexOf("<worlds>")) != -1) {
                                 sb.replace(index, index + 9, worldsList);
                              }

                              while ((index = sb.indexOf("%worlds%")) != -1) {
                                 sb.replace(index, index + 9, worldsList);
                              }

                              return sb.toString();
                           }
                        }
                     }

                     if (!restrictions.hasRequiredPermissions(player)) {
                        List<String> requiredPermissions = restrictions.getRequiredPermissions();
                        if (requiredPermissions != null && !requiredPermissions.isEmpty()) {
                           if (requiredPermissions.size() == 1) {
                              String permission = requiredPermissions.get(0);
                              if (permission != null && !permission.isEmpty()) {
                                 String message = this.plugin.getMessage("restriction.permission-required-single");
                                 if (message != null && !message.isEmpty()) {
                                    StringBuilder sb = new StringBuilder(message.length() + permission.length() * 2);
                                    sb.append(message);

                                    int index;
                                    while ((index = sb.indexOf("<permission>")) != -1) {
                                       sb.replace(index, index + 13, permission);
                                    }

                                    while ((index = sb.indexOf("%permission%")) != -1) {
                                       sb.replace(index, index + 14, permission);
                                    }

                                    return sb.toString();
                                 }
                              }
                           } else {
                              String permissionsList = String.join(", ", requiredPermissions);
                              String message = this.plugin.getMessage("restriction.permission-required-multiple");
                              if (message != null && !message.isEmpty()) {
                                 StringBuilder sb = new StringBuilder(message.length() + permissionsList.length() * 2);
                                 sb.append(message);

                                 int index;
                                 while ((index = sb.indexOf("<permissions>")) != -1) {
                                    sb.replace(index, index + 14, permissionsList);
                                 }

                                 while ((index = sb.indexOf("%permissions%")) != -1) {
                                    sb.replace(index, index + 15, permissionsList);
                                 }

                                 return sb.toString();
                              }
                           }
                        }
                     }

                     return null;
                  } else {
                     return null;
                  }
               }
            } else {
               return null;
            }
         }
      }
   }

   private void migrateCoinsEngineDynamicTax(FileConfiguration config) {
      try {
         if (!config.contains("currencies")) {
            return;
         }

         boolean globalDynamicTaxEnabled = config.getBoolean("dynamic-tax-enabled", false);
         ConfigurationSection globalTaxRateConfig = config.getConfigurationSection("tax-rate-config");
         if (!config.contains("dynamic-tax-enabled") && !config.contains("tax-rate-config")) {
            return;
         }

         ConfigurationSection currenciesSection = config.getConfigurationSection("currencies");
         if (currenciesSection == null) {
            return;
         }

         boolean migrated = false;

         for (String currencyId : currenciesSection.getKeys(false)) {
            String currencyPath = "currencies." + currencyId;
            if (!config.contains(currencyPath + ".dynamic-tax-enabled")) {
               config.set(currencyPath + ".dynamic-tax-enabled", globalDynamicTaxEnabled);
               if (globalTaxRateConfig != null) {
                  double baseTaxRate = config.getDouble("tax-rate-config.base-tax-rate", 0.1);
                  config.set(currencyPath + ".tax-rate-config.base-tax-rate", baseTaxRate);
                  if (config.contains("tax-rate-config.tiers")) {
                     List<?> tiers = config.getList("tax-rate-config.tiers");
                     if (tiers != null && !tiers.isEmpty()) {
                        config.set(currencyPath + ".tax-rate-config.tiers", tiers);
                     }
                  }
               }

               migrated = true;
            }
         }

         if (migrated) {
            config.set("dynamic-tax-enabled", null);
            config.set("tax-rate-config", null);
            File configFile = new File(this.plugin.getDataFolder(), "currencies/coinsengine.yml");
            config.save(configFile);
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
               this.plugin.getLogger().info("Auto-migrated coinsengine.yml from version 3 to version 4 (dynamic tax moved to per-currency)");
            }
         }
      } catch (Exception var12) {
         this.plugin.getLogger().warning("Failed to auto-migrate coinsengine.yml dynamic tax configuration: " + var12.getMessage());
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
            var12.printStackTrace();
         }
      }
   }

   private void migratePlaceholderDynamicTax(FileConfiguration config) {
      try {
         if (!config.contains("currencies")) {
            return;
         }

         boolean globalDynamicTaxEnabled = config.getBoolean("dynamic-tax-enabled", false);
         ConfigurationSection globalTaxRateConfig = config.getConfigurationSection("tax-rate-config");
         if (!config.contains("dynamic-tax-enabled") && !config.contains("tax-rate-config")) {
            return;
         }

         ConfigurationSection currenciesSection = config.getConfigurationSection("currencies");
         if (currenciesSection == null) {
            return;
         }

         boolean migrated = false;

         for (String currencyId : currenciesSection.getKeys(false)) {
            String currencyPath = "currencies." + currencyId;
            if (!config.contains(currencyPath + ".dynamic-tax-enabled")) {
               config.set(currencyPath + ".dynamic-tax-enabled", globalDynamicTaxEnabled);
               if (globalTaxRateConfig != null) {
                  double baseTaxRate = config.getDouble("tax-rate-config.base-tax-rate", 0.1);
                  config.set(currencyPath + ".tax-rate-config.base-tax-rate", baseTaxRate);
                  if (config.contains("tax-rate-config.tiers")) {
                     List<?> tiers = config.getList("tax-rate-config.tiers");
                     if (tiers != null && !tiers.isEmpty()) {
                        config.set(currencyPath + ".tax-rate-config.tiers", tiers);
                     }
                  }
               }

               migrated = true;
            }
         }

         if (migrated) {
            config.set("dynamic-tax-enabled", null);
            config.set("tax-rate-config", null);
            File configFile = new File(this.plugin.getDataFolder(), "currencies/customplaceholder.yml");
            config.save(configFile);
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
               this.plugin.getLogger().info("Auto-migrated customplaceholder.yml from version 3 to version 4 (dynamic tax moved to per-currency)");
            }
         }
      } catch (Exception var12) {
         this.plugin.getLogger().warning("Failed to auto-migrate customplaceholder.yml dynamic tax configuration: " + var12.getMessage());
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
            var12.printStackTrace();
         }
      }
   }

   private void ensurePlaceholderCurrencyFields(FileConfiguration config) {
      try {
         if (!config.contains("currencies")) {
            return;
         }

         ConfigurationSection currenciesSection = config.getConfigurationSection("currencies");
         if (currenciesSection == null) {
            return;
         }

         boolean needsSave = false;
         Map<String, Object> requiredFields = new HashMap<>();
         requiredFields.put("min-reserve-balance", 0);

         for (String currencyId : currenciesSection.getKeys(false)) {
            String currencyPath = "currencies." + currencyId;

            for (Entry<String, Object> field : requiredFields.entrySet()) {
               String fieldPath = currencyPath + "." + field.getKey();
               if (!config.contains(fieldPath)) {
                  config.set(fieldPath, field.getValue());
                  needsSave = true;
                  if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
                     this.plugin
                        .getLogger()
                        .info("Added missing field '" + field.getKey() + "' to currency '" + currencyId + "' with default value: " + field.getValue());
                  }
               }
            }
         }

         if (needsSave) {
            File configFile = new File(this.plugin.getDataFolder(), "currencies/customplaceholder.yml");
            config.save(configFile);
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
               this.plugin.getLogger().info("Updated customplaceholder.yml: Added missing fields to user currencies");
            }
         }
      } catch (Exception var11) {
         this.plugin.getLogger().warning("Failed to ensure placeholder currency fields: " + var11.getMessage());
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
            var11.printStackTrace();
         }
      }
   }

   private void ensureCoinsEngineCurrencyFields(FileConfiguration config) {
      try {
         if (!config.contains("currencies")) {
            return;
         }

         ConfigurationSection currenciesSection = config.getConfigurationSection("currencies");
         if (currenciesSection == null) {
            return;
         }

         boolean needsSave = false;
         Map<String, Object> requiredFields = new HashMap<>();
         requiredFields.put("unit", "Coins");
         requiredFields.put("display-name", null);
         requiredFields.put("syntax-command", "");
         requiredFields.put("broadcast-enabled", true);
         requiredFields.put("min-broadcast-amount", 100);
         requiredFields.put("min-bid", 1);
         requiredFields.put("max-bid", -1);
         requiredFields.put("min-reserve-balance", 0);
         requiredFields.put("tax-enabled", true);
         requiredFields.put("tax-rate", 0.1);
         requiredFields.put("dynamic-tax-enabled", false);

         for (String currencyId : currenciesSection.getKeys(false)) {
            String currencyPath = "currencies." + currencyId;

            for (Entry<String, Object> field : requiredFields.entrySet()) {
               String fieldPath = currencyPath + "." + field.getKey();
               if (!config.contains(fieldPath)) {
                  Object defaultValue = field.getValue();
                  if (field.getKey().equals("display-name") && defaultValue == null) {
                     defaultValue = currencyId;
                  }

                  config.set(fieldPath, defaultValue);
                  needsSave = true;
                  if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
                     this.plugin
                        .getLogger()
                        .info("Added missing field '" + field.getKey() + "' to CoinsEngine currency '" + currencyId + "' with default value: " + defaultValue);
                  }
               }
            }

            if (config.getBoolean(currencyPath + ".dynamic-tax-enabled", false)) {
               String taxConfigPath = currencyPath + ".tax-rate-config";
               if (!config.contains(taxConfigPath)) {
                  config.set(taxConfigPath + ".base-tax-rate", 0.1);
                  Map<String, Object> tier1 = new HashMap<>();
                  tier1.put("min-amount", 0);
                  tier1.put("max-amount", 100);
                  tier1.put("tax-rate", 0.05);
                  Map<String, Object> tier2 = new HashMap<>();
                  tier2.put("min-amount", 100);
                  tier2.put("max-amount", 1000);
                  tier2.put("tax-rate", 0.1);
                  Map<String, Object> tier3 = new HashMap<>();
                  tier3.put("min-amount", 1000);
                  tier3.put("max-amount", -1);
                  tier3.put("tax-rate", 0.15);
                  config.set(taxConfigPath + ".tiers", Arrays.asList(tier1, tier2, tier3));
                  needsSave = true;
                  if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
                     this.plugin.getLogger().info("Added missing tax-rate-config to CoinsEngine currency '" + currencyId + "'");
                  }
               }
            }
         }

         if (needsSave) {
            File configFile = new File(this.plugin.getDataFolder(), "currencies/coinsengine.yml");
            config.save(configFile);
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
               this.plugin.getLogger().info("Updated coinsengine.yml: Added missing fields to user currencies");
            }
         }
      } catch (Exception var12) {
         this.plugin.getLogger().warning("Failed to ensure CoinsEngine currency fields: " + var12.getMessage());
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)) {
            var12.printStackTrace();
         }
      }
   }

   @Generated
   public KStudio getPlugin() {
      return this.plugin;
   }

   @Generated
   public ConfigUpdater getConfigUpdater() {
      return this.configUpdater;
   }

   @Generated
   public Map<CoinFlipGame.CurrencyType, CurrencyHandler> getHandlers() {
      return this.handlers;
   }

   @Generated
   public Map<CoinFlipGame.CurrencyType, Boolean> getEnabledCurrencies() {
      return this.enabledCurrencies;
   }

   @Generated
   public Map<String, CoinsEngineCurrencyHandler> getCoinsEngineHandlers() {
      return this.coinsEngineHandlers;
   }

   @Generated
   public Map<String, Boolean> getEnabledCoinsEngineCurrencies() {
      return this.enabledCoinsEngineCurrencies;
   }

   @Generated
   public Map<String, PlaceholderCurrencyHandler> getPlaceholderHandlers() {
      return this.placeholderHandlers;
   }

   @Generated
   public Map<String, Boolean> getEnabledPlaceholderCurrencies() {
      return this.enabledPlaceholderCurrencies;
   }

   @Generated
   public Map<CoinFlipGame.CurrencyType, CurrencySettings> getCurrencySettings() {
      return this.currencySettings;
   }

   @Generated
   public Map<String, CurrencySettings> getCoinsEngineCurrencySettings() {
      return this.coinsEngineCurrencySettings;
   }

   @Generated
   public Map<String, CurrencySettings> getPlaceholderCurrencySettings() {
      return this.placeholderCurrencySettings;
   }

   @Generated
   public Map<String, CurrencyManager.CurrencyInfo> getSyntaxCommandMap() {
      return this.syntaxCommandMap;
   }

   @Generated
   public List<String> getCurrencyErrors() {
      return this.currencyErrors;
   }

   public static class CurrencyInfo {
      private final CoinFlipGame.CurrencyType type;
      private final String currencyId;

      public CurrencyInfo(CoinFlipGame.CurrencyType type, String currencyId) {
         this.type = type;
         this.currencyId = currencyId;
      }

      public CoinFlipGame.CurrencyType getType() {
         return this.type;
      }

      public String getCurrencyId() {
         return this.currencyId;
      }
   }
}
