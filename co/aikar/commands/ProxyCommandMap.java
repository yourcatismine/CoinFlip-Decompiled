package co.aikar.commands;

import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;

class ProxyCommandMap extends SimpleCommandMap {
   private BukkitCommandManager manager;
   CommandMap proxied;

   ProxyCommandMap(BukkitCommandManager manager, CommandMap proxied) {
      super(Bukkit.getServer());
      this.manager = manager;
      this.proxied = proxied;
   }

   public void registerAll(String fallbackPrefix, List<Command> commands) {
      this.proxied.registerAll(fallbackPrefix, commands);
   }

   public boolean register(String label, String fallbackPrefix, Command command) {
      return this.isOurCommand(command) ? super.register(label, fallbackPrefix, command) : this.proxied.register(label, fallbackPrefix, command);
   }

   boolean isOurCommand(String cmdLine) {
      String[] args = ACFPatterns.SPACE.split(cmdLine);
      return args.length != 0 && this.isOurCommand((Command)this.knownCommands.get(args[0].toLowerCase(Locale.ENGLISH)));
   }

   boolean isOurCommand(Command command) {
      return command instanceof RootCommand && ((RootCommand)command).getManager() == this.manager;
   }

   public boolean register(String fallbackPrefix, Command command) {
      return this.isOurCommand(command) ? super.register(fallbackPrefix, command) : this.proxied.register(fallbackPrefix, command);
   }

   public boolean dispatch(CommandSender sender, String cmdLine) throws CommandException {
      return this.isOurCommand(cmdLine) ? super.dispatch(sender, cmdLine) : this.proxied.dispatch(sender, cmdLine);
   }

   public void clearCommands() {
      super.clearCommands();
      this.proxied.clearCommands();
   }

   public Command getCommand(String name) {
      return this.isOurCommand(name) ? super.getCommand(name) : this.proxied.getCommand(name);
   }

   public List<String> tabComplete(CommandSender sender, String cmdLine) throws IllegalArgumentException {
      return this.isOurCommand(cmdLine) ? super.tabComplete(sender, cmdLine) : this.proxied.tabComplete(sender, cmdLine);
   }
}
