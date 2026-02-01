package com.kstudio.ultracoinflip.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.gui.cache.PlayerHeadCache;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.LegacyCompatibility;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

public class GUIHelper {
   private final KStudio plugin;
   private final boolean supportsComponent;
   private static final DecimalFormat COMMA_FORMAT = new DecimalFormat("#,###", DecimalFormatSymbols.getInstance(Locale.US));
   private final PlayerHeadCache headCache;

   public GUIHelper(KStudio plugin) {
      this.plugin = plugin;
      this.supportsComponent = this.checkComponentSupport();
      this.headCache = new PlayerHeadCache();
   }

   public String formatAmount(double amount) {
      return this.formatAmount(amount, null);
   }

   public String formatAmount(double amount, String currencyId) {
      GUIHelper.FormatType formatType = this.getFormatType(currencyId);
      boolean removeTrailingZeros = this.plugin.getConfig().getBoolean("number-format.remove-trailing-zeros", true);
      switch (formatType) {
         case COMPACT:
            return this.formatCompact(amount, removeTrailingZeros);
         case COMMAS:
            return this.formatWithCommas(amount, removeTrailingZeros);
         case FULL:
            return this.formatFull(amount, removeTrailingZeros);
         default:
            return this.formatCompact(amount, removeTrailingZeros);
      }
   }

   private GUIHelper.FormatType getFormatType(String currencyId) {
      if (currencyId != null && !currencyId.isEmpty()) {
         String perCurrencyType = this.plugin.getConfig().getString("number-format.per-currency." + currencyId);
         if (perCurrencyType != null && !perCurrencyType.isEmpty()) {
            try {
               return GUIHelper.FormatType.valueOf(perCurrencyType.toUpperCase());
            } catch (IllegalArgumentException var5) {
            }
         }
      }

      String globalType = this.plugin.getConfig().getString("number-format.type", "COMPACT");

      try {
         return GUIHelper.FormatType.valueOf(globalType.toUpperCase());
      } catch (IllegalArgumentException var4) {
         return GUIHelper.FormatType.COMPACT;
      }
   }

   private String formatCompact(double amount, boolean removeTrailingZeros) {
      if (amount < 0.0) {
         return this.formatWithCommas(amount, removeTrailingZeros);
      } else {
         double tThreshold = this.plugin.getConfig().getDouble("number-format.compact.t-threshold", 1000000.0) * 1000000.0;
         double bThreshold = this.plugin.getConfig().getDouble("number-format.compact.b-threshold", 1000.0) * 1000000.0;
         double mThreshold = this.plugin.getConfig().getDouble("number-format.compact.m-threshold", 1.0) * 1000000.0;
         double kThreshold = this.plugin.getConfig().getDouble("number-format.compact.k-threshold", 10000.0);
         String result;
         if (amount >= tThreshold) {
            double value = amount / 1.0E12;
            if (removeTrailingZeros && value == Math.floor(value)) {
               result = String.format("%.0fT", value);
            } else {
               result = String.format("%.2fT", value);
               if (removeTrailingZeros) {
                  result = result.replaceAll("\\.?0+T$", "T");
               }
            }
         } else if (amount >= bThreshold) {
            double value = amount / 1.0E9;
            if (removeTrailingZeros && value == Math.floor(value)) {
               result = String.format("%.0fB", value);
            } else {
               result = String.format("%.2fB", value);
               if (removeTrailingZeros) {
                  result = result.replaceAll("\\.?0+B$", "B");
               }
            }
         } else if (amount >= mThreshold) {
            double value = amount / 1000000.0;
            if (removeTrailingZeros && value == Math.floor(value)) {
               result = String.format("%.0fM", value);
            } else {
               result = String.format("%.2fM", value);
               if (removeTrailingZeros) {
                  result = result.replaceAll("\\.?0+M$", "M");
               }
            }
         } else {
            if (!(amount >= kThreshold)) {
               return this.formatWithCommas(amount, removeTrailingZeros);
            }

            double value = amount / 1000.0;
            if (removeTrailingZeros && value == Math.floor(value)) {
               result = String.format("%.0fk", value);
            } else {
               result = String.format("%.2fk", value);
               if (removeTrailingZeros) {
                  result = result.replaceAll("\\.?0+k$", "k");
               }
            }
         }

         return result;
      }
   }

   private String formatWithCommas(double amount, boolean removeTrailingZeros) {
      if (removeTrailingZeros && amount == Math.floor(amount)) {
         return COMMA_FORMAT.format((long)amount);
      } else {
         DecimalFormat df = new DecimalFormat("#,###.##", DecimalFormatSymbols.getInstance(Locale.US));
         String result = df.format(amount);
         if (removeTrailingZeros) {
            result = result.replaceAll("\\.0+$", "").replaceAll("(\\.[0-9]*[1-9])0+$", "$1");
         }

         return result;
      }
   }

   private String formatFull(double amount, boolean removeTrailingZeros) {
      if (removeTrailingZeros && amount == Math.floor(amount)) {
         return String.format("%.0f", amount);
      } else {
         String result = String.format("%.2f", amount);
         if (removeTrailingZeros) {
            result = result.replaceAll("\\.0+$", "").replaceAll("(\\.[0-9]*[1-9])0+$", "$1");
         }

         return result;
      }
   }

   public static Inventory getTopInventorySafely(InventoryClickEvent event, Player player) {
      if (event == null) {
         return null;
      } else {
         try {
            Method getViewMethod = event.getClass().getMethod("getView");
            Object view = getViewMethod.invoke(event);
            if (view != null) {
               Method getTopInventoryMethod = view.getClass().getMethod("getTopInventory");
               Object topInventory = getTopInventoryMethod.invoke(view);
               if (topInventory instanceof Inventory) {
                  return (Inventory)topInventory;
               }
            }
         } catch (Throwable var7) {
         }

         if (player != null) {
            try {
               Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
               Object playerView = getOpenInventoryMethod.invoke(player);
               if (playerView != null) {
                  Method getTopInventoryMethod = playerView.getClass().getMethod("getTopInventory");
                  Object topInventory = getTopInventoryMethod.invoke(playerView);
                  if (topInventory instanceof Inventory) {
                     return (Inventory)topInventory;
                  }
               }
            } catch (Throwable var6) {
            }
         }

         return null;
      }
   }

   public static Inventory getTopInventorySafely(Player player) {
      if (player == null) {
         return null;
      } else {
         try {
            Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
            Object view = getOpenInventoryMethod.invoke(player);
            if (view != null) {
               Method getTopInventoryMethod = view.getClass().getMethod("getTopInventory");
               Object topInventory = getTopInventoryMethod.invoke(view);
               if (topInventory instanceof Inventory) {
                  return (Inventory)topInventory;
               }
            }
         } catch (Throwable var5) {
         }

         return null;
      }
   }

   public static void setCursorSafely(Player player, ItemStack item) {
      if (player != null) {
         try {
            player.setItemOnCursor(item);

            try {
               Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
               Object view = getOpenInventoryMethod.invoke(player);
               if (view != null) {
                  Method setCursorMethod = view.getClass().getMethod("setCursor", ItemStack.class);
                  setCursorMethod.invoke(view, item);
               }
            } catch (Throwable var5) {
            }
         } catch (Throwable var6) {
         }
      }
   }

   private boolean checkComponentSupport() {
      try {
         Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
         Method displayNameMethod = ItemMeta.class.getMethod("displayName", componentClass);
         if (displayNameMethod == null) {
            return false;
         } else {
            try {
               Material testMaterial = Material.STONE;
               ItemStack testItem = new ItemStack(testMaterial);
               ItemMeta testMeta = testItem.getItemMeta();
               if (testMeta == null) {
                  return false;
               } else {
                  Class<?> miniMessageClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
                  Method miniMessageMethod = miniMessageClass.getMethod("miniMessage");
                  Object miniMessage = miniMessageMethod.invoke(null);
                  Method deserializeMethod = miniMessage.getClass().getMethod("deserialize", String.class);
                  Object testComponent = deserializeMethod.invoke(miniMessage, "<red>Test");
                  displayNameMethod.invoke(testMeta, testComponent);
                  if (!testMeta.hasDisplayName()) {
                     this.plugin.getLogger().info("[GUIHelper] Component API test failed - display name not set. Using legacy mode.");
                     return false;
                  } else {
                     this.plugin.getLogger().info("[GUIHelper] Component API test passed. Using Component mode.");
                     return true;
                  }
               }
            } catch (Throwable var11) {
               this.plugin.getLogger().info("[GUIHelper] Component API test failed: " + var11.getMessage() + ". Using legacy mode.");
               return false;
            }
         }
      } catch (Exception var12) {
         return false;
      }
   }

   public Inventory createInventory(InventoryHolder holder, int size, String title) {
      if (this.supportsComponent) {
         try {
            Component titleComponent = this.plugin.getAdventureHelper().parse(title);
            return this.createInventoryWithComponent(holder, size, titleComponent);
         } catch (Throwable var6) {
            String legacyTitle = this.plugin.getAdventureHelper().parseToLegacy(title);
            return this.createLegacyInventory(holder, size, legacyTitle);
         }
      } else {
         String legacyTitle = this.plugin.getAdventureHelper().parseToLegacy(title);
         return this.createLegacyInventory(holder, size, legacyTitle);
      }
   }

   public Inventory createInventory(InventoryHolder holder, int size, String title, Map<String, String> placeholders) {
      if (this.supportsComponent) {
         try {
            Component titleComponent = this.plugin.getAdventureHelper().parse(title, placeholders);
            return this.createInventoryWithComponent(holder, size, titleComponent);
         } catch (Throwable var7) {
            String legacyTitle = this.plugin.getAdventureHelper().parseToLegacy(title, placeholders);
            return this.createLegacyInventory(holder, size, legacyTitle);
         }
      } else {
         String legacyTitle = this.plugin.getAdventureHelper().parseToLegacy(title, placeholders);
         return this.createLegacyInventory(holder, size, legacyTitle);
      }
   }

   private Inventory createInventoryWithComponent(InventoryHolder holder, int size, Component title) {
      try {
         return Bukkit.createInventory(holder, size, title);
      } catch (Throwable var8) {
         try {
            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Method createInventoryMethod = Bukkit.class.getMethod("createInventory", InventoryHolder.class, int.class, componentClass);
            return (Inventory)createInventoryMethod.invoke(null, holder, size, title);
         } catch (Throwable var7) {
            String legacyTitle = this.plugin.getAdventureHelper().toLegacy(title);
            return this.createLegacyInventory(holder, size, legacyTitle);
         }
      }
   }

   private Inventory createLegacyInventory(InventoryHolder holder, int size, String title) {
      return Bukkit.createInventory(holder, size, title);
   }

   public void setDisplayName(ItemMeta meta, String text) {
      if (text != null && !text.isEmpty()) {
         String processedText = text.trim();
         if (!processedText.startsWith("&r") && !processedText.startsWith("<reset>")) {
            processedText = "&r" + processedText;
         }

         String legacyText = this.plugin.getAdventureHelper().parseToLegacy(processedText);
         this.setDisplayNameLegacy(meta, legacyText);
      }
   }

   private void setDisplayNameLegacy(ItemMeta meta, String displayName) {
      try {
         meta.setDisplayName(displayName);
      } catch (Throwable var6) {
         try {
            Method setDisplayNameMethod = meta.getClass().getMethod("setDisplayName", String.class);
            setDisplayNameMethod.invoke(meta, displayName);
         } catch (Throwable var5) {
            this.plugin.getLogger().warning("Failed to set display name: " + var5.getMessage());
         }
      }
   }

   public void setDisplayName(ItemMeta meta, String text, Map<String, String> placeholders, Player player) {
      if (text != null && !text.isEmpty()) {
         String processedText = text.trim();
         if (!processedText.startsWith("&r") && !processedText.startsWith("<reset>")) {
            processedText = "&r" + processedText;
         }

         String legacyText = this.plugin.getAdventureHelper().parseToLegacy(processedText, placeholders, player);
         this.setDisplayNameLegacy(meta, legacyText);
      }
   }

   public void setDisplayName(ItemMeta meta, String text, Map<String, String> placeholders) {
      this.setDisplayName(meta, text, placeholders, null);
   }

   public <T> List<T> createLore(String... texts) {
      List<String> lore = new ArrayList<>();

      for (String text : texts) {
         if (text != null && !text.isEmpty()) {
            String processedText = text.trim();
            if (!processedText.startsWith("&r")) {
               processedText = "&r" + processedText;
            }

            lore.add(this.plugin.getAdventureHelper().parseToLegacy(processedText));
         } else {
            lore.add("");
         }
      }

      return (List<T>)lore;
   }

   public <T> List<T> createLore(List<String> texts, Map<String, String> placeholders, Player player) {
      if (texts != null && !texts.isEmpty()) {
         List<String> lore = new ArrayList<>();

         for (String text : texts) {
            if (text != null && !text.trim().isEmpty()) {
               String processedText = text.trim();
               if (!processedText.startsWith("&r")) {
                  processedText = "&r" + processedText;
               }

               lore.add(this.plugin.getAdventureHelper().parseToLegacy(processedText, placeholders, player));
            }
         }

         return (List<T>)lore;
      } else {
         return new ArrayList<>();
      }
   }

   public <T> List<T> createLore(List<String> texts, Map<String, String> placeholders) {
      return this.createLore(texts, placeholders, null);
   }

   public <T> List<T> createLore(Map<String, String> placeholders, String... texts) {
      List<String> lore = new ArrayList<>();

      for (String text : texts) {
         if (text != null && !text.isEmpty()) {
            String processedText = text.trim();
            if (!processedText.startsWith("&r")) {
               processedText = "&r" + processedText;
            }

            lore.add(this.plugin.getAdventureHelper().parseToLegacy(processedText, placeholders));
         } else {
            lore.add("");
         }
      }

      return (List<T>)lore;
   }

   public void setLore(ItemMeta meta, List<?> lore) {
      if (lore != null && !lore.isEmpty()) {
         try {
            List<String> stringLore;
            if (!lore.isEmpty() && lore.get(0) instanceof Component) {
               stringLore = lore.stream().filter(c -> c != null).map(c -> this.plugin.getAdventureHelper().toLegacy((Component)c)).collect(Collectors.toList());
            } else {
               stringLore = (List<String>)lore;
            }

            List<String> filteredLore = stringLore.stream().filter(s -> s != null && !s.trim().isEmpty()).collect(Collectors.toList());
            if (!filteredLore.isEmpty()) {
               this.setLoreLegacy(meta, filteredLore);
            }
         } catch (Exception var5) {
            this.plugin.getLogger().warning("Failed to set lore: " + var5.getMessage());
         }
      }
   }

   private void setLoreLegacy(ItemMeta meta, List<String> lore) {
      try {
         meta.setLore(lore);
      } catch (Throwable var6) {
         try {
            Method setLoreMethod = meta.getClass().getMethod("setLore", List.class);
            setLoreMethod.invoke(meta, lore);
         } catch (Throwable var5) {
            this.plugin.getLogger().warning("Failed to set lore: " + var5.getMessage());
         }
      }
   }

   public <T> void addLoreLine(List<T> lore, String text) {
      if (text != null && !text.isEmpty()) {
         String processedText = text.trim();
         if (!processedText.startsWith("&r")) {
            processedText = "&r" + processedText;
         }

         lore.add((T)this.plugin.getAdventureHelper().parseToLegacy(processedText));
      } else {
         lore.add((T)"");
      }
   }

   public <T> void addLoreLine(List<T> lore, String text, Map<String, String> placeholders) {
      if (text != null && !text.isEmpty()) {
         String processedText = text.trim();
         if (!processedText.startsWith("&r")) {
            processedText = "&r" + processedText;
         }

         lore.add((T)this.plugin.getAdventureHelper().parseToLegacy(processedText, placeholders));
      } else {
         lore.add((T)"");
      }
   }

   public ItemStack createPlayerHead(Material material, Player player, String base64, boolean usePlayerSkin, String displayName, List<?> lore) {
      return this.createPlayerHead(material, player, base64, usePlayerSkin, displayName, lore, null, null);
   }

   public ItemStack createPlayerHead(
      Material material, Player player, String base64, boolean usePlayerSkin, String displayName, List<?> lore, Boolean glowing, Integer customModelData
   ) {
      UUID playerUuid = player != null && usePlayerSkin ? player.getUniqueId() : null;
      ItemStack cached = this.headCache.getCachedHead(playerUuid, base64, displayName, lore, glowing, customModelData);
      if (cached != null) {
         return cached;
      } else {
         Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
         if (playerHeadMaterial == null || !MaterialHelper.isPlayerHead(material)) {
            material = playerHeadMaterial;
         }

         if (material == null) {
            material = MaterialHelper.parseMaterial("PLAYER_HEAD", null);
            if (material == null) {
               this.plugin.getLogger().severe("Failed to parse PLAYER_HEAD material! Plugin may not work correctly.");
               Material barrierMaterial = MaterialHelper.getBarrierMaterial();
               return new ItemStack(barrierMaterial != null ? barrierMaterial : MaterialHelper.parseMaterial("BARRIER", null));
            }
         }

         ItemStack item = new ItemStack(material);
         SkullMeta meta = (SkullMeta)item.getItemMeta();
         if (usePlayerSkin && player != null) {
            LegacyCompatibility.setSkullOwner(meta, player);
         } else if (base64 != null && !base64.isEmpty()) {
            this.setBase64Texture(meta, base64);
         }

         if (displayName != null && !displayName.isEmpty()) {
            this.setDisplayName(meta, displayName);
         }

         if (lore != null && !lore.isEmpty()) {
            this.setLore(meta, lore);
         }

         if (glowing != null || customModelData != null) {
            this.applyItemProperties(meta, glowing, customModelData);
         }

         item.setItemMeta(meta);
         if (playerUuid == null) {
            this.headCache.cacheHead(playerUuid, base64, displayName, lore, glowing, customModelData, item);
         }

         return item;
      }
   }

   private void setBase64Texture(SkullMeta meta, String base64) {
      if (base64 != null && !base64.isEmpty()) {
         String textureValue = base64.trim();
         UUID uuid = UUID.randomUUID();

         try {
            if (this.trySetPaperTexture(meta, textureValue, uuid)) {
               return;
            }

            if (this.trySetSpigotTexture(meta, textureValue, uuid)) {
               return;
            }

            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "All texture methods failed, using default player head");
            }
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to set base64 texture for skull: " + var6.getMessage());
         }
      }
   }

   private boolean trySetPaperTexture(SkullMeta meta, String textureValue, UUID uuid) {
      try {
         Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
         Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
         PlayerProfile profile = Bukkit.createProfile(uuid, "CustomHead");
         ProfileProperty property = new ProfileProperty("textures", textureValue);
         profile.setProperty(property);
         meta.setPlayerProfile(profile);
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Successfully set base64 texture using Paper ProfileProperty API (direct)");
         }

         return true;
      } catch (ClassNotFoundException var6) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Paper ProfileProperty API not available: " + var6.getMessage());
         }
      } catch (NoClassDefFoundError | NoSuchMethodError var7) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Paper ProfileProperty API incompatible: " + var7.getMessage());
         }
      } catch (Exception var8) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Paper ProfileProperty API failed: " + var8.getMessage());
         }
      }

      return false;
   }

   private boolean trySetSpigotTexture(SkullMeta meta, String textureValue, UUID uuid) {
      try {
         String jsonString;
         try {
            jsonString = new String(Base64.getDecoder().decode(textureValue), StandardCharsets.UTF_8);
         } catch (IllegalArgumentException var11) {
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Invalid base64 texture format: " + var11.getMessage());
            }

            return false;
         }

         JsonObject json = new JsonParser().parse(jsonString).getAsJsonObject();
         String textureUrl = null;
         if (json.has("textures")) {
            JsonObject textures = json.getAsJsonObject("textures");
            if (textures.has("SKIN")) {
               JsonObject skin = textures.getAsJsonObject("SKIN");
               if (skin.has("url")) {
                  textureUrl = skin.get("url").getAsString();
               }
            }
         }

         if (textureUrl != null && !textureUrl.isEmpty()) {
            Class.forName("org.bukkit.profile.PlayerProfile");
            Class.forName("org.bukkit.profile.PlayerTextures");
            org.bukkit.profile.PlayerProfile profile = Bukkit.createPlayerProfile(uuid, "CustomHead");
            PlayerTextures textures = profile.getTextures();
            URI uri = new URI(textureUrl);
            URL url = uri.toURL();
            textures.setSkin(url);
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Successfully set base64 texture using Spigot PlayerProfile API (direct)");
            }

            return true;
         }

         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Could not extract URL from base64 JSON");
         }

         return false;
      } catch (ClassNotFoundException var12) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Spigot PlayerProfile API not available (likely 1.8-1.17): " + var12.getMessage());
         }
      } catch (NoClassDefFoundError | NoSuchMethodError var13) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Spigot PlayerProfile API incompatible: " + var13.getMessage());
         }
      } catch (Exception var14) {
         if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI, "Spigot PlayerProfile API failed: " + var14.getMessage());
         }
      }

      return false;
   }

   public void applyGlowing(ItemMeta meta, boolean glowing) {
      LegacyCompatibility.setGlowing(meta, glowing);
   }

   public void applyCustomModelData(ItemMeta meta, Integer customModelData) {
      if (customModelData != null && customModelData > 0) {
         LegacyCompatibility.setCustomModelData(meta, customModelData);
      } else {
         LegacyCompatibility.removeCustomModelData(meta);
      }
   }

   public void applyItemProperties(ItemMeta meta, String configPath, FileConfiguration config) {
      if (config.contains(configPath + ".glowing")) {
         boolean glowing = config.getBoolean(configPath + ".glowing", false);
         this.applyGlowing(meta, glowing);
      }

      if (config.contains(configPath + ".custom-model-data")) {
         Integer customModelData = config.getInt(configPath + ".custom-model-data", 0);
         if (customModelData > 0) {
            this.applyCustomModelData(meta, customModelData);
         }
      }
   }

   public void applyItemProperties(ItemMeta meta, Boolean glowing, Integer customModelData) {
      if (glowing != null) {
         this.applyGlowing(meta, glowing);
      }

      if (customModelData != null && customModelData > 0) {
         this.applyCustomModelData(meta, customModelData);
      }
   }

   public List<Integer> parseSlotList(FileConfiguration config, String path) {
      List<Integer> slots = new ArrayList<>();
      List<String> stringList = config.getStringList(path);
      if (!stringList.isEmpty()) {
         for (String slotStr : stringList) {
            this.parseSlotString(slotStr, slots, path);
         }
      } else {
         List<?> objectList = config.getList(path);
         if (objectList != null && !objectList.isEmpty()) {
            for (Object obj : objectList) {
               if (obj instanceof String) {
                  this.parseSlotString((String)obj, slots, path);
               } else if (obj instanceof Integer) {
                  slots.add((Integer)obj);
               } else if (obj instanceof Number) {
                  slots.add(((Number)obj).intValue());
               } else {
                  this.parseSlotString(String.valueOf(obj), slots, path);
               }
            }
         } else {
            List<Integer> intList = config.getIntegerList(path);
            slots.addAll(intList);
         }
      }

      return slots;
   }

   private void parseSlotString(String slotStr, List<Integer> slots, String path) {
      if (slotStr != null && !slotStr.trim().isEmpty()) {
         slotStr = slotStr.trim();
         if (slotStr.contains("-") && !slotStr.startsWith("-")) {
            String[] parts = slotStr.split("-", 2);
            if (parts.length == 2) {
               try {
                  int start = Integer.parseInt(parts[0].trim());
                  int end = Integer.parseInt(parts[1].trim());
                  if (start <= end) {
                     for (int i = start; i <= end; i++) {
                        slots.add(i);
                     }
                  } else {
                     for (int i = start; i >= end; i--) {
                        slots.add(i);
                     }
                  }
               } catch (NumberFormatException var9) {
                  this.plugin.getLogger().warning("Invalid slot range format: " + slotStr + " at path " + path);
               }
            } else {
               this.plugin.getLogger().warning("Invalid slot range format: " + slotStr + " at path " + path);
            }
         } else {
            try {
               int slot = Integer.parseInt(slotStr);
               slots.add(slot);
            } catch (NumberFormatException var8) {
               this.plugin.getLogger().warning("Invalid slot number: " + slotStr + " at path " + path);
            }
         }
      }
   }

   public static enum FormatType {
      COMPACT,
      COMMAS,
      FULL;
   }
}
