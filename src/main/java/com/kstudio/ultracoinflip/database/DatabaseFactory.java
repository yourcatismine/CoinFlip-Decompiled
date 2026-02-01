package com.kstudio.ultracoinflip.database;

import com.kstudio.ultracoinflip.KStudio;

public class DatabaseFactory {
   public static DatabaseManager createDatabaseManager(KStudio plugin) {
      String type = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
      switch (type) {
         case "MYSQL":
            return new MySQLDatabaseManager(plugin);
         case "SQLITE":
         default:
            return new SQLiteDatabaseManager(plugin);
      }
   }
}
