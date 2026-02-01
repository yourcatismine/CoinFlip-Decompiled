package co.aikar.commands;

import java.util.List;

@FunctionalInterface
public interface ExceptionHandler {
   boolean execute(BaseCommand command, RegisteredCommand registeredCommand, CommandIssuer sender, List<String> args, Throwable t);
}
