package co.aikar.commands;

import java.lang.reflect.Method;

public class BukkitRegisteredCommand extends RegisteredCommand<BukkitCommandExecutionContext> {
   BukkitRegisteredCommand(BaseCommand scope, String command, Method method, String prefSubCommand) {
      super(scope, command, method, prefSubCommand);
   }
}
