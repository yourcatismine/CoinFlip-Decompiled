package co.aikar.commands;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class ACFBukkitScheduler {
   private int localeTask;

   public void registerSchedulerDependencies(BukkitCommandManager manager) {
      manager.registerDependency(BukkitScheduler.class, Bukkit.getScheduler());
   }

   public void createDelayedTask(Plugin plugin, Runnable task, long delay) {
      Bukkit.getScheduler().runTaskLater(plugin, task, delay);
   }

   public void createLocaleTask(Plugin plugin, Runnable task, long delay, long period) {
      this.localeTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period).getTaskId();
   }

   public void cancelLocaleTask() {
      Bukkit.getScheduler().cancelTask(this.localeTask);
   }
}
