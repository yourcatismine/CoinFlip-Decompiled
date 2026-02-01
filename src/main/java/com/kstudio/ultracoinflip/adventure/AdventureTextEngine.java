package com.kstudio.ultracoinflip.adventure;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

public class AdventureTextEngine implements TextEngine {
   private final MiniMessage miniMessage = MiniMessage.miniMessage();
   private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.legacySection();

   @Override
   public void send(Player player, String text) {
      if (player != null && player.isOnline()) {
         String processed = this.convertLegacyToMiniMessage(text);
         Component component = this.miniMessage.deserialize(processed);
         player.sendMessage(component);
      }
   }

   @Override
   public void send(Player player, String text, Map<String, String> placeholders) {
      if (player != null && player.isOnline()) {
         String processed = text;
         if (placeholders != null && !placeholders.isEmpty()) {
            for (Entry<String, String> entry : placeholders.entrySet()) {
               String key = entry.getKey();
               String value = entry.getValue() != null ? entry.getValue() : "";
               processed = processed.replace("<" + key + ">", value);
               processed = processed.replace("%" + key + "%", value);
            }
         }

         processed = this.convertLegacyToMiniMessage(processed);
         Component component = this.miniMessage.deserialize(processed);
         player.sendMessage(component);
      }
   }

   @Override
   public String toLegacy(String text) {
      if (text != null && !text.isEmpty()) {
         String processed = this.convertLegacyToMiniMessage(text);
         Component component = this.miniMessage.deserialize(processed);
         return this.legacySerializer.serialize(component);
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

         processed = this.convertLegacyToMiniMessage(processed);
         Component component = this.miniMessage.deserialize(processed);
         return this.legacySerializer.serialize(component);
      } else {
         return "";
      }
   }

   @Override
   public boolean supportsInteractive() {
      return true;
   }

   private String convertLegacyToMiniMessage(String text) {
      if (text != null && !text.isEmpty()) {
         Map<String, String> colorMap = new HashMap<>();
         colorMap.put("&0", "<black>");
         colorMap.put("&1", "<dark_blue>");
         colorMap.put("&2", "<dark_green>");
         colorMap.put("&3", "<dark_aqua>");
         colorMap.put("&4", "<dark_red>");
         colorMap.put("&5", "<dark_purple>");
         colorMap.put("&6", "<gold>");
         colorMap.put("&7", "<gray>");
         colorMap.put("&8", "<dark_gray>");
         colorMap.put("&9", "<blue>");
         colorMap.put("&a", "<green>");
         colorMap.put("&b", "<aqua>");
         colorMap.put("&c", "<red>");
         colorMap.put("&d", "<light_purple>");
         colorMap.put("&e", "<yellow>");
         colorMap.put("&f", "<white>");
         colorMap.put("&k", "<obfuscated>");
         colorMap.put("&l", "<bold>");
         colorMap.put("&m", "<strikethrough>");
         colorMap.put("&n", "<underlined>");
         colorMap.put("&o", "<italic>");
         colorMap.put("&r", "<reset>");
         String result = text;

         for (Entry<String, String> entry : colorMap.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
         }

         return result;
      } else {
         return text;
      }
   }
}
