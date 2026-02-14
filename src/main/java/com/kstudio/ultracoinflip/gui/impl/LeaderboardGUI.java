package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class LeaderboardGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;
   private String filterType = "WINS";
   private CoinFlipGame.CurrencyType currencyType = CoinFlipGame.CurrencyType.MONEY;
   private String currencyId = null;
   private volatile List<DatabaseManager.LeaderboardEntry> cachedEntries = new ArrayList<>();
   private volatile boolean isLoading = false;
   private int[] PLAYER_HEAD_SLOTS = new int[] { 13, 21, 22, 23, 29, 30, 31, 32, 33, 37, 38, 39, 40, 41, 42, 43 };
   private int FILTER_SLOT = 10;
   private int CURRENCY_SLOT = 16;
   private int BACK_SLOT = 49;

   public LeaderboardGUI(KStudio plugin, Player viewer) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.setDefaultCurrency();
      this.setDefaultFilter();
   }

   private void setDefaultCurrency() {
      List<LeaderboardGUI.CurrencyItem> enabledCurrencies = this.getEnabledCurrencies();
      if (!enabledCurrencies.isEmpty()) {
         LeaderboardGUI.CurrencyItem firstCurrency = enabledCurrencies.get(0);
         this.currencyType = firstCurrency.type;
         this.currencyId = firstCurrency.currencyId;
      }
   }

   private void setDefaultFilter() {
      List<String> enabledFilters = this.getEnabledFilters();
      if (!enabledFilters.isEmpty()) {
         this.filterType = enabledFilters.get(0);
      } else {
         this.filterType = "WINS";
         this.plugin.getLogger().warning("No leaderboard filters are enabled in config.yml! Defaulting to WINS.");
      }
   }

   public LeaderboardGUI(KStudio plugin, Player viewer, String filterType, CoinFlipGame.CurrencyType currencyType,
         String currencyId) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.filterType = filterType != null ? filterType : "WINS";
      this.currencyType = currencyType != null ? currencyType : CoinFlipGame.CurrencyType.MONEY;
      this.currencyId = currencyId;
   }

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-leaderboard";
   }

   @Override
   protected Inventory createInventory() {
      String title = this.plugin.getGUIConfig().getString("leaderboard-gui.title", "&f&lᴄᴏɪɴꜰʟɪᴘ &8[&7Leaderboard&8]");
      int size = this.plugin.getGUIConfig().getInt("leaderboard-gui.size", 54);
      return this.plugin.getGuiHelper().createInventory(null, size, title, new HashMap<>());
   }

   @Override
   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         Inventory inventory = this.getInventory();
         if (inventory != null) {
            this.FILTER_SLOT = this.plugin.getGUIConfig().getInt("leaderboard-gui.filter.slot", 10);
            this.CURRENCY_SLOT = this.plugin.getGUIConfig().getInt("leaderboard-gui.currency.slot", 16);
            this.BACK_SLOT = this.plugin.getGUIConfig().getInt("leaderboard-gui.back.slot", 49);
            List<Integer> playerHeadSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
                  "leaderboard-gui.player-head-slots");
            if (playerHeadSlotsList.isEmpty()) {
               this.PLAYER_HEAD_SLOTS = new int[] { 13, 21, 22, 23, 29, 30, 31, 32, 33, 37, 38, 39, 40, 41, 42, 43 };
            } else {
               this.PLAYER_HEAD_SLOTS = playerHeadSlotsList.stream().mapToInt(i -> i != null ? i : -1)
                     .filter(i -> i >= 0 && i < inventory.getSize()).toArray();
            }

            this.getButtonMap().clear();
            String fillerMaterialName = this.plugin.getGUIConfig().getString("leaderboard-gui.filler.material",
                  "GRAY_STAINED_GLASS_PANE");
            ItemStack filler = MaterialHelper.createItemStack(fillerMaterialName);
            if (filler == null) {
               filler = MaterialHelper.createItemStack("GRAY_STAINED_GLASS_PANE");
            }

            if (filler == null) {
               filler = new ItemStack(Material.GLASS_PANE);
            }

            ItemMeta fillerMeta = filler.getItemMeta();
            if (fillerMeta != null) {
               String fillerDisplayName = this.plugin.getGUIConfig().getString("leaderboard-gui.filler.display-name",
                     " ");
               this.plugin.getGuiHelper().setDisplayName(fillerMeta, fillerDisplayName);
               filler.setItemMeta(fillerMeta);
            }

            Set<Integer> excludedSlots = new HashSet<>();
            excludedSlots.add(this.FILTER_SLOT);
            excludedSlots.add(this.CURRENCY_SLOT);
            excludedSlots.add(this.BACK_SLOT);

            for (int slot : this.PLAYER_HEAD_SLOTS) {
               excludedSlots.add(slot);
            }

            for (int i = 0; i < inventory.getSize(); i++) {
               if (!excludedSlots.contains(i)) {
                  inventory.setItem(i, filler);
               }
            }

            this.loadLeaderboardData();
            this.addButton(this.FILTER_SLOT,
                  new InventoryButton().creator(p -> this.createFilterButton()).consumer(event -> {
                     List<String> enabledFilters = this.getEnabledFilters();
                     if (enabledFilters.isEmpty()) {
                        this.plugin.getLogger().warning(
                              "No leaderboard filters are enabled! Please enable at least one filter in config.yml");
                     } else {
                        int currentIndex = enabledFilters.indexOf(this.filterType);
                        if (currentIndex == -1) {
                           this.filterType = enabledFilters.get(0);
                        } else {
                           int nextIndex = (currentIndex + 1) % enabledFilters.size();
                           this.filterType = enabledFilters.get(nextIndex);
                        }

                        synchronized (this) {
                           this.cachedEntries.clear();
                           this.isLoading = false;
                        }

                        this.updateFilterButton();
                        FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                           if (this.viewer != null && this.viewer.isOnline()) {
                              try {
                                 Inventory topInv = GUIHelper.getTopInventorySafely(this.viewer);
                                 if (topInv != null && topInv.equals(this.getInventory())) {
                                    this.displayPlayerHeads();
                                    this.updateFilterButton();
                                    this.updateCurrencyButton();
                                    super.decorate(this.viewer);
                                    this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                                 }
                              } catch (Exception var2x) {
                              }
                           }
                        });
                        this.loadLeaderboardData();
                     }
                  }));
            this.addButton(this.CURRENCY_SLOT,
                  new InventoryButton().creator(p -> this.createCurrencyButton()).consumer(event -> {
                     List<LeaderboardGUI.CurrencyItem> enabledCurrencies = this.getEnabledCurrencies();
                     if (enabledCurrencies.isEmpty()) {
                        this.plugin.getLogger().warning("No enabled currencies found for leaderboard!");
                     } else {
                        int currentIndex = -1;

                        for (int ix = 0; ix < enabledCurrencies.size(); ix++) {
                           LeaderboardGUI.CurrencyItem item = enabledCurrencies.get(ix);
                           boolean typeMatches = item.type == this.currencyType;
                           boolean idMatches;
                           if (this.currencyId == null && item.currencyId == null) {
                              idMatches = true;
                           } else if (this.currencyId != null && item.currencyId != null) {
                              idMatches = this.currencyId.equals(item.currencyId);
                           } else {
                              idMatches = false;
                           }

                           if (typeMatches && idMatches) {
                              currentIndex = ix;
                              break;
                           }
                        }

                        if (currentIndex == -1) {
                           currentIndex = 0;
                        }

                        int nextIndex = (currentIndex + 1) % enabledCurrencies.size();
                        LeaderboardGUI.CurrencyItem nextCurrency = enabledCurrencies.get(nextIndex);
                        this.currencyType = nextCurrency.type;
                        this.currencyId = nextCurrency.currencyId;
                        synchronized (this) {
                           this.cachedEntries.clear();
                           this.isLoading = false;
                        }

                        FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                           if (this.viewer != null && this.viewer.isOnline()) {
                              try {
                                 Inventory topInv = GUIHelper.getTopInventorySafely(this.viewer);
                                 if (topInv != null && topInv.equals(this.getInventory())) {
                                    this.updateCurrencyButton();
                                    this.updateFilterButton();
                                    this.displayPlayerHeads();
                                    super.decorate(this.viewer);
                                    this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                                 }
                              } catch (Exception var2x) {
                              }
                           }
                        });
                        this.loadLeaderboardData();
                     }
                  }));
            this.addButton(this.BACK_SLOT,
                  new InventoryButton().creator(p -> this.createBackButton()).consumer(event -> {
                     Player clicker = (Player) event.getWhoClicked();

                     try {
                        GUIHelper.setCursorSafely(clicker, null);
                        clicker.setItemOnCursor(null);
                     } catch (Exception var4x) {
                     }

                     FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                        if (this.viewer != null && this.viewer.isOnline()) {
                           try {
                              GUIHelper.setCursorSafely(this.viewer, null);
                              this.viewer.setItemOnCursor(null);
                              this.viewer.closeInventory();
                           } catch (Exception var2x) {
                           }

                           FoliaScheduler.runTaskLater(this.plugin, this.viewer, () -> {
                              if (this.viewer != null && this.viewer.isOnline()) {
                                 try {
                                    this.viewer.setItemOnCursor(null);
                                 } catch (Exception var2xx) {
                                 }

                                 this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, this.viewer, 1),
                                       this.viewer);
                              }
                           }, 2L);
                        }
                     });
                  }));
            this.displayPlayerHeads();
            super.decorate(player);
         }
      }
   }

   private Material parseMaterial(String materialName, Material fallback) {
      if (materialName != null && !materialName.isEmpty()) {
         try {
            return MaterialHelper.parseMaterial(materialName, MaterialHelper.getBarrierMaterial());
         } catch (IllegalArgumentException var4) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private void loadLeaderboardData() {
      synchronized (this) {
         if (this.isLoading) {
            return;
         }

         this.isLoading = true;
      }

      String currentFilter = this.filterType;
      CoinFlipGame.CurrencyType currentCurrencyType = this.currencyType;
      String currentCurrencyId = this.currencyId;
      Player currentViewer = this.viewer;
      FoliaScheduler.runTaskAsynchronously(
            this.plugin,
            () -> {
               try {
                  List<DatabaseManager.LeaderboardEntry> entries;
                  switch (currentFilter) {
                     case "WINS":
                        entries = this.plugin.getDatabaseManager().getTopPlayersByWins(15);
                        break;
                     case "PROFIT":
                        entries = this.plugin.getDatabaseManager().getTopPlayersByProfit(currentCurrencyType,
                              currentCurrencyId, 15);
                        break;
                     case "LARGEST_WIN":
                        entries = this.plugin.getDatabaseManager().getTopPlayersByLargestWin(currentCurrencyType,
                              currentCurrencyId, 15);
                        break;
                     case "WORST_PROFIT":
                        entries = this.plugin.getDatabaseManager().getTopPlayersByWorstProfit(currentCurrencyType,
                              currentCurrencyId, 15);
                        break;
                     case "WINSTREAK":
                        entries = this.plugin.getDatabaseManager().getTopPlayersByWinstreak(15);
                        break;
                     default:
                        entries = this.plugin.getDatabaseManager().getTopPlayersByWins(15);
                  }

                  synchronized (this) {
                     if (currentFilter.equals(this.filterType)
                           && currentCurrencyType == this.currencyType
                           && (currentCurrencyId == null ? this.currencyId == null
                                 : currentCurrencyId.equals(this.currencyId))) {
                        this.cachedEntries = entries != null ? new ArrayList<>(entries) : new ArrayList<>();
                     }
                  }

                  FoliaScheduler.runTask(
                        this.plugin,
                        currentViewer,
                        () -> {
                           if (currentViewer != null && currentViewer.isOnline()) {
                              try {
                                 Inventory topInv = GUIHelper.getTopInventorySafely(currentViewer);
                                 if (topInv == null || !topInv.equals(this.getInventory())) {
                                    this.isLoading = false;
                                    return;
                                 }

                                 if (currentFilter.equals(this.filterType)
                                       && currentCurrencyType == this.currencyType
                                       && (currentCurrencyId == null ? this.currencyId == null
                                             : currentCurrencyId.equals(this.currencyId))) {
                                    this.displayPlayerHeads();
                                    super.decorate(currentViewer);
                                    this.plugin.getInventoryUpdateBatcher().scheduleUpdate(currentViewer);
                                    return;
                                 }

                                 this.isLoading = false;
                              } catch (Exception var9) {
                                 return;
                              } finally {
                                 this.isLoading = false;
                              }
                           } else {
                              this.isLoading = false;
                           }
                        });
               } catch (Exception var10) {
                  this.plugin.getLogger().warning("Failed to load leaderboard data: " + var10.getMessage());
                  if (this.plugin.getDebugManager() != null
                        && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     var10.printStackTrace();
                  }

                  this.isLoading = false;
               }
            });
   }

   private void displayPlayerHeads() {
      Inventory inventory = this.getInventory();
      if (inventory != null) {
         for (int slot : this.PLAYER_HEAD_SLOTS) {
            this.getButtonMap().remove(slot);
            if (slot >= 0 && slot < inventory.getSize()) {
               inventory.setItem(slot, null);
            }
         }

         boolean currentlyLoading;
         List<DatabaseManager.LeaderboardEntry> entriesSnapshot;
         synchronized (this) {
            currentlyLoading = this.isLoading;
            entriesSnapshot = new ArrayList<>(this.cachedEntries);
         }

         boolean showLoading = currentlyLoading && entriesSnapshot.isEmpty();
         boolean hasData = !entriesSnapshot.isEmpty();
         if (this.PLAYER_HEAD_SLOTS != null && this.PLAYER_HEAD_SLOTS.length != 0) {
            for (int i = 0; i < this.PLAYER_HEAD_SLOTS.length; i++) {
               int slotx = this.PLAYER_HEAD_SLOTS[i];
               if (slotx >= 0 && slotx < inventory.getSize()) {
                  if (hasData && i < entriesSnapshot.size()) {
                     DatabaseManager.LeaderboardEntry entry = entriesSnapshot.get(i);
                     int rank = i + 1;
                     this.addButton(slotx, new InventoryButton().creator(p -> this.createPlayerHeadItem(entry, rank))
                           .consumer(event -> {
                           }));
                  } else if (showLoading && i == 0) {
                     Material loadingMaterial = MaterialHelper.getBarrierMaterial();
                     String materialName = this.plugin.getGUIConfig().getString("leaderboard-gui.loading.material",
                           "BARRIER");
                     loadingMaterial = this.parseMaterial(materialName, loadingMaterial);
                     ItemStack loadingItem = new ItemStack(loadingMaterial);
                     ItemMeta meta = loadingItem.getItemMeta();
                     if (meta != null) {
                        String loadingTitle = this.plugin.getGUIConfig().getString("leaderboard-gui.loading.title",
                              "&7Loading...");
                        List<String> loadingLore = this.plugin.getGUIConfig()
                              .getStringList("leaderboard-gui.loading.lore");
                        if (loadingLore == null || loadingLore.isEmpty()) {
                           loadingLore = new ArrayList<>();
                           loadingLore.add("&7Please wait while data loads");
                        }

                        this.plugin.getGuiHelper().setDisplayName(meta, loadingTitle);
                        List<?> lore = this.plugin.getGuiHelper().createLore(loadingLore, new HashMap<>());
                        this.plugin.getGuiHelper().setLore(meta, lore);
                        loadingItem.setItemMeta(meta);
                     }

                     this.addButton(slotx, new InventoryButton().creator(p -> loadingItem).consumer(event -> {
                     }));
                  } else {
                     this.addButton(slotx,
                           new InventoryButton().creator(p -> this.createUnknownItem()).consumer(event -> {
                           }));
                  }
               }
            }
         } else {
            this.plugin.getLogger().warning("Player head slots not configured! Cannot display leaderboard.");
         }
      }
   }

   private ItemStack createUnknownItem() {
      Material material = MaterialHelper.getBarrierMaterial();
      String materialName = this.plugin.getGUIConfig().getString("leaderboard-gui.unknown.material", "BARRIER");
      material = this.parseMaterial(materialName, material);
      String title = this.plugin.getGUIConfig().getString("leaderboard-gui.unknown.title", "Unknown");
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("leaderboard-gui.unknown.lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = new ArrayList<>();
         loreTemplate.add("&7No player data available");
      }

      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, new HashMap<>());
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         this.plugin.getGuiHelper().applyItemProperties(meta, "leaderboard-gui.unknown", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createPlayerHeadItem(DatabaseManager.LeaderboardEntry entry, int rank) {
      UUID uuid = entry.getUuid();
      String playerName = entry.getPlayerName();
      double value = entry.getValue();
      Player onlinePlayer = null;

      try {
         Player player = Bukkit.getPlayer(uuid);
         if (player != null && player.isOnline()) {
            onlinePlayer = player;
         }
      } catch (Exception var21) {
      }

      String displayNameTemplate = this.filterType;
      String valueLabel;
      String unit;
      String valueText;
      switch (displayNameTemplate) {
         case "WINS":
            valueLabel = "Wins";
            valueText = String.valueOf((int) value);
            unit = "";
            break;
         case "PROFIT":
            valueLabel = "Profit";
            valueText = this.plugin.getGuiHelper().formatAmount(value, this.currencyId);
            unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
            break;
         case "LARGEST_WIN":
            valueLabel = "Largest Win";
            valueText = this.plugin.getGuiHelper().formatAmount(value, this.currencyId);
            unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
            break;
         case "WORST_PROFIT":
            valueLabel = "Worst Profit";
            valueText = this.plugin.getGuiHelper().formatAmount(value, this.currencyId);
            unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
            break;
         case "WINSTREAK":
            valueLabel = "Winstreak";
            valueText = String.valueOf((int) value);
            unit = "";
            break;
         default:
            valueLabel = "Value";
            valueText = String.valueOf(value);
            unit = "";
      }

      displayNameTemplate = this.plugin.getGUIConfig().getString("leaderboard-gui.player-head.display-name",
            "&6&l#" + rank + " &e" + playerName);
      String rankStr = String.valueOf(rank);
      StringBuilder displayNameBuilder = new StringBuilder(
            displayNameTemplate.length() + playerName.length() + rankStr.length());
      displayNameBuilder.append(displayNameTemplate);

      int index;
      while ((index = displayNameBuilder.indexOf("<rank>")) != -1) {
         displayNameBuilder.replace(index, index + 6, rankStr);
      }

      while ((index = displayNameBuilder.indexOf("<player>")) != -1) {
         displayNameBuilder.replace(index, index + 8, playerName);
      }

      String displayName = displayNameBuilder.toString();
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("leaderboard-gui.player-head.lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = new ArrayList<>();
         loreTemplate.add("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         loreTemplate.add("&f" + valueLabel + ": &a<value><unit>");
         loreTemplate.add("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      }

      Map<String, String> placeholders = new HashMap<>();
      placeholders.put("rank", String.valueOf(rank));
      placeholders.put("player", playerName);
      placeholders.put("value", valueText);
      placeholders.put("unit", unit);
      placeholders.put("value_label", valueLabel);
      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders,
            onlinePlayer != null ? onlinePlayer : this.viewer);
      Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
      if (playerHeadMaterial == null) {
         this.plugin.getLogger().warning("Failed to parse PLAYER_HEAD material for leaderboard head!");
         Material barrierMaterial = MaterialHelper.getBarrierMaterial();
         return new ItemStack(
               barrierMaterial != null ? barrierMaterial : MaterialHelper.parseMaterial("BARRIER", null));
      } else {
         return this.plugin.getGuiHelper().createPlayerHead(playerHeadMaterial, onlinePlayer, null,
               onlinePlayer != null, displayName, lore, null, null);
      }
   }

   private void updateFilterButton() {
      Inventory inventory = this.getInventory();
      if (inventory != null) {
         ItemStack filterItem = this.createFilterButton();
         if (this.FILTER_SLOT >= 0 && this.FILTER_SLOT < inventory.getSize()) {
            inventory.setItem(this.FILTER_SLOT, filterItem);
         }
      }
   }

   private void updateCurrencyButton() {
      Inventory inventory = this.getInventory();
      if (inventory != null) {
         this.addButton(this.CURRENCY_SLOT,
               new InventoryButton().creator(p -> this.createCurrencyButton()).consumer(event -> {
                  List<LeaderboardGUI.CurrencyItem> enabledCurrencies = this.getEnabledCurrencies();
                  if (enabledCurrencies.isEmpty()) {
                     this.plugin.getLogger().warning("No enabled currencies found for leaderboard!");
                  } else {
                     int currentIndex = -1;

                     for (int i = 0; i < enabledCurrencies.size(); i++) {
                        LeaderboardGUI.CurrencyItem item = enabledCurrencies.get(i);
                        boolean typeMatches = item.type == this.currencyType;
                        boolean idMatches;
                        if (this.currencyId == null && item.currencyId == null) {
                           idMatches = true;
                        } else if (this.currencyId != null && item.currencyId != null) {
                           idMatches = this.currencyId.equals(item.currencyId);
                        } else {
                           idMatches = false;
                        }

                        if (typeMatches && idMatches) {
                           currentIndex = i;
                           break;
                        }
                     }

                     if (currentIndex == -1) {
                        currentIndex = 0;
                     }

                     int nextIndex = (currentIndex + 1) % enabledCurrencies.size();
                     LeaderboardGUI.CurrencyItem nextCurrency = enabledCurrencies.get(nextIndex);
                     this.currencyType = nextCurrency.type;
                     this.currencyId = nextCurrency.currencyId;
                     synchronized (this) {
                        this.cachedEntries.clear();
                        this.isLoading = false;
                     }

                     FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                        if (this.viewer != null && this.viewer.isOnline()) {
                           try {
                              Inventory topInv = GUIHelper.getTopInventorySafely(this.viewer);
                              if (topInv != null && topInv.equals(this.getInventory())) {
                                 this.updateCurrencyButton();
                                 this.updateFilterButton();
                                 this.displayPlayerHeads();
                                 super.decorate(this.viewer);
                                 this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                              }
                           } catch (Exception var2x) {
                           }
                        }
                     });
                     this.loadLeaderboardData();
                  }
               }));
         ItemStack currencyItem = this.createCurrencyButton();
         if (this.CURRENCY_SLOT >= 0 && this.CURRENCY_SLOT < inventory.getSize()) {
            inventory.setItem(this.CURRENCY_SLOT, currencyItem);
         }
      }
   }

   private ItemStack createFilterButton() {
      String materialName = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.material", "OAK_SIGN");
      Material material = MaterialHelper.parseMaterial(materialName, Material.PAPER);
      String title = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.title", "&6&lChange Filter");
      String filterTypeValue = this.filterType;
      String filterDisplayName;
      switch (filterTypeValue) {
         case "WINS":
            filterDisplayName = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.names.wins",
                  "Number of Wins");
            break;
         case "PROFIT":
            filterDisplayName = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.names.profit",
                  "Most Profit");
            break;
         case "LARGEST_WIN":
            filterDisplayName = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.names.largest-win",
                  "Largest Win");
            break;
         case "WORST_PROFIT":
            filterDisplayName = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.names.worst-profit",
                  "Worst Profit");
            break;
         case "WINSTREAK":
            filterDisplayName = this.plugin.getGUIConfig().getString("leaderboard-gui.filter.names.winstreak",
                  "Winstreak");
            break;
         default:
            filterDisplayName = "Unknown";
      }

      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("leaderboard-gui.filter.lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = new ArrayList<>();
         loreTemplate.add("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         loreTemplate.add("&fCurrent Filter: &e<filter>");
         loreTemplate.add("&7Click to change filter");
         loreTemplate.add("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      }

      Map<String, String> placeholders = new HashMap<>();
      placeholders.put("filter", filterDisplayName);
      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         this.plugin.getGuiHelper().applyItemProperties(meta, "leaderboard-gui.filter", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createCurrencyButton() {
      Material material = Material.GOLD_INGOT;
      String materialName = this.plugin.getGUIConfig().getString("leaderboard-gui.currency.material", "GOLD_INGOT");
      material = this.parseMaterial(materialName, material);
      String title = this.plugin.getGUIConfig().getString("leaderboard-gui.currency.title", "&6&lChange Currency");
      String currencyDisplayName = this.plugin.getCurrencyManager().getDisplayName(this.currencyType, this.currencyId);
      String unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("leaderboard-gui.currency.lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = new ArrayList<>();
         loreTemplate.add("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         loreTemplate.add("&fCurrent Currency: &e<currency>");
         loreTemplate.add("&7Click to change currency");
         loreTemplate.add("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
      }

      Map<String, String> placeholders = new HashMap<>();
      placeholders.put("currency", currencyDisplayName);
      placeholders.put("unit", unit);
      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         this.plugin.getGuiHelper().applyItemProperties(meta, "leaderboard-gui.currency", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createBackButton() {
      Material material = Material.NETHER_STAR;
      String materialName = this.plugin.getGUIConfig().getString("leaderboard-gui.back.material", "NETHER_STAR");
      material = this.parseMaterial(materialName, material);
      String title = this.plugin.getGUIConfig().getString("leaderboard-gui.back.title", "&6&lGo Back");
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("leaderboard-gui.back.lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = new ArrayList<>();
         loreTemplate.add("&7Click to return to main menu");
      }

      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, new HashMap<>());
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         this.plugin.getGuiHelper().applyItemProperties(meta, "leaderboard-gui.back", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private List<LeaderboardGUI.CurrencyItem> getEnabledCurrencies() {
      List<LeaderboardGUI.CurrencyItem> currencies = new ArrayList<>();
      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.MONEY)) {
         currencies.add(new LeaderboardGUI.CurrencyItem(CoinFlipGame.CurrencyType.MONEY, null));
      }

      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.PLAYERPOINTS)) {
         currencies.add(new LeaderboardGUI.CurrencyItem(CoinFlipGame.CurrencyType.PLAYERPOINTS, null));
      }

      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.TOKENMANAGER)) {
         currencies.add(new LeaderboardGUI.CurrencyItem(CoinFlipGame.CurrencyType.TOKENMANAGER, null));
      }

      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.BEASTTOKENS)) {
         currencies.add(new LeaderboardGUI.CurrencyItem(CoinFlipGame.CurrencyType.BEASTTOKENS, null));
      }

      for (String id : this.plugin.getCurrencyManager().getEnabledCoinsEngineCurrencyIds()) {
         currencies.add(new LeaderboardGUI.CurrencyItem(CoinFlipGame.CurrencyType.COINSENGINE, id));
      }

      for (String id : this.plugin.getCurrencyManager().getEnabledPlaceholderCurrencyIds()) {
         currencies.add(new LeaderboardGUI.CurrencyItem(CoinFlipGame.CurrencyType.PLACEHOLDER, id));
      }

      return currencies;
   }

   private List<String> getEnabledFilters() {
      List<String> enabledFilters = new ArrayList<>();
      if (this.plugin.getConfig().getBoolean("leaderboard.filters.wins", true)) {
         enabledFilters.add("WINS");
      }

      if (this.plugin.getConfig().getBoolean("leaderboard.filters.profit", true)) {
         enabledFilters.add("PROFIT");
      }

      if (this.plugin.getConfig().getBoolean("leaderboard.filters.largest-win", true)) {
         enabledFilters.add("LARGEST_WIN");
      }

      if (this.plugin.getConfig().getBoolean("leaderboard.filters.worst-profit", true)) {
         enabledFilters.add("WORST_PROFIT");
      }

      if (this.plugin.getConfig().getBoolean("leaderboard.filters.winstreak", true)) {
         enabledFilters.add("WINSTREAK");
      }

      return enabledFilters;
   }

   @Override
   public void onClose(InventoryCloseEvent event) {
      synchronized (this) {
         this.isLoading = false;
         this.cachedEntries.clear();
      }

      super.onClose(event);
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
