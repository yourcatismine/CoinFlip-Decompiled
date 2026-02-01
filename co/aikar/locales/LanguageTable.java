package co.aikar.locales;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.jetbrains.annotations.NotNull;

public class LanguageTable {
   private final Locale locale;
   private final Map<MessageKey, String> messages = new HashMap<>();

   LanguageTable(Locale locale) {
      this.locale = locale;
   }

   public String addMessage(MessageKey key, String message) {
      return this.messages.put(key, message);
   }

   public String getMessage(MessageKey key) {
      return this.messages.get(key);
   }

   public void addMessages(@NotNull Map<MessageKey, String> messages) {
      this.messages.putAll(messages);
   }

   public Locale getLocale() {
      return this.locale;
   }

   public boolean addMessageBundle(String bundleName) {
      return this.addMessageBundle(this.getClass().getClassLoader(), bundleName);
   }

   public boolean addMessageBundle(ClassLoader classLoader, String bundleName) {
      try {
         return this.addResourceBundle(ResourceBundle.getBundle(bundleName, this.locale, classLoader, new UTF8Control()));
      } catch (MissingResourceException var4) {
         return false;
      }
   }

   public boolean addResourceBundle(ResourceBundle bundle) {
      for (String key : bundle.keySet()) {
         this.addMessage(MessageKey.of(key), bundle.getString(key));
      }

      return !bundle.keySet().isEmpty();
   }
}
