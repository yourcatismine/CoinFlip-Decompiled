package com.kstudio.ultracoinflip.config;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import com.kstudio.ultracoinflip.util.VersionDetector;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
   private static final char LEGACY_COLOR_CHAR = '§';
   private final KStudio plugin;
   private final ConfigUpdater configUpdater;
   private FileConfiguration config;
   private FileConfiguration guiConfig;
   private FileConfiguration messages;
   private FileConfiguration soundsConfig;
   private File configFile;
   private File guiConfigFile;
   private File guiFolder;
   private File messagesFile;
   private File soundsFile;
   private String language;
   private List<String> configErrors = new ArrayList<>();
   private List<String> guiErrors = new ArrayList<>();
   private List<String> languageErrors = new ArrayList<>();
   private List<String> soundsErrors = new ArrayList<>();

   public ConfigManager(KStudio plugin) {
      this.plugin = plugin;
      this.configUpdater = new ConfigUpdater(plugin);
      this.loadConfigs();
      this.validateConfigs();
   }

   public void loadConfigs() {
      this.saveDefaultConfig();
      this.reloadConfig();
      this.saveDefaultGUIConfig();
      this.reloadGUIConfig();
      this.saveDefaultSoundsConfig();
      this.reloadSoundsConfig();
      this.saveAllDefaultLanguages();
      if (this.config != null) {
         String langValue = this.config.getString("language", "en");
         this.language = langValue != null ? langValue.toLowerCase() : "en";
         if ("zh".equals(this.language)) {
            this.plugin
               .getLogger()
               .info("Language 'zh' is deprecated. Using 'zh_cn' (Simplified Chinese) instead. Please update your config.yml to use 'zh_cn' or 'zh_tw'.");
            this.language = "zh_cn";
         }
      } else {
         this.language = "en";
         this.plugin.getLogger().warning("Config is null, using default language: en");
      }

      this.saveDefaultLanguage(this.language);
      this.reloadLanguage();
   }

   public void saveDefaultConfig() {
      if (this.configFile == null) {
         this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
      }

      if (!this.configFile.exists()) {
         this.plugin.saveDefaultConfig();
      }
   }

   public void reloadConfig() {
      this.configErrors.clear();
      if (this.configFile == null) {
         this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
      }

      try {
         this.configUpdater.updateConfig(this.configFile, "config.yml", "config-version", true);
      } catch (Exception var11) {
      }

      if (!this.configFile.exists()) {
         this.configErrors.add("Config file 'config.yml' does not exist! Creating default...");
         this.saveDefaultConfig();
      }

      try {
         this.config = YamlConfiguration.loadConfiguration(this.configFile);
         InputStream defConfigStream = null;

         try {
            defConfigStream = this.plugin.getResource("config.yml");
            if (defConfigStream != null) {
               YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
               this.config.setDefaults(defConfig);
            }
         } finally {
            if (defConfigStream != null) {
               try {
                  defConfigStream.close();
               } catch (Exception var10) {
               }
            }
         }
      } catch (Exception var13) {
         this.configErrors.add("Failed to load config.yml: " + var13.getMessage());
         this.plugin.getLogger().severe("Failed to load config.yml: " + var13.getMessage());
         var13.printStackTrace();
      }
   }

   public void saveDefaultGUIConfig() {
      if (this.guiFolder == null) {
         this.guiFolder = new File(this.plugin.getDataFolder(), "gui");
      }

      if (!this.guiFolder.exists()) {
         this.guiFolder.mkdirs();
         this.plugin.getLogger().info("Created GUI config folder: " + this.guiFolder.getPath());
      }

      String[] guiConfigFiles = new String[]{"mainmenu.yml", "history.yml", "flipping.yml", "create.yml", "leaderboard.yml", "heads-tails.yml", "settings.yml"};

      for (String fileName : guiConfigFiles) {
         File guiFile = new File(this.guiFolder, fileName);
         if (!guiFile.exists()) {
            try {
               this.plugin.saveResource("gui/" + fileName, false);
               this.plugin.getLogger().info("Saved default GUI config file: " + fileName);
            } catch (Exception var8) {
               this.plugin.getLogger().warning("Could not save default gui/" + fileName + ": " + var8.getMessage());
            }
         }
      }
   }

   public void reloadGUIConfig() {
      this.guiErrors.clear();
      if (this.guiFolder == null) {
         this.guiFolder = new File(this.plugin.getDataFolder(), "gui");
      }

      if (!this.guiFolder.exists() || !new File(this.guiFolder, "mainmenu.yml").exists()) {
         this.saveDefaultGUIConfig();
      }

      try {
         this.guiConfig = new YamlConfiguration();
         String[] guiConfigFiles = new String[]{
            "mainmenu.yml", "history.yml", "flipping.yml", "create.yml", "leaderboard.yml", "heads-tails.yml", "settings.yml"
         };

         for (String fileName : guiConfigFiles) {
            File guiFile = new File(this.guiFolder, fileName);
            if (guiFile.exists()) {
               try {
                  this.configUpdater.updateConfig(guiFile, "gui/" + fileName, "config-version", false);
               } catch (Exception var27) {
               }

               try {
                  FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(guiFile);
                  boolean fileModified = this.validateAndFixMaterialsInFile(fileConfig, fileName);
                  if (fileModified) {
                     try {
                        fileConfig.save(guiFile);
                        this.plugin
                           .getLogger()
                           .info("Auto-updated incompatible materials in " + fileName + " for version " + VersionDetector.getServerVersion());
                     } catch (Exception var26) {
                        this.plugin.getLogger().warning("Failed to save auto-fixed materials to " + fileName + ": " + var26.getMessage());
                     }
                  }

                  for (String key : fileConfig.getKeys(true)) {
                     if (!fileConfig.isConfigurationSection(key)) {
                        this.guiConfig.set(key, fileConfig.get(key));
                     }
                  }

                  InputStream defConfigStream = null;

                  try {
                     defConfigStream = this.plugin.getResource("gui/" + fileName);
                     if (defConfigStream != null) {
                        FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                        fileConfig.setDefaults(defConfig);

                        for (String keyx : defConfig.getKeys(true)) {
                           if (!defConfig.isConfigurationSection(keyx) && !this.guiConfig.contains(keyx)) {
                              this.guiConfig.set(keyx, defConfig.get(keyx));
                           }
                        }
                     }
                  } finally {
                     if (defConfigStream != null) {
                        try {
                           defConfigStream.close();
                        } catch (Exception var25) {
                        }
                     }
                  }
               } catch (Exception var29) {
                  this.guiErrors.add("Failed to load gui/" + fileName + ": " + var29.getMessage());
                  this.plugin.getLogger().warning("Failed to load gui/" + fileName + ": " + var29.getMessage());
               }
            } else {
               this.guiErrors.add("GUI config file 'gui/" + fileName + "' does not exist! Creating default...");
               this.plugin.getLogger().warning("GUI config file 'gui/" + fileName + "' does not exist!");
            }
         }

         if (this.guiConfigFile == null) {
            this.guiConfigFile = new File(this.plugin.getDataFolder(), "gui.yml");
         }

         if (this.guiConfigFile.exists()) {
            File oldGuiFile = new File(this.plugin.getDataFolder(), "gui.yml.old");
            if (!oldGuiFile.exists()) {
               try {
                  boolean renamed = this.guiConfigFile.renameTo(oldGuiFile);
                  if (renamed) {
                     this.plugin.getLogger().info("Renamed old gui.yml to gui.yml.old. Plugin now uses gui folder structure only.");
                  } else {
                     this.plugin.getLogger().warning("Failed to rename gui.yml to gui.yml.old. Please rename it manually to avoid conflicts.");
                  }
               } catch (Exception var24) {
                  this.plugin.getLogger().warning("Failed to rename gui.yml: " + var24.getMessage());
               }
            } else {
               this.plugin.getLogger().warning("Old gui.yml file detected but gui.yml.old already exists. Please remove one of them manually.");
            }
         }

         if (this.guiConfig.getKeys(true).isEmpty()) {
            this.plugin
               .getLogger()
               .severe(
                  "GUI folder structure appears to be empty! Please ensure gui folder has all required files (mainmenu.yml, history.yml, flipping.yml, create.yml)."
               );
         }
      } catch (Exception var30) {
         this.guiErrors.add("Failed to load GUI config: " + var30.getMessage());
         this.plugin.getLogger().severe("Failed to load GUI config: " + var30.getMessage());
         var30.printStackTrace();
      }
   }

   public void saveAllDefaultLanguages() {
      File langsFolder = new File(this.plugin.getDataFolder(), "langs");
      if (!langsFolder.exists()) {
         langsFolder.mkdirs();
         this.plugin.getLogger().info("Created language folder: " + langsFolder.getPath());
      }

      String[] availableLanguages = new String[]{"en", "vi", "de", "fr", "zh_cn", "zh_tw", "ru", "nl", "es", "ar", "it"};
      int savedCount = 0;
      int updatedCount = 0;
      int skippedCount = 0;
      int errorCount = 0;

      for (String lang : availableLanguages) {
         File langFile = new File(langsFolder, "messages_" + lang + ".yml");
         String resourcePath = "langs/messages_" + lang + ".yml";
         if (!langFile.exists()) {
            try {
               if (this.plugin.getResource(resourcePath) != null) {
                  this.plugin.saveResource(resourcePath, false);
                  this.plugin.getLogger().info("Saved default language file: messages_" + lang + ".yml");
                  savedCount++;
               } else {
                  this.plugin.getLogger().warning("Language file not found in JAR: " + resourcePath);
                  errorCount++;
               }
            } catch (Exception var25) {
               this.plugin.getLogger().warning("Could not save default messages_" + lang + ".yml: " + var25.getMessage());
               errorCount++;
            }
         } else if ("it".equals(lang) && "messages_it.yml".equals(langFile.getName())) {
            try {
               YamlConfiguration tempConfig = YamlConfiguration.loadConfiguration(langFile);
               int currentVersion = tempConfig.getInt("config-version", 0);
               if (currentVersion < 8) {
                  this.plugin
                     .getLogger()
                     .warning(
                        "Detected messages_it.yml file with version " + currentVersion + ". Deleting old version and restoring clean version 8 from JAR..."
                     );
                  File backupDir = new File(this.plugin.getDataFolder(), "langs" + File.separator + "backup");
                  if (!backupDir.exists()) {
                     backupDir.mkdirs();
                  }

                  String timestamp = String.valueOf(System.currentTimeMillis());
                  File backupFile = new File(backupDir, "messages_it_" + timestamp + ".backup.yml");
                  Files.copy(langFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                  this.plugin.getLogger().info("Backed up corrupted file to: " + backupFile.getName());
                  langFile.delete();
                  this.plugin.saveResource(resourcePath, false);
                  if (langFile.exists()) {
                     this.plugin.getLogger().info("Successfully restored messages_it.yml version 8 from JAR");
                     updatedCount++;
                  } else {
                     this.plugin.getLogger().severe("Failed to restore messages_it.yml from JAR - file does not exist after restore!");
                     errorCount++;
                  }
               } else {
                  this.plugin.getLogger().fine("messages_it.yml has version " + currentVersion + ", no need to restore");
               }
            } catch (Exception var24) {
               try {
                  YamlConfiguration.loadConfiguration(langFile);
                  this.plugin.getLogger().fine("messages_it.yml can be loaded, skipping restore");
               } catch (Exception var23) {
                  this.plugin.getLogger().warning("messages_it.yml cannot be loaded, likely corrupted. Restoring from JAR...");

                  try {
                     File backupDirx = new File(this.plugin.getDataFolder(), "langs" + File.separator + "backup");
                     if (!backupDirx.exists()) {
                        backupDirx.mkdirs();
                     }

                     String timestamp = String.valueOf(System.currentTimeMillis());
                     File backupFile = new File(backupDirx, "messages_it_" + timestamp + ".backup.yml");
                     Files.copy(langFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                     this.plugin.getLogger().info("Backed up corrupted file to: " + backupFile.getName());
                     langFile.delete();
                     this.plugin.saveResource(resourcePath, false);
                     if (langFile.exists()) {
                        this.plugin.getLogger().info("Successfully restored messages_it.yml version 8 from JAR");
                        updatedCount++;
                     } else {
                        this.plugin.getLogger().severe("Failed to restore messages_it.yml from JAR - file does not exist after restore!");
                        errorCount++;
                     }
                  } catch (Exception var22) {
                     this.plugin.getLogger().severe("Failed to restore messages_it.yml from JAR: " + var22.getMessage());
                     var22.printStackTrace();
                     errorCount++;
                  }
               }
            }
         } else {
            boolean canLoadFile = false;
            boolean needsRestore = false;

            try {
               YamlConfiguration.loadConfiguration(langFile);
               canLoadFile = true;
            } catch (Exception var27) {
               String errorMessage = var27.getMessage();
               Throwable cause = var27.getCause();
               boolean isYamlError = false;
               if (errorMessage == null
                  || !errorMessage.contains("InvalidConfigurationException")
                     && !errorMessage.contains("ParserException")
                     && !errorMessage.contains("while parsing")) {
                  if (cause != null) {
                     String causeMessage = cause.getMessage();
                     if (causeMessage != null
                        && (
                           causeMessage.contains("InvalidConfigurationException")
                              || causeMessage.contains("ParserException")
                              || causeMessage.contains("while parsing")
                        )) {
                        isYamlError = true;
                     }
                  }
               } else {
                  isYamlError = true;
               }

               if (isYamlError) {
                  this.plugin.getLogger().warning("YAML syntax error detected in " + langFile.getName() + ". Attempting to fix...");
                  if (this.fixYamlSyntaxErrors(langFile, lang)) {
                     try {
                        YamlConfiguration.loadConfiguration(langFile);
                        canLoadFile = true;
                        this.plugin.getLogger().info("Successfully fixed " + langFile.getName());
                     } catch (Exception var21) {
                        needsRestore = true;
                     }
                  } else {
                     needsRestore = true;
                  }
               } else {
                  needsRestore = true;
               }

               if (needsRestore) {
                  this.plugin.getLogger().warning("Cannot load " + langFile.getName() + " due to YAML syntax error. Restoring from default JAR file...");
                  if (this.backupAndRestoreLanguageFile(langFile, resourcePath, lang)) {
                     try {
                        YamlConfiguration.loadConfiguration(langFile);
                        canLoadFile = true;
                        this.plugin.getLogger().info("Successfully restored " + langFile.getName() + " from default JAR file");
                     } catch (Exception var20) {
                        this.plugin
                           .getLogger()
                           .severe("Restored file " + langFile.getName() + " is still invalid! This indicates a problem with the default file in the JAR.");
                        errorCount++;
                     }
                  } else {
                     this.plugin.getLogger().severe("Failed to restore " + langFile.getName() + " from JAR!");
                     errorCount++;
                  }
               }
            }

            if (canLoadFile) {
               try {
                  this.configUpdater.updateConfig(langFile, resourcePath, "config-version", true);
                  updatedCount++;
               } catch (Exception var26) {
                  String errorMsg = var26.getMessage();
                  if (errorMsg != null
                     && (errorMsg.contains("InvalidConfigurationException") || errorMsg.contains("ParserException") || errorMsg.contains("while parsing"))) {
                     this.plugin.getLogger().warning("File " + langFile.getName() + " became corrupted during update. Restoring from default...");
                     if (this.backupAndRestoreLanguageFile(langFile, resourcePath, lang)) {
                        this.plugin.getLogger().info("Successfully restored " + langFile.getName() + " after update corruption");
                        updatedCount++;
                     } else {
                        errorCount++;
                     }
                  } else {
                     this.plugin.getLogger().warning("Failed to update " + langFile.getName() + ": " + var26.getMessage() + ". File is still usable.");
                     updatedCount++;
                  }
               }
            }
         }
      }

      if (savedCount > 0 || updatedCount > 0 || errorCount > 0) {
         this.plugin
            .getLogger()
            .info("Language files check: " + savedCount + " saved, " + updatedCount + " updated, " + skippedCount + " skipped, " + errorCount + " errors");
      }
   }

   public void saveDefaultLanguage(String lang) {
      File langsFolder = new File(this.plugin.getDataFolder(), "langs");
      if (!langsFolder.exists()) {
         langsFolder.mkdirs();
      }

      if ("zh".equals(lang)) {
         this.plugin
            .getLogger()
            .info("Language 'zh' is deprecated. Using 'zh_cn' (Simplified Chinese) instead. Please update your config.yml to use 'zh_cn' or 'zh_tw'.");
         lang = "zh_cn";
         this.language = "zh_cn";
      }

      if (this.messagesFile == null) {
         this.messagesFile = new File(langsFolder, "messages_" + lang + ".yml");
      }

      if (!this.messagesFile.exists()) {
         try {
            this.plugin.saveResource("langs/messages_" + lang + ".yml", false);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Could not save default messages_" + lang + ".yml: " + var6.getMessage());
            if (!lang.equals("en")) {
               try {
                  this.plugin.saveResource("langs/messages_en.yml", false);
                  this.messagesFile = new File(langsFolder, "messages_en.yml");
                  this.language = "en";
               } catch (Exception var5) {
                  this.plugin.getLogger().severe("Could not save any language file!");
               }
            }
         }
      }
   }

   public void reloadLanguage() {
      this.languageErrors.clear();
      File langsFolder = new File(this.plugin.getDataFolder(), "langs");
      String newLanguage = "en";
      if (this.config != null) {
         String langValue = this.config.getString("language", "en");
         newLanguage = langValue != null ? langValue.toLowerCase() : "en";
      } else {
         this.plugin.getLogger().warning("Config is null while reloading language, using default language: en");
      }

      if ("zh".equals(newLanguage)) {
         this.plugin
            .getLogger()
            .info("Language 'zh' is deprecated. Using 'zh_cn' (Simplified Chinese) instead. Please update your config.yml to use 'zh_cn' or 'zh_tw'.");
         newLanguage = "zh_cn";
      }

      if (!newLanguage.equals(this.language)) {
         this.language = newLanguage;
         this.messagesFile = null;
         this.messages = null;
      }

      if (this.messagesFile == null) {
         this.messagesFile = new File(langsFolder, "messages_" + this.language + ".yml");
      }

      String resourcePath = "langs/messages_" + this.language + ".yml";
      boolean fileCanBeLoaded = false;
      if (!this.messagesFile.exists()) {
         this.languageErrors.add("Language file 'messages_" + this.language + ".yml' does not exist! Attempting to create default...");
         this.saveDefaultLanguage(this.language);
         if (!this.messagesFile.exists()) {
            this.languageErrors.add("Failed to create language file 'messages_" + this.language + ".yml'! Falling back to English...");
            this.language = "en";
            this.messagesFile = new File(langsFolder, "messages_en.yml");
            resourcePath = "langs/messages_en.yml";
            if (!this.messagesFile.exists()) {
               this.languageErrors.add("English language file 'messages_en.yml' also does not exist! Some messages may be missing.");
            }
         }
      }

      if (this.messagesFile.exists()) {
         try {
            YamlConfiguration.loadConfiguration(this.messagesFile);
            fileCanBeLoaded = true;
         } catch (Exception var31) {
            String errorMessage = var31.getMessage();
            Throwable cause = var31.getCause();
            boolean isYamlError = false;
            if (errorMessage == null
               || !errorMessage.contains("InvalidConfigurationException")
                  && !errorMessage.contains("ParserException")
                  && !errorMessage.contains("while parsing")) {
               if (cause != null) {
                  String causeMessage = cause.getMessage();
                  if (causeMessage != null
                     && (
                        causeMessage.contains("InvalidConfigurationException")
                           || causeMessage.contains("ParserException")
                           || causeMessage.contains("while parsing")
                     )) {
                     isYamlError = true;
                  }
               }
            } else {
               isYamlError = true;
            }

            if (!isYamlError) {
               this.plugin.getLogger().warning("Cannot load language file. Restoring from default JAR file...");
               if (!this.backupAndRestoreLanguageFile(this.messagesFile, resourcePath, this.language)) {
                  this.languageErrors.add("Failed to restore language file. Using default from JAR.");
                  this.loadDefaultLanguageOnly(this.language);
                  return;
               }

               try {
                  YamlConfiguration.loadConfiguration(this.messagesFile);
                  fileCanBeLoaded = true;
               } catch (Exception var24) {
                  this.languageErrors.add("Restored file is still invalid! Using default from JAR.");
                  this.loadDefaultLanguageOnly(this.language);
                  return;
               }
            } else {
               this.plugin.getLogger().warning("YAML syntax error detected in language file 'messages_" + this.language + ".yml'. Attempting to auto-fix...");
               if (this.fixYamlSyntaxErrors(this.messagesFile, this.language)) {
                  try {
                     YamlConfiguration.loadConfiguration(this.messagesFile);
                     fileCanBeLoaded = true;
                     this.plugin.getLogger().info("Successfully fixed language file 'messages_" + this.language + ".yml'");
                  } catch (Exception var30) {
                     this.plugin.getLogger().warning("Failed to fix language file. Restoring from default JAR file...");
                     if (!this.backupAndRestoreLanguageFile(this.messagesFile, resourcePath, this.language)) {
                        this.languageErrors.add("Failed to restore language file. Using default from JAR.");
                        this.loadDefaultLanguageOnly(this.language);
                        return;
                     }

                     try {
                        YamlConfiguration.loadConfiguration(this.messagesFile);
                        fileCanBeLoaded = true;
                     } catch (Exception var26) {
                        this.languageErrors.add("Restored file is still invalid! Using default from JAR.");
                        this.loadDefaultLanguageOnly(this.language);
                        return;
                     }
                  }
               } else {
                  this.plugin.getLogger().warning("Auto-fix failed. Restoring from default JAR file...");
                  if (!this.backupAndRestoreLanguageFile(this.messagesFile, resourcePath, this.language)) {
                     this.languageErrors.add("Failed to restore language file. Using default from JAR.");
                     this.loadDefaultLanguageOnly(this.language);
                     return;
                  }

                  try {
                     YamlConfiguration.loadConfiguration(this.messagesFile);
                     fileCanBeLoaded = true;
                  } catch (Exception var25) {
                     this.languageErrors.add("Restored file is still invalid! Using default from JAR.");
                     this.loadDefaultLanguageOnly(this.language);
                     return;
                  }
               }
            }
         }
      }

      if (fileCanBeLoaded && this.messagesFile.exists()) {
         try {
            this.configUpdater.updateConfig(this.messagesFile, resourcePath, "config-version", true);
         } catch (Exception var29) {
            String errorMsg = var29.getMessage();
            if (errorMsg != null
               && (errorMsg.contains("InvalidConfigurationException") || errorMsg.contains("ParserException") || errorMsg.contains("while parsing"))) {
               this.plugin.getLogger().warning("File became corrupted during update. Restoring from default...");
               if (!this.backupAndRestoreLanguageFile(this.messagesFile, resourcePath, this.language)) {
                  this.languageErrors.add("Failed to restore after update corruption. Using default from JAR.");
                  this.loadDefaultLanguageOnly(this.language);
                  return;
               }

               fileCanBeLoaded = true;
            }
         }
      }

      try {
         this.messages = YamlConfiguration.loadConfiguration(this.messagesFile);
         InputStream defConfigStream = null;

         try {
            defConfigStream = this.plugin.getResource(resourcePath);
            if (defConfigStream != null) {
               YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
               this.messages.setDefaults(defConfig);
            }
         } finally {
            if (defConfigStream != null) {
               try {
                  defConfigStream.close();
               } catch (Exception var23) {
               }
            }
         }
      } catch (Exception var28) {
         this.languageErrors.add("Failed to load language file 'messages_" + this.language + ".yml': " + var28.getMessage());
         this.plugin.getLogger().warning("Failed to load language file. Using default from JAR...");
         this.loadDefaultLanguageOnly(this.language);
      }
   }

   public void validateConfigs() {
      this.validateConfig();
      this.validateGUIConfig();
      this.validateLanguage();
      this.printValidationErrors();
   }

   private void validateConfig() {
      if (this.config == null) {
         this.configErrors.add("Config file is null! Cannot validate.");
      } else {
         String lang = this.config.getString("language", "en");
         if (lang == null || lang.isEmpty()) {
            this.configErrors.add("Language setting is missing or empty! Using default 'en'.");
         } else if (!lang.equalsIgnoreCase("vi")
            && !lang.equalsIgnoreCase("en")
            && !lang.equalsIgnoreCase("fr")
            && !lang.equalsIgnoreCase("nl")
            && !lang.equalsIgnoreCase("ru")
            && !lang.equalsIgnoreCase("zh")
            && !lang.equalsIgnoreCase("zh_cn")
            && !lang.equalsIgnoreCase("zh_tw")
            && !lang.equalsIgnoreCase("es")
            && !lang.equalsIgnoreCase("de")
            && !lang.equalsIgnoreCase("ar")
            && !lang.equalsIgnoreCase("it")) {
            this.configErrors
               .add(
                  "Invalid language setting '"
                     + lang
                     + "'! Valid options are: vi, en, fr, nl, ru, zh_cn, zh_tw, es, de, ar, it. Note: 'zh' is deprecated, use 'zh_cn' or 'zh_tw' instead. Using default 'en'."
               );
         }

         String dbType = this.config.getString("database.type", "SQLITE");
         if (dbType != null && !dbType.isEmpty()) {
            String upperType = dbType.toUpperCase();
            if (!upperType.equals("SQLITE") && !upperType.equals("MYSQL")) {
               this.configErrors.add("Invalid database.type '" + dbType + "'! Valid options are: SQLITE, MYSQL. Using default: SQLITE");
            }
         } else {
            this.configErrors.add("database.type is missing! Using default: SQLITE");
         }

         if (this.config.getString("database.type", "SQLITE").equalsIgnoreCase("MYSQL")) {
            String host = this.config.getString("database.mysql.host", "");
            if (host == null || host.isEmpty()) {
               this.configErrors.add("database.mysql.host is missing or empty! MySQL connection will fail.");
            }

            int port = this.config.getInt("database.mysql.port", -1);
            if (port < 1 || port > 65535) {
               this.configErrors.add("database.mysql.port is invalid! Must be between 1 and 65535. Current value: " + port);
            }

            String database = this.config.getString("database.mysql.database", "");
            if (database == null || database.isEmpty()) {
               this.configErrors.add("database.mysql.database is missing or empty! MySQL connection will fail.");
            }

            String username = this.config.getString("database.mysql.username", "");
            if (username == null || username.isEmpty()) {
               this.configErrors.add("database.mysql.username is missing or empty! MySQL connection will fail.");
            }
         }

         List<String> commandAliases = this.config.getStringList("command_aliases");
         if (commandAliases != null && !commandAliases.isEmpty()) {
            for (String alias : commandAliases) {
               if (alias == null || alias.trim().isEmpty()) {
                  this.configErrors.add("command_aliases contains an empty or null alias! Empty aliases will be ignored.");
               } else if (!alias.matches("^[a-zA-Z0-9_]+$")) {
                  this.configErrors.add("Invalid command alias '" + alias + "'! Aliases must contain only letters, numbers, and underscores.");
               }
            }

            if (commandAliases.size() > 10) {
               this.configErrors.add("command_aliases has more than 10 aliases! Too many aliases may cause issues. Consider reducing the number.");
            }
         } else {
            this.plugin.getLogger().info("command_aliases is empty or not set. Using default aliases from @CommandAlias annotation.");
         }

         boolean discordEnabled = this.config.getBoolean("discord.webhook.enabled", false);
         if (discordEnabled) {
            String webhookUrl = this.config.getString("discord.webhook.url", "");
            if (webhookUrl == null || webhookUrl.isEmpty()) {
               this.configErrors.add("Discord webhook is enabled but URL is missing or empty! Please set discord.webhook.url in config.yml");
            } else if (webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN")) {
               this.configErrors
                  .add("Discord webhook is enabled but URL is still set to default placeholder! Please set a valid Discord webhook URL in config.yml");
            } else if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") && !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
               this.configErrors
                  .add("Invalid Discord webhook URL format! URL must start with https://discord.com/api/webhooks/ or https://discordapp.com/api/webhooks/");
            }

            double minAmount = this.config.getDouble("discord.webhook.min-amount", 0.0);
            if (minAmount < 0.0) {
               this.configErrors.add("discord.webhook.min-amount must be >= 0! Current value: " + minAmount + ". Using default: 0");
            }

            if (this.config.getBoolean("discord.message.embed.enabled", true)) {
               int r = this.config.getInt("discord.message.embed.color.r", -1);
               int g = this.config.getInt("discord.message.embed.color.g", -1);
               int b = this.config.getInt("discord.message.embed.color.b", -1);
               if (r < 0 || r > 255) {
                  this.configErrors.add("discord.message.embed.color.r must be between 0 and 255! Current value: " + r + ". Using default: 0");
               }

               if (g < 0 || g > 255) {
                  this.configErrors.add("discord.message.embed.color.g must be between 0 and 255! Current value: " + g + ". Using default: 255");
               }

               if (b < 0 || b > 255) {
                  this.configErrors.add("discord.message.embed.color.b must be between 0 and 255! Current value: " + b + ". Using default: 0");
               }
            }
         }
      }
   }

   private void validateGUIConfig() {
      if (this.guiConfig == null) {
         this.guiErrors.add("GUI config file is null! Cannot validate.");
      } else {
         int listGuiSize = this.guiConfig.getInt("list-gui.size", -1);
         if (listGuiSize <= 0 || listGuiSize % 9 != 0 || listGuiSize > 54) {
            this.guiErrors.add("list-gui.size must be a positive multiple of 9 and not exceed 54! Current value: " + listGuiSize + ". Using default: 45");
         }

         int itemsPerPage = this.guiConfig.getInt("list-gui.items-per-page", -1);
         if (itemsPerPage <= 0) {
            this.guiErrors.add("list-gui.items-per-page must be greater than 0! Current value: " + itemsPerPage + ". Using default: 21");
         }

         String fillerMaterial = this.guiConfig.getString("list-gui.filler.material", "");
         if (fillerMaterial != null && !fillerMaterial.isEmpty()) {
            this.validateAndFixMaterial("list-gui.filler.material", fillerMaterial, "BLACK_STAINED_GLASS_PANE");
         }

         String statsMaterial = this.guiConfig.getString("list-gui.stats.material", "");
         if (statsMaterial != null && !statsMaterial.isEmpty()) {
            this.validateAndFixMaterial("list-gui.stats.material", statsMaterial, "BOOK");
         }

         int statsSlot = this.guiConfig.getInt("list-gui.stats.slot", -1);
         if (statsSlot < 0 || statsSlot >= listGuiSize) {
            this.guiErrors.add("list-gui.stats.slot (" + statsSlot + ") must be between 0 and " + (listGuiSize - 1) + "! Using default: 40");
         }

         int prevSlot = this.guiConfig.getInt("list-gui.navigation.previous.slot", -1);
         if (prevSlot < 0 || prevSlot >= listGuiSize) {
            this.guiErrors.add("list-gui.navigation.previous.slot (" + prevSlot + ") must be between 0 and " + (listGuiSize - 1) + "! Using default: 36");
         }

         int nextSlot = this.guiConfig.getInt("list-gui.navigation.next.slot", -1);
         if (nextSlot < 0 || nextSlot >= listGuiSize) {
            this.guiErrors.add("list-gui.navigation.next.slot (" + nextSlot + ") must be between 0 and " + (listGuiSize - 1) + "! Using default: 44");
         }

         String prevMaterial = this.guiConfig.getString("list-gui.navigation.previous.material", "");
         if (prevMaterial != null && !prevMaterial.isEmpty()) {
            this.validateAndFixMaterial("list-gui.navigation.previous.material", prevMaterial, "ARROW");
         }

         String nextMaterial = this.guiConfig.getString("list-gui.navigation.next.material", "");
         if (nextMaterial != null && !nextMaterial.isEmpty()) {
            this.validateAndFixMaterial("list-gui.navigation.next.material", nextMaterial, "ARROW");
         }

         int rollGuiSize = this.guiConfig.getInt("coinflip-gui.size", -1);
         if (rollGuiSize <= 0 || rollGuiSize % 9 != 0 || rollGuiSize > 54) {
            this.guiErrors.add("coinflip-gui.size must be a positive multiple of 9 and not exceed 54! Current value: " + rollGuiSize + ". Using default: 27");
         }

         String gameItemMaterial = this.guiConfig.getString("game-item.material", "");
         if (gameItemMaterial != null && !gameItemMaterial.isEmpty()) {
            this.validateAndFixMaterial("game-item.material", gameItemMaterial, "PLAYER_HEAD");
         }

         String statsItemMaterial = this.guiConfig.getString("stats-item.material", "");
         if (statsItemMaterial != null && !statsItemMaterial.isEmpty()) {
            this.validateAndFixMaterial("stats-item.material", statsItemMaterial, "BOOK");
         }

         int refreshInterval = this.guiConfig.getInt("list-gui.refresh-interval", -1);
         if (refreshInterval < 1) {
            this.guiErrors.add("list-gui.refresh-interval must be at least 1 tick! Current value: " + refreshInterval + ". Using default: 20");
         }
      }
   }

   private boolean validateAndFixMaterialsInFile(FileConfiguration fileConfig, String fileName) {
      boolean modified = false;

      for (String key : fileConfig.getKeys(true)) {
         if (!fileConfig.isConfigurationSection(key) && key.endsWith(".material")) {
            String materialValue = fileConfig.getString(key);
            if (materialValue != null && !materialValue.isEmpty()) {
               if (!MaterialHelper.isValidMaterial(materialValue)) {
                  String defaultMat = this.getDefaultMaterialForPath(key);
                  if (defaultMat != null && MaterialHelper.isValidMaterial(defaultMat)) {
                     fileConfig.set(key, defaultMat);
                     modified = true;
                     this.plugin
                        .getLogger()
                        .warning(
                           "[Auto-Fix] Invalid material '"
                              + materialValue
                              + "' at "
                              + key
                              + " in "
                              + fileName
                              + "! Using default: "
                              + defaultMat
                              + " (Server version: "
                              + VersionDetector.getServerVersion()
                              + ")"
                        );
                     this.guiErrors.add("Invalid material '" + materialValue + "' at " + key + " in " + fileName + "! Using default: " + defaultMat);
                  } else {
                     this.plugin
                        .getLogger()
                        .warning(
                           "[Config Error] Invalid material '"
                              + materialValue
                              + "' at "
                              + key
                              + " in "
                              + fileName
                              + " (Server version: "
                              + VersionDetector.getServerVersion()
                              + "). Material may not work correctly."
                        );
                     this.guiErrors.add("Invalid material '" + materialValue + "' at " + key + " in " + fileName + "! Material may not work correctly.");
                  }
               } else if (VersionDetector.isLegacy()
                  && !materialValue.toUpperCase().endsWith("_STAINED_GLASS_PANE")
                  && materialValue.toUpperCase().equals("PLAYER_HEAD")) {
               }
            }
         }
      }

      return modified;
   }

   private String getDefaultMaterialForPath(String configPath) {
      if (configPath == null) {
         return null;
      } else {
         String lower = configPath.toLowerCase();
         if (lower.contains("filler") || lower.contains("border")) {
            return "BLACK_STAINED_GLASS_PANE";
         } else if (lower.contains("player_head") || lower.contains("game-item") || lower.contains("head")) {
            return "PLAYER_HEAD";
         } else if (lower.contains("stats")) {
            return "BOOK";
         } else {
            return !lower.contains("navigation") && !lower.contains("previous") && !lower.contains("next") ? null : "ARROW";
         }
      }
   }

   private String validateAndFixMaterial(String configPath, String materialName, String defaultMaterial) {
      if (materialName == null || materialName.isEmpty()) {
         return defaultMaterial;
      } else if (MaterialHelper.isValidMaterial(materialName)) {
         return materialName;
      } else {
         String converted = MaterialHelper.convertMaterialForLegacy(materialName);
         if (converted != null && MaterialHelper.isValidMaterial(converted)) {
            this.plugin
               .getLogger()
               .info(
                  "[Auto-Fix] Converted incompatible material '"
                     + materialName
                     + "' to '"
                     + converted
                     + "' at "
                     + configPath
                     + " for version "
                     + VersionDetector.getServerVersion()
               );
            this.guiConfig.set(configPath, converted);
            this.guiErrors
               .add("Invalid material '" + materialName + "' at " + configPath + "! Auto-converted to '" + converted + "' for version compatibility.");
            return converted;
         } else {
            this.plugin.getLogger().warning("[Auto-Fix] Invalid material '" + materialName + "' at " + configPath + "! Using default: " + defaultMaterial);
            this.guiConfig.set(configPath, defaultMaterial);
            this.guiErrors.add("Invalid material '" + materialName + "' at " + configPath + "! Using default: " + defaultMaterial);
            return defaultMaterial;
         }
      }
   }

   public void saveDefaultSoundsConfig() {
      if (this.soundsFile == null) {
         this.soundsFile = new File(this.plugin.getDataFolder(), "sounds.yml");
      }

      if (!this.soundsFile.exists()) {
         try {
            this.plugin.saveResource("sounds.yml", false);
            this.plugin.getLogger().info("Saved default sounds.yml file");
         } catch (Exception var2) {
            this.plugin.getLogger().warning("Could not save default sounds.yml: " + var2.getMessage());
         }
      }
   }

   public void reloadSoundsConfig() {
      this.soundsErrors.clear();
      if (this.soundsFile == null) {
         this.soundsFile = new File(this.plugin.getDataFolder(), "sounds.yml");
      }

      if (!this.soundsFile.exists()) {
         this.soundsErrors.add("Sounds config file 'sounds.yml' does not exist! Creating default...");
         this.saveDefaultSoundsConfig();
      }

      try {
         this.configUpdater.updateConfig(this.soundsFile, "sounds.yml", "config-version", true);
      } catch (Exception var11) {
      }

      try {
         this.soundsConfig = YamlConfiguration.loadConfiguration(this.soundsFile);
         InputStream defConfigStream = null;

         try {
            defConfigStream = this.plugin.getResource("sounds.yml");
            if (defConfigStream != null) {
               YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
               this.soundsConfig.setDefaults(defConfig);
            }
         } finally {
            if (defConfigStream != null) {
               try {
                  defConfigStream.close();
               } catch (Exception var10) {
               }
            }
         }
      } catch (Exception var13) {
         this.soundsErrors.add("Failed to load sounds.yml: " + var13.getMessage());
         this.plugin.getLogger().severe("Failed to load sounds.yml: " + var13.getMessage());
         var13.printStackTrace();
      }
   }

   private void validateLanguage() {
      if (this.messages == null) {
         this.languageErrors.add("Language file is null! Cannot validate.");
      } else {
         String[] requiredKeys = new String[]{
            "prefix",
            "no-perms",
            "command.no-bet",
            "command.bet-cancelled",
            "command.usage",
            "command.invalid-currency",
            "command.invalid-amount",
            "command.min-bid",
            "command.max-bid",
            "command.not-enough-money",
            "command.already-bet",
            "command.bet-created-money",
            "command.broadcast-created",
            "game.cannot-play-self",
            "game.player-not-found",
            "game.winner",
            "game.loser",
            "game.broadcast-result",
            "titles.game-win-title",
            "titles.game-win-subtitle",
            "titles.game-lose-title",
            "titles.game-lose-subtitle"
         };

         for (String key : requiredKeys) {
            if (!this.messages.contains(key)) {
               this.languageErrors.add("Missing required message key: " + key);
            } else {
               String value = this.messages.getString(key);
               if (value == null || value.isEmpty()) {
                  this.languageErrors.add("Message key '" + key + "' is empty!");
               }
            }
         }
      }
   }

   private void printValidationErrors() {
      if (!this.configErrors.isEmpty() || !this.guiErrors.isEmpty() || !this.soundsErrors.isEmpty() || !this.languageErrors.isEmpty()) {
         this.plugin.getLogger().warning("==========================================");
         this.plugin.getLogger().warning("CONFIGURATION VALIDATION ERRORS DETECTED");
         this.plugin.getLogger().warning("==========================================");
         if (!this.configErrors.isEmpty()) {
            this.plugin.getLogger().warning("Config.yml Errors (" + this.configErrors.size() + "):");

            for (String error : this.configErrors) {
               this.plugin.getLogger().warning("  [ERROR] " + error);
            }

            this.plugin.getLogger().warning("");
         }

         if (!this.guiErrors.isEmpty()) {
            this.plugin.getLogger().warning("GUI.yml Errors (" + this.guiErrors.size() + "):");

            for (String error : this.guiErrors) {
               this.plugin.getLogger().warning("  [ERROR] " + error);
            }

            this.plugin.getLogger().warning("");
         }

         if (!this.soundsErrors.isEmpty()) {
            this.plugin.getLogger().warning("Sounds.yml Errors (" + this.soundsErrors.size() + "):");

            for (String error : this.soundsErrors) {
               this.plugin.getLogger().warning("  [ERROR] " + error);
            }

            this.plugin.getLogger().warning("");
         }

         if (!this.languageErrors.isEmpty()) {
            this.plugin.getLogger().warning("Language File Errors (" + this.languageErrors.size() + "):");

            for (String error : this.languageErrors) {
               this.plugin.getLogger().warning("  [ERROR] " + error);
            }

            this.plugin.getLogger().warning("");
         }

         this.plugin.getLogger().warning("Please fix these errors in your configuration files.");
         this.plugin.getLogger().warning("The plugin will continue to run with default values where applicable.");
         this.plugin.getLogger().warning("For help, contact support to the author on Discord.");
         this.plugin.getLogger().warning("==========================================");
      }
   }

   public FileConfiguration getConfig() {
      if (this.config == null) {
         this.reloadConfig();
      }

      return this.config;
   }

   public FileConfiguration getGUIConfig() {
      if (this.guiConfig == null) {
         this.reloadGUIConfig();
      }

      return this.guiConfig;
   }

   public FileConfiguration getMessages() {
      if (this.messages == null) {
         this.reloadLanguage();
      }

      return this.messages;
   }

   public FileConfiguration getSoundsConfig() {
      if (this.soundsConfig == null) {
         this.reloadSoundsConfig();
      }

      return this.soundsConfig;
   }

   public String getMessage(String key) {
      String message = this.getMessages().getString(key, key);
      if ("prefix".equalsIgnoreCase(key) && message != null && !message.isEmpty() && !this.containsResetFormatting(message)) {
         message = message + "<reset>";
      }

      return message;
   }

   public String getGUIConfigString(String key) {
      return this.getGUIConfig().getString(key, key);
   }

   public boolean isDebugEnabled() {
      if (this.config == null) {
         return false;
      } else {
         return this.config.contains("debug.enabled") ? this.config.getBoolean("debug.enabled", false) : this.config.getBoolean("debug", false);
      }
   }

   public void reloadDebugConfig() {
   }

   public String getMessageLegacy(String key) {
      String message = this.getMessages().getString(key, key);
      return this.translateAlternateColorCodes('&', message);
   }

   private boolean containsResetFormatting(String message) {
      String lower = message.toLowerCase();
      return lower.contains("<reset>") || lower.contains("&r") || message.contains("§r");
   }

   private String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
      if (textToTranslate == null) {
         return null;
      } else {
         char[] chars = textToTranslate.toCharArray();

         for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar && this.isLegacyColorCode(chars[i + 1])) {
               chars[i] = 167;
               chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
         }

         return new String(chars);
      }
   }

   private boolean isLegacyColorCode(char c) {
      char lower = Character.toLowerCase(c);
      return lower >= '0' && lower <= '9' || lower >= 'a' && lower <= 'f' || lower >= 'k' && lower <= 'o' || lower == 'r' || lower == 'x';
   }

   public void reload() {
      this.plugin.getLogger().info("Reloading configuration files...");
      this.reloadConfig();
      if (this.plugin.getDebugManager() != null) {
         this.plugin.getDebugManager().loadConfig();
         this.plugin.getDebugManager().info(DebugManager.Category.CONFIG, "Configuration reloaded, debug settings updated");
      }

      this.saveAllDefaultLanguages();
      String newLanguage = "en";
      if (this.config != null) {
         newLanguage = this.config.getString("language", "en").toLowerCase();
      } else {
         this.plugin.getLogger().warning("Config is null while reloading, using default language: en");
      }

      if (!newLanguage.equals(this.language)) {
         this.plugin.getLogger().info("Language changed from '" + this.language + "' to '" + newLanguage + "'");
         this.language = newLanguage;
         this.messagesFile = null;
         this.messages = null;
      }

      this.reloadGUIConfig();
      this.reloadSoundsConfig();
      this.reloadLanguage();
      if (this.plugin.getTransactionLogger() != null) {
         this.plugin.getTransactionLogger().reloadConfig();
      }

      if (this.plugin.getExploitDetector() != null) {
         this.plugin.getExploitDetector().reloadConfig();
      }

      this.validateConfigs();
      this.plugin
         .getLogger()
         .info(
            "Configuration reloaded. "
               + (this.configErrors.size() + this.guiErrors.size() + this.soundsErrors.size() + this.languageErrors.size())
               + " error(s) found. Check console for details."
         );
   }

   private boolean fixYamlSyntaxErrors(File file, String language) {
      if (!file.exists()) {
         return false;
      } else {
         try {
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            boolean modified = false;
            int lineNumber = 0;

            String line;
            while ((line = reader.readLine()) != null) {
               lineNumber++;
               String fixedLine = line;
               Pattern singleQuotePattern = Pattern.compile("^(\\s+)([^:]+):\\s*'([^']*'[^']*)'\\s*$");
               Matcher singleQuoteMatcher = singleQuotePattern.matcher(line);
               if (singleQuoteMatcher.find()) {
                  String indent = singleQuoteMatcher.group(1);
                  String key = singleQuoteMatcher.group(2).trim();
                  String value = singleQuoteMatcher.group(3);
                  String escapedValue = value.replace("\"", "\\\"");
                  fixedLine = indent + key + ": \"" + escapedValue + "\"";
                  modified = true;
                  this.plugin
                     .getLogger()
                     .warning(
                        "Auto-fixed YAML syntax error at line "
                           + lineNumber
                           + " in messages_"
                           + language
                           + ".yml (key: "
                           + key
                           + "). Changed single quotes to double quotes."
                     );
               } else if (line.matches(".*:\\s*'.*'.*'.*")) {
                  int colonIndex = line.indexOf(58);
                  if (colonIndex > 0) {
                     String beforeColon = line.substring(0, colonIndex + 1);
                     String afterColon = line.substring(colonIndex + 1).trim();
                     if (afterColon.startsWith("'")) {
                        StringBuilder content = new StringBuilder();
                        boolean inQuotes = true;
                        boolean escapeNext = false;

                        for (int i = 1; i < afterColon.length(); i++) {
                           char c = afterColon.charAt(i);
                           if (escapeNext) {
                              content.append(c);
                              escapeNext = false;
                           } else if (c == '\\') {
                              escapeNext = true;
                              content.append(c);
                           } else if (c == '\'' && inQuotes) {
                              if (i + 1 < afterColon.length() && afterColon.charAt(i + 1) == ' ') {
                                 break;
                              }

                              content.append(c);
                           } else {
                              content.append(c);
                           }
                        }

                        String escapedContent = content.toString().replace("\"", "\\\"");
                        fixedLine = beforeColon + " \"" + escapedContent + "\"";
                        modified = true;
                        this.plugin
                           .getLogger()
                           .warning(
                              "Auto-fixed complex YAML syntax error at line "
                                 + lineNumber
                                 + " in messages_"
                                 + language
                                 + ".yml. Changed single quotes to double quotes."
                           );
                     }
                  }
               }

               lines.add(fixedLine);
            }

            reader.close();
            if (modified) {
               BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));

               for (int ix = 0; ix < lines.size(); ix++) {
                  writer.write(lines.get(ix));
                  if (ix < lines.size() - 1) {
                     writer.newLine();
                  }
               }

               writer.close();
               return true;
            } else {
               return false;
            }
         } catch (IOException var19) {
            this.plugin.getLogger().warning("Failed to auto-fix YAML syntax errors in " + file.getName() + ": " + var19.getMessage());
            return false;
         }
      }
   }

   private boolean backupAndRestoreLanguageFile(File langFile, String resourcePath, String lang) {
      if (!langFile.exists()) {
         return false;
      } else {
         try {
            File backupDir = new File(this.plugin.getDataFolder(), "langs" + File.separator + "backup");
            if (!backupDir.exists()) {
               backupDir.mkdirs();
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            File backupFile = new File(backupDir, "messages_" + lang + "_" + timestamp + ".backup.yml");
            Files.copy(langFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            this.plugin.getLogger().info("Backed up corrupted file to: " + backupFile.getName());
            langFile.delete();
            this.plugin.saveResource(resourcePath, false);
            if (langFile.exists()) {
               this.plugin.getLogger().info("Successfully restored " + langFile.getName() + " from default JAR file");
               return true;
            } else {
               this.plugin.getLogger().warning("Failed to restore " + langFile.getName() + " from JAR");
               return false;
            }
         } catch (Exception var7) {
            this.plugin.getLogger().warning("Failed to backup and restore " + langFile.getName() + ": " + var7.getMessage());
            return false;
         }
      }
   }

   private void loadDefaultLanguageOnly(String language) {
      try {
         InputStream defConfigStream = this.plugin.getResource("langs/messages_" + language + ".yml");
         if (defConfigStream != null) {
            this.messages = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            this.plugin.getLogger().info("Loaded default language file from JAR for language: " + language);
         } else {
            defConfigStream = this.plugin.getResource("langs/messages_en.yml");
            if (defConfigStream != null) {
               this.messages = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
               this.plugin.getLogger().warning("Language file not found, using English as fallback");
            } else {
               this.plugin.getLogger().severe("Could not load any language file! Some messages may be missing.");
               this.messages = new YamlConfiguration();
            }
         }

         if (defConfigStream != null) {
            defConfigStream.close();
         }
      } catch (Exception var3) {
         this.plugin.getLogger().severe("Failed to load default language file: " + var3.getMessage());
         this.messages = new YamlConfiguration();
      }
   }
}
