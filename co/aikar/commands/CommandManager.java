package co.aikar.commands;

import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.lib.util.Table;
import co.aikar.locales.MessageKeyProvider;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;

public abstract class CommandManager<IT, I extends CommandIssuer, FT, MF extends MessageFormatter<FT>, CEC extends CommandExecutionContext<CEC, I>, CC extends ConditionContext<I>> {
   static ThreadLocal<Stack<CommandOperationContext>> commandOperationContext = ThreadLocal.withInitial(() -> new Stack<CommandOperationContext>() {
      public synchronized CommandOperationContext peek() {
         return super.size() == 0 ? null : (CommandOperationContext)super.peek();
      }
   });
   protected Map<String, RootCommand> rootCommands = new HashMap<>();
   protected final CommandReplacements replacements = new CommandReplacements(this);
   protected final CommandConditions<I, CEC, CC> conditions = new CommandConditions<>(this);
   protected ExceptionHandler defaultExceptionHandler = null;
   boolean logUnhandledExceptions = true;
   protected Table<Class<?>, String, Object> dependencies = new Table<>();
   protected CommandHelpFormatter helpFormatter = new CommandHelpFormatter(this);
   protected boolean usePerIssuerLocale = false;
   protected List<IssuerLocaleChangedCallback<I>> localeChangedCallbacks = new ArrayList<>();
   protected Set<Locale> supportedLanguages = new HashSet<>(
      Arrays.asList(
         Locales.ENGLISH,
         Locales.DUTCH,
         Locales.GERMAN,
         Locales.SPANISH,
         Locales.FRENCH,
         Locales.CZECH,
         Locales.PORTUGUESE,
         Locales.SWEDISH,
         Locales.NORWEGIAN_BOKMAAL,
         Locales.NORWEGIAN_NYNORSK,
         Locales.RUSSIAN,
         Locales.BULGARIAN,
         Locales.HUNGARIAN,
         Locales.TURKISH,
         Locales.JAPANESE,
         Locales.CHINESE,
         Locales.SIMPLIFIED_CHINESE,
         Locales.TRADITIONAL_CHINESE,
         Locales.KOREAN,
         Locales.ITALIAN
      )
   );
   protected Predicate<String> validNamePredicate = name -> true;
   protected Map<MessageType, MF> formatters = new IdentityHashMap<>();
   protected MF defaultFormatter;
   protected int defaultHelpPerPage = 10;
   protected Map<UUID, Locale> issuersLocale = new ConcurrentHashMap<>();
   private Set<String> unstableAPIs = new HashSet<>();
   private Annotations annotations = new Annotations<>(this);
   private CommandRouter router = new CommandRouter(this);

   public static CommandOperationContext getCurrentCommandOperationContext() {
      return commandOperationContext.get().peek();
   }

   public static CommandIssuer getCurrentCommandIssuer() {
      CommandOperationContext context = commandOperationContext.get().peek();
      return context != null ? context.getCommandIssuer() : null;
   }

   public static CommandManager getCurrentCommandManager() {
      CommandOperationContext context = commandOperationContext.get().peek();
      return context != null ? context.getCommandManager() : null;
   }

   public MF setFormat(MessageType type, MF formatter) {
      return this.formatters.put(type, formatter);
   }

   public MF getFormat(MessageType type) {
      return this.formatters.getOrDefault(type, this.defaultFormatter);
   }

   public void setFormat(MessageType type, FT... colors) {
      MF format = this.getFormat(type);

      for (int i = 1; i <= colors.length; i++) {
         format.setColor(i, colors[i - 1]);
      }
   }

   public void setFormat(MessageType type, int i, FT color) {
      MF format = this.getFormat(type);
      format.setColor(i, color);
   }

   public MF getDefaultFormatter() {
      return this.defaultFormatter;
   }

   public void setDefaultFormatter(MF defaultFormatter) {
      this.defaultFormatter = defaultFormatter;
   }

   public CommandConditions<I, CEC, CC> getCommandConditions() {
      return this.conditions;
   }

   public abstract CommandContexts<?> getCommandContexts();

   public abstract CommandCompletions<?> getCommandCompletions();

   @Deprecated
   @UnstableAPI
   public CommandHelp generateCommandHelp(@NotNull String command) {
      this.verifyUnstableAPI("help");
      CommandOperationContext context = getCurrentCommandOperationContext();
      if (context == null) {
         throw new IllegalStateException("This method can only be called as part of a command execution.");
      } else {
         return this.generateCommandHelp(context.getCommandIssuer(), command);
      }
   }

   @Deprecated
   @UnstableAPI
   public CommandHelp generateCommandHelp(CommandIssuer issuer, @NotNull String command) {
      this.verifyUnstableAPI("help");
      return this.generateCommandHelp(issuer, this.obtainRootCommand(command));
   }

   @Deprecated
   @UnstableAPI
   public CommandHelp generateCommandHelp() {
      this.verifyUnstableAPI("help");
      CommandOperationContext context = getCurrentCommandOperationContext();
      if (context == null) {
         throw new IllegalStateException("This method can only be called as part of a command execution.");
      } else {
         String commandLabel = context.getCommandLabel();
         return this.generateCommandHelp(context.getCommandIssuer(), this.obtainRootCommand(commandLabel));
      }
   }

   @Deprecated
   @UnstableAPI
   public CommandHelp generateCommandHelp(CommandIssuer issuer, RootCommand rootCommand) {
      this.verifyUnstableAPI("help");
      return new CommandHelp(this, rootCommand, issuer);
   }

   @Deprecated
   @UnstableAPI
   public int getDefaultHelpPerPage() {
      this.verifyUnstableAPI("help");
      return this.defaultHelpPerPage;
   }

   @Deprecated
   @UnstableAPI
   public void setDefaultHelpPerPage(int defaultHelpPerPage) {
      this.verifyUnstableAPI("help");
      this.defaultHelpPerPage = defaultHelpPerPage;
   }

   @Deprecated
   @UnstableAPI
   public void setHelpFormatter(CommandHelpFormatter helpFormatter) {
      this.helpFormatter = helpFormatter;
   }

   @Deprecated
   @UnstableAPI
   public CommandHelpFormatter getHelpFormatter() {
      return this.helpFormatter;
   }

   CommandRouter getRouter() {
      return this.router;
   }

   public abstract void registerCommand(BaseCommand command);

   public abstract boolean hasRegisteredCommands();

   public abstract boolean isCommandIssuer(Class<?> type);

   public abstract I getCommandIssuer(Object issuer);

   public abstract RootCommand createRootCommand(String cmd);

   public abstract Locales getLocales();

   public boolean usingPerIssuerLocale() {
      return this.usePerIssuerLocale;
   }

   public boolean usePerIssuerLocale(boolean setting) {
      boolean old = this.usePerIssuerLocale;
      this.usePerIssuerLocale = setting;
      return old;
   }

   public boolean isValidName(@NotNull String name) {
      return this.validNamePredicate.test(name);
   }

   @NotNull
   public Predicate<String> getValidNamePredicate() {
      return this.validNamePredicate;
   }

   public void setValidNamePredicate(@NotNull Predicate<String> isValidName) {
      this.validNamePredicate = isValidName;
   }

   public ConditionContext createConditionContext(CommandIssuer issuer, String config) {
      return new ConditionContext<>(issuer, config);
   }

   public abstract CommandExecutionContext createCommandContext(
      RegisteredCommand command, CommandParameter parameter, CommandIssuer sender, List<String> args, int i, Map<String, Object> passedArgs
   );

   public abstract CommandCompletionContext createCompletionContext(RegisteredCommand command, CommandIssuer sender, String input, String config, String[] args);

   public abstract void log(final LogLevel level, final String message, final Throwable throwable);

   public void log(final LogLevel level, final String message) {
      this.log(level, message, null);
   }

   public CommandReplacements getCommandReplacements() {
      return this.replacements;
   }

   public boolean hasPermission(CommandIssuer issuer, Set<String> permissions) {
      for (String permission : permissions) {
         if (!this.hasPermission(issuer, permission)) {
            return false;
         }
      }

      return true;
   }

   public boolean hasPermission(CommandIssuer issuer, String permission) {
      if (permission != null && !permission.isEmpty()) {
         for (String orPermission : ACFPatterns.PIPE.split(permission)) {
            if (!orPermission.isEmpty()) {
               boolean result = false;

               for (String perm : ACFPatterns.COMMA.split(orPermission)) {
                  if (!perm.isEmpty()) {
                     result = issuer.hasPermission(perm);
                     if (!result) {
                        break;
                     }
                  }
               }

               if (result) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public synchronized RootCommand getRootCommand(@NotNull String cmd) {
      return this.rootCommands.get(ACFPatterns.SPACE.split(cmd.toLowerCase(Locale.ENGLISH), 2)[0]);
   }

   public synchronized RootCommand obtainRootCommand(@NotNull String cmd) {
      return this.rootCommands.computeIfAbsent(ACFPatterns.SPACE.split(cmd.toLowerCase(Locale.ENGLISH), 2)[0], this::createRootCommand);
   }

   public abstract Collection<RootCommand> getRegisteredRootCommands();

   public RegisteredCommand createRegisteredCommand(BaseCommand command, String cmdName, Method method, String prefSubCommand) {
      return new RegisteredCommand(command, cmdName, method, prefSubCommand);
   }

   public void setDefaultExceptionHandler(ExceptionHandler exceptionHandler) {
      if (exceptionHandler == null && !this.logUnhandledExceptions) {
         throw new IllegalArgumentException("You may not disable the default exception handler and have logging of unhandled exceptions disabled");
      } else {
         this.defaultExceptionHandler = exceptionHandler;
      }
   }

   public void setDefaultExceptionHandler(ExceptionHandler exceptionHandler, boolean logExceptions) {
      if (exceptionHandler == null && !logExceptions) {
         throw new IllegalArgumentException("You may not disable the default exception handler and have logging of unhandled exceptions disabled");
      } else {
         this.logUnhandledExceptions = logExceptions;
         this.defaultExceptionHandler = exceptionHandler;
      }
   }

   public boolean isLoggingUnhandledExceptions() {
      return this.logUnhandledExceptions;
   }

   public ExceptionHandler getDefaultExceptionHandler() {
      return this.defaultExceptionHandler;
   }

   protected boolean handleUncaughtException(BaseCommand scope, RegisteredCommand registeredCommand, CommandIssuer sender, List<String> args, Throwable t) {
      if (t instanceof InvocationTargetException && t.getCause() != null) {
         t = t.getCause();
      }

      boolean result = false;
      if (scope.getExceptionHandler() != null) {
         result = scope.getExceptionHandler().execute(scope, registeredCommand, sender, args, t);
      } else if (this.defaultExceptionHandler != null) {
         result = this.defaultExceptionHandler.execute(scope, registeredCommand, sender, args, t);
      }

      return result;
   }

   public void sendMessage(IT issuerArg, MessageType type, MessageKeyProvider key, String... replacements) {
      this.sendMessage(this.getCommandIssuer(issuerArg), type, key, replacements);
   }

   public void sendMessage(CommandIssuer issuer, MessageType type, MessageKeyProvider key, String... replacements) {
      String message = this.formatMessage(issuer, type, key, replacements);

      for (String msg : ACFPatterns.NEWLINE.split(message)) {
         issuer.sendMessageInternal(ACFUtil.rtrim(msg));
      }
   }

   public String formatMessage(CommandIssuer issuer, MessageType type, MessageKeyProvider key, String... replacements) {
      String message = this.getLocales().getMessage(issuer, key.getMessageKey());
      if (replacements.length > 0) {
         message = ACFUtil.replaceStrings(message, replacements);
      }

      message = this.getCommandReplacements().replace(message);
      message = this.getLocales().replaceI18NStrings(message);
      MessageFormatter formatter = this.formatters.getOrDefault(type, this.defaultFormatter);
      if (formatter != null) {
         message = formatter.format(message);
      }

      return message;
   }

   public void onLocaleChange(IssuerLocaleChangedCallback<I> onChange) {
      this.localeChangedCallbacks.add(onChange);
   }

   public void notifyLocaleChange(I issuer, Locale oldLocale, Locale newLocale) {
      this.localeChangedCallbacks.forEach(cb -> {
         try {
            cb.onIssuerLocaleChange(issuer, oldLocale, newLocale);
         } catch (Exception var6) {
            this.log(LogLevel.ERROR, "Error in notifyLocaleChange", var6);
         }
      });
   }

   public Locale setIssuerLocale(IT issuer, Locale locale) {
      I commandIssuer = this.getCommandIssuer(issuer);
      Locale old = this.issuersLocale.put(commandIssuer.getUniqueId(), locale);
      if (!Objects.equals(old, locale)) {
         this.notifyLocaleChange(commandIssuer, old, locale);
      }

      return old;
   }

   public Locale getIssuerLocale(CommandIssuer issuer) {
      if (this.usingPerIssuerLocale() && issuer != null) {
         Locale locale = this.issuersLocale.get(issuer.getUniqueId());
         if (locale != null) {
            return locale;
         }
      }

      return this.getLocales().getDefaultLocale();
   }

   CommandOperationContext<I> createCommandOperationContext(BaseCommand command, CommandIssuer issuer, String commandLabel, String[] args, boolean isAsync) {
      return new CommandOperationContext<>(this, (I)issuer, command, commandLabel, args, isAsync);
   }

   public Set<Locale> getSupportedLanguages() {
      return this.supportedLanguages;
   }

   public void addSupportedLanguage(Locale locale) {
      this.supportedLanguages.add(locale);
      this.getLocales().loadMissingBundles();
   }

   public <T> void registerDependency(Class<? extends T> clazz, T instance) {
      this.registerDependency(clazz, clazz.getName(), instance);
   }

   public <T> void registerDependency(Class<? extends T> clazz, String key, T instance) {
      if (this.dependencies.containsKey(clazz, key)) {
         throw new IllegalStateException("There is already an instance of " + clazz.getName() + " with the key " + key + " registered!");
      } else {
         this.dependencies.put(clazz, key, instance);
      }
   }

   public <T> void unregisterDependency(Class<? extends T> clazz) {
      this.unregisterDependency(clazz, clazz.getName());
   }

   public <T> void unregisterDependency(Class<? extends T> clazz, String key) {
      if (!this.dependencies.containsKey(clazz, key)) {
         throw new IllegalStateException("Unable to unregister a dependency of " + clazz.getName() + " with the key " + key + " because it wasn't registered");
      } else {
         this.dependencies.remove(clazz, key);
      }
   }

   void injectDependencies(BaseCommand baseCommand) {
      Class clazz = baseCommand.getClass();

      do {
         for (Field field : clazz.getDeclaredFields()) {
            if (this.annotations.hasAnnotation(field, Dependency.class)) {
               String dependency = this.annotations.getAnnotationValue(field, Dependency.class);
               String key = dependency.isEmpty() ? field.getType().getName() : dependency;
               Object object = this.dependencies.row(field.getType()).get(key);
               if (object == null) {
                  throw new UnresolvedDependencyException(
                     "Could not find a registered instance of "
                        + field.getType().getName()
                        + " with key "
                        + key
                        + " for field "
                        + field.getName()
                        + " in class "
                        + baseCommand.getClass().getName()
                  );
               }

               try {
                  boolean accessible = field.isAccessible();
                  if (!accessible) {
                     field.setAccessible(true);
                  }

                  field.set(baseCommand, object);
                  field.setAccessible(accessible);
               } catch (IllegalAccessException var11) {
                  var11.printStackTrace();
               }
            }
         }

         clazz = clazz.getSuperclass();
      } while (!clazz.equals(BaseCommand.class));
   }

   @Deprecated
   public void enableUnstableAPI(String api) {
      this.unstableAPIs.add(api);
   }

   void verifyUnstableAPI(String api) {
      if (!this.unstableAPIs.contains(api)) {
         throw new IllegalStateException("Using an unstable API that has not been enabled ( " + api + "). See https://acfunstable.emc.gs");
      }
   }

   boolean hasUnstableAPI(String api) {
      return this.unstableAPIs.contains(api);
   }

   Annotations getAnnotations() {
      return this.annotations;
   }

   public String getCommandPrefix(CommandIssuer issuer) {
      return "";
   }
}
