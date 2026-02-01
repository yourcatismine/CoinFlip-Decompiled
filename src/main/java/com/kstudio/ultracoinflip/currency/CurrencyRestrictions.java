package com.kstudio.ultracoinflip.currency;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CurrencyRestrictions {
   private List<String> allowedWorlds;
   private List<String> blockedWorlds;
   private List<String> requiredPermissions;
   private boolean enabled;

   public CurrencyRestrictions() {
      this.allowedWorlds = new ArrayList<>();
      this.blockedWorlds = new ArrayList<>();
      this.requiredPermissions = new ArrayList<>();
      this.enabled = false;
   }

   public CurrencyRestrictions(List<String> allowedWorlds, List<String> blockedWorlds, List<String> requiredPermissions, boolean enabled) {
      this.allowedWorlds = allowedWorlds != null ? new ArrayList<>(allowedWorlds) : new ArrayList<>();
      this.blockedWorlds = blockedWorlds != null ? new ArrayList<>(blockedWorlds) : new ArrayList<>();
      this.requiredPermissions = requiredPermissions != null ? new ArrayList<>(requiredPermissions) : new ArrayList<>();
      this.enabled = enabled;
   }

   public boolean isRestrictionsEnabled() {
      return this.enabled;
   }

   public boolean isWorldAllowed(String worldName) {
      if (!this.enabled) {
         return true;
      } else if (this.blockedWorlds != null && !this.blockedWorlds.isEmpty() && this.blockedWorlds.contains(worldName)) {
         return false;
      } else {
         return this.allowedWorlds != null && !this.allowedWorlds.isEmpty() ? this.allowedWorlds.contains(worldName) : true;
      }
   }

   public boolean hasRequiredPermissions(Player player) {
      if (!this.enabled) {
         return true;
      } else if (this.requiredPermissions != null && !this.requiredPermissions.isEmpty()) {
         for (String permission : this.requiredPermissions) {
            if (player.hasPermission(permission)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public boolean canPlayerUse(Player player) {
      if (player == null) {
         return false;
      } else if (!this.enabled) {
         return true;
      } else {
         World world = player.getWorld();
         if (world == null) {
            return false;
         } else {
            String worldName = world.getName();
            if (worldName == null || worldName.isEmpty()) {
               return false;
            } else {
               return !this.isWorldAllowed(worldName) ? false : this.hasRequiredPermissions(player);
            }
         }
      }
   }

   public boolean canPlayersGambleTogether(Player player1, Player player2) {
      return this.canPlayerUse(player1) && this.canPlayerUse(player2);
   }

   @Deprecated
   public String getRestrictionReason(Player player) {
      if (player == null) {
         return null;
      } else if (!this.enabled) {
         return null;
      } else {
         World world = player.getWorld();
         if (world == null) {
            return null;
         } else {
            String worldName = world.getName();
            if (worldName != null && !worldName.isEmpty()) {
               if (!this.isWorldAllowed(worldName)) {
                  if (this.blockedWorlds != null && this.blockedWorlds.contains(worldName)) {
                     return "This currency is blocked in world '" + worldName + "'";
                  }

                  if (this.allowedWorlds != null && !this.allowedWorlds.isEmpty()) {
                     return "This currency is only allowed in worlds: " + String.join(", ", this.allowedWorlds);
                  }
               }

               if (!this.hasRequiredPermissions(player) && this.requiredPermissions != null && !this.requiredPermissions.isEmpty()) {
                  return this.requiredPermissions.size() == 1
                     ? "This currency requires permission: " + this.requiredPermissions.get(0)
                     : "This currency requires one of these permissions: " + String.join(", ", this.requiredPermissions);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      }
   }

   @Generated
   public List<String> getAllowedWorlds() {
      return this.allowedWorlds;
   }

   @Generated
   public List<String> getBlockedWorlds() {
      return this.blockedWorlds;
   }

   @Generated
   public List<String> getRequiredPermissions() {
      return this.requiredPermissions;
   }

   @Generated
   public boolean isEnabled() {
      return this.enabled;
   }

   @Generated
   public void setAllowedWorlds(List<String> allowedWorlds) {
      this.allowedWorlds = allowedWorlds;
   }

   @Generated
   public void setBlockedWorlds(List<String> blockedWorlds) {
      this.blockedWorlds = blockedWorlds;
   }

   @Generated
   public void setRequiredPermissions(List<String> requiredPermissions) {
      this.requiredPermissions = requiredPermissions;
   }

   @Generated
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
