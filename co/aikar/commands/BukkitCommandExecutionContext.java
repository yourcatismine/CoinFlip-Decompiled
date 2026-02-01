package co.aikar.commands;

import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BukkitCommandExecutionContext extends CommandExecutionContext<BukkitCommandExecutionContext, BukkitCommandIssuer> {
   protected BukkitCommandExecutionContext(
      RegisteredCommand cmd, CommandParameter param, BukkitCommandIssuer sender, List<String> args, int index, Map<String, Object> passedArgs
   ) {
      super(cmd, param, sender, args, index, passedArgs);
   }

   public CommandSender getSender() {
      return this.issuer.getIssuer();
   }

   public Player getPlayer() {
      return this.issuer.getPlayer();
   }
}
