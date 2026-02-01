package co.aikar.commands.lib.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.jetbrains.annotations.Nullable;

public class Table<R, C, V> implements Iterable<Table.Entry<R, C, V>> {
   private final Map<R, Map<C, V>> rowMap;
   private final Function<R, Map<C, V>> colMapSupplier;

   public Table() {
      this(new HashMap<>(), HashMap::new);
   }

   public Table(Supplier<Map<C, V>> columnMapSupplier) {
      this(new HashMap<>(), columnMapSupplier);
   }

   public Table(Map<R, Map<C, V>> backingRowMap, Supplier<Map<C, V>> columnMapSupplier) {
      this(backingRowMap, r -> columnMapSupplier.get());
   }

   public Table(Map<R, Map<C, V>> backingRowMap, Function<R, Map<C, V>> columnMapSupplier) {
      this.rowMap = backingRowMap;
      this.colMapSupplier = columnMapSupplier;
   }

   public V get(R row, C col) {
      return this.getIfExists(row, col);
   }

   public V getOrDefault(R row, C col, V def) {
      Map<C, V> colMap = this.getColMapIfExists(row);
      if (colMap == null) {
         return def;
      } else {
         V v = colMap.get(col);
         return v == null && !colMap.containsKey(col) ? def : v;
      }
   }

   public boolean containsKey(R row, C col) {
      Map<C, V> colMap = this.getColMapIfExists(row);
      return colMap == null ? false : colMap.containsKey(col);
   }

   @Nullable
   public V put(R row, C col, V val) {
      return this.getColMapForWrite(row).put(col, val);
   }

   public void forEach(Table.TableConsumer<R, C, V> consumer) {
      for (Table.Entry<R, C, V> entry : this) {
         consumer.accept(entry.getRow(), entry.getCol(), entry.getValue());
      }
   }

   public void forEach(Table.TablePredicate<R, C, V> predicate) {
      for (Table.Entry<R, C, V> entry : this) {
         if (!predicate.test(entry.getRow(), entry.getCol(), entry.getValue())) {
            return;
         }
      }
   }

   public void removeIf(Table.TablePredicate<R, C, V> predicate) {
      Iterator<Table.Entry<R, C, V>> it = this.iterator();

      while (it.hasNext()) {
         Table.Entry<R, C, V> entry = it.next();
         if (predicate.test(entry.getRow(), entry.getCol(), entry.getValue())) {
            it.remove();
         }
      }
   }

   public Stream<Table.Entry<R, C, V>> stream() {
      return this.stream(false);
   }

   public Stream<Table.Entry<R, C, V>> stream(boolean parallel) {
      return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this.iterator(), 0), parallel);
   }

   @Override
   public Iterator<Table.Entry<R, C, V>> iterator() {
      return new Iterator<Table.Entry<R, C, V>>() {
         Iterator<Map.Entry<R, Map<C, V>>> rowIter = Table.this.rowMap.entrySet().iterator();
         Iterator<Map.Entry<C, V>> colIter = null;
         private Map.Entry<R, Map<C, V>> rowEntry;
         private Map.Entry<C, V> colEntry;
         private Table.Entry<R, C, V> next = this.getNext();

         private Table.Entry<R, C, V> getNext() {
            if (this.colIter == null || !this.colIter.hasNext()) {
               if (!this.rowIter.hasNext()) {
                  return null;
               }

               this.rowEntry = this.rowIter.next();
               this.colIter = this.rowEntry.getValue().entrySet().iterator();
            }

            if (!this.colIter.hasNext()) {
               return null;
            } else {
               this.colEntry = this.colIter.next();
               return Table.this.new Node(this.rowEntry, this.colEntry);
            }
         }

         @Override
         public boolean hasNext() {
            return this.next != null;
         }

         public Table.Entry<R, C, V> next() {
            Table.Entry<R, C, V> entry = this.next;
            this.next = this.getNext();
            return entry;
         }

         @Override
         public void remove() {
            this.colIter.remove();
         }
      };
   }

   public void replaceAll(Table.TableFunction<R, C, V, V> function) {
      for (Table.Entry<R, C, V> entry : this) {
         entry.setValue(function.compose(entry.getRow(), entry.getCol(), entry.getValue()));
      }
   }

   public V remove(R row, C col) {
      Map<C, V> rowMap = this.rowMap.get(row);
      return rowMap == null ? null : rowMap.remove(col);
   }

   @Nullable
   public V replace(R row, C col, V val) {
      Map<C, V> rowMap = this.getColMapIfExists(row);
      if (rowMap == null) {
         return null;
      } else {
         return rowMap.get(col) == null && !rowMap.containsKey(col) ? null : rowMap.put(col, val);
      }
   }

   @Nullable
   public boolean replace(R row, C col, V old, V val) {
      Map<C, V> rowMap = this.getColMapIfExists(row);
      if (rowMap == null) {
         return false;
      } else if (Objects.equals(rowMap.get(col), old)) {
         rowMap.put(col, val);
         return true;
      } else {
         return false;
      }
   }

   public V computeIfAbsent(R row, C col, BiFunction<R, C, V> function) {
      return this.getColMapForWrite(row).computeIfAbsent(col, c -> function.apply(row, col));
   }

   public V computeIfPresent(R row, C col, Table.TableFunction<R, C, V, V> function) {
      Map<C, V> colMap = this.getColMapForWrite(row);
      V v = colMap.computeIfPresent(col, (c, old) -> function.compose(row, col, (V)old));
      this.removeIfEmpty(row, colMap);
      return v;
   }

   public V compute(R row, C col, Table.TableFunction<R, C, V, V> function) {
      Map<C, V> colMap = this.getColMapForWrite(row);
      V v = colMap.compute(col, (c, old) -> function.compose(row, col, (V)old));
      this.removeIfEmpty(row, colMap);
      return v;
   }

   public V merge(R row, C col, V val, Table.TableFunction<R, C, V, V> function) {
      Map<C, V> colMap = this.getColMapForWrite(row);
      V v = colMap.merge(col, val, (c, old) -> function.compose(row, col, (V)old));
      this.removeIfEmpty(row, colMap);
      return v;
   }

   public Map<C, V> row(final R row) {
      final Map<C, V> EMPTY = new HashMap<>(0);
      return new DelegatingMap<C, V>() {
         @Override
         public Map<C, V> delegate(boolean readOnly) {
            return readOnly ? Table.this.rowMap.getOrDefault(row, EMPTY) : Table.this.getColMapForWrite(row);
         }

         @Override
         public V remove(Object key) {
            Map<C, V> delegate = this.delegate(false);
            V remove = delegate.remove(key);
            Table.this.removeIfEmpty(row, delegate);
            return remove;
         }
      };
   }

   private V getIfExists(R row, C col) {
      Map<C, V> colMap = this.getColMapIfExists(row);
      return colMap == null ? null : colMap.get(col);
   }

   private Map<C, V> getColMapIfExists(R row) {
      Map<C, V> colMap = this.rowMap.get(row);
      if (colMap != null && colMap.isEmpty()) {
         this.rowMap.remove(row);
         colMap = null;
      }

      return colMap;
   }

   private Map<C, V> getColMapForWrite(R row) {
      return this.rowMap.computeIfAbsent(row, this.colMapSupplier);
   }

   private void removeIfEmpty(R row, Map<C, V> colMap) {
      if (colMap.isEmpty()) {
         this.rowMap.remove(row);
      }
   }

   public interface Entry<R, C, V> {
      R getRow();

      C getCol();

      V getValue();

      V setValue(V var1);
   }

   private class Node implements Table.Entry<R, C, V> {
      private final Map.Entry<R, Map<C, V>> rowEntry;
      private final Map.Entry<C, V> colEntry;

      Node(Map.Entry<R, Map<C, V>> rowEntry, Map.Entry<C, V> entry) {
         this.rowEntry = rowEntry;
         this.colEntry = entry;
      }

      @Override
      public final R getRow() {
         return this.rowEntry.getKey();
      }

      @Override
      public final C getCol() {
         return this.colEntry.getKey();
      }

      @Override
      public final V getValue() {
         return this.colEntry.getValue();
      }

      @Override
      public final V setValue(V value) {
         return this.colEntry.setValue(value);
      }
   }

   public interface TableConsumer<R, C, V> {
      void accept(R var1, C var2, V var3);
   }

   public interface TableFunction<R, C, V, RETURN> {
      RETURN compose(R var1, C var2, V var3);
   }

   public interface TablePredicate<R, C, V> {
      boolean test(R var1, C var2, V var3);
   }
}
