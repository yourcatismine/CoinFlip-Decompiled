package com.kstudio.ultracoinflip.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Scanner;
import java.util.function.Consumer;
import org.bukkit.plugin.java.JavaPlugin;

public class UpdateChecker {
   private final JavaPlugin plugin;
   private final int resourceId;

   public UpdateChecker(JavaPlugin plugin, int resourceId) {
      this.plugin = plugin;
      this.resourceId = resourceId;
   }

   public void getVersion(Consumer<String> consumer) {
      FoliaScheduler.runTaskAsynchronously(this.plugin, () -> {
         try {
            long timestamp = System.currentTimeMillis();
            URI uri = URI.create("https://api.spigotmc.org/legacy/update.php?resource=" + this.resourceId + "&_=" + timestamp);
            InputStream is = uri.toURL().openStream();

            try {
               Scanner scanner = new Scanner(is);

               try {
                  if (scanner.hasNext()) {
                     String version = scanner.next().trim();
                     consumer.accept(version);
                  }
               } catch (Throwable var11) {
                  try {
                     scanner.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }

                  throw var11;
               }

               scanner.close();
            } catch (Throwable var12) {
               if (is != null) {
                  try {
                     is.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (is != null) {
               is.close();
            }
         } catch (IOException var13) {
            this.plugin.getLogger().info("Unable to check for updates: " + var13.getMessage());
         }
      });
   }
}
