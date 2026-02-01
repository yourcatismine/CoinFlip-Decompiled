package co.aikar.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpSearchTags;
import co.aikar.commands.annotation.Private;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.contexts.ContextResolver;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class RegisteredCommand<CEC extends CommandExecutionContext<CEC, ? extends CommandIssuer>> {
   final BaseCommand scope;
   final Method method;
   final CommandParameter<CEC>[] parameters;
   final CommandManager manager;
   final List<String> registeredSubcommands = new ArrayList<>();
   String command;
   String prefSubCommand;
   String syntaxText;
   String helpText;
   String permission;
   String complete;
   String conditions;
   public String helpSearchTags;
   boolean isPrivate;
   final int requiredResolvers;
   final int consumeInputResolvers;
   final int doesNotConsumeInputResolvers;
   final int optionalResolvers;
   final Set<String> permissions = new HashSet<>();

   RegisteredCommand(BaseCommand scope, String command, Method method, String prefSubCommand) {
      this.scope = scope;
      this.manager = this.scope.manager;
      Annotations annotations = this.manager.getAnnotations();
      if (BaseCommand.isSpecialSubcommand(prefSubCommand)) {
         prefSubCommand = "";
         command = command.trim();
      }

      this.command = command + (!annotations.hasAnnotation(method, CommandAlias.class, false) && !prefSubCommand.isEmpty() ? prefSubCommand : "");
      this.method = method;
      this.prefSubCommand = prefSubCommand;
      this.permission = annotations.getAnnotationValue(method, CommandPermission.class, 9);
      this.complete = annotations.getAnnotationValue(method, CommandCompletion.class, 17);
      this.helpText = annotations.getAnnotationValue(method, Description.class, 17);
      this.conditions = annotations.getAnnotationValue(method, Conditions.class, 9);
      this.helpSearchTags = annotations.getAnnotationValue(method, HelpSearchTags.class, 9);
      this.syntaxText = annotations.getAnnotationValue(method, Syntax.class, 1);
      Parameter[] parameters = method.getParameters();
      this.parameters = new CommandParameter[parameters.length];
      this.isPrivate = annotations.hasAnnotation(method, Private.class) || annotations.getAnnotationFromClass(scope.getClass(), Private.class) != null;
      int requiredResolvers = 0;
      int consumeInputResolvers = 0;
      int doesNotConsumeInputResolvers = 0;
      int optionalResolvers = 0;
      CommandParameter<CEC> previousParam = null;

      for (int i = 0; i < parameters.length; i++) {
         CommandParameter<CEC> parameter = this.parameters[i] = new CommandParameter<>(this, parameters[i], i, i == parameters.length - 1);
         if (previousParam != null) {
            previousParam.setNextParam(parameter);
         }

         previousParam = parameter;
         if (!parameter.isCommandIssuer()) {
            if (!parameter.requiresInput()) {
               optionalResolvers++;
            } else {
               requiredResolvers++;
            }

            if (parameter.canConsumeInput()) {
               consumeInputResolvers++;
            } else {
               doesNotConsumeInputResolvers++;
            }
         }
      }

      this.requiredResolvers = requiredResolvers;
      this.consumeInputResolvers = consumeInputResolvers;
      this.doesNotConsumeInputResolvers = doesNotConsumeInputResolvers;
      this.optionalResolvers = optionalResolvers;
      this.computePermissions();
   }

   void invoke(CommandIssuer sender, List<String> args, CommandOperationContext context) {
      if (this.scope.canExecute(sender, this)) {
         this.preCommand();

         try {
            this.manager.getCommandConditions().validateConditions(context);
            Map<String, Object> passedArgs = this.resolveContexts(sender, args);
            if (passedArgs != null) {
               Object obj = this.method.invoke(this.scope, passedArgs.values().toArray());
               if (obj instanceof CompletionStage) {
                  CompletionStage<?> future = (CompletionStage<?>)obj;
                  future.exceptionally(t -> {
                     this.handleException(sender, args, t);
                     return null;
                  });
               }

               return;
            }
         } catch (Exception var10) {
            this.handleException(sender, args, var10);
            return;
         } finally {
            this.postCommand();
         }
      }
   }

   public void preCommand() {
   }

   public void postCommand() {
   }

   void handleException(CommandIssuer sender, List<String> args, Throwable e) {
      while (e instanceof ExecutionException || e instanceof CompletionException || e instanceof InvocationTargetException) {
         e = e.getCause();
      }

      if (e instanceof ShowCommandHelp) {
         ShowCommandHelp showHelp = (ShowCommandHelp)e;
         CommandHelp commandHelp = this.manager.generateCommandHelp();
         if (showHelp.search) {
            commandHelp.setSearch(showHelp.searchArgs == null ? args : showHelp.searchArgs);
         }

         commandHelp.showHelp(sender);
      } else if (e instanceof InvalidCommandArgument) {
         InvalidCommandArgument invalidCommandArg = (InvalidCommandArgument)e;
         if (invalidCommandArg.key != null) {
            sender.sendMessage(MessageType.ERROR, invalidCommandArg.key, invalidCommandArg.replacements);
         } else if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            sender.sendMessage(MessageType.ERROR, MessageKeys.ERROR_PREFIX, "{message}", e.getMessage());
         }

         if (invalidCommandArg.showSyntax) {
            this.scope.showSyntax(sender, this);
         }
      } else {
         try {
            if (!this.manager.handleUncaughtException(this.scope, this, sender, args, e)) {
               sender.sendMessage(MessageType.ERROR, MessageKeys.ERROR_PERFORMING_COMMAND);
            }

            boolean hasExceptionHandler = this.manager.defaultExceptionHandler != null || this.scope.getExceptionHandler() != null;
            if (!hasExceptionHandler || this.manager.logUnhandledExceptions) {
               this.manager.log(LogLevel.ERROR, "Exception in command: " + this.command + " " + ACFUtil.join(args), e);
            }
         } catch (Exception var6) {
            this.manager.log(LogLevel.ERROR, "Exception in handleException for command: " + this.command + " " + ACFUtil.join(args), e);
            this.manager.log(LogLevel.ERROR, "Exception triggered by exception handler:", var6);
         }
      }
   }

   @Nullable
   Map<String, Object> resolveContexts(CommandIssuer sender, List<String> args) throws InvalidCommandArgument {
      return this.resolveContexts(sender, args, null);
   }

   @Nullable
   Map<String, Object> resolveContexts(CommandIssuer sender, List<String> args, String name) throws InvalidCommandArgument {
      List<String> var26 = new ArrayList<>(args);
      String[] origArgs = var26.toArray(new String[var26.size()]);
      Map<String, Object> passedArgs = new LinkedHashMap<>();
      int remainingRequired = this.requiredResolvers;
      CommandOperationContext opContext = CommandManager.getCurrentCommandOperationContext();

      for (int i = 0; i < this.parameters.length && (name == null || !passedArgs.containsKey(name)); i++) {
         boolean isLast = i == this.parameters.length - 1;
         boolean allowOptional = remainingRequired == 0;
         CommandParameter<CEC> parameter = this.parameters[i];
         String parameterName = parameter.getName();
         Class<?> type = parameter.getType();
         ContextResolver<?, CEC> resolver = parameter.getResolver();
         CEC context = (CEC)this.manager.createCommandContext(this, parameter, sender, var26, i, passedArgs);
         boolean requiresInput = parameter.requiresInput();
         if (requiresInput && remainingRequired > 0) {
            remainingRequired--;
         }

         Set<String> parameterPermissions = parameter.getRequiredPermissions();
         if (var26.isEmpty() && (!isLast || type != String[].class)) {
            if (allowOptional && parameter.getDefaultValue() != null) {
               var26.add(parameter.getDefaultValue());
            } else {
               if (allowOptional && parameter.isOptional()) {
                  Object value;
                  if (parameter.isOptionalResolver() && this.manager.hasPermission(sender, parameterPermissions)) {
                     value = resolver.getContext(context);
                  } else {
                     value = null;
                  }

                  if (value == null && parameter.getClass().isPrimitive()) {
                     throw new IllegalStateException("Parameter " + parameter.getName() + " is primitive and does not support Optional.");
                  }

                  this.manager.getCommandConditions().validateConditions(context, value);
                  passedArgs.put(parameterName, value);
                  continue;
               }

               if (requiresInput) {
                  this.scope.showSyntax(sender, this);
                  return null;
               }
            }
         } else if (!this.manager.hasPermission(sender, parameterPermissions)) {
            sender.sendMessage(MessageType.ERROR, MessageKeys.PERMISSION_DENIED_PARAMETER, "{param}", parameterName);
            throw new InvalidCommandArgument(false);
         }

         if (parameter.getValues() != null) {
            String arg = !var26.isEmpty() ? (String)var26.get(0) : "";
            Set<String> possible = new HashSet<>();
            CommandCompletions commandCompletions = this.manager.getCommandCompletions();

            for (String s : parameter.getValues()) {
               if ("*".equals(s) || "@completions".equals(s)) {
                  s = commandCompletions.findDefaultCompletion(this, origArgs);
               }

               List<String> check = commandCompletions.getCompletionValues(this, sender, s, origArgs, opContext.isAsync());
               if (!check.isEmpty()) {
                  possible.addAll(check.stream().filter(Objects::nonNull).map(String::toLowerCase).collect(Collectors.toList()));
               } else {
                  possible.add(s.toLowerCase(Locale.ENGLISH));
               }
            }

            if (!possible.contains(arg.toLowerCase(Locale.ENGLISH))) {
               throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_ONE_OF, "{valid}", ACFUtil.join(possible, ", "));
            }
         }

         Object paramValue = resolver.getContext(context);
         this.manager.getCommandConditions().validateConditions(context, paramValue);
         passedArgs.put(parameterName, paramValue);
      }

      return passedArgs;
   }

   boolean hasPermission(CommandIssuer issuer) {
      return this.manager.hasPermission(issuer, this.getRequiredPermissions());
   }

   @Deprecated
   public String getPermission() {
      return this.permission != null && !this.permission.isEmpty() ? ACFPatterns.COMMA.split(this.permission)[0] : null;
   }

   void computePermissions() {
      this.permissions.clear();
      this.permissions.addAll(this.scope.getRequiredPermissions());
      if (this.permission != null && !this.permission.isEmpty()) {
         this.permissions.addAll(Arrays.asList(ACFPatterns.COMMA.split(this.permission)));
      }
   }

   public Set<String> getRequiredPermissions() {
      return this.permissions;
   }

   public boolean requiresPermission(String permission) {
      return this.getRequiredPermissions().contains(permission);
   }

   public String getPrefSubCommand() {
      return this.prefSubCommand;
   }

   public String getSyntaxText() {
      return this.getSyntaxText(null);
   }

   public String getSyntaxText(CommandIssuer issuer) {
      if (this.syntaxText != null) {
         return this.syntaxText;
      } else {
         StringBuilder syntaxBuilder = new StringBuilder(64);

         for (CommandParameter<?> parameter : this.parameters) {
            String syntax = parameter.getSyntax(issuer);
            if (syntax != null) {
               if (syntaxBuilder.length() > 0) {
                  syntaxBuilder.append(' ');
               }

               syntaxBuilder.append(syntax);
            }
         }

         return syntaxBuilder.toString().trim();
      }
   }

   public String getHelpText() {
      return this.helpText != null ? this.helpText : "";
   }

   public boolean isPrivate() {
      return this.isPrivate;
   }

   public String getCommand() {
      return this.command;
   }

   public void addSubcommand(String cmd) {
      this.registeredSubcommands.add(cmd);
   }

   public void addSubcommands(Collection<String> cmd) {
      this.registeredSubcommands.addAll(cmd);
   }

   public <T extends Annotation> T getAnnotation(Class<T> annotation) {
      return this.method.getAnnotation(annotation);
   }
}
