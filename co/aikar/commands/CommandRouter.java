package co.aikar.commands;

import co.aikar.commands.apachecommonslang.ApacheCommonsLangUtil;
import com.google.common.collect.SetMultimap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

class CommandRouter {
   private final CommandManager manager;

   CommandRouter(CommandManager manager) {
      this.manager = manager;
   }

   CommandRouter.CommandRouteResult matchCommand(CommandRouter.RouteSearch search, boolean completion) {
      Set<RegisteredCommand> cmds = search.commands;
      String[] args = search.args;
      if (!cmds.isEmpty()) {
         if (cmds.size() == 1) {
            return new CommandRouter.CommandRouteResult(ACFUtil.getFirstElement(cmds), search);
         }

         Optional<RegisteredCommand> optCmd = cmds.stream().filter(c -> this.isProbableMatch(c, args, completion)).min((c1, c2) -> {
            int a = c1.consumeInputResolvers;
            int b = c2.consumeInputResolvers;
            if (a == b) {
               return 0;
            } else {
               return a < b ? 1 : -1;
            }
         });
         if (optCmd.isPresent()) {
            return new CommandRouter.CommandRouteResult(optCmd.get(), search);
         }
      }

      return null;
   }

   private boolean isProbableMatch(RegisteredCommand c, String[] args, boolean completion) {
      int required = c.requiredResolvers;
      int optional = c.optionalResolvers;
      return args.length <= required + optional && (completion || args.length >= required);
   }

   CommandRouter.RouteSearch routeCommand(RootCommand command, String commandLabel, String[] args, boolean completion) {
      SetMultimap<String, RegisteredCommand> subCommands = command.getSubCommands();
      int argLength = args.length;

      for (int i = argLength; i >= 0; i--) {
         String subcommand = ApacheCommonsLangUtil.join(args, " ", 0, i).toLowerCase(Locale.ENGLISH);
         Set<RegisteredCommand> cmds = subCommands.get(subcommand);
         if (!cmds.isEmpty()) {
            return new CommandRouter.RouteSearch(cmds, Arrays.copyOfRange(args, i, argLength), commandLabel, subcommand, completion);
         }
      }

      Set<RegisteredCommand> defaultCommands = subCommands.get("__default");
      Set<RegisteredCommand> unknownCommands = subCommands.get("__catchunknown");
      if (!defaultCommands.isEmpty()) {
         Set<RegisteredCommand> matchedDefault = new HashSet<>();

         for (RegisteredCommand c : defaultCommands) {
            int required = c.requiredResolvers;
            int optional = c.optionalResolvers;
            CommandParameter lastParam = c.parameters.length > 0 ? c.parameters[c.parameters.length - 1] : null;
            if (argLength <= required + optional
               || lastParam != null && (lastParam.getType() == String[].class || argLength >= required && lastParam.consumesRest)) {
               matchedDefault.add(c);
            }
         }

         if (!matchedDefault.isEmpty()) {
            return new CommandRouter.RouteSearch(matchedDefault, args, commandLabel, null, completion);
         }
      }

      return !unknownCommands.isEmpty() ? new CommandRouter.RouteSearch(unknownCommands, args, commandLabel, null, completion) : null;
   }

   static class CommandRouteResult {
      final RegisteredCommand cmd;
      final String[] args;
      final String commandLabel;
      final String subcommand;

      CommandRouteResult(RegisteredCommand cmd, CommandRouter.RouteSearch search) {
         this(cmd, search.args, search.subcommand, search.commandLabel);
      }

      CommandRouteResult(RegisteredCommand cmd, String[] args, String subcommand, String commandLabel) {
         this.cmd = cmd;
         this.args = args;
         this.commandLabel = commandLabel;
         this.subcommand = subcommand;
      }
   }

   static class RouteSearch {
      final String[] args;
      final Set<RegisteredCommand> commands;
      final String commandLabel;
      final String subcommand;

      RouteSearch(Set<RegisteredCommand> commands, String[] args, String commandLabel, String subcommand, boolean completion) {
         this.commands = commands;
         this.args = args;
         this.commandLabel = commandLabel.toLowerCase(Locale.ENGLISH);
         this.subcommand = subcommand;
      }
   }
}
