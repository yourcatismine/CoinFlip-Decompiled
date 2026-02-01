package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.currency.CurrencySettings;
import com.kstudio.ultracoinflip.data.BettingLimitManager;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HeadsTailsSelectionGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;
   private final CoinFlipGame.CurrencyType currencyType;
   private final String currencyId;
   private final double amount;
   private final Runnable onCancel;
   private String cachedTitle;
   private int cachedSize;
   private String cachedFillerMaterialName;
   private String cachedFillerDisplayName;
   private int cachedHeadsSlot;
   private String cachedHeadsMaterialName;
   private String cachedHeadsTitle;
   private List<String> cachedHeadsLore;
   private int cachedTailsSlot;
   private String cachedTailsMaterialName;
   private String cachedTailsTitle;
   private List<String> cachedTailsLore;
   private int cachedCancelSlot;
   private String cachedCancelMaterialName;
   private String cachedCancelTitle;
   private List<String> cachedCancelLore;
   private int[] cachedFillerSlots;
   private static final Object gameCreationLock = new Object();

   public HeadsTailsSelectionGUI(KStudio plugin, Player viewer, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount, Runnable onCancel) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.currencyType = currencyType;
      this.currencyId = currencyId;
      this.amount = amount;
      this.onCancel = onCancel;
      this.cacheConfigValues();
   }

   private void cacheConfigValues() {
      this.cachedTitle = this.plugin.getGUIConfig().getString("heads-tails-gui.title", "&lChoose Heads or Tails");
      this.cachedSize = this.plugin.getGUIConfig().getInt("heads-tails-gui.size", 27);
      this.cachedFillerMaterialName = this.plugin.getGUIConfig().getString("heads-tails-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
      this.cachedFillerDisplayName = this.plugin.getGUIConfig().getString("heads-tails-gui.filler.display-name", " ");
      this.cachedHeadsSlot = this.plugin.getGUIConfig().getInt("heads-tails-gui.heads.slot", 11);
      this.cachedHeadsMaterialName = this.plugin.getGUIConfig().getString("heads-tails-gui.heads.material", "GOLD_BLOCK");
      this.cachedHeadsTitle = this.plugin.getGUIConfig().getString("heads-tails-gui.heads.title", "&6&lHEADS");
      this.cachedHeadsLore = this.plugin.getGUIConfig().getStringList("heads-tails-gui.heads.lore");
      this.cachedTailsSlot = this.plugin.getGUIConfig().getInt("heads-tails-gui.tails.slot", 15);
      this.cachedTailsMaterialName = this.plugin.getGUIConfig().getString("heads-tails-gui.tails.material", "IRON_BLOCK");
      this.cachedTailsTitle = this.plugin.getGUIConfig().getString("heads-tails-gui.tails.title", "&7&lTAILS");
      this.cachedTailsLore = this.plugin.getGUIConfig().getStringList("heads-tails-gui.tails.lore");
      this.cachedCancelSlot = this.plugin.getGUIConfig().getInt("heads-tails-gui.cancel.slot", 13);
      this.cachedCancelMaterialName = this.plugin.getGUIConfig().getString("heads-tails-gui.cancel.material", "BARRIER");
      this.cachedCancelTitle = this.plugin.getGUIConfig().getString("heads-tails-gui.cancel.title", "&c&lCancel");
      this.cachedCancelLore = this.plugin.getGUIConfig().getStringList("heads-tails-gui.cancel.lore");
      List<Integer> fillerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "heads-tails-gui.filler.slots");
      this.cachedFillerSlots = fillerSlotsList.isEmpty()
         ? new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19, 20, 21, 22, 23, 24, 25, 26}
         : fillerSlotsList.stream().mapToInt(i -> i != null ? i : -1).filter(i -> i >= 0 && i < this.cachedSize).toArray();
   }

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-heads-tails";
   }

   @Override
   protected Inventory createInventory() {
      return this.plugin.getGuiHelper().createInventory(null, this.cachedSize, this.cachedTitle, new HashMap<>());
   }

   @Override
   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         ItemStack filler = MaterialHelper.createItemStack(this.cachedFillerMaterialName);
         if (filler == null) {
            filler = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
         }

         if (filler == null) {
            filler = new ItemStack(Material.GLASS_PANE);
         }

         ItemMeta fillerMeta = filler.getItemMeta();
         if (fillerMeta != null) {
            this.plugin.getGuiHelper().setDisplayName(fillerMeta, this.cachedFillerDisplayName);
            this.plugin.getGuiHelper().applyItemProperties(fillerMeta, "heads-tails-gui.filler", this.plugin.getGUIConfig());
            filler.setItemMeta(fillerMeta);
         }

         for (int slot : this.cachedFillerSlots) {
            if (slot >= 0 && slot < this.getInventory().getSize()) {
               this.getInventory().setItem(slot, filler);
            }
         }

         Material headsMaterial = this.parseMaterial(this.cachedHeadsMaterialName, Material.GOLD_BLOCK);
         this.addButton(
            this.cachedHeadsSlot,
            new InventoryButton().creator(p -> this.createHeadsButton(headsMaterial, this.cachedHeadsTitle, this.cachedHeadsLore)).consumer(event -> {
               Player clicker = (Player)event.getWhoClicked();
               this.selectHeads(clicker);
            })
         );
         Material tailsMaterial = this.parseMaterial(this.cachedTailsMaterialName, Material.IRON_BLOCK);
         this.addButton(
            this.cachedTailsSlot,
            new InventoryButton().creator(p -> this.createTailsButton(tailsMaterial, this.cachedTailsTitle, this.cachedTailsLore)).consumer(event -> {
               Player clicker = (Player)event.getWhoClicked();
               this.selectTails(clicker);
            })
         );
         Material cancelMaterial = this.parseMaterial(this.cachedCancelMaterialName, Material.BARRIER);
         this.addButton(
            this.cachedCancelSlot,
            new InventoryButton().creator(p -> this.createCancelButton(cancelMaterial, this.cachedCancelTitle, this.cachedCancelLore)).consumer(event -> {
               Player clicker = (Player)event.getWhoClicked();
               this.cancelSelection(clicker);
            })
         );
         this.updateGUI();
      }
   }

   private void updateGUI() {
      FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
         if (this.viewer != null && this.viewer.isOnline()) {
            try {
               Inventory topInv = GUIHelper.getTopInventorySafely(this.viewer);
               if (topInv == null || !topInv.equals(this.getInventory())) {
                  return;
               }
            } catch (Exception var2) {
               return;
            }

            this.decorate(this.viewer);
         }
      });
   }

   private Material parseMaterial(String materialName, Material fallback) {
      return MaterialHelper.parseMaterial(materialName, fallback);
   }

   private ItemStack createHeadsButton(Material material, String title, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         if (lore != null && !lore.isEmpty()) {
            this.plugin.getGuiHelper().setLore(meta, lore);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "heads-tails-gui.heads", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createTailsButton(Material material, String title, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         if (lore != null && !lore.isEmpty()) {
            this.plugin.getGuiHelper().setLore(meta, lore);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "heads-tails-gui.tails", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createCancelButton(Material material, String title, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         if (lore != null && !lore.isEmpty()) {
            this.plugin.getGuiHelper().setLore(meta, lore);
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, "heads-tails-gui.cancel", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private void selectHeads(Player player) {
      if (player != null && player.isOnline()) {
         this.plugin.getSoundHelper().playSound(player, "gui.select-heads");
         FoliaScheduler.runTask(this.plugin, player, () -> {
            if (player != null && player.isOnline()) {
               try {
                  player.closeInventory();
               } catch (Exception var2) {
               }
            }
         });
         FoliaScheduler.runTaskLater(this.plugin, player, () -> {
            if (player != null && player.isOnline()) {
               this.createGameWithChoice(player, true);
            }
         }, 1L);
      }
   }

   private void selectTails(Player player) {
      if (player != null && player.isOnline()) {
         this.plugin.getSoundHelper().playSound(player, "gui.select-tails");
         FoliaScheduler.runTask(this.plugin, player, () -> {
            if (player != null && player.isOnline()) {
               try {
                  player.closeInventory();
               } catch (Exception var2) {
               }
            }
         });
         FoliaScheduler.runTaskLater(this.plugin, player, () -> {
            if (player != null && player.isOnline()) {
               this.createGameWithChoice(player, false);
            }
         }, 1L);
      }
   }

   private void cancelSelection(Player player) {
      if (player != null && player.isOnline()) {
         this.plugin.getSoundHelper().playSound(player, "gui.click");
         FoliaScheduler.runTask(this.plugin, player, () -> {
            if (player != null && player.isOnline()) {
               try {
                  player.closeInventory();
               } catch (Exception var2) {
               }
            }
         });
         if (this.onCancel != null) {
            FoliaScheduler.runTaskLater(this.plugin, player, () -> {
               if (player != null && player.isOnline()) {
                  this.onCancel.run();
               }
            }, 2L);
         }
      }
   }

   private void createGameWithChoice(Player player, boolean headsChoice) {
      if (player != null && player.isOnline()) {
         if (this.amount <= 0.0) {
            String message = this.plugin.getMessage("prefix") + " &cAmount must be greater than 0!";
            this.plugin.getAdventureHelper().sendMessage(player, message);
         } else {
            CurrencySettings currencySettings = this.plugin.getCurrencyManager().getCurrencySettings(this.currencyType, this.currencyId);
            if (!this.plugin.getCurrencyManager().canPlayerUseCurrency(player, this.currencyType, this.currencyId)) {
               String restrictionReason = this.plugin.getCurrencyManager().getRestrictionReason(player, this.currencyType, this.currencyId);
               String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("restriction.cannot-use-currency");
               if (restrictionReason != null && !restrictionReason.isEmpty()) {
                  message = message + " " + restrictionReason;
               }

               this.plugin.getAdventureHelper().sendMessage(player, message);
            } else {
               double minBid = currencySettings.getMinBid();
               if (this.amount < minBid) {
                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                     this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                  }

                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                     Map<String, String> placeholders = new HashMap<>();
                     String formattedMinBid = this.plugin.getGuiHelper().formatAmount(minBid, this.currencyId);
                     placeholders.put("amount", formattedMinBid);
                     String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.min-bid");
                     this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                  }
               } else {
                  double maxBid = currencySettings.getMaxBid();
                  if (maxBid != -1.0 && this.amount > maxBid) {
                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                        Map<String, String> placeholders = new HashMap<>();
                        String formattedMaxBid = this.plugin.getGuiHelper().formatAmount(maxBid, this.currencyId);
                        placeholders.put("amount", formattedMaxBid);
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.max-bid");
                        this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                     }
                  } else if (!this.plugin.getCurrencyManager().hasBalanceWithReserve(player, this.currencyType, this.currencyId, this.amount)) {
                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(player, "error.not-enough-money");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                        double currentBalance = this.plugin.getCurrencyManager().getBalance(player, this.currencyType, this.currencyId);
                        String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, this.currencyId);
                        String formattedAmount = this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId);
                        boolean isReserveIssue = this.plugin
                           .getCurrencyManager()
                           .isReserveBalanceIssue(player, this.currencyType, this.currencyId, this.amount);
                        Map<String, String> placeholders = new HashMap<>();
                        String messageKey;
                        if (isReserveIssue) {
                           double minReserve = this.plugin.getCurrencyManager().getMinReserveBalance(this.currencyType, this.currencyId);
                           double maxBet = Math.max(0.0, currentBalance - minReserve);
                           String formattedReserve = this.plugin.getGuiHelper().formatAmount(minReserve, this.currencyId);
                           String formattedMaxBet = this.plugin.getGuiHelper().formatAmount(maxBet, this.currencyId);
                           messageKey = this.getInsufficientBalanceReserveMessageKey(this.currencyType);
                           placeholders.put("reserve", formattedReserve);
                           placeholders.put("balance", formattedBalance);
                           placeholders.put("max_bet", formattedMaxBet);
                        } else {
                           messageKey = this.getInsufficientBalanceMessageKey(this.currencyType);
                           placeholders.put("amount", formattedAmount);
                           placeholders.put("balance", formattedBalance);
                        }

                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                        this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                     }
                  } else if (this.plugin.getCoinFlipManager().isInRollingGame(player.getUniqueId())) {
                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(player, "error.general");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.rolling-active");
                        this.plugin.getAdventureHelper().sendMessage(player, message);
                     }
                  } else {
                     synchronized (gameCreationLock) {
                        if (!this.plugin.getCoinFlipManager().canCreateMoreGames(player)) {
                           if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                              this.plugin.getSoundHelper().playSound(player, "error.general");
                           }

                           if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                              int maxGames = this.plugin.getCoinFlipManager().getMaxAllowedGames(player);
                              String messageKey = maxGames <= 1 ? "command.already-bet" : "command.bet-limit-reached";
                              String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                              if (maxGames > 1) {
                                 Map<String, String> placeholders = new HashMap<>();
                                 placeholders.put("limit", String.valueOf(maxGames));
                                 this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                              } else {
                                 this.plugin.getAdventureHelper().sendMessage(player, message);
                              }
                           }

                           return;
                        }

                        BettingLimitManager.LimitCheckResult limitResult = this.plugin
                           .getBettingLimitManager()
                           .canPlayerBet(player, this.currencyType, this.currencyId, this.amount);
                        if (limitResult != null) {
                           if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                              this.plugin.getSoundHelper().playSound(player, "error.general");
                           }

                           if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                              Map<String, String> placeholders = new HashMap<>();
                              placeholders.put("limit", this.plugin.getGuiHelper().formatAmount(limitResult.getLimit(), this.currencyId));
                              placeholders.put("current", this.plugin.getGuiHelper().formatAmount(limitResult.getCurrentTotal(), this.currencyId));
                              placeholders.put("remaining", this.plugin.getGuiHelper().formatAmount(limitResult.getRemaining(), this.currencyId));
                              String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command." + limitResult.getMessageKey());
                              this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                           }

                           return;
                        }

                        if (this.plugin.getCoinFlipManager().isInRollingGame(player.getUniqueId())) {
                           if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                              this.plugin.getSoundHelper().playSound(player, "error.general");
                           }

                           if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                              String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.rolling-active");
                              this.plugin.getAdventureHelper().sendMessage(player, message);
                           }

                           return;
                        }

                        if (!this.plugin.getCurrencyManager().withdraw(player, this.currencyType, this.currencyId, this.amount)) {
                           String message = this.plugin.getMessage("prefix") + " &cFailed to withdraw currency!";
                           this.plugin.getAdventureHelper().sendMessage(player, message);
                           return;
                        }

                        this.plugin.getCoinFlipManager().createGame(player, this.currencyType, this.currencyId, this.amount, headsChoice);
                        this.plugin.getBettingLimitManager().recordBet(player, this.currencyType, this.currencyId, this.amount);
                     }

                     Map<String, String> placeholders = new HashMap<>();
                     String formattedAmount = this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId);
                     placeholders.put("amount", formattedAmount);
                     String unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
                     placeholders.put("symbol", unit);
                     String choiceText = headsChoice ? this.plugin.getMessage("heads-tails.heads") : this.plugin.getMessage("heads-tails.tails");
                     placeholders.put("choice", choiceText);
                     String messageKey;
                     if (this.currencyType == CoinFlipGame.CurrencyType.MONEY) {
                        messageKey = "command.bet-created-money";
                     } else if (this.currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                        messageKey = "command.bet-created-playerpoints";
                     } else if (this.currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                        messageKey = "command.bet-created-tokenmanager";
                     } else if (this.currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                        messageKey = "command.bet-created-beasttokens";
                     } else if (this.currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                        messageKey = "command.bet-created-coinsengine";
                     } else if (this.currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                        messageKey = "command.bet-created-placeholder";
                     } else {
                        messageKey = "command.bet-created-money";
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-game-created")) {
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                        this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                        String choiceMessage = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("heads-tails.selected");
                        this.plugin.getAdventureHelper().sendMessage(player, choiceMessage, placeholders);
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(player, "game.create");
                     }

                     boolean broadcastEnabled = currencySettings.isBroadcastEnabled();
                     double minBroadcastAmount = currencySettings.getMinBroadcastAmount();
                     if (broadcastEnabled && this.amount >= minBroadcastAmount) {
                        Map<String, String> broadcastPlaceholders = new HashMap<>();
                        broadcastPlaceholders.put("player", player.getName());
                        String formattedBroadcastAmount = this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId);
                        broadcastPlaceholders.put("amount", formattedBroadcastAmount);
                        broadcastPlaceholders.put("symbol", unit);
                        broadcastPlaceholders.put("choice", choiceText);
                        String broadcastMsg = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.broadcast-created");
                        this.plugin
                           .getAdventureHelper()
                           .broadcastWithFilter(
                              broadcastMsg, broadcastPlaceholders, p -> this.plugin.getPlayerSettingsManager().isSettingEnabled(p, "message-broadcasts")
                           );
                     }
                  }
               }
            }
         }
      }
   }

   private String getInsufficientBalanceMessageKey(CoinFlipGame.CurrencyType currencyType) {
      if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
         return "command.not-enough-money";
      } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
         return "command.not-enough-playerpoints";
      } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
         return "command.not-enough-tokenmanager";
      } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
         return "command.not-enough-beasttokens";
      } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
         return "command.not-enough-coinsengine";
      } else {
         return currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER ? "command.not-enough-placeholder" : "command.not-enough-money";
      }
   }

   private String getInsufficientBalanceReserveMessageKey(CoinFlipGame.CurrencyType currencyType) {
      if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
         return "command.not-enough-money-reserve";
      } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
         return "command.not-enough-playerpoints-reserve";
      } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
         return "command.not-enough-tokenmanager-reserve";
      } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
         return "command.not-enough-beasttokens-reserve";
      } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
         return "command.not-enough-coinsengine-reserve";
      } else {
         return currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER ? "command.not-enough-placeholder-reserve" : "command.not-enough-money-reserve";
      }
   }
}
