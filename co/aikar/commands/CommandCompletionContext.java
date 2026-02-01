package co.aikar.commands;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandCompletionContext<I extends CommandIssuer> {
   private final RegisteredCommand command;
   protected final I issuer;
   private final String input;
   private final String config;
   private final Map<String, String> configs = new HashMap<>();
   private final List<String> args;

   CommandCompletionContext(RegisteredCommand command, I issuer, String input, String config, String[] args) {
      this.command = command;
      this.issuer = issuer;
      this.input = input;
      if (config != null) {
         String[] configs = ACFPatterns.COMMA.split(config);

         for (String conf : configs) {
            String[] confsplit = ACFPatterns.EQUALS.split(conf, 2);
            this.configs.put(confsplit[0].toLowerCase(Locale.ENGLISH), confsplit.length > 1 ? confsplit[1] : null);
         }

         this.config = configs[0];
      } else {
         this.config = null;
      }

      this.args = Arrays.asList(args);
   }

   public Map<String, String> getConfigs() {
      return this.configs;
   }

   public String getConfig(String key) {
      return this.getConfig(key, null);
   }

   public String getConfig(String key, String def) {
      return this.configs.getOrDefault(key.toLowerCase(Locale.ENGLISH), def);
   }

   public boolean hasConfig(String key) {
      return this.configs.containsKey(key.toLowerCase(Locale.ENGLISH));
   }

   public <T> T getContextValue(Class<? extends T> clazz) throws InvalidCommandArgument {
      return this.getContextValue(clazz, null);
   }

   public <T> T getContextValue(Class<? extends T> clazz, Integer paramIdx) throws InvalidCommandArgument {
      String name = null;
      if (paramIdx != null) {
         if (paramIdx >= this.command.parameters.length) {
            throw new IllegalArgumentException("Param index is higher than number of parameters");
         }

         CommandParameter param = this.command.parameters[paramIdx];
         Class<?> paramType = param.getType();
         if (!clazz.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(param.getName() + ":" + paramType.getName() + " can not satisfy " + clazz.getName());
         }

         name = param.getName();
      } else {
         CommandParameter[] parameters = this.command.parameters;

         for (int i = 0; i < parameters.length; i++) {
            CommandParameter parameter = parameters[i];
            if (clazz.isAssignableFrom(parameter.getType())) {
               paramIdx = i;
               name = parameter.getName();
               break;
            }
         }

         if (paramIdx == null) {
            throw new IllegalStateException("Can not find any parameter that can satisfy " + clazz.getName());
         }
      }

      return this.getContextValueByName(clazz, name);
   }

   public <T> T getContextValueByName(Class<? extends T> clazz, String name) throws InvalidCommandArgument {
      Map<String, Object> resolved = this.command.resolveContexts(this.issuer, this.args, name);
      if (resolved == null || !resolved.containsKey(name)) {
         ACFUtil.sneaky(new CommandCompletionTextLookupException());
      }

      return (T)resolved.get(name);
   }

   public CommandIssuer getIssuer() {
      return this.issuer;
   }

   public String getInput() {
      return this.input;
   }

   public String getConfig() {
      return this.config;
   }

   public boolean isAsync() {
      return CommandManager.getCurrentCommandOperationContext().isAsync();
   }
}
