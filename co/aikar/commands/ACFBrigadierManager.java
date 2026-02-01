package co.aikar.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Deprecated
@UnstableAPI
public class ACFBrigadierManager<S> {
   protected final CommandManager<?, ?, ?, ?, ?, ?> manager;
   private final Map<Class<?>, ArgumentType<?>> arguments = new HashMap<>();

   ACFBrigadierManager(CommandManager<?, ?, ?, ?, ?, ?> manager) {
      manager.verifyUnstableAPI("brigadier");
      this.manager = manager;
      this.registerArgument(String.class, StringArgumentType.word());
      this.registerArgument(float.class, FloatArgumentType.floatArg());
      this.registerArgument(Float.class, FloatArgumentType.floatArg());
      this.registerArgument(double.class, DoubleArgumentType.doubleArg());
      this.registerArgument(Double.class, DoubleArgumentType.doubleArg());
      this.registerArgument(boolean.class, BoolArgumentType.bool());
      this.registerArgument(Boolean.class, BoolArgumentType.bool());
      this.registerArgument(int.class, IntegerArgumentType.integer());
      this.registerArgument(Integer.class, IntegerArgumentType.integer());
      this.registerArgument(long.class, IntegerArgumentType.integer());
      this.registerArgument(Long.class, IntegerArgumentType.integer());
   }

   <T> void registerArgument(Class<T> clazz, ArgumentType<?> type) {
      this.arguments.put(clazz, type);
   }

   ArgumentType<Object> getArgumentTypeByClazz(CommandParameter param) {
      return (ArgumentType<Object>)(param.consumesRest
         ? StringArgumentType.greedyString()
         : (ArgumentType)this.arguments.getOrDefault(param.getType(), StringArgumentType.string()));
   }

   LiteralCommandNode<S> register(
      RootCommand rootCommand,
      LiteralCommandNode<S> root,
      SuggestionProvider<S> suggestionProvider,
      Command<S> executor,
      BiPredicate<RootCommand, S> permCheckerRoot,
      BiPredicate<RegisteredCommand, S> permCheckerSub
   ) {
      LiteralArgumentBuilder<S> rootBuilder = (LiteralArgumentBuilder<S>)LiteralArgumentBuilder.literal(root.getLiteral())
         .requires(sender -> permCheckerRoot.test(rootCommand, (S)sender));
      RegisteredCommand defaultCommand = rootCommand.getDefaultRegisteredCommand();
      if (defaultCommand != null && defaultCommand.requiredResolvers == 0) {
         rootBuilder.executes(executor);
      }

      root = rootBuilder.build();
      boolean isForwardingCommand = rootCommand.getDefCommand() instanceof ForwardingCommand;
      if (defaultCommand != null) {
         this.registerParameters(defaultCommand, root, suggestionProvider, executor, permCheckerSub);
      }

      for (Entry<String, RegisteredCommand> subCommand : rootCommand.getSubCommands().entries()) {
         if ((!BaseCommand.isSpecialSubcommand(subCommand.getKey()) || isForwardingCommand)
            && (subCommand.getKey().equals("help") || !subCommand.getValue().prefSubCommand.equals("help"))) {
            String commandName = subCommand.getKey();
            CommandNode<S> currentParent = root;
            Predicate<S> subPermChecker = sender -> permCheckerSub.test(subCommand.getValue(), sender);
            CommandNode<S> subCommandNode;
            if (isForwardingCommand) {
               subCommandNode = root;
            } else {
               if (commandName.contains(" ")) {
                  String[] split = ACFPatterns.SPACE.split(commandName);

                  for (int i = 0; i < split.length - 1; i++) {
                     if (currentParent.getChild(split[i]) == null) {
                        LiteralCommandNode<S> sub = ((LiteralArgumentBuilder)LiteralArgumentBuilder.literal(split[i]).requires(subPermChecker)).build();
                        currentParent.addChild(sub);
                        currentParent = sub;
                     } else {
                        currentParent = currentParent.getChild(split[i]);
                     }
                  }

                  commandName = split[split.length - 1];
               }

               subCommandNode = currentParent.getChild(commandName);
               if (subCommandNode == null) {
                  LiteralArgumentBuilder<S> argumentBuilder = (LiteralArgumentBuilder<S>)LiteralArgumentBuilder.literal(commandName).requires(subPermChecker);
                  if (subCommand.getValue().requiredResolvers == 0) {
                     argumentBuilder.executes(executor);
                  }

                  subCommandNode = argumentBuilder.build();
               }
            }

            this.registerParameters(subCommand.getValue(), subCommandNode, suggestionProvider, executor, permCheckerSub);
            if (!isForwardingCommand) {
               currentParent.addChild(subCommandNode);
            }
         }
      }

      return root;
   }

   void registerParameters(
      RegisteredCommand command,
      CommandNode<S> node,
      SuggestionProvider<S> suggestionProvider,
      Command<S> executor,
      BiPredicate<RegisteredCommand, S> permChecker
   ) {
      for (int i = 0; i < command.parameters.length; i++) {
         CommandParameter param = command.parameters[i];
         CommandParameter nextParam = param.getNextParam();
         if (!param.isCommandIssuer() && (!param.canExecuteWithoutInput() || nextParam == null || nextParam.canExecuteWithoutInput())) {
            RequiredArgumentBuilder<S, Object> builder = (RequiredArgumentBuilder<S, Object>)RequiredArgumentBuilder.argument(
                  param.getName(), this.getArgumentTypeByClazz(param)
               )
               .suggests(suggestionProvider)
               .requires(sender -> permChecker.test(command, (S)sender));
            if (nextParam == null || nextParam.canExecuteWithoutInput()) {
               builder.executes(executor);
            }

            CommandNode<S> subSubCommand = builder.build();
            node.addChild(subSubCommand);
            node = subSubCommand;
         }
      }
   }
}
