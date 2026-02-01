package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatInputManager implements Listener {
   private final KStudio plugin;
   private final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();

   public ChatInputManager(KStudio plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void requestInput(Player player, Consumer<String> callback) {
      if (player != null && player.isOnline()) {
         this.pendingInputs.put(player.getUniqueId(), callback);
      }
   }

   public void cancelInput(Player player) {
      if (player != null) {
         this.pendingInputs.remove(player.getUniqueId());
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.pendingInputs.containsKey(uuid)) {
         event.setCancelled(true);
         Consumer<String> callback = this.pendingInputs.remove(uuid);
         String rawMessage = event.getMessage();
         String cleanedMessage = this.stripColorCodes(rawMessage);
         this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (callback != null) {
               callback.accept(cleanedMessage);
            }
         });
      }
   }

   @EventHandler(priority = EventPriority.HIGHEST)
   public void onCommand(PlayerCommandPreprocessEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.pendingInputs.containsKey(uuid)) {
         event.setCancelled(true);
         Consumer<String> callback = this.pendingInputs.remove(uuid);
         String commandMessage = event.getMessage();
         String rawMessage = commandMessage.startsWith("/") ? commandMessage.substring(1) : commandMessage;
         String cleanedMessage = this.stripColorCodes(rawMessage);
         this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            if (callback != null) {
               this.plugin.getSoundHelper().playSound(player, "input.amount-submit");
               callback.accept(cleanedMessage);
            }
         });
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.pendingInputs.remove(event.getPlayer().getUniqueId());
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
