package co.aikar.commands;

import co.aikar.commands.lib.util.Table;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class CommandConditions<I extends CommandIssuer, CEC extends CommandExecutionContext<CEC, I>, CC extends ConditionContext<I>> {
   private CommandManager manager;
   private Map<String, CommandConditions.Condition<I>> conditions = new HashMap<>();
   private Table<Class<?>, String, CommandConditions.ParameterCondition<?, ?, ?>> paramConditions = new Table<>();

   CommandConditions(CommandManager manager) {
      this.manager = manager;
   }

   public CommandConditions.Condition<I> addCondition(@NotNull String id, @NotNull CommandConditions.Condition<I> handler) {
      return this.conditions.put(id.toLowerCase(Locale.ENGLISH), handler);
   }

   public <P> CommandConditions.ParameterCondition addCondition(
      Class<P> clazz, @NotNull String id, @NotNull CommandConditions.ParameterCondition<P, CEC, I> handler
   ) {
      return this.paramConditions.put(clazz, id.toLowerCase(Locale.ENGLISH), handler);
   }

   void validateConditions(CommandOperationContext context) throws InvalidCommandArgument {
      RegisteredCommand cmd = context.getRegisteredCommand();
      this.validateConditions(cmd.conditions, context);
      this.validateConditions(cmd.scope, context);
   }

   private void validateConditions(BaseCommand scope, CommandOperationContext operationContext) throws InvalidCommandArgument {
      this.validateConditions(scope.conditions, operationContext);
      if (scope.parentCommand != null) {
         this.validateConditions(scope.parentCommand, operationContext);
      }
   }

   private void validateConditions(String conditions, CommandOperationContext context) throws InvalidCommandArgument {
      if (conditions != null) {
         conditions = this.manager.getCommandReplacements().replace(conditions);
         CommandIssuer issuer = context.getCommandIssuer();

         for (String cond : ACFPatterns.PIPE.split(conditions)) {
            String[] split = ACFPatterns.COLON.split(cond, 2);
            String id = split[0].toLowerCase(Locale.ENGLISH);
            CommandConditions.Condition<I> condition = this.conditions.get(id);
            if (condition == null) {
               RegisteredCommand cmd = context.getRegisteredCommand();
               this.manager.log(LogLevel.ERROR, "Could not find command condition " + id + " for " + cmd.method.getName());
            } else {
               String config = split.length == 2 ? split[1] : null;
               CC conditionContext = (CC)this.manager.createConditionContext(issuer, config);
               condition.validateCondition(conditionContext);
            }
         }
      }
   }

   void validateConditions(CEC execContext, Object value) throws InvalidCommandArgument {
      String conditions = execContext.getCommandParameter().getConditions();
      if (conditions != null) {
         conditions = this.manager.getCommandReplacements().replace(conditions);
         I issuer = execContext.getIssuer();

         for (String cond : ACFPatterns.PIPE.split(conditions)) {
            String[] split = ACFPatterns.COLON.split(cond, 2);
            Class<?> cls = execContext.getParam().getType();
            String id = split[0].toLowerCase(Locale.ENGLISH);

            CommandConditions.ParameterCondition condition;
            do {
               condition = this.paramConditions.get(cls, id);
               if (condition != null || cls.getSuperclass() == null || cls.getSuperclass() == Object.class) {
                  break;
               }

               cls = cls.getSuperclass();
            } while (cls != null);

            if (condition == null) {
               RegisteredCommand cmd = execContext.getCmd();
               this.manager
                  .log(LogLevel.ERROR, "Could not find command condition " + id + " for " + cmd.method.getName() + "::" + execContext.getParam().getName());
            } else {
               String config = split.length == 2 ? split[1] : null;
               CC conditionContext = (CC)this.manager.createConditionContext(issuer, config);
               condition.validateCondition(conditionContext, execContext, value);
            }
         }
      }
   }

   public interface Condition<I extends CommandIssuer> {
      void validateCondition(ConditionContext<I> context) throws InvalidCommandArgument;
   }

   public interface ParameterCondition<P, CEC extends CommandExecutionContext, I extends CommandIssuer> {
      void validateCondition(ConditionContext<I> context, CEC execContext, P value) throws InvalidCommandArgument;
   }
}
