package co.aikar.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BukkitConditionContext extends ConditionContext<BukkitCommandIssuer> {
   protected BukkitConditionContext(BukkitCommandIssuer issuer, String config) {
      super(issuer, config);
   }

   public CommandSender getSender() {
      return this.getIssuer().getIssuer();
   }

   public Player getPlayer() {
      return this.getIssuer().getPlayer();
   }
}
