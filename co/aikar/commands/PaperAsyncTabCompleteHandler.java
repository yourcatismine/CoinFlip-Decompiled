package co.aikar.commands;

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import java.util.Arrays;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

class PaperAsyncTabCompleteHandler implements Listener {
   private final PaperCommandManager manager;

   PaperAsyncTabCompleteHandler(PaperCommandManager manager) {
      this.manager = manager;
      manager.log(LogLevel.INFO, "Enabled Asynchronous Tab Completion Support!");
   }

   @EventHandler(ignoreCancelled = true)
   public void onAsyncTabComplete(AsyncTabCompleteEvent event) {
      String buffer = event.getBuffer();
      if ((event.isCommand() || buffer.startsWith("/")) && buffer.indexOf(32) != -1) {
         try {
            List<String> completions = this.getCompletions(buffer, event.getCompletions(), event.getSender(), true);
            if (completions != null) {
               if (completions.size() == 1 && completions.get(0).equals("")) {
                  completions.set(0, " ");
               }

               event.setCompletions(completions);
               event.setHandled(true);
            }
         } catch (Exception var4) {
            if (!(var4 instanceof CommandCompletions.SyncCompletionRequired)) {
               throw var4;
            }
         }
      }
   }

   private List<String> getCompletions(String buffer, List<String> existingCompletions, CommandSender sender, boolean async) {
      String[] args = ACFPatterns.SPACE.split(buffer, -1);
      String commandLabel = stripLeadingSlash(args[0]);
      args = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[]{""};
      RootCommand rootCommand = this.manager.getRootCommand(commandLabel);
      if (rootCommand == null) {
         return null;
      } else {
         BukkitCommandIssuer issuer = this.manager.getCommandIssuer(sender);
         List<String> completions = rootCommand.getTabCompletions(issuer, commandLabel, args, false, async);
         return ACFUtil.preformOnImmutable(existingCompletions, list -> list.addAll(completions));
      }
   }

   private static String stripLeadingSlash(String arg) {
      return arg.startsWith("/") ? arg.substring(1) : arg;
   }
}
