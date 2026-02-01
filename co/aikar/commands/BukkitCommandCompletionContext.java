package co.aikar.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BukkitCommandCompletionContext extends CommandCompletionContext<BukkitCommandIssuer> {
   protected BukkitCommandCompletionContext(RegisteredCommand command, BukkitCommandIssuer issuer, String input, String config, String[] args) {
      super(command, issuer, input, config, args);
   }

   public CommandSender getSender() {
      return this.getIssuer().getIssuer();
   }

   public Player getPlayer() {
      return this.issuer.getPlayer();
   }
}
