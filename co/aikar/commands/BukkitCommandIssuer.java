package co.aikar.commands;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BukkitCommandIssuer implements CommandIssuer {
   private final BukkitCommandManager manager;
   private final CommandSender sender;

   protected BukkitCommandIssuer(BukkitCommandManager manager, CommandSender sender) {
      this.manager = manager;
      this.sender = sender;
   }

   @Override
   public boolean isPlayer() {
      return this.sender instanceof Player;
   }

   public CommandSender getIssuer() {
      return this.sender;
   }

   public Player getPlayer() {
      return this.isPlayer() ? (Player)this.sender : null;
   }

   @NotNull
   @Override
   public UUID getUniqueId() {
      return this.isPlayer() ? ((Player)this.sender).getUniqueId() : UUID.nameUUIDFromBytes(this.sender.getName().getBytes(StandardCharsets.UTF_8));
   }

   @Override
   public CommandManager getManager() {
      return this.manager;
   }

   @Override
   public void sendMessageInternal(String message) {
      this.sender.sendMessage(ACFBukkitUtil.color(message));
   }

   @Override
   public boolean hasPermission(String name) {
      return this.sender.hasPermission(name);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         BukkitCommandIssuer that = (BukkitCommandIssuer)o;
         return Objects.equals(this.sender, that.sender);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.sender);
   }
}
