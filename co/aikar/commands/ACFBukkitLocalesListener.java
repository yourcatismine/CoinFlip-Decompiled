package co.aikar.commands;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.Locale;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLocaleChangeEvent;

class ACFBukkitLocalesListener implements Listener {
   private final BukkitCommandManager manager;
   private MethodHandle localeMethod1_8 = null;
   private boolean checkedLocaleMethod1_8 = false;

   ACFBukkitLocalesListener(BukkitCommandManager manager) {
      this.manager = manager;
   }

   @EventHandler
   void onLocaleChange(PlayerLocaleChangeEvent event) {
      if (this.manager.autoDetectFromClient) {
         Player player = event.getPlayer();
         Locale locale = null;

         try {
            locale = event.locale();
         } catch (NoSuchMethodError var10) {
            try {
               if (!event.getLocale().equals(this.manager.issuersLocaleString.get(player.getUniqueId()))) {
                  locale = ACFBukkitUtil.stringToLocale(event.getLocale());
               }
            } catch (NoSuchMethodError var9) {
               try {
                  if (!this.checkedLocaleMethod1_8) {
                     this.checkedLocaleMethod1_8 = true;
                     Lookup lookup = MethodHandles.lookup();
                     MethodType type = MethodType.methodType(String.class);
                     this.localeMethod1_8 = lookup.findVirtual(PlayerLocaleChangeEvent.class, "getNewLocale", type);
                  }

                  if (this.localeMethod1_8 != null) {
                     String value = (String)this.localeMethod1_8.invoke((PlayerLocaleChangeEvent)event);
                     if (!value.equals(this.manager.issuersLocaleString.get(player.getUniqueId()))) {
                        locale = ACFBukkitUtil.stringToLocale(value);
                     }
                  }
               } catch (Throwable var8) {
                  this.manager.log(LogLevel.ERROR, "Error registering MethodHandle for LocaleChangeEvent", var8);
               }
            }
         }

         if (locale != null) {
            this.manager.setPlayerLocale(player, locale);
         }
      }
   }
}
