package com.kstudio.ultracoinflip.adventure;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.util.LegacyCompatibility;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.Sound.Source;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class AdventureHelper {
   private static final String SILENT_PERMISSION = "ultracoinflip.silent";
   private final KStudio plugin;
   private final TextService textService;

   public AdventureHelper(KStudio plugin) {
      this.plugin = plugin;
      this.textService = new TextService();
   }

   @Deprecated
   private static Object getMiniMessage() {
      return null;
   }

   @Deprecated
   private boolean hasInteractiveTags(String text) {
      return false;
   }

   @Deprecated
   private Object deserializeMiniMessage(String text) {
      if (this.textService.supportsInteractive()) {
         try {
            return MiniMessage.miniMessage().deserialize(text);
         } catch (Exception var4) {
            String legacy = this.textService.getEngine().toLegacy(text);
            return LegacyComponentSerializer.legacySection().deserialize(legacy);
         }
      } else {
         String legacy = this.textService.getEngine().toLegacy(text);

         try {
            return LegacyComponentSerializer.legacySection().deserialize(legacy);
         } catch (Exception var5) {
            return Component.empty();
         }
      }
   }

   @Deprecated
   private Object deserializeMiniMessage(String text, Object... resolvers) {
      return this.deserializeMiniMessage(text);
   }

   private Component deserializeLegacy(String text) {
      try {
         return LegacyComponentSerializer.legacySection().deserialize(text);
      } catch (Exception var3) {
         return Component.empty();
      }
   }

   private Component createEmptyComponent() {
      return Component.empty();
   }

   @Deprecated
   private Object[] createTagResolvers(Map<String, String> placeholders) {
      return new Object[0];
   }

   private boolean checkAdventureSupport() {
      try {
         try {
            Player.class.getMethod("sendMessage", Component.class);
            return true;
         } catch (NoSuchMethodException var2) {
            return false;
         }
      } catch (Exception var3) {
         return false;
      }
   }

   private String normalizeHexColors(String text) {
      if (text != null && !text.isEmpty()) {
         Pattern hexPattern = Pattern.compile("([&]?)#([0-9A-Fa-f]{6})");
         Matcher matcher = hexPattern.matcher(text);
         StringBuffer result = new StringBuffer();

         while (matcher.find()) {
            String hexCode = matcher.group(2).toUpperCase();
            matcher.appendReplacement(result, "<#" + hexCode + ">");
         }

         matcher.appendTail(result);
         return result.toString();
      } else {
         return text;
      }
   }

   private String convertLegacyToMiniMessage(String text) {
      if (text != null && !text.isEmpty()) {
         Pattern legacyPattern = Pattern.compile("&([0-9a-fk-or])");
         Matcher matcher = legacyPattern.matcher(text);
         StringBuffer result = new StringBuffer();

         while (matcher.find()) {
            String code = matcher.group(1);
            String miniMessageCode = this.convertLegacyCodeToMiniMessage(code);
            matcher.appendReplacement(result, miniMessageCode);
         }

         matcher.appendTail(result);
         return result.toString();
      } else {
         return text;
      }
   }

   private String convertLegacyCodeToMiniMessage(String code) {
      String var2 = code.toLowerCase();
      switch (var2) {
         case "0":
            return "<black>";
         case "1":
            return "<dark_blue>";
         case "2":
            return "<dark_green>";
         case "3":
            return "<dark_aqua>";
         case "4":
            return "<dark_red>";
         case "5":
            return "<dark_purple>";
         case "6":
            return "<gold>";
         case "7":
            return "<gray>";
         case "8":
            return "<dark_gray>";
         case "9":
            return "<blue>";
         case "a":
            return "<green>";
         case "b":
            return "<aqua>";
         case "c":
            return "<red>";
         case "d":
            return "<light_purple>";
         case "e":
            return "<yellow>";
         case "f":
            return "<white>";
         case "k":
            return "<obfuscated>";
         case "l":
            return "<bold>";
         case "m":
            return "<strikethrough>";
         case "n":
            return "<underline>";
         case "o":
            return "<italic>";
         case "r":
            return "<reset>";
         default:
            return "&" + code;
      }
   }

   public Component parse(String text) {
      if (text != null && !text.isEmpty()) {
         String normalized = this.normalizeHexColors(text);
         boolean hasHexColors = normalized.contains("<#");
         boolean hasMiniMessageTags = normalized.contains("<click:")
            || normalized.contains("<hover:")
            || normalized.contains("<key:")
            || normalized.contains("<insert:")
            || normalized.contains("<lang:")
            || normalized.contains("<reset>")
            || normalized.contains("<bold>")
            || normalized.contains("<italic>")
            || normalized.contains("<underlined>")
            || normalized.contains("<strikethrough>")
            || normalized.contains("<obfuscated>")
            || normalized.contains("<color:")
            || normalized.contains("<gradient:")
            || normalized.contains("<rainbow>");
         boolean isMiniMessageFormat = normalized.trim().startsWith("<") && !normalized.startsWith("&");
         if (!hasHexColors && !hasMiniMessageTags && !isMiniMessageFormat) {
            return this.deserializeLegacy(normalized);
         } else {
            String fullyConverted = this.convertLegacyToMiniMessage(normalized);
            Object component = this.deserializeMiniMessage(fullyConverted);
            return component != null ? (Component)component : this.createEmptyComponent();
         }
      } else {
         return Component.empty();
      }
   }

   public Component parse(String text, TagResolver... resolvers) {
      if (text != null && !text.isEmpty()) {
         String normalized = this.normalizeHexColors(text);
         boolean hasHexColors = normalized.contains("<#");
         boolean hasMiniMessageTags = normalized.contains("<click:")
            || normalized.contains("<hover:")
            || normalized.contains("<key:")
            || normalized.contains("<insert:")
            || normalized.contains("<lang:")
            || normalized.contains("<reset>")
            || normalized.contains("<bold>")
            || normalized.contains("<italic>")
            || normalized.contains("<underlined>")
            || normalized.contains("<strikethrough>")
            || normalized.contains("<obfuscated>")
            || normalized.contains("<color:")
            || normalized.contains("<gradient:")
            || normalized.contains("<rainbow>");
         boolean isMiniMessageFormat = normalized.trim().startsWith("<") && !normalized.startsWith("&");
         if (!hasHexColors && !hasMiniMessageTags && !isMiniMessageFormat) {
            return this.deserializeLegacy(normalized);
         } else {
            String fullyConverted = this.convertLegacyToMiniMessage(normalized);
            Object component = this.deserializeMiniMessage(fullyConverted, resolvers);
            return component != null ? (Component)component : this.createEmptyComponent();
         }
      } else {
         return Component.empty();
      }
   }

   public Component parse(String text, Map<String, String> placeholders, Player player) {
      if (text != null && !text.isEmpty()) {
         Component parsed = this.parse(text, placeholders);
         if (player != null && this.plugin.isPlaceholderAPI()) {
            String legacyText = this.toLegacy(parsed);
            String placeholderAPIText = PlaceholderAPI.setPlaceholders(player, legacyText);
            parsed = this.parse(placeholderAPIText);
         }

         return parsed;
      } else {
         return Component.empty();
      }
   }

   public Component parse(String text, Map<String, String> placeholders) {
      if (text != null && !text.isEmpty()) {
         String normalized = this.normalizeHexColors(text);
         boolean hasHexColors = normalized.contains("<#");
         boolean hasMiniMessageTags = normalized.contains("<click:")
            || normalized.contains("<hover:")
            || normalized.contains("<key:")
            || normalized.contains("<insert:")
            || normalized.contains("<lang:")
            || normalized.contains("<reset>")
            || normalized.contains("<bold>")
            || normalized.contains("<italic>")
            || normalized.contains("<underlined>")
            || normalized.contains("<strikethrough>")
            || normalized.contains("<obfuscated>")
            || normalized.contains("<color:")
            || normalized.contains("<gradient:")
            || normalized.contains("<rainbow>");
         boolean isMiniMessageFormat = normalized.trim().startsWith("<") && !normalized.startsWith("&");
         if (!hasHexColors && !hasMiniMessageTags && !isMiniMessageFormat) {
            if (placeholders != null && !placeholders.isEmpty()) {
               StringBuilder processed = new StringBuilder(normalized.length() + placeholders.size() * 20);
               processed.append(normalized);

               for (Entry<String, String> entry : placeholders.entrySet()) {
                  String key = entry.getKey();
                  String value = entry.getValue();
                  if (value == null) {
                     value = "";
                  }

                  String placeholder1 = "<" + key + ">";

                  int index1;
                  while ((index1 = processed.indexOf(placeholder1)) != -1) {
                     processed.replace(index1, index1 + placeholder1.length(), value);
                  }

                  String placeholder2 = "%" + key + "%";

                  int index2;
                  while ((index2 = processed.indexOf(placeholder2)) != -1) {
                     processed.replace(index2, index2 + placeholder2.length(), value);
                  }
               }

               String finalText = processed.toString();
               String finalNormalized = this.normalizeHexColors(finalText);
               boolean hasHexAfterReplace = finalNormalized.contains("<#");
               boolean hasLegacyColorCodes = finalNormalized.contains("&") && Pattern.compile("&[0-9a-fk-or]").matcher(finalNormalized).find();
               if (hasHexAfterReplace) {
                  String fullyConverted = this.convertLegacyToMiniMessage(finalNormalized);
                  Object component = this.deserializeMiniMessage(fullyConverted);
                  return component != null ? (Component)component : this.createEmptyComponent();
               } else {
                  return hasLegacyColorCodes ? this.deserializeLegacy(finalNormalized) : this.deserializeLegacy(finalNormalized);
               }
            } else {
               return this.deserializeLegacy(normalized);
            }
         } else {
            String fullyConverted = this.convertLegacyToMiniMessage(normalized);
            Object[] resolvers = this.createTagResolvers(placeholders);
            Object component = this.deserializeMiniMessage(fullyConverted, resolvers);
            return component != null ? (Component)component : this.createEmptyComponent();
         }
      } else {
         return Component.empty();
      }
   }

   public void sendMessage(Player player, Component component) {
      if (player != null && player.isOnline()) {
         if (this.textService.supportsInteractive()) {
            try {
               player.sendMessage(component);
               return;
            } catch (AbstractMethodError | NoSuchMethodError var4) {
            } catch (Exception var5) {
            }
         }

         this.sendLegacyMessage(player, component);
      }
   }

   public void sendMessage(Player player, String miniMessage) {
      if (miniMessage != null && !miniMessage.trim().isEmpty()) {
         if (player != null && player.isOnline()) {
            this.textService.getEngine().send(player, miniMessage);
         }
      }
   }

   public void sendMessage(Player player, String miniMessage, Map<String, String> placeholders) {
      if (miniMessage != null && !miniMessage.trim().isEmpty()) {
         if (player != null && player.isOnline()) {
            this.textService.getEngine().send(player, miniMessage, placeholders);
         }
      }
   }

   private void sendLegacyMessage(Player player, Component component) {
      if (player != null && player.isOnline()) {
         try {
            String legacy = this.toLegacy(component);
            player.sendMessage(legacy);
         } catch (Exception var4) {
         }
      }
   }

   public void broadcast(Component component) {
      for (Player player : Bukkit.getOnlinePlayers()) {
         if (player != null && player.isOnline() && !this.hasExplicitSilentPermission(player)) {
            this.sendMessage(player, component);
         }
      }
   }

   public void broadcast(String miniMessage) {
      if (miniMessage != null && !miniMessage.trim().isEmpty()) {
         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline() && !this.hasExplicitSilentPermission(player)) {
               this.textService.getEngine().send(player, miniMessage);
            }
         }
      }
   }

   public void broadcastWithFilter(String miniMessage, Map<String, String> placeholders, Predicate<Player> filter) {
      if (miniMessage != null && !miniMessage.trim().isEmpty()) {
         for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null && player.isOnline() && !this.hasExplicitSilentPermission(player) && (filter == null || filter.test(player))) {
               this.textService.getEngine().send(player, miniMessage, placeholders);
            }
         }
      }
   }

   public void broadcast(String miniMessage, Map<String, String> placeholders) {
      this.broadcastWithFilter(miniMessage, placeholders, null);
   }

   private void sendLegacyTitle(Player player, Component title, Component subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
      if (player != null && player.isOnline()) {
         try {
            String titleLegacy = this.toLegacy(title);
            String subtitleLegacy = this.toLegacy(subtitle);
            int fadeInTicks = (int)(fadeIn.toMillis() / 50L);
            int stayTicks = (int)(stay.toMillis() / 50L);
            int fadeOutTicks = (int)(fadeOut.toMillis() / 50L);

            try {
               Method sendTitleMethod = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
               sendTitleMethod.invoke(player, titleLegacy, subtitleLegacy, fadeInTicks, stayTicks, fadeOutTicks);
               return;
            } catch (NoSuchMethodException var14) {
               try {
                  Method sendTitleMethodx = player.getClass().getMethod("sendTitle", String.class, String.class);
                  sendTitleMethodx.invoke(player, titleLegacy, subtitleLegacy);
               } catch (NoSuchMethodException var13) {
               }
            }
         } catch (Throwable var15) {
         }
      }
   }

   private boolean hasExplicitSilentPermission(Player player) {
      return player == null ? false : player.isPermissionSet("ultracoinflip.silent") && player.hasPermission("ultracoinflip.silent");
   }

   public void sendTitle(Player player, Component title, Component subtitle, Duration fadeIn, Duration stay, Duration fadeOut) {
      if (player != null && player.isOnline()) {
         if (this.textService.supportsInteractive()) {
            try {
               Title titleObj = Title.title(title, subtitle, Times.times(fadeIn, stay, fadeOut));
               player.showTitle(titleObj);
               return;
            } catch (AbstractMethodError | NoSuchMethodError var8) {
            } catch (Exception var9) {
            }
         }

         this.sendLegacyTitle(player, title, subtitle, fadeIn, stay, fadeOut);
      }
   }

   public void sendTitle(Player player, String titleMiniMessage, String subtitleMiniMessage) {
      if (player != null && player.isOnline()) {
         this.sendLegacyTitleDirect(player, titleMiniMessage, subtitleMiniMessage, null, 10, 60, 10);
      }
   }

   public void sendTitle(Player player, String titleMiniMessage, String subtitleMiniMessage, Map<String, String> placeholders) {
      if (player != null && player.isOnline()) {
         this.sendLegacyTitleDirect(player, titleMiniMessage, subtitleMiniMessage, placeholders, 10, 60, 10);
      }
   }

   private void sendLegacyTitleDirect(Player player, String titleText, String subtitleText, Map<String, String> placeholders, int fadeIn, int stay, int fadeOut) {
      if (player != null && player.isOnline()) {
         try {
            String titleLegacy = placeholders != null ? this.parseToLegacy(titleText, placeholders) : this.parseToLegacy(titleText);
            String subtitleLegacy = placeholders != null ? this.parseToLegacy(subtitleText, placeholders) : this.parseToLegacy(subtitleText);

            try {
               Method sendTitleMethod = player.getClass().getMethod("sendTitle", String.class, String.class, int.class, int.class, int.class);
               sendTitleMethod.invoke(player, titleLegacy, subtitleLegacy, fadeIn, stay, fadeOut);
               return;
            } catch (NoSuchMethodException var12) {
               try {
                  Method sendTitleMethodx = player.getClass().getMethod("sendTitle", String.class, String.class);
                  sendTitleMethodx.invoke(player, titleLegacy, subtitleLegacy);
               } catch (NoSuchMethodException var11) {
               }
            }
         } catch (Throwable var13) {
         }
      }
   }

   public void sendActionBar(Player player, Component component) {
      if (player != null && player.isOnline()) {
         try {
            player.sendActionBar(component);
         } catch (AbstractMethodError | NoSuchMethodError var6) {
            String legacy = this.toLegacy(component);
            player.sendMessage(legacy);
         } catch (Exception var7) {
            try {
               String legacyx = this.toLegacy(component);
               player.sendMessage(legacyx);
            } catch (Exception var5) {
            }
         }
      }
   }

   public void sendActionBar(Player player, String miniMessage) {
      if (player != null && player.isOnline()) {
         this.sendLegacyActionBarDirect(player, miniMessage, null);
      }
   }

   public void sendActionBar(Player player, String miniMessage, Map<String, String> placeholders) {
      if (player != null && player.isOnline()) {
         this.sendLegacyActionBarDirect(player, miniMessage, placeholders);
      }
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private void sendLegacyActionBarDirect(Player player, String message, Map<String, String> placeholders) {
      if (player != null && player.isOnline()) {
         try {
            String legacy = placeholders != null
               ? this.textService.getEngine().toLegacy(message, placeholders)
               : this.textService.getEngine().toLegacy(message);

            try {
               Object spigot = player.getClass().getMethod("spigot").invoke(player);
               Class chatMessageTypeClass = Class.forName("net.md_5.bungee.api.ChatMessageType");
               Object actionBarType = Enum.valueOf(chatMessageTypeClass, "ACTION_BAR");
               Class<?> textComponentClass = Class.forName("net.md_5.bungee.api.chat.TextComponent");
               Object textComponent = textComponentClass.getConstructor(String.class).newInstance(legacy);
               Class<?> baseComponentClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent");
               spigot.getClass().getMethod("sendMessage", chatMessageTypeClass, baseComponentClass).invoke(spigot, actionBarType, textComponent);
            } catch (Exception var11) {
               player.sendMessage(legacy);
            }
         } catch (Exception var12) {
         }
      }
   }

   public BossBar sendBossBar(Player player, Component title, float progress, Color color, Overlay overlay, int duration) {
      if (player != null && player.isOnline()) {
         progress = Math.max(0.0F, Math.min(1.0F, progress));
         BossBar bossBar = BossBar.bossBar(title, progress, color, overlay);

         try {
            player.showBossBar(bossBar);
         } catch (AbstractMethodError | NoSuchMethodError var11) {
            String legacy = this.toLegacy(title);
            player.sendMessage(legacy + " " + (int)(progress * 100.0F) + "%");
         } catch (Exception var12) {
            try {
               String legacyx = this.toLegacy(title);
               player.sendMessage(legacyx + " " + (int)(progress * 100.0F) + "%");
            } catch (Exception var10) {
            }
         }

         if (duration > 0) {
         }

         return bossBar;
      } else {
         return null;
      }
   }

   public BossBar sendBossBar(
      Player player, String titleMiniMessage, float progress, String colorStr, String overlayStr, int duration, Map<String, String> placeholders
   ) {
      if (player != null && player.isOnline()) {
         Component title;
         try {
            title = this.parse(titleMiniMessage, placeholders);
         } catch (Throwable var16) {
            String legacy = this.textService.getEngine().toLegacy(titleMiniMessage, placeholders);

            try {
               Class<?> legacySerializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
               Method legacySectionMethod = legacySerializerClass.getMethod("legacySection");
               Object serializer = legacySectionMethod.invoke(null);
               Method deserializeMethod = serializer.getClass().getMethod("deserialize", String.class);
               title = (Component)deserializeMethod.invoke(serializer, legacy);
            } catch (Exception var15) {
               player.sendMessage(legacy + " " + (int)(progress * 100.0F) + "%");
               return null;
            }
         }

         Color color = this.parseBossBarColor(colorStr);
         Overlay overlay = this.parseBossBarOverlay(overlayStr);
         return this.sendBossBar(player, title, progress, color, overlay, duration);
      } else {
         return null;
      }
   }

   public BossBar sendBossBar(Player player, String titleMiniMessage, float progress, String colorStr, String overlayStr, int duration) {
      return this.sendBossBar(player, titleMiniMessage, progress, colorStr, overlayStr, duration, new HashMap<>());
   }

   public Color parseBossBarColor(String colorStr) {
      if (colorStr != null && !colorStr.isEmpty()) {
         try {
            return Color.valueOf(colorStr.toUpperCase());
         } catch (IllegalArgumentException var3) {
            return Color.GREEN;
         }
      } else {
         return Color.GREEN;
      }
   }

   public Overlay parseBossBarOverlay(String overlayStr) {
      if (overlayStr != null && !overlayStr.isEmpty()) {
         try {
            return Overlay.valueOf(overlayStr.toUpperCase());
         } catch (IllegalArgumentException var3) {
            return Overlay.PROGRESS;
         }
      } else {
         return Overlay.PROGRESS;
      }
   }

   public void removeBossBar(Player player, BossBar bossBar) {
      if (player != null && player.isOnline() && bossBar != null) {
         try {
            player.hideBossBar(bossBar);
         } catch (AbstractMethodError | NoSuchMethodError var4) {
         } catch (Exception var5) {
         }
      }
   }

   public void playSound(Player player, Sound sound) {
      if (player != null && player.isOnline()) {
         try {
            player.playSound(sound);
         } catch (AbstractMethodError | NoSuchMethodError var11) {
            try {
               String soundName = sound.name().asString();
               float volume = sound.volume();
               float pitch = sound.pitch();

               try {
                  String processedSound = soundName.toUpperCase();
                  if (processedSound.startsWith("minecraft:")) {
                     processedSound = processedSound.substring(10);
                  }

                  processedSound = processedSound.replace(".", "_");
                  org.bukkit.Sound bukkitSound = org.bukkit.Sound.valueOf(processedSound);
                  LegacyCompatibility.playSound(player, bukkitSound, volume, pitch);
               } catch (IllegalArgumentException var9) {
                  LegacyCompatibility.playSound(player, soundName, volume, pitch);
               }
            } catch (Exception var10) {
            }
         } catch (Exception var12) {
         }
      }
   }

   public void playSound(Player player, String soundName, float volume, float pitch) {
      if (player != null && player.isOnline() && soundName != null && !soundName.isEmpty()) {
         if (soundName.contains(":")) {
            try {
               String[] parts = soundName.split(":", 2);
               if (parts.length == 2) {
                  Key soundKey = Key.key(parts[0], parts[1]);
                  Sound adventureSound = Sound.sound(soundKey, Source.PLAYER, volume, pitch);
                  this.playSound(player, adventureSound);
                  return;
               }
            } catch (Exception var8) {
            }

            LegacyCompatibility.playSound(player, soundName, volume, pitch);
         } else {
            try {
               Method valueOfMethod = org.bukkit.Sound.class.getMethod("valueOf", String.class);
               org.bukkit.Sound sound = (org.bukkit.Sound)valueOfMethod.invoke(null, soundName.toUpperCase());
               LegacyCompatibility.playSound(player, sound, volume, pitch);
            } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException | NoSuchMethodException var9) {
               LegacyCompatibility.playSound(player, soundName, volume, pitch);
            }
         }
      }
   }

   public String toLegacy(Component component) {
      if (component == null) {
         return "";
      } else {
         try {
            Class<?> legacySerializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Method legacySectionMethod = legacySerializerClass.getMethod("legacySection");
            Object serializer = legacySectionMethod.invoke(null);
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method serializeMethod = serializer.getClass().getMethod("serialize", componentClass);
            return (String)serializeMethod.invoke(serializer, component);
         } catch (ClassNotFoundException var8) {
            return this.extractTextFromComponent(component);
         } catch (Exception var9) {
            try {
               return this.extractTextFromComponent(component);
            } catch (Exception var7) {
               return "";
            }
         }
      }
   }

   private String extractTextFromComponent(Component component) {
      if (component == null) {
         return "";
      } else {
         try {
            Method asPlainTextMethod = component.getClass().getMethod("asPlainText");
            return (String)asPlainTextMethod.invoke(component);
         } catch (NoSuchMethodException var8) {
            try {
               Method contentMethod = component.getClass().getMethod("content");
               Object content = contentMethod.invoke(component);
               if (content instanceof String) {
                  return (String)content;
               }
            } catch (Exception var7) {
            }

            try {
               String componentStr = component.toString();
               return this.extractLegacyColors(componentStr);
            } catch (Exception var6) {
               return "";
            }
         } catch (Exception var9) {
            try {
               return this.extractLegacyColors(component.toString());
            } catch (Exception var5) {
               return "";
            }
         }
      }
   }

   private String extractLegacyColors(String componentStr) {
      if (componentStr != null && !componentStr.isEmpty()) {
         String result = componentStr.replace("TextComponentImpl{content='", "").replace("'}", "").replace("TextComponent{content='", "").replace("'}", "");
         return !result.equals(componentStr) && !result.isEmpty() ? result : componentStr;
      } else {
         return "";
      }
   }

   public String parseToLegacy(String text) {
      return text != null && !text.isEmpty() ? this.parseDirectlyToLegacy(text, null, null) : "";
   }

   public String parseToLegacy(String text, Map<String, String> placeholders, Player player) {
      return text != null && !text.isEmpty() ? this.parseDirectlyToLegacy(text, placeholders, player) : "";
   }

   public String parseToLegacy(String text, Map<String, String> placeholders) {
      return text != null && !text.isEmpty() ? this.parseDirectlyToLegacy(text, placeholders, null) : "";
   }

   private String parseDirectlyToLegacy(String text, Map<String, String> placeholders, Player player) {
      if (text != null && !text.isEmpty()) {
         String result = text;
         if (placeholders != null && !placeholders.isEmpty()) {
            for (Entry<String, String> entry : placeholders.entrySet()) {
               String key = entry.getKey();
               String value = entry.getValue() != null ? entry.getValue() : "";
               result = result.replace("<" + key + ">", value);
               result = result.replace("%" + key + "%", value);
            }
         }

         if (player != null && this.plugin.isPlaceholderAPI()) {
            try {
               result = PlaceholderAPI.setPlaceholders(player, result);
            } catch (Exception var9) {
            }
         }

         result = this.convertMiniMessageToLegacy(result);
         result = this.removeHexColors(result);
         return ChatColor.translateAlternateColorCodes('&', result);
      } else {
         return "";
      }
   }

   private String convertMiniMessageToLegacy(String text) {
      if (text != null && !text.isEmpty()) {
         String result = text.replaceAll("<click:[^>]+>", "");
         result = result.replace("</click>", "");
         result = result.replaceAll("<hover:[^>]+>", "");
         result = result.replace("</hover>", "");
         result = result.replaceAll("<key:[^>]+>", "");
         result = result.replaceAll("<insert:[^>]+>", "");
         result = result.replaceAll("<lang:[^>]+>", "");
         result = result.replace("</lang>", "");
         result = result.replaceAll("<gradient:[^>]+>", "");
         result = result.replace("</gradient>", "");
         result = result.replaceAll("<rainbow[^>]*>", "");
         result = result.replace("</rainbow>", "");
         result = result.replace("<reset>", "&r")
            .replace("</reset>", "&r")
            .replace("<black>", "&0")
            .replace("</black>", "&r")
            .replace("<dark_blue>", "&1")
            .replace("</dark_blue>", "&r")
            .replace("<dark_green>", "&2")
            .replace("</dark_green>", "&r")
            .replace("<dark_aqua>", "&3")
            .replace("</dark_aqua>", "&r")
            .replace("<dark_red>", "&4")
            .replace("</dark_red>", "&r")
            .replace("<dark_purple>", "&5")
            .replace("</dark_purple>", "&r")
            .replace("<gold>", "&6")
            .replace("</gold>", "&r")
            .replace("<gray>", "&7")
            .replace("</gray>", "&r")
            .replace("<dark_gray>", "&8")
            .replace("</dark_gray>", "&r")
            .replace("<blue>", "&9")
            .replace("</blue>", "&r")
            .replace("<green>", "&a")
            .replace("</green>", "&r")
            .replace("<aqua>", "&b")
            .replace("</aqua>", "&r")
            .replace("<red>", "&c")
            .replace("</red>", "&r")
            .replace("<light_purple>", "&d")
            .replace("</light_purple>", "&r")
            .replace("<yellow>", "&e")
            .replace("</yellow>", "&r")
            .replace("<white>", "&f")
            .replace("</white>", "&r")
            .replace("<obfuscated>", "&k")
            .replace("</obfuscated>", "&r")
            .replace("<bold>", "&l")
            .replace("</bold>", "&r")
            .replace("<b>", "&l")
            .replace("</b>", "&r")
            .replace("<strikethrough>", "&m")
            .replace("</strikethrough>", "&r")
            .replace("<st>", "&m")
            .replace("</st>", "&r")
            .replace("<underline>", "&n")
            .replace("</underline>", "&r")
            .replace("<underlined>", "&n")
            .replace("</underlined>", "&r")
            .replace("<u>", "&n")
            .replace("</u>", "&r")
            .replace("<italic>", "&o")
            .replace("</italic>", "&r")
            .replace("<i>", "&o")
            .replace("</i>", "&r");
         result = result.replaceAll("<#[0-9A-Fa-f]{6}>", "");
         result = result.replaceAll("</?#[0-9A-Fa-f]{6}>", "");
         return result.replaceAll("<[^>]+>", "");
      } else {
         return text;
      }
   }

   private String removeHexColors(String text) {
      if (text != null && !text.isEmpty()) {
         String result = text.replaceAll("#[0-9A-Fa-f]{6}", "");
         return result.replaceAll("&#[0-9A-Fa-f]{6}", "");
      } else {
         return text;
      }
   }

   public Audience player(Player player) {
      return player;
   }

   public Audience all() {
      return Bukkit.getServer();
   }

   public static Map<String, String> placeholders(String... keyValues) {
      Map<String, String> map = new HashMap<>();

      for (int i = 0; i < keyValues.length; i += 2) {
         if (i + 1 < keyValues.length) {
            map.put(keyValues[i], keyValues[i + 1]);
         }
      }

      return map;
   }
}
