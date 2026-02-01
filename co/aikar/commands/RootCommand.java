package co.aikar.commands;

import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface RootCommand {
   void addChild(BaseCommand command);

   CommandManager getManager();

   SetMultimap<String, RegisteredCommand> getSubCommands();

   List<BaseCommand> getChildren();

   String getCommandName();

   default void addChildShared(List<BaseCommand> children, SetMultimap<String, RegisteredCommand> subCommands, BaseCommand command) {
      command.subCommands.entries().forEach(e -> subCommands.put((String)e.getKey(), (RegisteredCommand)e.getValue()));
      if (!children.contains(command)) {
         children.add(command);
      }
   }

   default String getUniquePermission() {
      Set<String> permissions = new HashSet<>();

      for (BaseCommand child : this.getChildren()) {
         for (RegisteredCommand<?> value : child.subCommands.values()) {
            Set<String> requiredPermissions = value.getRequiredPermissions();
            if (requiredPermissions.isEmpty()) {
               return null;
            }

            permissions.addAll(requiredPermissions);
         }
      }

      return permissions.size() == 1 ? permissions.iterator().next() : null;
   }

   default boolean hasAnyPermission(CommandIssuer issuer) {
      List<BaseCommand> children = this.getChildren();
      if (children.isEmpty()) {
         return true;
      } else {
         for (BaseCommand child : children) {
            if (child.hasPermission(issuer)) {
               for (RegisteredCommand value : child.getRegisteredCommands()) {
                  if (value.hasPermission(issuer)) {
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }

   default BaseCommand execute(CommandIssuer sender, String commandLabel, String[] args) {
      CommandRouter router = this.getManager().getRouter();
      CommandRouter.RouteSearch search = router.routeCommand(this, commandLabel, args, false);
      BaseCommand defCommand = this.getDefCommand();
      if (search != null) {
         CommandRouter.CommandRouteResult result = router.matchCommand(search, false);
         if (result != null) {
            BaseCommand scope = result.cmd.scope;
            scope.execute(sender, result);
            return scope;
         }

         RegisteredCommand firstElement = ACFUtil.getFirstElement(search.commands);
         if (firstElement != null) {
            defCommand = firstElement.scope;
         }
      }

      defCommand.help(sender, args);
      return defCommand;
   }

   default List<String> getTabCompletions(CommandIssuer sender, String alias, String[] args) {
      return this.getTabCompletions(sender, alias, args, false);
   }

   default List<String> getTabCompletions(CommandIssuer sender, String alias, String[] args, boolean commandsOnly) {
      return this.getTabCompletions(sender, alias, args, commandsOnly, false);
   }

   default List<String> getTabCompletions(CommandIssuer sender, String alias, String[] args, boolean commandsOnly, boolean isAsync) {
      Set<String> completions = new HashSet<>();
      this.getChildren().forEach(child -> {
         if (!commandsOnly) {
            completions.addAll(child.tabComplete(sender, this, args, isAsync));
         }

         completions.addAll(child.getCommandsForCompletion(sender, args));
      });
      return new ArrayList<>(completions);
   }

   default RegisteredCommand getDefaultRegisteredCommand() {
      BaseCommand defCommand = this.getDefCommand();
      return defCommand != null ? defCommand.getDefaultRegisteredCommand() : null;
   }

   default BaseCommand getDefCommand() {
      return null;
   }

   default String getDescription() {
      RegisteredCommand cmd = this.getDefaultRegisteredCommand();
      if (cmd != null) {
         return cmd.getHelpText();
      } else {
         BaseCommand defCommand = this.getDefCommand();
         return defCommand != null && defCommand.description != null ? defCommand.description : "";
      }
   }

   default String getUsage() {
      RegisteredCommand cmd = this.getDefaultRegisteredCommand();
      if (cmd != null) {
         return cmd.syntaxText != null ? cmd.syntaxText : "";
      } else {
         return "";
      }
   }
}
