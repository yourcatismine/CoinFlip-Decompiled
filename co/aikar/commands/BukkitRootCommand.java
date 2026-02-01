package co.aikar.commands;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

public class BukkitRootCommand extends Command implements RootCommand, PluginIdentifiableCommand {
   private final BukkitCommandManager manager;
   private final String name;
   private BaseCommand defCommand;
   private SetMultimap<String, RegisteredCommand> subCommands = HashMultimap.create();
   private List<BaseCommand> children = new ArrayList<>();
   boolean isRegistered = false;

   protected BukkitRootCommand(BukkitCommandManager manager, String name) {
      super(name);
      this.manager = manager;
      this.name = name;
   }

   @Override
   public String getDescription() {
      RegisteredCommand command = this.getDefaultRegisteredCommand();
      String description = null;
      if (command != null && !command.getHelpText().isEmpty()) {
         description = command.getHelpText();
      } else if (command != null && command.scope.description != null) {
         description = command.scope.description;
      } else if (this.defCommand.description != null) {
         description = this.defCommand.description;
      }

      return description != null ? this.manager.getLocales().replaceI18NStrings(description) : super.getDescription();
   }

   @Override
   public String getCommandName() {
      return this.name;
   }

   public List<String> tabComplete(CommandSender sender, String commandLabel, String[] args) throws IllegalArgumentException {
      if (commandLabel.contains(":")) {
         commandLabel = ACFPatterns.COLON.split(commandLabel, 2)[1];
      }

      return this.getTabCompletions(this.manager.getCommandIssuer(sender), commandLabel, args);
   }

   public boolean execute(CommandSender sender, String commandLabel, String[] args) {
      if (commandLabel.contains(":")) {
         commandLabel = ACFPatterns.COLON.split(commandLabel, 2)[1];
      }

      this.execute(this.manager.getCommandIssuer(sender), commandLabel, args);
      return true;
   }

   public boolean testPermissionSilent(CommandSender target) {
      return this.hasAnyPermission(this.manager.getCommandIssuer(target));
   }

   @Override
   public void addChild(BaseCommand command) {
      if (this.defCommand == null || !command.subCommands.get("__default").isEmpty()) {
         this.defCommand = command;
      }

      this.addChildShared(this.children, this.subCommands, command);
      this.setPermission(this.getUniquePermission());
   }

   @Override
   public CommandManager getManager() {
      return this.manager;
   }

   @Override
   public SetMultimap<String, RegisteredCommand> getSubCommands() {
      return this.subCommands;
   }

   @Override
   public List<BaseCommand> getChildren() {
      return this.children;
   }

   @Override
   public BaseCommand getDefCommand() {
      return this.defCommand;
   }

   public Plugin getPlugin() {
      return this.manager.getPlugin();
   }
}
