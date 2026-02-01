package co.aikar.commands.contexts;

import co.aikar.commands.CommandExecutionContext;
import co.aikar.commands.CommandIssuer;

public interface OptionalContextResolver<T, C extends CommandExecutionContext<?, ? extends CommandIssuer>> extends ContextResolver<T, C> {
}
