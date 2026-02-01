package co.aikar.commands.lib.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DelegatingMap<K, V> extends Map<K, V> {
   Map<K, V> delegate(boolean var1);

   @Override
   default int size() {
      return this.delegate(true).size();
   }

   @Override
   default boolean isEmpty() {
      return this.delegate(true).isEmpty();
   }

   @Override
   default boolean containsKey(Object key) {
      return this.delegate(true).containsKey(key);
   }

   @Override
   default boolean containsValue(Object value) {
      return this.delegate(true).containsValue(value);
   }

   @Override
   default V get(Object key) {
      return this.delegate(true).get(key);
   }

   @Nullable
   @Override
   default V put(K key, V value) {
      return this.delegate(false).put(key, value);
   }

   @Override
   default V remove(Object key) {
      return this.delegate(false).remove(key);
   }

   @Override
   default void putAll(@NotNull Map<? extends K, ? extends V> m) {
      this.delegate(false).putAll(m);
   }

   @Override
   default void clear() {
      this.delegate(false).clear();
   }

   @NotNull
   @Override
   default Set<K> keySet() {
      return this.delegate(false).keySet();
   }

   @NotNull
   @Override
   default Collection<V> values() {
      return this.delegate(false).values();
   }

   @NotNull
   @Override
   default Set<Entry<K, V>> entrySet() {
      return this.delegate(false).entrySet();
   }

   @Override
   default V getOrDefault(Object key, V defaultValue) {
      return this.delegate(true).getOrDefault(key, defaultValue);
   }

   @Override
   default void forEach(BiConsumer<? super K, ? super V> action) {
      this.delegate(true).forEach(action);
   }

   @Override
   default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
      this.delegate(false).replaceAll(function);
   }

   @Nullable
   @Override
   default V putIfAbsent(K key, V value) {
      return this.delegate(false).putIfAbsent(key, value);
   }

   @Override
   default boolean remove(Object key, Object value) {
      return this.delegate(false).remove(key, value);
   }

   @Override
   default boolean replace(K key, V oldValue, V newValue) {
      return this.delegate(false).replace(key, oldValue, newValue);
   }

   @Nullable
   @Override
   default V replace(K key, V value) {
      return this.delegate(false).replace(key, value);
   }

   @Override
   default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
      return this.delegate(false).computeIfAbsent(key, mappingFunction);
   }

   @Override
   default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      return this.delegate(false).computeIfPresent(key, remappingFunction);
   }

   @Override
   default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
      return this.delegate(false).compute(key, remappingFunction);
   }

   @Override
   default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
      return this.delegate(false).merge(key, value, remappingFunction);
   }
}
