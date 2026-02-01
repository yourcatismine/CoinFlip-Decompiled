package com.kstudio.ultracoinflip.adventure;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class LegacyTextEngine implements TextEngine {
   private static final Pattern MINIMESSAGE_TAG_PATTERN = Pattern.compile("<[^>]+>");

   @Override
   public void send(Player player, String text) {
      if (player != null && player.isOnline()) {
         String legacy = this.toLegacy(text);
         player.sendMessage(legacy);
      }
   }

   @Override
   public void send(Player player, String text, Map<String, String> placeholders) {
      if (player != null && player.isOnline()) {
         String legacy = this.toLegacy(text, placeholders);
         player.sendMessage(legacy);
      }
   }

   @Override
   public String toLegacy(String text) {
      if (text != null && !text.isEmpty()) {
         String stripped = this.stripMiniMessage(text);
         return ChatColor.translateAlternateColorCodes('&', stripped);
      } else {
         return "";
      }
   }

   @Override
   public String toLegacy(String text, Map<String, String> placeholders) {
      if (text != null && !text.isEmpty()) {
         String processed = text;
         if (placeholders != null && !placeholders.isEmpty()) {
            for (Entry<String, String> entry : placeholders.entrySet()) {
               String key = entry.getKey();
               String value = entry.getValue() != null ? entry.getValue() : "";
               processed = processed.replace("<" + key + ">", value);
               processed = processed.replace("%" + key + "%", value);
            }
         }

         return this.toLegacy(processed);
      } else {
         return "";
      }
   }

   @Override
   public boolean supportsInteractive() {
      return false;
   }

   private String stripMiniMessage(String text) {
      return MINIMESSAGE_TAG_PATTERN.matcher(text).replaceAll("");
   }
}
