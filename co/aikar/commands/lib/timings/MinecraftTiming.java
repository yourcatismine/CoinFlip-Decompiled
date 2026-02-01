package co.aikar.commands.lib.timings;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.bukkit.plugin.Plugin;

class MinecraftTiming extends MCTiming {
   private final Timing timing;

   MinecraftTiming(Plugin plugin, String name, MCTiming parent) {
      this.timing = Timings.of(plugin, name, parent instanceof MinecraftTiming ? ((MinecraftTiming)parent).timing : null);
   }

   @Override
   public MCTiming startTiming() {
      this.timing.startTimingIfSync();
      return this;
   }

   @Override
   public void stopTiming() {
      this.timing.stopTimingIfSync();
   }
}
