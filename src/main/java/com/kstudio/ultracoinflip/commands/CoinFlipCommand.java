package com.kstudio.ultracoinflip.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.currency.CurrencyHandler;
import com.kstudio.ultracoinflip.currency.CurrencyManager;
import com.kstudio.ultracoinflip.currency.CurrencySettings;
import com.kstudio.ultracoinflip.data.BettingLimitManager;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.gui.impl.CoinFlipHistoryGUI;
import com.kstudio.ultracoinflip.gui.impl.CoinFlipListGUI;
import com.kstudio.ultracoinflip.gui.impl.CreateCoinFlipGUI;
import com.kstudio.ultracoinflip.gui.impl.HeadsTailsSelectionGUI;
import com.kstudio.ultracoinflip.gui.impl.LeaderboardGUI;
import com.kstudio.ultracoinflip.gui.impl.SettingsGUI;
import com.kstudio.ultracoinflip.refund.RefundLimiter;
import com.kstudio.ultracoinflip.util.AmountParser;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("coinflip|cf")
@CommandPermission("ultracoinflip.use")
@Description("Open coinflip menu or manage games")
public class CoinFlipCommand extends BaseCommand {
   private final KStudio plugin;

   public CoinFlipCommand(KStudio plugin) {
      this.plugin = plugin;
   }

   public static String getCommandAliases(KStudio plugin) {
      List<String> aliases = plugin.getConfig().getStringList("command_aliases");
      return aliases != null && !aliases.isEmpty()
            ? aliases.stream()
                  .filter(alias -> alias != null && !alias.trim().isEmpty())
                  .map(String::trim)
                  .filter(alias -> !alias.isEmpty())
                  .collect(Collectors.joining("|"))
            : "coinflip|cf";
   }

   @Default
   @Description("Open the coinflip menu")
   public void onDefault(Player player) {
      this.plugin.getGuiManager().openGUI(new CoinFlipListGUI(this.plugin, player, 1), player);
   }

   @Subcommand("reload")
   @CommandPermission("ultracoinflip.reload")
   @Description("Reload plugin configuration")
   public void onReload(CommandSender sender) {
      if (!sender.hasPermission("ultracoinflip.reload") && !sender.hasPermission("ultracoinflip.admin")) {
         String noPermMessage = this.plugin.getMessage("command.no-permission");
         if (noPermMessage.equals("command.no-permission")) {
            noPermMessage = "&cYou don't have permission to use this command!";
         }

         String message = this.plugin.getMessage("prefix") + " " + noPermMessage;
         if (sender instanceof Player) {
            this.plugin.getAdventureHelper().sendMessage((Player) sender, message);
         } else {
            sender.sendMessage(this.plugin.getAdventureHelper().parseToLegacy(message));
         }
      } else if (this.plugin.getCoinFlipManager().isRefundInProgress()) {
         String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.reload-busy");
         if (sender instanceof Player) {
            this.plugin.getAdventureHelper().sendMessage((Player) sender, message);
         } else {
            sender.sendMessage(this.plugin.getAdventureHelper().parseToLegacy(message));
         }
      } else {
         RefundLimiter limiter = this.plugin.getRefundLimiter();

         try {
            if (limiter != null) {
               limiter.setReloading(true);
            }

            this.plugin.getLogger().info("Cleaning up games in animation phase before reload...");
            int cleanedGames = this.plugin.getCoinFlipManager().cleanupRollingGamesOnReload();
            if (cleanedGames > 0) {
               this.plugin.getLogger().info("Closed " + cleanedGames + " game(s) in animation phase before reload");
            } else {
               this.plugin.getLogger().info("No games in animation phase to cleanup");
            }

            this.plugin.getConfigManager().reload();
            this.plugin.getCoinFlipManager().reloadConfigCache();
            this.plugin.cacheUpdateCheckerConfig();
            this.plugin.getCurrencyManager().reload();
            if (this.plugin.getTransactionLogger() != null) {
               this.plugin.getTransactionLogger().reloadConfig();
            }

            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.reload-success");
            if (sender instanceof Player) {
               Player player = (Player) sender;
               this.plugin.getSoundHelper().playSound(player, "command.reload-success");
               this.plugin.getAdventureHelper().sendMessage(player, message);
            } else {
               sender.sendMessage(this.plugin.getAdventureHelper().parseToLegacy(message));
            }

            String reloaderName = sender instanceof Player ? sender.getName() : "Console";
            this.plugin.getLogger().info("Plugin configuration reloaded by " + reloaderName);
         } catch (Exception var9) {
            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.reload-error");
            if (sender instanceof Player) {
               Player player = (Player) sender;
               this.plugin.getSoundHelper().playSound(player, "command.reload-error");
               this.plugin.getAdventureHelper().sendMessage(player, message);
            } else {
               sender.sendMessage(this.plugin.getAdventureHelper().parseToLegacy(message));
            }

            this.plugin.getLogger()
                  .severe("[UltraCoinFlip] Failed to reload plugin configuration: " + var9.getMessage());
            this.plugin
                  .getLogger()
                  .severe(
                        "[UltraCoinFlip] Please check your configuration files for syntax errors. The plugin will continue using previous settings.");
            var9.printStackTrace();
         } finally {
            if (limiter != null) {
               limiter.setReloading(false);
            }
         }
      }
   }

   @Subcommand("audit|status")
   @CommandPermission("ultracoinflip.admin")
   @Description("View plugin status and active transactions")
   public void onAudit(CommandSender sender) {
      if (!sender.hasPermission("ultracoinflip.admin") && !sender.isOp()) {
         String noPermMessage = this.plugin.getMessage("command.no-permission");
         String message = this.plugin.getMessage("prefix") + " " + noPermMessage;
         if (sender instanceof Player) {
            this.plugin.getAdventureHelper().sendMessage((Player) sender, message);
         } else {
            sender.sendMessage(this.plugin.getAdventureHelper().parseToLegacy(message));
         }
      } else {
         RefundLimiter limiter = this.plugin.getRefundLimiter();
         String yes = this.plugin.getMessage("admin.audit-yes");
         String no = this.plugin.getMessage("admin.audit-no");
         List<String> messages = new ArrayList<>();
         messages.add(this.plugin.getMessage("admin.audit-header"));
         messages.add(
               this.plugin.getMessage("admin.audit-active-games").replace("<count>",
                     String.valueOf(this.plugin.getCoinFlipManager().getActiveGameCount())));
         messages.add(
               this.plugin.getMessage("admin.audit-rolling-games").replace("<count>",
                     String.valueOf(this.plugin.getCoinFlipManager().getRollingGameCount())));
         messages.add(
               this.plugin
                     .getMessage("admin.audit-players-in-transaction")
                     .replace("<count>",
                           String.valueOf(this.plugin.getCoinFlipManager().getPlayersInTransactionCount())));
         messages.add(
               this.plugin.getMessage("admin.audit-refund-in-progress").replace("<status>",
                     this.plugin.getCoinFlipManager().isRefundInProgress() ? yes : no));
         if (limiter != null) {
            messages.add(this.plugin.getMessage("admin.audit-global-lock").replace("<status>",
                  limiter.isGlobalLocked() ? yes : no));
            messages.add(this.plugin.getMessage("admin.audit-reloading").replace("<status>",
                  limiter.isReloading() ? yes : no));
            messages.add(this.plugin.getMessage("admin.audit-active-refund-count").replace("<count>",
                  String.valueOf(limiter.getActiveRefundCount())));
            messages.add(this.plugin.getMessage("admin.audit-games-processing").replace("<count>",
                  String.valueOf(limiter.getGamesProcessingCount())));
         }

         if (this.plugin.getTransactionLogger() != null) {
            messages.add(
                  this.plugin.getMessage("admin.audit-pending-logs").replace("<count>",
                        String.valueOf(this.plugin.getTransactionLogger().getPendingCount())));
         }

         messages.add(this.plugin.getMessage("admin.audit-footer"));

         for (String line : messages) {
            if (sender instanceof Player) {
               this.plugin.getAdventureHelper().sendMessage((Player) sender, line);
            } else {
               sender.sendMessage(this.plugin.getAdventureHelper().parseToLegacy(line));
            }
         }
      }
   }

   @Subcommand("delete|remove|cancel")
   @Description("Cancel your active coinflip game")
   public void onDelete(Player player, @Optional String[] args) {
      RefundLimiter limiter = this.plugin.getRefundLimiter();
      boolean onCooldown = limiter != null
            ? limiter.isOnCooldown(player.getUniqueId())
            : this.plugin.getCoinFlipManager().isOnRefundCooldown(player.getUniqueId());
      if (onCooldown) {
         int remaining = limiter != null
               ? limiter.getRemainingCooldownSeconds(player.getUniqueId())
               : this.plugin.getCoinFlipManager().getRemainingCooldown(player.getUniqueId());
         String message = this.plugin.getMessage("prefix")
               + " "
               + this.plugin.getMessage("command.cooldown-wait").replace("<seconds>", String.valueOf(remaining));
         this.plugin.getAdventureHelper().sendMessage(player, message);
         this.plugin.getSoundHelper().playSound(player, "error.general");
      } else if (limiter == null || !limiter.isGlobalLocked() && !limiter.isReloading()) {
         if (!this.plugin.getCoinFlipManager().hasActiveGame(player.getUniqueId())) {
            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.no-bet");
            this.plugin.getAdventureHelper().sendMessage(player, message);
         } else {
            int cancelled = this.plugin.getCoinFlipManager().refundAllGames(player);
            this.sendRefundResult(player, cancelled);
         }
      } else {
         String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.plugin-busy");
         this.plugin.getAdventureHelper().sendMessage(player, message);
         this.plugin.getSoundHelper().playSound(player, "error.general");
      }
   }

   private void sendRefundResult(Player player, int cancelled) {
      RefundLimiter limiter = this.plugin.getRefundLimiter();
      if (cancelled == -1) {
         int remaining = limiter != null
               ? limiter.getRemainingCooldownSeconds(player.getUniqueId())
               : this.plugin.getCoinFlipManager().getRemainingCooldown(player.getUniqueId());
         String message = this.plugin.getMessage("prefix")
               + " "
               + this.plugin.getMessage("command.cooldown-wait").replace("<seconds>", String.valueOf(remaining));
         this.plugin.getAdventureHelper().sendMessage(player, message);
         this.plugin.getSoundHelper().playSound(player, "error.general");
      } else if (cancelled == -2) {
         String message = this.plugin.getMessage("prefix") + " "
               + this.plugin.getMessage("command.transaction-in-progress");
         this.plugin.getAdventureHelper().sendMessage(player, message);
         this.plugin.getSoundHelper().playSound(player, "error.general");
      } else if (cancelled == -3) {
         String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.plugin-busy");
         this.plugin.getAdventureHelper().sendMessage(player, message);
         this.plugin.getSoundHelper().playSound(player, "error.general");
      } else if (cancelled == 0) {
         String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.no-bet");
         this.plugin.getAdventureHelper().sendMessage(player, message);
      } else {
         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "notification-sound")) {
            this.plugin.getSoundHelper().playSound(player, "game.cancel");
         }

         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player, "message-game-cancelled")) {
            String messageKey = cancelled > 1 ? "command.bet-cancelled-multiple" : "command.bet-cancelled";
            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage(messageKey);
            if (cancelled > 1) {
               Map<String, String> placeholders = new HashMap<>();
               placeholders.put("amount", String.valueOf(cancelled));
               this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
            } else {
               this.plugin.getAdventureHelper().sendMessage(player, message);
            }
         }
      }
   }

   @Subcommand("help")
   @Description("Show help information")
   public void onHelp(Player player) {
      List<String> helpMessages = this.plugin.getConfigManager().getMessages().getStringList("help.messages");
      if (helpMessages == null || helpMessages.isEmpty()) {
         helpMessages = new ArrayList<>();
         helpMessages.add("&6&l=== UltraCoinFlip Help ===");
         helpMessages.add("&e/coinflip &7- Open the coinflip menu");
         helpMessages.add("&e/coinflip create <currency> <amount> &7- Create a new coinflip game");
         helpMessages.add("&e/coinflip delete &7- Cancel your active coinflip game");
         helpMessages.add("&e/coinflip history &7- Open coinflip history GUI");
         helpMessages.add("&e/coinflip help &7- Show this help message");
      }

      for (String line : helpMessages) {
         if (line != null && !line.trim().isEmpty()) {
            this.plugin.getAdventureHelper().sendMessage(player, line);
         }
      }
   }

   @Subcommand("history")
   @Description("Open coinflip history GUI")
   public void onHistory(Player player) {
      this.plugin.getGuiManager().openGUI(new CoinFlipHistoryGUI(this.plugin, player, 1), player);
   }

   @Subcommand("leaderboard|lb|top")
   @Description("Open leaderboard GUI")
   public void onLeaderboard(Player player) {
      this.plugin.getGuiManager().openGUI(new LeaderboardGUI(this.plugin, player), player);
   }

   @Subcommand("settings|toggle")
   @Description("Open settings GUI")
   public void onSettings(Player player) {
      this.plugin.getGuiManager().openGUI(new SettingsGUI(this.plugin, player), player);
   }

   @Subcommand("create")
   @Description("Open create GUI or create a new coinflip game")
   public void onCreate(Player player) {
      if (this.plugin.getCurrencyManager() == null) {
         this.plugin.getSoundHelper().playSound(player, "error.general");
         String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.economy-not-ready");
         this.plugin.getAdventureHelper().sendMessage(player, message);
      } else {
         this.plugin.getGuiManager().openGUI(new CreateCoinFlipGUI(this.plugin, player), player);
      }
   }

   @Subcommand("create")
   @Syntax("<currency> <amount>")
   @Description("Create a new coinflip game with specific currency")
   @CommandCompletion("@currencies @smart-amounts")
   public void onCreate(Player player, @Single String currencyStr, @Single String amountStr) {
      this.onCreateWithAmount(player, amountStr, currencyStr);
   }

   private void onCreateWithAmount(Player player, String amountStr, String currencyStr) {
      if (this.plugin.getCurrencyManager() == null) {
         this.plugin.getSoundHelper().playSound(player, "error.general");
         String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.economy-not-ready");
         this.plugin.getAdventureHelper().sendMessage(player, message);
      } else {
         double amount;
         try {
            amount = AmountParser.parseFormattedAmount(amountStr);
         } catch (IllegalArgumentException var27) {
            String message = this.plugin.getMessage("prefix") + " &c" + var27.getMessage();
            this.plugin.getAdventureHelper().sendMessage(player, message);
            return;
         }

         if (amount <= 0.0) {
            this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
            String message = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("command.invalid-amount");
            this.plugin.getAdventureHelper().sendMessage(player, message);
         } else {
            String currencyId = null;
            CoinFlipGame.CurrencyType currencyType;
            if (currencyStr != null && !currencyStr.trim().isEmpty()) {
               currencyStr = currencyStr.toLowerCase();
               CurrencyManager.CurrencyInfo currencyInfo = this.plugin.getCurrencyManager()
                     .parseCurrencyFromSyntaxCommand(currencyStr);
               if (currencyInfo != null) {
                  currencyType = currencyInfo.getType();
                  currencyId = currencyInfo.getCurrencyId();
               } else if (currencyStr.startsWith("coinsengine:")) {
                  String id = currencyStr.substring("coinsengine:".length());
                  if (!this.plugin.getCurrencyManager().isCoinsEngineCurrencyEnabled(id)) {
                     String message = this.buildInvalidCurrencyMessage(this.plugin, "coinsengine", id);
                     this.plugin.getAdventureHelper().sendMessage(player, message);
                     return;
                  }

                  currencyType = CoinFlipGame.CurrencyType.COINSENGINE;
                  currencyId = id;
               } else if (currencyStr.startsWith("placeholder:")) {
                  String id = currencyStr.substring("placeholder:".length());
                  if (!this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(id)) {
                     String message = this.buildInvalidCurrencyMessage(this.plugin, "placeholder", id);
                     this.plugin.getAdventureHelper().sendMessage(player, message);
                     return;
                  }

                  currencyType = CoinFlipGame.CurrencyType.PLACEHOLDER;
                  currencyId = id;
               } else if (this.plugin.getCurrencyManager().isCoinsEngineCurrencyEnabled(currencyStr)) {
                  currencyType = CoinFlipGame.CurrencyType.COINSENGINE;
                  currencyId = currencyStr;
               } else {
                  if (!this.plugin.getCurrencyManager().isPlaceholderCurrencyEnabled(currencyStr)) {
                     this.plugin.getSoundHelper().playSound(player, "error.general");
                     String message = this.buildInvalidCurrencyMessage(this.plugin, null, currencyStr);
                     this.plugin.getAdventureHelper().sendMessage(player, message);
                     return;
                  }

                  currencyType = CoinFlipGame.CurrencyType.PLACEHOLDER;
                  currencyId = currencyStr;
               }
            } else if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.MONEY)) {
               currencyType = CoinFlipGame.CurrencyType.MONEY;
               currencyId = null;
            } else if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.PLAYERPOINTS)) {
               currencyType = CoinFlipGame.CurrencyType.PLAYERPOINTS;
               currencyId = null;
            } else if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.TOKENMANAGER)) {
               currencyType = CoinFlipGame.CurrencyType.TOKENMANAGER;
               currencyId = null;
            } else if (this.plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.BEASTTOKENS)) {
               currencyType = CoinFlipGame.CurrencyType.BEASTTOKENS;
               currencyId = null;
            } else {
               Set<String> enabledCoinsEngineCurrencies = this.plugin.getCurrencyManager()
                     .getEnabledCoinsEngineCurrencyIds();
               if (!enabledCoinsEngineCurrencies.isEmpty()) {
                  currencyType = CoinFlipGame.CurrencyType.COINSENGINE;
                  currencyId = enabledCoinsEngineCurrencies.iterator().next();
               } else {
                  Set<String> enabledPlaceholderCurrencies = this.plugin.getCurrencyManager()
                        .getEnabledPlaceholderCurrencyIds();
                  if (enabledPlaceholderCurrencies.isEmpty()) {
                     String message = this.plugin.getMessage("prefix") + " "
                           + this.plugin.getMessage("command.no-currency-available");
                     this.plugin.getAdventureHelper().sendMessage(player, message);
                     return;
                  }

                  currencyType = CoinFlipGame.CurrencyType.PLACEHOLDER;
                  currencyId = enabledPlaceholderCurrencies.iterator().next();
               }
            }

            if (!this.plugin.getCurrencyManager().isCurrencyEnabled(currencyType, currencyId)) {
               this.plugin.getSoundHelper().playSound(player, "error.general");
               String typeDisplay = currencyId != null ? currencyType.name() + " (" + currencyId + ")"
                     : currencyType.name();
               String message = this.plugin.getMessage("prefix") + " "
                     + this.plugin.getMessage("command.currency-disabled").replace("<type>", typeDisplay);
               this.plugin.getAdventureHelper().sendMessage(player, message);
            } else {
               if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE && currencyId != null) {
                  CurrencyHandler handler = this.plugin.getCurrencyManager().getHandler(currencyType, currencyId);
                  if (handler == null) {
                     String message = this.plugin.getMessage("prefix")
                           + " "
                           + this.plugin.getMessage("command.currency-not-available").replace("<id>", currencyId);
                     this.plugin.getAdventureHelper().sendMessage(player, message);
                     this.plugin.getAdventureHelper().sendMessage(player,
                           this.plugin.getMessage("command.currency-check-hint-1"));
                     this.plugin.getAdventureHelper().sendMessage(player,
                           this.plugin.getMessage("command.currency-check-hint-2").replace("<id>", currencyId));
                     this.plugin.getAdventureHelper().sendMessage(player,
                           this.plugin.getMessage("command.currency-check-hint-3"));
                     return;
                  }
               } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER && currencyId != null) {
                  CurrencyHandler handler = this.plugin.getCurrencyManager().getHandler(currencyType, currencyId);
                  if (handler == null) {
                     String message = this.plugin.getMessage("prefix")
                           + " "
                           + this.plugin.getMessage("command.currency-not-available").replace("<id>", currencyId);
                     this.plugin.getAdventureHelper().sendMessage(player, message);
                     this.plugin.getAdventureHelper().sendMessage(player,
                           this.plugin.getMessage("command.currency-check-hint-1"));
                     this.plugin.getAdventureHelper().sendMessage(player,
                           this.plugin.getMessage("command.currency-check-hint-2").replace("<id>", currencyId));
                     this.plugin.getAdventureHelper().sendMessage(player,
                           this.plugin.getMessage("command.currency-check-hint-3"));
                     return;
                  }
               }

               if (!this.plugin.getCurrencyManager().canPlayerUseCurrency(player, currencyType, currencyId)) {
                  this.plugin.getSoundHelper().playSound(player, "error.general");
                  String restrictionReason = this.plugin.getCurrencyManager().getRestrictionReason(player, currencyType,
                        currencyId);
                  String message = this.plugin.getMessage("prefix") + " "
                        + this.plugin.getMessage("restriction.cannot-use-currency");
                  if (restrictionReason != null && !restrictionReason.isEmpty()) {
                     message = message + " " + restrictionReason;
                  }

                  this.plugin.getAdventureHelper().sendMessage(player, message);
               } else {
                  CurrencySettings currencySettings = this.plugin.getCurrencyManager().getCurrencySettings(currencyType,
                        currencyId);
                  double minBid = currencySettings.getMinBid();
                  if (amount < minBid) {
                     this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                     Map<String, String> placeholders = new HashMap<>();
                     String formattedMinBid = this.plugin.getGuiHelper().formatAmount(minBid, currencyId);
                     placeholders.put("amount", formattedMinBid);
                     String message = this.plugin.getMessage("prefix") + " "
                           + this.plugin.getMessage("command.min-bid");
                     this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                  } else {
                     double maxBid = currencySettings.getMaxBid();
                     if (maxBid != -1.0 && amount > maxBid) {
                        this.plugin.getSoundHelper().playSound(player, "error.invalid-amount");
                        Map<String, String> placeholders = new HashMap<>();
                        String formattedMaxBid = this.plugin.getGuiHelper().formatAmount(maxBid, currencyId);
                        placeholders.put("amount", formattedMaxBid);
                        String message = this.plugin.getMessage("prefix") + " "
                              + this.plugin.getMessage("command.max-bid");
                        this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                     } else if (!this.plugin.getCurrencyManager().hasBalanceWithReserve(player, currencyType,
                           currencyId, amount)) {
                        this.plugin.getSoundHelper().playSound(player, "error.not-enough-money");
                        double currentBalance = this.plugin.getCurrencyManager().getBalance(player, currencyType,
                              currencyId);
                        String formattedBalance = this.plugin.getGuiHelper().formatAmount(currentBalance, currencyId);
                        String formattedAmount = this.plugin.getGuiHelper().formatAmount(amount, currencyId);
                        boolean isReserveIssue = this.plugin.getCurrencyManager().isReserveBalanceIssue(player,
                              currencyType, currencyId, amount);
                        Map<String, String> placeholders = new HashMap<>();
                        String messageKey;
                        if (isReserveIssue) {
                           double minReserve = this.plugin.getCurrencyManager().getMinReserveBalance(currencyType,
                                 currencyId);
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
                        this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                     } else if (this.plugin.getCoinFlipManager().isInRollingGame(player.getUniqueId())) {
                        this.plugin.getSoundHelper().playSound(player, "error.general");
                        String message = this.plugin.getMessage("prefix") + " "
                              + this.plugin.getMessage("command.rolling-active");
                        this.plugin.getAdventureHelper().sendMessage(player, message);
                     } else if (!this.plugin.getCoinFlipManager().canCreateMoreGames(player)) {
                        this.plugin.getSoundHelper().playSound(player, "error.general");
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
                     } else {
                        BettingLimitManager.LimitCheckResult limitResult = this.plugin
                              .getBettingLimitManager()
                              .canPlayerBet(player, currencyType, currencyId, amount);
                        if (limitResult != null) {
                           this.plugin.getSoundHelper().playSound(player, "error.general");
                           Map<String, String> placeholders = new HashMap<>();
                           placeholders.put("limit",
                                 this.plugin.getGuiHelper().formatAmount(limitResult.getLimit(), currencyId));
                           placeholders.put("current",
                                 this.plugin.getGuiHelper().formatAmount(limitResult.getCurrentTotal(), currencyId));
                           placeholders.put("remaining",
                                 this.plugin.getGuiHelper().formatAmount(limitResult.getRemaining(), currencyId));
                           String message = this.plugin.getMessage("prefix") + " "
                                 + this.plugin.getMessage("command." + limitResult.getMessageKey());
                           this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                        } else {
                           boolean headsTailsEnabled = this.plugin.getConfig().getBoolean("heads-tails.enabled", false);
                           final CoinFlipGame.CurrencyType finalCurrencyType = currencyType;
                           final String finalCurrencyId = currencyId;
                           if (headsTailsEnabled) {
                              FoliaScheduler.runTask(
                                    this.plugin,
                                    player,
                                    () -> {
                                       if (player != null && player.isOnline()) {
                                          this.plugin
                                                .getGuiManager()
                                                .openGUI(new HeadsTailsSelectionGUI(this.plugin, player,
                                                      finalCurrencyType, finalCurrencyId, amount, null), player);
                                       }
                                    });
                           } else {
                              if (!this.plugin.getCurrencyManager().withdraw(player, currencyType, currencyId,
                                    amount)) {
                                 String message = this.plugin.getMessage("prefix") + " "
                                       + this.plugin.getMessage("command.currency-withdraw-failed");
                                 this.plugin.getAdventureHelper().sendMessage(player, message);
                                 return;
                              }

                              Map<String, String> placeholders = new HashMap<>();
                              String formattedAmount = this.plugin.getGuiHelper().formatAmount(amount, currencyId);
                              placeholders.put("amount", formattedAmount);
                              String unit = this.plugin.getCurrencyManager().getUnit(currencyType, currencyId);
                              placeholders.put("symbol", unit);
                              String messageKey;
                              if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
                                 messageKey = "command.bet-created-money";
                              } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                                 messageKey = "command.bet-created-playerpoints";
                              } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                                 messageKey = "command.bet-created-tokenmanager";
                              } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                                 messageKey = "command.bet-created-beasttokens";
                              } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE) {
                                 messageKey = "command.bet-created-coinsengine";
                              } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER) {
                                 messageKey = "command.bet-created-placeholder";
                              } else {
                                 messageKey = "command.bet-created-money";
                              }

                              if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player,
                                    "message-game-created")) {
                                 String message = this.plugin.getMessage("prefix") + " "
                                       + this.plugin.getMessage(messageKey);
                                 this.plugin.getAdventureHelper().sendMessage(player, message, placeholders);
                              }

                              if (this.plugin.getPlayerSettingsManager().isSettingEnabled(player,
                                    "notification-sound")) {
                                 this.plugin.getSoundHelper().playSound(player, "game.create");
                              }

                              this.plugin.getCoinFlipManager().createGame(player, currencyType, currencyId, amount);
                              this.plugin.getBettingLimitManager().recordBet(player, currencyType, currencyId, amount);
                              boolean broadcastEnabled = currencySettings.isBroadcastEnabled();
                              double minBroadcastAmount = currencySettings.getMinBroadcastAmount();
                              if (broadcastEnabled && amount >= minBroadcastAmount) {
                                 Map<String, String> broadcastPlaceholders = new HashMap<>();
                                 broadcastPlaceholders.put("player", player.getName());
                                 String formattedBroadcastAmount = this.plugin.getGuiHelper().formatAmount(amount,
                                       currencyId);
                                 broadcastPlaceholders.put("amount", formattedBroadcastAmount);
                                 broadcastPlaceholders.put("symbol", unit);
                                 String broadcastMsg = this.plugin.getMessage("prefix") + " "
                                       + this.plugin.getMessage("command.broadcast-created");
                                 this.plugin
                                       .getAdventureHelper()
                                       .broadcastWithFilter(
                                             broadcastMsg,
                                             broadcastPlaceholders,
                                             p -> this.plugin.getPlayerSettingsManager().isSettingEnabled(p,
                                                   "message-broadcasts"));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private String buildInvalidCurrencyMessage(KStudio plugin, String currencyType, String attemptedId) {
      StringBuilder message = new StringBuilder();
      message.append(plugin.getMessage("prefix"));
      message.append(" ").append(plugin.getMessage("command.invalid-currency-help"));
      List<String> availableOptions = new ArrayList<>();
      if (plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.MONEY)) {
         availableOptions.add("&emoney");
      }

      if (plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.PLAYERPOINTS)) {
         availableOptions.add("&eplayerpoints");
      }

      if (plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.TOKENMANAGER)) {
         availableOptions.add("&etokenmanager");
      }

      if (plugin.getCurrencyManager().isCurrencyEnabled(CoinFlipGame.CurrencyType.BEASTTOKENS)) {
         availableOptions.add("&ebeasttokens");
      }

      Set<String> coinsEngineIds = plugin.getCurrencyManager().getEnabledCoinsEngineCurrencyIds();
      if (!coinsEngineIds.isEmpty()) {
         for (String id : coinsEngineIds) {
            String syntaxCmd = plugin.getCurrencyManager().getSyntaxCommand(CoinFlipGame.CurrencyType.COINSENGINE, id);
            if (syntaxCmd != null && !syntaxCmd.isEmpty()) {
               availableOptions.add("&e" + syntaxCmd + " &7(or &ecoinsengine:" + id + "&7)");
            } else {
               availableOptions.add("&ecoinsengine:" + id);
            }
         }
      }

      Set<String> placeholderIds = plugin.getCurrencyManager().getEnabledPlaceholderCurrencyIds();
      if (!placeholderIds.isEmpty()) {
         for (String idx : placeholderIds) {
            String syntaxCmd = plugin.getCurrencyManager().getSyntaxCommand(CoinFlipGame.CurrencyType.PLACEHOLDER, idx);
            if (syntaxCmd != null && !syntaxCmd.isEmpty()) {
               availableOptions.add("&e" + syntaxCmd + " &7(or &eplaceholder:" + idx + "&7)");
            } else {
               availableOptions.add("&eplaceholder:" + idx);
            }
         }
      }

      if (!availableOptions.isEmpty()) {
         message.append(" ").append(plugin.getMessage("command.available-currencies").replace("<list>",
               String.join("&7, ", availableOptions)));
      } else {
         message.append(" ").append(plugin.getMessage("command.no-currencies-enabled"));
      }

      if (currencyType != null && attemptedId != null) {
         if ("coinsengine".equals(currencyType)) {
            if (coinsEngineIds.isEmpty()) {
               message.append(" ").append(plugin.getMessage("command.coinsengine-none-enabled"));
            } else {
               message.append(" ").append(plugin.getMessage("command.coinsengine-ids-available").replace("<ids>",
                     String.join("&7, &e", coinsEngineIds)));
            }
         } else if ("placeholder".equals(currencyType)) {
            if (placeholderIds.isEmpty()) {
               message.append(" ").append(plugin.getMessage("command.placeholder-none-enabled"));
            } else {
               message.append(" ").append(plugin.getMessage("command.placeholder-ids-available").replace("<ids>",
                     String.join("&7, &e", placeholderIds)));
            }
         }
      }

      return message.toString();
   }
}
