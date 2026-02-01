package co.aikar.commands;

import org.bukkit.Bukkit;

@Deprecated
public class ACFFoliaScheduler extends ACFPaperScheduler {
   public ACFFoliaScheduler() {
      super(Bukkit.getAsyncScheduler());
   }
}
