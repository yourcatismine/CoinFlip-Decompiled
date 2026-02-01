package co.aikar.locales;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class LocaleManager<T> {
   private final Function<T, Locale> localeMapper;
   private Locale defaultLocale;
   private final Map<Locale, LanguageTable> tables = new HashMap<>();

   LocaleManager(Function<T, Locale> localeMapper, Locale defaultLocale) {
      this.localeMapper = localeMapper;
      this.defaultLocale = defaultLocale;
   }

   public static <T> LocaleManager<T> create(@NotNull Function<T, Locale> localeMapper) {
      return new LocaleManager<>(localeMapper, Locale.ENGLISH);
   }

   public static <T> LocaleManager<T> create(@NotNull Function<T, Locale> localeMapper, Locale defaultLocale) {
      return new LocaleManager<>(localeMapper, defaultLocale);
   }

   public Locale getDefaultLocale() {
      return this.defaultLocale;
   }

   public Locale setDefaultLocale(Locale defaultLocale) {
      Locale previous = this.defaultLocale;
      this.defaultLocale = defaultLocale;
      return previous;
   }

   public boolean addMessageBundle(@NotNull String bundleName, @NotNull Locale... locales) {
      return this.addMessageBundle(this.getClass().getClassLoader(), bundleName, locales);
   }

   public boolean addMessageBundle(@NotNull ClassLoader classLoader, @NotNull String bundleName, @NotNull Locale... locales) {
      if (locales.length == 0) {
         locales = new Locale[]{this.defaultLocale};
      }

      boolean found = false;

      for (Locale locale : locales) {
         if (this.getTable(locale).addMessageBundle(classLoader, bundleName)) {
            found = true;
         }
      }

      return found;
   }

   public void addMessages(@NotNull Locale locale, @NotNull Map<MessageKey, String> messages) {
      this.getTable(locale).addMessages(messages);
   }

   public String addMessage(@NotNull Locale locale, @NotNull MessageKey key, @NotNull String message) {
      return this.getTable(locale).addMessage(key, message);
   }

   public String getMessage(T context, @NotNull MessageKey key) {
      Locale locale = this.localeMapper.apply(context);
      String message = this.getTable(locale).getMessage(key);
      if (message == null && !locale.getCountry().isEmpty()) {
         message = this.getTable(new Locale(locale.getLanguage())).getMessage(key);
      }

      if (message == null && !Objects.equals(locale, this.defaultLocale)) {
         message = this.getTable(this.defaultLocale).getMessage(key);
      }

      return message;
   }

   @NotNull
   public LanguageTable getTable(@NotNull Locale locale) {
      return this.tables.computeIfAbsent(locale, LanguageTable::new);
   }

   public boolean addResourceBundle(ResourceBundle bundle, Locale locale) {
      return this.getTable(locale).addResourceBundle(bundle);
   }
}
