package co.aikar.commands.lib.expiringmap;

public interface EntryLoader<K, V> {
   V load(K var1);
}
