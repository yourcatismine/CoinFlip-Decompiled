package com.kstudio.ultracoinflip.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public interface InventoryHandler {
   void onClick(InventoryClickEvent var1);

   void onOpen(InventoryOpenEvent var1);

   void onClose(InventoryCloseEvent var1);
}
