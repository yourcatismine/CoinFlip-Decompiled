package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import net.wesjd.anvilgui.AnvilGUI;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.Consumer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AnvilInputManager {
   private final KStudio plugin;

   public AnvilInputManager(KStudio plugin) {
      this.plugin = plugin;
   }

   public void requestInput(Player player, String title, String initialText, Consumer<String> callback) {
      if (player != null && player.isOnline()) {
         String cleanedInitialText = initialText != null && !initialText.isEmpty() ? this.stripColorCodes(initialText) : "";
         ItemStack leftItem = new ItemStack(Material.PAPER);
         ItemMeta meta = leftItem.getItemMeta();
         if (meta != null && !cleanedInitialText.isEmpty()) {
            meta.setDisplayName(cleanedInitialText);
            leftItem.setItemMeta(meta);
         }

         AnvilGUI.Builder builder = new AnvilGUI.Builder()
            .plugin(this.plugin)
            .title(title != null ? title : "Enter amount")
            .text(cleanedInitialText)
            .itemLeft(leftItem);
         builder.mainThreadExecutor(runnable -> FoliaScheduler.runTask(this.plugin, player, runnable));
         builder.onClick((slot, stateSnapshot) -> {
            if (slot != 2) {
               return Collections.emptyList();
            } else {
               String input = stateSnapshot.getText();
               String cleanedInput = this.stripColorCodes(input);
               if (cleanedInput != null && !cleanedInput.isEmpty()) {
                  FoliaScheduler.runTask(this.plugin, player, () -> {
                     this.plugin.getSoundHelper().playSound(player, "input.amount-submit");
                     callback.accept(cleanedInput);
                  });
                  return Collections.singletonList(AnvilGUI.ResponseAction.close());
               } else {
                  this.plugin.getLogger().warning("[AnvilInputManager] Cleaned input is empty");
                  return Collections.emptyList();
               }
            }
         });
         builder.onClose(stateSnapshot -> {});
         FoliaScheduler.runTask(this.plugin, player, () -> {
            try {
               builder.open(player);
               FoliaScheduler.runTaskLater(this.plugin, player, () -> this.plugin.getSoundHelper().playSound(player, "gui.open"), 1L);
            } catch (Exception | NoClassDefFoundError var5x) {
               this.plugin.getLogger().warning("Failed to open AnvilGUI for " + player.getName() + ": " + var5x.getMessage());
               var5x.printStackTrace();
               if (callback != null) {
                  this.plugin.getChatInputManager().requestInput(player, callback);
               }
            }
         });
      } else {
         this.plugin.getLogger().warning("[AnvilInputManager] requestInput: Player is null or offline");
      }
   }

   public void cancelInput(Player player) {
      if (player != null && player.isOnline()) {
         try {
            Method getOpenInvMethod = player.getClass().getMethod("getOpenInventory");
            Object view = getOpenInvMethod.invoke(player);
            if (view != null) {
               player.closeInventory();
            }
         } catch (Exception var5) {
            try {
               player.closeInventory();
            } catch (Exception var4) {
            }
         }
      }
   }

   private String stripColorCodes(String message) {
      if (message != null && !message.isEmpty()) {
         String cleaned = message.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
         cleaned = cleaned.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
         cleaned = cleaned.replaceAll("<[^>]+>", "");
         cleaned = cleaned.replaceAll("</[^>]+>", "");
         cleaned = cleaned.replaceAll("<#[0-9A-Fa-f]{6}>", "");
         cleaned = cleaned.replaceAll("\u001b\\[[0-9;]*m", "");
         cleaned = cleaned.replaceAll("\\u001B\\[[0-9;]*m", "");
         cleaned = cleaned.replaceAll("&[#xX][0-9A-Fa-f]{6}", "");
         return cleaned.trim();
      } else {
         return message;
      }
   }
}
