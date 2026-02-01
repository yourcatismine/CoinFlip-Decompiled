package co.aikar.commands;

import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.ConsumesRest;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Name;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.annotation.Values;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.commands.contexts.IssuerAwareContextResolver;
import co.aikar.commands.contexts.IssuerOnlyContextResolver;
import co.aikar.commands.contexts.OptionalContextResolver;
import co.aikar.locales.MessageKey;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandParameter<CEC extends CommandExecutionContext<CEC, ? extends CommandIssuer>> {
   private final Parameter parameter;
   private final Class<?> type;
   private final String name;
   private final CommandManager manager;
   private final int paramIndex;
   private ContextResolver<?, CEC> resolver;
   private boolean optional;
   private Set<String> permissions = new HashSet<>();
   private String permission;
   private String description;
   private String defaultValue;
   private String syntax;
   private String conditions;
   private boolean requiresInput;
   private boolean commandIssuer;
   private String[] values;
   private Map<String, String> flags;
   private boolean canConsumeInput;
   private boolean optionalResolver;
   boolean consumesRest;
   private boolean isLast;
   private boolean isOptionalInput;
   private CommandParameter<CEC> nextParam;

   public CommandParameter(RegisteredCommand<CEC> command, Parameter param, int paramIndex, boolean isLast) {
      this.parameter = param;
      this.isLast = isLast;
      this.type = param.getType();
      this.manager = command.manager;
      this.paramIndex = paramIndex;
      Annotations annotations = this.manager.getAnnotations();
      String annotationName = annotations.getAnnotationValue(param, Name.class, 1);
      this.name = annotationName != null ? annotationName : param.getName();
      this.defaultValue = annotations.getAnnotationValue(param, Default.class, 1 | (this.type != String.class ? 8 : 0));
      this.description = annotations.getAnnotationValue(param, Description.class, 17);
      this.conditions = annotations.getAnnotationValue(param, Conditions.class, 9);
      this.resolver = (ContextResolver<?, CEC>)this.manager.getCommandContexts().getResolver(this.type);
      if (this.resolver == null) {
         ACFUtil.sneaky(new InvalidCommandContextException("Parameter " + this.type.getSimpleName() + " of " + command + " has no applicable context resolver"));
      }

      this.optional = annotations.hasAnnotation(param, Optional.class) || this.defaultValue != null || isLast && this.type == String[].class;
      this.permission = annotations.getAnnotationValue(param, CommandPermission.class, 9);
      this.optionalResolver = this.isOptionalResolver(this.resolver);
      this.requiresInput = !this.optional && !this.optionalResolver;
      this.commandIssuer = paramIndex == 0 && this.manager.isCommandIssuer(this.type);
      this.canConsumeInput = !this.commandIssuer && !(this.resolver instanceof IssuerOnlyContextResolver);
      this.consumesRest = isLast
         && (
            annotations.hasAnnotation(param, ConsumesRest.class)
               || this.type == String.class && !annotations.hasAnnotation(param, Single.class)
               || this.type == String[].class
         );
      this.values = annotations.getAnnotationValues(param, Values.class, 9);
      this.syntax = null;
      this.isOptionalInput = !this.requiresInput && this.canConsumeInput;
      if (!this.commandIssuer) {
         this.syntax = annotations.getAnnotationValue(param, Syntax.class);
      }

      this.flags = new HashMap<>();
      String flags = annotations.getAnnotationValue(param, Flags.class, 9);
      if (flags != null) {
         this.parseFlags(flags);
      }

      this.inheritContextFlags(command.scope);
      this.computePermissions();
   }

   private void inheritContextFlags(BaseCommand scope) {
      if (!scope.contextFlags.isEmpty()) {
         Class<?> pCls = this.type;

         do {
            this.parseFlags(scope.contextFlags.get(pCls));
         } while ((pCls = pCls.getSuperclass()) != null);
      }

      if (scope.parentCommand != null) {
         this.inheritContextFlags(scope.parentCommand);
      }
   }

   private void parseFlags(String flags) {
      if (flags != null) {
         for (String s : ACFPatterns.COMMA.split(this.manager.getCommandReplacements().replace(flags))) {
            String[] v = ACFPatterns.EQUALS.split(s, 2);
            if (!this.flags.containsKey(v[0])) {
               this.flags.put(v[0], v.length > 1 ? v[1] : null);
            }
         }
      }
   }

   private void computePermissions() {
      this.permissions.clear();
      if (this.permission != null && !this.permission.isEmpty()) {
         this.permissions.addAll(Arrays.asList(ACFPatterns.COMMA.split(this.permission)));
      }
   }

   private boolean isOptionalResolver(ContextResolver<?, CEC> resolver) {
      return resolver instanceof IssuerAwareContextResolver || resolver instanceof IssuerOnlyContextResolver || resolver instanceof OptionalContextResolver;
   }

   public Parameter getParameter() {
      return this.parameter;
   }

   public Class<?> getType() {
      return this.type;
   }

   public String getName() {
      return this.name;
   }

   public String getDisplayName(CommandIssuer issuer) {
      String translated = this.manager.getLocales().getOptionalMessage(issuer, MessageKey.of("acf-core.parameter." + this.name.toLowerCase()));
      return translated != null ? translated : this.name;
   }

   public CommandManager getManager() {
      return this.manager;
   }

   public int getParamIndex() {
      return this.paramIndex;
   }

   public ContextResolver<?, CEC> getResolver() {
      return this.resolver;
   }

   public void setResolver(ContextResolver<?, CEC> resolver) {
      this.resolver = resolver;
   }

   public boolean isOptionalInput() {
      return this.isOptionalInput;
   }

   public boolean isOptional() {
      return this.optional;
   }

   public void setOptional(boolean optional) {
      this.optional = optional;
   }

   public String getDescription() {
      return this.description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public String getDefaultValue() {
      return this.defaultValue;
   }

   public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
   }

   public boolean isCommandIssuer() {
      return this.commandIssuer;
   }

   public void setCommandIssuer(boolean commandIssuer) {
      this.commandIssuer = commandIssuer;
   }

   public String[] getValues() {
      return this.values;
   }

   public void setValues(String[] values) {
      this.values = values;
   }

   public Map<String, String> getFlags() {
      return this.flags;
   }

   public void setFlags(Map<String, String> flags) {
      this.flags = flags;
   }

   public boolean canConsumeInput() {
      return this.canConsumeInput;
   }

   public void setCanConsumeInput(boolean canConsumeInput) {
      this.canConsumeInput = canConsumeInput;
   }

   public void setOptionalResolver(boolean optionalResolver) {
      this.optionalResolver = optionalResolver;
   }

   public boolean isOptionalResolver() {
      return this.optionalResolver;
   }

   public boolean requiresInput() {
      return this.requiresInput;
   }

   public void setRequiresInput(boolean requiresInput) {
      this.requiresInput = requiresInput;
   }

   public String getSyntax() {
      return this.getSyntax(null);
   }

   public String getSyntax(CommandIssuer issuer) {
      if (this.commandIssuer) {
         return null;
      } else {
         if (this.syntax == null) {
            if (this.isOptionalInput) {
               return "[" + this.getDisplayName(issuer) + "]";
            }

            if (this.requiresInput) {
               return "<" + this.getDisplayName(issuer) + ">";
            }
         }

         return this.syntax;
      }
   }

   public void setSyntax(String syntax) {
      this.syntax = syntax;
   }

   public String getConditions() {
      return this.conditions;
   }

   public void setConditions(String conditions) {
      this.conditions = conditions;
   }

   public Set<String> getRequiredPermissions() {
      return this.permissions;
   }

   public void setNextParam(CommandParameter<CEC> nextParam) {
      this.nextParam = nextParam;
   }

   public CommandParameter<CEC> getNextParam() {
      return this.nextParam;
   }

   public boolean canExecuteWithoutInput() {
      return (!this.canConsumeInput || this.isOptionalInput()) && (this.nextParam == null || this.nextParam.canExecuteWithoutInput());
   }

   public boolean isLast() {
      return this.isLast;
   }
}
