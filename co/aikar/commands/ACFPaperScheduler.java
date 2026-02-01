package co.aikar.commands;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ACFPaperScheduler extends ACFBukkitScheduler {
   private final AsyncScheduler scheduler;
   private ScheduledTask localeTask;

   public ACFPaperScheduler(@NotNull AsyncScheduler scheduler) {
      this.scheduler = scheduler;
   }

   @Override
   public void registerSchedulerDependencies(BukkitCommandManager manager) {
      manager.registerDependency(AsyncScheduler.class, this.scheduler);
   }

   @Override
   public void createDelayedTask(Plugin plugin, Runnable task, long delay) {
      this.scheduler.runDelayed(plugin, scheduledTask -> task.run(), delay / 20L, TimeUnit.SECONDS);
   }

   @Override
   public void createLocaleTask(Plugin plugin, Runnable task, long delay, long period) {
      this.localeTask = this.scheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delay / 20L, period / 20L, TimeUnit.SECONDS);
   }

   @Override
   public void cancelLocaleTask() {
      this.localeTask.cancel();
   }
}
