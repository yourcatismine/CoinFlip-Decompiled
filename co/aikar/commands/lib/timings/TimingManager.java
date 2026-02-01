package co.aikar.commands.lib.timings;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.plugin.Plugin;

public class TimingManager {
   private static TimingType timingProvider;
   private static final Object LOCK = new Object();
   private final Plugin plugin;
   private final Map<String, MCTiming> timingCache = new HashMap<>(0);

   private TimingManager(Plugin plugin) {
      this.plugin = plugin;
   }

   public static TimingManager of(Plugin plugin) {
      return new TimingManager(plugin);
   }

   public MCTiming ofStart(String name) {
      return this.ofStart(name, null);
   }

   public MCTiming ofStart(String name, MCTiming parent) {
      return this.of(name, parent).startTiming();
   }

   public MCTiming of(String name) {
      return this.of(name, null);
   }

   public MCTiming of(String name, MCTiming parent) {
      if (timingProvider == null) {
         synchronized (LOCK) {
            if (timingProvider == null) {
               try {
                  Class<?> clazz = Class.forName("co.aikar.timings.Timing");
                  Method startTiming = clazz.getMethod("startTiming");
                  if (startTiming.getReturnType() != clazz) {
                     timingProvider = TimingType.MINECRAFT_18;
                  } else {
                     timingProvider = TimingType.MINECRAFT;
                  }
               } catch (NoSuchMethodException | ClassNotFoundException var10) {
                  try {
                     Class.forName("org.spigotmc.CustomTimingsHandler");
                     timingProvider = TimingType.SPIGOT;
                  } catch (ClassNotFoundException var9) {
                     timingProvider = TimingType.EMPTY;
                  }
               }
            }
         }
      }

      if (timingProvider.useCache()) {
         synchronized (this.timingCache) {
            String lowerKey = name.toLowerCase();
            MCTiming timing = this.timingCache.get(lowerKey);
            if (timing == null) {
               timing = timingProvider.newTiming(this.plugin, name, parent);
               this.timingCache.put(lowerKey, timing);
            }

            return timing;
         }
      } else {
         return timingProvider.newTiming(this.plugin, name, parent);
      }
   }
}
