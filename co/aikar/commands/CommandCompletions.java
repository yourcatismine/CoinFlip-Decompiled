package co.aikar.commands;

import co.aikar.commands.apachecommonslang.ApacheCommonsLangUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

public class CommandCompletions<C extends CommandCompletionContext> {
   private static final String DEFAULT_ENUM_ID = "@__defaultenum__";
   private final CommandManager manager;
   private Map<String, CommandCompletions.CommandCompletionHandler> completionMap = new HashMap<>();
   private Map<Class, String> defaultCompletions = new HashMap<>();

   public CommandCompletions(CommandManager manager) {
      this.manager = manager;
      this.registerStaticCompletion("empty", Collections.emptyList());
      this.registerStaticCompletion("nothing", Collections.emptyList());
      this.registerStaticCompletion("timeunits", Arrays.asList("minutes", "hours", "days", "weeks", "months", "years"));
      this.registerAsyncCompletion("range", c -> {
         String config = c.getConfig();
         if (config == null) {
            return Collections.emptyList();
         } else {
            String[] ranges = ACFPatterns.DASH.split(config);
            int start;
            int end;
            if (ranges.length != 2) {
               start = 0;
               end = ACFUtil.parseInt(ranges[0], 0);
            } else {
               start = ACFUtil.parseInt(ranges[0], 0);
               end = ACFUtil.parseInt(ranges[1], 0);
            }

            return IntStream.rangeClosed(start, end).mapToObj(Integer::toString).collect(Collectors.toList());
         }
      });
   }

   public CommandCompletions.CommandCompletionHandler registerCompletion(String id, CommandCompletions.CommandCompletionHandler<C> handler) {
      return this.completionMap.put(prepareCompletionId(id), handler);
   }

   public CommandCompletions.CommandCompletionHandler unregisterCompletion(String id) {
      if (!this.completionMap.containsKey(id)) {
         throw new IllegalStateException("The supplied key " + id + " does not exist in any completions");
      } else {
         return this.completionMap.remove(id);
      }
   }

   public CommandCompletions.CommandCompletionHandler registerAsyncCompletion(String id, CommandCompletions.AsyncCommandCompletionHandler<C> handler) {
      return this.completionMap.put(prepareCompletionId(id), handler);
   }

   public CommandCompletions.CommandCompletionHandler registerStaticCompletion(String id, String list) {
      return this.registerStaticCompletion(id, ACFPatterns.PIPE.split(list));
   }

   public CommandCompletions.CommandCompletionHandler registerStaticCompletion(String id, String[] completions) {
      return this.registerStaticCompletion(id, Arrays.asList(completions));
   }

   public CommandCompletions.CommandCompletionHandler registerStaticCompletion(String id, Supplier<Collection<String>> supplier) {
      return this.registerStaticCompletion(id, supplier.get());
   }

   public CommandCompletions.CommandCompletionHandler registerStaticCompletion(String id, Collection<String> completions) {
      return this.registerAsyncCompletion(id, x -> completions);
   }

   public void setDefaultCompletion(String id, Class... classes) {
      id = prepareCompletionId(id);
      CommandCompletions.CommandCompletionHandler completion = this.completionMap.get(id);
      if (completion == null) {
         throw new IllegalStateException("Completion not registered for " + id);
      } else {
         for (Class clazz : classes) {
            this.defaultCompletions.put(clazz, id);
         }
      }
   }

   @NotNull
   private static String prepareCompletionId(String id) {
      return (id.startsWith("@") ? "" : "@") + id.toLowerCase(Locale.ENGLISH);
   }

   @NotNull
   List<String> of(RegisteredCommand cmd, CommandIssuer sender, String[] args, boolean isAsync) {
      String[] completions = ACFPatterns.SPACE.split(cmd.complete);
      int argIndex = args.length - 1;
      String completion = argIndex < completions.length ? completions[argIndex] : null;
      if (completion == null || completion.isEmpty() || "*".equals(completion)) {
         completion = this.findDefaultCompletion(cmd, args);
      }

      if (completion == null && completions.length > 0) {
         String last = completions[completions.length - 1];
         if (last.startsWith("repeat@")) {
            completion = last;
         } else if (argIndex >= completions.length && cmd.parameters[cmd.parameters.length - 1].consumesRest) {
            completion = last;
         }
      }

      return completion == null ? Collections.emptyList() : this.getCompletionValues(cmd, sender, completion, args, isAsync);
   }

   String findDefaultCompletion(RegisteredCommand cmd, String[] args) {
      int i = 0;

      for (CommandParameter param : cmd.parameters) {
         if (param.canConsumeInput()) {
            if (++i == args.length) {
               for (Class type = param.getType(); type != null; type = type.getSuperclass()) {
                  String completion = this.defaultCompletions.get(type);
                  if (completion != null) {
                     return completion;
                  }
               }

               if (param.getType().isEnum()) {
                  CommandOperationContext ctx = CommandManager.getCurrentCommandOperationContext();
                  ctx.enumCompletionValues = ACFUtil.enumNames((Class<? extends Enum<?>>)param.getType());
                  return "@__defaultenum__";
               }
               break;
            }
         }
      }

      return null;
   }

   List<String> getCompletionValues(RegisteredCommand command, CommandIssuer sender, String completion, String[] args, boolean isAsync) {
      if ("@__defaultenum__".equals(completion)) {
         CommandOperationContext<?> ctx = CommandManager.getCurrentCommandOperationContext();
         return ctx.enumCompletionValues;
      } else {
         boolean repeat = completion.startsWith("repeat@");
         if (repeat) {
            completion = completion.substring(6);
         }

         completion = this.manager.getCommandReplacements().replace(completion);
         List<String> allCompletions = new ArrayList<>();
         String input = args.length > 0 ? args[args.length - 1] : "";
         String[] var9 = ACFPatterns.PIPE.split(completion);
         int var10 = var9.length;
         int var11 = 0;

         while (true) {
            if (var11 >= var10) {
               return allCompletions;
            }

            String value = var9[var11];
            String[] complete = ACFPatterns.COLONEQUALS.split(value, 2);
            CommandCompletions.CommandCompletionHandler handler = this.completionMap.get(complete[0].toLowerCase(Locale.ENGLISH));
            if (handler != null) {
               if (isAsync && !(handler instanceof CommandCompletions.AsyncCommandCompletionHandler)) {
                  ACFUtil.sneaky(new CommandCompletions.SyncCompletionRequired());
                  return null;
               }

               String config = complete.length == 1 ? null : complete[1];
               CommandCompletionContext context = this.manager.createCompletionContext(command, sender, input, config, args);

               try {
                  Collection<String> completions = handler.getCompletions(context);
                  if (!repeat
                     && completions != null
                     && command.parameters[command.parameters.length - 1].consumesRest
                     && args.length > ACFPatterns.SPACE.split(command.complete).length) {
                     String start = String.join(" ", args);
                     completions = completions.stream().map(s -> {
                        if (s != null && s.split(" ").length >= args.length && ApacheCommonsLangUtil.startsWithIgnoreCase(s, start)) {
                           String[] completionArgs = s.split(" ");
                           return String.join(" ", Arrays.copyOfRange(completionArgs, args.length - 1, completionArgs.length));
                        } else {
                           return (String)s;
                        }
                     }).collect(Collectors.toList());
                  }

                  if (completions == null) {
                     break;
                  }

                  allCompletions.addAll(completions);
               } catch (CommandCompletionTextLookupException var19) {
                  break;
               } catch (Exception var20) {
                  command.handleException(sender, Arrays.asList(args), var20);
                  break;
               }
            } else {
               allCompletions.add(value);
            }

            var11++;
         }

         return Collections.emptyList();
      }
   }

   public interface AsyncCommandCompletionHandler<C extends CommandCompletionContext> extends CommandCompletions.CommandCompletionHandler<C> {
   }

   public interface CommandCompletionHandler<C extends CommandCompletionContext> {
      Collection<String> getCompletions(C context) throws InvalidCommandArgument;
   }

   public static class SyncCompletionRequired extends RuntimeException {
   }
}
