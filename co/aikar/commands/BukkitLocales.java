package co.aikar.commands;

import co.aikar.locales.MessageKey;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class BukkitLocales extends Locales {
   private final BukkitCommandManager manager;

   public BukkitLocales(BukkitCommandManager manager) {
      super(manager);
      this.manager = manager;
      this.addBundleClassLoader(this.manager.getPlugin().getClass().getClassLoader());
   }

   @Override
   public void loadLanguages() {
      super.loadLanguages();
      String pluginName = "acf-" + this.manager.plugin.getDescription().getName();
      this.addMessageBundles("acf-minecraft", pluginName, pluginName.toLowerCase(Locale.ENGLISH));
   }

   public boolean loadYamlLanguageFile(File file, Locale locale) throws IOException, InvalidConfigurationException {
      YamlConfiguration yamlConfiguration = new YamlConfiguration();
      yamlConfiguration.load(file);
      return this.loadLanguage(yamlConfiguration, locale);
   }

   public boolean loadYamlLanguageFile(String file, Locale locale) throws IOException, InvalidConfigurationException {
      YamlConfiguration yamlConfiguration = new YamlConfiguration();
      yamlConfiguration.load(new File(this.manager.plugin.getDataFolder(), file));
      return this.loadLanguage(yamlConfiguration, locale);
   }

   public boolean loadLanguage(FileConfiguration config, Locale locale) {
      boolean loaded = false;

      for (String key : config.getKeys(true)) {
         if (config.isString(key) || config.isDouble(key) || config.isLong(key) || config.isInt(key) || config.isBoolean(key)) {
            String value = config.getString(key);
            if (value != null && !value.isEmpty()) {
               this.addMessage(locale, MessageKey.of(key), value);
               loaded = true;
            }
         }
      }

      return loaded;
   }
}
