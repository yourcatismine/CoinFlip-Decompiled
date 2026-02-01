package com.kstudio.ultracoinflip.gui;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class InventoryGUI implements InventoryHandler {
   private Inventory inventory;
   private final Map<Integer, InventoryButton> buttonMap = new HashMap<>();

   public Inventory getInventory() {
      if (this.inventory == null) {
         try {
            this.inventory = this.createInventory();
            if (this.inventory != null && (this.inventory.getSize() % 9 != 0 || this.inventory.getSize() < 9 || this.inventory.getSize() > 54)) {
               this.inventory = null;
            }
         } catch (Exception var2) {
            this.inventory = null;
         }
      }

      return this.inventory;
   }

   public void addButton(int slot, InventoryButton button) {
      this.buttonMap.put(slot, button);
   }

   public void decorate(Player player) {
      if (player != null && player.isOnline()) {
         if (this.inventory != null) {
            this.buttonMap.forEach((slot, button) -> {
               if (button != null && button.getIconCreator() != null) {
                  try {
                     ItemStack icon = button.getIconCreator().apply(player);
                     if (icon != null && slot >= 0 && slot < this.inventory.getSize()) {
                        this.inventory.setItem(slot, icon);
                     }
                  } catch (Exception var5) {
                  }
               }
            });
         }
      }
   }

   @Override
   public void onClick(InventoryClickEvent event) {
      if (event != null) {
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (player.isOnline()) {
               this.debugClick("pre-cancel", player, event);
               Inventory clickedInventory = event.getClickedInventory();
               int rawSlot = event.getRawSlot();
               int topInventorySize = this.inventory.getSize();
               boolean isClickInTopInventory = rawSlot >= 0 && rawSlot < topInventorySize;
               if (clickedInventory != null) {
                  isClickInTopInventory = isClickInTopInventory || clickedInventory.equals(this.inventory);
               }

               this.debugClick("rawSlot=" + rawSlot + " topSize=" + topInventorySize + " isTop=" + isClickInTopInventory, player, event);
               if (!isClickInTopInventory) {
                  ClickType clickType = event.getClick();
                  InventoryAction action = event.getAction();
                  String clickTypeName = clickType.name();
                  boolean dangerousClickType = clickType == ClickType.SHIFT_LEFT
                     || clickType == ClickType.SHIFT_RIGHT
                     || clickType == ClickType.DOUBLE_CLICK
                     || clickType == ClickType.NUMBER_KEY
                     || "SWAP_OFFHAND".equals(clickTypeName);
                  boolean dangerousAction = action == InventoryAction.COLLECT_TO_CURSOR
                     || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                     || action == InventoryAction.HOTBAR_SWAP
                     || action == InventoryAction.HOTBAR_MOVE_AND_READD;
                  if (dangerousClickType || dangerousAction) {
                     event.setCancelled(true);
                     event.setResult(Result.DENY);
                     this.clearPlayerCursor(player);
                  }
               } else {
                  event.setCancelled(true);
                  event.setResult(Result.DENY);
                  ClickType clickType = event.getClick();
                  InventoryAction action = event.getAction();
                  String clickTypeName2 = clickType.name();
                  boolean dangerousClickType = clickType == ClickType.SHIFT_LEFT
                     || clickType == ClickType.SHIFT_RIGHT
                     || clickType == ClickType.NUMBER_KEY
                     || "SWAP_OFFHAND".equals(clickTypeName2)
                     || clickType == ClickType.DOUBLE_CLICK
                     || clickType == ClickType.CREATIVE
                     || clickType == ClickType.MIDDLE;
                  boolean dangerousAction = action == InventoryAction.COLLECT_TO_CURSOR
                     || action == InventoryAction.MOVE_TO_OTHER_INVENTORY
                     || action == InventoryAction.HOTBAR_SWAP
                     || action == InventoryAction.HOTBAR_MOVE_AND_READD;
                  if (!dangerousClickType && !dangerousAction) {
                     int slot = event.getSlot();
                     if (slot < 0 || slot >= this.inventory.getSize()) {
                        this.debugClick("slot-out-of-range", player, event);
                     } else if (this.inventory != null && this.inventory.getSize() != 0) {
                        KStudio plugin = this.getPlugin();
                        if (plugin != null && plugin.getDebugManager() != null && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                           ItemStack cursorBeforeClear = player.getItemOnCursor();
                           ItemStack viewCursorBeforeClear = null;

                           try {
                              Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
                              Object view = getOpenInventoryMethod.invoke(player);
                              if (view != null) {
                                 Method getCursorMethod = view.getClass().getMethod("getCursor");
                                 Object cursor = getCursorMethod.invoke(view);
                                 if (cursor instanceof ItemStack) {
                                    viewCursorBeforeClear = (ItemStack)cursor;
                                 }
                              }
                           } catch (Throwable var22) {
                           }

                           plugin.getDebugManager()
                              .debug(
                                 DebugManager.Category.GUI,
                                 DebugManager.Level.INFO,
                                 String.format(
                                    "[InventoryGUI:before-clearCursor] player=%s slot=%d cursor=%s viewCursor=%s",
                                    player.getName(),
                                    slot,
                                    this.describeItem(cursorBeforeClear),
                                    this.describeItem(viewCursorBeforeClear)
                                 )
                              );
                        }

                        this.clearPlayerCursor(player);
                        if (plugin != null && plugin.getDebugManager() != null && plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                           ItemStack cursorAfterClear = player.getItemOnCursor();
                           ItemStack viewCursorAfterClear = null;

                           try {
                              Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
                              Object viewAfter = getOpenInventoryMethod.invoke(player);
                              if (viewAfter != null) {
                                 Method getCursorMethod = viewAfter.getClass().getMethod("getCursor");
                                 Object cursor = getCursorMethod.invoke(viewAfter);
                                 if (cursor instanceof ItemStack) {
                                    viewCursorAfterClear = (ItemStack)cursor;
                                 }
                              }
                           } catch (Throwable var21) {
                           }

                           plugin.getDebugManager()
                              .debug(
                                 DebugManager.Category.GUI,
                                 DebugManager.Level.INFO,
                                 String.format(
                                    "[InventoryGUI:after-clearCursor] player=%s slot=%d cursor=%s viewCursor=%s",
                                    player.getName(),
                                    slot,
                                    this.describeItem(cursorAfterClear),
                                    this.describeItem(viewCursorAfterClear)
                                 )
                              );
                        }

                        this.debugClick("post-cancel", player, event);
                        InventoryButton button = this.buttonMap.get(slot);
                        if (button != null && button.getEventConsumer() != null) {
                           try {
                              if (!player.isOnline()) {
                                 return;
                              }

                              button.getEventConsumer().accept(event);
                              if (plugin != null && plugin.getSoundHelper() != null) {
                                 try {
                                    plugin.getSoundHelper().playSound(player, "gui.click");
                                 } catch (Exception var20) {
                                 }
                              }
                           } catch (Exception var23) {
                              if (plugin != null && plugin.getLogger() != null) {
                                 plugin.getLogger().warning("Error executing button action in GUI for player " + player.getName() + ": " + var23.getMessage());
                              }
                           }
                        }
                     } else {
                        this.debugClick("inventory-null", player, event);
                     }
                  } else {
                     this.clearPlayerCursor(player);
                  }
               }
            }
         }
      }
   }

   private void debugClick(String stage, Player player, InventoryClickEvent event) {
      KStudio plugin = this.getPlugin();
      if (plugin != null && plugin.getDebugManager() != null) {
         if (plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
            String message = String.format(
               "[InventoryGUI:%s] player=%s slot=%d raw=%d click=%s action=%s cancelled=%s result=%s cursor=%s current=%s topEquals=%s",
               stage,
               player.getName(),
               event.getSlot(),
               event.getRawSlot(),
               event.getClick(),
               event.getAction(),
               event.isCancelled(),
               event.getResult(),
               this.describeItem(event.getCursor()),
               this.describeItem(event.getCurrentItem()),
               GUIHelper.getTopInventorySafely(player) != null && GUIHelper.getTopInventorySafely(player).equals(this.inventory)
            );
            plugin.getDebugManager().debug(DebugManager.Category.GUI, DebugManager.Level.INFO, message);
         }
      }
   }

   private String describeItem(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? item.getType().name() + "x" + item.getAmount() : "empty";
   }

   private void clearPlayerCursor(Player player) {
      if (player != null) {
         try {
            GUIHelper.setCursorSafely(player, null);
         } catch (Exception var4) {
            KStudio plugin = this.getPlugin();
            if (plugin != null && plugin.getDebugManager() != null) {
               plugin.getDebugManager()
                  .debug(DebugManager.Category.GUI, DebugManager.Level.WARNING, "Failed to clear cursor for " + player.getName() + ": " + var4.getMessage());
            }
         }
      }
   }

   @Override
   public void onOpen(InventoryOpenEvent event) {
      if (event != null && event.getPlayer() != null) {
         if (event.getPlayer() instanceof Player) {
            Player player = (Player)event.getPlayer();
            if (player.isOnline()) {
               if (this.inventory != null) {
                  try {
                     if (event.getInventory() == null || !event.getInventory().equals(this.inventory)) {
                        return;
                     }

                     this.decorate(player);
                     String soundKey = this.getOpenSoundKey();
                     if (soundKey != null && !soundKey.isEmpty()) {
                        try {
                           KStudio plugin = this.getPlugin();
                           if (plugin != null && plugin.getSoundHelper() != null) {
                              plugin.getSoundHelper().playSound(player, soundKey);
                           }
                        } catch (Exception var5) {
                        }
                     }
                  } catch (Exception var6) {
                     KStudio plugin = this.getPlugin();
                     if (plugin != null && plugin.getLogger() != null) {
                        plugin.getLogger().warning("Error opening GUI for player " + player.getName() + ": " + var6.getMessage());
                     }
                  }
               }
            }
         }
      }
   }

   protected String getOpenSoundKey() {
      return null;
   }

   protected KStudio getPlugin() {
      return null;
   }

   @Override
   public void onClose(InventoryCloseEvent event) {
      if (event != null && event.getPlayer() != null) {
         if (event.getPlayer() instanceof Player) {
            if (event.getInventory() != null && event.getInventory().equals(this.inventory)) {
               boolean hasOtherViewers = false;

               try {
                  List<HumanEntity> viewers = this.inventory.getViewers();
                  if (viewers != null) {
                     for (HumanEntity viewer : viewers) {
                        if (viewer != null && !viewer.equals(event.getPlayer())) {
                           hasOtherViewers = true;
                           break;
                        }
                     }
                  }
               } catch (Exception var7) {
                  hasOtherViewers = true;
               }

               if (!hasOtherViewers) {
                  try {
                     this.buttonMap.clear();
                  } catch (Exception var6) {
                  }
               }
            }
         }
      }
   }

   protected abstract Inventory createInventory();

   @Generated
   public Map<Integer, InventoryButton> getButtonMap() {
      return this.buttonMap;
   }
}
