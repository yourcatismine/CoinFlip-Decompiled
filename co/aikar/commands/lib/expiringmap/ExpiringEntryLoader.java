package co.aikar.commands.lib.expiringmap;

public interface ExpiringEntryLoader<K, V> {
   ExpiringValue<V> load(K var1);
}
