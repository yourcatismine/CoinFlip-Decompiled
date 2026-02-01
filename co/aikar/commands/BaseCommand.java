package co.aikar.commands;

import co.aikar.commands.annotation.CatchAll;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.PreCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.UnknownHandler;
import co.aikar.commands.apachecommonslang.ApacheCommonsLangUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public abstract class BaseCommand {
   static final String CATCHUNKNOWN = "__catchunknown";
   static final String DEFAULT = "__default";
   final SetMultimap<String, RegisteredCommand> subCommands = HashMultimap.create();
   final Set<BaseCommand> subScopes = new HashSet<>();
   final Map<Class<?>, String> contextFlags = new HashMap<>();
   @Nullable
   private Method preCommandHandler;
   private String execLabel;
   private String execSubcommand;
   private String[] origArgs;
   CommandManager<?, ?, ?, ?, ?, ?> manager = null;
   BaseCommand parentCommand;
   Map<String, RootCommand> registeredCommands = new HashMap<>();
   @Nullable
   String description;
   @Nullable
   String commandName;
   @Nullable
   String permission;
   @Nullable
   String conditions;
   boolean hasHelpCommand;
   private ExceptionHandler exceptionHandler = null;
   private final ThreadLocal<CommandOperationContext> lastCommandOperationContext = new ThreadLocal<>();
   @Nullable
   private String parentSubcommand;
   private final Set<String> permissions = new HashSet<>();

   public BaseCommand() {
   }

   @Deprecated
   public BaseCommand(@Nullable String cmd) {
      this.commandName = cmd;
   }

   public CommandOperationContext getLastCommandOperationContext() {
      return this.lastCommandOperationContext.get();
   }

   public String getExecCommandLabel() {
      return this.execLabel;
   }

   public String getExecSubcommand() {
      return this.execSubcommand;
   }

   public String[] getOrigArgs() {
      return this.origArgs;
   }

   void onRegister(CommandManager manager) {
      this.onRegister(manager, this.commandName);
   }

   private void onRegister(CommandManager manager, String cmd) {
      manager.injectDependencies(this);
      this.manager = manager;
      Annotations annotations = manager.getAnnotations();
      Class<? extends BaseCommand> self = (Class<? extends BaseCommand>)this.getClass();
      String[] cmdAliases = annotations.getAnnotationValues(self, CommandAlias.class, 11);
      if (cmd == null && cmdAliases != null) {
         cmd = cmdAliases[0];
      }

      this.commandName = cmd != null ? cmd : self.getSimpleName().toLowerCase(Locale.ENGLISH);
      this.permission = annotations.getAnnotationValue(self, CommandPermission.class, 1);
      this.description = annotations.getAnnotationValue(self, Description.class, 9);
      this.parentSubcommand = this.getParentSubcommand(self);
      this.conditions = annotations.getAnnotationValue(self, Conditions.class, 9);
      this.computePermissions();
      this.registerSubcommands();
      this.registerSubclasses(cmd);
      if (cmdAliases != null) {
         Set<String> cmdList = new HashSet<>();
         Collections.addAll(cmdList, cmdAliases);
         cmdList.remove(cmd);

         for (String cmdAlias : cmdList) {
            this.registerSubclasses(cmdAlias);
            this.register(cmdAlias, this);
         }
      }

      if (cmd != null) {
         this.register(cmd, this);
      }
   }

   private void registerSubclasses(String cmd) {
      for (Class<?> clazz : this.getClass().getDeclaredClasses()) {
         if (BaseCommand.class.isAssignableFrom(clazz)) {
            try {
               BaseCommand subScope = null;
               Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
               List<String> unusableConstructorsInfo = new ArrayList<>();

               for (Constructor<?> declaredConstructor : declaredConstructors) {
                  declaredConstructor.setAccessible(true);
                  Parameter[] parameters = declaredConstructor.getParameters();
                  if (parameters.length == 1 && parameters[0].getType().isAssignableFrom(this.getClass())) {
                     subScope = (BaseCommand)declaredConstructor.newInstance(this);
                  } else {
                     unusableConstructorsInfo.add(
                        "Found unusable constructor: "
                           + declaredConstructor.getName()
                           + "("
                           + Stream.of(parameters).map(p -> p.getType().getSimpleName() + " " + p.getName()).collect(Collectors.joining("<c2>,</c2> "))
                           + ")"
                     );
                  }
               }

               if (subScope != null) {
                  subScope.parentCommand = this;
                  this.subScopes.add(subScope);
                  subScope.onRegister(this.manager, cmd);
                  this.subCommands.putAll(subScope.subCommands);
                  this.registeredCommands.putAll(subScope.registeredCommands);
               } else {
                  this.manager.log(LogLevel.ERROR, "Could not find a subcommand ctor for " + clazz.getName());

                  for (String constructorInfo : unusableConstructorsInfo) {
                     this.manager.log(LogLevel.INFO, constructorInfo);
                  }
               }
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException var14) {
               this.manager.log(LogLevel.ERROR, "Error registering subclass", var14);
            }
         }
      }
   }

   private void registerSubcommands() {
      Annotations annotations = this.manager.getAnnotations();
      boolean foundCatchUnknown = false;
      boolean isParentEmpty = this.parentSubcommand == null || this.parentSubcommand.isEmpty();
      Set<Method> methods = new LinkedHashSet<>();
      Collections.addAll(methods, this.getClass().getDeclaredMethods());
      Collections.addAll(methods, this.getClass().getMethods());

      for (Method method : methods) {
         method.setAccessible(true);
         String sublist = null;
         String sub = this.getSubcommandValue(method);
         String helpCommand = annotations.getAnnotationValue(method, HelpCommand.class, 0);
         String commandAliases = annotations.getAnnotationValue(method, CommandAlias.class, 0);
         if (annotations.hasAnnotation(method, Default.class)) {
            if (!isParentEmpty) {
               sub = this.parentSubcommand;
            } else {
               this.registerSubcommand(method, "__default");
            }
         }

         if (sub != null) {
            sublist = sub;
         } else if (commandAliases != null) {
            sublist = commandAliases;
         } else if (helpCommand != null) {
            sublist = helpCommand;
            this.hasHelpCommand = true;
         }

         boolean preCommand = annotations.hasAnnotation(method, PreCommand.class);
         boolean hasCatchUnknown = annotations.hasAnnotation(method, CatchUnknown.class)
            || annotations.hasAnnotation(method, CatchAll.class)
            || annotations.hasAnnotation(method, UnknownHandler.class);
         if (hasCatchUnknown || !foundCatchUnknown && helpCommand != null) {
            if (!foundCatchUnknown) {
               if (hasCatchUnknown) {
                  this.subCommands.get("__catchunknown").clear();
                  foundCatchUnknown = true;
               }

               this.registerSubcommand(method, "__catchunknown");
            } else {
               ACFUtil.sneaky(
                  new IllegalStateException(
                     "Multiple @CatchUnknown/@HelpCommand commands, duplicate on " + method.getDeclaringClass().getName() + "#" + method.getName()
                  )
               );
            }
         } else if (preCommand) {
            if (this.preCommandHandler == null) {
               this.preCommandHandler = method;
            } else {
               ACFUtil.sneaky(
                  new IllegalStateException("Multiple @PreCommand commands, duplicate on " + method.getDeclaringClass().getName() + "#" + method.getName())
               );
            }
         }

         if (Objects.equals(method.getDeclaringClass(), this.getClass()) && sublist != null) {
            this.registerSubcommand(method, sublist);
         }
      }
   }

   private void computePermissions() {
      this.permissions.clear();
      if (this.permission != null && !this.permission.isEmpty()) {
         this.permissions.addAll(Arrays.asList(ACFPatterns.COMMA.split(this.permission)));
      }

      if (this.parentCommand != null) {
         this.permissions.addAll(this.parentCommand.getRequiredPermissions());
      }

      this.subCommands.values().forEach(RegisteredCommand::computePermissions);
      this.subScopes.forEach(BaseCommand::computePermissions);
   }

   private String getSubcommandValue(Method method) {
      String sub = this.manager.getAnnotations().getAnnotationValue(method, Subcommand.class, 0);
      if (sub == null) {
         return null;
      } else {
         Class<?> clazz = method.getDeclaringClass();
         String parent = this.getParentSubcommand(clazz);
         return parent != null && !parent.isEmpty() ? parent + " " + sub : sub;
      }
   }

   private String getParentSubcommand(Class<?> clazz) {
      List<String> subList = new ArrayList<>();

      while (clazz != null) {
         String sub = this.manager.getAnnotations().getAnnotationValue(clazz, Subcommand.class, 0);
         if (sub != null) {
            subList.add(sub);
         }

         clazz = clazz.getEnclosingClass();
      }

      Collections.reverse(subList);
      return ACFUtil.join(subList, " ");
   }

   private void register(String name, BaseCommand cmd) {
      String nameLower = name.toLowerCase(Locale.ENGLISH);
      RootCommand rootCommand = this.manager.obtainRootCommand(nameLower);
      rootCommand.addChild(cmd);
      this.registeredCommands.put(nameLower, rootCommand);
   }

   private void registerSubcommand(Method method, String subCommand) {
      subCommand = this.manager.getCommandReplacements().replace(subCommand.toLowerCase(Locale.ENGLISH));
      String[] subCommandParts = ACFPatterns.SPACE.split(subCommand);
      Set<String> cmdList = getSubCommandPossibilityList(subCommandParts);

      for (int i = 0; i < subCommandParts.length; i++) {
         String[] split = ACFPatterns.PIPE.split(subCommandParts[i]);
         if (split.length == 0 || split[0].isEmpty()) {
            throw new IllegalArgumentException("Invalid @Subcommand configuration for " + method.getName() + " - parts can not start with | or be empty");
         }

         subCommandParts[i] = split[0];
      }

      String prefSubCommand = ApacheCommonsLangUtil.join(subCommandParts, " ");
      String[] aliasNames = this.manager.getAnnotations().getAnnotationValues(method, CommandAlias.class, 3);
      String cmdName = aliasNames != null ? aliasNames[0] : this.commandName + " ";
      RegisteredCommand cmd = this.manager.createRegisteredCommand(this, cmdName, method, prefSubCommand);

      for (String subcmd : cmdList) {
         this.subCommands.put(subcmd, cmd);
      }

      cmd.addSubcommands(cmdList);
      if (aliasNames != null) {
         for (String name : aliasNames) {
            this.register(name, new ForwardingCommand(this, cmd, subCommandParts));
         }
      }
   }

   private static Set<String> getSubCommandPossibilityList(String[] subCommandParts) {
      int i = 0;
      Set<String> current = null;

      while (true) {
         Set<String> newList = new HashSet<>();
         if (i < subCommandParts.length) {
            for (String s1 : ACFPatterns.PIPE.split(subCommandParts[i])) {
               if (current != null) {
                  newList.addAll(current.stream().map(s -> s + " " + s1).collect(Collectors.toList()));
               } else {
                  newList.add(s1);
               }
            }
         }

         if (i + 1 >= subCommandParts.length) {
            return newList;
         }

         current = newList;
         i++;
      }
   }

   void execute(CommandIssuer issuer, CommandRouter.CommandRouteResult command) {
      try {
         CommandOperationContext commandContext = this.preCommandOperation(issuer, command.commandLabel, command.args, false);
         this.execSubcommand = command.subcommand;
         this.executeCommand(commandContext, issuer, command.args, command.cmd);
      } finally {
         this.postCommandOperation();
      }
   }

   private void postCommandOperation() {
      CommandManager.commandOperationContext.get().pop();
      this.lastCommandOperationContext.set(null);
      this.execSubcommand = null;
      this.execLabel = null;
      this.origArgs = new String[0];
   }

   private CommandOperationContext preCommandOperation(CommandIssuer issuer, String commandLabel, String[] args, boolean isAsync) {
      Stack<CommandOperationContext> contexts = CommandManager.commandOperationContext.get();
      CommandOperationContext context = this.manager.createCommandOperationContext(this, issuer, commandLabel, args, isAsync);
      contexts.push(context);
      this.lastCommandOperationContext.set(context);
      this.execSubcommand = null;
      this.execLabel = commandLabel;
      this.origArgs = args;
      return context;
   }

   public CommandIssuer getCurrentCommandIssuer() {
      return CommandManager.getCurrentCommandIssuer();
   }

   public CommandManager getCurrentCommandManager() {
      return CommandManager.getCurrentCommandManager();
   }

   private void executeCommand(CommandOperationContext commandOperationContext, CommandIssuer issuer, String[] args, RegisteredCommand cmd) {
      if (cmd.hasPermission(issuer)) {
         commandOperationContext.setRegisteredCommand(cmd);
         if (this.checkPrecommand(commandOperationContext, cmd, issuer, args)) {
            return;
         }

         List<String> sargs = Arrays.asList(args);
         cmd.invoke(issuer, sargs, commandOperationContext);
      } else {
         issuer.sendMessage(MessageType.ERROR, MessageKeys.PERMISSION_DENIED);
      }
   }

   @Deprecated
   public boolean canExecute(CommandIssuer issuer, RegisteredCommand<?> cmd) {
      return true;
   }

   public List<String> tabComplete(CommandIssuer issuer, String commandLabel, String[] args) {
      return this.tabComplete(issuer, commandLabel, args, false);
   }

   public List<String> tabComplete(CommandIssuer issuer, String commandLabel, String[] args, boolean isAsync) throws IllegalArgumentException {
      return this.tabComplete(issuer, this.manager.getRootCommand(commandLabel.toLowerCase(Locale.ENGLISH)), args, isAsync);
   }

   List<String> tabComplete(CommandIssuer issuer, RootCommand rootCommand, String[] args, boolean isAsync) throws IllegalArgumentException {
      if (args.length == 0) {
         args = new String[]{""};
      }

      String commandLabel = rootCommand.getCommandName();

      List var14;
      try {
         CommandRouter router = this.manager.getRouter();
         this.preCommandOperation(issuer, commandLabel, args, isAsync);
         CommandRouter.RouteSearch search = router.routeCommand(rootCommand, commandLabel, args, true);
         List<String> cmds = new ArrayList<>();
         if (search != null) {
            for (RegisteredCommand<?> command : search.commands) {
               if (command.scope == this) {
                  cmds.addAll(this.completeCommand(issuer, command, search.args, commandLabel, isAsync));
               }
            }
         }

         var14 = filterTabComplete(args[args.length - 1], cmds);
      } finally {
         this.postCommandOperation();
      }

      return var14;
   }

   List<String> getCommandsForCompletion(CommandIssuer issuer, String[] args) {
      Set<String> cmds = new HashSet<>();
      int cmdIndex = Math.max(0, args.length - 1);
      String argString = ApacheCommonsLangUtil.join(args, " ").toLowerCase(Locale.ENGLISH);

      for (Entry<String, RegisteredCommand> entry : this.subCommands.entries()) {
         String key = entry.getKey();
         if (key.startsWith(argString) && !isSpecialSubcommand(key)) {
            RegisteredCommand value = entry.getValue();
            if (value.hasPermission(issuer) && !value.isPrivate) {
               String[] split = ACFPatterns.SPACE.split(value.prefSubCommand);
               cmds.add(split[cmdIndex]);
            }
         }
      }

      return new ArrayList<>(cmds);
   }

   static boolean isSpecialSubcommand(String key) {
      return "__catchunknown".equals(key) || "__default".equals(key);
   }

   private List<String> completeCommand(CommandIssuer issuer, RegisteredCommand cmd, String[] args, String commandLabel, boolean isAsync) {
      if (cmd.hasPermission(issuer) && args.length != 0 && cmd.parameters.length != 0) {
         if (!cmd.parameters[cmd.parameters.length - 1].consumesRest && args.length > cmd.consumeInputResolvers) {
            return Collections.emptyList();
         } else {
            List<String> cmds = this.manager.getCommandCompletions().of(cmd, issuer, args, isAsync);
            return filterTabComplete(args[args.length - 1], cmds);
         }
      } else {
         return Collections.emptyList();
      }
   }

   private static List<String> filterTabComplete(String arg, List<String> cmds) {
      return cmds.stream()
         .distinct()
         .filter(cmd -> cmd != null && (arg.isEmpty() || ApacheCommonsLangUtil.startsWithIgnoreCase(cmd, arg)))
         .collect(Collectors.toList());
   }

   private boolean checkPrecommand(CommandOperationContext commandOperationContext, RegisteredCommand cmd, CommandIssuer issuer, String[] args) {
      Method pre = this.preCommandHandler;
      if (pre != null) {
         try {
            Class<?>[] types = pre.getParameterTypes();
            Object[] parameters = new Object[pre.getParameterCount()];

            for (int i = 0; i < parameters.length; i++) {
               Class<?> type = types[i];
               Object issuerObject = issuer.getIssuer();
               if (this.manager.isCommandIssuer(type) && type.isAssignableFrom(issuerObject.getClass())) {
                  parameters[i] = issuerObject;
               } else if (CommandIssuer.class.isAssignableFrom(type)) {
                  parameters[i] = issuer;
               } else if (RegisteredCommand.class.isAssignableFrom(type)) {
                  parameters[i] = cmd;
               } else if (String[].class.isAssignableFrom(type)) {
                  parameters[i] = args;
               } else {
                  parameters[i] = null;
               }
            }

            return (Boolean)pre.invoke(this, parameters);
         } catch (InvocationTargetException | IllegalAccessException var11) {
            this.manager.log(LogLevel.ERROR, "Exception encountered while command pre-processing", var11);
         }
      }

      return false;
   }

   @Deprecated
   @UnstableAPI
   public CommandHelp getCommandHelp() {
      return this.manager.generateCommandHelp();
   }

   @Deprecated
   @UnstableAPI
   public void showCommandHelp() {
      this.getCommandHelp().showHelp();
   }

   public void help(Object issuer, String[] args) {
      this.help(this.manager.getCommandIssuer(issuer), args);
   }

   public void help(CommandIssuer issuer, String[] args) {
      issuer.sendMessage(MessageType.ERROR, MessageKeys.UNKNOWN_COMMAND);
   }

   public void doHelp(Object issuer, String... args) {
      this.doHelp(this.manager.getCommandIssuer(issuer), args);
   }

   public void doHelp(CommandIssuer issuer, String... args) {
      this.help(issuer, args);
   }

   public void showSyntax(CommandIssuer issuer, RegisteredCommand<?> cmd) {
      issuer.sendMessage(
         MessageType.SYNTAX,
         MessageKeys.INVALID_SYNTAX,
         "{command}",
         this.manager.getCommandPrefix(issuer) + cmd.command,
         "{syntax}",
         cmd.getSyntaxText(issuer)
      );
   }

   public boolean hasPermission(Object issuer) {
      return this.hasPermission(this.manager.getCommandIssuer(issuer));
   }

   public boolean hasPermission(CommandIssuer issuer) {
      return this.manager.hasPermission(issuer, this.getRequiredPermissions());
   }

   public Set<String> getRequiredPermissions() {
      return this.permissions;
   }

   public boolean requiresPermission(String permission) {
      return this.getRequiredPermissions().contains(permission);
   }

   public String getName() {
      return this.commandName;
   }

   public ExceptionHandler getExceptionHandler() {
      return this.exceptionHandler;
   }

   public BaseCommand setExceptionHandler(ExceptionHandler exceptionHandler) {
      this.exceptionHandler = exceptionHandler;
      return this;
   }

   public RegisteredCommand getDefaultRegisteredCommand() {
      return ACFUtil.getFirstElement(this.subCommands.get("__default"));
   }

   public String setContextFlags(Class<?> cls, String flags) {
      return this.contextFlags.put(cls, flags);
   }

   public String getContextFlags(Class<?> cls) {
      return this.contextFlags.get(cls);
   }

   public List<RegisteredCommand> getRegisteredCommands() {
      List<RegisteredCommand> registeredCommands = new ArrayList<>();
      registeredCommands.addAll(this.subCommands.values());
      return registeredCommands;
   }

   protected SetMultimap<String, RegisteredCommand> getSubCommands() {
      return this.subCommands;
   }
}
