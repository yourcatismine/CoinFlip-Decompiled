package co.aikar.commands.contexts;

import org.bukkit.entity.Player;

@Deprecated
public class OnlinePlayer extends co.aikar.commands.bukkit.contexts.OnlinePlayer {
   public OnlinePlayer(Player player) {
      super(player);
   }
}
