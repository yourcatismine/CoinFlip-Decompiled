package com.kstudio.ultracoinflip.config;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigUpdater {
   private final KStudio plugin;
   private static Boolean boostedYamlAvailable = null;
   private static Class<?> yamlDocumentClass = null;
   private static Class<?> generalSettingsClass = null;
   private static Class<?> loaderSettingsClass = null;
   private static Class<?> dumperSettingsClass = null;
   private static Class<?> updaterSettingsClass = null;
   private static Class<?> basicVersioningClass = null;
   private static Class<?> settingsInterface = null;

   public ConfigUpdater(KStudio plugin) {
      this.plugin = plugin;
      this.checkBoostedYamlAvailability();
   }

   private void checkBoostedYamlAvailability() {
      if (boostedYamlAvailable == null) {
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         if (classLoader == null) {
            classLoader = this.getClass().getClassLoader();
         }

         try {
            try {
               yamlDocumentClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.YamlDocument", true, classLoader);
               generalSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.general.GeneralSettings", true, classLoader);
               loaderSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.loader.LoaderSettings", true, classLoader);
               dumperSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.dumper.DumperSettings", true, classLoader);
               updaterSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.updater.UpdaterSettings", true, classLoader);

               try {
                  settingsInterface = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.Settings", true, classLoader);
               } catch (ClassNotFoundException var15) {
                  settingsInterface = generalSettingsClass.getInterfaces()[0];
               }

               try {
                  basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.dvs.versioning.BasicVersioning", true, classLoader);
               } catch (ClassNotFoundException var14) {
                  basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.updater.updater.BasicVersioning", true, classLoader);
               }

               boostedYamlAvailable = true;
               this.debugLog("BoostedYAML found in relocated package");
            } catch (ClassNotFoundException var16) {
               try {
                  yamlDocumentClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.YamlDocument", true, classLoader);
                  generalSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.general.GeneralSettings", true, classLoader);
                  loaderSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.loader.LoaderSettings", true, classLoader);
                  dumperSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.dumper.DumperSettings", true, classLoader);
                  updaterSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.updater.UpdaterSettings", true, classLoader);

                  try {
                     settingsInterface = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.Settings", true, classLoader);
                  } catch (ClassNotFoundException var12) {
                     settingsInterface = generalSettingsClass.getInterfaces()[0];
                  }

                  try {
                     basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.dvs.versioning.BasicVersioning", true, classLoader);
                  } catch (ClassNotFoundException var11) {
                     basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.updater.updater.BasicVersioning", true, classLoader);
                  }

                  boostedYamlAvailable = true;
                  this.debugLog("BoostedYAML found in original package");
               } catch (ClassNotFoundException var13) {
                  try {
                     yamlDocumentClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.YamlDocument");
                     generalSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.general.GeneralSettings");
                     loaderSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.loader.LoaderSettings");
                     dumperSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.dumper.DumperSettings");
                     updaterSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.updater.UpdaterSettings");

                     try {
                        settingsInterface = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.Settings");
                     } catch (ClassNotFoundException var9) {
                        settingsInterface = generalSettingsClass.getInterfaces()[0];
                     }

                     try {
                        basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.dvs.versioning.BasicVersioning");
                     } catch (ClassNotFoundException var8) {
                        basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.updater.updater.BasicVersioning");
                     }

                     boostedYamlAvailable = true;
                     this.debugLog("BoostedYAML found in relocated package (fallback)");
                  } catch (ClassNotFoundException var10) {
                     yamlDocumentClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.YamlDocument");
                     generalSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.general.GeneralSettings");
                     loaderSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.loader.LoaderSettings");
                     dumperSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.dumper.DumperSettings");
                     updaterSettingsClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.updater.UpdaterSettings");

                     try {
                        settingsInterface = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.settings.Settings");
                     } catch (ClassNotFoundException var7) {
                        settingsInterface = generalSettingsClass.getInterfaces()[0];
                     }

                     try {
                        basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.dvs.versioning.BasicVersioning");
                     } catch (ClassNotFoundException var6) {
                        basicVersioningClass = Class.forName("com.kstudio.ultracoinflip.libs.boostedyaml.updater.updater.BasicVersioning");
                     }

                     boostedYamlAvailable = true;
                     this.debugLog("BoostedYAML found in original package (fallback)");
                  }
               }
            }
         } catch (ClassNotFoundException var17) {
            boostedYamlAvailable = false;
            this.debugLog("BoostedYAML not found in classpath - config updates will be disabled");
            this.debugLog("ClassNotFoundException details: " + var17.getMessage());
         }
      }
   }

   private boolean isDebugEnabled() {
      try {
         return this.plugin.getDebugManager() != null
            ? this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CONFIG)
            : this.plugin.getConfigManager() != null && this.plugin.getConfigManager().isDebugEnabled();
      } catch (Exception var2) {
         return false;
      }
   }

   private void debugLog(String message) {
      if (this.isDebugEnabled()) {
         try {
            if (this.plugin.getDebugManager() != null) {
               this.plugin.getDebugManager().info(DebugManager.Category.CONFIG, message);
               return;
            }

            this.plugin.getLogger().info("[DEBUG] " + message);
         } catch (Exception var3) {
         }
      }
   }

   public boolean isBoostedYamlAvailable() {
      return boostedYamlAvailable != null && boostedYamlAvailable;
   }

   public Object updateConfig(File configFile, String defaultResourcePath, String versionRoute) {
      return this.updateConfig(configFile, defaultResourcePath, versionRoute, true);
   }

   public Object updateConfig(File configFile, String defaultResourcePath, String versionRoute, boolean createIfNotExists) {
      if (!this.isBoostedYamlAvailable()) {
         if (this.isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] BoostedYAML not available, skipping config update for: " + configFile.getName());
         }

         return null;
      } else {
         this.debugLog("Updating config file: " + configFile.getName());
         this.debugLog("Config file path: " + configFile.getAbsolutePath());
         this.debugLog("Default resource path: " + defaultResourcePath);
         this.debugLog("Version route: " + versionRoute);
         InputStream defaultStream = null;
         boolean isLanguageFile = defaultResourcePath != null && defaultResourcePath.contains("langs/messages_");

         try {
            if (isLanguageFile && configFile.exists()) {
               try {
                  YamlConfiguration.loadConfiguration(configFile);
               } catch (Exception var52) {
                  String errorMessage = var52.getMessage();
                  Throwable cause = var52.getCause();
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
                     if (this.isDebugEnabled()) {
                        this.plugin
                           .getLogger()
                           .info("[DEBUG] YAML syntax error detected in " + configFile.getName() + " before update. ConfigManager will handle it.");
                     }

                     return null;
                  }
               }
            }

            defaultStream = this.plugin.getResource(defaultResourcePath);
            if (defaultStream != null) {
               if (!configFile.exists() && createIfNotExists) {
                  File parentDir = configFile.getParentFile();
                  if (parentDir != null && !parentDir.exists()) {
                     parentDir.mkdirs();
                  }
               }

               if (this.isDebugEnabled()) {
                  this.plugin.getLogger().info("[DEBUG] Creating YamlDocument...");
               }

               Object document = this.createYamlDocument(configFile, defaultStream, versionRoute);
               defaultStream = null;
               if (document != null) {
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] YamlDocument created successfully");
                  }

                  Method updateMethod = yamlDocumentClass.getMethod("update");
                  updateMethod.invoke(document);
                  Method saveMethod = yamlDocumentClass.getMethod("save");
                  saveMethod.invoke(document);
                  Integer version = 1;
                  Integer oldVersion = null;

                  try {
                     if (configFile.exists()) {
                        try {
                           FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(configFile);
                           Object oldVersionObj = oldConfig.get(versionRoute);
                           if (oldVersionObj instanceof Integer) {
                              oldVersion = (Integer)oldVersionObj;
                           } else if (oldVersionObj instanceof Number) {
                              oldVersion = ((Number)oldVersionObj).intValue();
                           } else if (oldVersionObj instanceof String) {
                              try {
                                 oldVersion = Integer.parseInt((String)oldVersionObj);
                              } catch (NumberFormatException var44) {
                              }
                           }
                        } catch (Exception var45) {
                           String errorMessagex = var45.getMessage();
                           Throwable causex = var45.getCause();
                           boolean isYamlErrorx = false;
                           if (errorMessagex == null
                              || !errorMessagex.contains("InvalidConfigurationException")
                                 && !errorMessagex.contains("ParserException")
                                 && !errorMessagex.contains("while parsing")) {
                              if (causex != null) {
                                 String causeMessage = causex.getMessage();
                                 if (causeMessage != null
                                    && (
                                       causeMessage.contains("InvalidConfigurationException")
                                          || causeMessage.contains("ParserException")
                                          || causeMessage.contains("while parsing")
                                    )) {
                                    isYamlErrorx = true;
                                 }
                              }
                           } else {
                              isYamlErrorx = true;
                           }

                           if (isYamlErrorx) {
                              if (this.isDebugEnabled()) {
                                 this.plugin.getLogger().info("[DEBUG] Cannot read old version from " + configFile.getName() + " due to YAML syntax error");
                              }
                           } else if (this.isDebugEnabled()) {
                              this.plugin.getLogger().info("[DEBUG] Cannot read old version from " + configFile.getName() + ": " + var45.getMessage());
                           }
                        }
                     }

                     try {
                        Method getIntMethod = yamlDocumentClass.getMethod("getInt", String.class, int.class);
                        version = (Integer)getIntMethod.invoke(document, versionRoute, 1);
                     } catch (NoSuchMethodException var48) {
                        try {
                           Method getIntMethodx = yamlDocumentClass.getMethod("getInt", String.class);
                           Object versionObj = getIntMethodx.invoke(document, versionRoute);
                           if (versionObj instanceof Integer) {
                              version = (Integer)versionObj;
                           } else if (versionObj instanceof Number) {
                              version = ((Number)versionObj).intValue();
                           }
                        } catch (NoSuchMethodException var47) {
                           try {
                              Method getMethod = yamlDocumentClass.getMethod("get", String.class, Object.class);
                              Object versionObjx = getMethod.invoke(document, versionRoute, 1);
                              if (versionObjx instanceof Integer) {
                                 version = (Integer)versionObjx;
                              } else if (versionObjx instanceof Number) {
                                 version = ((Number)versionObjx).intValue();
                              }
                           } catch (NoSuchMethodException var46) {
                              if (this.isDebugEnabled()) {
                                 this.plugin.getLogger().info("[DEBUG] Could not find method to get version, using default: 1");
                              }
                           }
                        }
                     }
                  } catch (Exception var49) {
                     if (this.isDebugEnabled()) {
                        this.plugin.getLogger().info("[DEBUG] Error getting version: " + var49.getMessage());
                     }
                  }

                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Successfully updated config file: " + configFile.getName() + " (version: " + version + ")");
                  }

                  if (!isLanguageFile) {
                     this.cleanupDeprecatedKeys(configFile, defaultResourcePath, versionRoute);
                  }

                  if (oldVersion != null
                     && version != null
                     && oldVersion < version
                     && (configFile.getName().equals("flipping.yml") || configFile.getName().equals("gui.yml"))
                     && this.isDebugEnabled()) {
                     this.plugin
                        .getLogger()
                        .info(
                           "[DEBUG] Major version update detected ("
                              + oldVersion
                              + " -> "
                              + version
                              + ") for "
                              + configFile.getName()
                              + ". Comments will be updated from default config."
                        );
                  }

                  return document;
               } else if (isLanguageFile && configFile.exists()) {
                  try {
                     YamlConfiguration.loadConfiguration(configFile);
                     if (this.isDebugEnabled()) {
                        this.plugin
                           .getLogger()
                           .info("[DEBUG] Failed to update language file " + configFile.getName() + " but file is still usable. Continuing...");
                     }

                     return null;
                  } catch (Exception var53) {
                     this.plugin
                        .getLogger()
                        .warning("Failed to update language file: " + configFile.getName() + ". The file may be corrupted. It will be recreated automatically.");
                     return null;
                  }
               } else {
                  this.plugin
                     .getLogger()
                     .warning("Failed to update configuration file: " + configFile.getName() + ". The file may be corrupted or incompatible.");
                  return null;
               }
            } else {
               this.plugin.getLogger().warning("Default resource not found: " + defaultResourcePath);
               if (!createIfNotExists && !configFile.exists()) {
                  return null;
               } else {
                  if (!configFile.exists() && createIfNotExists) {
                     configFile.getParentFile().mkdirs();

                     try {
                        this.plugin.saveResource(defaultResourcePath, false);
                        if (configFile.exists()) {
                           this.plugin.getLogger().info("Created default config file: " + configFile.getName());
                           return null;
                        }
                     } catch (Exception var51) {
                        this.plugin.getLogger().warning("Could not save default resource: " + defaultResourcePath);
                     }
                  }

                  return null;
               }
            }
         } catch (Exception var54) {
            Exception e = var54;
            if (!isLanguageFile || !configFile.exists()) {
               this.plugin.getLogger().warning("Failed to update configuration file: " + configFile.getName() + ". Error: " + var54.getMessage());
               if (this.isDebugEnabled()) {
                  this.plugin.getLogger().info("[DEBUG] Stack trace:");
                  var54.printStackTrace();
               }

               return null;
            } else {
               try {
                  YamlConfiguration.loadConfiguration(configFile);
                  if (this.isDebugEnabled()) {
                     this.plugin
                        .getLogger()
                        .info("[DEBUG] Error updating language file " + configFile.getName() + ": " + e.getMessage() + " (file is still usable)");
                     this.plugin.getLogger().info("[DEBUG] Stack trace:");
                     e.printStackTrace();
                  }

                  return null;
               } catch (Exception var50) {
                  this.plugin
                     .getLogger()
                     .warning(
                        "Failed to update language file: "
                           + configFile.getName()
                           + ". Error: "
                           + var54.getMessage()
                           + ". The file will be recreated automatically."
                     );
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Stack trace:");
                     var54.printStackTrace();
                  }

                  return null;
               }
            }
         } finally {
            if (defaultStream != null) {
               try {
                  defaultStream.close();
               } catch (Exception var43) {
               }
            }
         }
      }
   }

   private Object createYamlDocument(File configFile, InputStream defaultStream, String versionRoute) {
      try {
         if (this.isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] Getting GeneralSettings...");
         }

         Object generalSettings = this.getDefaultSetting(generalSettingsClass, "GeneralSettings");
         if (this.isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] Creating LoaderSettings with auto-update...");
         }

         Object loaderSettingsBuilder = loaderSettingsClass.getMethod("builder").invoke(null);
         loaderSettingsBuilder.getClass().getMethod("setAutoUpdate", boolean.class).invoke(loaderSettingsBuilder, true);
         Object loaderSettings = loaderSettingsBuilder.getClass().getMethod("build").invoke(loaderSettingsBuilder);
         if (this.isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] Getting DumperSettings...");
         }

         Object dumperSettings = this.getDefaultSetting(dumperSettingsClass, "DumperSettings");
         if (this.isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] Creating UpdaterSettings with versioning...");
         }

         Object updaterSettingsBuilder = updaterSettingsClass.getMethod("builder").invoke(null);
         if (configFile.getName().equals("customplaceholder.yml") || configFile.getName().equals("coinsengine.yml")) {
            try {
               Method addIgnoredRouteMethod = updaterSettingsBuilder.getClass().getMethod("addIgnoredRoute", String.class);
               addIgnoredRouteMethod.invoke(updaterSettingsBuilder, "currencies");
               if (this.isDebugEnabled()) {
                  this.plugin.getLogger().info("[DEBUG] Added 'currencies' to ignored routes for " + configFile.getName());
               }
            } catch (NoSuchMethodException var28) {
               if (this.isDebugEnabled()) {
                  this.plugin.getLogger().info("[DEBUG] addIgnoredRoute method not found, trying setKeepAll or alternative");
               }

               try {
                  Method setKeepAllMethod = updaterSettingsBuilder.getClass().getMethod("setKeepAll", boolean.class);
                  setKeepAllMethod.invoke(updaterSettingsBuilder, true);
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Set keepAll=true for " + configFile.getName());
                  }
               } catch (NoSuchMethodException var21) {
                  this.plugin
                     .getLogger()
                     .warning(
                        "BoostedYAML version does not support ignoring routes. User-added currencies in "
                           + configFile.getName()
                           + " may be removed on config update."
                     );
               }
            }
         }

         if (this.isDebugEnabled()) {
            this.plugin.getLogger().info("[DEBUG] Creating BasicVersioning with route: " + versionRoute);
         }

         Object basicVersioning = basicVersioningClass.getConstructor(String.class).newInstance(versionRoute);
         Method setVersioningMethod = null;

         try {
            setVersioningMethod = updaterSettingsBuilder.getClass().getMethod("setVersioning", basicVersioningClass);
         } catch (NoSuchMethodException var27) {
            try {
               Class<?>[] interfaces = basicVersioningClass.getInterfaces();
               if (interfaces.length > 0) {
                  for (Class<?> iface : interfaces) {
                     try {
                        setVersioningMethod = updaterSettingsBuilder.getClass().getMethod("setVersioning", iface);
                        if (this.isDebugEnabled()) {
                           this.plugin.getLogger().info("[DEBUG] Found setVersioning with interface: " + iface.getName());
                        }
                        break;
                     } catch (NoSuchMethodException var25) {
                     }
                  }
               }

               if (setVersioningMethod == null) {
                  throw new NoSuchMethodException("No interface found");
               }
            } catch (Exception var26) {
               try {
                  Class<?> versioningSuperclass = basicVersioningClass.getSuperclass();
                  if (versioningSuperclass != null && !versioningSuperclass.equals(Object.class)) {
                     setVersioningMethod = updaterSettingsBuilder.getClass().getMethod("setVersioning", versioningSuperclass);
                     if (this.isDebugEnabled()) {
                        this.plugin.getLogger().info("[DEBUG] Found setVersioning with superclass: " + versioningSuperclass.getName());
                     }
                  }
               } catch (Exception var24) {
                  Method[] methods = updaterSettingsBuilder.getClass().getMethods();
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Searching for versioning methods in UpdaterSettings$Builder...");
                     this.plugin.getLogger().info("[DEBUG] Available methods with 'version' in name:");

                     for (Method m : methods) {
                        if (m.getName().toLowerCase().contains("version")) {
                           this.plugin.getLogger().info("[DEBUG]   - " + m.getName() + "(" + Arrays.toString((Object[])m.getParameterTypes()) + ")");
                        }
                     }
                  }

                  for (Method mx : methods) {
                     if (mx.getName().toLowerCase().contains("version") && mx.getParameterCount() == 1) {
                        Class<?> paramType = mx.getParameterTypes()[0];
                        if (paramType.isAssignableFrom(basicVersioningClass) || basicVersioningClass.isAssignableFrom(paramType)) {
                           setVersioningMethod = mx;
                           if (this.isDebugEnabled()) {
                              this.plugin.getLogger().info("[DEBUG] Found versioning method: " + mx.getName() + " with param: " + paramType.getName());
                           }
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (setVersioningMethod == null) {
            throw new NoSuchMethodException("Could not find setVersioning method in UpdaterSettings$Builder");
         } else {
            setVersioningMethod.invoke(updaterSettingsBuilder, basicVersioning);
            Object updaterSettings = updaterSettingsBuilder.getClass().getMethod("build").invoke(updaterSettingsBuilder);
            if (this.isDebugEnabled()) {
               this.plugin.getLogger().info("[DEBUG] Calling YamlDocument.create()...");
               Method[] allMethods = yamlDocumentClass.getMethods();
               this.plugin.getLogger().info("[DEBUG] Available create() methods:");

               for (Method mxx : allMethods) {
                  if (mxx.getName().equals("create") && mxx.getParameterCount() >= 2) {
                     this.plugin.getLogger().info("[DEBUG]   - create(" + Arrays.toString((Object[])mxx.getParameterTypes()) + ")");
                  }
               }
            }

            Method createMethod = null;

            try {
               createMethod = yamlDocumentClass.getMethod(
                  "create", File.class, InputStream.class, generalSettingsClass, loaderSettingsClass, dumperSettingsClass, updaterSettingsClass
               );
            } catch (NoSuchMethodException var23) {
               try {
                  createMethod = yamlDocumentClass.getMethod("create", File.class, InputStream.class);
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Found create() with 2 parameters - settings will be ignored");
                  }
               } catch (NoSuchMethodException var22) {
                  Method[] methods = yamlDocumentClass.getMethods();

                  for (Method mxxx : methods) {
                     if (mxxx.getName().equals("create")) {
                        Class<?>[] params = mxxx.getParameterTypes();
                        if (params.length >= 2 && params[0].isAssignableFrom(File.class) && params[1].isAssignableFrom(InputStream.class)) {
                           createMethod = mxxx;
                           if (this.isDebugEnabled()) {
                              this.plugin.getLogger().info("[DEBUG] Found create() method: " + Arrays.toString((Object[])params));
                           }
                           break;
                        }
                     }
                  }
               }
            }

            if (createMethod == null) {
               throw new NoSuchMethodException("Could not find YamlDocument.create() method");
            } else {
               Class<?>[] paramTypes = createMethod.getParameterTypes();
               Object result;
               if (createMethod.getParameterCount() == 2) {
                  result = createMethod.invoke(null, configFile, defaultStream);
               } else if (createMethod.getParameterCount() == 3 && paramTypes[2].isArray()) {
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Creating Settings array...");
                  }

                  Object settingsArray = Array.newInstance(settingsInterface != null ? settingsInterface : Object.class, 4);
                  Array.set(settingsArray, 0, generalSettings);
                  Array.set(settingsArray, 1, loaderSettings);
                  Array.set(settingsArray, 2, dumperSettings);
                  Array.set(settingsArray, 3, updaterSettings);
                  if (this.isDebugEnabled()) {
                     this.plugin.getLogger().info("[DEBUG] Settings array created with " + Array.getLength(settingsArray) + " elements");
                  }

                  result = createMethod.invoke(null, configFile, defaultStream, settingsArray);
               } else if (createMethod.getParameterCount() == 6) {
                  result = createMethod.invoke(null, configFile, defaultStream, generalSettings, loaderSettings, dumperSettings, updaterSettings);
               } else {
                  Object[] args = new Object[paramTypes.length];
                  args[0] = configFile;
                  args[1] = defaultStream;
                  if (paramTypes.length > 2 && paramTypes[2].isArray()) {
                     Object settingsArray = Array.newInstance(settingsInterface != null ? settingsInterface : Object.class, 4);
                     Array.set(settingsArray, 0, generalSettings);
                     Array.set(settingsArray, 1, loaderSettings);
                     Array.set(settingsArray, 2, dumperSettings);
                     Array.set(settingsArray, 3, updaterSettings);
                     args[2] = settingsArray;
                  } else {
                     if (paramTypes.length > 2 && paramTypes[2].isAssignableFrom(generalSettingsClass)) {
                        args[2] = generalSettings;
                     }

                     if (paramTypes.length > 3 && paramTypes[3].isAssignableFrom(loaderSettingsClass)) {
                        args[3] = loaderSettings;
                     }

                     if (paramTypes.length > 4 && paramTypes[4].isAssignableFrom(dumperSettingsClass)) {
                        args[4] = dumperSettings;
                     }

                     if (paramTypes.length > 5 && paramTypes[5].isAssignableFrom(updaterSettingsClass)) {
                        args[5] = updaterSettings;
                     }
                  }

                  result = createMethod.invoke(null, args);
               }

               if (this.isDebugEnabled()) {
                  this.plugin.getLogger().info("[DEBUG] YamlDocument.create() returned: " + (result != null ? "success" : "null"));
               }

               return result;
            }
         }
      } catch (Exception var29) {
         boolean isLanguageFile = configFile != null && configFile.getPath().contains("langs" + File.separator + "messages_");
         if (isLanguageFile) {
            if (this.isDebugEnabled()) {
               this.plugin
                  .getLogger()
                  .info("[DEBUG] Error creating configuration document for language file: " + var29.getClass().getSimpleName() + ": " + var29.getMessage());
               this.plugin.getLogger().info("[DEBUG] Stack trace:");
               var29.printStackTrace();
            }
         } else {
            this.plugin.getLogger().warning("Error creating configuration document: " + var29.getClass().getSimpleName() + ": " + var29.getMessage());
            if (this.isDebugEnabled()) {
               this.plugin.getLogger().info("[DEBUG] Stack trace:");
               var29.printStackTrace();
            }
         }

         return null;
      }
   }

   private Object getDefaultSetting(Class<?> settingsClass, String className) throws Exception {
      try {
         Field defaultField = settingsClass.getField("DEFAULT");
         return defaultField.get(null);
      } catch (NoSuchFieldException var10) {
         try {
            Method getDefaultMethod = settingsClass.getMethod("getDefault");
            return getDefaultMethod.invoke(null);
         } catch (NoSuchMethodException var9) {
            try {
               Method builderMethod = settingsClass.getMethod("builder");
               Object builder = builderMethod.invoke(null);
               Method buildMethod = builder.getClass().getMethod("build");
               return buildMethod.invoke(builder);
            } catch (Exception var8) {
               throw new Exception("Could not get DEFAULT setting for " + className, var8);
            }
         }
      }
   }

   private void cleanupDeprecatedKeys(File configFile, String defaultResourcePath, String versionRoute) {
      if (configFile.exists()) {
         if (!configFile.getName().equals("customplaceholder.yml") && !configFile.getName().equals("coinsengine.yml")) {
            if (!configFile.getPath().contains("langs" + File.separator + "messages_") && !configFile.getName().startsWith("messages_")) {
               InputStream defaultStream = null;

               try {
                  FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
                  defaultStream = this.plugin.getResource(defaultResourcePath);
                  if (defaultStream != null) {
                     FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                     Set<String> defaultKeys = defaultConfig.getKeys(true);
                     Set<String> currentKeys = currentConfig.getKeys(true);
                     List<String> deprecatedKeys = new ArrayList<>();

                     for (String key : currentKeys) {
                        if (!key.equals(versionRoute) && !defaultKeys.contains(key)) {
                           boolean isParentKey = false;

                           for (String defaultKey : defaultKeys) {
                              if (defaultKey.startsWith(key + ".")) {
                                 isParentKey = true;
                                 break;
                              }
                           }

                           if (!isParentKey) {
                              deprecatedKeys.add(key);
                           }
                        }
                     }

                     if (!deprecatedKeys.isEmpty()) {
                        List<String> keysToRemove = new ArrayList<>();

                        for (String keyx : deprecatedKeys) {
                           boolean hasParentInList = false;

                           for (String otherKey : deprecatedKeys) {
                              if (!keyx.equals(otherKey) && keyx.startsWith(otherKey + ".")) {
                                 hasParentInList = true;
                                 break;
                              }
                           }

                           if (!hasParentInList) {
                              keysToRemove.add(keyx);
                           }
                        }

                        keysToRemove.sort((k1, k2) -> {
                           int depth1 = k1.split("\\.").length;
                           int depth2 = k2.split("\\.").length;
                           return Integer.compare(depth2, depth1);
                        });
                        if (!keysToRemove.isEmpty()) {
                           for (String keyx : keysToRemove) {
                              try {
                                 currentConfig.set(keyx, null);
                              } catch (Exception var28) {
                                 this.plugin.getLogger().fine("Could not remove deprecated key '" + keyx + "': " + var28.getMessage());
                              }
                           }

                           try {
                              currentConfig.save(configFile);
                           } catch (Exception var27) {
                           }

                           return;
                        }
                     }

                     return;
                  }

                  this.plugin.getLogger().fine("Could not load default config for cleanup: " + defaultResourcePath);
               } catch (Exception var29) {
                  return;
               } finally {
                  if (defaultStream != null) {
                     try {
                        defaultStream.close();
                     } catch (Exception var26) {
                     }
                  }
               }
            }
         }
      }
   }
}
