package co.aikar.commands.lib.expiringmap;

public interface ExpirationListener<K, V> {
   void expired(K var1, V var2);
}
