package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.currency.CurrencySettings;
import com.kstudio.ultracoinflip.data.BettingLimitManager;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.AmountParser;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CreateCoinFlipGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;
   private double currentAmount = 100.0;
   private CoinFlipGame.CurrencyType selectedCurrencyType = CoinFlipGame.CurrencyType.MONEY;
   private String selectedCurrencyId = null;
   private boolean houseMode = false;

   public CreateCoinFlipGUI(KStudio plugin, Player viewer) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.setDefaultCurrency();
   }

   public void setHouseMode(boolean houseMode) {
      this.houseMode = houseMode;
   }

   public boolean isHouseMode() {
      return this.houseMode;
   }

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-create";
   }

   private void setDefaultCurrency() {
      List<CreateCoinFlipGUI.CurrencyItem> enabledCurrencies = this.getEnabledCurrencies();
      if (enabledCurrencies.isEmpty()) {
         this.selectedCurrencyType = null;
         this.selectedCurrencyId = null;
      } else {
         CreateCoinFlipGUI.CurrencyItem firstCurrency = enabledCurrencies.get(0);
         this.selectedCurrencyType = firstCurrency.type;
         this.selectedCurrencyId = firstCurrency.currencyId;
      }
   }

   private Material parseMaterial(String materialName, Material fallback) {
      return MaterialHelper.parseMaterial(materialName, fallback);
   }

   @Override
   protected Inventory createInventory() {
      String title = this.plugin.getGUIConfig().getString("create-gui.title", "&lCreate CoinFlip");
      int size = this.plugin.getGUIConfig().getInt("create-gui.size", 27);
      return this.plugin.getGuiHelper().createInventory(null, size, title, new HashMap<>());
   }

   @Override
   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         String fillerMaterialName = this.plugin.getGUIConfig().getString("create-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
         ItemStack filler = MaterialHelper.createItemStack(fillerMaterialName);
         if (filler == null) {
            filler = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
         }

         if (filler == null) {
            filler = new ItemStack(Material.GLASS_PANE);
         }

         ItemMeta fillerMeta = filler.getItemMeta();
         if (fillerMeta != null) {
            String fillerDisplayName = this.plugin.getGUIConfig().getString("create-gui.filler.display-name", " ");
            this.plugin.getGuiHelper().setDisplayName(fillerMeta, fillerDisplayName);
            this.plugin.getGuiHelper().applyItemProperties(fillerMeta, "create-gui.filler", this.plugin.getGUIConfig());
            filler.setItemMeta(fillerMeta);
         }

         List<Integer> fillerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "create-gui.filler.slots");
         Set<Integer> excludedSlots = new HashSet<>();
         Set<String> decreaseButtonKeys = (Set<String>)(this.plugin.getGUIConfig().getConfigurationSection("create-gui.decrease.buttons") != null
            ? this.plugin.getGUIConfig().getConfigurationSection("create-gui.decrease.buttons").getKeys(false)
            : new HashSet<>());

         for (String buttonKey : decreaseButtonKeys) {
            String slotPath = "create-gui.decrease.buttons." + buttonKey + ".slot";
            if (this.plugin.getGUIConfig().contains(slotPath)) {
               int slot = this.plugin.getGUIConfig().getInt(slotPath);
               if (slot >= 0 && slot < this.getInventory().getSize()) {
                  excludedSlots.add(slot);
               }
            }
         }

         Set<String> increaseButtonKeys = (Set<String>)(this.plugin.getGUIConfig().getConfigurationSection("create-gui.increase.buttons") != null
            ? this.plugin.getGUIConfig().getConfigurationSection("create-gui.increase.buttons").getKeys(false)
            : new HashSet<>());

         for (String buttonKeyx : increaseButtonKeys) {
            String slotPath = "create-gui.increase.buttons." + buttonKeyx + ".slot";
            if (this.plugin.getGUIConfig().contains(slotPath)) {
               int slot = this.plugin.getGUIConfig().getInt(slotPath);
               if (slot >= 0 && slot < this.getInventory().getSize()) {
                  excludedSlots.add(slot);
               }
            }
         }

         excludedSlots.add(this.plugin.getGUIConfig().getInt("create-gui.currency.slot", 13));
         excludedSlots.add(this.plugin.getGUIConfig().getInt("create-gui.custom-amount.slot", 22));
         excludedSlots.add(this.plugin.getGUIConfig().getInt("create-gui.create.slot", 18));
         int[] fillerSlots = fillerSlotsList.isEmpty()
            ? new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 19, 20, 21, 23, 24, 25, 26}
            : fillerSlotsList.stream()
               .mapToInt(i -> i != null ? i : -1)
               .filter(i -> i >= 0 && i < this.getInventory().getSize() && !excludedSlots.contains(i))
               .toArray();

         for (int slot : fillerSlots) {
            if (slot >= 0 && slot < this.getInventory().getSize()) {
               this.getInventory().setItem(slot, filler);
            }
         }

         Map<Integer, String> usedSlots = new HashMap<>();

         for (String buttonKeyxx : decreaseButtonKeys) {
            String buttonPath = "create-gui.decrease.buttons." + buttonKeyxx;
            if (!this.plugin.getGUIConfig().contains(buttonPath + ".slot")) {
               this.plugin
                  .getLogger()
                  .warning(
                     "[UltraCoinFlip] Decrease button '"
                        + buttonKeyxx
                        + "' is missing slot configuration in create.yml. Skipping this button. Please add 'slot: <number>' to the button configuration."
                  );
            } else {
               int slotx = this.plugin.getGUIConfig().getInt(buttonPath + ".slot");
               if (slotx >= 0 && slotx < this.getInventory().getSize()) {
                  if (usedSlots.containsKey(slotx)) {
                     this.plugin
                        .getLogger()
                        .warning(
                           "[UltraCoinFlip] Slot conflict: Decrease button '"
                              + buttonKeyxx
                              + "' at slot "
                              + slotx
                              + " conflicts with '"
                              + usedSlots.get(slotx)
                              + "'. The last button will be used. Please fix slot assignments in create.yml."
                        );
                  }

                  double amount = this.plugin.getGUIConfig().getDouble(buttonPath + ".amount", 1.0);
                  if (amount <= 0.0) {
                     this.plugin
                        .getLogger()
                        .warning(
                           "[UltraCoinFlip] Invalid amount "
                              + amount
                              + " for decrease button '"
                              + buttonKeyxx
                              + "' (must be > 0). Auto-fixed to 1.0. Please fix in create.yml."
                        );
                     amount = 1.0;
                  }

                  int level;
                  try {
                     level = Integer.parseInt(buttonKeyxx) - 1;
                     if (level < 0) {
                        level = 0;
                     }
                  } catch (NumberFormatException var22) {
                     level = 0;
                  }

                  double finalAmount = amount;
                  final int finalLevel = level;
                  usedSlots.put(slotx, "Decrease button " + buttonKeyxx);
                  this.addButton(slotx, new InventoryButton().creator(p -> this.createDecreaseButton(finalLevel, finalAmount)).consumer(event -> {
                     this.decreaseAmount(finalAmount);
                     this.updateGUI();
                  }));
               } else {
                  this.plugin
                     .getLogger()
                     .warning(
                        "[UltraCoinFlip] Invalid slot "
                           + slotx
                           + " for decrease button '"
                           + buttonKeyxx
                           + "' (GUI size: "
                           + this.getInventory().getSize()
                           + "). Skipping this button. Please fix in create.yml."
                     );
               }
            }
         }

         for (String buttonKeyxxx : increaseButtonKeys) {
            String buttonPath = "create-gui.increase.buttons." + buttonKeyxxx;
            if (!this.plugin.getGUIConfig().contains(buttonPath + ".slot")) {
               this.plugin
                  .getLogger()
                  .warning(
                     "[UltraCoinFlip] Increase button '"
                        + buttonKeyxxx
                        + "' is missing slot configuration in create.yml. Skipping this button. Please add 'slot: <number>' to the button configuration."
                  );
            } else {
               int slotx = this.plugin.getGUIConfig().getInt(buttonPath + ".slot");
               if (slotx >= 0 && slotx < this.getInventory().getSize()) {
                  if (usedSlots.containsKey(slotx)) {
                     this.plugin
                        .getLogger()
                        .warning(
                           "[UltraCoinFlip] Slot conflict: Increase button '"
                              + buttonKeyxxx
                              + "' at slot "
                              + slotx
                              + " conflicts with '"
                              + usedSlots.get(slotx)
                              + "'. The last button will be used. Please fix slot assignments in create.yml."
                        );
                  }

                  double amountx = this.plugin.getGUIConfig().getDouble(buttonPath + ".amount", 1.0);
                  if (amountx <= 0.0) {
                     this.plugin
                        .getLogger()
                        .warning(
                           "[UltraCoinFlip] Invalid amount "
                              + amountx
                              + " for increase button '"
                              + buttonKeyxxx
                              + "' (must be > 0). Auto-fixed to 1.0. Please fix in create.yml."
                        );
                     amountx = 1.0;
                  }

                  int level;
                  try {
                     level = Integer.parseInt(buttonKeyxxx) - 1;
                     if (level < 0) {
                        level = 0;
                     }
                  } catch (NumberFormatException var21) {
                     level = 0;
                  }

                  int finalLevel = level;
                  double finalAmount = amountx;
                  usedSlots.put(slotx, "Increase button " + buttonKeyxxx);
                  this.addButton(slotx, new InventoryButton().creator(p -> this.createIncreaseButton(finalLevel, finalAmount)).consumer(event -> {
                     this.increaseAmount(finalAmount);
                     this.updateGUI();
                  }));
               } else {
                  this.plugin
                     .getLogger()
                     .warning(
                        "[UltraCoinFlip] Invalid slot "
                           + slotx
                           + " for increase button '"
                           + buttonKeyxxx
                           + "' (GUI size: "
                           + this.getInventory().getSize()
                           + "). Skipping this button. Please fix in create.yml."
                     );
               }
            }
         }

         int currencySlot = this.plugin.getGUIConfig().getInt("create-gui.currency.slot", 13);
         int customAmountSlot = this.plugin.getGUIConfig().getInt("create-gui.custom-amount.slot", 22);
         int createSlot = this.plugin.getGUIConfig().getInt("create-gui.create.slot", 18);
         if (usedSlots.containsKey(currencySlot)) {
            this.plugin
               .getLogger()
               .warning(
                  "[UltraCoinFlip] Slot conflict: Currency button at slot "
                     + currencySlot
                     + " conflicts with '"
                     + usedSlots.get(currencySlot)
                     + "'. The last button will be used. Please fix slot assignments in create.yml."
               );
         }

         if (usedSlots.containsKey(customAmountSlot)) {
            this.plugin
               .getLogger()
               .warning(
                  "[UltraCoinFlip] Slot conflict: Custom amount button at slot "
                     + customAmountSlot
                     + " conflicts with '"
                     + usedSlots.get(customAmountSlot)
                     + "'. The last button will be used. Please fix slot assignments in create.yml."
               );
         }

         if (usedSlots.containsKey(createSlot)) {
            this.plugin
               .getLogger()
               .warning(
                  "[UltraCoinFlip] Slot conflict: Create button at slot "
                     + createSlot
                     + " conflicts with '"
                     + usedSlots.get(createSlot)
                     + "'. The last button will be used. Please fix slot assignments in create.yml."
               );
         }

         this.addButton(
            currencySlot,
            new InventoryButton()
               .creator(p -> this.createCurrencyButton())
               .consumer(
                  event -> {
                     List<CreateCoinFlipGUI.CurrencyItem> enabledCurrencies = this.getEnabledCurrencies();
                     if (enabledCurrencies.isEmpty()) {
                        this.plugin
                           .getLogger()
                           .warning(
                              "[UltraCoinFlip] No enabled currencies found! Players cannot create coinflip games. Please enable at least one currency in the currencies folder."
                           );
                     } else {
                        int currentIndex = -1;

                        for (int i = 0; i < enabledCurrencies.size(); i++) {
                           CreateCoinFlipGUI.CurrencyItem item = enabledCurrencies.get(i);
                           boolean typeMatches = item.type == this.selectedCurrencyType;
                           boolean idMatches;
                           if (this.selectedCurrencyId == null && item.currencyId == null) {
                              idMatches = true;
                           } else if (this.selectedCurrencyId != null && item.currencyId != null) {
                              idMatches = this.selectedCurrencyId.equals(item.currencyId);
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
                        CreateCoinFlipGUI.CurrencyItem nextCurrency = enabledCurrencies.get(nextIndex);
                        this.selectedCurrencyType = nextCurrency.type;
                        this.selectedCurrencyId = nextCurrency.currencyId;
                        FoliaScheduler.runTask(this.plugin, this.viewer, () -> {
                           if (this.viewer != null && this.viewer.isOnline()) {
                              try {
                                 Inventory topInv = GUIHelper.getTopInventorySafely(this.viewer);
                                 if (topInv != null && topInv.equals(this.getInventory())) {
                                    this.updateCurrencyButton();
                                    this.updateCreateButton();
                                    super.decorate(this.viewer);
                                    this.plugin.getInventoryUpdateBatcher().scheduleUpdate(this.viewer);
                                 }
                              } catch (Exception var2x) {
                              }
                           }
                        });
                     }
                  }
               )
         );
         this.addButton(
            customAmountSlot,
            new InventoryButton()
               .creator(p -> this.createCustomAmountButton())
               .consumer(
                  event -> {
                     String inputMethod = this.plugin.getConfig().getString("input.method", "CHAT").toUpperCase();
                     player.closeInventory();
                     if ("ANVIL".equals(inputMethod)) {
                        String anvilTitle = this.plugin.getGUIConfig().getString("create-gui.custom-amount.anvil.title", "&6Enter Amount");
                        String initialTextTemplate = this.plugin.getGUIConfig().getString("create-gui.custom-amount.anvil.initial-text", "&e<current>");
                        String placeholder = this.plugin
                           .getGUIConfig()
                           .getString("create-gui.custom-amount.anvil.placeholder", "Enter amount (1K, 1M, 1B, 1T)");
                        String initialText = "";
                        if (initialTextTemplate != null && !initialTextTemplate.isEmpty()) {
                           if (!initialTextTemplate.contains("<current>")) {
                              initialText = initialTextTemplate;
                           } else if (this.currentAmount > 0.0) {
                              String plainAmount;
                              if (this.currentAmount == Math.floor(this.currentAmount)) {
                                 plainAmount = String.format("%.0f", this.currentAmount);
                              } else {
                                 plainAmount = String.format("%.2f", this.currentAmount);
                                 plainAmount = plainAmount.replaceAll("\\.0+$", "").replaceAll("(\\.[0-9]*[1-9])0+$", "$1");
                              }

                              initialText = initialTextTemplate.replace("<current>", plainAmount);
                           } else if (placeholder != null && !placeholder.isEmpty()) {
                              initialText = placeholder;
                           } else {
                              initialText = "";
                           }
                        } else if (placeholder != null && !placeholder.isEmpty()) {
                           initialText = placeholder;
                        }

                        String parsedTitle = this.plugin.getAdventureHelper().parseToLegacy(anvilTitle);
                        this.plugin.getAnvilInputManager().requestInput(player, parsedTitle, initialText, input -> {
                           try {
                              double amountxx = AmountParser.parseFormattedAmount(input);
                              if (amountxx <= 0.0) {
                                 if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                                    this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                                 }

                                 if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                                    String errorMsg = this.plugin.getMessage("prefix") + " &cAmount must be greater than 0!";
                                    this.plugin.getAdventureHelper().sendMessage(player, errorMsg);
                                 }

                                 return;
                              }

                              CreateCoinFlipGUI newGUI = new CreateCoinFlipGUI(this.plugin, this.viewer);
                              newGUI.setSelectedCurrency(this.selectedCurrencyType, this.selectedCurrencyId);
                              newGUI.setCurrentAmount(amountxx);
                              this.plugin.getGuiManager().openGUI(newGUI, this.viewer);
                           } catch (IllegalArgumentException var6x) {
                              if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                                 this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                              }

                              if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                                 String errorMsg = this.plugin.getMessage("prefix") + " &c" + var6x.getMessage();
                                 this.plugin.getAdventureHelper().sendMessage(player, errorMsg);
                              }
                           }
                        });
                     } else {
                        String messageTemplate = this.plugin
                           .getGUIConfig()
                           .getString(
                              "create-gui.custom-amount.chat-message", this.plugin.getMessage("prefix") + " &7Enter amount (supports format: 1K, 1M, 1B, 1T):"
                           );
                        this.plugin.getAdventureHelper().sendMessage(player, messageTemplate);
                        this.plugin.getChatInputManager().requestInput(player, input -> {
                           try {
                              double amountxx = AmountParser.parseFormattedAmount(input);
                              if (amountxx <= 0.0) {
                                 if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                                    this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                                 }

                                 if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                                    String errorMsg = this.plugin.getMessage("prefix") + " &cAmount must be greater than 0!";
                                    this.plugin.getAdventureHelper().sendMessage(player, errorMsg);
                                 }

                                 return;
                              }

                              CreateCoinFlipGUI newGUI = new CreateCoinFlipGUI(this.plugin, this.viewer);
                              newGUI.setSelectedCurrency(this.selectedCurrencyType, this.selectedCurrencyId);
                              newGUI.setCurrentAmount(amountxx);
                              this.plugin.getGuiManager().openGUI(newGUI, this.viewer);
                           } catch (IllegalArgumentException var6x) {
                              if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                                 this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                              }

                              if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                                 String errorMsg = this.plugin.getMessage("prefix") + " &c" + var6x.getMessage();
                                 this.plugin.getAdventureHelper().sendMessage(player, errorMsg);
                              }
                           }
                        });
                     }
                  }
               )
         );
         this.addButton(
            createSlot,
            new InventoryButton()
               .creator(p -> this.createCreateButton())
               .consumer(
                  event -> {
                     if (this.selectedCurrencyType == null
                        || !this.plugin.getCurrencyManager().isCurrencyEnabled(this.selectedCurrencyType, this.selectedCurrencyId)) {
                        String message = this.plugin.getMessage("prefix") + " &cPlease select a currency before creating a game!";
                        this.plugin.getAdventureHelper().sendMessage(player, message);
                     } else if (!this.plugin.getCurrencyManager().canPlayerUseCurrency(player, this.selectedCurrencyType, this.selectedCurrencyId)) {
                        String restrictionReason = this.plugin
                           .getCurrencyManager()
                           .getRestrictionReason(player, this.selectedCurrencyType, this.selectedCurrencyId);
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("restriction.cannot-use-currency");
                        if (restrictionReason != null && !restrictionReason.isEmpty()) {
                           message = message + " " + restrictionReason;
                        }

                        this.plugin.getAdventureHelper().sendMessage(player, message);
                     } else if (!this.plugin
                        .getCurrencyManager()
                        .hasBalanceWithReserve(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount)) {
                        if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                           this.plugin.getSoundHelper().playSound(player, "error.not-enough-money");
                        }

                        if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                           double currentBalance = this.plugin.getCurrencyManager().getBalance(player, this.selectedCurrencyType, this.selectedCurrencyId);
                           String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, this.selectedCurrencyId);
                           String formattedAmount = this.plugin.getGuiHelper().formatAmount(this.currentAmount, this.selectedCurrencyId);
                           boolean isReserveIssue = this.plugin
                              .getCurrencyManager()
                              .isReserveBalanceIssue(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount);
                           Map<String, String> placeholders = new HashMap<>();
                           String messageKey;
                           if (isReserveIssue) {
                              double minReserve = this.plugin.getCurrencyManager().getMinReserveBalance(this.selectedCurrencyType, this.selectedCurrencyId);
                              double maxBet = currentBalance - minReserve;
                              String formattedReserve = this.plugin.getGuiHelper().formatAmount(minReserve, this.selectedCurrencyId);
                              String formattedMaxBet = this.plugin.getGuiHelper().formatAmount(maxBet, this.selectedCurrencyId);
                              if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.MONEY) {
                                 messageKey = "command.not-enough-money-reserve";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                                 messageKey = "command.not-enough-playerpoints-reserve";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                                 messageKey = "command.not-enough-tokenmanager-reserve";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                                 messageKey = "command.not-enough-beasttokens-reserve";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                                 messageKey = "command.not-enough-coinsengine-reserve";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                                 messageKey = "command.not-enough-placeholder-reserve";
                              } else {
                                 messageKey = "command.not-enough-money-reserve";
                              }

                              placeholders.put("reserve", formattedReserve);
                              placeholders.put("balance", formattedBalance);
                              placeholders.put("max_bet", formattedMaxBet);
                           } else {
                              if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.MONEY) {
                                 messageKey = "command.not-enough-money";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                                 messageKey = "command.not-enough-playerpoints";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                                 messageKey = "command.not-enough-tokenmanager";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                                 messageKey = "command.not-enough-beasttokens";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                                 messageKey = "command.not-enough-coinsengine";
                              } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                                 messageKey = "command.not-enough-placeholder";
                              } else {
                                 messageKey = "command.not-enough-money";
                              }

                              placeholders.put("amount", formattedAmount);
                              placeholders.put("balance", formattedBalance);
                           }

                           String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                           this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                        }
                     } else {
                        this.createCoinFlip(player);
                     }
                  }
               )
         );
         super.decorate(player);
      }
   }

   private ItemStack createDecreaseButton(int level, double amount) {
      String buttonPath = "create-gui.decrease.buttons." + (level + 1);
      String materialName = this.plugin
         .getGUIConfig()
         .getString(buttonPath + ".material", this.plugin.getGUIConfig().getString("create-gui.decrease.material", "RED_STAINED_GLASS_PANE"));
      ItemStack item = MaterialHelper.createItemStack(materialName);
      if (item == null) {
         item = MaterialHelper.getRedStainedGlassPaneItem();
         if (item == null) {
            item = new ItemStack(MaterialHelper.getRedStainedGlassPane() != null ? MaterialHelper.getRedStainedGlassPane() : Material.GLASS_PANE);
         }
      }

      String titleTemplate = this.plugin
         .getGUIConfig()
         .getString(buttonPath + ".title", this.plugin.getGUIConfig().getString("create-gui.decrease.title", "&r&c&lDecrease <amount>"));
      Map<String, String> placeholders = new HashMap<>();
      String formattedAmount = this.formatAmount(amount);
      placeholders.put("amount", formattedAmount);
      String title = this.plugin.getAdventureHelper().parseToLegacy(titleTemplate, placeholders);
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList(buttonPath + ".lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.decrease.lore");
         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&r&7Click to decrease amount");
            loreTemplate.add("&r&7by <amount>");
         }
      }

      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         String defaultPath = "create-gui.decrease";
         Boolean glowing = null;
         Integer customModelData = null;
         if (this.plugin.getGUIConfig().contains(buttonPath + ".glowing")) {
            glowing = this.plugin.getGUIConfig().getBoolean(buttonPath + ".glowing", false);
         } else if (this.plugin.getGUIConfig().contains(defaultPath + ".glowing")) {
            glowing = this.plugin.getGUIConfig().getBoolean(defaultPath + ".glowing", false);
         } else {
            glowing = false;
         }

         if (this.plugin.getGUIConfig().contains(buttonPath + ".custom-model-data")) {
            customModelData = this.plugin.getGUIConfig().getInt(buttonPath + ".custom-model-data", 0);
         } else if (this.plugin.getGUIConfig().contains(defaultPath + ".custom-model-data")) {
            customModelData = this.plugin.getGUIConfig().getInt(defaultPath + ".custom-model-data", 0);
         } else {
            customModelData = 0;
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, glowing, customModelData);
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createIncreaseButton(int level, double amount) {
      String buttonPath = "create-gui.increase.buttons." + (level + 1);
      String materialName = this.plugin
         .getGUIConfig()
         .getString(buttonPath + ".material", this.plugin.getGUIConfig().getString("create-gui.increase.material", "LIME_STAINED_GLASS_PANE"));
      ItemStack item = MaterialHelper.createItemStack(materialName);
      if (item == null) {
         item = MaterialHelper.getLimeStainedGlassPaneItem();
         if (item == null) {
            item = new ItemStack(MaterialHelper.getLimeStainedGlassPane() != null ? MaterialHelper.getLimeStainedGlassPane() : Material.GLASS_PANE);
         }
      }

      String titleTemplate = this.plugin
         .getGUIConfig()
         .getString(buttonPath + ".title", this.plugin.getGUIConfig().getString("create-gui.increase.title", "&r&a&lIncrease <amount>"));
      Map<String, String> placeholders = new HashMap<>();
      String formattedAmount = this.formatAmount(amount);
      placeholders.put("amount", formattedAmount);
      String title = this.plugin.getAdventureHelper().parseToLegacy(titleTemplate, placeholders);
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList(buttonPath + ".lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.increase.lore");
         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&r&7Click to increase amount");
            loreTemplate.add("&r&7by <amount>");
         }
      }

      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         String defaultPath = "create-gui.increase";
         Boolean glowing = null;
         Integer customModelData = null;
         if (this.plugin.getGUIConfig().contains(buttonPath + ".glowing")) {
            glowing = this.plugin.getGUIConfig().getBoolean(buttonPath + ".glowing", false);
         } else if (this.plugin.getGUIConfig().contains(defaultPath + ".glowing")) {
            glowing = this.plugin.getGUIConfig().getBoolean(defaultPath + ".glowing", false);
         } else {
            glowing = false;
         }

         if (this.plugin.getGUIConfig().contains(buttonPath + ".custom-model-data")) {
            customModelData = this.plugin.getGUIConfig().getInt(buttonPath + ".custom-model-data", 0);
         } else if (this.plugin.getGUIConfig().contains(defaultPath + ".custom-model-data")) {
            customModelData = this.plugin.getGUIConfig().getInt(defaultPath + ".custom-model-data", 0);
         } else {
            customModelData = 0;
         }

         this.plugin.getGuiHelper().applyItemProperties(meta, glowing, customModelData);
         item.setItemMeta(meta);
      }

      return item;
   }

   private void updateCurrencyButton() {
      Inventory inventory = this.getInventory();
      if (inventory != null) {
         int currencySlot = this.plugin.getGUIConfig().getInt("create-gui.currency.slot", 4);
         ItemStack currencyItem = this.createCurrencyButton();
         if (currencySlot >= 0 && currencySlot < inventory.getSize()) {
            inventory.setItem(currencySlot, currencyItem);
         }
      }
   }

   private void updateCreateButton() {
      Inventory inventory = this.getInventory();
      if (inventory != null) {
         int createSlot = this.plugin.getGUIConfig().getInt("create-gui.create.slot", 13);
         ItemStack createItem = this.createCreateButton();
         if (createSlot >= 0 && createSlot < inventory.getSize()) {
            inventory.setItem(createSlot, createItem);
         }
      }
   }

   private ItemStack createCurrencyButton() {
      String materialName = this.plugin.getGUIConfig().getString("create-gui.currency.material", "SUNFLOWER");
      Material material = MaterialHelper.parseMaterial(materialName, Material.GOLD_INGOT);
      String title = this.plugin.getGUIConfig().getString("create-gui.currency.title", "&6&lChange Currency");
      String currencyDisplayName = this.selectedCurrencyType != null
         ? this.plugin.getCurrencyManager().getDisplayName(this.selectedCurrencyType, this.selectedCurrencyId)
         : "Not selected";
      String unit = this.selectedCurrencyType != null ? this.plugin.getCurrencyManager().getUnit(this.selectedCurrencyType, this.selectedCurrencyId) : "";
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.currency.lore");
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
         this.plugin.getGuiHelper().applyItemProperties(meta, "create-gui.currency", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private List<CreateCoinFlipGUI.CurrencyItem> getEnabledCurrencies() {
      List<CreateCoinFlipGUI.CurrencyItem> currencies = new ArrayList<>();
      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.MONEY)
         && (this.viewer == null || this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, CoinFlipGame.CurrencyType.MONEY, null))) {
         currencies.add(new CreateCoinFlipGUI.CurrencyItem(CoinFlipGame.CurrencyType.MONEY, null));
      }

      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.PLAYERPOINTS)
         && (this.viewer == null || this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, CoinFlipGame.CurrencyType.PLAYERPOINTS, null))) {
         currencies.add(new CreateCoinFlipGUI.CurrencyItem(CoinFlipGame.CurrencyType.PLAYERPOINTS, null));
      }

      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.TOKENMANAGER)
         && (this.viewer == null || this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, CoinFlipGame.CurrencyType.TOKENMANAGER, null))) {
         currencies.add(new CreateCoinFlipGUI.CurrencyItem(CoinFlipGame.CurrencyType.TOKENMANAGER, null));
      }

      if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.BEASTTOKENS)
         && (this.viewer == null || this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, CoinFlipGame.CurrencyType.BEASTTOKENS, null))) {
         currencies.add(new CreateCoinFlipGUI.CurrencyItem(CoinFlipGame.CurrencyType.BEASTTOKENS, null));
      }

      for (String id : this.plugin.getCurrencyManager().getEnabledCoinsEngineCurrencyIds()) {
         if (this.viewer == null || this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, CoinFlipGame.CurrencyType.COINSENGINE, id)) {
            currencies.add(new CreateCoinFlipGUI.CurrencyItem(CoinFlipGame.CurrencyType.COINSENGINE, id));
         }
      }

      for (String idx : this.plugin.getCurrencyManager().getEnabledPlaceholderCurrencyIds()) {
         if (this.viewer == null || this.plugin.getCurrencyManager().canPlayerUseCurrency(this.viewer, CoinFlipGame.CurrencyType.PLACEHOLDER, idx)) {
            currencies.add(new CreateCoinFlipGUI.CurrencyItem(CoinFlipGame.CurrencyType.PLACEHOLDER, idx));
         }
      }

      return currencies;
   }

   private ItemStack createCustomAmountButton() {
      String materialName = this.plugin.getGUIConfig().getString("create-gui.custom-amount.material", "ANVIL");

      Material material;
      try {
         material = Material.valueOf(materialName.toUpperCase());
      } catch (IllegalArgumentException var8) {
         material = Material.ANVIL;
      }

      String title = this.plugin.getGUIConfig().getString("create-gui.custom-amount.title", "&r&6&lEnter Custom Amount");
      List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.custom-amount.lore");
      if (loreTemplate == null || loreTemplate.isEmpty()) {
         loreTemplate = new ArrayList<>();
         loreTemplate.add("&r&7Click to enter amount");
         loreTemplate.add("&r&7Supports format: &e1K, 1M, 1B, 1T");
      }

      List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, new HashMap<>());
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         this.plugin.getGuiHelper().applyItemProperties(meta, "create-gui.custom-amount", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createCreateButton() {
      Material material = Material.NETHER_STAR;
      String title = "&r&a&lCreate CoinFlip";
      Map<String, String> placeholders = new HashMap<>();
      List<?> lore;
      if (this.selectedCurrencyType != null && this.plugin.getCurrencyManager().isCurrencyEnabled(this.selectedCurrencyType, this.selectedCurrencyId)) {
         String currencyUnit = this.plugin.getCurrencyManager().getUnit(this.selectedCurrencyType, this.selectedCurrencyId);
         String currencyName = this.plugin.getCurrencyManager().getDisplayName(this.selectedCurrencyType, this.selectedCurrencyId);
         placeholders.put("amount", this.formatAmount(this.currentAmount));
         placeholders.put("currency", currencyName);
         placeholders.put("unit", currencyUnit);
         if (!this.plugin.getCurrencyManager().hasBalanceWithReserve(this.viewer, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount)) {
            String materialName = this.plugin.getGUIConfig().getString("create-gui.create.material-insufficient", "BARRIER");

            try {
               material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException var13) {
               material = MaterialHelper.getBarrierMaterial();
            }

            String titleTemplate = this.plugin.getGUIConfig().getString("create-gui.create.title-insufficient", "&r&c&lInsufficient Balance");
            title = this.plugin.getAdventureHelper().parseToLegacy(titleTemplate, placeholders);
            List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.create.lore-insufficient");
            if (loreTemplate == null || loreTemplate.isEmpty()) {
               loreTemplate = new ArrayList<>();
               loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
               loreTemplate.add("&r&fAmount: &e<amount><unit>");
               loreTemplate.add("&r&fCurrency: &b<currency>");
               loreTemplate.add("&r&fCurrent balance: &c<balance><unit>");
               loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
               loreTemplate.add("&r&c&l» Insufficient balance!");
            }

            double balance = this.plugin.getCurrencyManager().getBalance(this.viewer, this.selectedCurrencyType, this.selectedCurrencyId);
            placeholders.put("balance", this.formatAmount(balance));
            lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
         } else {
            String materialName = this.plugin.getGUIConfig().getString("create-gui.create.material", "NETHER_STAR");

            try {
               material = Material.valueOf(materialName.toUpperCase());
            } catch (IllegalArgumentException var12) {
               material = Material.NETHER_STAR;
            }

            String titleTemplate = this.plugin.getGUIConfig().getString("create-gui.create.title", "&r&a&lCreate CoinFlip");
            title = this.plugin.getAdventureHelper().parseToLegacy(titleTemplate, placeholders);
            List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.create.lore");
            if (loreTemplate == null || loreTemplate.isEmpty()) {
               loreTemplate = new ArrayList<>();
               loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
               loreTemplate.add("&r&fAmount: &e<amount><unit>");
               loreTemplate.add("&r&fCurrency: &b<currency>");
               loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━");
               loreTemplate.add("&r&a&l» Click to create game");
            }

            lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
         }
      } else {
         String materialName = this.plugin.getGUIConfig().getString("create-gui.create.material-disabled", "BARRIER");

         try {
            material = Material.valueOf(materialName.toUpperCase());
         } catch (IllegalArgumentException var14) {
            material = MaterialHelper.getBarrierMaterial();
         }

         String titleTemplate = this.plugin.getGUIConfig().getString("create-gui.create.title-no-currency", "&r&c&lNo Currency Selected");
         title = this.plugin.getAdventureHelper().parseToLegacy(titleTemplate, new HashMap<>());
         List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("create-gui.create.lore-no-currency");
         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&r&7Please select a currency");
            loreTemplate.add("&r&7before creating a game");
         }

         lore = this.plugin.getGuiHelper().createLore(loreTemplate, new HashMap<>());
      }

      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         this.plugin.getGuiHelper().setDisplayName(meta, title);
         this.plugin.getGuiHelper().setLore(meta, lore);
         this.plugin.getGuiHelper().applyItemProperties(meta, "create-gui.create", this.plugin.getGUIConfig());
         item.setItemMeta(meta);
      }

      return item;
   }

   private void decreaseAmount(double amount) {
      double newAmount = this.currentAmount - amount;
      if (this.selectedCurrencyType != null && this.plugin.getCurrencyManager().isCurrencyEnabled(this.selectedCurrencyType, this.selectedCurrencyId)) {
         try {
            CurrencySettings currencySettings = this.plugin.getCurrencyManager().getCurrencySettings(this.selectedCurrencyType, this.selectedCurrencyId);
            double minBid = currencySettings.getMinBid();
            if (newAmount < minBid) {
               this.currentAmount = 0.0;
            } else {
               this.currentAmount = newAmount;
            }
         } catch (Exception var8) {
            this.currentAmount = Math.max(0.0, newAmount);
         }
      } else {
         this.currentAmount = Math.max(0.0, newAmount);
      }
   }

   private void increaseAmount(double amount) {
      this.currentAmount += amount;
      if (this.currentAmount > 8.988465674311579E307) {
         this.currentAmount = 8.988465674311579E307;
      }

      if (this.selectedCurrencyType != null && this.plugin.getCurrencyManager().isCurrencyEnabled(this.selectedCurrencyType, this.selectedCurrencyId)) {
         try {
            CurrencySettings currencySettings = this.plugin.getCurrencyManager().getCurrencySettings(this.selectedCurrencyType, this.selectedCurrencyId);
            double maxBid = currencySettings.getMaxBid();
            if (maxBid > 0.0 && this.currentAmount > maxBid) {
               this.currentAmount = maxBid;
            }
         } catch (Exception var6) {
         }
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

   private void createCoinFlip(Player player) {
      if (this.currentAmount <= 0.0) {
         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
            String message = this.plugin.getMessage("prefix") + " &cAmount must be greater than 0!";
            this.plugin.getAdventureHelper().sendMessage(player, message);
         }
      } else {
         CurrencySettings currencySettings = this.plugin.getCurrencyManager().getCurrencySettings(this.selectedCurrencyType, this.selectedCurrencyId);
         double minBid = currencySettings.getMinBid();
         if (this.currentAmount < minBid) {
            if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
               this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
            }

            if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
               Map<String, String> placeholders = new HashMap<>();
               String formattedMinBid = this.plugin.getGuiHelper().formatAmount(minBid, this.selectedCurrencyId);
               placeholders.put("amount", formattedMinBid);
               String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.min-bid");
               this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
            }
         } else {
            double maxBid = currencySettings.getMaxBid();
            if (maxBid != -1.0 && this.currentAmount > maxBid) {
               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
               }

               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                  Map<String, String> placeholders = new HashMap<>();
                  String formattedMaxBid = this.plugin.getGuiHelper().formatAmount(maxBid, this.selectedCurrencyId);
                  placeholders.put("amount", formattedMaxBid);
                  String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.max-bid");
                  this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
               }
            } else if (!this.plugin.getCurrencyManager().hasBalanceWithReserve(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount)) {
               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(player, "error.not-enough-money");
               }

               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                  double currentBalance = this.plugin.getCurrencyManager().getBalance(player, this.selectedCurrencyType, this.selectedCurrencyId);
                  String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, this.selectedCurrencyId);
                  String formattedAmount = this.plugin.getGuiHelper().formatAmount(this.currentAmount, this.selectedCurrencyId);
                  boolean isReserveIssue = this.plugin
                     .getCurrencyManager()
                     .isReserveBalanceIssue(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount);
                  Map<String, String> placeholders = new HashMap<>();
                  String messageKey;
                  if (isReserveIssue) {
                     double minReserve = this.plugin.getCurrencyManager().getMinReserveBalance(this.selectedCurrencyType, this.selectedCurrencyId);
                     double maxBet = Math.max(0.0, currentBalance - minReserve);
                     String formattedReserve = this.plugin.getGuiHelper().formatAmount(minReserve, this.selectedCurrencyId);
                     String formattedMaxBet = this.plugin.getGuiHelper().formatAmount(maxBet, this.selectedCurrencyId);
                     if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.MONEY) {
                        messageKey = "command.not-enough-money-reserve";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                        messageKey = "command.not-enough-playerpoints-reserve";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                        messageKey = "command.not-enough-tokenmanager-reserve";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                        messageKey = "command.not-enough-beasttokens-reserve";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                        messageKey = "command.not-enough-coinsengine-reserve";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                        messageKey = "command.not-enough-placeholder-reserve";
                     } else {
                        messageKey = "command.not-enough-money-reserve";
                     }

                     placeholders.put("reserve", formattedReserve);
                     placeholders.put("balance", formattedBalance);
                     placeholders.put("max_bet", formattedMaxBet);
                  } else {
                     if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.MONEY) {
                        messageKey = "command.not-enough-money";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                        messageKey = "command.not-enough-playerpoints";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                        messageKey = "command.not-enough-tokenmanager";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                        messageKey = "command.not-enough-beasttokens";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                        messageKey = "command.not-enough-coinsengine";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                        messageKey = "command.not-enough-placeholder";
                     } else {
                        messageKey = "command.not-enough-money";
                     }

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
            } else if (!this.plugin.getCoinFlipManager().canCreateMoreGames(player)) {
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
            } else {
               BettingLimitManager.LimitCheckResult limitResult = this.plugin
                  .getBettingLimitManager()
                  .canPlayerBet(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount);
               if (limitResult != null) {
                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                     this.plugin.getSoundHelper().playSound(player, "error.general");
                  }

                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-error")) {
                     Map<String, String> placeholders = new HashMap<>();
                     placeholders.put("limit", this.plugin.getGuiHelper().formatAmount(limitResult.getLimit(), this.selectedCurrencyId));
                     placeholders.put("current", this.plugin.getGuiHelper().formatAmount(limitResult.getCurrentTotal(), this.selectedCurrencyId));
                     placeholders.put("remaining", this.plugin.getGuiHelper().formatAmount(limitResult.getRemaining(), this.selectedCurrencyId));
                     String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command." + limitResult.getMessageKey());
                     this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                  }
               } else {
                  boolean headsTailsEnabled = this.plugin.getConfig().getBoolean("heads-tails.enabled", false);
                  if (headsTailsEnabled) {
                     FoliaScheduler.runTask(this.plugin, player, () -> {
                        if (player != null && player.isOnline()) {
                           player.closeInventory();
                        }
                     });
                     FoliaScheduler.runTaskLater(
                        this.plugin,
                        player,
                        () -> {
                           if (player != null && player.isOnline()) {
                              this.plugin
                                 .getGuiManager()
                                 .openGUI(
                                    new HeadsTailsSelectionGUI(
                                       this.plugin, player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount, () -> {
                                          if (player != null && player.isOnline()) {
                                             this.plugin.getGuiManager().openGUI(new CreateCoinFlipGUI(this.plugin, player), player);
                                          }
                                       }
                                    ),
                                    player
                                 );
                           }
                        },
                        2L
                     );
                  } else {
                     if (this.houseMode) {
                        this.plugin.getCoinFlipManager().createHouseGame(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount);
                        return;
                     }

                     if (!this.plugin.getCurrencyManager().withdraw(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount)) {
                        String message = this.plugin.getMessage("prefix") + " &cFailed to withdraw currency!";
                        this.plugin.getAdventureHelper().sendMessage(player, message);
                        return;
                     }

                     this.plugin.getCoinFlipManager().createGame(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount);
                     this.plugin.getBettingLimitManager().recordBet(player, this.selectedCurrencyType, this.selectedCurrencyId, this.currentAmount);
                     Map<String, String> placeholders = new HashMap<>();
                     String formattedAmount = this.plugin.getGuiHelper().formatAmount(this.currentAmount, this.selectedCurrencyId);
                     placeholders.put("amount", formattedAmount);
                     String unit = this.plugin.getCurrencyManager().getUnit(this.selectedCurrencyType, this.selectedCurrencyId);
                     placeholders.put("symbol", unit);
                     String messageKey;
                     if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.MONEY) {
                        messageKey = "command.bet-created-money";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                        messageKey = "command.bet-created-playerpoints";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                        messageKey = "command.bet-created-tokenmanager";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                        messageKey = "command.bet-created-beasttokens";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                        messageKey = "command.bet-created-coinsengine";
                     } else if (this.selectedCurrencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                        messageKey = "command.bet-created-placeholder";
                     } else {
                        messageKey = "command.bet-created-money";
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-game-created")) {
                        String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
                        this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
                        this.plugin.getSoundHelper().playSound(player, "game.create");
                     }

                     boolean broadcastEnabled = currencySettings.isBroadcastEnabled();
                     double minBroadcastAmount = currencySettings.getMinBroadcastAmount();
                     if (broadcastEnabled && this.currentAmount >= minBroadcastAmount) {
                        Map<String, String> broadcastPlaceholders = new HashMap<>();
                        broadcastPlaceholders.put("player", player.getName());
                        String formattedBroadcastAmount = this.plugin.getGuiHelper().formatAmount(this.currentAmount, this.selectedCurrencyId);
                        broadcastPlaceholders.put("amount", formattedBroadcastAmount);
                        broadcastPlaceholders.put("symbol", unit);
                        String broadcastMsg = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.broadcast-created");
                        this.plugin
                           .getAdventureHelper()
                           .broadcastWithFilter(
                              broadcastMsg, broadcastPlaceholders, p -> this.plugin.getPlayerSettingsManager().isSettingEnabled(p, "message-broadcasts")
                           );
                     }

                     player.closeInventory();
                     this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, this.viewer, 1), this.viewer);
                  }
               }
            }
         }
      }
   }

   private String formatAmount(double amount) {
      return AmountParser.formatAmount(amount);
   }

   public CoinFlipGame.CurrencyType getSelectedCurrencyType() {
      return this.selectedCurrencyType;
   }

   public String getSelectedCurrencyId() {
      return this.selectedCurrencyId;
   }

   public void setSelectedCurrency(CoinFlipGame.CurrencyType type, String currencyId) {
      this.selectedCurrencyType = type;
      this.selectedCurrencyId = currencyId;
   }

   public double getCurrentAmount() {
      return this.currentAmount;
   }

   public void setCurrentAmount(double amount) {
      this.currentAmount = amount;
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
