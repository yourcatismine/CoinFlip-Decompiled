package com.kstudio.ultracoinflip.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FoliaScheduler {
   private static volatile Boolean isFolia = null;
   private static final Lookup LOOKUP = MethodHandles.publicLookup();
   private static volatile Object globalRegionScheduler = null;
   private static volatile Object regionScheduler = null;
   private static volatile Object asyncScheduler = null;
   private static volatile Method getGlobalRegionSchedulerMethod = null;
   private static volatile Method getRegionSchedulerMethod = null;
   private static volatile Method getAsyncSchedulerMethod = null;
   private static volatile MethodHandle entityGetSchedulerHandle = null;
   private static final ConcurrentMap<Class<?>, FoliaScheduler.SchedulerBundle> schedulerBundles = new ConcurrentHashMap<>();
   private static final ConcurrentMap<Class<?>, FoliaScheduler.EntitySchedulerBundle> entitySchedulerBundles = new ConcurrentHashMap<>();
   private static final ConcurrentMap<Class<?>, FoliaScheduler.RegionSchedulerBundle> regionSchedulerBundles = new ConcurrentHashMap<>();
   private static volatile FoliaScheduler.AsyncInvoker asyncSchedulerInvoker = null;
   private static final Object initializationLock = new Object();

   private static FoliaScheduler.SchedulerBundle getSchedulerBundle(Object scheduler) {
      if (scheduler == null) {
         return FoliaScheduler.SchedulerBundle.EMPTY;
      } else {
         Class<?> schedulerClass = scheduler.getClass();
         FoliaScheduler.SchedulerBundle bundle = schedulerBundles.get(schedulerClass);
         return bundle != null ? bundle : cacheSchedulerBundle(schedulerClass);
      }
   }

   private static FoliaScheduler.EntitySchedulerBundle getEntitySchedulerBundle(Object scheduler) {
      if (scheduler == null) {
         return FoliaScheduler.EntitySchedulerBundle.EMPTY;
      } else {
         Class<?> schedulerClass = scheduler.getClass();
         FoliaScheduler.EntitySchedulerBundle bundle = entitySchedulerBundles.get(schedulerClass);
         return bundle != null ? bundle : cacheEntitySchedulerBundle(schedulerClass);
      }
   }

   private static FoliaScheduler.RegionSchedulerBundle getRegionSchedulerBundle(Object scheduler) {
      if (scheduler == null) {
         return FoliaScheduler.RegionSchedulerBundle.EMPTY;
      } else {
         Class<?> schedulerClass = scheduler.getClass();
         FoliaScheduler.RegionSchedulerBundle bundle = regionSchedulerBundles.get(schedulerClass);
         return bundle != null ? bundle : cacheRegionSchedulerBundle(schedulerClass);
      }
   }

   public static boolean isFolia() {
      if (isFolia == null) {
         synchronized (initializationLock) {
            if (isFolia == null) {
               try {
                  Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                  isFolia = true;
                  initializeFoliaMethods();
               } catch (ClassNotFoundException var3) {
                  isFolia = false;
               }
            }
         }
      }

      return isFolia;
   }

   private static void initializeFoliaMethods() {
      try {
         getGlobalRegionSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
         getRegionSchedulerMethod = Bukkit.class.getMethod("getRegionScheduler");
         getAsyncSchedulerMethod = Bukkit.class.getMethod("getAsyncScheduler");
         Method entityGetSchedulerMethod = Entity.class.getMethod("getScheduler");
         entityGetSchedulerHandle = LOOKUP.unreflect(entityGetSchedulerMethod);
         globalRegionScheduler = getGlobalRegionSchedulerMethod.invoke(null);
         regionScheduler = getRegionSchedulerMethod.invoke(null);
         asyncScheduler = getAsyncSchedulerMethod.invoke(null);
         if (globalRegionScheduler != null) {
            cacheSchedulerBundle(globalRegionScheduler.getClass());
         }

         if (regionScheduler != null) {
            cacheRegionSchedulerBundle(regionScheduler.getClass());
         }

         if (asyncScheduler != null) {
            asyncSchedulerInvoker = createAsyncInvoker(asyncScheduler.getClass());
         }
      } catch (Exception var3) {
         Exception e = var3;

         try {
            if (Bukkit.getServer() != null && Bukkit.getServer().getLogger() != null) {
               Bukkit.getServer()
                  .getLogger()
                  .warning(
                     "[FoliaScheduler] Could not initialize Folia scheduler methods: "
                        + e.getMessage()
                        + ". Async tasks will use Java CompletableFuture as fallback."
                  );
            }
         } catch (Exception var2) {
         }
      }
   }

   private static FoliaScheduler.AsyncInvoker createAsyncInvoker(Class<?> schedulerClass) {
      MethodHandle primaryHandle = findHandle(schedulerClass, "runNow", Plugin.class, Consumer.class);
      if (primaryHandle != null) {
         return (scheduler, plugin, task) -> {
            primaryHandle.invoke(scheduler, plugin, createConsumer(task));
         };
      } else {
         MethodHandle fallbackHandle = findHandle(schedulerClass, "runNow", Plugin.class, Consumer.class, Runnable.class);
         if (fallbackHandle != null) {
            return (scheduler, plugin, task) -> {
               fallbackHandle.invoke(scheduler, plugin, createConsumer(task), (Runnable)null);
            };
         } else {
            try {
               if (Bukkit.getServer() != null && Bukkit.getServer().getLogger() != null) {
                  Bukkit.getServer()
                     .getLogger()
                     .warning("[FoliaScheduler] Could not find async scheduler runNow method. Async tasks will use Java CompletableFuture as fallback.");
               }
            } catch (Exception var4) {
            }

            return null;
         }
      }
   }

   private static FoliaScheduler.SchedulerBundle cacheSchedulerBundle(Class<?> schedulerClass) {
      MethodHandle runHandle = findHandle(schedulerClass, "run", Plugin.class, Consumer.class);
      MethodHandle runDelayedHandle = findHandle(schedulerClass, "runDelayed", Plugin.class, Consumer.class, long.class);
      MethodHandle runAtFixedRateHandle = findHandle(schedulerClass, "runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
      FoliaScheduler.SchedulerBundle bundle = new FoliaScheduler.SchedulerBundle(runHandle, runDelayedHandle, runAtFixedRateHandle);
      schedulerBundles.put(schedulerClass, bundle);
      return bundle;
   }

   private static FoliaScheduler.EntitySchedulerBundle cacheEntitySchedulerBundle(Class<?> schedulerClass) {
      MethodHandle runHandle = findHandle(schedulerClass, "run", Plugin.class, Consumer.class, Runnable.class);
      MethodHandle runDelayedHandle = findHandle(schedulerClass, "runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class);
      MethodHandle runAtFixedRateHandle = findHandle(schedulerClass, "runAtFixedRate", Plugin.class, Consumer.class, Runnable.class, long.class, long.class);
      FoliaScheduler.EntitySchedulerBundle bundle = new FoliaScheduler.EntitySchedulerBundle(runHandle, runDelayedHandle, runAtFixedRateHandle);
      entitySchedulerBundles.put(schedulerClass, bundle);
      return bundle;
   }

   private static FoliaScheduler.RegionSchedulerBundle cacheRegionSchedulerBundle(Class<?> schedulerClass) {
      MethodHandle runHandle = findHandle(schedulerClass, "run", Plugin.class, Location.class, Consumer.class);
      MethodHandle runDelayedHandle = findHandle(schedulerClass, "runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
      FoliaScheduler.RegionSchedulerBundle bundle = new FoliaScheduler.RegionSchedulerBundle(runHandle, runDelayedHandle);
      regionSchedulerBundles.put(schedulerClass, bundle);
      return bundle;
   }

   private static MethodHandle findHandle(Class<?> owner, String name, Class<?>... parameterTypes) {
      try {
         Method method = owner.getMethod(name, parameterTypes);
         method.setAccessible(true);
         return LOOKUP.unreflect(method);
      } catch (IllegalAccessException | NoSuchMethodException var4) {
         return null;
      }
   }

   private static boolean isValidEntity(Entity entity) {
      if (entity == null) {
         return false;
      } else {
         try {
            return entity.isValid() && !entity.isDead();
         } catch (Exception var2) {
            return false;
         }
      }
   }

   private static Object resolveEntityScheduler(Entity entity) {
      if (entityGetSchedulerHandle != null && entity != null) {
         try {
            return (Object)entityGetSchedulerHandle.invoke((Entity)entity);
         } catch (Throwable var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   private static boolean isValidPlayer(Player player) {
      if (player == null) {
         return false;
      } else {
         try {
            return player.isOnline() && isValidEntity(player);
         } catch (Exception var2) {
            return false;
         }
      }
   }

   public static void runTaskAsynchronously(Plugin plugin, Runnable task) {
      if (task != null && plugin != null) {
         if (isFolia()) {
            try {
               if (asyncScheduler != null && asyncSchedulerInvoker != null) {
                  asyncSchedulerInvoker.invoke(asyncScheduler, plugin, task);
               } else {
                  if (asyncScheduler != null && asyncSchedulerInvoker == null) {
                     asyncSchedulerInvoker = createAsyncInvoker(asyncScheduler.getClass());
                     if (asyncSchedulerInvoker != null) {
                        asyncSchedulerInvoker.invoke(asyncScheduler, plugin, task);
                        return;
                     }
                  }

                  CompletableFuture.runAsync(task);
               }
            } catch (Throwable var7) {
               try {
                  CompletableFuture.runAsync(task);
               } catch (Exception var6) {
                  Exception e2 = var6;

                  try {
                     if (Bukkit.getServer() != null && Bukkit.getServer().getLogger() != null) {
                        Bukkit.getServer().getLogger().severe("[FoliaScheduler] Failed to run async task: " + e2.getMessage());
                     }
                  } catch (Exception var5) {
                  }
               }
            }
         } else {
            try {
               Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            } catch (UnsupportedOperationException var8) {
               CompletableFuture.runAsync(task);
            }
         }
      }
   }

   private static Consumer<?> createConsumer(Runnable task) {
      return task == null ? scheduledTask -> {} : scheduledTask -> {
         try {
            task.run();
         } catch (Exception var5) {
            Exception e = var5;

            try {
               if (Bukkit.getServer() != null && Bukkit.getServer().getLogger() != null) {
                  Bukkit.getServer().getLogger().severe("Error in scheduled task: " + e.getMessage());
                  e.printStackTrace();
               }
            } catch (Exception var4) {
            }
         }
      };
   }

   public static void runTask(Plugin plugin, Runnable task) {
      if (task != null && plugin != null) {
         if (isFolia()) {
            try {
               FoliaScheduler.SchedulerBundle bundle = getSchedulerBundle(globalRegionScheduler);
               if (bundle.runHandle != null) {
                  bundle.runHandle.invoke((Object)globalRegionScheduler, (Plugin)plugin, (Consumer)createConsumer(task));
                  return;
               }
            } catch (Throwable var3) {
            }
         }

         Bukkit.getScheduler().runTask(plugin, task);
      }
   }

   public static void runTask(Plugin plugin, Entity entity, Runnable task) {
      if (task != null && plugin != null && isValidEntity(entity)) {
         if (isFolia()) {
            try {
               Object entityScheduler = resolveEntityScheduler(entity);
               FoliaScheduler.EntitySchedulerBundle bundle = getEntitySchedulerBundle(entityScheduler);
               if (bundle.runHandle != null) {
                  bundle.runHandle.invoke((Object)entityScheduler, (Plugin)plugin, (Consumer)createConsumer(task), (Void)null);
                  return;
               }
            } catch (Throwable var5) {
            }
         }

         if (isValidEntity(entity)) {
            Bukkit.getScheduler().runTask(plugin, task);
         }
      }
   }

   public static void runTask(Plugin plugin, Player player, Runnable task) {
      if (isValidPlayer(player)) {
         runTask(plugin, (Entity)player, task);
      }
   }

   public static void runTaskLater(Plugin plugin, Runnable task, long delay) {
      if (task != null && plugin != null && delay >= 0L) {
         if (isFolia()) {
            try {
               FoliaScheduler.SchedulerBundle bundle = getSchedulerBundle(globalRegionScheduler);
               if (bundle.runDelayedHandle != null) {
                  bundle.runDelayedHandle.invoke((Object)globalRegionScheduler, (Plugin)plugin, (Consumer)createConsumer(task), (long)delay);
                  return;
               }
            } catch (Throwable var5) {
            }
         }

         Bukkit.getScheduler().runTaskLater(plugin, task, delay);
      }
   }

   public static void runTaskLater(Plugin plugin, Entity entity, Runnable task, long delay) {
      if (task != null && plugin != null && delay >= 0L && isValidEntity(entity)) {
         if (isFolia()) {
            try {
               Object entityScheduler = resolveEntityScheduler(entity);
               FoliaScheduler.EntitySchedulerBundle bundle = getEntitySchedulerBundle(entityScheduler);
               if (bundle.runDelayedHandle != null) {
                  bundle.runDelayedHandle.invoke((Object)entityScheduler, (Plugin)plugin, (Consumer)createConsumer(task), (Void)null, (long)delay);
                  return;
               }
            } catch (Throwable var7) {
            }
         }

         if (isValidEntity(entity)) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delay);
         }
      }
   }

   public static void runTaskLater(Plugin plugin, Player player, Runnable task, long delay) {
      if (isValidPlayer(player)) {
         runTaskLater(plugin, (Entity)player, task, delay);
      }
   }

   public static Object runTaskTimer(Plugin plugin, Runnable task, long delay, long period) {
      if (task != null && plugin != null && delay >= 0L && period > 0L) {
         if (isFolia()) {
            try {
               FoliaScheduler.SchedulerBundle bundle = getSchedulerBundle(globalRegionScheduler);
               if (bundle.runAtFixedRateHandle != null) {
                  return (Object)bundle.runAtFixedRateHandle
                     .invoke((Object)globalRegionScheduler, (Plugin)plugin, (Consumer)createConsumer(task), (long)delay, (long)period);
               }
            } catch (Throwable var7) {
            }
         }

         return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, period);
      } else {
         return null;
      }
   }

   public static Object runTaskTimer(Plugin plugin, Entity entity, Runnable task, long delay, long period) {
      if (task != null && plugin != null && delay >= 0L && period > 0L && isValidEntity(entity)) {
         if (isFolia()) {
            try {
               Object entityScheduler = resolveEntityScheduler(entity);
               FoliaScheduler.EntitySchedulerBundle bundle = getEntitySchedulerBundle(entityScheduler);
               if (bundle.runAtFixedRateHandle != null) {
                  return (Object)bundle.runAtFixedRateHandle
                     .invoke((Object)entityScheduler, (Plugin)plugin, (Consumer)createConsumer(task), (Void)null, (long)delay, (long)period);
               }
            } catch (Throwable var9) {
            }
         }

         return isValidEntity(entity) ? Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, period) : null;
      } else {
         return null;
      }
   }

   public static Object runTaskTimer(Plugin plugin, Player player, Runnable task, long delay, long period) {
      return !isValidPlayer(player) ? null : runTaskTimer(plugin, (Entity)player, task, delay, period);
   }

   public static void runTask(Plugin plugin, Location location, Runnable task) {
      if (task != null && plugin != null && location != null) {
         if (isFolia()) {
            try {
               FoliaScheduler.RegionSchedulerBundle bundle = getRegionSchedulerBundle(regionScheduler);
               if (bundle.runHandle != null) {
                  bundle.runHandle.invoke((Object)regionScheduler, (Plugin)plugin, (Location)location, (Consumer)createConsumer(task));
                  return;
               }
            } catch (Throwable var4) {
            }
         }

         Bukkit.getScheduler().runTask(plugin, task);
      }
   }

   public static void runTaskLater(Plugin plugin, Location location, Runnable task, long delay) {
      if (task != null && plugin != null && location != null && delay >= 0L) {
         if (isFolia()) {
            try {
               FoliaScheduler.RegionSchedulerBundle bundle = getRegionSchedulerBundle(regionScheduler);
               if (bundle.runDelayedHandle != null) {
                  bundle.runDelayedHandle.invoke((Object)regionScheduler, (Plugin)plugin, (Location)location, (Consumer)createConsumer(task), (long)delay);
                  return;
               }
            } catch (Throwable var6) {
            }
         }

         Bukkit.getScheduler().runTaskLater(plugin, task, delay);
      }
   }

   public static void cancelTask(Plugin plugin, Object taskId) {
      if (taskId != null && plugin != null) {
         if (isFolia()) {
            try {
               Class<?> scheduledTaskClass = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
               if (scheduledTaskClass.isInstance(taskId)) {
                  Method cancelMethod = scheduledTaskClass.getMethod("cancel");
                  cancelMethod.invoke(taskId);
                  return;
               }
            } catch (Exception var4) {
            }
         }

         if (taskId instanceof Integer) {
            Bukkit.getScheduler().cancelTask((Integer)taskId);
         }
      }
   }

   public static boolean isRegionThread() {
      return isFolia() ? true : Bukkit.isPrimaryThread();
   }

   @FunctionalInterface
   private interface AsyncInvoker {
      void invoke(Object var1, Plugin var2, Runnable var3) throws Throwable;
   }

   private static final class EntitySchedulerBundle {
      private static final FoliaScheduler.EntitySchedulerBundle EMPTY = new FoliaScheduler.EntitySchedulerBundle(null, null, null);
      private final MethodHandle runHandle;
      private final MethodHandle runDelayedHandle;
      private final MethodHandle runAtFixedRateHandle;

      private EntitySchedulerBundle(MethodHandle runHandle, MethodHandle runDelayedHandle, MethodHandle runAtFixedRateHandle) {
         this.runHandle = runHandle;
         this.runDelayedHandle = runDelayedHandle;
         this.runAtFixedRateHandle = runAtFixedRateHandle;
      }
   }

   private static final class RegionSchedulerBundle {
      private static final FoliaScheduler.RegionSchedulerBundle EMPTY = new FoliaScheduler.RegionSchedulerBundle(null, null);
      private final MethodHandle runHandle;
      private final MethodHandle runDelayedHandle;

      private RegionSchedulerBundle(MethodHandle runHandle, MethodHandle runDelayedHandle) {
         this.runHandle = runHandle;
         this.runDelayedHandle = runDelayedHandle;
      }
   }

   private static final class SchedulerBundle {
      private static final FoliaScheduler.SchedulerBundle EMPTY = new FoliaScheduler.SchedulerBundle(null, null, null);
      private final MethodHandle runHandle;
      private final MethodHandle runDelayedHandle;
      private final MethodHandle runAtFixedRateHandle;

      private SchedulerBundle(MethodHandle runHandle, MethodHandle runDelayedHandle, MethodHandle runAtFixedRateHandle) {
         this.runHandle = runHandle;
         this.runDelayedHandle = runDelayedHandle;
         this.runAtFixedRateHandle = runAtFixedRateHandle;
      }
   }
}
