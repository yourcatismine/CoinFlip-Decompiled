package co.aikar.commands.contexts;

import co.aikar.commands.CommandExecutionContext;
import co.aikar.commands.CommandIssuer;
import co.aikar.commands.InvalidCommandArgument;

@FunctionalInterface
public interface ContextResolver<T, C extends CommandExecutionContext<?, ? extends CommandIssuer>> {
   T getContext(C c) throws InvalidCommandArgument;
}
