package co.aikar.commands;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

@Deprecated
@UnstableAPI
public class PaperBrigadierManager implements Listener {
   private final PaperCommandManager manager;
   private final ACFBrigadierManager<BukkitBrigadierCommandSource> brigadierManager;

   public PaperBrigadierManager(Plugin plugin, PaperCommandManager manager) {
      manager.verifyUnstableAPI("brigadier");
      manager.log(LogLevel.INFO, "Enabled Brigadier Support!");
      this.manager = manager;
      this.brigadierManager = new ACFBrigadierManager<>(manager);
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onCommandRegister(CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
      RootCommand acfCommand = this.manager.getRootCommand(event.getCommandLabel());
      if (acfCommand != null) {
         event.setLiteral(
            this.brigadierManager
               .register(acfCommand, event.getLiteral(), event.getBrigadierCommand(), event.getBrigadierCommand(), this::checkPermRoot, this::checkPermSub)
         );
      }
   }

   private boolean checkPermSub(RegisteredCommand registeredCommand, BukkitBrigadierCommandSource sender) {
      return registeredCommand.hasPermission(this.manager.getCommandIssuer(sender.getBukkitSender()));
   }

   private boolean checkPermRoot(RootCommand rootCommand, BukkitBrigadierCommandSource sender) {
      return rootCommand.hasAnyPermission(this.manager.getCommandIssuer(sender.getBukkitSender()));
   }
}
