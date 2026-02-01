package co.aikar.commands.lib.timings;

import java.lang.reflect.InvocationTargetException;
import org.bukkit.plugin.Plugin;

enum TimingType {
   SPIGOT(true) {
      @Override
      MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
         return new SpigotTiming(command);
      }
   },
   MINECRAFT {
      @Override
      MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
         return new MinecraftTiming(plugin, command, parent);
      }
   },
   MINECRAFT_18 {
      @Override
      MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
         try {
            return new Minecraft18Timing(plugin, command, parent);
         } catch (IllegalAccessException | InvocationTargetException var5) {
            return new EmptyTiming();
         }
      }
   },
   EMPTY;

   private final boolean useCache;

   public boolean useCache() {
      return this.useCache;
   }

   private TimingType() {
      this(false);
   }

   private TimingType(boolean useCache) {
      this.useCache = useCache;
   }

   MCTiming newTiming(Plugin plugin, String command, MCTiming parent) {
      return new EmptyTiming();
   }
}
