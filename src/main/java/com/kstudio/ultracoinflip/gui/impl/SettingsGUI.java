package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SettingsGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-settings";
   }

   private Material parseMaterial(String materialName, Material fallback) {
      return MaterialHelper.parseMaterial(materialName, fallback);
   }

   @Override
   protected Inventory createInventory() {
      String titleTemplate = this.plugin.getGUIConfig().getString("settings-gui.title", "&6&lCoinFlip Settings");
      int size = this.plugin.getGUIConfig().getInt("settings-gui.size", 45);
      return this.plugin.getGuiHelper().createInventory(null, size, titleTemplate, null);
   }

   @Override
   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         Inventory inventory = this.getInventory();
         if (inventory != null) {
            String fillerMaterialName = this.plugin.getGUIConfig().getString("settings-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
            ItemStack fillerItem = MaterialHelper.createItemStack(fillerMaterialName);
            if (fillerItem == null) {
               fillerItem = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
            }

            if (fillerItem == null) {
               fillerItem = new ItemStack(Material.GLASS_PANE);
            }

            ItemMeta fillerMeta = fillerItem.getItemMeta();
            String fillerDisplayName = this.plugin.getGUIConfig().getString("settings-gui.filler.display-name", " ");
            this.plugin.getGuiHelper().setDisplayName(fillerMeta, fillerDisplayName);
            this.plugin.getGuiHelper().applyItemProperties(fillerMeta, "settings-gui.filler", this.plugin.getGUIConfig());
            fillerItem.setItemMeta(fillerMeta);
            ItemStack filler = fillerItem;
            int backSlot = this.plugin.getGUIConfig().getInt("settings-gui.back.slot", 40);
            List<Integer> settingsSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "settings-gui.settings-slots");
            List<Integer> settingsSlots = settingsSlotsList.isEmpty()
               ? Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34)
               : settingsSlotsList;

            for (int i = 0; i < inventory.getSize(); i++) {
               inventory.setItem(i, null);
            }

            List<Integer> fillerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "settings-gui.filler.slots");
            Set<Integer> excludedSlots = new HashSet<>();
            excludedSlots.add(backSlot);
            excludedSlots.addAll(settingsSlots);
            int[] fillerSlots = fillerSlotsList.isEmpty()
               ? new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44}
               : fillerSlotsList.stream()
                  .mapToInt(i -> i != null ? i : -1)
                  .filter(i -> i >= 0 && i < inventory.getSize() && !excludedSlots.contains(i))
                  .toArray();
            ItemStack finalFiller = filler;

            for (int slot : fillerSlots) {
               if (slot >= 0 && slot < inventory.getSize()) {
                  this.addButton(slot, new InventoryButton().creator(p -> finalFiller).consumer(event -> {}));
               }
            }

            String enabledMaterialName = this.plugin.getGUIConfig().getString("settings-gui.toggle.enabled", "LIME_DYE");
            String disabledMaterialName = this.plugin.getGUIConfig().getString("settings-gui.toggle.disabled", "GRAY_DYE");
            Material enabledMaterial = this.parseMaterial(enabledMaterialName, Material.LIME_DYE);
            Material disabledMaterial = this.parseMaterial(disabledMaterialName, Material.GRAY_DYE);
            ConfigurationSection settingsSection = this.plugin.getGUIConfig().getConfigurationSection("settings-gui.settings");
            if (settingsSection != null) {
               int slotIndex = 0;
               Set<String> settingKeys = settingsSection.getKeys(false);
               List<String> sortedKeys = new ArrayList<>(settingKeys);
               sortedKeys.sort((k1, k2) -> {
                  int slot1 = this.plugin.getGUIConfig().getInt("settings-gui.settings." + k1 + ".slot", 999);
                  int slot2 = this.plugin.getGUIConfig().getInt("settings-gui.settings." + k2 + ".slot", 999);
                  return Integer.compare(slot1, slot2);
               });

               for (String settingKey : sortedKeys) {
                  String configPath = "settings-gui.settings." + settingKey;
                  boolean settingEnabled = this.plugin.getGUIConfig().getBoolean(configPath + ".enabled", true);
                  if (settingEnabled) {
                     int slotx = this.plugin.getGUIConfig().getInt(configPath + ".slot", -1);
                     if (slotx < 0 || slotx >= inventory.getSize()) {
                        if (slotIndex >= settingsSlots.size()) {
                           break;
                        }

                        slotx = settingsSlots.get(slotIndex);
                     }

                     if (slotx == backSlot) {
                        slotIndex++;
                     } else {
                        this.addButton(slotx, new InventoryButton().creator(p -> {
                           boolean currentValue = this.plugin.getPlayerSettingsManager().getSetting(p.getUniqueId(), settingKey);
                           return this.createToggleButton(settingKey, configPath, currentValue, enabledMaterial, disabledMaterial);
                        }).consumer(event -> {
                           Player clicker = (Player)event.getWhoClicked();
                           this.plugin.getPlayerSettingsManager().toggleSetting(clicker.getUniqueId(), settingKey);
                           this.plugin.getSoundHelper().playSound(clicker, "gui.click");
                           this.decorate(clicker);
                           this.plugin.getInventoryUpdateBatcher().scheduleUpdate(clicker);
                        }));
                        slotIndex++;
                     }
                  }
               }
            }

            String backMaterialName = this.plugin.getGUIConfig().getString("settings-gui.back.material", "NETHER_STAR");
            Material backMaterial = this.parseMaterial(backMaterialName, Material.NETHER_STAR);
            String backTitle = this.plugin.getGUIConfig().getString("settings-gui.back.title", "&r&c&lBack to Main Menu");
            List<String> backLore = this.plugin.getGUIConfig().getStringList("settings-gui.back.lore");
            this.addButton(backSlot, new InventoryButton().creator(p -> this.createBackButton(backMaterial, backTitle, backLore)).consumer(event -> {
               Player clicker = (Player)event.getWhoClicked();
               this.plugin.getSoundHelper().playSound(clicker, "gui.open-list");

               try {
                  clicker.closeInventory();
               } catch (Exception var4x) {
               }

               FoliaScheduler.runTaskLater(this.plugin, this.viewer, () -> {
                  if (this.viewer != null && this.viewer.isOnline()) {
                     this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, this.viewer, 1), this.viewer);
                  }
               }, 2L);
            }));
            super.decorate(player);
         }
      }
   }

   private ItemStack createToggleButton(String settingKey, String configPath, boolean enabled, Material enabledMaterial, Material disabledMaterial) {
      Material material = enabled ? enabledMaterial : disabledMaterial;
      if (material == null) {
         material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         String titleKey = enabled ? "title-enabled" : "title-disabled";
         String title = this.plugin.getGUIConfig().getString(configPath + "." + titleKey, enabled ? "&a&l✓ &fSetting" : "&7&l✗ &fSetting");
         List<String> lore = this.plugin.getGUIConfig().getStringList(configPath + "." + (enabled ? "lore-enabled" : "lore-disabled"));
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         if (lore != null && !lore.isEmpty()) {
            List<?> loreList = this.plugin.getGuiHelper().createLore(lore, new HashMap<>());
            this.plugin.getGuiHelper().setLore(meta, loreList);
         }

         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createBackButton(Material material, String title, List<String> lore) {
      if (material == null) {
         material = Material.NETHER_STAR;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&c&lBack to Main Menu");
         if (lore != null && !lore.isEmpty()) {
            List<?> loreList = this.plugin.getGuiHelper().createLore(lore, new HashMap<>());
            this.plugin.getGuiHelper().setLore(meta, loreList);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "settings-gui.back", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
         return item;
      }
   }

   @Generated
   public SettingsGUI(KStudio plugin, Player viewer) {
      this.plugin = plugin;
      this.viewer = viewer;
   }
}
