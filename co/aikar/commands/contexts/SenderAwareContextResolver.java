package co.aikar.commands.contexts;

import co.aikar.commands.CommandExecutionContext;
import co.aikar.commands.CommandIssuer;

@Deprecated
public interface SenderAwareContextResolver<T, C extends CommandExecutionContext<?, ? extends CommandIssuer>> extends IssuerAwareContextResolver<T, C> {
}
