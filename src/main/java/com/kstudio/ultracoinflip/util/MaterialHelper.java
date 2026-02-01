package com.kstudio.ultracoinflip.util;

import java.lang.reflect.Method;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MaterialHelper {
   public static Material parseMaterial(String materialName, Material fallback) {
      if (materialName != null && !materialName.isEmpty()) {
         try {
            Class<?> xMaterialClass = null;

            try {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            } catch (ClassNotFoundException var11) {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            }

            Method matchMethod = xMaterialClass.getMethod("matchXMaterial", String.class);
            Object optional = matchMethod.invoke(null, materialName);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            if ((Boolean)isPresentMethod.invoke(optional)) {
               Method getMethod = optional.getClass().getMethod("get");
               Object xMaterial = getMethod.invoke(optional);
               Method parseMaterialMethod = xMaterialClass.getMethod("parseMaterial");
               Material material = (Material)parseMaterialMethod.invoke(xMaterial);
               if (material != null) {
                  return material;
               }
            }
         } catch (Exception var12) {
         }

         try {
            return Material.valueOf(materialName.toUpperCase());
         } catch (IllegalArgumentException var10) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   public static ItemStack createItemStack(String materialName, int amount) {
      if (materialName != null && !materialName.isEmpty()) {
         try {
            Class<?> xMaterialClass = null;

            try {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            } catch (ClassNotFoundException var11) {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            }

            Method matchMethod = xMaterialClass.getMethod("matchXMaterial", String.class);
            Object optional = matchMethod.invoke(null, materialName);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            if ((Boolean)isPresentMethod.invoke(optional)) {
               Method getMethod = optional.getClass().getMethod("get");
               Object xMaterial = getMethod.invoke(optional);
               Method parseItemMethod = xMaterialClass.getMethod("parseItem");
               ItemStack item = (ItemStack)parseItemMethod.invoke(xMaterial);
               if (item != null) {
                  item.setAmount(amount);
                  return item;
               }
            }
         } catch (Exception var12) {
         }

         try {
            Material material = Material.valueOf(materialName.toUpperCase());
            return new ItemStack(material, amount);
         } catch (IllegalArgumentException var10) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static ItemStack createItemStack(String materialName) {
      return createItemStack(materialName, 1);
   }

   public static boolean materialExists(String materialName) {
      if (materialName != null && !materialName.isEmpty()) {
         try {
            Class<?> xMaterialClass = null;

            try {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            } catch (ClassNotFoundException var10) {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            }

            Method matchMethod = xMaterialClass.getMethod("matchXMaterial", String.class);
            Object optional = matchMethod.invoke(null, materialName);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            if ((Boolean)isPresentMethod.invoke(optional)) {
               Method getMethod = optional.getClass().getMethod("get");
               Object xMaterial = getMethod.invoke(optional);
               Method parseMaterialMethod = xMaterialClass.getMethod("parseMaterial");
               Material material = (Material)parseMaterialMethod.invoke(xMaterial);
               return material != null;
            }
         } catch (Exception var11) {
         }

         try {
            Material.valueOf(materialName.toUpperCase());
            return true;
         } catch (IllegalArgumentException var9) {
            return false;
         }
      } else {
         return false;
      }
   }

   public static String getStandardizedName(String materialName) {
      if (materialName != null && !materialName.isEmpty()) {
         try {
            Class<?> xMaterialClass = null;

            try {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            } catch (ClassNotFoundException var9) {
               xMaterialClass = Class.forName("com.kstudio.ultracoinflip.libs.xseries.XMaterial");
            }

            Method matchMethod = xMaterialClass.getMethod("matchXMaterial", String.class);
            Object optional = matchMethod.invoke(null, materialName);
            Method isPresentMethod = optional.getClass().getMethod("isPresent");
            if ((Boolean)isPresentMethod.invoke(optional)) {
               Method getMethod = optional.getClass().getMethod("get");
               Object xMaterial = getMethod.invoke(optional);
               Method parseMaterialMethod = xMaterialClass.getMethod("parseMaterial");
               Material material = (Material)parseMaterialMethod.invoke(xMaterial);
               if (material != null) {
                  return material.name();
               }
            }
         } catch (Exception var10) {
         }

         return materialName.toUpperCase();
      } else {
         return materialName;
      }
   }

   public static Material getPlayerHeadMaterial() {
      return parseMaterial("PLAYER_HEAD", null);
   }

   public static boolean isPlayerHead(Material material) {
      return material == null ? false : "PLAYER_HEAD".equals(material.name());
   }

   public static Material getBarrierMaterial() {
      return parseMaterial("BARRIER", null);
   }

   public static Material getBlackStainedGlassPane() {
      return parseMaterial("BLACK_STAINED_GLASS_PANE", null);
   }

   public static Material getGrayStainedGlassPane() {
      return parseMaterial("GRAY_STAINED_GLASS_PANE", null);
   }

   public static Material getBookMaterial() {
      return parseMaterial("BOOK", null);
   }

   public static Material getRedWoolMaterial() {
      return parseMaterial("RED_WOOL", null);
   }

   public static Material getRedStainedGlassPane() {
      return parseMaterial("RED_STAINED_GLASS_PANE", null);
   }

   public static Material getLimeStainedGlassPane() {
      return parseMaterial("LIME_STAINED_GLASS_PANE", null);
   }

   public static ItemStack getBlackStainedGlassPaneItem() {
      return createItemStack("BLACK_STAINED_GLASS_PANE");
   }

   public static ItemStack getGrayStainedGlassPaneItem() {
      return createItemStack("GRAY_STAINED_GLASS_PANE");
   }

   public static ItemStack getRedStainedGlassPaneItem() {
      return createItemStack("RED_STAINED_GLASS_PANE");
   }

   public static ItemStack getLimeStainedGlassPaneItem() {
      return createItemStack("LIME_STAINED_GLASS_PANE");
   }

   @Deprecated
   public static Material[] getRainbowGlassMaterials() {
      return new Material[]{
         parseMaterial("RED_STAINED_GLASS_PANE", null),
         parseMaterial("ORANGE_STAINED_GLASS_PANE", null),
         parseMaterial("YELLOW_STAINED_GLASS_PANE", null),
         parseMaterial("LIME_STAINED_GLASS_PANE", null),
         parseMaterial("GREEN_STAINED_GLASS_PANE", null),
         parseMaterial("CYAN_STAINED_GLASS_PANE", null),
         parseMaterial("LIGHT_BLUE_STAINED_GLASS_PANE", null),
         parseMaterial("BLUE_STAINED_GLASS_PANE", null),
         parseMaterial("PURPLE_STAINED_GLASS_PANE", null),
         parseMaterial("MAGENTA_STAINED_GLASS_PANE", null),
         parseMaterial("PINK_STAINED_GLASS_PANE", null)
      };
   }

   public static ItemStack[] getRainbowGlassItems() {
      return new ItemStack[]{
         createItemStack("RED_STAINED_GLASS_PANE"),
         createItemStack("ORANGE_STAINED_GLASS_PANE"),
         createItemStack("YELLOW_STAINED_GLASS_PANE"),
         createItemStack("LIME_STAINED_GLASS_PANE"),
         createItemStack("GREEN_STAINED_GLASS_PANE"),
         createItemStack("CYAN_STAINED_GLASS_PANE"),
         createItemStack("LIGHT_BLUE_STAINED_GLASS_PANE"),
         createItemStack("BLUE_STAINED_GLASS_PANE"),
         createItemStack("PURPLE_STAINED_GLASS_PANE"),
         createItemStack("MAGENTA_STAINED_GLASS_PANE"),
         createItemStack("PINK_STAINED_GLASS_PANE")
      };
   }

   public static boolean isSameMaterial(Material mat1, Material mat2) {
      if (mat1 == null && mat2 == null) {
         return true;
      } else {
         return mat1 != null && mat2 != null ? mat1.name().equals(mat2.name()) : false;
      }
   }

   public static String validateAndConvertMaterial(String materialName, String defaultMaterial) {
      if (materialName != null && !materialName.isEmpty()) {
         Material parsed = parseMaterial(materialName, null);
         if (parsed != null) {
            return parsed.name();
         } else {
            String converted = convertMaterialForLegacy(materialName);
            if (converted != null) {
               Material convertedMat = parseMaterial(converted, null);
               if (convertedMat != null) {
                  return convertedMat.name();
               }
            }

            return defaultMaterial;
         }
      } else {
         return defaultMaterial;
      }
   }

   public static String convertMaterialForLegacy(String materialName) {
      if (materialName != null && !materialName.isEmpty()) {
         String upper = materialName.toUpperCase();
         if (upper.endsWith("_STAINED_GLASS_PANE")) {
            return "STAINED_GLASS_PANE";
         } else {
            return upper.equals("PLAYER_HEAD") ? "SKULL_ITEM" : null;
         }
      } else {
         return null;
      }
   }

   public static boolean isValidMaterial(String materialName) {
      if (materialName != null && !materialName.isEmpty()) {
         Material parsed = parseMaterial(materialName, null);
         return parsed != null;
      } else {
         return false;
      }
   }
}
