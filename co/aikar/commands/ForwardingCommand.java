package co.aikar.commands;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ForwardingCommand extends BaseCommand {
   private final BaseCommand command;
   private final String[] baseArgs;
   private final RegisteredCommand regCommand;

   ForwardingCommand(BaseCommand baseCommand, RegisteredCommand regCommand, String[] baseArgs) {
      this.regCommand = regCommand;
      this.commandName = baseCommand.commandName;
      this.command = baseCommand;
      this.baseArgs = baseArgs;
      this.manager = baseCommand.manager;
      this.subCommands.put("__default", regCommand);
   }

   @Override
   public List<RegisteredCommand> getRegisteredCommands() {
      return Collections.singletonList(this.regCommand);
   }

   @Override
   public CommandOperationContext getLastCommandOperationContext() {
      return this.command.getLastCommandOperationContext();
   }

   @Override
   public Set<String> getRequiredPermissions() {
      return this.command.getRequiredPermissions();
   }

   @Override
   public boolean hasPermission(Object issuer) {
      return this.command.hasPermission(issuer);
   }

   @Override
   public boolean requiresPermission(String permission) {
      return this.command.requiresPermission(permission);
   }

   @Override
   public boolean hasPermission(CommandIssuer sender) {
      return this.command.hasPermission(sender);
   }

   @Override
   public List<String> tabComplete(CommandIssuer issuer, RootCommand rootCommand, String[] args, boolean isAsync) throws IllegalArgumentException {
      return this.command.tabComplete(issuer, rootCommand, args, isAsync);
   }

   @Override
   public void execute(CommandIssuer issuer, CommandRouter.CommandRouteResult result) {
      result = new CommandRouter.CommandRouteResult(this.regCommand, result.args, ACFUtil.join(this.baseArgs), result.commandLabel);
      this.command.execute(issuer, result);
   }

   BaseCommand getCommand() {
      return this.command;
   }
}
