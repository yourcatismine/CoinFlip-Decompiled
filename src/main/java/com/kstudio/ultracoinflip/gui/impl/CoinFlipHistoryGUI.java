package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.CoinFlipLog;
import com.kstudio.ultracoinflip.data.PlayerStats;
import com.kstudio.ultracoinflip.gui.GUIHelper;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.LegacyCompatibility;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class CoinFlipHistoryGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player viewer;
   private final int page;

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-history";
   }

   private Material parseMaterial(String materialName, Material fallback) {
      return MaterialHelper.parseMaterial(materialName, fallback);
   }

   @Override
   protected Inventory createInventory() {
      String titleTemplate = this.plugin.getGUIConfig().getString("history-gui.title", "&lCoinFlip History &8[&7Page <page>&8]");
      int size = this.plugin.getGUIConfig().getInt("history-gui.size", 45);
      Map<String, String> placeholders = Collections.singletonMap("page", String.valueOf(this.page));
      return this.plugin.getGuiHelper().createInventory(null, size, titleTemplate, placeholders);
   }

   @Override
   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         try {
            List<CoinFlipLog> allLogs = this.plugin.getDatabaseManager().getPlayerLogs(player.getUniqueId(), 0);
            if (allLogs == null) {
               allLogs = new ArrayList<>();
            }

            int elementsPerPage = this.plugin.getGUIConfig().getInt("history-gui.items-per-page", 21);
            int startIndex = (this.page - 1) * elementsPerPage;
            String fillerMaterialName = this.plugin.getGUIConfig().getString("history-gui.filler.material", "BLACK_STAINED_GLASS_PANE");
            ItemStack filler = MaterialHelper.createItemStack(fillerMaterialName);
            if (filler == null) {
               filler = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
            }

            if (filler == null) {
               filler = new ItemStack(Material.GLASS_PANE);
            }

            ItemMeta fillerMeta = filler.getItemMeta();
            String fillerDisplayName = this.plugin.getGUIConfig().getString("history-gui.filler.display-name", " ");
            this.plugin.getGuiHelper().setDisplayName(fillerMeta, fillerDisplayName);
            this.plugin.getGuiHelper().applyItemProperties(fillerMeta, "history-gui.filler", this.plugin.getGUIConfig());
            filler.setItemMeta(fillerMeta);
            int statsSlot = this.plugin.getGUIConfig().getInt("history-gui.stats.slot", 4);
            int backSlot = this.plugin.getGUIConfig().getInt("history-gui.back.slot", 40);
            int prevSlot = this.plugin.getGUIConfig().getInt("history-gui.navigation.previous.slot", 38);
            int nextSlot = this.plugin.getGUIConfig().getInt("history-gui.navigation.next.slot", 42);
            boolean hasPrevPage = this.page > 1;
            boolean hasNextPage = startIndex + elementsPerPage < allLogs.size();
            List<Integer> fillerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "history-gui.filler.slots");
            Set<Integer> excludedSlots = new HashSet<>();
            excludedSlots.add(statsSlot);
            int[] fillerSlots = fillerSlotsList.isEmpty()
               ? new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44}
               : fillerSlotsList.stream()
                  .mapToInt(i -> i != null ? i : -1)
                  .filter(i -> i >= 0 && i < this.getInventory().getSize() && !excludedSlots.contains(i))
                  .toArray();

            for (int slot : fillerSlots) {
               if (slot >= 0 && slot < this.getInventory().getSize()) {
                  this.getInventory().setItem(slot, filler);
               }
            }

            List<Integer> historySlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(), "history-gui.history-slots");
            int[] historySlots = historySlotsList.isEmpty()
               ? new int[]{10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34}
               : historySlotsList.stream().mapToInt(i -> i != null ? i : -1).filter(i -> i >= 0 && i < this.getInventory().getSize()).toArray();

            for (int i = 0; i < elementsPerPage && startIndex + i < allLogs.size() && i < historySlots.length; i++) {
               CoinFlipLog log = allLogs.get(startIndex + i);
               int slotx = historySlots[i];
               if (slotx >= 0 && slotx < this.getInventory().getSize()) {
                  this.addButton(slotx, new InventoryButton().creator(p -> this.createHistoryItem(log, player.getUniqueId())).consumer(event -> {}));
               }
            }

            PlayerStats stats = this.plugin.getCoinFlipManager().getStats(player.getUniqueId());
            this.addButton(statsSlot, new InventoryButton().creator(p -> this.createStatsItem(stats)).consumer(event -> {}));
            String backMaterialName = this.plugin.getGUIConfig().getString("history-gui.back.material", "NETHER_STAR");
            Material backMaterial = this.parseMaterial(backMaterialName, Material.NETHER_STAR);
            String backTitle = this.plugin.getGUIConfig().getString("history-gui.back.title", "&r&c&lBack to Main Menu");
            List<String> backLore = this.plugin.getGUIConfig().getStringList("history-gui.back.lore");
            this.addButton(
               backSlot,
               new InventoryButton()
                  .creator(p -> this.createBackButton(backMaterial, backTitle, backLore))
                  .consumer(event -> this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, this.viewer, 1), this.viewer))
            );
            if (hasPrevPage) {
               Map<String, String> navPlaceholders = new HashMap<>();
               navPlaceholders.put("page", String.valueOf(this.page - 1));
               String prevText = this.plugin
                  .getAdventureHelper()
                  .parseToLegacy(this.plugin.getGUIConfig().getString("history-gui.navigation.previous.text", "&cPrevious &8(&e<page>&8)"), navPlaceholders);
               String prevMaterialName = this.plugin.getGUIConfig().getString("history-gui.navigation.previous.material", "RED_DYE");
               Material prevMaterial = this.parseMaterial(prevMaterialName, Material.RED_DYE);
               this.addButton(
                  prevSlot,
                  new InventoryButton().creator(p -> this.createNavigationItem(prevMaterial, prevText, "history-gui.navigation.previous")).consumer(event -> {
                     Player clicker = (Player)event.getWhoClicked();
                     this.plugin.getSoundHelper().playSound(clicker, "gui.page-change");

                     try {
                        GUIHelper.setCursorSafely(clicker, null);
                        clicker.closeInventory();
                     } catch (Exception var4x) {
                     }

                     FoliaScheduler.runTaskLater(this.plugin, this.viewer, () -> {
                        if (this.viewer != null && this.viewer.isOnline()) {
                           this.plugin.getGuiManager().openGUI(new CoinFlipHistoryGUI(this.plugin, this.viewer, this.page - 1), this.viewer);
                        }
                     }, 2L);
                  })
               );
            }

            if (hasNextPage) {
               Map<String, String> navPlaceholders = new HashMap<>();
               navPlaceholders.put("page", String.valueOf(this.page + 1));
               String nextText = this.plugin
                  .getAdventureHelper()
                  .parseToLegacy(this.plugin.getGUIConfig().getString("history-gui.navigation.next.text", "&aNext Page &8(&e<page>&8)"), navPlaceholders);
               String nextMaterialName = this.plugin.getGUIConfig().getString("history-gui.navigation.next.material", "LIME_DYE");
               Material nextMaterial = this.parseMaterial(nextMaterialName, Material.LIME_DYE);
               this.addButton(
                  nextSlot,
                  new InventoryButton().creator(p -> this.createNavigationItem(nextMaterial, nextText, "history-gui.navigation.next")).consumer(event -> {
                     Player clicker = (Player)event.getWhoClicked();
                     this.plugin.getSoundHelper().playSound(clicker, "gui.page-change");

                     try {
                        GUIHelper.setCursorSafely(clicker, null);
                        clicker.closeInventory();
                     } catch (Exception var4x) {
                     }

                     FoliaScheduler.runTaskLater(this.plugin, this.viewer, () -> {
                        if (this.viewer != null && this.viewer.isOnline()) {
                           this.plugin.getGuiManager().openGUI(new CoinFlipHistoryGUI(this.plugin, this.viewer, this.page + 1), this.viewer);
                        }
                     }, 2L);
                  })
               );
            }

            super.decorate(player);
         } catch (Exception var29) {
            this.plugin.getLogger().severe("[UltraCoinFlip] Failed to load coin flip history: " + var29.getMessage());
            this.plugin
               .getLogger()
               .severe("[UltraCoinFlip] This may be caused by database issues or corrupted data. Check your database connection and data integrity.");
            var29.printStackTrace();
         }
      }
   }

   private ItemStack createHistoryItem(CoinFlipLog log, UUID playerUUID) {
      if (log != null && playerUUID != null) {
         String materialName = this.plugin.getGUIConfig().getString("history-item.material", "PLAYER_HEAD");
         Material playerHeadFallback = MaterialHelper.getPlayerHeadMaterial();
         Material material = this.parseMaterial(materialName, playerHeadFallback);
         boolean isWinner = log.getWinnerUUID().equals(playerUUID);
         String result;
         if (isWinner) {
            result = this.plugin.getGUIConfig().getString("history-item.placeholders.result.win-format", "&a&lWIN");
         } else {
            result = this.plugin.getGUIConfig().getString("history-item.placeholders.result.lose-format", "&c&lLOSE");
         }

         boolean isHouseGame = log.getGameType() != null && log.getGameType() == CoinFlipLog.GameType.HOUSE;
         String opponentName;
         UUID opponentUUID;
         if (isHouseGame) {
            opponentName = this.plugin.getConfig().getString("house.name", this.plugin.getGUIConfig().getString("coinflip-gui.players.bot.name", "Bot"));
            opponentUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
         } else if (isWinner) {
            opponentName = log.getLoserName();
            opponentUUID = log.getLoserUUID();
         } else {
            opponentName = log.getWinnerName();
            opponentUUID = log.getWinnerUUID();
         }

         String dateFormatPattern = this.plugin.getGUIConfig().getString("history-item.placeholders.date-format", "yyyy-MM-dd HH:mm");

         String dateStr;
         try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormatPattern).withZone(ZoneId.systemDefault());
            dateStr = formatter.format(Instant.ofEpochMilli(log.getTimestamp()));
         } catch (DateTimeParseException | IllegalArgumentException var29) {
            this.plugin.getLogger().warning("Invalid date format pattern: " + dateFormatPattern + ", using default format");
            DateTimeFormatter defaultFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
            dateStr = defaultFormatter.format(Instant.ofEpochMilli(log.getTimestamp()));
         }

         String unit = this.plugin.getCurrencyManager().getUnit(log.getCurrencyType(), log.getCurrencyId());
         String displayNameTemplate = this.plugin
            .getGUIConfig()
            .getString("history-item.display-name", "&r&6<result> &f- &e<amount><symbol> &7vs &b<opponent>");
         String amountStr = this.plugin.getGuiHelper().formatAmount(log.getAmount());
         StringBuilder displayNameBuilder = new StringBuilder(
            displayNameTemplate.length() + result.length() + amountStr.length() + unit.length() + opponentName.length()
         );
         displayNameBuilder.append(displayNameTemplate);

         int index;
         while ((index = displayNameBuilder.indexOf("<result>")) != -1) {
            displayNameBuilder.replace(index, index + 8, result);
         }

         while ((index = displayNameBuilder.indexOf("<amount>")) != -1) {
            displayNameBuilder.replace(index, index + 8, amountStr);
         }

         while ((index = displayNameBuilder.indexOf("<symbol>")) != -1) {
            displayNameBuilder.replace(index, index + 8, unit);
         }

         while ((index = displayNameBuilder.indexOf("<opponent>")) != -1) {
            displayNameBuilder.replace(index, index + 10, opponentName);
         }

         String displayName = displayNameBuilder.toString();
         List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("history-item.lore.lines");
         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━");
            loreTemplate.add("&r&fOpponent: &b<opponent>");
            loreTemplate.add("&r&fAmount: &e<amount><symbol>");
            loreTemplate.add("&r&fResult: <result>");
            loreTemplate.add("&r&fDate: &7<date>");
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━");
         }

         Map<String, String> placeholders = new HashMap<>();
         placeholders.put("opponent", opponentName);
         placeholders.put("amount", this.plugin.getGuiHelper().formatAmount(log.getAmount()));
         placeholders.put("symbol", unit);
         placeholders.put("result", result);
         placeholders.put("date", dateStr);
         placeholders.put(
            "game_type",
            isHouseGame
               ? (
                  this.plugin.getMessage("house.game-type") != null && !this.plugin.getMessage("house.game-type").equals("house.game-type")
                     ? this.plugin.getMessage("house.game-type")
                     : "Bot"
               )
               : (
                  this.plugin.getMessage("house.game-type-player") != null
                        && !this.plugin.getMessage("house.game-type-player").equals("house.game-type-player")
                     ? this.plugin.getMessage("house.game-type-player")
                     : "Player"
               )
         );
         List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
         if (MaterialHelper.isPlayerHead(material)) {
            String headType = this.plugin.getGUIConfig().getString("history-item.player-head.type", "Opponent");
            headType = headType != null ? headType : "Opponent";
            Player headPlayer = null;
            String base64Texture = "";
            boolean usePlayerSkin = false;
            if (isHouseGame) {
               base64Texture = this.plugin
                  .getConfig()
                  .getString(
                     "house.display.texture",
                     this.plugin
                        .getGUIConfig()
                        .getString(
                           "coinflip-gui.players.bot.texture",
                           "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ4ZDU1YjMzZWQ2ZmViNjE0ZTJjYTVkNGY1MGJiMzdmMTYxYWRhMzU4MmZjZmM2ZTQwMjg4YzZmYjA2ZjFmIn19fQ=="
                        )
                  );
               usePlayerSkin = false;
            } else if ("Player".equalsIgnoreCase(headType)) {
               headPlayer = this.viewer;
               usePlayerSkin = true;
            } else if ("Opponent".equalsIgnoreCase(headType)) {
               try {
                  OfflinePlayer opponent = this.plugin.getServer().getOfflinePlayer(opponentUUID);
                  if (opponent instanceof Player && ((Player)opponent).isOnline()) {
                     headPlayer = (Player)opponent;
                     usePlayerSkin = true;
                  } else {
                     ItemStack item = new ItemStack(material);
                     SkullMeta skullMeta = (SkullMeta)item.getItemMeta();
                     if (skullMeta != null) {
                        LegacyCompatibility.setSkullOwner(skullMeta, opponent);
                        this.plugin.getGuiHelper().setDisplayName(skullMeta, displayName);
                        this.plugin.getGuiHelper().setLore(skullMeta, lore);
                        item.setItemMeta(skullMeta);
                        return item;
                     }
                  }
               } catch (Exception var30) {
                  if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Could not get opponent player for head: " + var30.getMessage());
                  }
               }
            } else if ("Base64".equalsIgnoreCase(headType)) {
               base64Texture = this.plugin.getGUIConfig().getString("history-item.player-head.texture", "");
               usePlayerSkin = false;
            }

            Boolean glowing = this.plugin.getGUIConfig().contains("history-item.glowing")
               ? this.plugin.getGUIConfig().getBoolean("history-item.glowing", false)
               : null;
            Integer customModelData = this.plugin.getGUIConfig().contains("history-item.custom-model-data")
               ? this.plugin.getGUIConfig().getInt("history-item.custom-model-data", 0)
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
               this.plugin.getGuiHelper().applyItemProperties(meta, "history-item", this.plugin.getGUIConfig());
               item.setItemMeta(meta);
            }

            return item;
         }
      } else {
         this.plugin
            .getLogger()
            .warning("[UltraCoinFlip] Cannot create history item: log or playerUUID is null. This may indicate corrupted history data. Using fallback item.");
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
         String materialName = this.plugin.getGUIConfig().getString("history-gui.stats.material", "PLAYER_HEAD");
         Material playerHeadFallback = MaterialHelper.getPlayerHeadMaterial();
         Material material = this.parseMaterial(materialName, playerHeadFallback);
         String title = this.plugin.getGUIConfig().getString("history-gui.stats.title", "&r&6&lYour Statistics");
         Map<String, String> placeholders = new HashMap<>();
         placeholders.put("wins", String.valueOf(stats.getWins()));
         placeholders.put("defeats", String.valueOf(stats.getDefeats()));
         placeholders.put("win_percentage", String.format("%.2f", stats.getWinPercentage()));
         placeholders.put("winrate_money", String.format("%.2f", stats.getWinPercentageMoney()));
         placeholders.put("winrate_playerpoints", String.format("%.2f", stats.getWinPercentagePlayerPoints()));
         placeholders.put("winrate_tokenmanager", String.format("%.2f", stats.getWinPercentageTokenManager()));
         placeholders.put("winrate_beasttokens", String.format("%.2f", stats.getWinPercentageBeastTokens()));
         placeholders.put("profit_money", this.plugin.getGuiHelper().formatAmount(stats.getProfitMoney()));
         placeholders.put("profit_playerpoints", this.plugin.getGuiHelper().formatAmount(stats.getProfitPlayerPoints()));
         placeholders.put("profit_tokenmanager", this.plugin.getGuiHelper().formatAmount(stats.getProfitTokenManager()));
         placeholders.put("profit_beasttokens", this.plugin.getGuiHelper().formatAmount(stats.getProfitBeastTokens()));
         placeholders.put("loss_money", this.plugin.getGuiHelper().formatAmount(stats.getLossMoney()));
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
         List<String> loreTemplate = this.plugin.getGUIConfig().getStringList("stats-item.lore.lines");
         if (loreTemplate == null || loreTemplate.isEmpty()) {
            loreTemplate = new ArrayList<>();
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            loreTemplate.add("&r&fWins: &a<wins> &8│ &r&fLosses: &c<defeats>");
            loreTemplate.add("&r&fWin Rate: &e<win_percentage>%");
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            loreTemplate.add("&r&fProfit: &a<profit_money><money_unit>");
            loreTemplate.add("&r&fLoss: &c<loss_money><money_unit>");
            loreTemplate.add("&r&fNet: <net_money><money_unit>");
            loreTemplate.add("&r&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
         }

         List<?> lore = this.plugin.getGuiHelper().createLore(loreTemplate, placeholders);
         if (MaterialHelper.isPlayerHead(material)) {
            String headType = this.plugin.getGUIConfig().getString("history-gui.stats.player-head.type", "Player");
            headType = headType != null ? headType : "Player";
            Player headPlayer = null;
            String base64Texture = "";
            boolean usePlayerSkin = false;
            if ("Player".equalsIgnoreCase(headType)) {
               if (this.viewer != null && this.viewer.isOnline()) {
                  headPlayer = this.viewer;
                  usePlayerSkin = true;
               } else {
                  this.plugin.getLogger().warning("Cannot use player head: viewer is null or offline");
               }
            } else if ("Base64".equalsIgnoreCase(headType)) {
               base64Texture = this.plugin.getGUIConfig().getString("history-gui.stats.player-head.texture", "");
               usePlayerSkin = false;
            }

            Boolean glowing = this.plugin.getGUIConfig().contains("history-gui.stats.glowing")
               ? this.plugin.getGUIConfig().getBoolean("history-gui.stats.glowing", false)
               : null;
            Integer customModelData = this.plugin.getGUIConfig().contains("history-gui.stats.custom-model-data")
               ? this.plugin.getGUIConfig().getInt("history-gui.stats.custom-model-data", 0)
               : null;
            if (customModelData != null && customModelData <= 0) {
               customModelData = null;
            }

            ItemStack headItem = this.plugin
               .getGuiHelper()
               .createPlayerHead(material, headPlayer, base64Texture, usePlayerSkin, title, lore, glowing, customModelData);
            if (headItem == null) {
               this.plugin.getLogger().warning("Failed to create player head, using fallback");
               headItem = new ItemStack(material);
               ItemMeta meta = headItem.getItemMeta();
               if (meta != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta, title);
                  this.plugin.getGuiHelper().setLore(meta, lore);
                  this.plugin.getGuiHelper().applyItemProperties(meta, "history-gui.stats", this.plugin.getGUIConfig());
                  headItem.setItemMeta(meta);
               }
            }

            return headItem;
         } else {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, title);
               this.plugin.getGuiHelper().setLore(meta, lore);
               this.plugin.getGuiHelper().applyItemProperties(meta, "history-gui.stats", this.plugin.getGUIConfig());
               item.setItemMeta(meta);
            }

            return item;
         }
      }
   }

   private ItemStack createBackButton(Material material, String title, List<String> lore) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      this.plugin.getGuiHelper().setDisplayName(meta, title);
      if (lore != null && !lore.isEmpty()) {
         List<?> loreList = this.plugin.getGuiHelper().createLore(lore, new HashMap<>());
         this.plugin.getGuiHelper().setLore(meta, loreList);
      }

      this.plugin.getGuiHelper().applyItemProperties(meta, "history-gui.back", this.plugin.getGUIConfig());
      item.setItemMeta(meta);
      return item;
   }

   private ItemStack createNavigationItem(Material material, String name, String configPath) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      this.plugin.getGuiHelper().setDisplayName(meta, name);
      if (configPath != null) {
         this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
      }

      item.setItemMeta(meta);
      return item;
   }

   @Generated
   public CoinFlipHistoryGUI(KStudio plugin, Player viewer, int page) {
      this.plugin = plugin;
      this.viewer = viewer;
      this.page = page;
   }
}
