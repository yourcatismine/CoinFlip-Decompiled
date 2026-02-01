package co.aikar.commands;

import org.bukkit.ChatColor;

public class BukkitMessageFormatter extends MessageFormatter<ChatColor> {
   public BukkitMessageFormatter(ChatColor... colors) {
      super(colors);
   }

   String format(ChatColor color, String message) {
      return color + message;
   }
}
