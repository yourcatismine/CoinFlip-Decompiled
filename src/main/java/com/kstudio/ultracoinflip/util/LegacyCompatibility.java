package com.kstudio.ultracoinflip.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class LegacyCompatibility {
   private static final boolean HAS_SOUND_CATEGORY = checkSoundCategorySupport();
   private static final boolean HAS_CUSTOM_MODEL_DATA = checkCustomModelDataSupport();
   private static final boolean HAS_GLOWING = checkGlowingSupport();
   private static final boolean HAS_SET_OWNING_PLAYER = checkSetOwningPlayerSupport();
   private static final boolean HAS_PLAYER_PROFILE = checkPlayerProfileSupport();
   private static final Object SOUND_CATEGORY_PLAYERS = initSoundCategoryPlayers();
   private static final Enchantment DURABILITY_ENCHANT = initDurabilityEnchantment();

   private static boolean checkSoundCategorySupport() {
      try {
         Class.forName("org.bukkit.SoundCategory");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }

   private static boolean checkCustomModelDataSupport() {
      try {
         ItemMeta.class.getMethod("hasCustomModelData");
         return true;
      } catch (NoSuchMethodException var1) {
         return false;
      }
   }

   private static boolean checkGlowingSupport() {
      try {
         ItemMeta.class.getMethod("addItemFlags", ItemFlag[].class);
         return true;
      } catch (NoSuchMethodException var1) {
         return false;
      }
   }

   private static boolean checkSetOwningPlayerSupport() {
      try {
         SkullMeta.class.getMethod("setOwningPlayer", OfflinePlayer.class);
         return true;
      } catch (NoSuchMethodException var1) {
         return false;
      }
   }

   private static boolean checkPlayerProfileSupport() {
      try {
         Class.forName("org.bukkit.profile.PlayerProfile");
         return true;
      } catch (ClassNotFoundException var1) {
         return false;
      }
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static Object initSoundCategoryPlayers() {
      if (!HAS_SOUND_CATEGORY) {
         return null;
      } else {
         try {
            Class soundCategoryClass = Class.forName("org.bukkit.SoundCategory");
            return Enum.valueOf(soundCategoryClass, "PLAYERS");
         } catch (Exception var2) {
            return null;
         }
      }
   }

   private static Enchantment initDurabilityEnchantment() {
      try {
         Class<?> registryClass = Class.forName("org.bukkit.Registry");
         Object enchantmentRegistry = registryClass.getField("ENCHANTMENT").get(null);
         Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
         Object unbreakingKey = namespacedKeyClass.getMethod("minecraft", String.class).invoke(null, "unbreaking");
         Method getMethod = enchantmentRegistry.getClass().getMethod("get", namespacedKeyClass);
         Enchantment enchantment = (Enchantment)getMethod.invoke(enchantmentRegistry, namespacedKeyClass.cast(unbreakingKey));
         if (enchantment != null) {
            return enchantment;
         }
      } catch (Exception var10) {
      }

      try {
         Class<?> namespacedKeyClass = Class.forName("org.bukkit.NamespacedKey");
         Object unbreakingKey = namespacedKeyClass.getMethod("minecraft", String.class).invoke(null, "unbreaking");
         Method getByKeyMethod = Enchantment.class.getMethod("getByKey", namespacedKeyClass);
         Enchantment enchantment = (Enchantment)getByKeyMethod.invoke(null, namespacedKeyClass.cast(unbreakingKey));
         if (enchantment != null) {
            return enchantment;
         }
      } catch (Exception var9) {
      }

      try {
         try {
            Field durabilityField = Enchantment.class.getField("DURABILITY");
            return (Enchantment)durabilityField.get(null);
         } catch (NoSuchFieldException var7) {
            try {
               Field unbreakingField = Enchantment.class.getField("UNBREAKING");
               return (Enchantment)unbreakingField.get(null);
            } catch (NoSuchFieldException var6) {
               return null;
            }
         }
      } catch (Exception var8) {
         return null;
      }
   }

   public static void playSound(Player player, Sound sound, float volume, float pitch) {
      if (player != null && player.isOnline() && sound != null) {
         try {
            Class<?> soundClass = sound.getClass();
            if (HAS_SOUND_CATEGORY && SOUND_CATEGORY_PLAYERS != null) {
               try {
                  Class<?> soundCategoryClass = Class.forName("org.bukkit.SoundCategory");
                  Method playSoundMethod = player.getClass().getMethod("playSound", Location.class, soundClass, soundCategoryClass, float.class, float.class);
                  playSoundMethod.invoke(player, player.getLocation(), sound, soundCategoryClass.cast(SOUND_CATEGORY_PLAYERS), volume, pitch);
                  return;
               } catch (Throwable var7) {
               }
            }

            Method playSoundMethod = player.getClass().getMethod("playSound", Location.class, soundClass, float.class, float.class);
            playSoundMethod.invoke(player, player.getLocation(), sound, volume, pitch);
         } catch (Throwable var8) {
         }
      }
   }

   private static String convertToLegacySoundName(String soundName) {
      if (soundName != null && !soundName.isEmpty()) {
         String upperSound = soundName.toUpperCase();
         switch (upperSound) {
            case "ENTITY_PLAYER_LEVELUP":
               return "LEVEL_UP";
            case "ENTITY_VILLAGER_NO":
               return "VILLAGER_NO";
            case "ENTITY_VILLAGER_YES":
               return "VILLAGER_YES";
            case "ENTITY_VILLAGER_HAGGLE":
               return "VILLAGER_HAGGLE";
            case "ENTITY_VILLAGER_IDLE":
               return "VILLAGER_IDLE";
            case "ENTITY_VILLAGER_HURT":
               return "VILLAGER_HIT";
            case "ENTITY_VILLAGER_DEATH":
               return "VILLAGER_DEATH";
            case "ENTITY_EXPERIENCE_ORB_PICKUP":
               return "ORB_PICKUP";
            case "BLOCK_NOTE_BLOCK_PLING":
               return "NOTE_PLING";
            case "BLOCK_NOTE_BLOCK_BASS":
               return "NOTE_BASS";
            case "BLOCK_NOTE_BLOCK_PIANO":
               return "NOTE_PIANO";
            case "BLOCK_NOTE_BLOCK_BASS_DRUM":
               return "NOTE_BASS_DRUM";
            case "BLOCK_NOTE_BLOCK_SNARE_DRUM":
               return "NOTE_SNARE_DRUM";
            case "BLOCK_NOTE_BLOCK_STICKS":
               return "NOTE_STICKS";
            case "BLOCK_CHEST_OPEN":
               return "CHEST_OPEN";
            case "BLOCK_CHEST_CLOSE":
               return "CHEST_CLOSE";
            case "BLOCK_ANVIL_USE":
               return "ANVIL_USE";
            case "BLOCK_ANVIL_LAND":
               return "ANVIL_LAND";
            case "BLOCK_ANVIL_BREAK":
               return "ANVIL_BREAK";
            case "BLOCK_WOODEN_BUTTON_CLICK_ON":
            case "BLOCK_WOODEN_BUTTON_CLICK_OFF":
               return "WOOD_CLICK";
            case "UI_BUTTON_CLICK":
               return "WOOD_CLICK";
            default:
               return soundName;
         }
      } else {
         return soundName;
      }
   }

   public static void playSound(Player player, String soundName, float volume, float pitch) {
      if (player != null && player.isOnline() && soundName != null && !soundName.isEmpty()) {
         boolean isLegacy = VersionDetector.isLegacy();
         boolean success = tryPlaySound(player, soundName, volume, pitch);
         if (!success) {
            if (isLegacy) {
               String legacySoundName = convertToLegacySoundName(soundName);
               if (!legacySoundName.equals(soundName)) {
                  success = tryPlaySound(player, legacySoundName, volume, pitch);
                  if (success) {
                     return;
                  }
               }
            }
         }
      }
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static boolean tryPlaySound(Player player, String soundName, float volume, float pitch) {
      try {
         Class soundClass = Class.forName("org.bukkit.Sound");
         Object sound = Enum.valueOf(soundClass, soundName.toUpperCase());
         if (HAS_SOUND_CATEGORY && SOUND_CATEGORY_PLAYERS != null) {
            try {
               Class<?> soundCategoryClass = Class.forName("org.bukkit.SoundCategory");
               Method playSoundMethod = player.getClass().getMethod("playSound", Location.class, soundClass, soundCategoryClass, float.class, float.class);
               playSoundMethod.invoke(player, player.getLocation(), sound, soundCategoryClass.cast(SOUND_CATEGORY_PLAYERS), volume, pitch);
               return true;
            } catch (Throwable var8) {
            }
         }

         Method playSoundMethod = player.getClass().getMethod("playSound", Location.class, soundClass, float.class, float.class);
         playSoundMethod.invoke(player, player.getLocation(), sound, volume, pitch);
         return true;
      } catch (Throwable var9) {
         return false;
      }
   }

   public static void setCustomModelData(ItemMeta meta, Integer customModelData) {
      if (meta != null && customModelData != null && customModelData > 0) {
         if (HAS_CUSTOM_MODEL_DATA) {
            try {
               Method setMethod = meta.getClass().getMethod("setCustomModelData", Integer.class);
               setMethod.invoke(meta, customModelData);
            } catch (Exception var3) {
            }
         }
      }
   }

   public static void removeCustomModelData(ItemMeta meta) {
      if (meta != null) {
         if (HAS_CUSTOM_MODEL_DATA) {
            try {
               Method setMethod = meta.getClass().getMethod("setCustomModelData", Integer.class);
               setMethod.invoke(meta, (Integer)null);
            } catch (Exception var2) {
            }
         }
      }
   }

   public static boolean hasCustomModelData(ItemMeta meta) {
      if (meta == null) {
         return false;
      } else if (HAS_CUSTOM_MODEL_DATA) {
         try {
            Method hasMethod = meta.getClass().getMethod("hasCustomModelData");
            return (Boolean)hasMethod.invoke(meta);
         } catch (Exception var2) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void setGlowing(ItemMeta meta, boolean glowing) {
      if (meta != null) {
         if (HAS_GLOWING) {
            if (DURABILITY_ENCHANT == null) {
               return;
            }

            if (glowing) {
               meta.addEnchant(DURABILITY_ENCHANT, 1, true);
               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            } else {
               meta.removeEnchant(DURABILITY_ENCHANT);
               meta.removeItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }
         }
      }
   }

   public static void setSkullOwner(SkullMeta meta, OfflinePlayer player) {
      if (meta != null && player != null) {
         if (HAS_PLAYER_PROFILE) {
            try {
               UUID uuid = player.getUniqueId();
               String playerName = player.getName();
               if (uuid != null) {
                  Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
                  Object profile = null;
                  if (player instanceof Player && ((Player)player).isOnline()) {
                     try {
                        Method getPlayerProfileMethod = ((Player)player).getClass().getMethod("getPlayerProfile");
                        profile = getPlayerProfileMethod.invoke((Player)player);
                        if (profile != null) {
                           try {
                              Method getTexturesMethod = playerProfileClass.getMethod("getTextures");
                              Object textures = getTexturesMethod.invoke(profile);
                              if (textures != null) {
                                 Class<?> playerTexturesClass = Class.forName("org.bukkit.profile.PlayerTextures");
                                 Method getSkinMethod = playerTexturesClass.getMethod("getSkin");
                                 URL skinUrl = (URL)getSkinMethod.invoke(textures);
                                 if (skinUrl == null) {
                                    profile = null;
                                 }
                              } else {
                                 profile = null;
                              }
                           } catch (Exception var14) {
                              profile = null;
                           }
                        }
                     } catch (Exception var15) {
                        profile = null;
                     }
                  }

                  if (profile == null) {
                     Method createProfileMethod = Bukkit.class.getMethod("createPlayerProfile", UUID.class, String.class);
                     profile = createProfileMethod.invoke(null, uuid, playerName != null ? playerName : "Player");
                  }

                  Method setOwnerProfileMethod = meta.getClass().getMethod("setOwnerProfile", playerProfileClass);
                  setOwnerProfileMethod.invoke(meta, profile);
                  return;
               }
            } catch (ClassNotFoundException var16) {
            } catch (Exception var17) {
            }
         }

         if (HAS_SET_OWNING_PLAYER) {
            try {
               meta.setOwningPlayer(player);
            } catch (Exception var13) {
            }
         } else {
            try {
               Method setOwnerMethod = meta.getClass().getMethod("setOwner", String.class);
               String playerName = player.getName();
               if (playerName != null) {
                  setOwnerMethod.invoke(meta, playerName);
               }
            } catch (Exception var12) {
            }
         }
      }
   }

   public static void setSkullOwner(SkullMeta meta, String playerName) {
      if (meta != null && playerName != null && !playerName.isEmpty()) {
         try {
            Method setOwnerMethod = meta.getClass().getMethod("setOwner", String.class);
            setOwnerMethod.invoke(meta, playerName);
         } catch (Exception var5) {
            if (HAS_SET_OWNING_PLAYER) {
               try {
                  OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                  meta.setOwningPlayer(player);
               } catch (Exception var4) {
               }
            }
         }
      }
   }

   public static boolean hasPlayerProfileSupport() {
      return HAS_PLAYER_PROFILE;
   }

   public static boolean hasSoundCategorySupport() {
      return HAS_SOUND_CATEGORY;
   }

   public static boolean hasCustomModelDataSupport() {
      return HAS_CUSTOM_MODEL_DATA;
   }

   public static boolean hasGlowingSupport() {
      return HAS_GLOWING;
   }

   public static boolean hasSetOwningPlayerSupport() {
      return HAS_SET_OWNING_PLAYER;
   }

   public static boolean isServerStopping() {
      try {
         Method isStoppingMethod = Bukkit.getServer().getClass().getMethod("isStopping");
         return (Boolean)isStoppingMethod.invoke(Bukkit.getServer());
      } catch (NoSuchMethodException var1) {
         return false;
      } catch (Throwable var2) {
         return false;
      }
   }
}
