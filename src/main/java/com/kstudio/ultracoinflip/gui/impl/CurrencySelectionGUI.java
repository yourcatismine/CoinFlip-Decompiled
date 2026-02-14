package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.AmountParser;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CurrencySelectionGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;
   private final CreateCoinFlipGUI parentGUI;

   public CurrencySelectionGUI(KStudio plugin, Player viewer, CreateCoinFlipGUI parentGUI) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.parentGUI = parentGUI;
   }

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-currency";
   }

   @Override
   protected Inventory createInventory() {
      String title = this.plugin.getGUIConfig().getString("currency-selection-gui.title", "&lSelect Currency");
      int size = this.plugin.getGUIConfig().getInt("currency-selection-gui.size", 36);
      return this.plugin.getGuiHelper().createInventory(null, size, title, new HashMap<>());
   }

   @Override
   public void decorate(Player player) {
      String fillerMaterialName = this.plugin.getGUIConfig().getString("currency-selection-gui.filler.material",
            "BLACK_STAINED_GLASS_PANE");
      ItemStack filler = MaterialHelper.createItemStack(fillerMaterialName);
      if (filler == null) {
         filler = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
      }

      if (filler == null) {
         filler = new ItemStack(Material.GLASS_PANE);
      }

      ItemMeta fillerMeta = filler.getItemMeta();
      if (fillerMeta != null) {
         String fillerDisplayName = this.plugin.getGUIConfig().getString("currency-selection-gui.filler.display-name",
               " ");
         this.plugin.getGuiHelper().setDisplayName(fillerMeta, fillerDisplayName);
         this.plugin.getGuiHelper().applyItemProperties(fillerMeta, "currency-selection-gui.filler",
               this.plugin.getGUIConfig());
         filler.setItemMeta(fillerMeta);
      }

      for (int slot : this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
            "currency-selection-gui.filler.slots")) {
         if (slot >= 0 && slot < this.getInventory().getSize()) {
            this.getInventory().setItem(slot, filler);
         }
      }

      List<Integer> currencySlots = new ArrayList<>();
      List<Integer> row1 = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
            "currency-selection-gui.currency-slots.row1");
      List<Integer> row2 = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
            "currency-selection-gui.currency-slots.row2");
      currencySlots.addAll(row1);
      currencySlots.addAll(row2);
      List<CurrencySelectionGUI.CurrencyItem> currencies = this.getAllCurrencies();
      int slotIndex = 0;

      for (CurrencySelectionGUI.CurrencyItem currency : currencies) {
         if (slotIndex >= currencySlots.size()) {
            break;
         }

         int slotx = currencySlots.get(slotIndex);
         CoinFlipGame.CurrencyType type = currency.type;
         String currencyId = currency.currencyId;
         this.addButton(slotx,
               new InventoryButton().creator(p -> this.createCurrencyItem(type, currencyId)).consumer(event -> {
                  if (this.plugin.getCurrencyManager().isCurrencyEnabled(type, currencyId)) {
                     this.parentGUI.setSelectedCurrency(type, currencyId);
                     CreateCoinFlipGUI newGUI = new CreateCoinFlipGUI(this.plugin, this.viewer);
                     newGUI.setSelectedCurrency(type, currencyId);
                     newGUI.setCurrentAmount(this.parentGUI.getCurrentAmount());
                     this.plugin.getGuiManager().openGUI(newGUI, this.viewer);
                  }
               }));
         slotIndex++;
      }

      int backSlot = this.plugin.getGUIConfig().getInt("currency-selection-gui.back.slot", 40);
      String backMaterialName = this.plugin.getGUIConfig().getString("currency-selection-gui.back.material", "BARRIER");
      Material backMaterial = MaterialHelper.getBarrierMaterial();
      Material parsedBackMaterial = MaterialHelper.parseMaterial(backMaterialName, backMaterial);
      if (parsedBackMaterial != null) {
         backMaterial = parsedBackMaterial;
      }

      Material finalBackMaterial = backMaterial;
      String backTitle = this.plugin.getGUIConfig().getString("currency-selection-gui.back.title", "&r&c&lGo Back");
      List<String> backLoreConfig = this.plugin.getGUIConfig().getStringList("currency-selection-gui.back.lore");
      List<String> backLore;
      if (backLoreConfig != null && !backLoreConfig.isEmpty()) {
         backLore = backLoreConfig;
      } else {
         backLore = new ArrayList<>();
         backLore.add("&r&7Click to go back");
      }

      List<String> finalBackLore = backLore;
      this.addButton(
            backSlot,
            new InventoryButton()
                  .creator(p -> this.createBackButton(finalBackMaterial, backTitle, finalBackLore))
                  .consumer(event -> this.plugin.getGuiManager()
                        .openGUI(new CreateCoinFlipGUI(this.plugin, this.viewer), this.viewer)));
      super.decorate(player);
   }

   private List<CurrencySelectionGUI.CurrencyItem> getAllCurrencies() {
      List<CurrencySelectionGUI.CurrencyItem> currencies = new ArrayList<>();
      currencies.add(new CurrencySelectionGUI.CurrencyItem(CoinFlipGame.CurrencyType.MONEY, null));
      currencies.add(new CurrencySelectionGUI.CurrencyItem(CoinFlipGame.CurrencyType.PLAYERPOINTS, null));
      currencies.add(new CurrencySelectionGUI.CurrencyItem(CoinFlipGame.CurrencyType.TOKENMANAGER, null));
      currencies.add(new CurrencySelectionGUI.CurrencyItem(CoinFlipGame.CurrencyType.BEASTTOKENS, null));
      File coinsEngineFile = new File(this.plugin.getDataFolder(), "currencies/coinsengine.yml");
      if (coinsEngineFile.exists()) {
         FileConfiguration coinsEngineConfig = YamlConfiguration.loadConfiguration(coinsEngineFile);
         if (coinsEngineConfig.contains("currencies")) {
            ConfigurationSection currenciesSection = coinsEngineConfig.getConfigurationSection("currencies");
            if (currenciesSection != null) {
               for (String currencyId : currenciesSection.getKeys(false)) {
                  if (currencyId != null && !currencyId.isEmpty()) {
                     currencies.add(
                           new CurrencySelectionGUI.CurrencyItem(CoinFlipGame.CurrencyType.COINSENGINE, currencyId));
                  }
               }
            }
         }
      }

      File placeholderFile = new File(this.plugin.getDataFolder(), "currencies/customplaceholder.yml");
      if (placeholderFile.exists()) {
         FileConfiguration placeholderConfig = YamlConfiguration.loadConfiguration(placeholderFile);
         if (placeholderConfig.contains("currencies")) {
            ConfigurationSection currenciesSection = placeholderConfig.getConfigurationSection("currencies");
            if (currenciesSection != null) {
               for (String currencyIdx : currenciesSection.getKeys(false)) {
                  if (currencyIdx != null && !currencyIdx.isEmpty()) {
                     currencies.add(
                           new CurrencySelectionGUI.CurrencyItem(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyIdx));
                  }
               }
            }
         }
      }

      return currencies;
   }

   private ItemStack createCurrencyItem(CoinFlipGame.CurrencyType type, String currencyId) {
      boolean isEnabled = this.plugin.getCurrencyManager().isCurrencyEnabled(type, currencyId);
      boolean isSelected = this.parentGUI.getSelectedCurrencyType() == type
            && (currencyId == null ? this.parentGUI.getSelectedCurrencyId() == null
                  : currencyId.equals(this.parentGUI.getSelectedCurrencyId()));
      String configKey = this.getCurrencyConfigKey(type, currencyId);
      String basePath = "currency-selection-gui.currencies." + configKey;
      String defaultPath = "currency-selection-gui.currency-default";
      Material material;
      if (isEnabled) {
         String materialName = this.plugin
               .getGUIConfig()
               .getString(basePath + ".material",
                     this.plugin.getGUIConfig().getString(defaultPath + ".material", "GOLD_INGOT"));

         try {
            material = Material.valueOf(materialName.toUpperCase());
         } catch (IllegalArgumentException var21) {
            material = Material.GOLD_INGOT;
         }
      } else {
         String disabledMaterialName = this.plugin.getGUIConfig().getString(defaultPath + ".disabled-material",
               "BARRIER");
         Material barrierFallback = MaterialHelper.getBarrierMaterial();
         material = MaterialHelper.parseMaterial(disabledMaterialName, barrierFallback);
         if (material == null) {
            material = barrierFallback;
         }
      }

      String displayName = isEnabled
            ? this.plugin.getCurrencyManager().getDisplayName(type, currencyId)
            : type.name() + (currencyId != null ? ":" + currencyId : "");
      String titleTemplate;
      if (isEnabled) {
         if (isSelected) {
            titleTemplate = this.plugin
                  .getGUIConfig()
                  .getString(
                        basePath + ".selected-title", this.plugin.getGUIConfig()
                              .getString(defaultPath + ".selected-title", "&r&6&l<displayName> &a&l(Selected)"));
         } else {
            titleTemplate = this.plugin
                  .getGUIConfig()
                  .getString(basePath + ".title",
                        this.plugin.getGUIConfig().getString(defaultPath + ".title", "&r&6&l<displayName>"));
         }
      } else {
         titleTemplate = this.plugin.getGUIConfig().getString(defaultPath + ".disabled-title",
               "&r&c&l<displayName> &7(Not Loaded)");
      }

      Map<String, String> placeholders = new HashMap<>();
      placeholders.put("displayName", displayName);
      String title = this.plugin.getAdventureHelper().parseToLegacy(titleTemplate, placeholders);
      List<String> lore;
      if (isEnabled) {
         List<String> loreTemplate = this.plugin.getGUIConfig().getStringList(basePath + ".lore");
         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = this.plugin.getGUIConfig().getStringList(defaultPath + ".lore");
         }

         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
            loreTemplate.add("&r&fUnit: &e<unit>");
            loreTemplate.add("&r&fBalance: &a<balance><unit>");
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
            loreTemplate.add("&r&a&l» Click to select");
         }

         String unit = this.plugin.getCurrencyManager().getUnit(type, currencyId);
         double balance = this.plugin.getCurrencyManager().getBalance(this.viewer, type, currencyId);
         placeholders.put("unit", unit);
         placeholders.put("balance", AmountParser.formatAmount(balance));
         List<?> loreList = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
         lore = new ArrayList<>();

         for (Object obj : loreList) {
            if (obj instanceof String) {
               lore.add((String) obj);
            }
         }
      } else {
         List<String> disabledLoreTemplate = this.plugin.getGUIConfig().getStringList(defaultPath + ".disabled-lore");
         if (disabledLoreTemplate == null || disabledLoreTemplate.isEmpty()) {
            disabledLoreTemplate = new ArrayList<>();
            disabledLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
            disabledLoreTemplate.add("&r&7This currency is not loaded");
            disabledLoreTemplate.add("&r&7or not available");
            disabledLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
         }

         List<?> loreList = this.plugin.getGuiHelper().createLore(disabledLoreTemplate, placeholders);
         lore = new ArrayList<>();

         for (Object objx : loreList) {
            if (objx instanceof String) {
               lore.add((String) objx);
            }
         }
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         boolean glowing = this.plugin.getGUIConfig().getBoolean(basePath + ".glowing",
               this.plugin.getGUIConfig().getBoolean(defaultPath + ".glowing", false));
         int customModelData = this.plugin
               .getGUIConfig()
               .getInt(basePath + ".custom-model-data",
                     this.plugin.getGUIConfig().getInt(defaultPath + ".custom-model-data", 0));
         this.plugin.getGuiHelper().applyItemProperties(meta, glowing, customModelData);
         item.setItemMeta(meta);
      }

      return item;
   }

   private String getCurrencyConfigKey(CoinFlipGame.CurrencyType type, String currencyId) {
      String typeName = type.name().toLowerCase();
      return currencyId != null ? "currency-" + typeName + "-" + currencyId.toLowerCase() : "currency-" + typeName;
   }

   private ItemStack createBackButton(Material material, String title, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         if (lore != null && !lore.isEmpty()) {
            this.plugin.getGuiHelper().setLore(meta, this.plugin.getGuiHelper().createLore(lore, new HashMap<>()));
         }

         this.plugin
               .getGuiHelper()
               .applyItemProperties(
                     meta,
                     this.plugin.getGUIConfig().getBoolean("currency-selection-gui.back.glowing", false),
                     this.plugin.getGUIConfig().getInt("currency-selection-gui.back.custom-model-data", 0));
         item.setItemMeta(meta);
      }

      return item;
   }

   private static class CurrencyItem {
      final CoinFlipGame.CurrencyType type;
      final String currencyId;

      CurrencyItem(CoinFlipGame.CurrencyType type, String currencyId) {
         this.type = type;
         this.currencyId = currencyId;
      }
   }
}
