package com.kstudio.ultracoinflip.gui.impl;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.currency.CurrencySettings;
import com.kstudio.ultracoinflip.data.CoinFlipGame;
import com.kstudio.ultracoinflip.data.CoinFlipLog;
import com.kstudio.ultracoinflip.data.HouseCoinFlipManager;
import com.kstudio.ultracoinflip.data.PlayerStats;
import com.kstudio.ultracoinflip.gui.InventoryButton;
import com.kstudio.ultracoinflip.gui.InventoryGUI;
import com.kstudio.ultracoinflip.util.DebugManager;
import com.kstudio.ultracoinflip.util.FoliaScheduler;
import com.kstudio.ultracoinflip.util.LegacyCompatibility;
import com.kstudio.ultracoinflip.util.MaterialHelper;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class CoinFlipRollGUI extends InventoryGUI {
   private final KStudio plugin;
   private final Player player1;
   private final Player player2;
   private final double amount;
   private final CoinFlipGame.CurrencyType currencyType;
   private final String currencyId;
   private ItemStack[] animationItems;
   private List<ItemStack> animationPool;
   private List<ItemStack> scheduledRollItems;
   private Player winner;
   private Player loser;
   private final boolean isBotGame;
   private final String botName;
   private final String botTexture;
   private final SecureRandom secureRandom = new SecureRandom();
   private boolean animationRunning = false;
   private volatile Object currentAnimationTaskId = null;
   private ItemStack cachedWinnerHead = null;
   private ItemStack cachedLoserHead = null;
   private boolean finished = false;
   private int winnerInsertTick;
   private int resultSlotIndex;
   private final UUID player1UUID;
   private final UUID player2UUID;
   private final Set<UUID> playersClosedGUI = new HashSet<>();
   private int cachedAnimationSlotCount;
   private int cachedAnimationStartSlot;
   private int cachedResultSlot;
   private int cachedAnimationSpeed;
   private int cachedAnimationDuration;
   private int cachedInventorySize;
   private boolean cachedAudioDuringRollEnabled;
   private String cachedAudioDuringRollSound;
   private float cachedAudioDuringRollVolume;
   private float cachedAudioDuringRollPitch;
   private boolean cachedAudioOnCompleteEnabled;
   private String cachedAudioOnCompleteSound;
   private float cachedAudioOnCompleteVolume;
   private float cachedAudioOnCompletePitch;
   private String cachedBorderAnimationMode;
   private int cachedBorderAnimationSpeed;
   private List<Integer> cachedBorderSlots;
   private ItemStack[] rainbowGlassItems;
   private ItemStack[] customColorPaletteItems;
   private String cachedBorderWaveDirection;
   private int cachedBorderWaveSpeed;
   private double cachedBorderPulseMinBrightness;
   private double cachedBorderPulseMaxBrightness;
   private int cachedBorderPulseCycleDuration;
   private String cachedBorderGradientDirection;
   private int cachedBorderGradientSmoothness;
   private int cachedBorderFadeInDuration;
   private int cachedBorderFadeOutDuration;
   private int cachedBorderFadeHoldDuration;
   private int cachedBorderRandomChangeFrequency;
   private String cachedBorderSequenceDirection;
   private int cachedBorderSequenceDelay;
   private String cachedRollingAnimationType;
   private double cachedBounceIntensity;
   private int cachedBounceFrequency;
   private int cachedFadeInSlots;
   private int cachedFadeOutSlots;
   private double cachedFadeMinOpacity;
   private int cachedSpinRotationSpeed;
   private String cachedSpinRotationAxis;
   private double cachedScaleMinScale;
   private double cachedScaleMaxScale;
   private int cachedScaleSpeed;
   private double cachedGlowIntensity;
   private int cachedGlowSpeed;
   private String cachedGlowColor;
   private List<String> cachedComboEffects;
   private double cachedComboIntensity;
   private int cachedSlotMachineCenterSlot;
   private int cachedSlotMachineChangeSpeed;
   private double cachedSlotMachineChangeIntensity;
   private boolean cachedSlotMachineGlowOnChange;
   private boolean cachedSlotMachineChangeSound;
   private int cachedCircularCenterSlot;
   private int cachedCircularRadius;
   private int cachedCircularRotationSpeed;
   private boolean cachedCircularClockwise;
   private boolean cachedCircularShowCenter;
   private boolean cachedCircularOuterGlow;
   private int cachedCircularRotationSmoothness;
   private List<Integer> cachedCircularPlayerSlots;
   private int cachedVerticalStartRow;
   private int cachedVerticalColumn;
   private int cachedVerticalRowsToAnimate;
   private String cachedVerticalDirection;
   private boolean cachedVerticalBounceOnEdges;
   private List<Integer> cachedVerticalPlayerSlots;
   private int cachedVerticalArrowSlot;
   private int cachedDefaultArrowSlot;
   private String cachedBorderDisplayName;
   private String cachedBorderMaterialName;
   private Set<Integer> cachedCircularArrowSlots;
   private boolean cachedTitlesEnabled;
   private boolean cachedActionbarEnabled;
   private boolean cachedBossbarEnabled;
   private String cachedWinnerBossbarColor;
   private String cachedWinnerBossbarOverlay;
   private float cachedWinnerBossbarProgress;
   private int cachedBossbarDuration;
   private String cachedLoserBossbarColor;
   private String cachedLoserBossbarOverlay;
   private float cachedLoserBossbarProgress;

   @Override
   protected KStudio getPlugin() {
      return this.plugin;
   }

   @Override
   protected String getOpenSoundKey() {
      return "gui.open-roll";
   }

   public CoinFlipRollGUI(KStudio plugin, Player player1, Player player2, double amount,
         CoinFlipGame.CurrencyType currencyType) {
      this(plugin, player1, player2, amount, currencyType, null);
   }

   public CoinFlipRollGUI(KStudio plugin, Player player1, Player player2, double amount,
         CoinFlipGame.CurrencyType currencyType, String currencyId) {
      this.plugin = plugin;
      this.player1 = player1;
      this.player2 = player2;
      this.amount = amount;
      this.currencyType = currencyType;
      this.currencyId = currencyId;
      this.isBotGame = false;
      this.botName = null;
      this.botTexture = null;
      this.player1UUID = player1 != null ? player1.getUniqueId() : null;
      this.player2UUID = player2 != null ? player2.getUniqueId() : null;
      this.cacheNotificationConfigValues();
   }

   public CoinFlipRollGUI(KStudio plugin, Player player, double amount, CoinFlipGame.CurrencyType currencyType,
         String currencyId, boolean isBotGame) {
      this.plugin = plugin;
      this.player1 = player;
      this.player2 = null;
      this.amount = amount;
      this.currencyType = currencyType;
      this.currencyId = currencyId;
      this.isBotGame = isBotGame;
      this.botName = plugin.getConfig().getString("house.name",
            plugin.getGUIConfig().getString("coinflip-gui.players.bot.name", "YourServer"));
      this.botTexture = plugin.getConfig()
            .getString(
                  "house.display.texture",
                  plugin.getGUIConfig()
                        .getString(
                              "coinflip-gui.players.bot.texture",
                              "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ4ZDU1YjMzZWQ2ZmViNjE0ZTJjYTVkNGY1MGJiMzdmMTYxYWRhMzU4MmZjZmM2ZTQwMjg4YzZmYjA2ZjFmIn19fQ=="));
      this.player1UUID = player != null ? player.getUniqueId() : null;
      this.player2UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
      this.cacheNotificationConfigValues();
   }

   private void cacheNotificationConfigValues() {
      this.cachedTitlesEnabled = this.plugin.getConfig().getBoolean("titles.enabled", true);
      this.cachedActionbarEnabled = this.plugin.getConfig().getBoolean("actionbar.enabled", false);
      this.cachedBossbarEnabled = this.plugin.getConfig().getBoolean("bossbar.enabled", false);
      this.cachedWinnerBossbarColor = this.plugin.getConfig().getString("bossbar.color", "GREEN");
      this.cachedWinnerBossbarOverlay = this.plugin.getConfig().getString("bossbar.overlay", "PROGRESS");
      this.cachedWinnerBossbarProgress = (float) this.plugin.getConfig().getDouble("bossbar.progress", 1.0);
      this.cachedBossbarDuration = this.plugin.getConfig().getInt("bossbar.duration", 5);
      this.cachedLoserBossbarColor = this.plugin.getConfig().getString("bossbar.color-lose", "RED");
      this.cachedLoserBossbarOverlay = this.plugin.getConfig().getString("bossbar.overlay-lose", "PROGRESS");
      this.cachedLoserBossbarProgress = (float) this.plugin.getConfig().getDouble("bossbar.progress-lose", 0.0);
   }

   private void cacheConfigValues() {
      this.cachedAnimationSlotCount = this.plugin.getGUIConfig().getInt("coinflip-gui.rolling.timing.slot-amount", 7);
      this.cachedAnimationStartSlot = this.plugin.getGUIConfig().getInt("coinflip-gui.rolling.timing.first-slot", 10);
      this.cachedResultSlot = this.plugin.getGUIConfig().getInt("coinflip-gui.rolling.timing.result-slot", 13);
      this.cachedAnimationSpeed = this.plugin.getGUIConfig().getInt("coinflip-gui.rolling.timing.frame-speed", 3);
      this.cachedAnimationDuration = this.plugin.getGUIConfig().getInt("coinflip-gui.rolling.timing.duration", 60);
      this.cachedInventorySize = this.getInventory().getSize();
      this.cachedBorderAnimationMode = this.plugin.getGUIConfig()
            .getString("coinflip-gui.layout.border-item.animation-mode", "rainbow");
      this.cachedBorderAnimationSpeed = this.plugin.getGUIConfig()
            .getInt("coinflip-gui.layout.border-item.animation-speed", 3);
      List<Integer> borderSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
            "coinflip-gui.layout.border-item.positions");
      this.cachedBorderSlots = borderSlotsList.isEmpty()
            ? Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26)
            : borderSlotsList;
      this.rainbowGlassItems = MaterialHelper.getRainbowGlassItems();
      List<String> colorPaletteList = this.plugin.getGUIConfig()
            .getStringList("coinflip-gui.layout.border-item.animation-settings.color-palette");
      if (colorPaletteList != null && !colorPaletteList.isEmpty()) {
         List<ItemStack> customPalette = new ArrayList<>();

         for (String materialName : colorPaletteList) {
            ItemStack item = MaterialHelper.createItemStack(materialName);
            if (item != null) {
               customPalette.add(item);
            }
         }

         if (!customPalette.isEmpty()) {
            this.customColorPaletteItems = customPalette.toArray(new ItemStack[0]);
         } else {
            this.customColorPaletteItems = null;
         }
      } else {
         this.customColorPaletteItems = null;
      }

      String borderSettingsPath = "coinflip-gui.layout.border-item.animation-settings.";
      this.cachedBorderWaveDirection = this.plugin.getGUIConfig().getString(borderSettingsPath + "wave.direction",
            "left-to-right");
      this.cachedBorderWaveSpeed = this.plugin.getGUIConfig().getInt(borderSettingsPath + "wave.speed", 2);
      this.cachedBorderPulseMinBrightness = this.plugin.getGUIConfig()
            .getDouble(borderSettingsPath + "pulse.min-brightness", 0.3);
      this.cachedBorderPulseMaxBrightness = this.plugin.getGUIConfig()
            .getDouble(borderSettingsPath + "pulse.max-brightness", 1.0);
      this.cachedBorderPulseCycleDuration = this.plugin.getGUIConfig()
            .getInt(borderSettingsPath + "pulse.cycle-duration", 20);
      this.cachedBorderGradientDirection = this.plugin.getGUIConfig()
            .getString(borderSettingsPath + "gradient.direction", "left-to-right");
      this.cachedBorderGradientSmoothness = this.plugin.getGUIConfig()
            .getInt(borderSettingsPath + "gradient.smoothness", 5);
      this.cachedBorderFadeInDuration = this.plugin.getGUIConfig().getInt(borderSettingsPath + "fade.fade-in-duration",
            10);
      this.cachedBorderFadeOutDuration = this.plugin.getGUIConfig()
            .getInt(borderSettingsPath + "fade.fade-out-duration", 10);
      this.cachedBorderFadeHoldDuration = this.plugin.getGUIConfig().getInt(borderSettingsPath + "fade.hold-duration",
            20);
      this.cachedBorderRandomChangeFrequency = this.plugin.getGUIConfig()
            .getInt(borderSettingsPath + "random.change-frequency", 5);
      this.cachedBorderSequenceDirection = this.plugin.getGUIConfig()
            .getString(borderSettingsPath + "sequence.direction", "left-to-right");
      this.cachedBorderSequenceDelay = this.plugin.getGUIConfig()
            .getInt(borderSettingsPath + "sequence.delay-between-items", 2);
      this.cachedRollingAnimationType = this.plugin.getGUIConfig().getString("coinflip-gui.rolling.animation-type",
            "default");
      String rollingSettingsPath = "coinflip-gui.rolling.animation-settings.";
      this.cachedBounceIntensity = this.plugin.getGUIConfig().getDouble(rollingSettingsPath + "bounce.intensity", 0.5);
      this.cachedBounceFrequency = this.plugin.getGUIConfig().getInt(rollingSettingsPath + "bounce.frequency", 3);
      this.cachedFadeInSlots = this.plugin.getGUIConfig().getInt(rollingSettingsPath + "fade.fade-in-slots", 2);
      this.cachedFadeOutSlots = this.plugin.getGUIConfig().getInt(rollingSettingsPath + "fade.fade-out-slots", 2);
      this.cachedFadeMinOpacity = this.plugin.getGUIConfig().getDouble(rollingSettingsPath + "fade.min-opacity", 0.3);
      this.cachedSpinRotationSpeed = this.plugin.getGUIConfig().getInt(rollingSettingsPath + "spin.rotation-speed", 2);
      this.cachedSpinRotationAxis = this.plugin.getGUIConfig().getString(rollingSettingsPath + "spin.rotation-axis",
            "y");
      this.cachedScaleMinScale = this.plugin.getGUIConfig().getDouble(rollingSettingsPath + "scale.min-scale", 0.8);
      this.cachedScaleMaxScale = this.plugin.getGUIConfig().getDouble(rollingSettingsPath + "scale.max-scale", 1.2);
      this.cachedScaleSpeed = this.plugin.getGUIConfig().getInt(rollingSettingsPath + "scale.scale-speed", 3);
      this.cachedGlowIntensity = this.plugin.getGUIConfig().getDouble(rollingSettingsPath + "glow.glow-intensity", 0.5);
      this.cachedGlowSpeed = this.plugin.getGUIConfig().getInt(rollingSettingsPath + "glow.glow-speed", 2);
      this.cachedGlowColor = this.plugin.getGUIConfig().getString(rollingSettingsPath + "glow.glow-color", "YELLOW");
      this.cachedComboEffects = this.plugin.getGUIConfig().getStringList(rollingSettingsPath + "combo.effects");
      if (this.cachedComboEffects == null || this.cachedComboEffects.isEmpty()) {
         this.cachedComboEffects = Arrays.asList("bounce", "glow");
      }

      this.cachedComboIntensity = this.plugin.getGUIConfig().getDouble(rollingSettingsPath + "combo.intensity", 0.5);
      String timingPath = "coinflip-gui.rolling.timing.";
      String slotMachinePlayerSlotPath = "coinflip-gui.layout.player-slots.slot-machine.player-slot";
      if (this.plugin.getGUIConfig().contains(slotMachinePlayerSlotPath)) {
         this.cachedSlotMachineCenterSlot = this.plugin.getGUIConfig().getInt(slotMachinePlayerSlotPath, 13);
      } else {
         this.cachedSlotMachineCenterSlot = this.plugin.getGUIConfig().getInt(timingPath + "slot-machine.center-slot",
               13);
      }

      this.cachedSlotMachineChangeSpeed = this.plugin.getGUIConfig().getInt(timingPath + "slot-machine.change-speed",
            1);
      this.cachedSlotMachineChangeIntensity = this.plugin.getGUIConfig()
            .getDouble(rollingSettingsPath + "slot-machine.change-intensity", 0.8);
      this.cachedSlotMachineGlowOnChange = this.plugin.getGUIConfig()
            .getBoolean(rollingSettingsPath + "slot-machine.glow-on-change", true);
      this.cachedSlotMachineChangeSound = this.plugin.getGUIConfig()
            .getBoolean(rollingSettingsPath + "slot-machine.change-sound", true);
      this.cachedCircularCenterSlot = this.plugin.getGUIConfig().getInt(timingPath + "circular.center-slot", 22);
      this.cachedCircularRadius = this.plugin.getGUIConfig().getInt(timingPath + "circular.radius", 3);
      this.cachedCircularRotationSpeed = this.plugin.getGUIConfig().getInt(timingPath + "circular.rotation-speed", 2);
      this.cachedCircularClockwise = this.plugin.getGUIConfig().getBoolean(timingPath + "circular.clockwise", true);
      this.cachedCircularShowCenter = this.plugin.getGUIConfig()
            .getBoolean(rollingSettingsPath + "circular.show-center", true);
      this.cachedCircularOuterGlow = this.plugin.getGUIConfig().getBoolean(rollingSettingsPath + "circular.outer-glow",
            false);
      this.cachedCircularRotationSmoothness = this.plugin.getGUIConfig()
            .getInt(rollingSettingsPath + "circular.rotation-smoothness", 5);
      String circularPlayerSlotsPath = "coinflip-gui.layout.player-slots.circular.player-slots";
      List<Integer> circularPlayerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
            circularPlayerSlotsPath);
      if (circularPlayerSlotsList.isEmpty()) {
         this.cachedCircularPlayerSlots = Arrays.asList(3, 4, 5, 11, 15, 20, 24, 33, 39, 40, 41, 29);
      } else {
         this.cachedCircularPlayerSlots = circularPlayerSlotsList;
      }

      this.cachedVerticalStartRow = this.plugin.getGUIConfig().getInt(timingPath + "vertical.start-row", 1);
      this.cachedVerticalColumn = this.plugin.getGUIConfig().getInt(timingPath + "vertical.column", 4);
      this.cachedVerticalRowsToAnimate = this.plugin.getGUIConfig().getInt(timingPath + "vertical.rows-to-animate", 3);
      this.cachedVerticalDirection = this.plugin.getGUIConfig().getString(rollingSettingsPath + "vertical.direction",
            "top-to-bottom");
      this.cachedVerticalBounceOnEdges = this.plugin.getGUIConfig()
            .getBoolean(rollingSettingsPath + "vertical.bounce-on-edges", false);
      String verticalPlayerSlotsPath = "coinflip-gui.layout.player-slots.vertical.player-slots";
      List<Integer> verticalPlayerSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
            verticalPlayerSlotsPath);
      if (verticalPlayerSlotsList.isEmpty()) {
         this.cachedVerticalPlayerSlots = Arrays.asList(4, 13, 22, 31, 40);
      } else {
         this.cachedVerticalPlayerSlots = verticalPlayerSlotsList;
      }

      this.cachedVerticalArrowSlot = this.plugin.getGUIConfig()
            .getInt("coinflip-gui.layout.player-slots.vertical.arrow-slot", 21);
      FileConfiguration soundsConfig = this.plugin.getSoundsConfig();
      this.cachedAudioDuringRollEnabled = soundsConfig.getBoolean("animation.during-roll.enabled", true);
      this.cachedAudioDuringRollSound = soundsConfig.getString("animation.during-roll.sound",
            "BLOCK_WOODEN_BUTTON_CLICK_ON");
      this.cachedAudioDuringRollVolume = (float) soundsConfig.getDouble("animation.during-roll.volume", 1.0);
      this.cachedAudioDuringRollPitch = (float) soundsConfig.getDouble("animation.during-roll.pitch", 1.0);
      this.cachedAudioDuringRollVolume = Math.max(0.0F, Math.min(1.0F, this.cachedAudioDuringRollVolume));
      this.cachedAudioDuringRollPitch = Math.max(0.5F, Math.min(2.0F, this.cachedAudioDuringRollPitch));
      this.cachedAudioOnCompleteEnabled = soundsConfig.getBoolean("animation.on-complete.enabled", true);
      this.cachedAudioOnCompleteSound = soundsConfig.getString("animation.on-complete.sound", "ENTITY_PLAYER_LEVELUP");
      this.cachedAudioOnCompleteVolume = (float) soundsConfig.getDouble("animation.on-complete.volume", 1.0);
      this.cachedAudioOnCompletePitch = (float) soundsConfig.getDouble("animation.on-complete.pitch", 1.0);
      this.cachedAudioOnCompleteVolume = Math.max(0.0F, Math.min(1.0F, this.cachedAudioOnCompleteVolume));
      this.cachedAudioOnCompletePitch = Math.max(0.5F, Math.min(2.0F, this.cachedAudioOnCompletePitch));
      this.cachedDefaultArrowSlot = this.plugin.getGUIConfig()
            .getInt("coinflip-gui.layout.player-slots.default.arrow-slot", 4);
      this.cachedBorderDisplayName = this.plugin.getGUIConfig().getString("coinflip-gui.layout.border-item.name", " ");
      this.cachedBorderMaterialName = this.plugin.getGUIConfig().getString("coinflip-gui.layout.border-item.material",
            "BLACK_STAINED_GLASS_PANE");
      this.cachedCircularArrowSlots = new HashSet<>();
      String arrowSlotsPath = "coinflip-gui.layout.player-slots.circular.arrow-slots";
      if (this.plugin.getGUIConfig().contains(arrowSlotsPath)) {
         List<?> arrowSlotsList = this.plugin.getGUIConfig().getList(arrowSlotsPath);
         if (arrowSlotsList != null) {
            for (Object arrowSlotObj : arrowSlotsList) {
               if (arrowSlotObj instanceof Map) {
                  Map<String, Object> arrowSlotMap = (Map<String, Object>) arrowSlotObj;
                  Object slotObj = arrowSlotMap.get("slot");
                  if (slotObj instanceof Number) {
                     this.cachedCircularArrowSlots.add(((Number) slotObj).intValue());
                  }
               }
            }
         }
      }
   }

   @Override
   protected Inventory createInventory() {
      String titleTemplate = this.plugin.getGUIConfig().getString("coinflip-gui.title", "&l&6FLIPPING COIN...");
      int size = this.plugin.getGUIConfig().getInt("coinflip-gui.size", 27);
      String animationType = this.plugin.getGUIConfig().getString("coinflip-gui.rolling.animation-type", "default");
      if (animationType != null) {
         animationType = animationType.toLowerCase();
         if ("circular".equals(animationType)) {
            size = 45;
         } else if ("vertical".equals(animationType)) {
            size = 45;
         }
      }

      return this.plugin.getGuiHelper().createInventory(null, size, titleTemplate);
   }

   public void startAnimation() {
      boolean player1Wins = this.secureRandom.nextBoolean();
      if (this.isBotGame) {
         this.winner = player1Wins ? this.player1 : null;
         this.loser = player1Wins ? null : this.player1;
      } else {
         this.winner = player1Wins ? this.player1 : this.player2;
         this.loser = player1Wins ? this.player2 : this.player1;
         this.plugin
               .getCoinFlipManager()
               .registerRollingGame(this.player1.getUniqueId(), this.player2.getUniqueId(), this.amount,
                     this.currencyType, this.currencyId);
      }

      this.cacheConfigValues();
      this.cachedWinnerHead = this.createPlayerItem("winner", this.winner);
      this.cachedLoserHead = this.createPlayerItem("loser", this.loser);
      this.setupPlayerHeads();
      this.setupBorders();
      String currentAnimationType = this.cachedRollingAnimationType != null
            ? this.cachedRollingAnimationType.toLowerCase()
            : "default";
      if ("vertical".equals(currentAnimationType)) {
         this.setupVerticalArrowSlot();
      }

      this.setupAnimationPool();
      this.prepareAnimationSchedule();
      String animationType = this.cachedRollingAnimationType != null ? this.cachedRollingAnimationType.toLowerCase()
            : "default";
      int animationSlotCount = this.cachedAnimationSlotCount;
      int animationStartSlot = this.cachedAnimationStartSlot;
      int inventorySize = this.cachedInventorySize;
      if ("slot-machine".equals(animationType)) {
         animationSlotCount = 1;
      } else if ("circular".equals(animationType)) {
         int radius = this.cachedCircularRadius;
         animationSlotCount = Math.min(8, radius * 8);
         if (animationSlotCount == 0) {
            animationSlotCount = 8;
         }
      } else if ("vertical".equals(animationType)) {
         if (this.cachedVerticalPlayerSlots != null && !this.cachedVerticalPlayerSlots.isEmpty()) {
            animationSlotCount = this.cachedVerticalPlayerSlots.size();
         } else {
            animationSlotCount = this.cachedVerticalRowsToAnimate;
         }
      }

      if ("slot-machine".equals(animationType)) {
         int centerSlot = this.cachedSlotMachineCenterSlot;
         if (centerSlot < 0 || centerSlot >= inventorySize) {
            this.plugin.getLogger().warning("Invalid slot-machine center slot: " + centerSlot + ". Using default: 13");
            this.cachedSlotMachineCenterSlot = Math.max(0, Math.min(13, inventorySize - 1));
         }
      } else if ("circular".equals(animationType)) {
         int centerSlot = this.cachedCircularCenterSlot;
         if (centerSlot < 0 || centerSlot >= inventorySize) {
            this.plugin.getLogger().warning("Invalid circular center slot: " + centerSlot + ". Using default: 13");
            this.cachedCircularCenterSlot = Math.max(0, Math.min(13, inventorySize - 1));
         }
      } else if ("vertical".equals(animationType)) {
         int column = this.cachedVerticalColumn;
         int startRow = this.cachedVerticalStartRow;
         if (column < 0 || column >= 9) {
            this.plugin.getLogger().warning("Invalid vertical column: " + column + ". Using default: 4");
            this.cachedVerticalColumn = 4;
         }

         if (startRow < 0 || startRow >= inventorySize / 9) {
            this.plugin.getLogger().warning("Invalid vertical start row: " + startRow + ". Using default: 1");
            this.cachedVerticalStartRow = 1;
         }

         if (startRow + animationSlotCount > inventorySize / 9) {
            animationSlotCount = Math.max(1, inventorySize / 9 - startRow);
            this.cachedVerticalRowsToAnimate = animationSlotCount;
         }
      } else {
         if (animationStartSlot < 0 || animationStartSlot >= inventorySize) {
            this.plugin
                  .getLogger()
                  .warning("Invalid animation start slot: " + animationStartSlot + " (inventory size: " + inventorySize
                        + "). Using default: 10");
            animationStartSlot = Math.max(0, Math.min(10, inventorySize - 1));
            this.cachedAnimationStartSlot = animationStartSlot;
         }

         if (animationStartSlot + animationSlotCount > inventorySize) {
            this.plugin.getLogger().warning("Animation slots exceed inventory size. Adjusting slot count...");
            animationSlotCount = Math.max(1, inventorySize - animationStartSlot);
            this.cachedAnimationSlotCount = animationSlotCount;
         }
      }

      if (animationSlotCount <= 0) {
         this.plugin
               .getLogger()
               .severe(
                     "[UltraCoinFlip] Invalid animation slot count: "
                           + animationSlotCount
                           + ". Cannot start animation. This is likely a configuration error in flipping.yml. Refunding players...");
         this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
      } else if (this.animationPool != null && !this.animationPool.isEmpty()) {
         if (!"slot-machine".equals(animationType) && !"circular".equals(animationType)
               && !"vertical".equals(animationType)) {
            this.animationItems = new ItemStack[animationSlotCount];
         }

         if (!"slot-machine".equals(animationType) && !"circular".equals(animationType)
               && !"vertical".equals(animationType)) {
            for (int i = 0; i < animationSlotCount; i++) {
               if (!this.animationPool.isEmpty()) {
                  ItemStack original = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size()));
                  if (original != null) {
                     this.animationItems[i] = original.clone();
                     int slot = animationStartSlot + i;
                     if (slot >= 0 && slot < inventorySize) {
                        this.getInventory().setItem(slot, this.animationItems[i]);
                     }
                  } else {
                     this.plugin.getLogger().warning("Animation pool contains null item at index. Skipping...");
                  }
               }
            }
         }

         this.setupProtectedSlots();
         if (this.isBotGame) {
            if (this.player1 == null || !this.player1.isOnline()) {
               this.plugin.getLogger().warning("Player disconnected before bot animation could start. Refunding...");
               this.plugin.getCoinFlipManager()
                     .refundRollingGame(this.player1 != null ? this.player1.getUniqueId() : null);
               return;
            }
         } else if (this.player1 == null || !this.player1.isOnline() || this.player2 == null
               || !this.player2.isOnline()) {
            this.plugin.getLogger()
                  .warning("One or both players disconnected before animation could start. Refunding...");
            this.plugin.getCoinFlipManager()
                  .refundRollingGame(this.player1 != null ? this.player1.getUniqueId() : this.player2.getUniqueId());
            return;
         }

         try {
            if (this.isBotGame) {
               this.player1.openInventory(this.getInventory());
            } else {
               this.player1.openInventory(this.getInventory());
               this.player2.openInventory(this.getInventory());
            }
         } catch (Exception var10) {
            this.plugin
                  .getLogger()
                  .severe(
                        "[UltraCoinFlip] Failed to open inventory for players. This may be caused by server issues or player disconnection. Refunding players...");
            this.plugin.getLogger().severe("[UltraCoinFlip] Error details: " + var10.getMessage());
            if (this.plugin.getDebugManager() != null
                  && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               var10.printStackTrace();
            }

            var10.printStackTrace();
            this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
            return;
         }

         this.plugin.getGuiManager().registerHandledInventory(this.getInventory(), this);
         this.animationRunning = true;
         this.playersClosedGUI.clear();
         if (this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-game-start-sound")) {
            this.plugin.getSoundHelper().playSound(this.player1, "game.start");
         }

         if (!this.isBotGame
               && this.player2 != null
               && this.player2.isOnline()
               && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player2,
                     "notification-game-start-sound")) {
            this.plugin.getSoundHelper().playSound(this.player2, "game.start");
         }

         this.animateRoll(0);
      } else {
         this.plugin
               .getLogger()
               .severe(
                     "[UltraCoinFlip] Animation pool is empty! Cannot start animation. This may be caused by missing animation items in flipping.yml. Refunding players...");
         this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
      }
   }

   private void setupBorders() {
      try {
         String borderAnimationMode = this.cachedBorderAnimationMode;
         String animationType = this.cachedRollingAnimationType != null ? this.cachedRollingAnimationType.toLowerCase()
               : "default";
         int arrowSlot = this.cachedDefaultArrowSlot;
         int slotMachinePlayerSlot = -1;
         Set<Integer> circularExcludedSlots = new HashSet<>();
         if ("slot-machine".equals(animationType)) {
            slotMachinePlayerSlot = this.cachedSlotMachineCenterSlot;
         }

         if ("circular".equals(animationType)) {
            if (this.cachedCircularPlayerSlots != null) {
               circularExcludedSlots.addAll(this.cachedCircularPlayerSlots);
            }

            if (this.cachedCircularArrowSlots != null && !this.cachedCircularArrowSlots.isEmpty()) {
               circularExcludedSlots.addAll(this.cachedCircularArrowSlots);
            }
         }

         List<Integer> borderSlotsList = this.plugin.getGuiHelper().parseSlotList(this.plugin.getGUIConfig(),
               "coinflip-gui.layout.border-item.positions");
         List<Integer> borderSlots = borderSlotsList.isEmpty()
               ? Arrays.asList(0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26)
               : borderSlotsList;
         int inventorySize = this.getInventory().getSize();
         if ("slot-machine".equals(animationType) && slotMachinePlayerSlot >= 0) {
            borderSlots = new ArrayList<>();

            for (int i = 0; i < inventorySize; i++) {
               if (i != slotMachinePlayerSlot) {
                  borderSlots.add(i);
               }
            }
         }

         if ("circular".equals(animationType)) {
            borderSlots = new ArrayList<>();

            for (int ix = 0; ix < inventorySize; ix++) {
               if (!circularExcludedSlots.contains(ix)) {
                  borderSlots.add(ix);
               }
            }
         }

         if ("vertical".equals(animationType)) {
            Set<Integer> verticalExcludedSlots = new HashSet<>();
            if (this.cachedVerticalPlayerSlots != null) {
               verticalExcludedSlots.addAll(this.cachedVerticalPlayerSlots);
            }

            verticalExcludedSlots.add(this.cachedVerticalArrowSlot);
            borderSlots = new ArrayList<>();

            for (int ixx = 0; ixx < inventorySize; ixx++) {
               if (!verticalExcludedSlots.contains(ixx)) {
                  borderSlots.add(ixx);
               }
            }
         }

         String borderDisplayName = this.cachedBorderDisplayName;
         if ("basic".equalsIgnoreCase(borderAnimationMode)) {
            String borderMaterialName = this.cachedBorderMaterialName;
            ItemStack border = MaterialHelper.createItemStack(borderMaterialName);
            if (border == null) {
               border = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
            }

            if (border == null) {
               border = new ItemStack(Material.GLASS_PANE);
               this.plugin.getLogger()
                     .warning("Invalid border material: " + borderMaterialName + ". Using default: GLASS_PANE");
            }

            ItemMeta borderMeta = border.getItemMeta();
            if (borderMeta != null) {
               this.plugin.getGuiHelper().setDisplayName(borderMeta, borderDisplayName);
               border.setItemMeta(borderMeta);
            }

            for (int slot : borderSlots) {
               if ((slot != arrowSlot || !"default".equals(animationType)) && slot != slotMachinePlayerSlot
                     && !circularExcludedSlots.contains(slot)) {
                  if (slot >= 0 && slot < inventorySize) {
                     this.getInventory().setItem(slot, border);
                  } else {
                     this.plugin.getLogger().warning(
                           "Invalid border slot: " + slot + " (inventory size: " + inventorySize + "). Skipping...");
                  }
               }
            }
         } else if (this.rainbowGlassItems != null && this.rainbowGlassItems.length > 0) {
            ItemStack borderx = this.rainbowGlassItems[0].clone();
            ItemMeta meta = borderx.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, borderDisplayName);
               borderx.setItemMeta(meta);
            }

            for (int slotx : borderSlots) {
               if (slotx != arrowSlot && slotx != slotMachinePlayerSlot && !circularExcludedSlots.contains(slotx)) {
                  if (slotx >= 0 && slotx < inventorySize) {
                     this.getInventory().setItem(slotx, borderx);
                  } else {
                     this.plugin.getLogger().warning(
                           "Invalid border slot: " + slotx + " (inventory size: " + inventorySize + "). Skipping...");
                  }
               }
            }
         } else {
            ItemStack borderx = this.createBorderItem(MaterialHelper.getBlackStainedGlassPane(), borderDisplayName);

            for (int slotxx : borderSlots) {
               if (slotxx != arrowSlot && slotxx != slotMachinePlayerSlot && !circularExcludedSlots.contains(slotxx)
                     && slotxx >= 0 && slotxx < inventorySize) {
                  this.getInventory().setItem(slotxx, borderx);
               }
            }
         }

         if ("slot-machine".equals(animationType) && slotMachinePlayerSlot >= 0) {
            String borderMaterialNamex = this.cachedBorderMaterialName;
            ItemStack borderx = MaterialHelper.createItemStack(borderMaterialNamex);
            if (borderx == null) {
               borderx = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
            }

            if (borderx == null) {
               borderx = new ItemStack(Material.GLASS_PANE);
            }

            ItemMeta borderMeta = borderx.getItemMeta();
            if (borderMeta != null) {
               this.plugin.getGuiHelper().setDisplayName(borderMeta, borderDisplayName);
               borderx.setItemMeta(borderMeta);
            }

            for (int slotxxx = 0; slotxxx < inventorySize; slotxxx++) {
               if (slotxxx != slotMachinePlayerSlot) {
                  ItemStack currentItem = this.getInventory().getItem(slotxxx);
                  if (currentItem == null || currentItem.getType() == Material.AIR) {
                     this.getInventory().setItem(slotxxx, borderx.clone());
                  }
               }
            }
         }
      } catch (Exception var15) {
         this.plugin.getLogger().severe("Failed to setup borders: " + var15.getMessage());
         var15.printStackTrace();
      }
   }

   private void setupProtectedSlots() {
      try {
         int inventorySize = this.cachedInventorySize;
         int animationStartSlot = this.cachedAnimationStartSlot;
         int animationSlotCount = this.cachedAnimationSlotCount;
         int resultSlotTemp = this.cachedResultSlot;
         int arrowSlot = this.plugin.getGUIConfig().getInt("coinflip-gui.layout.player-slots.arrow-slot", 4);
         if (resultSlotTemp < 0 || resultSlotTemp >= inventorySize) {
            resultSlotTemp = animationStartSlot + animationSlotCount / 2;
         }

         int resultSlot = resultSlotTemp;
         if (arrowSlot >= 0 && arrowSlot < inventorySize) {
            this.addButton(arrowSlot, new InventoryButton().creator(p -> {
               Inventory inv = this.getInventory();
               return inv != null && arrowSlot >= 0 && arrowSlot < inv.getSize() ? inv.getItem(arrowSlot) : null;
            }).consumer(event -> {
            }));
         }

         String animationType = this.cachedRollingAnimationType != null ? this.cachedRollingAnimationType.toLowerCase()
               : "default";
         if ("slot-machine".equals(animationType)) {
            int playerSlot = this.cachedSlotMachineCenterSlot;
            if (playerSlot >= 0 && playerSlot < inventorySize) {
               this.addButton(playerSlot, new InventoryButton().creator(p -> {
                  Inventory inv = this.getInventory();
                  return inv != null && playerSlot >= 0 && playerSlot < inv.getSize() ? inv.getItem(playerSlot) : null;
               }).consumer(event -> {
               }));
            }
         } else if ("circular".equals(animationType)) {
            if (this.cachedCircularPlayerSlots != null) {
               for (int slot : this.cachedCircularPlayerSlots) {
                  if (slot >= 0 && slot < inventorySize) {
                     this.addButton(slot, new InventoryButton().creator(p -> {
                        Inventory inv = this.getInventory();
                        return inv != null && slot >= 0 && slot < inv.getSize() ? inv.getItem(slot) : null;
                     }).consumer(event -> {
                     }));
                  }
               }
            }

            String arrowSlotsPath = "coinflip-gui.layout.player-slots.circular.arrow-slots";
            if (this.plugin.getGUIConfig().contains(arrowSlotsPath)) {
               List<?> arrowSlotsList = this.plugin.getGUIConfig().getList(arrowSlotsPath);
               if (arrowSlotsList != null) {
                  for (Object arrowSlotObj : arrowSlotsList) {
                     if (arrowSlotObj instanceof Map) {
                        Map<String, Object> arrowSlotMap = (Map<String, Object>) arrowSlotObj;
                        Object slotObj = arrowSlotMap.get("slot");
                        if (slotObj instanceof Number) {
                           int slotx = ((Number) slotObj).intValue();
                           if (slotx >= 0 && slotx < inventorySize) {
                              final int finalSlot = slotx;
                              this.addButton(slotx, new InventoryButton().creator(p -> {
                                 Inventory inv = this.getInventory();
                                 return inv != null && finalSlot >= 0 && finalSlot < inv.getSize() ? inv.getItem(finalSlot) : null;
                              }).consumer(event -> {
                              }));
                           }
                        }
                     }
                  }
               }
            }
         } else if (!"vertical".equals(animationType)) {
            for (int i = 0; i < animationSlotCount; i++) {
               int slotx = animationStartSlot + i;
               if (slotx >= 0 && slotx < inventorySize) {
                  final int finalSlot = slotx;
                  this.addButton(slotx, new InventoryButton().creator(p -> {
                     Inventory inv = this.getInventory();
                     return inv != null && finalSlot >= 0 && finalSlot < inv.getSize() ? inv.getItem(finalSlot) : null;
                  }).consumer(event -> {
                  }));
               }
            }
         } else {
            if (this.cachedVerticalPlayerSlots != null) {
               for (int slotx : this.cachedVerticalPlayerSlots) {
                  if (slotx >= 0 && slotx < inventorySize) {
                     final int finalSlot = slotx;
                     this.addButton(slotx, new InventoryButton().creator(p -> {
                        Inventory inv = this.getInventory();
                        return inv != null && finalSlot >= 0 && finalSlot < inv.getSize() ? inv.getItem(finalSlot) : null;
                     }).consumer(event -> {
                     }));
                  }
               }
            }

            int verticalArrowSlot = this.cachedVerticalArrowSlot;
            if (verticalArrowSlot >= 0 && verticalArrowSlot < inventorySize) {
               this.addButton(verticalArrowSlot, new InventoryButton().creator(p -> {
                  Inventory inv = this.getInventory();
                  return inv != null && verticalArrowSlot >= 0 && verticalArrowSlot < inv.getSize()
                        ? inv.getItem(verticalArrowSlot)
                        : null;
               }).consumer(event -> {
               }));
            }
         }

         Map<Integer, InventoryButton> buttonMap = this.getButtonMap();

         for (int slotxx = 0; slotxx < inventorySize; slotxx++) {
            if (buttonMap == null || !buttonMap.containsKey(slotxx)) {
               Inventory inv = this.getInventory();
               if (inv != null && slotxx >= 0 && slotxx < inv.getSize()) {
                  ItemStack item = inv.getItem(slotxx);
                  if (item != null && item.getType() != Material.AIR) {
                     int finalSlot = slotxx;
                     this.addButton(slotxx, new InventoryButton().creator(p -> {
                        Inventory inventory = this.getInventory();
                        return inventory != null && finalSlot >= 0 && finalSlot < inventory.getSize()
                              ? inventory.getItem(finalSlot)
                              : null;
                     }).consumer(event -> {
                     }));
                  }
               }
            }
         }

         if (resultSlot >= 0 && resultSlot < inventorySize) {
            boolean resultSlotInAnimation = resultSlot >= animationStartSlot
                  && resultSlot < animationStartSlot + animationSlotCount;
            if (!resultSlotInAnimation) {
               this.addButton(resultSlot, new InventoryButton().creator(p -> {
                  Inventory inv = this.getInventory();
                  return inv != null && resultSlot >= 0 && resultSlot < inv.getSize() ? inv.getItem(resultSlot) : null;
               }).consumer(event -> {
               }));
            }
         }
      } catch (Exception var16) {
         this.plugin.getLogger().severe("Failed to setup protected slots: " + var16.getMessage());
         var16.printStackTrace();
      }
   }

   private void setupPlayerHeads() {
      try {
         String animationType = this.cachedRollingAnimationType != null ? this.cachedRollingAnimationType.toLowerCase()
               : "default";
         int inventorySize = this.getInventory().getSize();
         if ("circular".equals(animationType)) {
            this.setupCircularPlayerHeads();
         } else if ("slot-machine".equals(animationType)) {
            this.setupSlotMachinePlayerHead();
         } else if ("vertical".equals(animationType)) {
            this.setupVerticalArrowSlot();
         } else {
            String configPath = "coinflip-gui.layout.player-slots.default";
            int arrowSlot = this.plugin.getGUIConfig().getInt(configPath + ".arrow-slot", 4);
            String arrowTexture = this.plugin
                  .getGUIConfig()
                  .getString(
                        configPath + ".arrow-texture",
                        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjFlMWU3MzBjNzcyNzljOGUyZTE1ZDhiMjcxYTExN2U1ZTJjYTkzZDI1YzhiZTNhMDBjYzkyYTAwY2MwYmI4NSJ9fX0=");
            String arrowName = this.plugin.getGUIConfig().getString(configPath + ".arrow-name", "⬇️ WINNER ⬇️");
            if (arrowSlot < 0 || arrowSlot >= inventorySize) {
               this.plugin.getLogger().warning("Invalid arrow slot: " + arrowSlot + ". Using default: 4");
               arrowSlot = 4;
            }

            Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
            if (playerHeadMaterial == null) {
               this.plugin.getLogger().warning("Failed to parse PLAYER_HEAD material for arrow head!");
               return;
            }

            ItemStack arrowHead = this.plugin.getGuiHelper().createPlayerHead(playerHeadMaterial, null, arrowTexture,
                  false, arrowName, new ArrayList());
            if (arrowHead != null) {
               this.getInventory().setItem(arrowSlot, arrowHead);
            }
         }
      } catch (Exception var9) {
         this.plugin.getLogger().severe("Failed to setup player heads: " + var9.getMessage());
         var9.printStackTrace();
      }
   }

   private void setupSlotMachinePlayerHead() {
      try {
         int playerSlot = this.cachedSlotMachineCenterSlot;
         int inventorySize = this.getInventory().getSize();
         if (playerSlot < 0 || playerSlot >= inventorySize) {
            this.plugin.getLogger().warning("Invalid slot-machine player slot: " + playerSlot + ". Using default: 13");
            playerSlot = 13;
            this.cachedSlotMachineCenterSlot = playerSlot;
         }

         if (this.animationPool != null && !this.animationPool.isEmpty()) {
            ItemStack initialItem = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size()))
                  .clone();
            if (initialItem != null) {
               this.getInventory().setItem(playerSlot, initialItem);
            }
         }
      } catch (Exception var4) {
         this.plugin.getLogger().severe("Failed to setup slot-machine player head: " + var4.getMessage());
         var4.printStackTrace();
      }
   }

   private void setupCircularPlayerHeads() {
      try {
         int inventorySize = this.getInventory().getSize();
         if (this.cachedCircularPlayerSlots == null || this.cachedCircularPlayerSlots.isEmpty()) {
            this.plugin.getLogger().warning("Circular player slots not configured!");
            return;
         }

         ItemStack player1Item = this.createAnimationItem("player1", this.player1);
         ItemStack player2Item = this.createAnimationItem("player2", this.player2);
         if (player1Item == null || player2Item == null) {
            this.plugin.getLogger().warning("Failed to create player items for circular animation!");
            return;
         }

         for (int i = 0; i < this.cachedCircularPlayerSlots.size(); i++) {
            int slot = this.cachedCircularPlayerSlots.get(i);
            if (slot >= 0 && slot < inventorySize) {
               ItemStack itemToPlace = i % 2 == 0 ? player1Item.clone() : player2Item.clone();
               this.getInventory().setItem(slot, itemToPlace);
            }
         }

         String configPath = "coinflip-gui.layout.player-slots.circular.arrow-slots";
         if (this.plugin.getGUIConfig().contains(configPath)) {
            List<?> arrowSlotsList = this.plugin.getGUIConfig().getList(configPath);
            if (arrowSlotsList != null) {
               for (Object arrowSlotObj : arrowSlotsList) {
                  if (arrowSlotObj instanceof Map) {
                     Map<String, Object> arrowSlotMap = (Map<String, Object>) arrowSlotObj;
                     Object slotObj = arrowSlotMap.get("slot");
                     Object useHeadObj = arrowSlotMap.get("use-head");
                     Object materialObj = arrowSlotMap.get("material");
                     Object textureObj = arrowSlotMap.get("texture");
                     Object nameObj = arrowSlotMap.get("name");
                     if (slotObj instanceof Number && nameObj instanceof String) {
                        int arrowSlot = ((Number) slotObj).intValue();
                        String arrowName = (String) nameObj;
                        boolean useHead = useHeadObj instanceof Boolean ? (Boolean) useHeadObj : true;
                        String materialName = materialObj instanceof String ? (String) materialObj
                              : "BLACK_STAINED_GLASS_PANE";
                        String texture = textureObj instanceof String ? (String) textureObj : "";
                        if (arrowSlot >= 0 && arrowSlot < inventorySize) {
                           ItemStack arrowItem;
                           if (useHead) {
                              Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
                              if (playerHeadMaterial != null) {
                                 arrowItem = this.plugin
                                       .getGuiHelper()
                                       .createPlayerHead(
                                             playerHeadMaterial,
                                             null,
                                             texture.isEmpty()
                                                   ? "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjFlMWU3MzBjNzcyNzljOGUyZTE1ZDhiMjcxYTExN2U1ZTJjYTkzZDI1YzhiZTNhMDBjYzkyYTAwY2MwYmI4NSJ9fX0="
                                                   : texture,
                                             false,
                                             arrowName,
                                             new ArrayList());
                              } else {
                                 Material material = MaterialHelper.parseMaterial(materialName,
                                       MaterialHelper.getBlackStainedGlassPane());
                                 arrowItem = new ItemStack(material);
                                 ItemMeta meta = arrowItem.getItemMeta();
                                 if (meta != null) {
                                    this.plugin.getGuiHelper().setDisplayName(meta, arrowName);
                                    arrowItem.setItemMeta(meta);
                                 }
                              }
                           } else {
                              Material material = MaterialHelper.parseMaterial(materialName,
                                    MaterialHelper.getBlackStainedGlassPane());
                              arrowItem = new ItemStack(material);
                              ItemMeta meta = arrowItem.getItemMeta();
                              if (meta != null) {
                                 this.plugin.getGuiHelper().setDisplayName(meta, arrowName);
                                 arrowItem.setItemMeta(meta);
                              }
                           }

                           if (arrowItem != null) {
                              this.getInventory().setItem(arrowSlot, arrowItem);
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var23) {
         this.plugin.getLogger().severe("Failed to setup circular player heads: " + var23.getMessage());
         var23.printStackTrace();
      }
   }

   private ItemStack createPlayerHeadItem(Player player, String displayName) {
      try {
         if (displayName != null && !displayName.trim().startsWith("&r") && !displayName.trim().startsWith("<reset>")) {
            displayName = "&r" + displayName;
         }

         String configPath = "coinflip-gui.rolling.players.player1";
         Boolean glowing = this.plugin.getGUIConfig().contains(configPath + ".glowing")
               ? this.plugin.getGUIConfig().getBoolean(configPath + ".glowing", false)
               : null;
         Integer customModelData = this.plugin.getGUIConfig().contains(configPath + ".custom-model-data")
               ? this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0)
               : null;
         if (customModelData != null && customModelData <= 0) {
            customModelData = null;
         }

         Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
         if (playerHeadMaterial == null) {
            this.plugin.getLogger().warning("Failed to parse PLAYER_HEAD material for player head!");
            return null;
         } else {
            return this.plugin.getGuiHelper().createPlayerHead(playerHeadMaterial, player, "", true, displayName,
                  new ArrayList(), glowing, customModelData);
         }
      } catch (Exception var7) {
         this.plugin.getLogger().warning("Failed to create player head for "
               + (player != null ? player.getName() : "null") + ": " + var7.getMessage());
         var7.printStackTrace();
         return null;
      }
   }

   private void setupAnimationPool() {
      this.animationPool = new ArrayList<>();
      if (this.isBotGame) {
         ItemStack player1Item = this.createAnimationItem("player1", this.player1);
         if (player1Item != null) {
            this.animationPool.add(player1Item);
         }

         ItemStack botItem = this.createAnimationItem("bot", null);
         if (botItem != null) {
            this.animationPool.add(botItem);
         }

         if (this.animationPool.isEmpty()) {
            this.plugin.getLogger().warning("No animation items created for bot game! Creating default heads.");

            try {
               Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
               if (playerHeadMaterial == null) {
                  this.plugin.getLogger().severe("Failed to parse PLAYER_HEAD material! Cannot create default heads.");
                  return;
               }

               ItemStack defaultHead1 = new ItemStack(playerHeadMaterial);
               SkullMeta meta1 = (SkullMeta) defaultHead1.getItemMeta();
               if (meta1 != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta1, "&e&lPlayer");
                  defaultHead1.setItemMeta(meta1);
               }

               this.animationPool.add(defaultHead1);
               ItemStack defaultBotHead = new ItemStack(playerHeadMaterial);
               SkullMeta botMeta = (SkullMeta) defaultBotHead.getItemMeta();
               if (botMeta != null) {
                  this.plugin.getGuiHelper().setDisplayName(botMeta, "&e&lBot");
                  defaultBotHead.setItemMeta(botMeta);
               }

               this.animationPool.add(defaultBotHead);
            } catch (Exception var11) {
               this.plugin.getLogger()
                     .severe("Failed to create default animation heads for bot game: " + var11.getMessage());
            }
         }
      } else {
         ItemStack player1Itemx = this.createAnimationItem("player1", this.player1);
         if (player1Itemx != null) {
            this.animationPool.add(player1Itemx);
         }

         ItemStack player2Item = this.createAnimationItem("player2", this.player2);
         if (player2Item != null) {
            this.animationPool.add(player2Item);
         }

         if (this.animationPool.isEmpty()) {
            this.plugin.getLogger().warning("No animation items created! Creating default heads.");

            try {
               String configPath1 = "coinflip-gui.rolling.players.player1";
               String configPath2 = "coinflip-gui.rolling.players.player2";
               Material playerHeadMaterialx = MaterialHelper.getPlayerHeadMaterial();
               if (playerHeadMaterialx == null) {
                  this.plugin.getLogger().severe("Failed to parse PLAYER_HEAD material! Cannot create default heads.");
                  return;
               }

               ItemStack defaultHead1x = new ItemStack(playerHeadMaterialx);
               SkullMeta meta1x = (SkullMeta) defaultHead1x.getItemMeta();
               if (meta1x != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta1x, "&e&lPlayer 1");
                  this.plugin.getGuiHelper().applyItemProperties(meta1x, configPath1, this.plugin.getGUIConfig());
                  defaultHead1x.setItemMeta(meta1x);
               }

               this.animationPool.add(defaultHead1x);
               ItemStack defaultHead2 = new ItemStack(playerHeadMaterialx);
               SkullMeta meta2 = (SkullMeta) defaultHead2.getItemMeta();
               if (meta2 != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta2, "&e&lPlayer 2");
                  this.plugin.getGuiHelper().applyItemProperties(meta2, configPath2, this.plugin.getGUIConfig());
                  defaultHead2.setItemMeta(meta2);
               }

               this.animationPool.add(defaultHead2);
            } catch (Exception var10) {
               this.plugin.getLogger().severe("Failed to create default animation heads: " + var10.getMessage());
            }
         }
      }
   }

   private void prepareAnimationSchedule() {
      if (this.animationPool != null && !this.animationPool.isEmpty()) {
         int animationDuration = Math.max(1, this.cachedAnimationDuration);
         int slotCount = Math.max(1, this.cachedAnimationSlotCount);
         this.resultSlotIndex = this.calculateResultSlotIndex();
         this.winnerInsertTick = animationDuration - slotCount + this.resultSlotIndex;
         if (this.winnerInsertTick < 0) {
            this.winnerInsertTick = 0;
         }

         if (this.winnerInsertTick >= animationDuration) {
            this.winnerInsertTick = animationDuration - 1;
         }

         this.scheduledRollItems = new ArrayList<>(animationDuration);
         ItemStack winnerAnimationItem = this.createAnimationItem(this.winner == this.player1 ? "player1" : "player2",
               this.winner);
         if (winnerAnimationItem == null) {
            winnerAnimationItem = this.cloneRandomPoolItem();
         }

         for (int tick = 0; tick < animationDuration; tick++) {
            if (tick == this.winnerInsertTick && winnerAnimationItem != null) {
               this.scheduledRollItems.add(winnerAnimationItem.clone());
            } else {
               ItemStack randomItem = this.cloneRandomPoolItem();
               if (randomItem == null && winnerAnimationItem != null) {
                  randomItem = winnerAnimationItem.clone();
               }

               if (randomItem == null) {
                  randomItem = this.createDefaultAnimationItem();
               }

               if (randomItem != null) {
                  this.scheduledRollItems.add(randomItem);
               }
            }
         }
      } else {
         this.scheduledRollItems = null;
      }
   }

   private int calculateResultSlotIndex() {
      int animationStartSlot = this.cachedAnimationStartSlot;
      int slotCount = this.cachedAnimationSlotCount;
      int desiredResultSlot = this.cachedResultSlot;
      int maxSlot = animationStartSlot + slotCount - 1;
      if (slotCount <= 0) {
         slotCount = 1;
      }

      if (desiredResultSlot < animationStartSlot || desiredResultSlot > maxSlot) {
         desiredResultSlot = animationStartSlot + slotCount / 2;
         this.cachedResultSlot = desiredResultSlot;
      }

      return Math.max(0, Math.min(slotCount - 1, desiredResultSlot - animationStartSlot));
   }

   private ItemStack cloneRandomPoolItem() {
      if (this.animationPool != null && !this.animationPool.isEmpty()) {
         ItemStack original = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size()));
         return original != null ? original.clone() : null;
      } else {
         return null;
      }
   }

   private ItemStack createDefaultAnimationItem() {
      try {
         Material playerHeadMaterial = MaterialHelper.parseMaterial("PLAYER_HEAD", null);
         if (playerHeadMaterial == null) {
            return null;
         } else {
            ItemStack defaultItem = new ItemStack(playerHeadMaterial);
            ItemMeta meta = defaultItem.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, "&7?");
               defaultItem.setItemMeta(meta);
            }

            return defaultItem;
         }
      } catch (Exception var4) {
         this.plugin.getLogger().warning("Failed to create default animation item: " + var4.getMessage());
         return null;
      }
   }

   private ItemStack getScheduledItemForTick(int tick) {
      if (this.scheduledRollItems != null && tick >= 0 && tick < this.scheduledRollItems.size()) {
         ItemStack scheduled = this.scheduledRollItems.get(tick);
         if (scheduled != null) {
            return scheduled.clone();
         }
      }

      return this.cloneRandomPoolItem();
   }

   private ItemStack createAnimationItem(String playerKey, Player player) {
      try {
         if (this.isBotGame && "bot".equals(playerKey)) {
            return this.createBotAnimationItem();
         } else {
            String configPath = "coinflip-gui.rolling.players." + playerKey;
            boolean useHead = this.plugin.getGUIConfig().getBoolean(configPath + ".use-head", true);
            String materialName = this.plugin.getGUIConfig().getString(configPath + ".material", "RED_WOOL");
            String displayNameTemplate = this.plugin.getGUIConfig().getString(configPath + ".display-name",
                  "&e&l{PLAYER}");
            String displayName = displayNameTemplate.replace("{PLAYER}", player != null ? player.getName() : playerKey);
            if (useHead) {
               if (player != null && player.isOnline()) {
                  return this.createPlayerHeadItem(player, displayName);
               } else {
                  Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
                  if (playerHeadMaterial == null) {
                     this.plugin.getLogger().warning("Failed to parse PLAYER_HEAD material for fallback head!");
                     return null;
                  } else {
                     ItemStack head = new ItemStack(playerHeadMaterial);
                     SkullMeta meta = (SkullMeta) head.getItemMeta();
                     if (meta != null) {
                        this.plugin.getGuiHelper().setDisplayName(meta, displayName);
                        this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
                        head.setItemMeta(meta);
                     }

                     return head;
                  }
               }
            } else {
               Material redWoolFallback = MaterialHelper.getRedWoolMaterial();
               Material material = MaterialHelper.parseMaterial(materialName, redWoolFallback);
               if (MaterialHelper.isSameMaterial(material, redWoolFallback)
                     && !materialName.equalsIgnoreCase("RED_WOOL")) {
                  this.plugin.getLogger()
                        .warning("Invalid material for " + playerKey + ": " + materialName + ". Using RED_WOOL.");
               }

               ItemStack item = new ItemStack(material);
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta, displayName);
                  this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
                  item.setItemMeta(meta);
               }

               return item;
            }
         }
      } catch (Exception var12) {
         this.plugin.getLogger()
               .warning("Failed to create animation item for " + playerKey + ": " + var12.getMessage());
         var12.printStackTrace();
         return null;
      }
   }

   private ItemStack createBotAnimationItem() {
      try {
         String configPath = "coinflip-gui.rolling.players.bot";
         String materialName = this.plugin
               .getConfig()
               .getString("house.display.material",
                     this.plugin.getGUIConfig().getString(configPath + ".material", "PLAYER_HEAD"));
         String displayNameTemplate = this.plugin
               .getConfig()
               .getString("house.display.display-name",
                     this.plugin.getGUIConfig().getString(configPath + ".display-name", "&e&l{BOT}"));
         String displayName = displayNameTemplate.replace("{BOT}", this.botName != null ? this.botName : "Bot");
         boolean useHead = this.plugin
               .getGUIConfig()
               .getBoolean(configPath + ".use-head",
                     MaterialHelper.isPlayerHead(MaterialHelper.parseMaterial(materialName, null)));
         if (useHead) {
            Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
            if (playerHeadMaterial == null) {
               this.plugin.getLogger().warning("Failed to parse PLAYER_HEAD material for bot head!");
               return null;
            } else {
               List<?> lore = new ArrayList();
               return this.plugin
                     .getGuiHelper()
                     .createPlayerHead(
                           playerHeadMaterial,
                           null,
                           this.botTexture != null ? this.botTexture : "",
                           false,
                           displayName,
                           lore,
                           this.plugin.getGUIConfig().getBoolean(configPath + ".glowing", false),
                           this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0) > 0
                                 ? this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0)
                                 : null);
            }
         } else {
            Material material = MaterialHelper.parseMaterial(materialName, Material.GOLD_INGOT);
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               this.plugin.getGuiHelper().setDisplayName(meta, displayName);
               this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
               item.setItemMeta(meta);
            }

            return item;
         }
      } catch (Exception var9) {
         this.plugin.getLogger().warning("Failed to create bot animation item: " + var9.getMessage());
         var9.printStackTrace();
         return null;
      }
   }

   private ItemStack createPlayerItem(String type, Player player) {
      if (this.isBotGame && player == null) {
         return this.createBotResultItem(type);
      } else {
         String materialName = this.plugin.getGUIConfig().getString("coinflip-gui.results." + type + ".item-type",
               "PLAYER_HEAD");
         String skinSource = this.plugin.getGUIConfig().getString("coinflip-gui.results." + type + ".skin.source",
               "player");
         String base64 = this.plugin.getGUIConfig().getString("coinflip-gui.results." + type + ".skin.texture", "");
         String displayName = this.plugin.getGUIConfig().getString("coinflip-gui.results." + type + ".display.name",
               "&e{PLAYER}");
         List<String> lore = this.plugin.getGUIConfig()
               .getStringList("coinflip-gui.results." + type + ".display.description");

         try {
            Material material = MaterialHelper.parseMaterial(materialName, null);
            if (material == null) {
               this.plugin.getLogger().warning("Invalid material for result item " + type + ": " + materialName);
               return null;
            } else {
               Map<String, String> placeholders = new HashMap<>();
               placeholders.put("PLAYER", player != null ? player.getName() : "Unknown");
               displayName = displayName.replace("{PLAYER}", player != null ? player.getName() : "Unknown");
               boolean usePlayerSkin = "player".equalsIgnoreCase(skinSource);
               boolean useCustomTexture = "custom".equalsIgnoreCase(skinSource) && base64 != null && !base64.isEmpty();
               String configPath = "coinflip-gui.results." + type;
               Boolean glowing = this.plugin.getGUIConfig().contains(configPath + ".glowing")
                     ? this.plugin.getGUIConfig().getBoolean(configPath + ".glowing", false)
                     : null;
               Integer customModelData = this.plugin.getGUIConfig().contains(configPath + ".custom-model-data")
                     ? this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0)
                     : null;
               if (customModelData != null && customModelData <= 0) {
                  customModelData = null;
               }

               if (MaterialHelper.isPlayerHead(material)) {
                  List<?> processedLore = (List<?>) (lore != null
                        ? this.plugin.getGuiHelper().createLore(lore, placeholders)
                        : new ArrayList());
                  return this.plugin
                        .getGuiHelper()
                        .createPlayerHead(material, player, useCustomTexture ? base64 : "", usePlayerSkin, displayName,
                              processedLore, glowing, customModelData);
               } else {
                  ItemStack item = new ItemStack(material);
                  ItemMeta meta = item.getItemMeta();
                  if (meta != null) {
                     this.plugin.getGuiHelper().setDisplayName(meta, displayName, placeholders);
                     if (lore != null && !lore.isEmpty()) {
                        List<?> loreList = this.plugin.getGuiHelper().createLore(lore, placeholders);
                        this.plugin.getGuiHelper().setLore(meta, loreList);
                     }

                     this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
                     item.setItemMeta(meta);
                  }

                  return item;
               }
            }
         } catch (IllegalArgumentException var18) {
            this.plugin.getLogger().warning("Invalid material for result item " + type + ": " + materialName);
            return null;
         }
      }
   }

   private ItemStack createBotResultItem(String type) {
      try {
         String configPath = "coinflip-gui.results." + type;
         String materialName = this.plugin
               .getConfig()
               .getString("house.display.material",
                     this.plugin.getGUIConfig().getString(configPath + ".item-type", "PLAYER_HEAD"));
         String displayNameTemplate = this.plugin
               .getConfig()
               .getString("house.display.display-name",
                     this.plugin.getGUIConfig().getString(configPath + ".display.name", "&e{BOT}"));
         String skinSource = this.plugin.getGUIConfig().getString(configPath + ".skin.source", "player");
         String base64 = this.plugin.getGUIConfig().getString(configPath + ".skin.texture", "");
         List<String> lore = this.plugin.getGUIConfig()
               .getStringList("coinflip-gui.results." + type + ".display.description");
         Material material = MaterialHelper.parseMaterial(materialName, null);
         if (material == null) {
            this.plugin.getLogger().warning("Invalid material for bot result item " + type + ": " + materialName);
            return null;
         } else {
            String displayName = displayNameTemplate.replace("{BOT}", this.botName != null ? this.botName : "Bot");
            displayName = displayName.replace("{PLAYER}", this.botName != null ? this.botName : "Bot");
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("BOT", this.botName != null ? this.botName : "Bot");
            placeholders.put("PLAYER", this.botName != null ? this.botName : "Bot");
            boolean useCustomTexture = "custom".equalsIgnoreCase(skinSource) && this.botTexture != null
                  && !this.botTexture.isEmpty();
            Boolean glowing = this.plugin.getGUIConfig().contains(configPath + ".glowing")
                  ? this.plugin.getGUIConfig().getBoolean(configPath + ".glowing", false)
                  : null;
            Integer customModelData = this.plugin.getGUIConfig().contains(configPath + ".custom-model-data")
                  ? this.plugin.getGUIConfig().getInt(configPath + ".custom-model-data", 0)
                  : null;
            if (customModelData != null && customModelData <= 0) {
               customModelData = null;
            }

            if (MaterialHelper.isPlayerHead(material)) {
               List<?> processedLore = (List<?>) (lore != null
                     ? this.plugin.getGuiHelper().createLore(lore, placeholders)
                     : new ArrayList());
               return this.plugin
                     .getGuiHelper()
                     .createPlayerHead(material, null, useCustomTexture ? this.botTexture : "", false, displayName,
                           processedLore, glowing, customModelData);
            } else {
               ItemStack item = new ItemStack(material);
               ItemMeta meta = item.getItemMeta();
               if (meta != null) {
                  this.plugin.getGuiHelper().setDisplayName(meta, displayName, placeholders);
                  if (lore != null && !lore.isEmpty()) {
                     List<?> loreList = this.plugin.getGuiHelper().createLore(lore, placeholders);
                     this.plugin.getGuiHelper().setLore(meta, loreList);
                  }

                  this.plugin.getGuiHelper().applyItemProperties(meta, configPath, this.plugin.getGUIConfig());
                  item.setItemMeta(meta);
               }

               return item;
            }
         }
      } catch (Exception var17) {
         this.plugin.getLogger().warning("Failed to create bot result item: " + var17.getMessage());
         var17.printStackTrace();
         return null;
      }
   }

   private boolean isWinnerItem(ItemStack item, Player winner) {
      if (item == null || winner == null) {
         return false;
      } else if (!MaterialHelper.isPlayerHead(item.getType())) {
         return false;
      } else {
         ItemMeta meta = item.getItemMeta();
         if (!(meta instanceof SkullMeta)) {
            return false;
         } else {
            SkullMeta skullMeta = (SkullMeta) meta;

            try {
               if (skullMeta.hasOwner()) {
                  String ownerName = skullMeta.getOwner();
                  if (ownerName != null && ownerName.equalsIgnoreCase(winner.getName())) {
                     return true;
                  }

                  try {
                     UUID ownerUUID = skullMeta.getOwningPlayer() != null ? skullMeta.getOwningPlayer().getUniqueId()
                           : null;
                     if (ownerUUID != null && ownerUUID.equals(winner.getUniqueId())) {
                        return true;
                     }
                  } catch (Exception var9) {
                  }
               }
            } catch (Exception var10) {
               try {
                  String ownerName = skullMeta.getOwner();
                  if (ownerName != null && ownerName.equalsIgnoreCase(winner.getName())) {
                     return true;
                  }
               } catch (Exception var8) {
               }
            }

            try {
               String displayName = skullMeta.getDisplayName();
               if (displayName != null && displayName.contains(winner.getName())) {
                  return true;
               }
            } catch (Exception var7) {
            }

            return false;
         }
      }
   }

   private void animateRoll(int tick) {
      if (!this.animationRunning) {
         if (this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin.getDebugManager().verbose(DebugManager.Category.GUI,
                  "animateRoll(" + tick + ") called but animationRunning=false");
         }
      } else {
         if (this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin
                  .getDebugManager()
                  .verbose(DebugManager.Category.GUI,
                        "animateRoll(" + tick + ") - Animation running, duration: " + this.cachedAnimationDuration);
         }

         boolean player1Online = this.player1 != null && this.player1.isOnline();
         boolean player2Online = this.player2 != null && this.player2.isOnline();
         if (!player1Online && !player2Online) {
            this.animationRunning = false;
            this.cancelCurrentAnimationTask();
            if (this.plugin.getDebugManager() != null
                  && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin.getDebugManager().info(DebugManager.Category.GUI,
                     "Animation stopped: Both players disconnected");
            }
         } else {
            boolean player1InGame = this.player1 != null
                  && this.plugin.getCoinFlipManager().isInRollingGame(this.player1.getUniqueId());
            boolean player2InGame = this.player2 != null
                  && this.plugin.getCoinFlipManager().isInRollingGame(this.player2.getUniqueId());
            if (!player1InGame && !player2InGame) {
               this.animationRunning = false;
               this.cancelCurrentAnimationTask();
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin.getDebugManager().info(DebugManager.Category.GUI,
                        "Animation stopped: Game was refunded (removed from activeRollingGames)");
               }
            } else {
               int animationSpeed = this.cachedAnimationSpeed;
               int animationDuration = this.cachedAnimationDuration;
               if (tick >= animationDuration) {
                  this.animationRunning = false;
                  this.cancelCurrentAnimationTask();
                  String animationType = this.cachedRollingAnimationType != null
                        ? this.cachedRollingAnimationType.toLowerCase()
                        : "default";
                  int inventorySize = this.cachedInventorySize;
                  ItemStack winnerHead = this.cachedWinnerHead != null ? this.cachedWinnerHead.clone()
                        : this.createPlayerItem("winner", this.winner);
                  if (winnerHead != null) {
                     Inventory inventory = this.getInventory();
                     if (inventory != null) {
                        if ("slot-machine".equals(animationType)) {
                           int centerSlot = this.cachedSlotMachineCenterSlot;
                           if (centerSlot >= 0 && centerSlot < inventorySize) {
                              inventory.setItem(centerSlot, winnerHead.clone());
                           }
                        } else if ("circular".equals(animationType)) {
                           int winnerSlot = 20;
                           if (winnerSlot >= 0 && winnerSlot < inventorySize) {
                              inventory.setItem(winnerSlot, winnerHead.clone());
                           }
                        } else if ("vertical".equals(animationType)) {
                           int winnerSlot = 22;
                           if (winnerSlot >= 0 && winnerSlot < inventorySize) {
                              inventory.setItem(winnerSlot, winnerHead.clone());
                           }
                        } else {
                           int animationStartSlot = this.cachedAnimationStartSlot;
                           int animationSlotCount = this.cachedAnimationSlotCount;
                           int resultSlot = this.cachedResultSlot;
                           if (resultSlot < 0 || resultSlot >= inventorySize) {
                              resultSlot = animationStartSlot + animationSlotCount / 2;
                           }

                           int resultSlotIndex = resultSlot - animationStartSlot;
                           if (resultSlotIndex >= 0 && resultSlotIndex < this.animationItems.length) {
                              this.animationItems[resultSlotIndex] = winnerHead.clone();
                           }

                           if (animationSlotCount > 0 && animationSlotCount <= this.animationItems.length) {
                              int lastIndex = animationSlotCount - 1;
                              if (lastIndex >= 0 && lastIndex < this.animationItems.length) {
                                 this.animationItems[lastIndex] = winnerHead.clone();
                              }
                           }

                           if (resultSlot >= 0 && resultSlot < inventorySize) {
                              inventory.setItem(resultSlot, winnerHead.clone());
                           }

                           int lastSlot = animationStartSlot + animationSlotCount - 1;
                           if (lastSlot >= 0 && lastSlot < inventorySize && lastSlot != resultSlot) {
                              inventory.setItem(lastSlot, winnerHead.clone());
                           }
                        }

                        this.updateInventoryForPlayers();
                     }
                  }

                  Player finishSchedulerPlayer = null;
                  if (this.player1 != null && this.player1.isOnline()) {
                     finishSchedulerPlayer = this.player1;
                  } else if (this.player2 != null && this.player2.isOnline()) {
                     finishSchedulerPlayer = this.player2;
                  }

                  if (finishSchedulerPlayer != null) {
                     FoliaScheduler.runTaskLater(this.plugin, finishSchedulerPlayer, () -> this.finishRoll(), 5L);
                  } else {
                     FoliaScheduler.runTaskLater(this.plugin, () -> this.finishRoll(), 5L);
                  }
               } else {
                  boolean bothClosedGUI = this.playersClosedGUI.contains(this.player1UUID)
                        && this.playersClosedGUI.contains(this.player2UUID);
                  String animationTypex = this.cachedRollingAnimationType != null
                        ? this.cachedRollingAnimationType.toLowerCase()
                        : "default";
                  final String animationType = animationTypex;
                  boolean needsInventoryUpdate = false;
                  int tempAnimationSlotCount = this.cachedAnimationSlotCount;
                  int tempAnimationStartSlot = this.cachedAnimationStartSlot;
                  int inventorySizex = this.cachedInventorySize;
                  final int inventorySize = inventorySizex;
                  if ("default".equals(animationTypex)) {
                     if (this.animationItems == null || this.animationItems.length == 0) {
                        this.plugin.getLogger().warning("Animation items array is null or empty! Stopping animation.");
                        this.animationRunning = false;
                        if (this.player1 != null) {
                           this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
                        }

                        return;
                     }

                     if (tempAnimationSlotCount > this.animationItems.length) {
                        this.plugin
                              .getLogger()
                              .warning(
                                    "Animation slot count (" + tempAnimationSlotCount + ") exceeds array length ("
                                          + this.animationItems.length + "). Adjusting...");
                        tempAnimationSlotCount = this.animationItems.length;
                     }

                     if (tempAnimationSlotCount <= 0) {
                        this.plugin.getLogger().warning(
                              "Invalid animation slot count: " + tempAnimationSlotCount + ". Stopping animation.");
                        this.animationRunning = false;
                        if (this.player1 != null) {
                           this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
                        }

                        return;
                     }

                     if (tempAnimationStartSlot < 0 || tempAnimationStartSlot >= inventorySizex) {
                        this.plugin.getLogger()
                              .warning("Invalid animation start slot: " + tempAnimationStartSlot + ". Adjusting...");
                        tempAnimationStartSlot = Math.max(0, Math.min(tempAnimationStartSlot, inventorySizex - 1));
                     }

                     if (tempAnimationStartSlot + tempAnimationSlotCount > inventorySizex) {
                        tempAnimationSlotCount = Math.max(1, inventorySizex - tempAnimationStartSlot);
                     }
                  }

                  int finalAnimationSlotCount = tempAnimationSlotCount;
                  int finalAnimationStartSlot = tempAnimationStartSlot;
                  if ("default".equals(animationTypex)) {
                     for (int i = 0; i < finalAnimationSlotCount - 1; i++) {
                        if (i + 1 >= this.animationItems.length) {
                           this.plugin
                                 .getLogger()
                                 .warning("Array index out of bounds: " + (i + 1) + " >= " + this.animationItems.length
                                       + ". Stopping animation.");
                           this.animationRunning = false;
                           if (this.player1 != null) {
                              this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
                           }

                           return;
                        }

                        if (this.animationItems[i + 1] != null) {
                           this.animationItems[i] = this.animationItems[i + 1].clone();
                           needsInventoryUpdate = true;
                        } else {
                           if (this.animationPool == null || this.animationPool.isEmpty()) {
                              this.plugin.getLogger().warning("Animation pool is empty and item at index " + (i + 1)
                                    + " is null. Cannot continue animation.");
                              this.animationRunning = false;
                              if (this.player1 != null) {
                                 this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
                              }

                              return;
                           }

                           ItemStack original = this.animationPool
                                 .get(this.secureRandom.nextInt(this.animationPool.size()));
                           if (original != null) {
                              this.animationItems[i] = original.clone();
                              needsInventoryUpdate = true;
                           }
                        }
                     }

                     ItemStack scheduledItem = this.getScheduledItemForTick(tick);
                     if (scheduledItem == null) {
                        this.plugin.getLogger()
                              .warning("Scheduled animation item is null at tick " + tick + ". Stopping animation.");
                        this.animationRunning = false;
                        if (this.player1 != null) {
                           this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
                        }

                        return;
                     }

                     int lastIndex = finalAnimationSlotCount - 1;
                     if (lastIndex < 0 || lastIndex >= this.animationItems.length) {
                        this.plugin.getLogger().warning(
                              "Array index out of bounds for last item: " + lastIndex + ". Stopping animation.");
                        this.animationRunning = false;
                        if (this.player1 != null) {
                           this.plugin.getCoinFlipManager().refundRollingGame(this.player1.getUniqueId());
                        }

                        return;
                     }

                     this.animationItems[lastIndex] = scheduledItem;
                     needsInventoryUpdate = true;
                  }

                  if (this.plugin.getDebugManager() != null
                        && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     this.plugin
                           .getDebugManager()
                           .verbose(
                                 DebugManager.Category.GUI,
                                 "Tick "
                                       + tick
                                       + " - bothClosedGUI="
                                       + bothClosedGUI
                                       + ", player1Online="
                                       + (this.player1 != null && this.player1.isOnline())
                                       + ", player2Online="
                                       + (this.player2 != null && this.player2.isOnline()));
                  }

                  if (!bothClosedGUI) {
                     Player tempSchedulerPlayer = null;
                     if (this.player1 != null && this.player1.isOnline()) {
                        tempSchedulerPlayer = this.player1;
                     } else if (this.player2 != null && this.player2.isOnline()) {
                        tempSchedulerPlayer = this.player2;
                     }

                     if (this.plugin.getDebugManager() != null
                           && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                        this.plugin
                              .getDebugManager()
                              .verbose(
                                    DebugManager.Category.GUI,
                                    "Tick " + tick + " - schedulerPlayer="
                                          + (tempSchedulerPlayer != null ? tempSchedulerPlayer.getName() : "null"));
                     }

                     if (tempSchedulerPlayer != null) {
                        int finalSlotCount = finalAnimationSlotCount;
                        final boolean finalNeedsInventoryUpdate = needsInventoryUpdate;
                        final Player schedulerPlayer = tempSchedulerPlayer;
                        FoliaScheduler.runTask(
                              this.plugin,
                              tempSchedulerPlayer,
                              () -> {
                                 if (!this.animationRunning) {
                                    if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager()
                                          .isCategoryEnabled(DebugManager.Category.GUI)) {
                                       this.plugin
                                             .getDebugManager()
                                             .verbose(DebugManager.Category.GUI,
                                                   "Animation task skipped: animationRunning=false at tick " + tick);
                                    }
                                 } else {
                                    if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager()
                                          .isCategoryEnabled(DebugManager.Category.GUI)) {
                                       this.plugin
                                             .getDebugManager()
                                             .verbose(
                                                   DebugManager.Category.GUI,
                                                   "Animation tick " + tick + " - Type: " + animationType
                                                         + ", Scheduler: " + schedulerPlayer.getName());
                                    }

                                    if ("slot-machine".equals(animationType)) {
                                       this.updateSlotMachineAnimationDirect(tick);
                                    } else if ("circular".equals(animationType)) {
                                       this.updateCircularAnimationDirect(tick);
                                    } else if ("vertical".equals(animationType)) {
                                       this.updateVerticalAnimationDirect(tick);
                                    } else if (finalNeedsInventoryUpdate) {
                                       this.batchUpdateAnimationItems(finalAnimationStartSlot, finalSlotCount,
                                             inventorySize);
                                    }

                                    this.updateBorderAnimation(tick);
                                    if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager()
                                          .isCategoryEnabled(DebugManager.Category.GUI)) {
                                       this.plugin
                                             .getDebugManager()
                                             .verbose(
                                                   DebugManager.Category.GUI, "Animation tick " + tick
                                                         + " - Updated inventory, calling updateInventoryForPlayers()");
                                    }

                                    this.updateInventoryForPlayers();
                                 }
                              });
                     } else {
                        if ("slot-machine".equals(animationTypex)) {
                           this.updateSlotMachineAnimationDirect(tick);
                        } else if ("circular".equals(animationTypex)) {
                           this.updateCircularAnimationDirect(tick);
                        } else if ("vertical".equals(animationTypex)) {
                           this.updateVerticalAnimationDirect(tick);
                        } else if (needsInventoryUpdate) {
                           this.batchUpdateAnimationItems(finalAnimationStartSlot, finalAnimationSlotCount,
                                 inventorySizex);
                        }

                        this.updateBorderAnimation(tick);
                     }
                  } else if (needsInventoryUpdate && "default".equals(animationTypex)) {
                     this.batchUpdateAnimationItems(finalAnimationStartSlot, finalAnimationSlotCount, inventorySizex);
                  }

                  boolean atLeastOnePlayerHasGUI = !this.playersClosedGUI.contains(this.player1UUID)
                        || !this.playersClosedGUI.contains(this.player2UUID);
                  if (atLeastOnePlayerHasGUI) {
                     this.playAnimationTickSound();
                  }

                  double progress = (double) tick / animationDuration;
                  long delay;
                  if (progress < 0.3) {
                     delay = Math.max(1, animationSpeed / 3);
                  } else if (progress < 0.6) {
                     delay = Math.max(1, animationSpeed);
                  } else if (progress < 0.85) {
                     delay = Math.max(1, animationSpeed * 2);
                  } else {
                     double finalProgress = (progress - 0.85) / 0.15;
                     long baseDelay = animationSpeed * 3;
                     long maxDelay = animationSpeed * 5;
                     delay = Math.max(1L, (long) (baseDelay + (maxDelay - baseDelay) * finalProgress));
                  }

                  int nextTick = tick + 1;
                  if (delay > 0L) {
                     FoliaScheduler.runTaskLater(this.plugin, () -> {
                        if (this.animationRunning) {
                           this.animateRoll(nextTick);
                        }
                     }, delay);
                  } else {
                     FoliaScheduler.runTask(this.plugin, () -> {
                        if (this.animationRunning) {
                           this.animateRoll(nextTick);
                        }
                     });
                  }
               }
            }
         }
      }
   }

   private void cancelCurrentAnimationTask() {
      this.animationRunning = false;
      this.currentAnimationTaskId = null;
      if (this.scheduledRollItems != null) {
         this.scheduledRollItems.clear();
      }

      this.cachedWinnerHead = null;
      this.cachedLoserHead = null;
   }

   private void batchUpdateAnimationItems(int animationStartSlot, int animationSlotCount, int inventorySize) {
      Inventory inventory = this.getInventory();
      String animationType = this.cachedRollingAnimationType != null ? this.cachedRollingAnimationType.toLowerCase()
            : "default";
      if (!"slot-machine".equals(animationType) && !"circular".equals(animationType)
            && !"vertical".equals(animationType)) {
         for (int i = 0; i < animationSlotCount; i++) {
            int slot = animationStartSlot + i;
            if (slot >= 0 && slot < inventorySize && i < this.animationItems.length && this.animationItems[i] != null) {
               ItemStack item = this.animationItems[i].clone();
               if (!"default".equals(animationType)) {
                  item = this.applyRollingAnimationEffect(item, i, animationSlotCount, animationType);
               }

               inventory.setItem(slot, item);
            }
         }
      }
   }

   private ItemStack applyRollingAnimationEffect(ItemStack item, int slotIndex, int totalSlots, String animationType) {
      if (item != null && item.getItemMeta() != null) {
         ItemStack modifiedItem = item.clone();
         ItemMeta meta = modifiedItem.getItemMeta();
         if (meta == null) {
            return modifiedItem;
         } else {
            double slotProgress = (double) slotIndex / Math.max(1, totalSlots - 1);
            int currentTick = (int) (System.currentTimeMillis() / 50L) % 100;
            String originalName = meta.getDisplayName();
            if (originalName == null) {
               originalName = "";
            }

            String modifiedName = originalName;
            if ("bounce".equals(animationType)
                  || this.cachedComboEffects != null && this.cachedComboEffects.contains("bounce")) {
               double adjustedIntensity = this.cachedBounceIntensity * (1.0 + slotProgress * 0.5);
               double bouncePhase = (currentTick * this.cachedBounceFrequency + slotIndex * 2) % (Math.PI * 2);
               double bounceValue = Math.abs(Math.sin(bouncePhase)) * adjustedIntensity;
               if (bounceValue > 0.5) {
                  try {
                     LegacyCompatibility.setGlowing(meta, true);
                  } catch (Exception var25) {
                  }

                  if (!originalName.isEmpty()) {
                     String bounceSymbol = bounceValue > 0.8 ? "⬆" : "⬇";
                     modifiedName = originalName + " " + bounceSymbol;
                  }
               } else {
                  try {
                     LegacyCompatibility.setGlowing(meta, false);
                  } catch (Exception var24) {
                  }
               }
            }

            if ("spin".equals(animationType)
                  || this.cachedComboEffects != null && this.cachedComboEffects.contains("spin")) {
               int rotationTick = (currentTick * this.cachedSpinRotationSpeed + slotIndex) % 8;
               String[] spinSymbols;
               if ("x".equalsIgnoreCase(this.cachedSpinRotationAxis)) {
                  spinSymbols = new String[] { "↔", "⇄", "⇆", "↔", "⇄", "⇆", "↔", "⇄" };
               } else if ("z".equalsIgnoreCase(this.cachedSpinRotationAxis)) {
                  spinSymbols = new String[] { "↕", "⇅", "⇵", "↕", "⇅", "⇵", "↕", "⇅" };
               } else {
                  spinSymbols = new String[] { "◐", "◓", "◑", "◒", "◐", "◓", "◑", "◒" };
               }

               String spinSymbol = spinSymbols[rotationTick % spinSymbols.length];
               if (!originalName.isEmpty()) {
                  modifiedName = originalName.replaceAll(" [◐◓◑◒↔⇄⇆↕⇅⇵]", "") + " " + spinSymbol;
               } else {
                  modifiedName = spinSymbol;
               }

               try {
                  LegacyCompatibility.setGlowing(meta, true);
               } catch (Exception var23) {
               }
            }

            if ("scale".equals(animationType)
                  || this.cachedComboEffects != null && this.cachedComboEffects.contains("scale")) {
               double scalePhase = (currentTick * this.cachedScaleSpeed + slotIndex) % (Math.PI * 2);
               double scaleValue = this.cachedScaleMinScale
                     + (this.cachedScaleMaxScale - this.cachedScaleMinScale) * (0.5 + 0.5 * Math.sin(scalePhase));
               if (scaleValue > 1.0) {
                  try {
                     LegacyCompatibility.setGlowing(meta, true);
                  } catch (Exception var22) {
                  }

                  if (!originalName.isEmpty()) {
                     String scaleSymbol = scaleValue > 1.1 ? "⬆⬆" : "⬆";
                     modifiedName = originalName + " " + scaleSymbol;
                  }
               } else {
                  try {
                     LegacyCompatibility.setGlowing(meta, false);
                  } catch (Exception var21) {
                  }

                  if (!originalName.isEmpty()) {
                     modifiedName = originalName + " ⬇";
                  }
               }
            }

            if (("fade".equals(animationType)
                  || this.cachedComboEffects != null && this.cachedComboEffects.contains("fade"))
                  && !originalName.isEmpty()) {
               double opacity = 1.0;
               if (slotIndex < this.cachedFadeInSlots) {
                  opacity = this.cachedFadeMinOpacity
                        + (1.0 - this.cachedFadeMinOpacity) * ((double) slotIndex / this.cachedFadeInSlots);
               } else if (slotIndex >= totalSlots - this.cachedFadeOutSlots) {
                  int fadeOutIndex = slotIndex - (totalSlots - this.cachedFadeOutSlots);
                  opacity = 1.0 - (1.0 - this.cachedFadeMinOpacity) * ((double) fadeOutIndex / this.cachedFadeOutSlots);
               }

               opacity = Math.max(this.cachedFadeMinOpacity, Math.min(1.0, opacity));
               if (opacity < 0.3) {
                  modifiedName = originalName.replace("&l", "").replace("&e", "&8").replace("&a", "&8").replace("&c",
                        "&8");
               } else if (opacity < 0.5) {
                  modifiedName = originalName.replace("&l", "").replace("&e", "&7").replace("&a", "&7").replace("&c",
                        "&7");
               } else if (opacity < 0.7) {
                  modifiedName = originalName.replace("&l", "");
               }
            }

            if ("glow".equals(animationType)
                  || this.cachedComboEffects != null && this.cachedComboEffects.contains("glow")) {
               double glowPhase = (currentTick * this.cachedGlowSpeed + slotIndex) % (Math.PI * 2);
               double glowValue = (Math.sin(glowPhase) + 1.0) / 2.0;
               if (glowValue > 1.0 - this.cachedGlowIntensity) {
                  try {
                     LegacyCompatibility.setGlowing(meta, true);
                  } catch (Exception var20) {
                  }

                  if (!originalName.isEmpty()) {
                     String glowSymbol = "✨";
                     modifiedName = originalName + " " + glowSymbol;
                  }
               } else {
                  try {
                     LegacyCompatibility.setGlowing(meta, false);
                  } catch (Exception var19) {
                  }
               }
            }

            if (!modifiedName.equals(originalName)) {
               this.plugin.getGuiHelper().setDisplayName(meta, modifiedName);
            }

            if ("combo".equals(animationType) && this.cachedComboEffects != null && this.cachedComboIntensity < 1.0) {
            }

            if (this.cachedGlowColor != null
                  && !this.cachedGlowColor.isEmpty()
                  && !"glow".equals(animationType)
                  && this.cachedComboEffects != null
                  && this.cachedComboEffects.contains("glow")) {
            }

            modifiedItem.setItemMeta(meta);
            return modifiedItem;
         }
      } else {
         return item;
      }
   }

   private void updateBorderAnimation(int tick) {
      if (!"basic".equalsIgnoreCase(this.cachedBorderAnimationMode)) {
         if (tick % this.cachedBorderAnimationSpeed != 0) {
            if (this.plugin.getDebugManager() != null
                  && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin
                     .getDebugManager()
                     .verbose(
                           DebugManager.Category.GUI,
                           "Border animation skipped at tick "
                                 + tick
                                 + " (speed="
                                 + this.cachedBorderAnimationSpeed
                                 + ", tick % speed = "
                                 + tick % this.cachedBorderAnimationSpeed
                                 + ")");
            }
         } else {
            if (this.plugin.getDebugManager() != null
                  && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin
                     .getDebugManager()
                     .verbose(DebugManager.Category.GUI,
                           "Updating border animation at tick " + tick + ", mode: " + this.cachedBorderAnimationMode);
            }

            try {
               String animationType = this.cachedRollingAnimationType != null
                     ? this.cachedRollingAnimationType.toLowerCase()
                     : "default";
               int arrowSlot = this.plugin.getGUIConfig().getInt("coinflip-gui.layout.player-slots.default.arrow-slot",
                     4);
               int slotMachinePlayerSlot = -1;
               Set<Integer> circularExcludedSlots = new HashSet<>();
               Set<Integer> verticalExcludedSlots = new HashSet<>();
               int verticalArrowSlot = -1;
               if ("slot-machine".equals(animationType)) {
                  slotMachinePlayerSlot = this.cachedSlotMachineCenterSlot;
               }

               if ("circular".equals(animationType)) {
                  if (this.cachedCircularPlayerSlots != null) {
                     circularExcludedSlots.addAll(this.cachedCircularPlayerSlots);
                  }

                  String arrowSlotsPath = "coinflip-gui.layout.player-slots.circular.arrow-slots";
                  if (this.plugin.getGUIConfig().contains(arrowSlotsPath)) {
                     List<?> arrowSlotsList = this.plugin.getGUIConfig().getList(arrowSlotsPath);
                     if (arrowSlotsList != null) {
                        for (Object arrowSlotObj : arrowSlotsList) {
                           if (arrowSlotObj instanceof Map) {
                              Map<String, Object> arrowSlotMap = (Map<String, Object>) arrowSlotObj;
                              Object slotObj = arrowSlotMap.get("slot");
                              if (slotObj instanceof Number) {
                                 circularExcludedSlots.add(((Number) slotObj).intValue());
                              }
                           }
                        }
                     }
                  }
               }

               if ("vertical".equals(animationType)) {
                  if (this.cachedVerticalPlayerSlots != null) {
                     verticalExcludedSlots.addAll(this.cachedVerticalPlayerSlots);
                  }

                  verticalArrowSlot = this.cachedVerticalArrowSlot;
               }

               String borderDisplayName = this.plugin.getGUIConfig().getString("coinflip-gui.layout.border-item.name",
                     " ");
               Inventory inventory = this.getInventory();
               int inventorySize = this.cachedInventorySize;
               ItemStack[] colorPaletteItems = this.customColorPaletteItems != null
                     && this.customColorPaletteItems.length > 0
                           ? this.customColorPaletteItems
                           : this.rainbowGlassItems;
               if (colorPaletteItems == null || colorPaletteItems.length == 0) {
                  return;
               }

               String mode = this.cachedBorderAnimationMode.toLowerCase();
               if ("rainbow".equals(mode)) {
                  int colorIndex = tick / this.cachedBorderAnimationSpeed % colorPaletteItems.length;
                  ItemStack border = this.createBorderItemFromItemStack(colorPaletteItems[colorIndex],
                        borderDisplayName);
                  this.updateAllBorderSlots(
                        inventory, inventorySize, arrowSlot, border, slotMachinePlayerSlot, circularExcludedSlots,
                        verticalExcludedSlots, verticalArrowSlot);
               } else if ("pulse".equals(mode)) {
                  int cycleTick = tick % (this.cachedBorderPulseCycleDuration * this.cachedBorderAnimationSpeed);
                  double progress = (double) cycleTick
                        / (this.cachedBorderPulseCycleDuration * this.cachedBorderAnimationSpeed);
                  double brightness = this.cachedBorderPulseMinBrightness
                        + (this.cachedBorderPulseMaxBrightness - this.cachedBorderPulseMinBrightness)
                              * (0.5 + 0.5 * Math.sin(progress * 2.0 * Math.PI * 3.0));
                  ItemStack darkItem = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
                  ItemStack brightItem = MaterialHelper.createItemStack("WHITE_STAINED_GLASS_PANE");
                  if (darkItem == null) {
                     darkItem = MaterialHelper.createItemStack("GRAY_STAINED_GLASS_PANE");
                  }

                  if (brightItem == null) {
                     brightItem = MaterialHelper.createItemStack("LIGHT_GRAY_STAINED_GLASS_PANE");
                  }

                  ItemStack currentItem;
                  if (brightness < 0.5) {
                     currentItem = darkItem != null ? darkItem : colorPaletteItems[0];
                  } else {
                     currentItem = brightItem != null ? brightItem : colorPaletteItems[colorPaletteItems.length - 1];
                  }

                  ItemStack border = this.createBorderItemFromItemStack(currentItem, borderDisplayName);
                  this.updateAllBorderSlots(
                        inventory, inventorySize, arrowSlot, border, slotMachinePlayerSlot, circularExcludedSlots,
                        verticalExcludedSlots, verticalArrowSlot);
               } else if ("wave".equals(mode)) {
                  int waveSpeed = this.cachedBorderWaveSpeed;
                  int wavePosition = tick / this.cachedBorderAnimationSpeed * waveSpeed;
                  boolean reverseWave = "right-to-left".equals(this.cachedBorderWaveDirection)
                        || "bottom-to-top".equals(this.cachedBorderWaveDirection);
                  List<Integer> slotsToUpdate = this.cachedBorderSlots;
                  if ("slot-machine".equals(this.cachedRollingAnimationType) && slotMachinePlayerSlot >= 0) {
                     slotsToUpdate = new ArrayList<>();

                     for (int s = 0; s < inventorySize; s++) {
                        if (s != slotMachinePlayerSlot) {
                           slotsToUpdate.add(s);
                        }
                     }
                  } else if ("circular".equals(this.cachedRollingAnimationType)) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sx = 0; sx < inventorySize; sx++) {
                        if (!circularExcludedSlots.contains(sx) && sx != arrowSlot) {
                           slotsToUpdate.add(sx);
                        }
                     }
                  } else if ("vertical".equals(this.cachedRollingAnimationType)) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxx = 0; sxx < inventorySize; sxx++) {
                        if (!verticalExcludedSlots.contains(sxx) && sxx != arrowSlot) {
                           slotsToUpdate.add(sxx);
                        }
                     }
                  }

                  for (int i = 0; i < slotsToUpdate.size(); i++) {
                     int slot = slotsToUpdate.get(i);
                     if ((slot != arrowSlot || !"default".equals(this.cachedRollingAnimationType))
                           && slot != verticalArrowSlot
                           && slot != slotMachinePlayerSlot
                           && !circularExcludedSlots.contains(slot)
                           && !verticalExcludedSlots.contains(slot)
                           && slot >= 0
                           && slot < inventorySize) {
                        int slotIndex = reverseWave ? slotsToUpdate.size() - 1 - i : i;
                        int slotWavePos = (wavePosition + slotIndex) % (colorPaletteItems.length * 2);
                        int colorIndex = slotWavePos < colorPaletteItems.length ? slotWavePos
                              : colorPaletteItems.length * 2 - 1 - slotWavePos;
                        colorIndex = Math.max(0, Math.min(colorPaletteItems.length - 1, colorIndex));
                        ItemStack border = this.createBorderItemFromItemStack(colorPaletteItems[colorIndex],
                              borderDisplayName);
                        inventory.setItem(slot, border);
                     }
                  }
               } else if ("gradient".equals(mode)) {
                  List<Integer> slotsToUpdate = this.cachedBorderSlots;
                  if ("slot-machine".equals(this.cachedRollingAnimationType) && slotMachinePlayerSlot >= 0) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxxx = 0; sxxx < inventorySize; sxxx++) {
                        if (sxxx != slotMachinePlayerSlot) {
                           slotsToUpdate.add(sxxx);
                        }
                     }
                  } else if ("circular".equals(this.cachedRollingAnimationType)) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxxxx = 0; sxxxx < inventorySize; sxxxx++) {
                        if (!circularExcludedSlots.contains(sxxxx) && sxxxx != arrowSlot) {
                           slotsToUpdate.add(sxxxx);
                        }
                     }
                  } else if ("vertical".equals(this.cachedRollingAnimationType)) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxxxxx = 0; sxxxxx < inventorySize; sxxxxx++) {
                        if (!verticalExcludedSlots.contains(sxxxxx) && sxxxxx != arrowSlot) {
                           slotsToUpdate.add(sxxxxx);
                        }
                     }
                  }

                  int totalSlots = slotsToUpdate.size();
                  boolean reverseGradient = "right-to-left".equals(this.cachedBorderGradientDirection);

                  for (int ix = 0; ix < totalSlots; ix++) {
                     int slot = slotsToUpdate.get(ix);
                     if (slot != arrowSlot
                           && slot != verticalArrowSlot
                           && slot != slotMachinePlayerSlot
                           && !circularExcludedSlots.contains(slot)
                           && !verticalExcludedSlots.contains(slot)
                           && slot >= 0
                           && slot < inventorySize) {
                        int slotIndex = reverseGradient ? totalSlots - 1 - ix : ix;
                        double gradientPos = (double) slotIndex / Math.max(1, totalSlots - 1);
                        double animOffset = (double) (tick / this.cachedBorderAnimationSpeed
                              % (colorPaletteItems.length * this.cachedBorderGradientSmoothness))
                              / (colorPaletteItems.length * this.cachedBorderGradientSmoothness);
                        gradientPos = (gradientPos + animOffset) % 1.0;
                        int colorIndex = (int) (gradientPos * (colorPaletteItems.length - 1));
                        colorIndex = Math.max(0, Math.min(colorPaletteItems.length - 1, colorIndex));
                        ItemStack border = this.createBorderItemFromItemStack(colorPaletteItems[colorIndex],
                              borderDisplayName);
                        inventory.setItem(slot, border);
                     }
                  }
               } else if ("fade".equals(mode)) {
                  int totalCycle = this.cachedBorderFadeInDuration + this.cachedBorderFadeHoldDuration
                        + this.cachedBorderFadeOutDuration;
                  int cycleTickx = tick * 3 / this.cachedBorderAnimationSpeed % totalCycle;
                  double brightnessx;
                  if (cycleTickx < this.cachedBorderFadeInDuration) {
                     brightnessx = (double) cycleTickx / this.cachedBorderFadeInDuration;
                  } else if (cycleTickx < this.cachedBorderFadeInDuration + this.cachedBorderFadeHoldDuration) {
                     brightnessx = 1.0;
                  } else {
                     int fadeOutTick = cycleTickx - this.cachedBorderFadeInDuration - this.cachedBorderFadeHoldDuration;
                     brightnessx = 1.0 - (double) fadeOutTick / this.cachedBorderFadeOutDuration;
                  }

                  brightnessx = Math.max(0.0, Math.min(1.0, brightnessx));
                  ItemStack darkItemx = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
                  if (darkItemx == null) {
                     darkItemx = MaterialHelper.createItemStack("GRAY_STAINED_GLASS_PANE");
                  }

                  ItemStack brightItemx = MaterialHelper.createItemStack("WHITE_STAINED_GLASS_PANE");
                  if (brightItemx == null) {
                     brightItemx = MaterialHelper.createItemStack("LIGHT_GRAY_STAINED_GLASS_PANE");
                  }

                  if (darkItemx == null && colorPaletteItems.length > 0) {
                     darkItemx = colorPaletteItems[0];
                  }

                  if (brightItemx == null && colorPaletteItems.length > 0) {
                     brightItemx = colorPaletteItems[colorPaletteItems.length - 1];
                  }

                  ItemStack midItem = MaterialHelper.createItemStack("GRAY_STAINED_GLASS_PANE");
                  if (midItem == null && colorPaletteItems.length > 0) {
                     midItem = colorPaletteItems[colorPaletteItems.length / 2];
                  }

                  ItemStack currentItem;
                  if (brightnessx < 0.33) {
                     currentItem = darkItemx;
                  } else if (brightnessx < 0.67) {
                     currentItem = midItem != null ? midItem : darkItemx;
                  } else {
                     currentItem = brightItemx;
                  }

                  if (currentItem == null) {
                     currentItem = darkItemx != null ? darkItemx
                           : (colorPaletteItems.length > 0 ? colorPaletteItems[0] : null);
                  }

                  ItemStack border = this.createBorderItemFromItemStack(currentItem, borderDisplayName);
                  this.updateAllBorderSlots(
                        inventory, inventorySize, arrowSlot, border, slotMachinePlayerSlot, circularExcludedSlots,
                        verticalExcludedSlots, verticalArrowSlot);
               } else if ("random".equals(mode)) {
                  if (tick % (this.cachedBorderRandomChangeFrequency * this.cachedBorderAnimationSpeed) == 0) {
                     List<Integer> slotsToUpdate = this.cachedBorderSlots;
                     if ("slot-machine".equals(this.cachedRollingAnimationType) && slotMachinePlayerSlot >= 0) {
                        slotsToUpdate = new ArrayList<>();

                        for (int sxxxxxx = 0; sxxxxxx < inventorySize; sxxxxxx++) {
                           if (sxxxxxx != slotMachinePlayerSlot && sxxxxxx != arrowSlot) {
                              slotsToUpdate.add(sxxxxxx);
                           }
                        }
                     } else if ("circular".equals(this.cachedRollingAnimationType)) {
                        slotsToUpdate = new ArrayList<>();

                        for (int sxxxxxxx = 0; sxxxxxxx < inventorySize; sxxxxxxx++) {
                           if (!circularExcludedSlots.contains(sxxxxxxx) && sxxxxxxx != arrowSlot) {
                              slotsToUpdate.add(sxxxxxxx);
                           }
                        }
                     } else if ("vertical".equals(this.cachedRollingAnimationType)) {
                        slotsToUpdate = new ArrayList<>();

                        for (int sxxxxxxxx = 0; sxxxxxxxx < inventorySize; sxxxxxxxx++) {
                           if (!verticalExcludedSlots.contains(sxxxxxxxx) && sxxxxxxxx != arrowSlot) {
                              slotsToUpdate.add(sxxxxxxxx);
                           }
                        }
                     }

                     for (int slot : slotsToUpdate) {
                        if (slot != arrowSlot
                              && slot != verticalArrowSlot
                              && slot != slotMachinePlayerSlot
                              && !circularExcludedSlots.contains(slot)
                              && !verticalExcludedSlots.contains(slot)
                              && slot >= 0
                              && slot < inventorySize) {
                           int colorIndex = this.secureRandom.nextInt(colorPaletteItems.length);
                           ItemStack border = this.createBorderItemFromItemStack(colorPaletteItems[colorIndex],
                                 borderDisplayName);
                           inventory.setItem(slot, border);
                        }
                     }
                  }
               } else if ("sequence".equals(mode)) {
                  int sequenceTick = tick / this.cachedBorderAnimationSpeed / this.cachedBorderSequenceDelay * 3;
                  boolean reverseSequence = "right-to-left".equals(this.cachedBorderSequenceDirection);
                  List<Integer> slotsToUpdate = this.cachedBorderSlots;
                  if ("slot-machine".equals(this.cachedRollingAnimationType) && slotMachinePlayerSlot >= 0) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxxxxxxxxx = 0; sxxxxxxxxx < inventorySize; sxxxxxxxxx++) {
                        if (sxxxxxxxxx != slotMachinePlayerSlot) {
                           slotsToUpdate.add(sxxxxxxxxx);
                        }
                     }
                  } else if ("circular".equals(this.cachedRollingAnimationType)) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxxxxxxxxxx = 0; sxxxxxxxxxx < inventorySize; sxxxxxxxxxx++) {
                        if (!circularExcludedSlots.contains(sxxxxxxxxxx) && sxxxxxxxxxx != arrowSlot) {
                           slotsToUpdate.add(sxxxxxxxxxx);
                        }
                     }
                  } else if ("vertical".equals(this.cachedRollingAnimationType)) {
                     slotsToUpdate = new ArrayList<>();

                     for (int sxxxxxxxxxxx = 0; sxxxxxxxxxxx < inventorySize; sxxxxxxxxxxx++) {
                        if (!verticalExcludedSlots.contains(sxxxxxxxxxxx) && sxxxxxxxxxxx != arrowSlot) {
                           slotsToUpdate.add(sxxxxxxxxxxx);
                        }
                     }
                  }

                  ItemStack accentItem = MaterialHelper.createItemStack("LIME_STAINED_GLASS_PANE");
                  if (accentItem == null) {
                     accentItem = MaterialHelper.createItemStack("YELLOW_STAINED_GLASS_PANE");
                  }

                  if (accentItem == null && colorPaletteItems.length > 0) {
                     accentItem = colorPaletteItems[colorPaletteItems.length / 2];
                  }

                  ItemStack backgroundItem = MaterialHelper.createItemStack("BLACK_STAINED_GLASS_PANE");
                  if (backgroundItem == null) {
                     backgroundItem = MaterialHelper.createItemStack("GRAY_STAINED_GLASS_PANE");
                  }

                  if (backgroundItem == null && colorPaletteItems.length > 0) {
                     backgroundItem = colorPaletteItems[0];
                  }

                  int waveWidth = Math.max(1, Math.min(3, slotsToUpdate.size() / 4));

                  for (int ixx = 0; ixx < slotsToUpdate.size(); ixx++) {
                     int slotx = slotsToUpdate.get(ixx);
                     if ((slotx != arrowSlot || !"default".equals(this.cachedRollingAnimationType))
                           && slotx != verticalArrowSlot
                           && slotx != slotMachinePlayerSlot
                           && !circularExcludedSlots.contains(slotx)
                           && !verticalExcludedSlots.contains(slotx)
                           && slotx >= 0
                           && slotx < inventorySize) {
                        int slotIndex = reverseSequence ? slotsToUpdate.size() - 1 - ixx : ixx;
                        int wavePosition = sequenceTick % slotsToUpdate.size();
                        int distanceFromWave = Math.abs(slotIndex - wavePosition);
                        int wrappedDistance = Math.min(distanceFromWave, slotsToUpdate.size() - distanceFromWave);
                        ItemStack currentItemx;
                        if (wrappedDistance < waveWidth) {
                           double fadeFactor = 1.0 - (double) wrappedDistance / waveWidth;
                           if (fadeFactor > 0.3) {
                              currentItemx = accentItem;
                           } else {
                              currentItemx = backgroundItem;
                           }
                        } else {
                           currentItemx = backgroundItem;
                        }

                        ItemStack border = this.createBorderItemFromItemStack(currentItemx, borderDisplayName);
                        inventory.setItem(slotx, border);
                     }
                  }
               }
            } catch (Exception var28) {
               this.plugin.getLogger().warning("Failed to update border animation: " + var28.getMessage());
               var28.printStackTrace();
            }
         }
      }
   }

   private ItemStack createBorderItemFromItemStack(ItemStack source, String displayName) {
      if (source == null) {
         return this.createBorderItem(Material.GLASS_PANE, displayName);
      } else {
         ItemStack border = source.clone();
         ItemMeta borderMeta = border.getItemMeta();
         if (borderMeta != null) {
            this.plugin.getGuiHelper().setDisplayName(borderMeta, displayName);
            border.setItemMeta(borderMeta);
         }

         return border;
      }
   }

   private ItemStack createBorderItem(Material material, String displayName) {
      ItemStack border = MaterialHelper
            .createItemStack(material != null ? material.name() : "BLACK_STAINED_GLASS_PANE");
      if (border == null) {
         border = new ItemStack(material != null ? material : Material.GLASS_PANE);
      }

      ItemMeta borderMeta = border.getItemMeta();
      if (borderMeta != null) {
         this.plugin.getGuiHelper().setDisplayName(borderMeta, displayName);
         border.setItemMeta(borderMeta);
      }

      return border;
   }

   private void updateAllBorderSlots(
         Inventory inventory,
         int inventorySize,
         int arrowSlot,
         ItemStack border,
         int slotMachinePlayerSlot,
         Set<Integer> circularExcludedSlots,
         Set<Integer> verticalExcludedSlots,
         int verticalArrowSlot) {
      if ("slot-machine".equals(this.cachedRollingAnimationType) && slotMachinePlayerSlot >= 0) {
         for (int slot = 0; slot < inventorySize; slot++) {
            if (slot != slotMachinePlayerSlot) {
               inventory.setItem(slot, border.clone());
            }
         }
      } else if ("circular".equals(this.cachedRollingAnimationType)) {
         for (int slotx = 0; slotx < inventorySize; slotx++) {
            if (!circularExcludedSlots.contains(slotx) && slotx != arrowSlot) {
               inventory.setItem(slotx, border.clone());
            }
         }
      } else if ("vertical".equals(this.cachedRollingAnimationType)) {
         for (int slotxx = 0; slotxx < inventorySize; slotxx++) {
            if (!verticalExcludedSlots.contains(slotxx) && slotxx != arrowSlot) {
               inventory.setItem(slotxx, border.clone());
            }
         }
      } else {
         for (int slotxxx : this.cachedBorderSlots) {
            if (slotxxx != arrowSlot
                  && slotxxx != verticalArrowSlot
                  && slotxxx != slotMachinePlayerSlot
                  && !circularExcludedSlots.contains(slotxxx)
                  && !verticalExcludedSlots.contains(slotxxx)
                  && slotxxx >= 0
                  && slotxxx < inventorySize) {
               inventory.setItem(slotxxx, border.clone());
            }
         }
      }
   }

   private boolean updateSlotMachineAnimation(int tick) {
      int effectiveChangeSpeed = Math.max(1,
            (int) (this.cachedSlotMachineChangeSpeed / (1.0 + this.cachedSlotMachineChangeIntensity)));
      if (tick % effectiveChangeSpeed != 0) {
         return false;
      } else {
         ItemStack newItem = this.getScheduledItemForTick(tick);
         if (newItem == null && this.animationPool != null && !this.animationPool.isEmpty()) {
            newItem = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
         }

         if (newItem != null) {
            if (this.cachedSlotMachineGlowOnChange) {
               try {
                  ItemMeta meta = newItem.getItemMeta();
                  if (meta != null) {
                     LegacyCompatibility.setGlowing(meta, this.cachedSlotMachineChangeIntensity > 0.5);
                     newItem.setItemMeta(meta);
                  }
               } catch (Exception var5) {
               }
            }

            int centerSlot = this.cachedSlotMachineCenterSlot;
            if (centerSlot >= 0 && centerSlot < this.cachedInventorySize) {
               this.getInventory().setItem(centerSlot, newItem);
               this.updateInventoryForPlayers();
               if (this.cachedSlotMachineChangeSound && (this.player1 != null || this.player2 != null)) {
                  this.playAnimationTickSound();
               }

               return true;
            }
         }

         return false;
      }
   }

   private void updateSlotMachineAnimationDirect(int tick) {
      int effectiveChangeSpeed = Math.max(1,
            (int) (this.cachedSlotMachineChangeSpeed / (1.0 + this.cachedSlotMachineChangeIntensity)));
      if (tick % effectiveChangeSpeed == 0) {
         ItemStack newItem = this.getScheduledItemForTick(tick);
         if (newItem == null && this.animationPool != null && !this.animationPool.isEmpty()) {
            newItem = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
         }

         if (newItem != null) {
            if (this.cachedSlotMachineGlowOnChange) {
               try {
                  ItemMeta meta = newItem.getItemMeta();
                  if (meta != null) {
                     LegacyCompatibility.setGlowing(meta, this.cachedSlotMachineChangeIntensity > 0.5);
                     newItem.setItemMeta(meta);
                  }
               } catch (Exception var5) {
               }
            }

            int centerSlot = this.cachedSlotMachineCenterSlot;
            if (centerSlot >= 0 && centerSlot < this.cachedInventorySize) {
               this.getInventory().setItem(centerSlot, newItem);
               if (this.cachedSlotMachineChangeSound && (this.player1 != null || this.player2 != null)) {
                  this.playAnimationTickSound();
               }
            }
         }
      }
   }

   private boolean updateCircularAnimation(int tick) {
      int inventorySize = this.cachedInventorySize;
      if (this.cachedCircularPlayerSlots != null && !this.cachedCircularPlayerSlots.isEmpty()) {
         int circleSize = this.cachedCircularPlayerSlots.size();
         ItemStack[] circularItems = new ItemStack[circleSize];
         if (this.animationPool != null && !this.animationPool.isEmpty()) {
            for (int i = 0; i < circleSize; i++) {
               circularItems[i] = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
            }

            double smoothnessFactor = Math.max(1, this.cachedCircularRotationSmoothness) / 10.0;
            int rotationOffset = (int) (tick * this.cachedCircularRotationSpeed * smoothnessFactor
                  * (this.cachedCircularClockwise ? 1 : -1) % circleSize);
            if (rotationOffset < 0) {
               rotationOffset += circleSize;
            }

            boolean needsUpdate = false;

            for (int i = 0; i < circleSize; i++) {
               int itemIndex = (i - rotationOffset + circleSize) % circleSize;
               if (circularItems[itemIndex] != null) {
                  int targetSlot = this.cachedCircularPlayerSlots.get(i);
                  if (targetSlot >= 0 && targetSlot < inventorySize) {
                     ItemStack item = circularItems[itemIndex].clone();
                     if (this.cachedCircularOuterGlow) {
                        try {
                           ItemMeta meta = item.getItemMeta();
                           if (meta != null) {
                              LegacyCompatibility.setGlowing(meta, true);
                              item.setItemMeta(meta);
                           }
                        } catch (Exception var14) {
                        }
                     }

                     this.getInventory().setItem(targetSlot, item);
                     needsUpdate = true;
                  }
               }
            }

            if (needsUpdate) {
               this.updateInventoryForPlayers();
            }

            return needsUpdate;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean updateVerticalAnimation(int tick) {
      int inventorySize = this.cachedInventorySize;
      if (this.cachedVerticalPlayerSlots != null && !this.cachedVerticalPlayerSlots.isEmpty()) {
         int slotCount = this.cachedVerticalPlayerSlots.size();
         ItemStack[] verticalItems = new ItemStack[slotCount];
         if (this.animationPool != null && !this.animationPool.isEmpty()) {
            for (int i = 0; i < slotCount; i++) {
               verticalItems[i] = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
            }

            int frameSpeed = this.cachedAnimationSpeed;
            if (tick % frameSpeed != 0) {
               return false;
            } else {
               for (int i = slotCount - 1; i > 0; i--) {
                  if (verticalItems[i - 1] != null) {
                     verticalItems[i] = verticalItems[i - 1].clone();
                  }
               }

               ItemStack newItem = this.getScheduledItemForTick(tick);
               if (newItem == null && this.animationPool != null && !this.animationPool.isEmpty()) {
                  newItem = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
               }

               if (newItem != null) {
                  verticalItems[0] = newItem;
               }

               boolean needsUpdate = false;

               for (int ix = 0; ix < slotCount; ix++) {
                  int slot = this.cachedVerticalPlayerSlots.get(ix);
                  if (slot >= 0 && slot < inventorySize && verticalItems[ix] != null) {
                     ItemStack item = verticalItems[ix].clone();
                     this.getInventory().setItem(slot, item);
                     needsUpdate = true;
                  }
               }

               int arrowSlot = this.cachedVerticalArrowSlot;
               if (arrowSlot >= 0 && arrowSlot < inventorySize) {
                  Inventory inventory = this.getInventory();
                  ItemStack currentArrowItem = inventory.getItem(arrowSlot);
                  boolean needsArrowRestore = false;
                  if (currentArrowItem == null) {
                     needsArrowRestore = true;
                  } else if (!MaterialHelper.isPlayerHead(currentArrowItem.getType())) {
                     needsArrowRestore = true;
                  }

                  if (needsArrowRestore) {
                     this.setupVerticalArrowSlot();
                     needsUpdate = true;
                  }
               }

               if (needsUpdate) {
                  this.updateInventoryForPlayers();
               }

               return needsUpdate;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private void updateCircularAnimationDirect(int tick) {
      int inventorySize = this.cachedInventorySize;
      if (this.cachedCircularPlayerSlots != null && !this.cachedCircularPlayerSlots.isEmpty()) {
         int circleSize = this.cachedCircularPlayerSlots.size();
         ItemStack[] circularItems = new ItemStack[circleSize];
         if (this.animationPool != null && !this.animationPool.isEmpty()) {
            for (int i = 0; i < circleSize; i++) {
               circularItems[i] = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
            }

            double smoothnessFactor = Math.max(1, this.cachedCircularRotationSmoothness) / 10.0;
            int rotationOffset = (int) (tick * this.cachedCircularRotationSpeed * smoothnessFactor
                  * (this.cachedCircularClockwise ? 1 : -1) % circleSize);
            if (rotationOffset < 0) {
               rotationOffset += circleSize;
            }

            for (int i = 0; i < circleSize; i++) {
               int itemIndex = (i - rotationOffset + circleSize) % circleSize;
               if (circularItems[itemIndex] != null) {
                  int targetSlot = this.cachedCircularPlayerSlots.get(i);
                  if (targetSlot >= 0 && targetSlot < inventorySize) {
                     ItemStack item = circularItems[itemIndex].clone();
                     if (this.cachedCircularOuterGlow) {
                        try {
                           ItemMeta meta = item.getItemMeta();
                           if (meta != null) {
                              LegacyCompatibility.setGlowing(meta, true);
                              item.setItemMeta(meta);
                           }
                        } catch (Exception var13) {
                        }
                     }

                     this.getInventory().setItem(targetSlot, item);
                  }
               }
            }
         }
      }
   }

   private void updateVerticalAnimationDirect(int tick) {
      int inventorySize = this.cachedInventorySize;
      if (this.cachedVerticalPlayerSlots != null && !this.cachedVerticalPlayerSlots.isEmpty()) {
         int slotCount = this.cachedVerticalPlayerSlots.size();
         ItemStack[] verticalItems = new ItemStack[slotCount];
         if (this.animationPool != null && !this.animationPool.isEmpty()) {
            for (int i = 0; i < slotCount; i++) {
               verticalItems[i] = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
            }

            int frameSpeed = this.cachedAnimationSpeed;
            if (tick % frameSpeed == 0) {
               for (int i = slotCount - 1; i > 0; i--) {
                  if (verticalItems[i - 1] != null) {
                     verticalItems[i] = verticalItems[i - 1].clone();
                  }
               }

               ItemStack newItem = this.getScheduledItemForTick(tick);
               if (newItem == null && this.animationPool != null && !this.animationPool.isEmpty()) {
                  newItem = this.animationPool.get(this.secureRandom.nextInt(this.animationPool.size())).clone();
               }

               if (newItem != null) {
                  verticalItems[0] = newItem;
               }

               for (int ix = 0; ix < slotCount; ix++) {
                  int slot = this.cachedVerticalPlayerSlots.get(ix);
                  if (slot >= 0 && slot < inventorySize && verticalItems[ix] != null) {
                     ItemStack item = verticalItems[ix].clone();
                     this.getInventory().setItem(slot, item);
                  }
               }

               int arrowSlot = this.cachedVerticalArrowSlot;
               if (arrowSlot >= 0 && arrowSlot < inventorySize) {
                  Inventory inventory = this.getInventory();
                  ItemStack currentArrowItem = inventory.getItem(arrowSlot);
                  boolean needsArrowRestore = false;
                  if (currentArrowItem == null) {
                     needsArrowRestore = true;
                  } else if (!MaterialHelper.isPlayerHead(currentArrowItem.getType())) {
                     needsArrowRestore = true;
                  }

                  if (needsArrowRestore) {
                     this.setupVerticalArrowSlot();
                  }
               }
            }
         }
      }
   }

   private void setupVerticalArrowSlot() {
      try {
         int arrowSlot = this.cachedVerticalArrowSlot;
         int inventorySize = this.getInventory().getSize();
         if (arrowSlot < 0 || arrowSlot >= inventorySize) {
            this.plugin.getLogger().warning("Invalid vertical arrow slot: " + arrowSlot + ". Using default: 12");
            arrowSlot = 12;
            this.cachedVerticalArrowSlot = arrowSlot;
         }

         String arrowTexture = this.plugin
               .getGUIConfig()
               .getString(
                     "coinflip-gui.layout.player-slots.vertical.arrow-texture",
                     "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjFlMWU3MzBjNzcyNzljOGUyZTE1ZDhiMjcxYTExN2U1ZTJjYTkzZDI1YzhiZTNhMDBjYzkyYTAwY2MwYmI4NSJ9fX0=");
         String arrowName = this.plugin.getGUIConfig().getString("coinflip-gui.layout.player-slots.vertical.arrow-name",
               "⬇️ WINNER ⬇️");
         Material playerHeadMaterial = MaterialHelper.getPlayerHeadMaterial();
         if (playerHeadMaterial == null) {
            this.plugin.getLogger().warning("Failed to parse PLAYER_HEAD material for arrow head!");
            return;
         }

         ItemStack arrowHead = this.plugin.getGuiHelper().createPlayerHead(playerHeadMaterial, null, arrowTexture,
               false, arrowName, new ArrayList());
         if (arrowHead != null) {
            this.getInventory().setItem(arrowSlot, arrowHead);
         }
      } catch (Exception var7) {
         this.plugin.getLogger().severe("Failed to setup vertical arrow slot: " + var7.getMessage());
         var7.printStackTrace();
      }
   }

   private void updateInventoryForPlayers() {
      boolean bothClosed = this.isBotGame
            ? this.playersClosedGUI.contains(this.player1UUID)
            : this.playersClosedGUI.contains(this.player1UUID) && this.playersClosedGUI.contains(this.player2UUID);
      if (bothClosed) {
         if (this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin
                  .getDebugManager()
                  .verbose(DebugManager.Category.GUI,
                        "updateInventoryForPlayers() skipped: both players closed GUI (tracked via onClose)");
         }
      } else {
         boolean player1Updated = false;
         boolean player2Updated = false;
         if (this.player1 != null && this.player1.isOnline() && !this.playersClosedGUI.contains(this.player1UUID)) {
            try {
               this.player1.updateInventory();
               player1Updated = true;
            } catch (Exception var6) {
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin.getDebugManager().verbose(DebugManager.Category.GUI,
                        "Error updating inventory for player1: " + var6.getMessage());
               }
            }
         }

         if (!this.isBotGame && this.player2 != null && this.player2.isOnline()
               && !this.playersClosedGUI.contains(this.player2UUID)) {
            try {
               this.player2.updateInventory();
               player2Updated = true;
            } catch (Exception var5) {
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin.getDebugManager().verbose(DebugManager.Category.GUI,
                        "Error updating inventory for player2: " + var5.getMessage());
               }
            }
         }

         if (this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            this.plugin
                  .getDebugManager()
                  .verbose(DebugManager.Category.GUI, "updateInventoryForPlayers() - Player1 updated: " + player1Updated
                        + ", Player2 updated: " + player2Updated);
         }
      }
   }

   private void playAnimationTickSound() {
      if (this.cachedAudioDuringRollEnabled) {
         String soundName = this.cachedAudioDuringRollSound;
         float volume = this.cachedAudioDuringRollVolume;
         float pitch = this.cachedAudioDuringRollPitch;

         try {
            if (this.player1 != null
                  && this.player1.isOnline()
                  && !this.playersClosedGUI.contains(this.player1UUID)
                  && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1,
                        "notification-animation-sound")) {
               this.plugin.getAdventureHelper().playSound(this.player1, soundName, volume, pitch);
            }
         } catch (Exception var6) {
         }

         if (!this.isBotGame) {
            try {
               if (this.player2 != null
                     && this.player2.isOnline()
                     && !this.playersClosedGUI.contains(this.player2UUID)
                     && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player2,
                           "notification-animation-sound")) {
                  this.plugin.getAdventureHelper().playSound(this.player2, soundName, volume, pitch);
               }
            } catch (Exception var5) {
            }
         }
      }
   }

   private void finishRoll() {
      if (this.finished) {
         this.plugin.getLogger().warning("finishRoll() called multiple times. Ignoring duplicate call.");
      } else {
         this.finished = true;
         this.animationRunning = false;

         try {
            if (this.isBotGame) {
               this.finishBotGame();
               return;
            }

            if (this.player1 == null || this.player2 == null) {
               this.plugin.getLogger()
                     .warning("finishRoll() called but player1 or player2 is null. Game may have been refunded.");
               return;
            }

            // CRITICAL: Immediately unregister the game and remove the LOSER's backup
            // This MUST happen before any other logic to prevent the exploit where
            // the loser disconnects quickly to avoid losing money
            Player winner = this.winner;
            Player loser = this.loser;
            if (winner != null && loser != null) {
               // Remove the loser's backup FIRST - they cannot get a refund after losing
               this.plugin.getCoinFlipManager().removeBackupForPlayer(loser.getUniqueId());
               // Unregister the game to prevent refundRollingGame from running
               this.plugin.getCoinFlipManager().unregisterRollingGame(this.player1UUID, this.player2UUID);
               
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin
                        .getDebugManager()
                        .info(
                              DebugManager.Category.GUI,
                              "finishRoll() - Immediately removed loser backup and unregistered game to prevent exploit. Winner: " 
                                    + winner.getName() + ", Loser: " + loser.getName());
               }
            } else {
               this.plugin.getLogger()
                     .warning("finishRoll() called but winner or loser is null. Game may have been refunded.");
               return;
            }

            boolean winnerOnline = winner.isOnline();
            boolean loserOnline = loser.isOnline();
            if (winnerOnline || loserOnline) {
               this.playAnimationCompleteSound();
               int inventorySize = this.cachedInventorySize;
               ItemStack winnerItem = this.cachedWinnerHead != null ? this.cachedWinnerHead.clone()
                     : this.createPlayerItem("winner", winner);
               ItemStack loserItem = this.cachedLoserHead != null ? this.cachedLoserHead.clone()
                     : this.createPlayerItem("loser", loser);
               if (winnerItem == null || loserItem == null) {
                  this.plugin
                        .getLogger()
                        .warning("Failed to create winner/loser items. Winner item: " + (winnerItem != null)
                              + ", Loser item: " + (loserItem != null));
               }

               String animationType = this.cachedRollingAnimationType != null
                     ? this.cachedRollingAnimationType.toLowerCase()
                     : "default";
               Inventory inventory = this.getInventory();
               if ("slot-machine".equals(animationType)) {
                  int centerSlot = this.cachedSlotMachineCenterSlot;
                  if (winnerItem != null && centerSlot >= 0 && centerSlot < inventorySize) {
                     inventory.setItem(centerSlot, winnerItem.clone());
                  }
               } else if ("circular".equals(animationType)) {
                  int winnerSlot = 20;
                  if (winnerItem != null && winnerSlot >= 0 && winnerSlot < inventorySize) {
                     inventory.setItem(winnerSlot, winnerItem.clone());
                  }
               } else if ("vertical".equals(animationType)) {
                  int winnerSlot = 22;
                  if (winnerItem != null && winnerSlot >= 0 && winnerSlot < inventorySize) {
                     inventory.setItem(winnerSlot, winnerItem.clone());
                  }
               } else {
                  int resultSlot = this.cachedResultSlot;
                  if (resultSlot < 0 || resultSlot >= inventorySize) {
                     this.plugin.getLogger()
                           .warning("Invalid result slot: " + resultSlot + ". Using center of animation row.");
                     resultSlot = this.cachedAnimationStartSlot + this.cachedAnimationSlotCount / 2;
                  }

                  if (this.animationItems != null && this.animationItems.length > 0) {
                     for (int i = 0; i < this.cachedAnimationSlotCount && i < this.animationItems.length; i++) {
                        int slot = this.cachedAnimationStartSlot + i;
                        if (slot >= 0 && slot < inventorySize) {
                           if (slot == resultSlot) {
                              if (winnerItem != null) {
                                 inventory.setItem(slot, winnerItem.clone());
                              }
                           } else if (this.animationItems[i] != null) {
                              inventory.setItem(slot, this.animationItems[i].clone());
                           } else {
                              ItemStack currentItem = inventory.getItem(slot);
                              if (currentItem == null) {
                                 int resultSlotIndex = resultSlot - this.cachedAnimationStartSlot;
                                 boolean shouldBeWinner = Math.abs(i - resultSlotIndex) % 2 == 0;
                                 ItemStack itemToSet = shouldBeWinner ? winnerItem : loserItem;
                                 if (itemToSet != null) {
                                    inventory.setItem(slot, itemToSet.clone());
                                 }
                              }
                           }
                        }
                     }
                  }

                  if (winnerItem != null && resultSlot >= 0 && resultSlot < inventorySize) {
                     ItemStack currentResultItem = inventory.getItem(resultSlot);
                     if (currentResultItem == null || !this.isWinnerItem(currentResultItem, winner)) {
                        inventory.setItem(resultSlot, winnerItem.clone());
                     }
                  }
               }

               this.updateInventoryForPlayers();
               String unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
               double totalPot = this.amount * 2.0;
               double taxRate = this.plugin.getTaxRateCalculator().calculateTaxRate(winner, this.amount,
                     this.currencyType, this.currencyId);
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.CURRENCY)) {
                  this.plugin
                        .getDebugManager()
                        .info(
                              DebugManager.Category.CURRENCY,
                              "CoinFlipRollGUI: Tax calculation - Player="
                                    + (winner != null ? winner.getName() : "null")
                                    + ", Amount="
                                    + this.amount
                                    + ", Currency="
                                    + this.currencyType
                                    + ":"
                                    + this.currencyId
                                    + ", TaxRate="
                                    + taxRate
                                    + ", TotalPot="
                                    + totalPot);
               }

               double tax = Math.round(totalPot * taxRate * 100.0) / 100.0;
               double taxedAmount = Math.round(totalPot * (1.0 - taxRate) * 100.0) / 100.0;
               double minWinAmount = this.amount;
               if (taxedAmount < minWinAmount && taxRate < 1.0) {
                  double maxTax = totalPot - minWinAmount;
                  tax = Math.round(maxTax * 100.0) / 100.0;
                  taxedAmount = Math.round((totalPot - tax) * 100.0) / 100.0;
                  this.plugin
                        .getLogger()
                        .warning("Adjusted tax to ensure winner receives at least their bet. Tax: " + tax
                              + ", Winner receives: " + taxedAmount);
               }

               if (taxedAmount > totalPot) {
                  taxedAmount = totalPot;
                  tax = 0.0;
                  this.plugin.getLogger().warning(
                        "Tax calculation error: Winner amount exceeded total pot. Setting to total pot with no tax.");
               }

               double actualTax = totalPot - taxedAmount;
               double actualTaxRate = totalPot > 0.0 ? actualTax / totalPot : 0.0;
               Map<String, String> winnerPlaceholders = new HashMap<>();
               winnerPlaceholders.put("prefix", this.plugin.getMessage("prefix"));
               winnerPlaceholders.put("loser", loserOnline ? loser.getName() : "Unknown");
               winnerPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(taxedAmount));
               winnerPlaceholders.put("symbol", unit);
               winnerPlaceholders.put("tax_rate", String.valueOf((int) (actualTaxRate * 100.0)));
               winnerPlaceholders.put("tax", this.plugin.getGuiHelper().formatAmount(actualTax));
               Map<String, String> loserPlaceholders = new HashMap<>();
               loserPlaceholders.put("prefix", this.plugin.getMessage("prefix"));
               loserPlaceholders.put("winner", winnerOnline ? winner.getName() : "Unknown");
               loserPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(this.amount));
               loserPlaceholders.put("symbol", unit);
               boolean titlesEnabled = this.cachedTitlesEnabled;
               boolean actionbarEnabled = this.cachedActionbarEnabled;
               boolean bossbarEnabled = this.cachedBossbarEnabled;
               if (winnerOnline) {
                  UUID winnerUUID = null;
                  UUID loserUUID = null;

                  try {
                     if (winner != null) {
                        winnerUUID = winner.getUniqueId();
                     }

                     if (loser != null) {
                        loserUUID = loser.getUniqueId();
                     }
                  } catch (Exception var64) {
                     this.plugin.getLogger()
                           .warning("Failed to get UUIDs for consecutive wins tracking: " + var64.getMessage());
                  }

                  int newConsecutiveWins = 0;
                  if (winnerUUID != null && loserUUID != null) {
                     newConsecutiveWins = this.plugin.getCoinFlipManager().incrementConsecutiveWins(winnerUUID,
                           loserUUID);
                     this.plugin.getCoinFlipManager().resetConsecutiveWins(loserUUID, winnerUUID);
                  }

                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(winner, "message-game-won")) {
                     String winnerMsg = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.winner");
                     this.plugin.getAdventureHelper().sendMessage(winner, winnerMsg, winnerPlaceholders);
                  }

                  if (newConsecutiveWins > 1) {
                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(winner,
                           "notification-consecutive-win-sound")) {
                        this.plugin.getSoundHelper().playSound(winner, "game.consecutive-win");
                     }

                     if (this.plugin.getPlayerSettingsManager().isSettingEnabled(winner, "message-consecutive-wins")) {
                        String consecutiveWinsMsgKey = this.plugin.getMessage("game.consecutive-win");
                        if (consecutiveWinsMsgKey != null && !consecutiveWinsMsgKey.equals("game.consecutive-win")
                              && !consecutiveWinsMsgKey.trim().isEmpty()) {
                           Map<String, String> consecutiveWinsPlaceholders = new HashMap<>();
                           consecutiveWinsPlaceholders.put("prefix", this.plugin.getMessage("prefix"));
                           consecutiveWinsPlaceholders.put("count", String.valueOf(newConsecutiveWins));
                           String opponentName = "Unknown";
                           if (loser != null) {
                              try {
                                 String name = loser.getName();
                                 opponentName = name != null && !name.trim().isEmpty() ? name : "Unknown";
                              } catch (Exception var65) {
                                 opponentName = "Unknown";
                              }
                           }

                           consecutiveWinsPlaceholders.put("opponent", opponentName);
                           String ordinalSuffix = this.getOrdinalSuffix(newConsecutiveWins);
                           consecutiveWinsPlaceholders.put("ordinal", ordinalSuffix);
                           String fullConsecutiveWinsMsg = this.plugin.getMessage("prefix") + " "
                                 + consecutiveWinsMsgKey;
                           this.plugin.getAdventureHelper().sendMessage(winner, fullConsecutiveWinsMsg,
                                 consecutiveWinsPlaceholders);
                        }
                     }
                  }

                  if (titlesEnabled
                        && this.plugin.getPlayerSettingsManager().isSettingEnabled(winner, "notification-title")) {
                     Map<String, String> winnerTitlePlaceholders = new HashMap<>();
                     winnerTitlePlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(taxedAmount));
                     winnerTitlePlaceholders.put("symbol", unit);
                     this.plugin
                           .getAdventureHelper()
                           .sendTitle(
                                 winner,
                                 this.plugin.getConfigManager().getMessages().getString("titles.game-win-title",
                                       "&a&lYOU WON!"),
                                 this.plugin.getConfigManager().getMessages().getString("titles.game-win-subtitle",
                                       "&7Received &a<amount><symbol>"),
                                 winnerTitlePlaceholders);
                  }

                  if (actionbarEnabled
                        && this.plugin.getPlayerSettingsManager().isSettingEnabled(winner, "notification-actionbar")) {
                     Map<String, String> winnerActionbarPlaceholders = new HashMap<>();
                     winnerActionbarPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(taxedAmount));
                     winnerActionbarPlaceholders.put("symbol", unit);
                     String winnerActionbarMsg = this.plugin
                           .getConfigManager()
                           .getMessages()
                           .getString("actionbar.game-win", "&a&lYOU WON! &fReceived &a<amount><symbol>");
                     this.plugin.getAdventureHelper().sendActionBar(winner, winnerActionbarMsg,
                           winnerActionbarPlaceholders);
                  }

                  if (bossbarEnabled
                        && this.plugin.getPlayerSettingsManager().isSettingEnabled(winner, "notification-bossbar")) {
                     Map<String, String> winnerBossbarPlaceholders = new HashMap<>();
                     winnerBossbarPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(taxedAmount));
                     winnerBossbarPlaceholders.put("symbol", unit);
                     String winnerBossbarMsg = this.plugin
                           .getConfigManager()
                           .getMessages()
                           .getString("bossbar.game-win", "&a&lYOU WON! &fReceived &a<amount><symbol>");
                     String winnerBossbarColor = this.cachedWinnerBossbarColor;
                     String winnerBossbarOverlay = this.cachedWinnerBossbarOverlay;
                     float winnerBossbarProgress = this.cachedWinnerBossbarProgress;
                     int winnerBossbarDuration = this.cachedBossbarDuration;
                     BossBar winnerBossBar = this.plugin
                           .getAdventureHelper()
                           .sendBossBar(
                                 winner,
                                 winnerBossbarMsg,
                                 winnerBossbarProgress,
                                 winnerBossbarColor,
                                 winnerBossbarOverlay,
                                 winnerBossbarDuration,
                                 winnerBossbarPlaceholders);
                     if (winnerBossBar != null && winnerBossbarDuration > 0) {
                        FoliaScheduler.runTaskLater(this.plugin, winner, () -> {
                           if (winner != null && winner.isOnline()) {
                              this.plugin.getAdventureHelper().removeBossBar(winner, winnerBossBar);
                           }
                        }, winnerBossbarDuration * 20L);
                     }
                  }

                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(winner, "notification-sound")) {
                     this.plugin.getSoundHelper().playSound(winner, "game.win");
                  }
               }

               if (loserOnline) {
                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(loser, "message-game-lost")) {
                     String loserMsg = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("game.loser");
                     this.plugin.getAdventureHelper().sendMessage(loser, loserMsg, loserPlaceholders);
                  }

                  if (titlesEnabled
                        && this.plugin.getPlayerSettingsManager().isSettingEnabled(loser, "notification-title")) {
                     Map<String, String> loserTitlePlaceholders = new HashMap<>();
                     loserTitlePlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(this.amount));
                     loserTitlePlaceholders.put("symbol", unit);
                     this.plugin
                           .getAdventureHelper()
                           .sendTitle(
                                 loser,
                                 this.plugin.getConfigManager().getMessages().getString("titles.game-lose-title",
                                       "&c&lYOU LOST!"),
                                 this.plugin.getConfigManager().getMessages().getString("titles.game-lose-subtitle",
                                       "&7Lost &c<amount><symbol>"),
                                 loserTitlePlaceholders);
                  }

                  if (actionbarEnabled
                        && this.plugin.getPlayerSettingsManager().isSettingEnabled(loser, "notification-actionbar")) {
                     Map<String, String> loserActionbarPlaceholders = new HashMap<>();
                     loserActionbarPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(this.amount));
                     loserActionbarPlaceholders.put("symbol", unit);
                     String loserActionbarMsg = this.plugin
                           .getConfigManager()
                           .getMessages()
                           .getString("actionbar.game-lose", "&c&lYOU LOST! &fLost &c<amount><symbol>");
                     this.plugin.getAdventureHelper().sendActionBar(loser, loserActionbarMsg,
                           loserActionbarPlaceholders);
                  }

                  if (bossbarEnabled
                        && this.plugin.getPlayerSettingsManager().isSettingEnabled(loser, "notification-bossbar")) {
                     Map<String, String> loserBossbarPlaceholders = new HashMap<>();
                     loserBossbarPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(this.amount));
                     loserBossbarPlaceholders.put("symbol", unit);
                     String loserBossbarMsg = this.plugin
                           .getConfigManager()
                           .getMessages()
                           .getString("bossbar.game-lose", "&c&lYOU LOST! &fLost &c<amount><symbol>");
                     String loserBossbarColor = this.cachedLoserBossbarColor;
                     String loserBossbarOverlay = this.cachedLoserBossbarOverlay;
                     float loserBossbarProgress = this.cachedLoserBossbarProgress;
                     int loserBossbarDuration = this.cachedBossbarDuration;
                     BossBar loserBossBar = this.plugin
                           .getAdventureHelper()
                           .sendBossBar(
                                 loser, loserBossbarMsg, loserBossbarProgress, loserBossbarColor, loserBossbarOverlay,
                                 loserBossbarDuration, loserBossbarPlaceholders);
                     if (loserBossBar != null && loserBossbarDuration > 0) {
                        FoliaScheduler.runTaskLater(this.plugin, loser, () -> {
                           if (loser != null && loser.isOnline()) {
                              this.plugin.getAdventureHelper().removeBossBar(loser, loserBossBar);
                           }
                        }, loserBossbarDuration * 20L);
                     }
                  }

                  if (this.plugin.getPlayerSettingsManager().isSettingEnabled(loser, "notification-sound")) {
                     this.plugin.getSoundHelper().playSound(loser, "game.lose");
                  }
               }

               CurrencySettings currencySettings = this.plugin.getCurrencyManager()
                     .getCurrencySettings(this.currencyType, this.currencyId);
               boolean broadcastEnabled = currencySettings != null && currencySettings.isBroadcastEnabled();
               double minBroadcastAmount = currencySettings != null ? currencySettings.getMinBroadcastAmount() : 0.0;
               if (broadcastEnabled && this.amount >= minBroadcastAmount && winnerOnline && loserOnline) {
                  Map<String, String> broadcastPlaceholders = new HashMap<>();
                  broadcastPlaceholders.put("prefix", this.plugin.getMessage("prefix"));
                  broadcastPlaceholders.put("winner", winner.getName());
                  broadcastPlaceholders.put("loser", loser.getName());
                  broadcastPlaceholders.put("amount", this.plugin.getGuiHelper().formatAmount(this.amount));
                  broadcastPlaceholders.put("symbol", unit);
                  broadcastPlaceholders.put("taxed_amount", this.plugin.getGuiHelper().formatAmount(taxedAmount));
                  String broadcastMsg = this.plugin.getMessage("prefix") + " "
                        + this.plugin.getMessage("game.broadcast-result");
                  this.plugin
                        .getAdventureHelper()
                        .broadcastWithFilter(
                              broadcastMsg, broadcastPlaceholders, p -> this.plugin.getPlayerSettingsManager()
                                    .isSettingEnabled(p, "message-game-result-broadcast"));
               }

               if (winnerOnline && loserOnline) {
                  String currencyDisplayName = this.plugin.getCurrencyManager().getDisplayName(this.currencyType,
                        this.currencyId);
                  this.plugin.getDiscordWebhookHandler().sendGameResult(winner.getName(), loser.getName(), this.amount,
                        taxedAmount, currencyDisplayName, unit);
               }

               if (winnerOnline) {
                  this.plugin.getCurrencyManager().deposit(winner, this.currencyType, this.currencyId, taxedAmount);
                  this.plugin.getCoinFlipManager().removeBackupForPlayer(winner.getUniqueId());
               } else {
                  try {
                     this.plugin.getCoinFlipManager().saveBackupForRefund(winner.getUniqueId(), this.currencyType,
                           this.currencyId, taxedAmount);
                     if (this.plugin.getDebugManager() != null
                           && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                        this.plugin
                              .getDebugManager()
                              .info(
                                    DebugManager.Category.GUI, "Winner " + winner.getUniqueId()
                                          + " disconnected. Saved backup with winnings amount: " + taxedAmount);
                     }
                  } catch (Exception var63) {
                     this.plugin.getLogger().severe("Failed to save backup for disconnected winner "
                           + winner.getUniqueId() + ": " + var63.getMessage());
                     var63.printStackTrace();
                  }
               }

               // Note: Loser backup already removed at the start of finishRoll() to prevent exploit
               if (winnerOnline) {
                  PlayerStats winnerStats = this.plugin.getCoinFlipManager().getStats(winner.getUniqueId());
                  winnerStats.setWins(winnerStats.getWins() + 1);
                  winnerStats.setWinstreak(winnerStats.getWinstreak() + 1);
                  double winnerNetProfit = taxedAmount - this.amount;
                  if (this.currencyType == CoinFlipGame.CurrencyType.MONEY) {
                     winnerStats.setProfitMoney(winnerStats.getProfitMoney() + Math.max(0.0, winnerNetProfit));
                     winnerStats.setWinsMoney(winnerStats.getWinsMoney() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                     winnerStats
                           .setProfitPlayerPoints(winnerStats.getProfitPlayerPoints() + Math.max(0.0, winnerNetProfit));
                     winnerStats.setWinsPlayerPoints(winnerStats.getWinsPlayerPoints() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                     winnerStats
                           .setProfitTokenManager(winnerStats.getProfitTokenManager() + Math.max(0.0, winnerNetProfit));
                     winnerStats.setWinsTokenManager(winnerStats.getWinsTokenManager() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                     winnerStats
                           .setProfitBeastTokens(winnerStats.getProfitBeastTokens() + Math.max(0.0, winnerNetProfit));
                     winnerStats.setWinsBeastTokens(winnerStats.getWinsBeastTokens() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER && this.currencyId != null) {
                     winnerStats.incrementWinsPlaceholder(this.currencyId);
                  }

                  this.plugin.getCoinFlipManager().saveStats(winner.getUniqueId(), winnerStats);
               }

               if (loserOnline) {
                  PlayerStats loserStats = this.plugin.getCoinFlipManager().getStats(loser.getUniqueId());
                  loserStats.setDefeats(loserStats.getDefeats() + 1);
                  loserStats.setWinstreak(0);
                  double loserLoss = this.amount;
                  if (this.currencyType == CoinFlipGame.CurrencyType.MONEY) {
                     loserStats.setLossMoney(loserStats.getLossMoney() + loserLoss);
                     loserStats.setDefeatsMoney(loserStats.getDefeatsMoney() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                     loserStats.setLossPlayerPoints(loserStats.getLossPlayerPoints() + loserLoss);
                     loserStats.setDefeatsPlayerPoints(loserStats.getDefeatsPlayerPoints() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                     loserStats.setLossTokenManager(loserStats.getLossTokenManager() + loserLoss);
                     loserStats.setDefeatsTokenManager(loserStats.getDefeatsTokenManager() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                     loserStats.setLossBeastTokens(loserStats.getLossBeastTokens() + loserLoss);
                     loserStats.setDefeatsBeastTokens(loserStats.getDefeatsBeastTokens() + 1);
                  } else if (this.currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER && this.currencyId != null) {
                     loserStats.incrementDefeatsPlaceholder(this.currencyId);
                  }

                  this.plugin.getCoinFlipManager().saveStats(loser.getUniqueId(), loserStats);
               }

               if (!winnerOnline || !loserOnline) {
                  double finalTaxedAmount = taxedAmount;
                  FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
                     try {
                        if (!winnerOnline) {
                           try {
                              PlayerStats offlineWinnerStats = this.plugin.getCoinFlipManager()
                                    .getStats(winner.getUniqueId());
                              offlineWinnerStats.setWins(offlineWinnerStats.getWins() + 1);
                              offlineWinnerStats.setWinstreak(offlineWinnerStats.getWinstreak() + 1);
                              double offlineWinnerNetProfit = finalTaxedAmount - this.amount;
                              if (this.currencyType == CoinFlipGame.CurrencyType.MONEY) {
                                 offlineWinnerStats.setProfitMoney(
                                       offlineWinnerStats.getProfitMoney() + Math.max(0.0, offlineWinnerNetProfit));
                                 offlineWinnerStats.setWinsMoney(offlineWinnerStats.getWinsMoney() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                                 offlineWinnerStats.setProfitPlayerPoints(offlineWinnerStats.getProfitPlayerPoints()
                                       + Math.max(0.0, offlineWinnerNetProfit));
                                 offlineWinnerStats.setWinsPlayerPoints(offlineWinnerStats.getWinsPlayerPoints() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                                 offlineWinnerStats.setProfitTokenManager(offlineWinnerStats.getProfitTokenManager()
                                       + Math.max(0.0, offlineWinnerNetProfit));
                                 offlineWinnerStats.setWinsTokenManager(offlineWinnerStats.getWinsTokenManager() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                                 offlineWinnerStats.setProfitBeastTokens(offlineWinnerStats.getProfitBeastTokens()
                                       + Math.max(0.0, offlineWinnerNetProfit));
                                 offlineWinnerStats.setWinsBeastTokens(offlineWinnerStats.getWinsBeastTokens() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER
                                    && this.currencyId != null) {
                                 offlineWinnerStats.incrementWinsPlaceholder(this.currencyId);
                              }

                              this.plugin.getCoinFlipManager().saveStats(winner.getUniqueId(), offlineWinnerStats);
                           } catch (Exception var11x) {
                              this.plugin.getLogger().warning("Failed to update stats for offline winner "
                                    + winner.getUniqueId() + ": " + var11x.getMessage());
                           }
                        }

                        if (!loserOnline) {
                           try {
                              PlayerStats offlineLoserStats = this.plugin.getCoinFlipManager()
                                    .getStats(loser.getUniqueId());
                              offlineLoserStats.setDefeats(offlineLoserStats.getDefeats() + 1);
                              offlineLoserStats.setWinstreak(0);
                              double offlineLoserLoss = this.amount;
                              if (this.currencyType == CoinFlipGame.CurrencyType.MONEY) {
                                 offlineLoserStats.setLossMoney(offlineLoserStats.getLossMoney() + offlineLoserLoss);
                                 offlineLoserStats.setDefeatsMoney(offlineLoserStats.getDefeatsMoney() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
                                 offlineLoserStats
                                       .setLossPlayerPoints(offlineLoserStats.getLossPlayerPoints() + offlineLoserLoss);
                                 offlineLoserStats
                                       .setDefeatsPlayerPoints(offlineLoserStats.getDefeatsPlayerPoints() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
                                 offlineLoserStats
                                       .setLossTokenManager(offlineLoserStats.getLossTokenManager() + offlineLoserLoss);
                                 offlineLoserStats
                                       .setDefeatsTokenManager(offlineLoserStats.getDefeatsTokenManager() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
                                 offlineLoserStats
                                       .setLossBeastTokens(offlineLoserStats.getLossBeastTokens() + offlineLoserLoss);
                                 offlineLoserStats.setDefeatsBeastTokens(offlineLoserStats.getDefeatsBeastTokens() + 1);
                              } else if (this.currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER
                                    && this.currencyId != null) {
                                 offlineLoserStats.incrementDefeatsPlaceholder(this.currencyId);
                              }

                              this.plugin.getCoinFlipManager().saveStats(loser.getUniqueId(), offlineLoserStats);
                           } catch (Exception var10x) {
                              this.plugin.getLogger().warning("Failed to update stats for offline loser "
                                    + loser.getUniqueId() + ": " + var10x.getMessage());
                           }
                        }
                     } catch (Exception var12x) {
                        this.plugin.getLogger()
                              .severe("Error in combined offline stats update: " + var12x.getMessage());
                     }
                  });
               }

               double finalTaxedAmount = taxedAmount;
               long currentTimestamp = System.currentTimeMillis();
               FoliaScheduler.runTaskAsynchronously(
                     this.plugin,
                     () -> {
                        try {
                           CoinFlipLog log = new CoinFlipLog(
                                 this.player1.getUniqueId(),
                                 this.player1.getName(),
                                 this.player2.getUniqueId(),
                                 this.player2.getName(),
                                 winner.getUniqueId(),
                                 winner.getName(),
                                 loser.getUniqueId(),
                                 loser.getName(),
                                 this.currencyType,
                                 this.currencyId,
                                 this.amount,
                                 totalPot,
                                 actualTaxRate,
                                 actualTax,
                                 finalTaxedAmount,
                                 currentTimestamp);
                           this.plugin.getDatabaseManager().saveCoinFlipLog(log);
                        } catch (Exception var14x) {
                           this.plugin.getLogger().warning("Failed to save coin flip log: " + var14x.getMessage());
                           if (this.plugin.getDebugManager() != null) {
                              this.plugin.getDebugManager().error(DebugManager.Category.DATABASE,
                                    "Failed to save coin flip log", var14x);
                           }
                        }
                     });
               return;
            }

            if (this.plugin.getDebugManager() != null
                  && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               this.plugin.getDebugManager().info(DebugManager.Category.GUI,
                     "finishRoll() called but both players are offline. Refund was already handled.");
            }
         } catch (Exception var66) {
            this.plugin.getLogger().severe("Exception in finishRoll(): " + var66.getMessage());
            var66.printStackTrace();
            return;
         } finally {
            if (this.player1UUID != null && this.player2UUID != null) {
               UUID finalPlayer1UUID = this.player1UUID;
               UUID finalPlayer2UUID = this.player2UUID;
               FoliaScheduler.runTaskLater(this.plugin, () -> {
                  try {
                     this.closeGUIForBothPlayers();
                  } catch (Exception var5x) {
                     this.plugin.getLogger().warning("Error closing GUI in finally block: " + var5x.getMessage());
                  }

                  try {
                     this.plugin.getCoinFlipManager().unregisterRollingGame(finalPlayer1UUID, finalPlayer2UUID);
                  } catch (Exception var4x) {
                     this.plugin.getLogger()
                           .warning("Error unregistering game in finally block: " + var4x.getMessage());
                  }
               }, 60L);
            } else {
               this.plugin.getLogger()
                     .warning("Cannot unregister rolling game: player UUIDs are null. Attempting to close GUI anyway.");

               try {
                  this.closeGUIForBothPlayers();
               } catch (Exception var62) {
                  this.plugin.getLogger().warning("Error closing GUI when UUIDs are null: " + var62.getMessage());
               }
            }
         }
      }
   }

   private void finishBotGame() {
      UUID finalPlayer1UUID = this.player1UUID;
      UUID finalPlayer2UUID = this.player2UUID;

      try {
         if (this.player1 != null && this.player1.isOnline()) {
            Player winner = this.winner;
            Player loser = this.loser;
            boolean playerWins = winner == this.player1;
            this.playAnimationCompleteSound();
            int inventorySize = this.cachedInventorySize;
            ItemStack winnerItem = this.cachedWinnerHead != null ? this.cachedWinnerHead.clone()
                  : this.createPlayerItem("winner", winner);
            if (this.cachedLoserHead != null) {
               this.cachedLoserHead.clone();
            } else {
               this.createPlayerItem("loser", loser);
            }

            String animationType = this.cachedRollingAnimationType != null
                  ? this.cachedRollingAnimationType.toLowerCase()
                  : "default";
            Inventory inventory = this.getInventory();
            if ("slot-machine".equals(animationType)) {
               int centerSlot = this.cachedSlotMachineCenterSlot;
               if (winnerItem != null && centerSlot >= 0 && centerSlot < inventorySize) {
                  inventory.setItem(centerSlot, winnerItem.clone());
               }
            } else if ("circular".equals(animationType)) {
               int winnerSlot = 20;
               if (winnerItem != null && winnerSlot >= 0 && winnerSlot < inventorySize) {
                  inventory.setItem(winnerSlot, winnerItem.clone());
               }
            } else if ("vertical".equals(animationType)) {
               int winnerSlot = 22;
               if (winnerItem != null && winnerSlot >= 0 && winnerSlot < inventorySize) {
                  inventory.setItem(winnerSlot, winnerItem.clone());
               }
            } else {
               int resultSlot = this.cachedResultSlot;
               if (resultSlot < 0 || resultSlot >= inventorySize) {
                  resultSlot = this.cachedAnimationStartSlot + this.cachedAnimationSlotCount / 2;
               }

               if (winnerItem != null && resultSlot >= 0 && resultSlot < inventorySize) {
                  inventory.setItem(resultSlot, winnerItem.clone());
               }
            }

            this.updateInventoryForPlayers();
            String unit = this.plugin.getCurrencyManager().getUnit(this.currencyType, this.currencyId);
            double totalPot = this.amount * 2.0;
            double taxRate = this.plugin.getTaxRateCalculator().calculateTaxRate(this.player1, this.amount,
                  this.currencyType, this.currencyId);
            double tax = Math.round(totalPot * taxRate * 100.0) / 100.0;
            double taxedAmount = Math.round(totalPot * (1.0 - taxRate) * 100.0) / 100.0;
            if (taxedAmount < this.amount && taxRate < 1.0) {
               double maxTax = totalPot - this.amount;
               tax = Math.round(maxTax * 100.0) / 100.0;
               taxedAmount = Math.round((totalPot - tax) * 100.0) / 100.0;
            }

            double actualTax = totalPot - taxedAmount;
            double actualTaxRate = totalPot > 0.0 ? actualTax / totalPot : 0.0;
            String botName = this.botName != null ? this.botName
                  : this.plugin.getConfig().getString("house.name", "Bot");
            if (playerWins) {
               this.plugin.getCurrencyManager().deposit(this.player1, this.currencyType, this.currencyId, taxedAmount);
               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "message-bot-game")) {
                  Map<String, String> placeholders = new HashMap<>();
                  placeholders.put("amount", this.plugin.getGuiHelper().formatAmount(taxedAmount, this.currencyId));
                  placeholders.put("symbol", unit);
                  placeholders.put("tax", this.plugin.getGuiHelper().formatAmount(actualTax, this.currencyId));
                  String winMessage = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("house.win");
                  this.plugin.getAdventureHelper().sendMessage(this.player1, winMessage, placeholders);
               }

               if (this.cachedTitlesEnabled
                     && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-title")) {
                  Map<String, String> winnerTitlePlaceholders = new HashMap<>();
                  winnerTitlePlaceholders.put("amount",
                        this.plugin.getGuiHelper().formatAmount(taxedAmount, this.currencyId));
                  winnerTitlePlaceholders.put("symbol", unit);
                  this.plugin
                        .getAdventureHelper()
                        .sendTitle(
                              this.player1,
                              this.plugin.getConfigManager().getMessages().getString("titles.game-win-title",
                                    "&a&lYOU WON!"),
                              this.plugin.getConfigManager().getMessages().getString("titles.game-win-subtitle",
                                    "&7Received &a<amount><symbol>"),
                              winnerTitlePlaceholders);
               }

               if (this.cachedActionbarEnabled && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1,
                     "notification-actionbar")) {
                  Map<String, String> winnerActionbarPlaceholders = new HashMap<>();
                  winnerActionbarPlaceholders.put("amount",
                        this.plugin.getGuiHelper().formatAmount(taxedAmount, this.currencyId));
                  winnerActionbarPlaceholders.put("symbol", unit);
                  String winnerActionbarMsg = this.plugin
                        .getConfigManager()
                        .getMessages()
                        .getString("actionbar.game-win", "&a&lYOU WON! &fReceived &a<amount><symbol>");
                  this.plugin.getAdventureHelper().sendActionBar(this.player1, winnerActionbarMsg,
                        winnerActionbarPlaceholders);
               }

               if (this.cachedBossbarEnabled
                     && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-bossbar")) {
                  Map<String, String> winnerBossbarPlaceholders = new HashMap<>();
                  winnerBossbarPlaceholders.put("amount",
                        this.plugin.getGuiHelper().formatAmount(taxedAmount, this.currencyId));
                  winnerBossbarPlaceholders.put("symbol", unit);
                  String winnerBossbarMsg = this.plugin
                        .getConfigManager()
                        .getMessages()
                        .getString("bossbar.game-win", "&a&lYOU WON! &fReceived &a<amount><symbol>");
                  String winnerBossbarColor = this.cachedWinnerBossbarColor;
                  String winnerBossbarOverlay = this.cachedWinnerBossbarOverlay;
                  float winnerBossbarProgress = this.cachedWinnerBossbarProgress;
                  int winnerBossbarDuration = this.cachedBossbarDuration;
                  BossBar winnerBossBar = this.plugin
                        .getAdventureHelper()
                        .sendBossBar(
                              this.player1,
                              winnerBossbarMsg,
                              winnerBossbarProgress,
                              winnerBossbarColor,
                              winnerBossbarOverlay,
                              winnerBossbarDuration,
                              winnerBossbarPlaceholders);
                  if (winnerBossBar != null && winnerBossbarDuration > 0) {
                     Player finalPlayer = this.player1;
                     FoliaScheduler.runTaskLater(this.plugin, finalPlayer, () -> {
                        if (finalPlayer != null && finalPlayer.isOnline()) {
                           this.plugin.getAdventureHelper().removeBossBar(finalPlayer, winnerBossBar);
                        }
                     }, winnerBossbarDuration * 20L);
                  }
               }

               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(this.player1, "game.win");
               }

               PlayerStats playerStats = this.plugin.getCoinFlipManager().getStats(this.player1.getUniqueId());
               playerStats.setWins(playerStats.getWins() + 1);
               playerStats.setWinstreak(playerStats.getWinstreak() + 1);
               double netProfit = taxedAmount - this.amount;
               this.updateStatsForCurrency(playerStats, this.currencyType, this.currencyId, true, netProfit);
               this.plugin.getCoinFlipManager().saveStats(this.player1.getUniqueId(), playerStats);
            } else {
               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "message-bot-game")) {
                  Map<String, String> placeholders = new HashMap<>();
                  placeholders.put("amount", this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId));
                  placeholders.put("symbol", unit);
                  String loseMessage = this.plugin.getMessage("prefix") + " " + this.plugin.getMessage("house.lose");
                  this.plugin.getAdventureHelper().sendMessage(this.player1, loseMessage, placeholders);
               }

               if (this.cachedTitlesEnabled
                     && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-title")) {
                  Map<String, String> loserTitlePlaceholders = new HashMap<>();
                  loserTitlePlaceholders.put("amount",
                        this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId));
                  loserTitlePlaceholders.put("symbol", unit);
                  this.plugin
                        .getAdventureHelper()
                        .sendTitle(
                              this.player1,
                              this.plugin.getConfigManager().getMessages().getString("titles.game-lose-title",
                                    "&c&lYOU LOST!"),
                              this.plugin.getConfigManager().getMessages().getString("titles.game-lose-subtitle",
                                    "&7Lost &c<amount><symbol>"),
                              loserTitlePlaceholders);
               }

               if (this.cachedActionbarEnabled && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1,
                     "notification-actionbar")) {
                  Map<String, String> loserActionbarPlaceholders = new HashMap<>();
                  loserActionbarPlaceholders.put("amount",
                        this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId));
                  loserActionbarPlaceholders.put("symbol", unit);
                  String loserActionbarMsg = this.plugin
                        .getConfigManager()
                        .getMessages()
                        .getString("actionbar.game-lose", "&c&lYOU LOST! &fLost &c<amount><symbol>");
                  this.plugin.getAdventureHelper().sendActionBar(this.player1, loserActionbarMsg,
                        loserActionbarPlaceholders);
               }

               if (this.cachedBossbarEnabled
                     && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-bossbar")) {
                  Map<String, String> loserBossbarPlaceholders = new HashMap<>();
                  loserBossbarPlaceholders.put("amount",
                        this.plugin.getGuiHelper().formatAmount(this.amount, this.currencyId));
                  loserBossbarPlaceholders.put("symbol", unit);
                  String loserBossbarMsg = this.plugin
                        .getConfigManager()
                        .getMessages()
                        .getString("bossbar.game-lose", "&c&lYOU LOST! &fLost &c<amount><symbol>");
                  String loserBossbarColor = this.cachedLoserBossbarColor;
                  String loserBossbarOverlay = this.cachedLoserBossbarOverlay;
                  float loserBossbarProgress = this.cachedLoserBossbarProgress;
                  int loserBossbarDuration = this.cachedBossbarDuration;
                  BossBar loserBossBar = this.plugin
                        .getAdventureHelper()
                        .sendBossBar(
                              this.player1,
                              loserBossbarMsg,
                              loserBossbarProgress,
                              loserBossbarColor,
                              loserBossbarOverlay,
                              loserBossbarDuration,
                              loserBossbarPlaceholders);
                  if (loserBossBar != null && loserBossbarDuration > 0) {
                     Player finalPlayer = this.player1;
                     FoliaScheduler.runTaskLater(this.plugin, finalPlayer, () -> {
                        if (finalPlayer != null && finalPlayer.isOnline()) {
                           this.plugin.getAdventureHelper().removeBossBar(finalPlayer, loserBossBar);
                        }
                     }, loserBossbarDuration * 20L);
                  }
               }

               if (this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1, "notification-sound")) {
                  this.plugin.getSoundHelper().playSound(this.player1, "game.lose");
               }

               PlayerStats playerStats = this.plugin.getCoinFlipManager().getStats(this.player1.getUniqueId());
               playerStats.setDefeats(playerStats.getDefeats() + 1);
               playerStats.setWinstreak(0);
               this.updateStatsForCurrency(playerStats, this.currencyType, this.currencyId, false, this.amount);
               this.plugin.getCoinFlipManager().saveStats(this.player1.getUniqueId(), playerStats);
            }

            HouseCoinFlipManager houseManager = this.plugin.getHouseCoinFlipManager();
            if (houseManager != null) {
               houseManager.removePendingGame(this.player1.getUniqueId());
            }

            if (this.plugin.getConfig().getBoolean("house.notifications.enabled", true)
                  && this.plugin.getConfig().getBoolean("discord.webhook.enabled", false)) {
               String currencyDisplayName = this.plugin.getCurrencyManager().getDisplayName(this.currencyType,
                     this.currencyId);
               if (playerWins) {
                  this.plugin.getDiscordWebhookHandler().sendGameResult(this.player1.getName(), botName, this.amount,
                        taxedAmount, currencyDisplayName, unit);
               } else {
                  this.plugin.getDiscordWebhookHandler().sendGameResult(botName, this.player1.getName(), this.amount,
                        0.0, currencyDisplayName, unit);
               }
            }

            if (this.plugin.getConfig().getBoolean("house.history.enabled", true)) {
               UUID finalPlayerUuid = this.player1.getUniqueId();
               String finalPlayerName = this.player1.getName();
               UUID finalBotUuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
               UUID finalWinnerUuid = playerWins ? finalPlayerUuid : finalBotUuid;
               String finalWinnerName = playerWins ? finalPlayerName : botName;
               UUID finalLoserUuid = playerWins ? finalBotUuid : finalPlayerUuid;
               String finalLoserName = playerWins ? botName : finalPlayerName;
               double finalTaxedAmount = playerWins ? taxedAmount : 0.0;
               long currentTimestamp = System.currentTimeMillis();
               FoliaScheduler.runTaskAsynchronously(
                     this.plugin,
                     () -> {
                        try {
                           CoinFlipLog log = new CoinFlipLog(
                                 finalPlayerUuid,
                                 finalPlayerName,
                                 finalBotUuid,
                                 botName,
                                 finalWinnerUuid,
                                 finalWinnerName,
                                 finalLoserUuid,
                                 finalLoserName,
                                 this.currencyType,
                                 this.currencyId,
                                 this.amount,
                                 totalPot,
                                 actualTaxRate,
                                 actualTax,
                                 finalTaxedAmount,
                                 currentTimestamp,
                                 CoinFlipLog.GameType.HOUSE);
                           this.plugin.getDatabaseManager().saveCoinFlipLog(log);
                        } catch (Exception var20x) {
                           this.plugin.getLogger().warning("Failed to save bot coin flip log: " + var20x.getMessage());
                        }
                     });
            }

            FoliaScheduler.runTaskLater(this.plugin, this.player1, () -> {
               if (this.player1 != null && this.player1.isOnline()) {
                  try {
                     this.player1.closeInventory();
                  } catch (Exception var2x) {
                  }
               }
            }, 60L);
            return;
         }

         this.plugin.getLogger().warning("finishBotGame() called but player is offline. Refund was already handled.");
      } catch (Exception var56) {
         this.plugin.getLogger().severe("Error in finishBotGame(): " + var56.getMessage());
         var56.printStackTrace();
         return;
      } finally {
         try {
            HouseCoinFlipManager houseManagerx = this.plugin.getHouseCoinFlipManager();
            if (houseManagerx != null && finalPlayer1UUID != null) {
               houseManagerx.removePendingGame(finalPlayer1UUID);
            }
         } catch (Exception var55) {
            this.plugin.getLogger().warning("Error removing pending game in finally block: " + var55.getMessage());
         }

         try {
            if (finalPlayer1UUID != null && finalPlayer2UUID != null) {
               this.plugin.getCoinFlipManager().unregisterRollingGame(finalPlayer1UUID, finalPlayer2UUID);
            }
         } catch (Exception var54) {
            this.plugin.getLogger().warning("Error unregistering bot game in finally block: " + var54.getMessage());
         }
      }
   }

   private void updateStatsForCurrency(PlayerStats stats, CoinFlipGame.CurrencyType currencyType, String currencyId,
         boolean isWin, double amount) {
      if (currencyType == CoinFlipGame.CurrencyType.MONEY) {
         if (isWin) {
            stats.setProfitMoney(stats.getProfitMoney() + Math.max(0.0, amount));
            stats.setWinsMoney(stats.getWinsMoney() + 1);
         } else {
            stats.setLossMoney(stats.getLossMoney() + amount);
            stats.setDefeatsMoney(stats.getDefeatsMoney() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.PLAYERPOINTS) {
         if (isWin) {
            stats.setProfitPlayerPoints(stats.getProfitPlayerPoints() + Math.max(0.0, amount));
            stats.setWinsPlayerPoints(stats.getWinsPlayerPoints() + 1);
         } else {
            stats.setLossPlayerPoints(stats.getLossPlayerPoints() + amount);
            stats.setDefeatsPlayerPoints(stats.getDefeatsPlayerPoints() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.TOKENMANAGER) {
         if (isWin) {
            stats.setProfitTokenManager(stats.getProfitTokenManager() + Math.max(0.0, amount));
            stats.setWinsTokenManager(stats.getWinsTokenManager() + 1);
         } else {
            stats.setLossTokenManager(stats.getLossTokenManager() + amount);
            stats.setDefeatsTokenManager(stats.getDefeatsTokenManager() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.BEASTTOKENS) {
         if (isWin) {
            stats.setProfitBeastTokens(stats.getProfitBeastTokens() + Math.max(0.0, amount));
            stats.setWinsBeastTokens(stats.getWinsBeastTokens() + 1);
         } else {
            stats.setLossBeastTokens(stats.getLossBeastTokens() + amount);
            stats.setDefeatsBeastTokens(stats.getDefeatsBeastTokens() + 1);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.PLACEHOLDER && currencyId != null) {
         if (isWin) {
            stats.incrementWinsPlaceholder(currencyId);
         } else {
            stats.incrementDefeatsPlaceholder(currencyId);
         }
      } else if (currencyType == CoinFlipGame.CurrencyType.COINSENGINE && currencyId != null) {
         if (isWin) {
            stats.incrementWinsPlaceholder(currencyId);
         } else {
            stats.incrementDefeatsPlaceholder(currencyId);
         }
      }
   }

   private void closeGUIForBothPlayers() {
      Player finalPlayer1 = this.player1;
      Player finalPlayer2 = this.player2;
      if (finalPlayer1 != null && finalPlayer1.isOnline() && !this.playersClosedGUI.contains(this.player1UUID)) {
         FoliaScheduler.runTask(this.plugin, finalPlayer1, () -> {
            try {
               if (finalPlayer1.isOnline()) {
                  finalPlayer1.closeInventory();
               }
            } catch (Exception var3) {
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin.getDebugManager().verbose(DebugManager.Category.GUI,
                        "Error closing inventory for player1: " + var3.getMessage());
               }
            }
         });
      }

      if (!this.isBotGame && finalPlayer2 != null && finalPlayer2.isOnline()
            && !this.playersClosedGUI.contains(this.player2UUID)) {
         FoliaScheduler.runTask(this.plugin, finalPlayer2, () -> {
            try {
               if (finalPlayer2.isOnline()) {
                  finalPlayer2.closeInventory();
               }
            } catch (Exception var3) {
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin.getDebugManager().verbose(DebugManager.Category.GUI,
                        "Error closing inventory for player2: " + var3.getMessage());
               }
            }
         });
      }
   }

   private void playAnimationCompleteSound() {
      if (this.cachedAudioOnCompleteEnabled) {
         String soundName = this.cachedAudioOnCompleteSound;
         float volume = this.cachedAudioOnCompleteVolume;
         float pitch = this.cachedAudioOnCompletePitch;
         if (this.player1 != null
               && this.player1.isOnline()
               && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player1,
                     "notification-animation-sound")) {
            this.plugin.getAdventureHelper().playSound(this.player1, soundName, volume, pitch);
         }

         if (this.player2 != null
               && this.player2.isOnline()
               && this.plugin.getPlayerSettingsManager().isSettingEnabled(this.player2,
                     "notification-animation-sound")) {
            this.plugin.getAdventureHelper().playSound(this.player2, soundName, volume, pitch);
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent event) {
      if (this.finished) {
         super.onClose(event);
      } else {

         if (this.player1 != null && this.player2 != null && this.animationRunning && !this.finished) {
            Player closingPlayer = (Player) event.getPlayer();
            if (closingPlayer != null) {
               UUID closingPlayerUUIDx = closingPlayer.getUniqueId();
               this.playersClosedGUI.add(closingPlayerUUIDx);
               if (this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                  this.plugin
                        .getDebugManager()
                        .info(
                              DebugManager.Category.GUI,
                              "Player " + closingPlayer.getName()
                                    + " closed GUI. Game continues running in background and will notify when complete.");
               }
            }
         }

         super.onClose(event);
      }
   }

   private String getOrdinalSuffix(int number) {
      if (number < 1) {
         return "th";
      } else {
         int lastDigit = number % 10;
         int lastTwoDigits = number % 100;
         if (lastTwoDigits >= 11 && lastTwoDigits <= 13) {
            return "th";
         } else {
            switch (lastDigit) {
               case 1:
                  return "st";
               case 2:
                  return "nd";
               case 3:
                  return "rd";
               default:
                  return "th";
            }
         }
      }
   }
}
