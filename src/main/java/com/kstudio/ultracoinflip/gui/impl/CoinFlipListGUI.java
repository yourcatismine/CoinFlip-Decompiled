package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.HouseCoinFlipManager;
import com.kstudio.ultracoinflip.data.PlayerStats;
import com.kstudio.ultracoinflip.database.DatabaseManager;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.security.ExploitDetector;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CoinFlipListGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;
   private int page;
   private String filterType;
   private Object refreshTaskId = null;
   private int lastDisplayedGamesCount = 0;
   private static List<String> cachedGameItemLoreTemplate = null;
   private static List<String> cachedStatsItemLoreTemplate = null;
   private static int lastConfigVersion = -1;
   private static final Object joinGameLock = new Object();
   private static final Set<UUID> playersJoiningGame = ConcurrentHashMap.newKeySet();
   private static final Set<UUID> gamesBeingJoined = ConcurrentHashMap.newKeySet();

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-list";
   }

   public CoinFlipListGUI(KStudio plugin, Player viewer, int page) {
      this(plugin, viewer, page, "NONE");
   }

   public CoinFlipListGUI(KStudio plugin, Player viewer, int page, String filterType) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.page = page;
      this.filterType = filterType != null ? filterType : "NONE";
   }

   public Player getViewer() {
      return this.viewer;
   }

   private Material parseMaterial(String materialName, Material fallback) {
      return MaterialHelper.parseMaterial(materialName, fallback);
   }

   @Override
   protected Inventory createInventory() {
      String titleTemplate = this.plugin.getGUIConfig().getString("list-gui.title", "&8UltraCoinFlip &8[&7Page <page>&8]");
      int size = this.plugin.getGUIConfig().getInt("list-gui.size", 45);
      Map<String, String> placeholders = Collections.singletonMap("page", String.valueOf(this.page));
      return this.plugin.getGuiHelper().createInventory(null, size, titleTemplate, placeholders);
   }

   @Override
   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         Inventory inventory = this.getInventory();
         if (inventory != null) {
            List<CoinFlipGame> allGames = this.plugin.getCoinFlipManager().getAllGames();
            if (allGames == null) {
               allGames = new ArrayList<>();
            }

            int gamesVersion = this.plugin.getCoinFlipManager().getGamesListVersion();
            allGames = this.plugin.getFilteredGamesCache().getFilteredGames(allGames, this.filterType, gamesVersion);
            allGames = this.applyCurrencyRestrictionFilter(allGames);
            int elementsPerPage = this.plugin.getGUIConfig().getInt("list-gui.items-per-page", 21);
            int startIndex = (this.page - 1) * elementsPerPage;
            String fillerMaterialName = this.plugin.getGUIConfig().getString("list-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
            String fillerDisplayName = this.plugin.getGUIConfig().getString("list-gui.filler.display-name", " ");
            ItemStack filler = this.plugin.getItemStackPool().getFillerItem(fillerMaterialName, fillerDisplayName);
            if (filler == null) {
               filler = MaterialHelper.createItemStack(fillerMaterialName);
               if (filler == null) {
                  filler = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
               }

               if (filler == null) {
                  filler = new ItemStack(Material.GLASS_PANE);
               }

               ItemMeta fillerMeta = filler.getItemMeta();
               this.plugin.getGuiHelper().setDisplayName(fillerMeta, fillerDisplayName);
               filler.setItemMeta(fillerMeta);
               this.plugin.getItemStackPool().poolFillerItem(fillerMaterialName, fillerDisplayName, filler);
            }

            int prevSlot = this.plugin.getGUIConfig().getInt("list-gui.navigation.previous.slot", 18);
            int nextSlot = this.plugin.getGUIConfig().getInt("list-gui.navigation.next.slot", 26);
            boolean hasPrevPage = this.page > 1;
            boolean hasNextPage = startIndex + elementsPerPage < allGames.size();
            int statsSlot = this.plugin.getGUIConfig().getInt("list-gui.stats.slot", 4);
            int leaderboardSlot = this.plugin.getGUIConfig().getInt("list-gui.leaderboard.slot", 38);
            int historySlot = this.plugin.getGUIConfig().getInt("list-gui.history.slot", 39);
            int createSlot = this.plugin.getGUIConfig().getInt("list-gui.create.slot", 40);
            int filterSlot = this.plugin.getGUIConfig().getInt("list-gui.filter.slot", 41);
            int houseSlot = this.plugin.getGUIConfig().getInt("list-gui.house.slot", 42);
            List<Integer> fillerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "list-gui.filler.slots");
            Set<Integer> excludedSlots = new HashSet<>();
            excludedSlots.add(statsSlot);
            excludedSlots.add(leaderboardSlot);
            excludedSlots.add(historySlot);
            excludedSlots.add(createSlot);
            excludedSlots.add(houseSlot);
            excludedSlots.add(filterSlot);
            int[] fillerSlots = fillerSlotsList.isEmpty()
               ? new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 42, 43, 44}
               : fillerSlotsList.stream()
                  .mapToInt(i -> i != null ? i : -1)
                  .filter(i -> i >= 0 && i < this.getInventory().getSize() && !excludedSlots.contains(i))
                  .toArray();

            for (int slot : fillerSlots) {
               if (slot >= 0 && slot < inventory.getSize()) {
                  inventory.setItem(slot, filler);
               }
            }

            List<Integer> gameSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "list-gui.game-slots");
            int[] gameSlots = gameSlotsList.isEmpty()
               ? new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34}
               : gameSlotsList.stream().mapToInt(i -> i != null ? i : -1).filter(i -> i >= 0 && i < inventory.getSize()).toArray();
            Map<Integer, InventoryButton> buttonMap = this.getButtonMap();

            for (int slotx : gameSlots) {
               if (buttonMap != null) {
                  buttonMap.remove(slotx);
               }

               if (slotx >= 0 && slotx < inventory.getSize()) {
                  inventory.setItem(slotx, null);
               }
            }

            for (int i = 0; i < elementsPerPage && startIndex + i < allGames.size() && i < gameSlots.length; i++) {
               CoinFlipGame game = allGames.get(startIndex + i);
               int slotx = gameSlots[i];
               if (slotx >= 0 && slotx < inventory.getSize()) {
                  this.addButton(
                     slotx,
                     new InventoryButton()
                        .creator(p -> this.createGameItem(game))
                        .consumer(event -> this.handleGameClick(event, (Player)event.getWhoClicked(), game))
                  );
               }
            }

            PlayerStats stats = this.plugin.getCoinFlipManager().getStats(player.getUniqueId());
            this.addButton(statsSlot, new InventoryButton().creator(p -> this.createStatsItem(stats)).consumer(event -> {}));
            String leaderboardMaterialName = this.plugin.getGUIConfig().getString("list-gui.leaderboard.material", "GOLD_INGOT");
            Material leaderboardMaterial = this.parseMaterial(leaderboardMaterialName, Material.GOLD_INGOT);
            String leaderboardTitle = this.plugin.getGUIConfig().getString("list-gui.leaderboard.title", "&r&6&lLeaderboard");
            List<String> leaderboardLore = this.plugin.getGUIConfig().getStringList("list-gui.leaderboard.lore");
            this.addButton(
               leaderboardSlot,
               new InventoryButton().creator(p -> this.createLeaderboardButton(leaderboardMaterial, leaderboardTitle, leaderboardLore)).consumer(event -> {
                  this.logButtonClick("Leaderboard", event);
                  Player clicker = (Player)event.getWhoClicked();

                  try {
                     GUIHelper.setCursorSafely(clicker, null);
                     clicker.setItemOnCursor(null);
                  } catch (Exception var4x) {
                  }

                  this.logGuiDebug(String.format("[Leaderboard:opening] player=%s", clicker.getName()));
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

                              this.logGuiDebug("Opening leaderboard GUI from main menu for " + this.viewer.getName());
                              this.plugin.getGuiManager().openGUI(new LeaderboardGUI(this.plugin, this.viewer), this.viewer);
                           }
                        }, 2L);
                     }
                  });
               })
            );
            String historyMaterialName = this.plugin.getGUIConfig().getString("list-gui.history.material", "BOOK");
            Material historyMaterial = this.parseMaterial(historyMaterialName, MaterialHelper.getBookMaterial());
            String historyTitle = this.plugin.getGUIConfig().getString("list-gui.history.title", "&r&6&lYour CoinFlip History");
            List<String> historyLore = this.plugin.getGUIConfig().getStringList("list-gui.history.lore");
            this.addButton(
               historySlot, new InventoryButton().creator(p -> this.createHistoryButton(historyMaterial, historyTitle, historyLore)).consumer(event -> {
                  this.logButtonClick("History", event);
                  Player clicker = (Player)event.getWhoClicked();

                  try {
                     GUIHelper.setCursorSafely(clicker, null);
                     clicker.setItemOnCursor(null);
                  } catch (Exception var4x) {
                  }

                  this.logGuiDebug(String.format("[History:opening] player=%s", clicker.getName()));
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

                              this.logGuiDebug("Opening history GUI (page 1) from main menu for " + this.viewer.getName());
                              this.plugin.getGuiManager().openGUI(new CoinFlipHistoryGUI(this.plugin, this.viewer, 1), this.viewer);
                           }
                        }, 2L);
                     }
                  });
               })
            );
            String createMaterialName = this.plugin.getGUIConfig().getString("list-gui.create.material", "NETHER_STAR");
            Material createMaterial = this.parseMaterial(createMaterialName, Material.NETHER_STAR);
            String createTitle = this.plugin.getGUIConfig().getString("list-gui.create.title", "&r&6&lCreate CoinFlip");
            List<String> createLore = this.plugin.getGUIConfig().getStringList("list-gui.create.lore");
            this.addButton(createSlot, new InventoryButton().creator(p -> this.createCreateButton(createMaterial, createTitle, createLore)).consumer(event -> {
               this.logButtonClick("Create", event);
               Player clicker = (Player)event.getWhoClicked();

               try {
                  GUIHelper.setCursorSafely(clicker, null);
                  clicker.setItemOnCursor(null);
               } catch (Exception var4x) {
               }

               this.logGuiDebug(String.format("[Create:opening] player=%s", clicker.getName()));
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

                           this.logGuiDebug("Opening create GUI from main menu for " + this.viewer.getName());
                           this.plugin.getGuiManager().openGUI(new CreateCoinFlipGUI(this.plugin, this.viewer), this.viewer);
                        }
                     }, 2L);
                  }
               });
            }));
            HouseCoinFlipManager houseManager = this.plugin.getHouseCoinFlipManager();
            if (houseManager != null && houseManager.isEnabled()) {
               String houseMaterialName = this.plugin
                  .getConfig()
                  .getString("house.display.material", this.plugin.getGUIConfig().getString("list-gui.house.material", "PLAYER_HEAD"));
               Material houseMaterial = this.parseMaterial(
                  houseMaterialName, MaterialHelper.getPlayerHeadMaterial() != null ? MaterialHelper.getPlayerHeadMaterial() : Material.DIAMOND
               );
               String houseTitle = this.plugin.getGUIConfig().getString("list-gui.house.title", "&r&6&lPlay with Bot");
               List<String> houseLore = this.plugin.getGUIConfig().getStringList("list-gui.house.lore");
               this.addButton(
                  houseSlot, new InventoryButton().creator(p -> this.createHouseButton(p, houseMaterial, houseTitle, houseLore)).consumer(event -> {
                     this.logButtonClick("House", event);
                     Player clicker = (Player)event.getWhoClicked();

                     try {
                        GUIHelper.setCursorSafely(clicker, null);
                        clicker.setItemOnCursor(null);
                     } catch (Exception var4x) {
                     }

                     this.logGuiDebug(String.format("[House:opening] player=%s", clicker.getName()));
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

                                 this.logGuiDebug("Opening create GUI (house mode) from main menu for " + this.viewer.getName());
                                 CreateCoinFlipGUI createGUI = new CreateCoinFlipGUI(this.plugin, this.viewer);
                                 createGUI.setHouseMode(true);
                                 this.plugin.getGuiManager().openGUI(createGUI, this.viewer);
                              }
                           }, 2L);
                        }
                     });
                  })
               );
            }

            String filterMaterialName = this.plugin.getGUIConfig().getString("list-gui.filter.material", "HOPPER");
            Material filterMaterial = this.parseMaterial(filterMaterialName, Material.HOPPER);
            String filterTitle = this.plugin.getGUIConfig().getString("list-gui.filter.title", "&r&6&lFilter Games");
            List<String> filterLoreTemplate = this.plugin.getGUIConfig().getStringList("list-gui.filter.lore");
            this.addButton(
               filterSlot,
               new InventoryButton()
                  .creator(p -> this.createFilterButton(filterMaterial, filterTitle, filterLoreTemplate, this.filterType))
                  .consumer(event -> {
                     this.logButtonClick("Filter", event);
                     String finalNextFilter = this.filterType;
                     String nextFilter;
                     switch (finalNextFilter) {
                        case "NONE":
                           nextFilter = "TIME_NEWEST";
                           break;
                        case "TIME_NEWEST":
                           nextFilter = "AMOUNT_ASCENDING";
                           break;
                        case "AMOUNT_ASCENDING":
                           nextFilter = "AMOUNT_DESCENDING";
                           break;
                        case "AMOUNT_DESCENDING":
                           nextFilter = "NONE";
                           break;
                        default:
                           nextFilter = "NONE";
                     }

                     finalNextFilter = nextFilter;
                     Player clicker = (Player)event.getWhoClicked();
                     this.logGuiDebug(String.format("[Filter:updating] player=%s filter=%s->%s", clicker.getName(), this.filterType, finalNextFilter));
                     this.filterType = finalNextFilter;
                     this.updateFilterButton();
                     final String filterToApply = finalNextFilter;
                     FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                        if (this.viewer != null && this.viewer.isOnline()) {
                           try {
                              if (!this.hasChestInventoryOpen(this.viewer)) {
                                 return;
                              }
                           } catch (Exception var3x) {
                              return;
                           }

                           this.logGuiDebug("Applying filter '" + filterToApply + "' for " + this.viewer.getName());
                           this.decorate(this.viewer);
                           this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                        }
                     });
                  })
            );
            if (hasPrevPage) {
               Map<String, String> navPlaceholders = new HashMap<>();
               navPlaceholders.put("page", String.valueOf(this.page - 1));
               String prevText = this.plugin
                  .getAdventureHelper()
                  .parseToLegacy(this.plugin.getGUIConfig().getString("list-gui.navigation.previous.text", "&cPrevious &8(&e<page>&8)"), navPlaceholders);
               String prevMaterialName = this.plugin.getGUIConfig().getString("list-gui.navigation.previous.material", "PLAYER_HEAD");
               Material playerHeadFallback = MaterialHelper.getPlayerHeadMaterial();
               Material prevMaterial = this.parseMaterial(prevMaterialName, playerHeadFallback);
               String prevBase64 = this.plugin.getGUIConfig().getString("list-gui.navigation.previous.base64-texture", "");
               this.addButton(
                  prevSlot,
                  new InventoryButton()
                     .creator(p -> this.createNavigationItem(prevMaterial, prevText, prevBase64, "list-gui.navigation.previous"))
                     .consumer(event -> {
                        Player clicker = (Player)event.getWhoClicked();
                        this.plugin.getSoundHelper().playSound(clicker, "gui.page-change");
                        this.logGuiDebug(String.format("[PrevPage:updating] player=%s page=%d->%d", clicker.getName(), this.page, this.page - 1));
                        this.page--;
                        FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                           if (this.viewer != null && this.viewer.isOnline()) {
                              if (this.hasChestInventoryOpen(this.viewer)) {
                                 this.decorate(this.viewer);
                                 this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                              }
                           }
                        });
                     })
               );
            }

            if (hasNextPage) {
               Map<String, String> navPlaceholders = new HashMap<>();
               navPlaceholders.put("page", String.valueOf(this.page + 1));
               String nextText = this.plugin
                  .getAdventureHelper()
                  .parseToLegacy(this.plugin.getGUIConfig().getString("list-gui.navigation.next.text", "&aNext Page &8(&e<page>&8)"), navPlaceholders);
               String nextMaterialName = this.plugin.getGUIConfig().getString("list-gui.navigation.next.material", "PLAYER_HEAD");
               Material nextMaterial = this.parseMaterial(nextMaterialName, MaterialHelper.getPlayerHeadMaterial());
               String nextBase64 = this.plugin.getGUIConfig().getString("list-gui.navigation.next.base64-texture", "");
               this.addButton(
                  nextSlot,
                  new InventoryButton()
                     .creator(p -> this.createNavigationItem(nextMaterial, nextText, nextBase64, "list-gui.navigation.next"))
                     .consumer(event -> {
                        Player clicker = (Player)event.getWhoClicked();
                        this.plugin.getSoundHelper().playSound(clicker, "gui.page-change");
                        this.logGuiDebug(String.format("[NextPage:updating] player=%s page=%d->%d", clicker.getName(), this.page, this.page + 1));
                        this.page++;
                        FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                           if (this.viewer != null && this.viewer.isOnline()) {
                              if (this.hasChestInventoryOpen(this.viewer)) {
                                 this.decorate(this.viewer);
                                 this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                              }
                           }
                        });
                     })
               );
            }

            super.decorate(player);
            this.lastDisplayedGamesCount = allGames.size();
            boolean autoRefresh = this.plugin.getGUIConfig().getBoolean("list-gui.auto-refresh", true);
            if (autoRefresh) {
               this.startAutoRefresh(allGames);
            }
         }
      }
   }

   private void logButtonClick(String buttonName, InventoryClickEvent event) {
      if (event != null && this.viewer != null) {
         try {
            boolean clickingTop = false;
            int topSize = 0;
            Inventory topInv = GUIHelper.getTopInventorySafely(this.viewer);
            if (topInv != null) {
               topSize = topInv.getSize();
               clickingTop = event.getRawSlot() < topSize;
            }

            String message = String.format(
               "%s button click by %s | slot=%d raw=%d click=%s action=%s cursor=%s current=%s inTop=%s topSize=%d filter=%s page=%d",
               buttonName,
               this.viewer.getName(),
               event.getSlot(),
               event.getRawSlot(),
               event.getClick(),
               event.getAction(),
               this.describeItem(event.getCursor()),
               this.describeItem(event.getCurrentItem()),
               clickingTop,
               topSize,
               this.filterType,
               this.page
            );
            this.logGuiDebug(message);
         } catch (Exception var7) {
         }
      }
   }

   private String describeItem(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? item.getType().name() + "x" + item.getAmount() : "empty";
   }

   private void logGuiDebug(String message) {
      if (this.plugin != null && message != null) {
         try {
            if (this.plugin.getDebugManager() != null) {
               this.plugin.getDebugManager().debug(DebugManager.Category.GUI, DebugManager.Level.INFO, "[CoinFlipListGUI] " + message);
            }
         } catch (Exception var3) {
         }
      }
   }

   private void startAutoRefresh(List<CoinFlipGame> currentGames) {
      if (this.refreshTaskId != null) {
         FoliaScheduler.cancelTask(this.plugin, this.refreshTaskId);
         this.refreshTaskId = null;
      }

      if (this.viewer != null && this.viewer.isOnline()) {
         int refreshInterval = this.plugin.getGUIConfig().getInt("list-gui.refresh-interval", 20);
         if (refreshInterval <= 0) {
            refreshInterval = 20;
         }

         String currentFilterType = this.filterType;
         int currentPage = this.page;
         this.refreshTaskId = FoliaScheduler.runTaskTimer(this.plugin, this.viewer, () -> {
            if (this.viewer != null && this.viewer.isOnline()) {
               try {
                  if (!this.hasChestInventoryOpen(this.viewer)) {
                     this.stopAutoRefresh();
                     return;
                  }

                  String currentTitle = this.getOpenInventoryTitle(this.viewer);
                  if (currentTitle == null) {
                     this.stopAutoRefresh();
                     return;
                  }

                  String listGuiTitleTemplate = this.plugin.getGUIConfig().getString("list-gui.title", "&8UltraCoinFlip &8[&7Page <page>&8]");
                  String strippedTemplate = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', listGuiTitleTemplate.replace("<page>", "")));
                  String strippedCurrent = ChatColor.stripColor(currentTitle);
                  String basePattern = strippedTemplate.split("\\d")[0].trim();
                  if (basePattern.isEmpty()) {
                     basePattern = strippedTemplate.substring(0, Math.min(10, strippedTemplate.length()));
                  }

                  if (!strippedCurrent.contains(basePattern) && !strippedCurrent.toLowerCase().contains("page")) {
                     this.stopAutoRefresh();
                     return;
                  }

                  List<CoinFlipGame> latestGames = this.plugin.getCoinFlipManager().getAllGames();
                  if (latestGames == null) {
                     latestGames = new ArrayList<>();
                  }

                  List<CoinFlipGame> filteredGames = this.applyFilter(latestGames);
                  if (filteredGames.size() != this.lastDisplayedGamesCount) {
                     this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, this.viewer, currentPage, currentFilterType), this.viewer);
                     return;
                  }
               } catch (Exception var10) {
                  if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Error in auto-refresh task: " + var10.getMessage());
                  }

                  this.stopAutoRefresh();
               }
            } else {
               this.stopAutoRefresh();
            }
         }, (long)refreshInterval, (long)refreshInterval);
      }
   }

   private long computeGamesHash(List<CoinFlipGame> games) {
      long hash = 0L;

      for (CoinFlipGame game : games) {
         long gameHash = game.getGameId().hashCode();
         UUID hostUuid = game.getHostUuid();
         gameHash = gameHash * 31L + (hostUuid != null ? hostUuid.hashCode() : 0);
         gameHash = gameHash * 31L + Double.hashCode(game.getAmount());
         gameHash = gameHash * 31L + game.getCurrencyType().hashCode();
         String currencyId = game.getCurrencyId();
         gameHash = gameHash * 31L + (currencyId != null ? currencyId.hashCode() : 0);
         hash = hash * 31L + gameHash;
      }

      return hash;
   }

   private boolean hasGamesChanged(List<CoinFlipGame> oldGames, List<CoinFlipGame> newGames) {
      if (oldGames.size() != newGames.size()) {
         return true;
      } else {
         Set<UUID> newGameIds = new HashSet<>(newGames.size());
         Map<UUID, CoinFlipGame> newGamesMap = new HashMap<>(newGames.size());

         for (CoinFlipGame newGame : newGames) {
            UUID id = newGame.getGameId();
            newGameIds.add(id);
            newGamesMap.put(id, newGame);
         }

         for (CoinFlipGame oldGame : oldGames) {
            UUID id = oldGame.getGameId();
            if (!newGameIds.contains(id)) {
               return true;
            }

            CoinFlipGame newGame = newGamesMap.get(id);
            if (newGame == null) {
               return true;
            }

            if (oldGame.getAmount() == newGame.getAmount() && oldGame.getCurrencyType() == newGame.getCurrencyType()) {
               String oldCurrencyId = oldGame.getCurrencyId();
               String newCurrencyId = newGame.getCurrencyId();
               if (oldCurrencyId == null ? newCurrencyId == null : oldCurrencyId.equals(newCurrencyId)) {
                  continue;
               }

               return true;
            }

            return true;
         }

         return false;
      }
   }

   private void stopAutoRefresh() {
      if (this.refreshTaskId != null) {
         FoliaScheduler.cancelTask(this.plugin, this.refreshTaskId);
         this.refreshTaskId = null;
      }
   }

   private boolean hasChestInventoryOpen(Player player) {
      if (player != null && player.isOnline()) {
         try {
            Method getOpenInvMethod = player.getClass().getMethod("getOpenInventory");
            Object view = getOpenInvMethod.invoke(player);
            if (view == null) {
               return false;
            } else {
               Method getTopMethod = view.getClass().getMethod("getTopInventory");
               Inventory topInv = (Inventory)getTopMethod.invoke(view);
               if (topInv == null) {
                  return false;
               } else {
                  try {
                     return topInv.getType() == InventoryType.CHEST;
                  } catch (IncompatibleClassChangeError var8) {
                     int size = topInv.getSize();
                     return size > 0 && size <= 54;
                  }
               }
            }
         } catch (Exception var9) {
            return false;
         }
      } else {
         return false;
      }
   }

   private String getOpenInventoryTitle(Player player) {
      if (player != null && player.isOnline()) {
         try {
            Method getOpenInvMethod = player.getClass().getMethod("getOpenInventory");
            Object view = getOpenInvMethod.invoke(player);
            if (view == null) {
               return null;
            } else {
               Method getTitleMethod = view.getClass().getMethod("getTitle");
               return (String)getTitleMethod.invoke(view);
            }
         } catch (Exception var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   public void refreshGUI() {
      if (this.viewer != null && this.viewer.isOnline()) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin
               .getDebugManager()
               .verbose(DebugManager.Category.GUI, "[CoinFlipListGUI] refreshGUI() called for " + this.viewer.getName() + ", page=" + this.page);
         }

         FoliaScheduler.runTask(
            this.plugin,
            this.viewer,
            () -> {
               if (this.viewer != null && this.viewer.isOnline()) {
                  try {
                     if (!this.hasChestInventoryOpen(this.viewer)) {
                        return;
                     }

                     String currentTitle = this.getOpenInventoryTitle(this.viewer);
                     if (currentTitle == null) {
                        return;
                     }

                     String listGuiTitleTemplate = this.plugin.getGUIConfig().getString("list-gui.title", "&8UltraCoinFlip &8[&7Page <page>&8]");
                     String strippedTemplate = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', listGuiTitleTemplate.replace("<page>", "")));
                     String strippedCurrent = ChatColor.stripColor(currentTitle);
                     String basePattern = strippedTemplate.split("\\d")[0].trim();
                     if (basePattern.isEmpty()) {
                        basePattern = strippedTemplate.substring(0, Math.min(10, strippedTemplate.length()));
                     }

                     if (!strippedCurrent.contains(basePattern) && !strippedCurrent.toLowerCase().contains("page")) {
                        if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                           this.plugin
                              .getDebugManager()
                              .verbose(
                                 DebugManager.Category.GUI,
                                 "[CoinFlipListGUI] Skipping refresh for " + this.viewer.getName() + " (different GUI open: '" + strippedCurrent + "')"
                              );
                        }
                     } else {
                        if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                           this.plugin
                              .getDebugManager()
                              .verbose(
                                 DebugManager.Category.GUI,
                                 "[CoinFlipListGUI] Refreshing GUI for " + this.viewer.getName() + " (title matches: '" + strippedCurrent + "')"
                              );
                        }

                        this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, this.viewer, this.page, this.filterType), this.viewer);
                     }
                  } catch (Exception var6) {
                     if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                        this.plugin
                           .getDebugManager()
                           .verbose(
                              DebugManager.Category.GUI,
                              "[CoinFlipListGUI] Error checking inventory for " + this.viewer.getName() + ": " + var6.getMessage() + ", skipping refresh"
                           );
                     }
                  }
               }
            }
         );
      }
   }

   public static void refreshAllViewers(KStudio plugin) {
      if (plugin != null && plugin.getGuiManager() != null) {
         if (plugin.getDebugManager() != null && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            plugin.getDebugManager().verbose(DebugManager.Category.GUI, "[CoinFlipListGUI] refreshAllViewers() called");
         }

         int[] count = new int[]{0};
         plugin.getGuiManager()
            .forEachHandler(
               CoinFlipListGUI.class,
               gui -> {
                  count[0]++;

                  try {
                     gui.refreshGUI();
                  } catch (Exception var4) {
                     if (plugin.getDebugManager() != null && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                        plugin.getDebugManager()
                           .verbose(
                              DebugManager.Category.GUI,
                              "Error refreshing CoinFlipListGUI for "
                                 + (gui.getViewer() != null ? gui.getViewer().getName() : "unknown")
                                 + ": "
                                 + var4.getMessage()
                           );
                     }
                  }
               }
            );
         if (plugin.getDebugManager() != null && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            plugin.getDebugManager().verbose(DebugManager.Category.GUI, "[CoinFlipListGUI] refreshAllViewers() found " + count[0] + " handlers");
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent event) {
      this.stopAutoRefresh();
      super.onClose(event);
   }

   private ItemStack createGameItem(CoinFlipGame game) {
      if (game != null && game.getHost() != null) {
         String materialName = this.plugin.getGUIConfig().getString("game-item.material", "PLAYER_HEAD");
         Material playerHeadFallback = MaterialHelper.getPlayerHeadMaterial();
         Material material = this.parseMaterial(materialName, playerHeadFallback);
         String unit = this.plugin.getCurrencyManager().getUnit(game.getCurrencyType(), game.getCurrencyId());
         String typeDisplayName;
         if (game.getCurrencyType() == CoinFlipGame.CurrencyType.MONEY) {
            typeDisplayName = "Money";
         } else if (game.getCurrencyType() == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
            typeDisplayName = "PlayerPoints";
         } else if (game.getCurrencyType() == CoinFlipGame.CurrencyType.TOKENMANAGER) {
            typeDisplayName = "⛃ Tokens";
         } else if (game.getCurrencyType() == CoinFlipGame.CurrencyType.BEASTTOKENS) {
            typeDisplayName = this.plugin.getCurrencyManager().getDisplayName(game.getCurrencyType(), game.getCurrencyId());
         } else {
            typeDisplayName = this.plugin.getCurrencyManager().getDisplayName(game.getCurrencyType(), game.getCurrencyId());
         }

         UUID hostUuid = game.getHostUuid();
         PlayerStats hostStats = hostUuid != null ? this.plugin.getCoinFlipManager().getStats(hostUuid) : new PlayerStats();
         String winRate = hostStats.getTotalGames() > 0 ? String.format("%.1f", hostStats.getWinPercentage()) : "N/A";
         double taxRate = this.plugin.getTaxRateCalculator().calculateTaxRate(this.viewer, game.getAmount(), game.getCurrencyType(), game.getCurrencyId());
         double totalPot = game.getAmount() * 2.0;
         double taxedAmount = totalPot * (1.0 - taxRate);
         String taxedAmountFormatted = this.plugin.getGuiHelper().formatAmount(taxedAmount);
         String taxRatePercent = String.format("%.0f", taxRate * 100.0);
         String clickColor = this.plugin.getGUIConfig().getString("game-item.lore.click-color", "&a&l» &a");
         String cancelColor = this.plugin.getGUIConfig().getString("game-item.lore.cancel-color", "&c&l» &c");
         String clickMessageText = this.plugin.getGUIConfig().getString("game-item.lore.click-message", "Click to accept");
         String clickMessage = clickColor + clickMessageText;
         String cancelMessageText = this.plugin.getGUIConfig().getString("game-item.lore.cancel-message", "Right-click to cancel");
         String cancelMessage = cancelColor + cancelMessageText;
         Player hostPlayer = game.getHost();
         String hostName;
         if (hostPlayer != null) {
            hostName = hostPlayer.getName();
         } else if (game.getHostUuid() != null) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(game.getHostUuid());
            String name = offlinePlayer != null ? offlinePlayer.getName() : null;
            hostName = name != null && !name.isEmpty() ? name : "Unknown";
         } else {
            hostName = "Unknown";
         }

         boolean isOwnGame = this.viewer != null && game.getHostUuid() != null && this.viewer.getUniqueId().equals(game.getHostUuid());
         Map<String, String> displayPlaceholders = new HashMap<>();
         displayPlaceholders.put("player", hostName != null ? hostName : "Unknown");
         displayPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(game.getAmount()));
         displayPlaceholders.put("symbol", unit);
         String displayNameTemplate = this.plugin.getGUIConfig().getString("game-item.display-name", "&r&6&l<player> &f- &e<amount><symbol>");
         Player placeholderSource = hostPlayer != null ? hostPlayer : this.viewer;
         String displayName = this.plugin.getAdventureHelper().parseToLegacy(displayNameTemplate, displayPlaceholders, placeholderSource);
         List<String> loreTemplate;
         synchronized (CoinFlipListGUI.class) {
            FileConfiguration guiConfig = this.plugin.getGUIConfig();
            if (guiConfig == null) {
               loreTemplate = new ArrayList<>();
            } else {
               int currentConfigVersion = guiConfig.getInt("config-version", 0);
               if (cachedGameItemLoreTemplate == null || lastConfigVersion != currentConfigVersion) {
                  cachedGameItemLoreTemplate = guiConfig.getStringList("game-item.lore.lines");
                  if (cachedGameItemLoreTemplate == null || cachedGameItemLoreTemplate.isEmpty()) {
                     cachedGameItemLoreTemplate = new ArrayList<>();
                     cachedGameItemLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                     cachedGameItemLoreTemplate.add("&r&fType: &b<type>");
                     cachedGameItemLoreTemplate.add("&r&fWin Rate: &e<win_rate>%");
                     cachedGameItemLoreTemplate.add("&r&fWin Reward: &a<taxed_amount><symbol>");
                     cachedGameItemLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                     cachedGameItemLoreTemplate.add("&r<click>");
                     cachedGameItemLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                  }

                  lastConfigVersion = currentConfigVersion;
               }

               loreTemplate = cachedGameItemLoreTemplate != null ? new ArrayList<>(cachedGameItemLoreTemplate) : new ArrayList<>();
            }
         }

         if (isOwnGame) {
            boolean hasCancelPlaceholder = false;

            for (String line : loreTemplate) {
               if (line != null && line.contains("<cancel>")) {
                  hasCancelPlaceholder = true;
                  break;
               }
            }

            if (!hasCancelPlaceholder) {
               for (int i = 0; i < loreTemplate.size(); i++) {
                  String linex = loreTemplate.get(i);
                  if (linex != null && linex.contains("<click>")) {
                     String cancelLine = "<cancel>";
                     loreTemplate.add(i + 1, cancelLine);
                     break;
                  }
               }
            }
         }

         Map<String, String> placeholders = new HashMap<>();
         placeholders.put("type", typeDisplayName);
         placeholders.put("win_rate", winRate);
         placeholders.put("click", clickMessage);
         placeholders.put("cancel", isOwnGame ? cancelMessage : "");
         placeholders.put("taxed_amount", taxedAmountFormatted);
         placeholders.put("tax_rate", taxRatePercent);
         placeholders.put("symbol", unit);
         List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders, hostPlayer);
         if (MaterialHelper.isPlayerHead(material)) {
            String headType = this.plugin.getGUIConfig().getString("game-item.player-head.type", "Host");
            headType = headType != null ? headType : "Host";
            Player headPlayer = null;
            String base64Texture = "";
            boolean usePlayerSkin = false;
            if ("Host".equalsIgnoreCase(headType)) {
               headPlayer = game.getHost();
               usePlayerSkin = true;
            } else if ("Base64".equalsIgnoreCase(headType)) {
               base64Texture = this.plugin.getGUIConfig().getString("game-item.player-head.texture", "");
               usePlayerSkin = false;
            }

            Boolean glowing = this.plugin.getGUIConfig().contains("game-item.glowing")
               ? this.plugin.getGUIConfig().getBoolean("game-item.glowing", false)
               : null;
            Integer customModelData = this.plugin.getGUIConfig().contains("game-item.custom-model-data")
               ? this.plugin.getGUIConfig().getInt("game-item.custom-model-data", 0)
               : null;
            if (customModelData != null && customModelData <= 0) {
               customModelData = null;
            }

            return this.plugin.getGuiHelper().createPlayerHead(material, headPlayer, base64Texture, usePlayerSkin, displayName, lore, glowing, customModelData);
         } else {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, displayName);
               this.plugin.getGuiHelper().setLore(meta, lore);
               this.plugin.getGuiHelper().applyItemProperties(meta, "game-item", this.plugin.getGUIConfig());
               item.setItemMeta(meta);
            }

            return item;
         }
      } else {
         this.plugin.getLogger().warning("Cannot create game item: game or host is null");
         Material barrierMaterial = MaterialHelper.getBarrierMaterial();
         return new ItemStack(barrierMaterial != null ? barrierMaterial : MaterialHelper.parseMaterial("BARRIER", null));
      }
   }

   private ItemStack createStatsItem(PlayerStats stats) {
      if (stats == null) {
         this.plugin.getLogger().warning("Cannot create stats item: stats is null");
         Material barrierMaterial = MaterialHelper.getBarrierMaterial();
         return new ItemStack(barrierMaterial != null ? barrierMaterial : MaterialHelper.parseMaterial("BARRIER", null));
      } else {
         String materialName = this.plugin.getGUIConfig().getString("list-gui.stats.material", "PLAYER_HEAD");
         Material playerHeadFallback = MaterialHelper.getPlayerHeadMaterial();
         Material material = this.parseMaterial(materialName, playerHeadFallback);
         String title = this.plugin.getGUIConfig().getString("stats-item.title", "&r&6&lYour Statistics");
         if (this.plugin.getGUIConfig().contains("list-gui.stats.title")) {
            title = this.plugin.getGUIConfig().getString("list-gui.stats.title", title);
         }

         Map<String, String> placeholders = new HashMap<>();
         placeholders.put("wins", String.valueOf(stats.getWins()));
         placeholders.put("defeats", String.valueOf(stats.getDefeats()));
         placeholders.put("win_percentage", String.format("%.2f", stats.getWinPercentage()));
         placeholders.put("winrate_money", String.format("%.2f", stats.getWinPercentageMoney()));
         placeholders.put("winrate_playerpoints", String.format("%.2f", stats.getWinPercentagePlayerPoints()));
         placeholders.put("winrate_tokenmanager", String.format("%.2f", stats.getWinPercentageTokenManager()));
         placeholders.put("winrate_beasttokens", String.format("%.2f", stats.getWinPercentageBeastTokens()));
         placeholders.put("profit_money", this.plugin.getGuiHelper().formatAmount(stats.getProfitMoney()));
         placeholders.put("profit_orbs", "0");
         placeholders.put("profit_playerpoints", this.plugin.getGuiHelper().formatAmount(stats.getProfitPlayerPoints()));
         placeholders.put("profit_tokenmanager", this.plugin.getGuiHelper().formatAmount(stats.getProfitTokenManager()));
         placeholders.put("profit_beasttokens", this.plugin.getGuiHelper().formatAmount(stats.getProfitBeastTokens()));
         placeholders.put("loss_money", this.plugin.getGuiHelper().formatAmount(stats.getLossMoney()));
         placeholders.put("loss_orbs", "0");
         placeholders.put("loss_playerpoints", this.plugin.getGuiHelper().formatAmount(stats.getLossPlayerPoints()));
         placeholders.put("loss_tokenmanager", this.plugin.getGuiHelper().formatAmount(stats.getLossTokenManager()));
         placeholders.put("loss_beasttokens", this.plugin.getGuiHelper().formatAmount(stats.getLossBeastTokens()));
         placeholders.put("money_unit", this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.MONEY));
         placeholders.put("playerpoints_unit", this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.PLAYERPOINTS));
         placeholders.put("tokenmanager_unit", this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.TOKENMANAGER));
         placeholders.put("beasttokens_unit", this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.BEASTTOKENS));

         for (String currencyId : this.plugin.getCurrencyManager().getEnabledCoinsEngineCurrencyIds()) {
            String unit = this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.COINSENGINE, currencyId);
            placeholders.put("coinsengine_" + currencyId + "_unit", unit);
            String displayName = this.plugin.getCurrencyManager().getDisplayName(CoinFlipGame.CurrencyType.COINSENGINE, currencyId);
            placeholders.put("coinsengine_" + currencyId + "_display", displayName);
         }

         for (String currencyId : this.plugin.getCurrencyManager().getEnabledPlaceholderCurrencyIds()) {
            String unit = this.plugin.getCurrencyManager().getUnit(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyId);
            placeholders.put("placeholder_" + currencyId + "_unit", unit);
            String displayName = this.plugin.getCurrencyManager().getDisplayName(CoinFlipGame.CurrencyType.PLACEHOLDER, currencyId);
            placeholders.put("placeholder_" + currencyId + "_display", displayName);
            placeholders.put("winrate_" + currencyId, "0.00");
         }

         double netMoney = stats.getNetProfitMoney();
         double netPlayerPoints = stats.getNetProfitPlayerPoints();
         double netTokenManager = stats.getNetProfitTokenManager();
         double netBeastTokens = stats.getNetProfitBeastTokens();
         String netMoneyColor = netMoney >= 0.0 ? "&a" : "&c";
         String netPlayerPointsColor = netPlayerPoints >= 0.0 ? "&a" : "&c";
         String netTokenManagerColor = netTokenManager >= 0.0 ? "&a" : "&c";
         String netBeastTokensColor = netBeastTokens >= 0.0 ? "&a" : "&c";
         placeholders.put("net_money", netMoneyColor + this.plugin.getGuiHelper().formatAmount(netMoney));
         placeholders.put("net_playerpoints", netPlayerPointsColor + this.plugin.getGuiHelper().formatAmount(netPlayerPoints));
         placeholders.put("net_tokenmanager", netTokenManagerColor + this.plugin.getGuiHelper().formatAmount(netTokenManager));
         placeholders.put("net_beasttokens", netBeastTokensColor + this.plugin.getGuiHelper().formatAmount(netBeastTokens));
         String parsedTitle = this.plugin.getAdventureHelper().parseToLegacy(title, placeholders, this.viewer);
         List<String> loreTemplate;
         synchronized (CoinFlipListGUI.class) {
            FileConfiguration guiConfig = this.plugin.getGUIConfig();
            if (guiConfig == null) {
               loreTemplate = new ArrayList<>();
            } else {
               int currentConfigVersion = guiConfig.getInt("config-version", 0);
               if (cachedStatsItemLoreTemplate == null || lastConfigVersion != currentConfigVersion) {
                  cachedStatsItemLoreTemplate = guiConfig.getStringList("stats-item.lore.lines");
                  if (cachedStatsItemLoreTemplate == null || cachedStatsItemLoreTemplate.isEmpty()) {
                     cachedStatsItemLoreTemplate = new ArrayList<>();
                     cachedStatsItemLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                     cachedStatsItemLoreTemplate.add("&r&fWins: &a<wins> &8│ &r&fLosses: &c<defeats>");
                     cachedStatsItemLoreTemplate.add("&r&fWin Rate: &e<win_percentage>%");
                     cachedStatsItemLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                     cachedStatsItemLoreTemplate.add("&r&fProfit: &a<profit_money><money_unit>");
                     cachedStatsItemLoreTemplate.add("&r&fLoss: &c<loss_money><money_unit>");
                     cachedStatsItemLoreTemplate.add("&r&fNet: <net_money><money_unit>");
                     cachedStatsItemLoreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                  }

                  lastConfigVersion = currentConfigVersion;
               }

               loreTemplate = (List<String>)(cachedStatsItemLoreTemplate != null ? cachedStatsItemLoreTemplate : new ArrayList<>());
            }
         }

         List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders, this.viewer);
         if (MaterialHelper.isPlayerHead(material)) {
            String headType = this.plugin.getGUIConfig().getString("list-gui.stats.player-head.type", "Player");
            headType = headType != null ? headType : "Player";
            Player headPlayer = null;
            String base64Texture = "";
            boolean usePlayerSkin = false;
            if ("Player".equalsIgnoreCase(headType)) {
               headPlayer = this.viewer;
               usePlayerSkin = true;
            } else if ("Base64".equalsIgnoreCase(headType)) {
               base64Texture = this.plugin.getGUIConfig().getString("list-gui.stats.player-head.texture", "");
               usePlayerSkin = false;
            }

            Boolean glowing = this.plugin.getGUIConfig().contains("list-gui.stats.glowing")
               ? this.plugin.getGUIConfig().getBoolean("list-gui.stats.glowing", false)
               : null;
            Integer customModelData = this.plugin.getGUIConfig().contains("list-gui.stats.custom-model-data")
               ? this.plugin.getGUIConfig().getInt("list-gui.stats.custom-model-data", 0)
               : null;
            if (customModelData != null && customModelData <= 0) {
               customModelData = null;
            }

            return this.plugin.getGuiHelper().createPlayerHead(material, headPlayer, base64Texture, usePlayerSkin, parsedTitle, lore, glowing, customModelData);
         } else {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, parsedTitle);
               this.plugin.getGuiHelper().setLore(meta, lore);
               this.plugin.getGuiHelper().applyItemProperties(meta, "list-gui.stats", this.plugin.getGUIConfig());
               item.setItemMeta(meta);
            }

            return item;
         }
      }
   }

   private ItemStack createNavigationItem(Material material, String name, String base64Texture, String configPath) {
      if (material == null) {
         material = MaterialHelper.getPlayerHeadMaterial();
      }

      Boolean glowing = null;
      Integer customModelData = null;
      if (configPath != null) {
         if (this.plugin.getGUIConfig().contains(configPath + ".glowing")) {
            glowing = this.plugin.getGUIConfig().getBoolean(configPath + ".glowing", false);
         }

         if (this.plugin.getGUIConfig().contains(configPath + ".custom-model-data")) {
            customModelData = this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0);
            if (customModelData <= 0) {
               customModelData = null;
            }
         }
      }

      if (MaterialHelper.isPlayerHead(material)) {
         if (base64Texture != null && !base64Texture.isEmpty()) {
            ItemStack item = this.plugin
               .getGuiHelper()
               .createPlayerHead(material, null, base64Texture, false, name != null ? name : "", new ArrayList(), glowing, customModelData);
            if (item != null && name != null && !name.isEmpty()) {
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta, name);
                  item.setItemMeta(meta);
               }
            }

            Material barrierMaterial = MaterialHelper.getBarrierMaterial();
            return item != null ? item : new ItemStack(barrierMaterial != null ? barrierMaterial : MaterialHelper.parseMaterial("BARRIER", null));
         } else {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, name != null ? name : "");
               if (glowing != null || customModelData != null) {
                  this.plugin.getGuiHelper().applyItemProperties(meta, glowing, customModelData);
               }

               item.setItemMeta(meta);
            }

            return item;
         }
      } else {
         ItemStack item = new ItemStack(material);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            this.plugin.getGuiHelper().setDisplayName(meta, name != null ? name : "");
            if (glowing != null || customModelData != null) {
               this.plugin.getGuiHelper().applyItemProperties(meta, glowing, customModelData);
            }

            item.setItemMeta(meta);
         }

         return item;
      }
   }

   private ItemStack createHistoryButton(Material material, String title, List<String> lore) {
      if (material == null) {
         material = MaterialHelper.getBookMaterial();
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&6&lYour CoinFlip History");
         if (lore != null && !lore.isEmpty()) {
            List<?> loreList = this.plugin.getGuiHelper().createLore(lore, new HashMap<>());
            this.plugin.getGuiHelper().setLore(meta, loreList);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "list-gui.history", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createLeaderboardButton(Material material, String title, List<String> lore) {
      if (material == null) {
         material = Material.GOLD_INGOT;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&6&lLeaderboard");
         if (lore != null && !lore.isEmpty()) {
            List<?> loreList = this.plugin.getGuiHelper().createLore(lore, new HashMap<>());
            this.plugin.getGuiHelper().setLore(meta, loreList);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "list-gui.leaderboard", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createCreateButton(Material material, String title, List<String> lore) {
      if (material == null) {
         material = Material.NETHER_STAR;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&6&lCreate CoinFlip");
         if (lore != null && !lore.isEmpty()) {
            List<?> loreList = this.plugin.getGuiHelper().createLore(lore, new HashMap<>());
            this.plugin.getGuiHelper().setLore(meta, loreList);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "list-gui.create", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createHouseButton(Player player, Material material, String title, List<String> lore) {
      if (material == null) {
         material = MaterialHelper.getPlayerHeadMaterial();
         if (material == null) {
            material = Material.DIAMOND;
         }
      }

      String botName = this.plugin.getConfig().getString("house.name", "Bot");
      DatabaseManager.BotGameStats botStats = null;

      try {
         botStats = this.plugin.getDatabaseManager().getPlayerBotStats(player.getUniqueId());
      } catch (Exception var17) {
         this.plugin.getLogger().warning("Failed to get bot stats for player " + player.getName() + ": " + var17.getMessage());
      }

      Map<String, String> placeholders = new HashMap<>();
      placeholders.put("bot_name", botName);
      if (botStats != null) {
         placeholders.put("bot_total_games", String.valueOf(botStats.getTotalGames()));
         double winRate = botStats.getWinRate();
         placeholders.put("bot_win_rate", String.format("%.1f", winRate));
         double profit = botStats.getProfit();
         String profitColor = profit >= 0.0 ? "&a" : "&c";
         placeholders.put("bot_profit_color", profitColor);
         CoinFlipGame.CurrencyType defaultCurrencyType = CoinFlipGame.CurrencyType.MONEY;
         String defaultCurrencyId = null;
         String profitSymbol = this.plugin.getCurrencyManager().getUnit(defaultCurrencyType, defaultCurrencyId);
         String formattedProfit = this.plugin.getGuiHelper().formatAmount(Math.abs(profit), defaultCurrencyId);
         placeholders.put("bot_profit", formattedProfit);
         placeholders.put("bot_profit_symbol", profitSymbol);
      } else {
         placeholders.put("bot_total_games", "0");
         placeholders.put("bot_win_rate", "0.0");
         placeholders.put("bot_profit_color", "&7");
         placeholders.put("bot_profit", "0");
         placeholders.put("bot_profit_symbol", "");
      }

      List<String> processedLore = new ArrayList<>();
      if (lore != null && !lore.isEmpty()) {
         for (String line : lore) {
            if (line != null) {
               String processedLine = line;
               if (line.trim().isEmpty()) {
                  processedLine = "&r ";
               } else {
                  for (Entry<String, String> entry : placeholders.entrySet()) {
                     processedLine = processedLine.replace("<" + entry.getKey() + ">", entry.getValue());
                  }
               }

               processedLore.add(processedLine);
            }
         }
      }

      String configPath = "list-gui.house";
      Boolean glowing = this.plugin.getGUIConfig().contains(configPath + ".glowing")
         ? this.plugin.getGUIConfig().getBoolean(configPath + ".glowing", false)
         : null;
      Integer customModelData = this.plugin.getGUIConfig().contains(configPath + ".custom-model-data")
         ? this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0)
         : null;
      if (customModelData != null && customModelData <= 0) {
         customModelData = null;
      }

      if (!MaterialHelper.isPlayerHead(material)) {
         ItemStack item = new ItemStack(material);
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return item;
         } else {
            this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&6&lPlay with Bot");
            if (!processedLore.isEmpty()) {
               List<?> loreList = this.plugin.getGuiHelper().createLore(processedLore, placeholders);
               this.plugin.getGuiHelper().setLore(meta, loreList);
            }

            this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
            item.setItemMeta(meta);
            return item;
         }
      } else {
         String base64Texture = null;
         String playerHeadType = this.plugin.getGUIConfig().getString(configPath + ".player-head.type", "Default");
         if ("Base64".equalsIgnoreCase(playerHeadType)) {
            base64Texture = this.plugin.getGUIConfig().getString(configPath + ".player-head.texture", "");
         }

         if (base64Texture != null && !base64Texture.isEmpty()) {
            ItemStack item = this.plugin
               .getGuiHelper()
               .createPlayerHead(material, null, base64Texture, false, title != null ? title : "&r&6&lPlay with Bot", processedLore, glowing, customModelData);
            if (item != null) {
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  if (title != null && !title.isEmpty()) {
                     this.plugin.getGuiHelper().setDisplayName(meta, title);
                  }

                  if (!processedLore.isEmpty()) {
                     List<?> loreList = this.plugin.getGuiHelper().createLore(processedLore, placeholders);
                     this.plugin.getGuiHelper().setLore(meta, loreList);
                  }

                  item.setItemMeta(meta);
               }
            }

            Material barrierMaterial = MaterialHelper.getBarrierMaterial();
            return item != null ? item : new ItemStack(barrierMaterial != null ? barrierMaterial : MaterialHelper.parseMaterial("BARRIER", null));
         } else {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&6&lPlay with Bot");
               if (!processedLore.isEmpty()) {
                  List<?> loreList = this.plugin.getGuiHelper().createLore(processedLore, placeholders);
                  this.plugin.getGuiHelper().setLore(meta, loreList);
               }

               if (glowing != null || customModelData != null) {
                  this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
               }

               item.setItemMeta(meta);
            }

            return item;
         }
      }
   }

   private ItemStack createFilterButton(Material material, String title, List<String> loreTemplate, String currentFilter) {
      if (material == null) {
         material = Material.HOPPER;
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         this.plugin.getGuiHelper().setDisplayName(meta, title != null ? title : "&r&6&lFilter Games");
         if (currentFilter == null) {
            currentFilter = "NONE";
         }

         String filterDisplayName;
         switch (currentFilter) {
            case "TIME_NEWEST":
               filterDisplayName = this.plugin.getGUIConfig().getString("list-gui.filter.filter-names.time-newest", "Time (Newest)");
               break;
            case "AMOUNT_ASCENDING":
               filterDisplayName = this.plugin.getGUIConfig().getString("list-gui.filter.filter-names.amount-ascending", "Amount (Low to High)");
               break;
            case "AMOUNT_DESCENDING":
               filterDisplayName = this.plugin.getGUIConfig().getString("list-gui.filter.filter-names.amount-descending", "Amount (High to Low)");
               break;
            default:
               filterDisplayName = this.plugin.getGUIConfig().getString("list-gui.filter.filter-names.none", "None");
         }

         if (filterDisplayName == null || filterDisplayName.isEmpty()) {
            filterDisplayName = "None";
         }

         if (loreTemplate != null && !loreTemplate.isEmpty()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("filter", filterDisplayName);
            List<?> loreList = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
            this.plugin.getGuiHelper().setLore(meta, loreList);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "list-gui.filter", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
         return item;
      }
   }

   private void updateFilterButton() {
      Inventory inventory = this.getInventory();
      if (inventory != null) {
         int filterSlot = this.plugin.getGUIConfig().getInt("list-gui.filter.slot", 41);
         if (filterSlot >= 0 && filterSlot < inventory.getSize()) {
            String filterMaterialName = this.plugin.getGUIConfig().getString("list-gui.filter.material", "HOPPER");
            Material filterMaterial = this.parseMaterial(filterMaterialName, Material.HOPPER);
            String filterTitle = this.plugin.getGUIConfig().getString("list-gui.filter.title", "&r&6&lFilter Games");
            List<String> filterLoreTemplate = this.plugin.getGUIConfig().getStringList("list-gui.filter.lore");
            ItemStack filterItem = this.createFilterButton(filterMaterial, filterTitle, filterLoreTemplate, this.filterType);
            inventory.setItem(filterSlot, filterItem);
         }
      }
   }

   private void handleGameClick(InventoryClickEvent event, Player clicker, CoinFlipGame game) {
      if (event.getClick() == ClickType.RIGHT) {
         if (game.getHost() != null && game.getHost().equals(clicker)) {
            if (this.plugin.getCoinFlipManager().isOnRefundCooldown(clicker.getUniqueId())) {
               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(clicker, "error.general");
               }

               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
                  int remaining = this.plugin.getCoinFlipManager().getRemainingCooldown(clicker.getUniqueId());
                  String message = this.plugin.getMessage("prefix")
                     + " "
                     + this.plugin.getMessage("command.cooldown-wait").replace("<seconds>", String.valueOf(remaining));
                  this.plugin.getAdventureHelper().sendMessage(clicker, message);
               }
            } else {
               UUID gameIdToRemove = game.getGameId();
               if (!this.plugin.getCoinFlipManager().refundGame(clicker, gameIdToRemove)) {
                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
                     String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.player-not-found");
                     this.plugin.getAdventureHelper().sendMessage(clicker, message);
                  }
               } else {
                  event.setCancelled(true);
                  Inventory inv = this.getInventory();
                  if (inv != null && event.getSlot() >= 0 && event.getSlot() < inv.getSize()) {
                     inv.setItem(event.getSlot(), null);
                     Map<Integer, InventoryButton> buttonMap = this.getButtonMap();
                     if (buttonMap != null) {
                        buttonMap.remove(event.getSlot());
                     }
                  }

                  this.plugin.getInventoryUpdateBatcher().scheduleUpdate(clicker);
                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
                     this.plugin.getSoundHelper().playSound(clicker, "game.cancel");
                  }

                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-game-cancelled")) {
                     String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.bet-cancelled");
                     this.plugin.getAdventureHelper().sendMessage(clicker, message);
                  }
               }
            }
         }
      } else if (game.getHost() != null && game.getHost().equals(clicker)) {
         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
            this.plugin.getSoundHelper().playSound(clicker, "error.general");
         }

         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.cannot-play-self");
            this.plugin.getAdventureHelper().sendMessage(clicker, message);
         }
      } else if (this.plugin.getCoinFlipManager().isInRollingGame(clicker.getUniqueId())) {
         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
            this.plugin.getSoundHelper().playSound(clicker, "error.general");
         }

         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.rolling-active");
            this.plugin.getAdventureHelper().sendMessage(clicker, message);
         }
      } else if (!playersJoiningGame.add(clicker.getUniqueId())) {
         ExploitDetector detector = this.plugin.getExploitDetector();
         if (detector != null && detector.isEnabled()) {
            detector.report(clicker, ExploitDetector.ExploitType.DOUBLE_CLICK_JOIN, "GameId=" + game.getGameId() + ", Amount=" + game.getAmount());
         }

         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
            this.plugin.getSoundHelper().playSound(clicker, "error.general");
         }
      } else {
         UUID gameId = game.getGameId();
         if (!gamesBeingJoined.add(gameId)) {
            playersJoiningGame.remove(clicker.getUniqueId());
            ExploitDetector detectorx = this.plugin.getExploitDetector();
            if (detectorx != null && detectorx.isEnabled()) {
               detectorx.report(clicker, ExploitDetector.ExploitType.RACE_CONDITION_JOIN, "GameId=" + gameId + ", Amount=" + game.getAmount());
            }

            if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
               this.plugin.getSoundHelper().playSound(clicker, "error.general");
            }

            if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
               String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.player-not-found");
               this.plugin.getAdventureHelper().sendMessage(clicker, message);
            }
         } else {
            try {
               CoinFlipGame rollGame;
               synchronized (joinGameLock) {
                  if (this.plugin.getCoinFlipManager().getGameById(gameId) == null) {
                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(clicker, "error.general");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.player-not-found");
                        this.plugin.getAdventureHelper().sendMessage(clicker, message);
                     }

                     return;
                  }

                  double amount = game.getAmount();
                  CoinFlipGame.CurrencyType currencyType = game.getCurrencyType();
                  String currencyId = game.getCurrencyId();
                  Player hostPlayer = game.getHost();
                  if (hostPlayer != null && hostPlayer.isOnline()) {
                     if (!this.plugin.getCurrencyManager().canPlayersGambleTogether(hostPlayer, clicker, currencyType, currencyId)) {
                        boolean joinerCanUse = this.plugin.getCurrencyManager().canPlayerUseCurrency(clicker, currencyType, currencyId);
                        String message;
                        if (!joinerCanUse) {
                           String restrictionReason = this.plugin.getCurrencyManager().getRestrictionReason(clicker, currencyType, currencyId);
                           message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("restriction.cannot-join-game");
                           if (restrictionReason != null && !restrictionReason.isEmpty()) {
                              message = message + " " + restrictionReason;
                           }
                        } else {
                           message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("restriction.host-cannot-use");
                        }

                        this.plugin.getSoundHelper().playSound(clicker, "error.general");
                        this.plugin.getAdventureHelper().sendMessage(clicker, message);
                        return;
                     }
                  } else if (!this.plugin.getCurrencyManager().canPlayerUseCurrency(clicker, currencyType, currencyId)) {
                     this.plugin.getSoundHelper().playSound(clicker, "error.general");
                     String restrictionReason = this.plugin.getCurrencyManager().getRestrictionReason(clicker, currencyType, currencyId);
                     String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("restriction.cannot-join-game");
                     if (restrictionReason != null && !restrictionReason.isEmpty()) {
                        message = message + " " + restrictionReason;
                     }

                     this.plugin.getAdventureHelper().sendMessage(clicker, message);
                     return;
                  }

                  if (!this.plugin.getCurrencyManager().hasBalanceWithReserve(clicker, currencyType, currencyId, amount)) {
                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(clicker, "error.not-enough-money");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
                        double currentBalance = this.plugin.getCurrencyManager().getBalance(clicker, currencyType, currencyId);
                        String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, currencyId);
                        String formattedAmount = this.plugin.getGuiHelper().formatAmount(amount, currencyId);
                        boolean isReserveIssue = this.plugin.getCurrencyManager().isReserveBalanceIssue(clicker, currencyType, currencyId, amount);
                        Map<String, String> placeholders = new HashMap<>();
                        String messageKey;
                        if (isReserveIssue) {
                           double minReserve = this.plugin.getCurrencyManager().getMinReserveBalance(currencyType, currencyId);
                           double maxBet = Math.max(0.0, currentBalance - minReserve);
                           String formattedReserve = this.plugin.getGuiHelper().formatAmount(minReserve, currencyId);
                           String formattedMaxBet = this.plugin.getGuiHelper().formatAmount(maxBet, currencyId);
                           if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
                              messageKey = "command.not-enough-money-reserve";
                           } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                              messageKey = "command.not-enough-playerpoints-reserve";
                           } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                              messageKey = "command.not-enough-tokenmanager-reserve";
                           } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                              messageKey = "command.not-enough-beasttokens-reserve";
                           } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                              messageKey = "command.not-enough-coinsengine-reserve";
                           } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                              messageKey = "command.not-enough-placeholder-reserve";
                           } else {
                              messageKey = "command.not-enough-money-reserve";
                           }

                           placeholders.put("reserve", formattedReserve);
                           placeholders.put("balance", formattedBalance);
                           placeholders.put("max_bet", formattedMaxBet);
                        } else {
                           if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
                              messageKey = "command.not-enough-money";
                           } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                              messageKey = "command.not-enough-playerpoints";
                           } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                              messageKey = "command.not-enough-tokenmanager";
                           } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                              messageKey = "command.not-enough-beasttokens";
                           } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                              messageKey = "command.not-enough-coinsengine";
                           } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                              messageKey = "command.not-enough-placeholder";
                           } else {
                              messageKey = "command.not-enough-money";
                           }

                           placeholders.put("amount", formattedAmount);
                           placeholders.put("balance", formattedBalance);
                        }

                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                        this.plugin.getAdventureHelper().sendMessage(clicker, message, placeholders);
                     }

                     return;
                  }

                  CoinFlipGame removedGame = this.plugin.getCoinFlipManager().takeGameForRoll(gameId);
                  if (removedGame == null) {
                     ExploitDetector detectorxx = this.plugin.getExploitDetector();
                     if (detectorxx != null && detectorxx.isEnabled()) {
                        detectorxx.report(clicker, ExploitDetector.ExploitType.GAME_ALREADY_TAKEN, "GameId=" + gameId + ", Amount=" + amount);
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(clicker, "error.general");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "message-error")) {
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.player-not-found");
                        this.plugin.getAdventureHelper().sendMessage(clicker, message);
                     }

                     return;
                  }

                  if (!this.plugin.getCurrencyManager().withdraw(clicker, currencyType, currencyId, amount)) {
                     this.plugin.getCoinFlipManager().restoreGameAfterFailedJoin(removedGame);
                     ExploitDetector detectorxxx = this.plugin.getExploitDetector();
                     if (detectorxxx != null && detectorxxx.isEnabled()) {
                        detectorxxx.report(
                           clicker,
                           ExploitDetector.ExploitType.JOIN_ROLLBACK,
                           "GameId=" + gameId + ", Amount=" + amount + ", Currency=" + currencyType + "/" + (currencyId != null ? currencyId : "default")
                        );
                     }

                     this.plugin.getSoundHelper().playSound(clicker, "error.general");
                     String message = this.plugin.getMessage("prefix") + " &cFailed to withdraw currency!";
                     this.plugin.getAdventureHelper().sendMessage(clicker, message);
                     return;
                  }

                  rollGame = removedGame;
               }

               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(clicker, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(clicker, "game.join");
               }

               Player rollHostPlayer = rollGame.getHost();
               if (rollHostPlayer != null
                  && rollHostPlayer.isOnline()
                  && this.plugin.getPlayerSettingsManager().isSettingEnabled(rollHostPlayer, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(rollHostPlayer, "game.join");
               }

               clicker.closeInventory();
               if (rollHostPlayer != null && rollHostPlayer.isOnline()) {
                  rollHostPlayer.closeInventory();
               }

               CoinFlipRollGUI rollGUI = new CoinFlipRollGUI(
                  this.plugin, rollGame.getHost(), clicker, rollGame.getAmount(), rollGame.getCurrencyType(), rollGame.getCurrencyId()
               );
               rollGUI.startAnimation();
            } finally {
               playersJoiningGame.remove(clicker.getUniqueId());
               gamesBeingJoined.remove(gameId);
            }
         }
      }
   }

   private List<CoinFlipGame> applyCurrencyRestrictionFilter(List<CoinFlipGame> games) {
      if (games == null || games.isEmpty()) {
         return games;
      } else if (this.viewer != null && this.viewer.isOnline()) {
         List<CoinFlipGame> filteredGames = new ArrayList<>(games);
         filteredGames.removeIf(
            game -> {
               if (game == null) {
                  return true;
               } else {
                  Player hostPlayer = game.getHost();
                  CoinFlipGame.CurrencyType currencyType = game.getCurrencyType();
                  String currencyId = game.getCurrencyId();
                  return hostPlayer != null && hostPlayer.isOnline()
                     ? !this.plugin.getCurrencyManager().canPlayersGambleTogether(hostPlayer, this.viewer, currencyType, currencyId)
                     : !this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, currencyType, currencyId);
               }
            }
         );
         return filteredGames;
      } else {
         return games;
      }
   }

   private List<CoinFlipGame> applyFilter(List<CoinFlipGame> games) {
      if (games != null && !games.isEmpty()) {
         int gamesVersion = this.plugin.getCoinFlipManager().getGamesListVersion();
         List<CoinFlipGame> sortedGames = this.plugin.getFilteredGamesCache().getFilteredGames(games, this.filterType, gamesVersion);
         return this.applyCurrencyRestrictionFilter(sortedGames);
      } else {
         return games;
      }
   }
}
