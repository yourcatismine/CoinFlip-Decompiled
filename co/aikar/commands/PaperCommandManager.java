package co.aikar.commands;

import org.bukkit.plugin.Plugin;

public class PaperCommandManager extends BukkitCommandManager {
   private boolean brigadierAvailable;

   public PaperCommandManager(Plugin plugin) {
      super(plugin);

      try {
         Class.forName("com.destroystokyo.paper.event.server.AsyncTabCompleteEvent");
         plugin.getServer().getPluginManager().registerEvents(new PaperAsyncTabCompleteHandler(this), plugin);
      } catch (ClassNotFoundException var4) {
      }

      try {
         Class.forName("com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent");
         this.brigadierAvailable = true;
      } catch (ClassNotFoundException var3) {
      }
   }

   @Override
   public void enableUnstableAPI(String api) {
      super.enableUnstableAPI(api);
      if ("brigadier".equals(api) && this.brigadierAvailable) {
         new PaperBrigadierManager(this.plugin, this);
      }
   }

   @Override
   public synchronized CommandContexts<BukkitCommandExecutionContext> getCommandContexts() {
      if (this.contexts == null) {
         this.contexts = new PaperCommandContexts(this);
      }

      return this.contexts;
   }

   @Override
   public synchronized CommandCompletions<BukkitCommandCompletionContext> getCommandCompletions() {
      if (this.completions == null) {
         this.completions = new PaperCommandCompletions(this);
      }

      return this.completions;
   }
}
