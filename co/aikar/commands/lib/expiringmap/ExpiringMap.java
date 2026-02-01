package co.aikar.commands.lib.expiringmap;

import co.aikar.commands.lib.expiringmap.internal.Assert;
import co.aikar.commands.lib.expiringmap.internal.NamedThreadFactory;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExpiringMap<K, V> implements ConcurrentMap<K, V> {
   static volatile ScheduledExecutorService EXPIRER;
   static volatile ThreadPoolExecutor LISTENER_SERVICE;
   static ThreadFactory THREAD_FACTORY;
   List<ExpirationListener<K, V>> expirationListeners;
   List<ExpirationListener<K, V>> asyncExpirationListeners;
   private AtomicLong expirationNanos;
   private int maxSize;
   private final AtomicReference<ExpirationPolicy> expirationPolicy;
   private final EntryLoader<? super K, ? extends V> entryLoader;
   private final ExpiringEntryLoader<? super K, ? extends V> expiringEntryLoader;
   private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
   private final Lock readLock = this.readWriteLock.readLock();
   private final Lock writeLock = this.readWriteLock.writeLock();
   private final ExpiringMap.EntryMap<K, V> entries;
   private final boolean variableExpiration;

   public static void setThreadFactory(ThreadFactory threadFactory) {
      THREAD_FACTORY = Assert.notNull(threadFactory, "threadFactory");
   }

   private ExpiringMap(ExpiringMap.Builder<K, V> builder) {
      if (EXPIRER == null) {
         synchronized (ExpiringMap.class) {
            if (EXPIRER == null) {
               EXPIRER = Executors.newSingleThreadScheduledExecutor(
                  (ThreadFactory)(THREAD_FACTORY == null ? new NamedThreadFactory("ExpiringMap-Expirer") : THREAD_FACTORY)
               );
            }
         }
      }

      if (LISTENER_SERVICE == null && builder.asyncExpirationListeners != null) {
         this.initListenerService();
      }

      this.variableExpiration = builder.variableExpiration;
      this.entries = (ExpiringMap.EntryMap<K, V>)(this.variableExpiration ? new ExpiringMap.EntryTreeHashMap<>() : new ExpiringMap.EntryLinkedHashMap<>());
      if (builder.expirationListeners != null) {
         this.expirationListeners = new CopyOnWriteArrayList<>(builder.expirationListeners);
      }

      if (builder.asyncExpirationListeners != null) {
         this.asyncExpirationListeners = new CopyOnWriteArrayList<>(builder.asyncExpirationListeners);
      }

      this.expirationPolicy = new AtomicReference<>(builder.expirationPolicy);
      this.expirationNanos = new AtomicLong(TimeUnit.NANOSECONDS.convert(builder.duration, builder.timeUnit));
      this.maxSize = builder.maxSize;
      this.entryLoader = builder.entryLoader;
      this.expiringEntryLoader = builder.expiringEntryLoader;
   }

   public static ExpiringMap.Builder<Object, Object> builder() {
      return new ExpiringMap.Builder<>();
   }

   public static <K, V> ExpiringMap<K, V> create() {
      return new ExpiringMap<>((ExpiringMap.Builder<K, V>)builder());
   }

   public synchronized void addExpirationListener(ExpirationListener<K, V> listener) {
      Assert.notNull(listener, "listener");
      if (this.expirationListeners == null) {
         this.expirationListeners = new CopyOnWriteArrayList<>();
      }

      this.expirationListeners.add(listener);
   }

   public synchronized void addAsyncExpirationListener(ExpirationListener<K, V> listener) {
      Assert.notNull(listener, "listener");
      if (this.asyncExpirationListeners == null) {
         this.asyncExpirationListeners = new CopyOnWriteArrayList<>();
      }

      this.asyncExpirationListeners.add(listener);
      if (LISTENER_SERVICE == null) {
         this.initListenerService();
      }
   }

   @Override
   public void clear() {
      this.writeLock.lock();

      try {
         for (ExpiringMap.ExpiringEntry<K, V> entry : this.entries.values()) {
            entry.cancel();
         }

         this.entries.clear();
      } finally {
         this.writeLock.unlock();
      }
   }

   @Override
   public boolean containsKey(Object key) {
      this.readLock.lock();

      boolean var2;
      try {
         var2 = this.entries.containsKey(key);
      } finally {
         this.readLock.unlock();
      }

      return var2;
   }

   @Override
   public boolean containsValue(Object value) {
      this.readLock.lock();

      boolean var2;
      try {
         var2 = this.entries.containsValue(value);
      } finally {
         this.readLock.unlock();
      }

      return var2;
   }

   @Override
   public Set<Entry<K, V>> entrySet() {
      return new AbstractSet<Entry<K, V>>() {
         @Override
         public void clear() {
            ExpiringMap.this.clear();
         }

         @Override
         public boolean contains(Object entry) {
            if (!(entry instanceof Entry)) {
               return false;
            } else {
               Entry<?, ?> e = (Entry<?, ?>)entry;
               return ExpiringMap.this.containsKey(e.getKey());
            }
         }

         @Override
         public Iterator<Entry<K, V>> iterator() {
            return (Iterator<Entry<K, V>>)(ExpiringMap.this.entries instanceof ExpiringMap.EntryLinkedHashMap
               ? (ExpiringMap.EntryLinkedHashMap)ExpiringMap.this.entries.new EntryIterator()
               : (ExpiringMap.EntryTreeHashMap)ExpiringMap.this.entries.new EntryIterator());
         }

         @Override
         public boolean remove(Object entry) {
            if (entry instanceof Entry) {
               Entry<?, ?> e = (Entry<?, ?>)entry;
               return ExpiringMap.this.remove(e.getKey()) != null;
            } else {
               return false;
            }
         }

         @Override
         public int size() {
            return ExpiringMap.this.size();
         }
      };
   }

   @Override
   public boolean equals(Object obj) {
      this.readLock.lock();

      boolean var2;
      try {
         var2 = this.entries.equals(obj);
      } finally {
         this.readLock.unlock();
      }

      return var2;
   }

   @Override
   public V get(Object key) {
      ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
      if (entry == null) {
         return this.load((K)key);
      } else {
         if (ExpirationPolicy.ACCESSED.equals(entry.expirationPolicy.get())) {
            this.resetEntry(entry, false);
         }

         return entry.getValue();
      }
   }

   private V load(K key) {
      if (this.entryLoader == null && this.expiringEntryLoader == null) {
         return null;
      } else {
         this.writeLock.lock();

         Object duration;
         try {
            ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
            if (entry != null) {
               return entry.getValue();
            }

            if (this.entryLoader != null) {
               V value = (V)this.entryLoader.load(key);
               this.put(key, value);
               return value;
            }

            ExpiringValue<? extends V> expiringValue = this.expiringEntryLoader.load(key);
            if (expiringValue != null) {
               long durationx = expiringValue.getTimeUnit() == null ? this.expirationNanos.get() : expiringValue.getDuration();
               TimeUnit timeUnit = expiringValue.getTimeUnit() == null ? TimeUnit.NANOSECONDS : expiringValue.getTimeUnit();
               this.put(
                  key,
                  (V)expiringValue.getValue(),
                  expiringValue.getExpirationPolicy() == null ? this.expirationPolicy.get() : expiringValue.getExpirationPolicy(),
                  durationx,
                  timeUnit
               );
               return (V)expiringValue.getValue();
            }

            this.put(key, null);
            duration = null;
         } finally {
            this.writeLock.unlock();
         }

         return (V)duration;
      }
   }

   public long getExpiration() {
      return TimeUnit.NANOSECONDS.toMillis(this.expirationNanos.get());
   }

   public long getExpiration(K key) {
      Assert.notNull(key, "key");
      ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
      Assert.element(entry, key);
      return TimeUnit.NANOSECONDS.toMillis(entry.expirationNanos.get());
   }

   public ExpirationPolicy getExpirationPolicy(K key) {
      Assert.notNull(key, "key");
      ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
      Assert.element(entry, key);
      return entry.expirationPolicy.get();
   }

   public long getExpectedExpiration(K key) {
      Assert.notNull(key, "key");
      ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
      Assert.element(entry, key);
      return TimeUnit.NANOSECONDS.toMillis(entry.expectedExpiration.get() - System.nanoTime());
   }

   public int getMaxSize() {
      return this.maxSize;
   }

   @Override
   public int hashCode() {
      this.readLock.lock();

      int var1;
      try {
         var1 = this.entries.hashCode();
      } finally {
         this.readLock.unlock();
      }

      return var1;
   }

   @Override
   public boolean isEmpty() {
      this.readLock.lock();

      boolean var1;
      try {
         var1 = this.entries.isEmpty();
      } finally {
         this.readLock.unlock();
      }

      return var1;
   }

   @Override
   public Set<K> keySet() {
      return new AbstractSet<K>() {
         @Override
         public void clear() {
            ExpiringMap.this.clear();
         }

         @Override
         public boolean contains(Object key) {
            return ExpiringMap.this.containsKey(key);
         }

         @Override
         public Iterator<K> iterator() {
            return (Iterator<K>)(ExpiringMap.this.entries instanceof ExpiringMap.EntryLinkedHashMap
               ? (ExpiringMap.EntryLinkedHashMap)ExpiringMap.this.entries.new KeyIterator()
               : (ExpiringMap.EntryTreeHashMap)ExpiringMap.this.entries.new KeyIterator());
         }

         @Override
         public boolean remove(Object value) {
            return ExpiringMap.this.remove(value) != null;
         }

         @Override
         public int size() {
            return ExpiringMap.this.size();
         }
      };
   }

   @Override
   public V put(K key, V value) {
      Assert.notNull(key, "key");
      return this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
   }

   public V put(K key, V value, ExpirationPolicy expirationPolicy) {
      return this.put(key, value, expirationPolicy, this.expirationNanos.get(), TimeUnit.NANOSECONDS);
   }

   public V put(K key, V value, long duration, TimeUnit timeUnit) {
      return this.put(key, value, this.expirationPolicy.get(), duration, timeUnit);
   }

   public V put(K key, V value, ExpirationPolicy expirationPolicy, long duration, TimeUnit timeUnit) {
      Assert.notNull(key, "key");
      Assert.notNull(expirationPolicy, "expirationPolicy");
      Assert.notNull(timeUnit, "timeUnit");
      Assert.operation(this.variableExpiration, "Variable expiration is not enabled");
      return this.putInternal(key, value, expirationPolicy, TimeUnit.NANOSECONDS.convert(duration, timeUnit));
   }

   @Override
   public void putAll(Map<? extends K, ? extends V> map) {
      Assert.notNull(map, "map");
      long expiration = this.expirationNanos.get();
      ExpirationPolicy expirationPolicy = this.expirationPolicy.get();
      this.writeLock.lock();

      try {
         for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            this.putInternal((K)entry.getKey(), (V)entry.getValue(), expirationPolicy, expiration);
         }
      } finally {
         this.writeLock.unlock();
      }
   }

   @Override
   public V putIfAbsent(K key, V value) {
      Assert.notNull(key, "key");
      this.writeLock.lock();

      Object var3;
      try {
         if (this.entries.containsKey(key)) {
            return this.entries.get(key).getValue();
         }

         var3 = this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
      } finally {
         this.writeLock.unlock();
      }

      return (V)var3;
   }

   @Override
   public V remove(Object key) {
      Assert.notNull(key, "key");
      this.writeLock.lock();

      Object var3;
      try {
         ExpiringMap.ExpiringEntry<K, V> entry = this.entries.remove(key);
         if (entry != null) {
            if (entry.cancel()) {
               this.scheduleEntry(this.entries.first());
            }

            return entry.getValue();
         }

         var3 = null;
      } finally {
         this.writeLock.unlock();
      }

      return (V)var3;
   }

   @Override
   public boolean remove(Object key, Object value) {
      Assert.notNull(key, "key");
      this.writeLock.lock();

      boolean var4;
      try {
         ExpiringMap.ExpiringEntry<K, V> entry = this.entries.get(key);
         if (entry == null || !entry.getValue().equals(value)) {
            return false;
         }

         this.entries.remove(key);
         if (entry.cancel()) {
            this.scheduleEntry(this.entries.first());
         }

         var4 = true;
      } finally {
         this.writeLock.unlock();
      }

      return var4;
   }

   @Override
   public V replace(K key, V value) {
      Assert.notNull(key, "key");
      this.writeLock.lock();

      Object var3;
      try {
         if (!this.entries.containsKey(key)) {
            return null;
         }

         var3 = this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
      } finally {
         this.writeLock.unlock();
      }

      return (V)var3;
   }

   @Override
   public boolean replace(K key, V oldValue, V newValue) {
      Assert.notNull(key, "key");
      this.writeLock.lock();

      boolean var5;
      try {
         ExpiringMap.ExpiringEntry<K, V> entry = this.entries.get(key);
         if (entry == null || !entry.getValue().equals(oldValue)) {
            return false;
         }

         this.putInternal(key, newValue, this.expirationPolicy.get(), this.expirationNanos.get());
         var5 = true;
      } finally {
         this.writeLock.unlock();
      }

      return var5;
   }

   public void removeExpirationListener(ExpirationListener<K, V> listener) {
      Assert.notNull(listener, "listener");

      for (int i = 0; i < this.expirationListeners.size(); i++) {
         if (this.expirationListeners.get(i).equals(listener)) {
            this.expirationListeners.remove(i);
            return;
         }
      }
   }

   public void removeAsyncExpirationListener(ExpirationListener<K, V> listener) {
      Assert.notNull(listener, "listener");

      for (int i = 0; i < this.asyncExpirationListeners.size(); i++) {
         if (this.asyncExpirationListeners.get(i).equals(listener)) {
            this.asyncExpirationListeners.remove(i);
            return;
         }
      }
   }

   public void resetExpiration(K key) {
      Assert.notNull(key, "key");
      ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
      if (entry != null) {
         this.resetEntry(entry, false);
      }
   }

   public void setExpiration(K key, long duration, TimeUnit timeUnit) {
      Assert.notNull(key, "key");
      Assert.notNull(timeUnit, "timeUnit");
      Assert.operation(this.variableExpiration, "Variable expiration is not enabled");
      this.writeLock.lock();

      try {
         ExpiringMap.ExpiringEntry<K, V> entry = this.entries.get(key);
         if (entry != null) {
            entry.expirationNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
            this.resetEntry(entry, true);
         }
      } finally {
         this.writeLock.unlock();
      }
   }

   public void setExpiration(long duration, TimeUnit timeUnit) {
      Assert.notNull(timeUnit, "timeUnit");
      Assert.operation(this.variableExpiration, "Variable expiration is not enabled");
      this.expirationNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
   }

   public void setExpirationPolicy(ExpirationPolicy expirationPolicy) {
      Assert.notNull(expirationPolicy, "expirationPolicy");
      this.expirationPolicy.set(expirationPolicy);
   }

   public void setExpirationPolicy(K key, ExpirationPolicy expirationPolicy) {
      Assert.notNull(key, "key");
      Assert.notNull(expirationPolicy, "expirationPolicy");
      Assert.operation(this.variableExpiration, "Variable expiration is not enabled");
      ExpiringMap.ExpiringEntry<K, V> entry = this.getEntry(key);
      if (entry != null) {
         entry.expirationPolicy.set(expirationPolicy);
      }
   }

   public void setMaxSize(int maxSize) {
      Assert.operation(maxSize > 0, "maxSize");
      this.maxSize = maxSize;
   }

   @Override
   public int size() {
      this.readLock.lock();

      int var1;
      try {
         var1 = this.entries.size();
      } finally {
         this.readLock.unlock();
      }

      return var1;
   }

   @Override
   public String toString() {
      this.readLock.lock();

      String var1;
      try {
         var1 = this.entries.toString();
      } finally {
         this.readLock.unlock();
      }

      return var1;
   }

   @Override
   public Collection<V> values() {
      return new AbstractCollection<V>() {
         @Override
         public void clear() {
            ExpiringMap.this.clear();
         }

         @Override
         public boolean contains(Object value) {
            return ExpiringMap.this.containsValue(value);
         }

         @Override
         public Iterator<V> iterator() {
            return (Iterator<V>)(ExpiringMap.this.entries instanceof ExpiringMap.EntryLinkedHashMap
               ? (ExpiringMap.EntryLinkedHashMap)ExpiringMap.this.entries.new ValueIterator()
               : (ExpiringMap.EntryTreeHashMap)ExpiringMap.this.entries.new ValueIterator());
         }

         @Override
         public int size() {
            return ExpiringMap.this.size();
         }
      };
   }

   void notifyListeners(final ExpiringMap.ExpiringEntry<K, V> entry) {
      if (this.asyncExpirationListeners != null) {
         for (final ExpirationListener<K, V> listener : this.asyncExpirationListeners) {
            LISTENER_SERVICE.execute(new Runnable() {
               @Override
               public void run() {
                  try {
                     listener.expired(entry.key, entry.getValue());
                  } catch (Exception var2) {
                  }
               }
            });
         }
      }

      if (this.expirationListeners != null) {
         for (ExpirationListener<K, V> listener : this.expirationListeners) {
            try {
               listener.expired(entry.key, entry.getValue());
            } catch (Exception var5) {
            }
         }
      }
   }

   ExpiringMap.ExpiringEntry<K, V> getEntry(Object key) {
      this.readLock.lock();

      ExpiringMap.ExpiringEntry var2;
      try {
         var2 = this.entries.get(key);
      } finally {
         this.readLock.unlock();
      }

      return var2;
   }

   V putInternal(K key, V value, ExpirationPolicy expirationPolicy, long expirationNanos) {
      this.writeLock.lock();

      try {
         ExpiringMap.ExpiringEntry<K, V> entry = this.entries.get(key);
         V oldValue = null;
         if (entry == null) {
            entry = new ExpiringMap.ExpiringEntry<>(
               key,
               value,
               this.variableExpiration ? new AtomicReference<>(expirationPolicy) : this.expirationPolicy,
               this.variableExpiration ? new AtomicLong(expirationNanos) : this.expirationNanos
            );
            if (this.entries.size() >= this.maxSize) {
               ExpiringMap.ExpiringEntry<K, V> expiredEntry = this.entries.first();
               this.entries.remove(expiredEntry.key);
               this.notifyListeners(expiredEntry);
            }

            this.entries.put(key, entry);
            if (this.entries.size() == 1 || this.entries.first().equals(entry)) {
               this.scheduleEntry(entry);
            }
         } else {
            oldValue = entry.getValue();
            if (!ExpirationPolicy.ACCESSED.equals(expirationPolicy) && (oldValue == null && value == null || oldValue != null && oldValue.equals(value))) {
               return value;
            }

            entry.setValue(value);
            this.resetEntry(entry, false);
         }

         return oldValue;
      } finally {
         this.writeLock.unlock();
      }
   }

   void resetEntry(ExpiringMap.ExpiringEntry<K, V> entry, boolean scheduleFirstEntry) {
      this.writeLock.lock();

      try {
         boolean scheduled = entry.cancel();
         this.entries.reorder(entry);
         if (scheduled || scheduleFirstEntry) {
            this.scheduleEntry(this.entries.first());
         }
      } finally {
         this.writeLock.unlock();
      }
   }

   void scheduleEntry(ExpiringMap.ExpiringEntry<K, V> entry) {
      if (entry != null && !entry.scheduled) {
         Runnable runnable = null;
         synchronized (entry) {
            if (!entry.scheduled) {
               final WeakReference<ExpiringMap.ExpiringEntry<K, V>> entryReference = new WeakReference<>(entry);
               runnable = new Runnable() {
                  @Override
                  public void run() {
                     ExpiringMap.ExpiringEntry<K, V> entryx = entryReference.get();
                     ExpiringMap.this.writeLock.lock();

                     try {
                        if (entryx != null && entryx.scheduled) {
                           ExpiringMap.this.entries.remove(entryx.key);
                           ExpiringMap.this.notifyListeners(entryx);
                        }

                        try {
                           Iterator<ExpiringMap.ExpiringEntry<K, V>> iterator = ExpiringMap.this.entries.valuesIterator();
                           boolean schedulePending = true;

                           while (iterator.hasNext() && schedulePending) {
                              ExpiringMap.ExpiringEntry<K, V> nextEntry = iterator.next();
                              if (nextEntry.expectedExpiration.get() <= System.nanoTime()) {
                                 iterator.remove();
                                 ExpiringMap.this.notifyListeners(nextEntry);
                              } else {
                                 ExpiringMap.this.scheduleEntry(nextEntry);
                                 schedulePending = false;
                              }
                           }
                        } catch (NoSuchElementException var8) {
                        }
                     } finally {
                        ExpiringMap.this.writeLock.unlock();
                     }
                  }
               };
               Future<?> entryFuture = EXPIRER.schedule(runnable, entry.expectedExpiration.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
               entry.schedule(entryFuture);
            }
         }
      }
   }

   private static <K, V> Entry<K, V> mapEntryFor(final ExpiringMap.ExpiringEntry<K, V> entry) {
      return new Entry<K, V>() {
         @Override
         public K getKey() {
            return entry.key;
         }

         @Override
         public V getValue() {
            return entry.value;
         }

         @Override
         public V setValue(V value) {
            throw new UnsupportedOperationException();
         }
      };
   }

   private void initListenerService() {
      synchronized (ExpiringMap.class) {
         if (LISTENER_SERVICE == null) {
            LISTENER_SERVICE = (ThreadPoolExecutor)Executors.newCachedThreadPool(
               (ThreadFactory)(THREAD_FACTORY == null ? new NamedThreadFactory("ExpiringMap-Listener-%s") : THREAD_FACTORY)
            );
         }
      }
   }

   public static final class Builder<K, V> {
      private ExpirationPolicy expirationPolicy = ExpirationPolicy.CREATED;
      private List<ExpirationListener<K, V>> expirationListeners;
      private List<ExpirationListener<K, V>> asyncExpirationListeners;
      private TimeUnit timeUnit = TimeUnit.SECONDS;
      private boolean variableExpiration;
      private long duration = 60L;
      private int maxSize = Integer.MAX_VALUE;
      private EntryLoader<K, V> entryLoader;
      private ExpiringEntryLoader<K, V> expiringEntryLoader;

      private Builder() {
      }

      public <K1 extends K, V1 extends V> ExpiringMap<K1, V1> build() {
         return new ExpiringMap<>(this);
      }

      public ExpiringMap.Builder<K, V> expiration(long duration, TimeUnit timeUnit) {
         this.duration = duration;
         this.timeUnit = Assert.notNull(timeUnit, "timeUnit");
         return this;
      }

      public ExpiringMap.Builder<K, V> maxSize(int maxSize) {
         Assert.operation(maxSize > 0, "maxSize");
         this.maxSize = maxSize;
         return this;
      }

      public <K1 extends K, V1 extends V> ExpiringMap.Builder<K1, V1> entryLoader(EntryLoader<? super K1, ? super V1> loader) {
         this.assertNoLoaderSet();
         this.entryLoader = Assert.notNull(loader, "loader");
         return this;
      }

      public <K1 extends K, V1 extends V> ExpiringMap.Builder<K1, V1> expiringEntryLoader(ExpiringEntryLoader<? super K1, ? super V1> loader) {
         this.assertNoLoaderSet();
         this.expiringEntryLoader = Assert.notNull(loader, "loader");
         this.variableExpiration();
         return this;
      }

      public <K1 extends K, V1 extends V> ExpiringMap.Builder<K1, V1> expirationListener(ExpirationListener<? super K1, ? super V1> listener) {
         Assert.notNull(listener, "listener");
         if (this.expirationListeners == null) {
            this.expirationListeners = new ArrayList<>();
         }

         this.expirationListeners.add(listener);
         return this;
      }

      public <K1 extends K, V1 extends V> ExpiringMap.Builder<K1, V1> expirationListeners(List<ExpirationListener<? super K1, ? super V1>> listeners) {
         Assert.notNull(listeners, "listeners");
         if (this.expirationListeners == null) {
            this.expirationListeners = new ArrayList<>(listeners.size());
         }

         for (ExpirationListener<? super K1, ? super V1> listener : listeners) {
            this.expirationListeners.add(listener);
         }

         return this;
      }

      public <K1 extends K, V1 extends V> ExpiringMap.Builder<K1, V1> asyncExpirationListener(ExpirationListener<? super K1, ? super V1> listener) {
         Assert.notNull(listener, "listener");
         if (this.asyncExpirationListeners == null) {
            this.asyncExpirationListeners = new ArrayList<>();
         }

         this.asyncExpirationListeners.add(listener);
         return this;
      }

      public <K1 extends K, V1 extends V> ExpiringMap.Builder<K1, V1> asyncExpirationListeners(List<ExpirationListener<? super K1, ? super V1>> listeners) {
         Assert.notNull(listeners, "listeners");
         if (this.asyncExpirationListeners == null) {
            this.asyncExpirationListeners = new ArrayList<>(listeners.size());
         }

         for (ExpirationListener<? super K1, ? super V1> listener : listeners) {
            this.asyncExpirationListeners.add(listener);
         }

         return this;
      }

      public ExpiringMap.Builder<K, V> expirationPolicy(ExpirationPolicy expirationPolicy) {
         this.expirationPolicy = Assert.notNull(expirationPolicy, "expirationPolicy");
         return this;
      }

      public ExpiringMap.Builder<K, V> variableExpiration() {
         this.variableExpiration = true;
         return this;
      }

      private void assertNoLoaderSet() {
         Assert.state(this.entryLoader == null && this.expiringEntryLoader == null, "Either entryLoader or expiringEntryLoader may be set, not both");
      }
   }

   private static class EntryLinkedHashMap<K, V> extends LinkedHashMap<K, ExpiringMap.ExpiringEntry<K, V>> implements ExpiringMap.EntryMap<K, V> {
      private static final long serialVersionUID = 1L;

      private EntryLinkedHashMap() {
      }

      @Override
      public boolean containsValue(Object value) {
         for (ExpiringMap.ExpiringEntry<K, V> entry : this.values()) {
            V v = entry.value;
            if (v == value || value != null && value.equals(v)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public ExpiringMap.ExpiringEntry<K, V> first() {
         return this.isEmpty() ? null : this.values().iterator().next();
      }

      @Override
      public void reorder(ExpiringMap.ExpiringEntry<K, V> value) {
         this.remove(value.key);
         value.resetExpiration();
         this.put(value.key, value);
      }

      @Override
      public Iterator<ExpiringMap.ExpiringEntry<K, V>> valuesIterator() {
         return this.values().iterator();
      }

      abstract class AbstractHashIterator {
         private final Iterator<Entry<K, ExpiringMap.ExpiringEntry<K, V>>> iterator = EntryLinkedHashMap.this.entrySet().iterator();
         private ExpiringMap.ExpiringEntry<K, V> next;

         public boolean hasNext() {
            return this.iterator.hasNext();
         }

         public ExpiringMap.ExpiringEntry<K, V> getNext() {
            this.next = this.iterator.next().getValue();
            return this.next;
         }

         public void remove() {
            this.iterator.remove();
         }
      }

      public final class EntryIterator extends ExpiringMap.EntryLinkedHashMap<K, V>.AbstractHashIterator implements Iterator<Entry<K, V>> {
         public final Entry<K, V> next() {
            return ExpiringMap.mapEntryFor(this.getNext());
         }
      }

      final class KeyIterator extends ExpiringMap.EntryLinkedHashMap<K, V>.AbstractHashIterator implements Iterator<K> {
         @Override
         public final K next() {
            return this.getNext().key;
         }
      }

      final class ValueIterator extends ExpiringMap.EntryLinkedHashMap<K, V>.AbstractHashIterator implements Iterator<V> {
         @Override
         public final V next() {
            return this.getNext().value;
         }
      }
   }

   private interface EntryMap<K, V> extends Map<K, ExpiringMap.ExpiringEntry<K, V>> {
      ExpiringMap.ExpiringEntry<K, V> first();

      void reorder(ExpiringMap.ExpiringEntry<K, V> var1);

      Iterator<ExpiringMap.ExpiringEntry<K, V>> valuesIterator();
   }

   private static class EntryTreeHashMap<K, V> extends HashMap<K, ExpiringMap.ExpiringEntry<K, V>> implements ExpiringMap.EntryMap<K, V> {
      private static final long serialVersionUID = 1L;
      SortedSet<ExpiringMap.ExpiringEntry<K, V>> sortedSet = new ConcurrentSkipListSet<>();

      private EntryTreeHashMap() {
      }

      @Override
      public void clear() {
         super.clear();
         this.sortedSet.clear();
      }

      @Override
      public boolean containsValue(Object value) {
         for (ExpiringMap.ExpiringEntry<K, V> entry : this.values()) {
            V v = entry.value;
            if (v == value || value != null && value.equals(v)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public ExpiringMap.ExpiringEntry<K, V> first() {
         return this.sortedSet.isEmpty() ? null : this.sortedSet.first();
      }

      public ExpiringMap.ExpiringEntry<K, V> put(K key, ExpiringMap.ExpiringEntry<K, V> value) {
         this.sortedSet.add(value);
         return (ExpiringMap.ExpiringEntry<K, V>)super.put(key, (V)value);
      }

      public ExpiringMap.ExpiringEntry<K, V> remove(Object key) {
         ExpiringMap.ExpiringEntry<K, V> entry = (ExpiringMap.ExpiringEntry<K, V>)super.remove(key);
         if (entry != null) {
            this.sortedSet.remove(entry);
         }

         return entry;
      }

      @Override
      public void reorder(ExpiringMap.ExpiringEntry<K, V> value) {
         this.sortedSet.remove(value);
         value.resetExpiration();
         this.sortedSet.add(value);
      }

      @Override
      public Iterator<ExpiringMap.ExpiringEntry<K, V>> valuesIterator() {
         return new ExpiringMap.EntryTreeHashMap.ExpiringEntryIterator();
      }

      abstract class AbstractHashIterator {
         private final Iterator<ExpiringMap.ExpiringEntry<K, V>> iterator = EntryTreeHashMap.this.sortedSet.iterator();
         protected ExpiringMap.ExpiringEntry<K, V> next;

         public boolean hasNext() {
            return this.iterator.hasNext();
         }

         public ExpiringMap.ExpiringEntry<K, V> getNext() {
            this.next = this.iterator.next();
            return this.next;
         }

         public void remove() {
            ExpiringMap.EntryTreeHashMap.super.remove(this.next.key);
            this.iterator.remove();
         }
      }

      final class EntryIterator extends ExpiringMap.EntryTreeHashMap<K, V>.AbstractHashIterator implements Iterator<Entry<K, V>> {
         public final Entry<K, V> next() {
            return ExpiringMap.mapEntryFor(this.getNext());
         }
      }

      final class ExpiringEntryIterator extends ExpiringMap.EntryTreeHashMap<K, V>.AbstractHashIterator implements Iterator<ExpiringMap.ExpiringEntry<K, V>> {
         public final ExpiringMap.ExpiringEntry<K, V> next() {
            return this.getNext();
         }
      }

      final class KeyIterator extends ExpiringMap.EntryTreeHashMap<K, V>.AbstractHashIterator implements Iterator<K> {
         @Override
         public final K next() {
            return this.getNext().key;
         }
      }

      final class ValueIterator extends ExpiringMap.EntryTreeHashMap<K, V>.AbstractHashIterator implements Iterator<V> {
         @Override
         public final V next() {
            return this.getNext().value;
         }
      }
   }

   static class ExpiringEntry<K, V> implements Comparable<ExpiringMap.ExpiringEntry<K, V>> {
      final AtomicLong expirationNanos;
      final AtomicLong expectedExpiration;
      final AtomicReference<ExpirationPolicy> expirationPolicy;
      final K key;
      volatile Future<?> entryFuture;
      V value;
      volatile boolean scheduled;

      ExpiringEntry(K key, V value, AtomicReference<ExpirationPolicy> expirationPolicy, AtomicLong expirationNanos) {
         this.key = key;
         this.value = value;
         this.expirationPolicy = expirationPolicy;
         this.expirationNanos = expirationNanos;
         this.expectedExpiration = new AtomicLong();
         this.resetExpiration();
      }

      public int compareTo(ExpiringMap.ExpiringEntry<K, V> other) {
         if (this.key.equals(other.key)) {
            return 0;
         } else {
            return this.expectedExpiration.get() < other.expectedExpiration.get() ? -1 : 1;
         }
      }

      @Override
      public int hashCode() {
         int prime = 31;
         int result = 1;
         result = 31 * result + (this.key == null ? 0 : this.key.hashCode());
         return 31 * result + (this.value == null ? 0 : this.value.hashCode());
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else if (this.getClass() != obj.getClass()) {
            return false;
         } else {
            ExpiringMap.ExpiringEntry<?, ?> other = (ExpiringMap.ExpiringEntry<?, ?>)obj;
            if (!this.key.equals(other.key)) {
               return false;
            } else {
               if (this.value == null) {
                  if (other.value != null) {
                     return false;
                  }
               } else if (!this.value.equals(other.value)) {
                  return false;
               }

               return true;
            }
         }
      }

      @Override
      public String toString() {
         return this.value.toString();
      }

      synchronized boolean cancel() {
         boolean result = this.scheduled;
         if (this.entryFuture != null) {
            this.entryFuture.cancel(false);
         }

         this.entryFuture = null;
         this.scheduled = false;
         return result;
      }

      synchronized V getValue() {
         return this.value;
      }

      void resetExpiration() {
         this.expectedExpiration.set(this.expirationNanos.get() + System.nanoTime());
      }

      synchronized void schedule(Future<?> entryFuture) {
         this.entryFuture = entryFuture;
         this.scheduled = true;
      }

      synchronized void setValue(V value) {
         this.value = value;
      }
   }
}
