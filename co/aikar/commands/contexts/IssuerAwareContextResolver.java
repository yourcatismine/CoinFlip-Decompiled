package co.aikar.commands.contexts;

import co.aikar.commands.CommandExecutionContext;
import co.aikar.commands.CommandIssuer;

public interface IssuerAwareContextResolver<T, C extends CommandExecutionContext<?, ? extends CommandIssuer>> extends ContextResolver<T, C> {
}
