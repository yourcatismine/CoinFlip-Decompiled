package co.aikar.commands;

import co.aikar.commands.annotation.Single;
import co.aikar.commands.annotation.Split;
import co.aikar.commands.annotation.Values;
import co.aikar.commands.contexts.ContextResolver;
import co.aikar.commands.contexts.IssuerAwareContextResolver;
import co.aikar.commands.contexts.IssuerOnlyContextResolver;
import co.aikar.commands.contexts.OptionalContextResolver;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class CommandContexts<R extends CommandExecutionContext<?, ? extends CommandIssuer>> {
   protected final Map<Class<?>, ContextResolver<?, R>> contextMap = new HashMap<>();
   protected final CommandManager manager;

   CommandContexts(CommandManager manager) {
      this.manager = manager;
      this.registerIssuerOnlyContext(CommandIssuer.class, c -> c.getIssuer());
      this.registerContext(Short.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -32768, (short)32767).shortValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(short.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -32768, (short)32767).shortValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(Integer.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, Integer.MIN_VALUE, Integer.MAX_VALUE).intValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(int.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, Integer.MIN_VALUE, Integer.MAX_VALUE).intValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(Long.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, Long.MIN_VALUE, Long.MAX_VALUE).longValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(long.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, Long.MIN_VALUE, Long.MAX_VALUE).longValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(Float.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -Float.MAX_VALUE, Float.MAX_VALUE).floatValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(float.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -Float.MAX_VALUE, Float.MAX_VALUE).floatValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(Double.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -Double.MAX_VALUE, Double.MAX_VALUE).doubleValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(double.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -Double.MAX_VALUE, Double.MAX_VALUE).doubleValue();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(Number.class, c -> {
         String number = c.popFirstArg();

         try {
            return this.parseAndValidateNumber(number, c, -Double.MAX_VALUE, Double.MAX_VALUE);
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", number);
         }
      });
      this.registerContext(BigDecimal.class, c -> {
         String numberStr = c.popFirstArg();

         try {
            BigDecimal number = ACFUtil.parseBigNumber(numberStr, c.hasFlag("suffixes"));
            this.validateMinMax(c, number);
            return number;
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", numberStr);
         }
      });
      this.registerContext(BigInteger.class, c -> {
         String numberStr = c.popFirstArg();

         try {
            BigDecimal number = ACFUtil.parseBigNumber(numberStr, c.hasFlag("suffixes"));
            this.validateMinMax(c, number);
            return number.toBigIntegerExact();
         } catch (NumberFormatException var4) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", numberStr);
         }
      });
      this.registerContext(Boolean.class, c -> ACFUtil.isTruthy(c.popFirstArg()));
      this.registerContext(boolean.class, c -> ACFUtil.isTruthy(c.popFirstArg()));
      this.registerContext(char.class, c -> {
         String s = c.popFirstArg();
         if (s.length() > 1) {
            throw new InvalidCommandArgument(MessageKeys.MUST_BE_MAX_LENGTH, "{max}", String.valueOf(1));
         } else {
            return s.charAt(0);
         }
      });
      this.registerContext(String.class, c -> {
         if (c.hasAnnotation(Values.class)) {
            return c.popFirstArg();
         } else {
            String ret = c.isLastArg() && !c.hasAnnotation(Single.class) ? ACFUtil.join(c.getArgs()) : c.popFirstArg();
            Integer minLen = c.getFlagValue("minlen", (Integer)null);
            Integer maxLen = c.getFlagValue("maxlen", (Integer)null);
            if (minLen != null && ret.length() < minLen) {
               throw new InvalidCommandArgument(MessageKeys.MUST_BE_MIN_LENGTH, "{min}", String.valueOf(minLen));
            } else if (maxLen != null && ret.length() > maxLen) {
               throw new InvalidCommandArgument(MessageKeys.MUST_BE_MAX_LENGTH, "{max}", String.valueOf(maxLen));
            } else {
               return ret;
            }
         }
      });
      this.registerContext(String[].class, c -> {
         List<String> args = c.getArgs();
         String val;
         if (c.isLastArg() && !c.hasAnnotation(Single.class)) {
            val = ACFUtil.join(args);
         } else {
            val = c.popFirstArg();
         }

         String split = c.getAnnotationValue(Split.class, 8);
         if (split != null) {
            if (val.isEmpty()) {
               throw new InvalidCommandArgument();
            } else {
               return ACFPatterns.getPattern(split).split(val);
            }
         } else {
            if (!c.isLastArg()) {
               ACFUtil.sneaky(new IllegalStateException("Weird Command signature... String[] should be last or @Split"));
            }

            String[] result = args.toArray(new String[0]);
            args.clear();
            return result;
         }
      });
      this.registerContext(Enum.class, c -> {
         String first = c.popFirstArg();
         Class<? extends Enum<?>> enumCls = (Class<? extends Enum<?>>)c.getParam().getType();
         Enum<?> match = ACFUtil.simpleMatch(enumCls, first);
         if (match == null) {
            List<String> names = ACFUtil.enumNames(enumCls);
            throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_ONE_OF, "{valid}", ACFUtil.join(names, ", "));
         } else {
            return match;
         }
      });
      this.registerOptionalContext(CommandHelp.class, c -> {
         String first = c.getFirstArg();
         String last = c.getLastArg();
         Integer page = 1;
         List<String> search = null;
         if (last != null && ACFUtil.isInteger(last)) {
            c.popLastArg();
            page = ACFUtil.parseInt(last);
            if (page == null) {
               throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", last);
            }

            if (!c.getArgs().isEmpty()) {
               search = c.getArgs();
            }
         } else if (first != null && ACFUtil.isInteger(first)) {
            c.popFirstArg();
            page = ACFUtil.parseInt(first);
            if (page == null) {
               throw new InvalidCommandArgument(MessageKeys.MUST_BE_A_NUMBER, "{num}", first);
            }

            if (!c.getArgs().isEmpty()) {
               search = c.getArgs();
            }
         } else if (!c.getArgs().isEmpty()) {
            search = c.getArgs();
         }

         CommandHelp commandHelp = manager.generateCommandHelp();
         commandHelp.setPage(page);
         Integer perPage = c.getFlagValue("perpage", (Integer)null);
         if (perPage != null) {
            commandHelp.setPerPage(perPage);
         }

         if (search != null) {
            String cmd = String.join(" ", search);
            if (commandHelp.testExactMatch(cmd)) {
               return commandHelp;
            }
         }

         commandHelp.setSearch(search);
         return commandHelp;
      });
   }

   @NotNull
   private Number parseAndValidateNumber(String number, R c, Number minValue, Number maxValue) throws InvalidCommandArgument {
      Number val = ACFUtil.parseNumber(number, c.hasFlag("suffixes"));
      this.validateMinMax(c, val, minValue, maxValue);
      return val;
   }

   private void validateMinMax(R c, Number val) throws InvalidCommandArgument {
      this.validateMinMax(c, val, null, null);
   }

   private void validateMinMax(R c, Number val, Number minValue, Number maxValue) throws InvalidCommandArgument {
      Number var5 = c.getFlagValue("min", minValue);
      Number var6 = c.getFlagValue("max", maxValue);
      if (var6 != null && val.doubleValue() > var6.doubleValue()) {
         throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_AT_MOST, "{max}", String.valueOf(var6));
      } else if (var5 != null && val.doubleValue() < var5.doubleValue()) {
         throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_AT_LEAST, "{min}", String.valueOf(var5));
      }
   }

   @Deprecated
   public <T> void registerSenderAwareContext(Class<T> context, IssuerAwareContextResolver<T, R> supplier) {
      this.contextMap.put(context, supplier);
   }

   public <T> void registerIssuerAwareContext(Class<T> context, IssuerAwareContextResolver<T, R> supplier) {
      this.contextMap.put(context, supplier);
   }

   public <T> void registerIssuerOnlyContext(Class<T> context, IssuerOnlyContextResolver<T, R> supplier) {
      this.contextMap.put(context, supplier);
   }

   public <T> void registerOptionalContext(Class<T> context, OptionalContextResolver<T, R> supplier) {
      this.contextMap.put(context, supplier);
   }

   public <T> void registerContext(Class<T> context, ContextResolver<T, R> supplier) {
      this.contextMap.put(context, supplier);
   }

   public ContextResolver<?, R> getResolver(Class<?> type) {
      Class<?> rootType = type;

      while (type != Object.class) {
         ContextResolver<?, R> resolver = this.contextMap.get(type);
         if (resolver != null) {
            return resolver;
         }

         if ((type = type.getSuperclass()) == null) {
            break;
         }
      }

      this.manager.log(LogLevel.ERROR, "Could not find context resolver", new IllegalStateException("No context resolver defined for " + rootType.getName()));
      return null;
   }
}
