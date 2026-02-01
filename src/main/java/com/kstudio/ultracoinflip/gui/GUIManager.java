package com.kstudio.ultracoinflip.gui;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.gui.cache.ClickCooldownManager;
import com.kstudio.ultracoinflip.util.DebugManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIManager {
   private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>();
   private Logger logger;
   private KStudio plugin;
   private final ClickCooldownManager clickCooldownManager = new ClickCooldownManager(100L);

   public void setLogger(Logger logger) {
      this.logger = logger;
   }

   public void setPlugin(KStudio plugin) {
      this.plugin = plugin;
   }

   public void openGUI(InventoryGUI gui, Player player) {
      if (gui == null) {
         if (this.logger != null) {
            this.logger.warning("Attempted to open null GUI");
         }
      } else if (player != null && player.isOnline()) {
         try {
            if (player.isDead() || !player.isValid()) {
               if (this.logger != null) {
                  this.logger.warning("Attempted to open GUI for invalid player: " + player.getName());
               }

               return;
            }
         } catch (Exception var20) {
         }

         try {
            Inventory inventory = gui.getInventory();
            if (inventory == null) {
               if (this.logger != null) {
                  this.logger.warning("GUI inventory is null");
               }

               return;
            }

            if (inventory.getSize() <= 0 || inventory.getSize() > 54) {
               if (this.logger != null) {
                  this.logger.warning("Invalid inventory size: " + inventory.getSize());
               }

               return;
            }

            try {
               Inventory currentTop = GUIHelper.getTopInventorySafely(player);
               if (currentTop != null && !currentTop.equals(inventory)) {
                  this.unregisterInventory(currentTop);
                  player.closeInventory();
               }
            } catch (IncompatibleClassChangeError var15) {
               try {
                  player.closeInventory();
               } catch (Exception var13) {
               }
            } catch (Exception var16) {
               if (this.logger != null) {
                  this.logger.warning("Error closing existing inventory for " + player.getName() + ": " + var16.getMessage());
               }
            }

            if (!player.isOnline()) {
               if (this.logger != null) {
                  this.logger.warning("Player went offline before GUI could open: " + player.getName());
               }

               return;
            }

            if (this.plugin != null && this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               ItemStack cursorBefore = player.getItemOnCursor();
               ItemStack viewCursorBefore = null;
               boolean hasOpenInv = false;

               try {
                  Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
                  Object view = getOpenInventoryMethod.invoke(player);
                  if (view != null) {
                     hasOpenInv = true;
                     Method getCursorMethod = view.getClass().getMethod("getCursor");
                     Object cursor = getCursorMethod.invoke(view);
                     if (cursor instanceof ItemStack) {
                        viewCursorBefore = (ItemStack)cursor;
                     }
                  }
               } catch (Throwable var12) {
                  hasOpenInv = false;
               }

               this.plugin
                  .getDebugManager()
                  .debug(
                     DebugManager.Category.GUI,
                     DebugManager.Level.INFO,
                     String.format(
                        "[GUIManager:before-open] player=%s cursor=%s viewCursor=%s hasOpenInv=%s",
                        player.getName(),
                        this.describeItem(cursorBefore),
                        this.describeItem(viewCursorBefore),
                        hasOpenInv
                     )
                  );
            }

            this.registerHandledInventory(inventory, gui);
            player.openInventory(inventory);
            if (this.plugin != null && this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
               ItemStack cursorAfter = player.getItemOnCursor();
               ItemStack viewCursorAfter = null;

               try {
                  Method getOpenInventoryMethod = player.getClass().getMethod("getOpenInventory");
                  Object viewAfter = getOpenInventoryMethod.invoke(player);
                  if (viewAfter != null) {
                     Method getCursorMethod = viewAfter.getClass().getMethod("getCursor");
                     Object cursor = getCursorMethod.invoke(viewAfter);
                     if (cursor instanceof ItemStack) {
                        viewCursorAfter = (ItemStack)cursor;
                     }
                  }
               } catch (Throwable var11) {
               }

               Inventory topInv = GUIHelper.getTopInventorySafely(player);
               this.plugin
                  .getDebugManager()
                  .debug(
                     DebugManager.Category.GUI,
                     DebugManager.Level.INFO,
                     String.format(
                        "[GUIManager:after-open] player=%s cursor=%s viewCursor=%s topInvSize=%d",
                        player.getName(),
                        this.describeItem(cursorAfter),
                        this.describeItem(viewCursorAfter),
                        topInv != null ? topInv.getSize() : 0
                     )
                  );
            }
         } catch (IncompatibleClassChangeError var17) {
            try {
               Inventory inventoryx = gui.getInventory();
               if (inventoryx != null && inventoryx.getSize() > 0 && inventoryx.getSize() <= 54) {
                  this.registerHandledInventory(inventoryx, gui);
                  player.openInventory(inventoryx);
               } else if (this.logger != null) {
                  this.logger.warning("Failed to open GUI for player " + player.getName() + ": Invalid inventory");
               }
            } catch (Exception var14) {
               if (this.logger != null) {
                  this.logger.severe("Failed to open GUI for player " + player.getName() + " due to compatibility error: " + var14.getMessage());
               }
            }
         } catch (IllegalStateException var18) {
            if (this.logger != null) {
               this.logger.warning("Failed to open GUI for player " + player.getName() + ": " + var18.getMessage());
            }
         } catch (Exception var19) {
            if (this.logger != null) {
               this.logger.severe("Unexpected error opening GUI for player " + player.getName());
               var19.printStackTrace();
            }
         }
      } else {
         if (this.logger != null) {
            this.logger.warning("Attempted to open GUI for null or offline player");
         }
      }
   }

   public void registerHandledInventory(Inventory inventory, InventoryHandler handler) {
      this.activeInventories.put(inventory, handler);
   }

   public void unregisterInventory(Inventory inventory) {
      this.activeInventories.remove(inventory);
   }

   public InventoryHandler getHandler(Inventory inventory) {
      return inventory == null ? null : this.activeInventories.get(inventory);
   }

   public <T extends InventoryHandler> void forEachHandler(Class<T> type, Consumer<T> consumer) {
      if (type != null && consumer != null) {
         for (InventoryHandler handler : new ArrayList<>(this.activeInventories.values())) {
            if (type.isInstance(handler)) {
               consumer.accept(type.cast(handler));
            }
         }
      }
   }

   public void handleClick(InventoryClickEvent event) {
      if (event != null) {
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (player.isOnline()) {
               if (this.activeInventories.isEmpty()) {
                  if (this.plugin != null
                     && this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     this.plugin
                        .getDebugManager()
                        .verbose(DebugManager.Category.GUI, "[GUIManager:handleClick] SKIP - No active GUIs registered, letting event pass through");
                  }
               } else {
                  if (this.plugin != null
                     && this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     Inventory clickedInv = event.getClickedInventory();
                     Inventory topInv = GUIHelper.getTopInventorySafely(event, player);
                     boolean clickedIsOurs = clickedInv != null && this.activeInventories.containsKey(clickedInv);
                     boolean topIsOurs = topInv != null && this.activeInventories.containsKey(topInv);
                     this.plugin
                        .getDebugManager()
                        .debug(
                           DebugManager.Category.GUI,
                           DebugManager.Level.INFO,
                           String.format(
                              "[GUIManager:handleClick] player=%s clickedInv=%s topInv=%s clickedIsOurs=%s topIsOurs=%s activeGUIs=%d slot=%d action=%s click=%s",
                              player.getName(),
                              clickedInv != null ? clickedInv.getType().name() : "null",
                              topInv != null ? topInv.getType().name() : "null",
                              clickedIsOurs,
                              topIsOurs,
                              this.activeInventories.size(),
                              event.getSlot(),
                              event.getAction().name(),
                              event.getClick().name()
                           )
                        );
                  }

                  Inventory clickedInventory = event.getClickedInventory();
                  if (clickedInventory != null && this.activeInventories.containsKey(clickedInventory)) {
                     event.setCancelled(true);
                     event.setResult(Result.DENY);
                     if (this.clickCooldownManager.canClick(player)) {
                        InventoryHandler handler = this.activeInventories.get(clickedInventory);
                        if (handler != null) {
                           try {
                              handler.onClick(event);
                           } catch (Exception var9) {
                              if (this.logger != null) {
                                 this.logger.warning("Error handling click event for player " + player.getName() + ": " + var9.getMessage());
                              }
                           }
                        }
                     }
                  } else {
                     Inventory topInventory = GUIHelper.getTopInventorySafely(event, player);
                     if (topInventory == null) {
                        if (clickedInventory != null) {
                           for (Inventory guiInventory : this.activeInventories.keySet()) {
                              if (guiInventory != null && guiInventory.equals(clickedInventory)) {
                                 event.setCancelled(true);
                                 event.setResult(Result.DENY);
                                 if (!this.clickCooldownManager.canClick(player)) {
                                    return;
                                 }

                                 InventoryHandler handler = this.activeInventories.get(guiInventory);
                                 if (handler != null) {
                                    try {
                                       handler.onClick(event);
                                    } catch (Exception var10) {
                                       if (this.logger != null) {
                                          this.logger.warning("Error handling click event for player " + player.getName() + ": " + var10.getMessage());
                                       }
                                    }
                                 }

                                 return;
                              }
                           }
                        }
                     } else {
                        if (topInventory != null) {
                           InventoryHandler handler = this.activeInventories.get(topInventory);
                           if (handler != null) {
                              event.setCancelled(true);
                              event.setResult(Result.DENY);
                              if (!this.clickCooldownManager.canClick(player)) {
                                 return;
                              }

                              try {
                                 handler.onClick(event);
                              } catch (Exception var11) {
                                 if (this.logger != null) {
                                    this.logger.warning("Error handling click event for player " + player.getName() + ": " + var11.getMessage());
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void handleDrag(InventoryDragEvent event) {
      if (event != null) {
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (player.isOnline()) {
               if (this.activeInventories.isEmpty()) {
                  if (this.plugin != null
                     && this.plugin.getDebugManager() != null
                     && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)) {
                     this.plugin
                        .getDebugManager()
                        .verbose(DebugManager.Category.GUI, "[GUIManager:handleDrag] SKIP - No active GUIs registered, letting event pass through");
                  }
               } else {
                  Inventory topInventory = GUIHelper.getTopInventorySafely(player);
                  if (topInventory != null) {
                     InventoryHandler handler = this.activeInventories.get(topInventory);
                     if (handler != null) {
                        int topSize = topInventory.getSize();

                        for (int slot : event.getRawSlots()) {
                           if (slot < topSize) {
                              event.setCancelled(true);
                              event.setResult(Result.DENY);
                              return;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void handleOpen(InventoryOpenEvent event) {
      InventoryHandler handler = this.activeInventories.get(event.getInventory());
      if (handler != null) {
         handler.onOpen(event);
      }
   }

   public void handleClose(InventoryCloseEvent event) {
      if (event != null) {
         Inventory inventory = event.getInventory();
         if (inventory != null) {
            if (this.plugin != null
               && this.plugin.getDebugManager() != null
               && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GUI)
               && event.getPlayer() instanceof Player) {
               Player player = (Player)event.getPlayer();
               ItemStack cursor = player.getItemOnCursor();
               this.plugin
                  .getDebugManager()
                  .debug(
                     DebugManager.Category.GUI,
                     DebugManager.Level.INFO,
                     String.format("[GUIManager:on-close] player=%s cursor=%s", player.getName(), this.describeItem(cursor))
                  );
            }

            InventoryHandler handler = this.activeInventories.get(inventory);
            if (handler != null) {
               try {
                  handler.onClose(event);
               } catch (Exception var19) {
                  if (this.logger != null) {
                     this.logger.warning("Error handling close event: " + var19.getMessage());
                  }
               } finally {
                  boolean hasOtherViewers = false;

                  try {
                     List<HumanEntity> viewers = inventory.getViewers();
                     if (viewers != null) {
                        for (HumanEntity viewer : viewers) {
                           if (viewer != null && !viewer.equals(event.getPlayer())) {
                              hasOtherViewers = true;
                              break;
                           }
                        }
                     }
                  } catch (Exception var18) {
                     hasOtherViewers = true;
                  }

                  if (!hasOtherViewers) {
                     this.unregisterInventory(inventory);
                  }
               }
            } else {
               this.unregisterInventory(inventory);
            }
         }
      }
   }

   private String describeItem(ItemStack item) {
      return item != null && item.getType() != Material.AIR ? item.getType().name() + "x" + item.getAmount() : "empty";
   }
}
