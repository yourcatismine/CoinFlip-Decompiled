package co.aikar.commands;

import co.aikar.locales.LocaleManager;
import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import org.jetbrains.annotations.NotNull;

public class Locales {
   public static final Locale ENGLISH = Locale.ENGLISH;
   public static final Locale GERMAN = Locale.GERMAN;
   public static final Locale FRENCH = Locale.FRENCH;
   public static final Locale JAPANESE = Locale.JAPANESE;
   public static final Locale ITALIAN = Locale.ITALIAN;
   public static final Locale KOREAN = Locale.KOREAN;
   public static final Locale CHINESE = Locale.CHINESE;
   public static final Locale SIMPLIFIED_CHINESE = Locale.SIMPLIFIED_CHINESE;
   public static final Locale TRADITIONAL_CHINESE = Locale.TRADITIONAL_CHINESE;
   public static final Locale SPANISH = new Locale("es");
   public static final Locale DUTCH = new Locale("nl");
   public static final Locale DANISH = new Locale("da");
   public static final Locale CZECH = new Locale("cs");
   public static final Locale GREEK = new Locale("el");
   public static final Locale LATIN = new Locale("la");
   public static final Locale BULGARIAN = new Locale("bg");
   public static final Locale AFRIKAANS = new Locale("af");
   public static final Locale HINDI = new Locale("hi");
   public static final Locale HEBREW = new Locale("he");
   public static final Locale POLISH = new Locale("pl");
   public static final Locale PORTUGUESE = new Locale("pt");
   public static final Locale FINNISH = new Locale("fi");
   public static final Locale SWEDISH = new Locale("sv");
   public static final Locale RUSSIAN = new Locale("ru");
   public static final Locale ROMANIAN = new Locale("ro");
   public static final Locale VIETNAMESE = new Locale("vi");
   public static final Locale THAI = new Locale("th");
   public static final Locale TURKISH = new Locale("tr");
   public static final Locale UKRANIAN = new Locale("uk");
   public static final Locale ARABIC = new Locale("ar");
   public static final Locale WELSH = new Locale("cy");
   public static final Locale NORWEGIAN_BOKMAAL = new Locale("nb");
   public static final Locale NORWEGIAN_NYNORSK = new Locale("nn");
   public static final Locale HUNGARIAN = new Locale("hu");
   private final CommandManager manager;
   private final LocaleManager<CommandIssuer> localeManager;
   private final Map<ClassLoader, SetMultimap<String, Locale>> loadedBundles = new HashMap<>();
   private final List<ClassLoader> registeredClassLoaders = new ArrayList<>();

   public Locales(CommandManager manager) {
      this.manager = manager;
      this.localeManager = LocaleManager.create(manager::getIssuerLocale);
      this.addBundleClassLoader(this.getClass().getClassLoader());
   }

   public void loadLanguages() {
      this.addMessageBundles("acf-core");
   }

   public Locale getDefaultLocale() {
      return this.localeManager.getDefaultLocale();
   }

   public Locale setDefaultLocale(Locale locale) {
      return this.localeManager.setDefaultLocale(locale);
   }

   public void loadMissingBundles() {
      for (Locale locale : this.manager.getSupportedLanguages()) {
         for (SetMultimap<String, Locale> localeData : this.loadedBundles.values()) {
            for (String bundleName : new HashSet(localeData.keys())) {
               this.addMessageBundle(bundleName, locale);
            }
         }
      }
   }

   public void addMessageBundles(String... bundleNames) {
      for (String bundleName : bundleNames) {
         for (Locale locale : this.manager.getSupportedLanguages()) {
            this.addMessageBundle(bundleName, locale);
         }
      }
   }

   public boolean addMessageBundle(String bundleName, Locale locale) {
      boolean found = false;

      for (ClassLoader classLoader : this.registeredClassLoaders) {
         if (this.addMessageBundle(classLoader, bundleName, locale)) {
            found = true;
         }
      }

      return found;
   }

   public boolean addMessageBundle(ClassLoader classLoader, String bundleName, Locale locale) {
      SetMultimap<String, Locale> classLoadersLocales = this.loadedBundles.getOrDefault(classLoader, HashMultimap.create());
      if (!classLoadersLocales.containsEntry(bundleName, locale) && this.localeManager.addMessageBundle(classLoader, bundleName, locale)) {
         classLoadersLocales.put(bundleName, locale);
         this.loadedBundles.put(classLoader, classLoadersLocales);
         return true;
      } else {
         return false;
      }
   }

   public void addMessageStrings(Locale locale, @NotNull Map<String, String> messages) {
      Map<MessageKey, String> map = new HashMap<>(messages.size());
      messages.forEach((key, value) -> map.put(MessageKey.of(key), value));
      this.localeManager.addMessages(locale, map);
   }

   public void addMessages(Locale locale, @NotNull Map<? extends MessageKeyProvider, String> messages) {
      Map<MessageKey, String> messagesMap = new LinkedHashMap<>();

      for (Entry<? extends MessageKeyProvider, String> entry : messages.entrySet()) {
         messagesMap.put(entry.getKey().getMessageKey(), entry.getValue());
      }

      this.localeManager.addMessages(locale, messagesMap);
   }

   public String addMessage(Locale locale, MessageKeyProvider key, String message) {
      return this.localeManager.addMessage(locale, key.getMessageKey(), message);
   }

   public String getMessage(CommandIssuer issuer, MessageKeyProvider key) {
      MessageKey msgKey = key.getMessageKey();
      String message = this.localeManager.getMessage(issuer, msgKey);
      if (message == null) {
         this.manager.log(LogLevel.ERROR, "Missing Language Key: " + msgKey.getKey());
         message = "<MISSING_LANGUAGE_KEY:" + msgKey.getKey() + ">";
      }

      return message;
   }

   public String getOptionalMessage(CommandIssuer issuer, MessageKey key) {
      return issuer == null ? this.localeManager.getTable(this.getDefaultLocale()).getMessage(key) : this.localeManager.getMessage(issuer, key);
   }

   public String replaceI18NStrings(String message) {
      if (message == null) {
         return null;
      } else {
         Matcher matcher = ACFPatterns.I18N_STRING.matcher(message);
         if (!matcher.find()) {
            return message;
         } else {
            CommandIssuer issuer = CommandManager.getCurrentCommandIssuer();
            matcher.reset();
            StringBuffer sb = new StringBuffer(message.length());

            while (matcher.find()) {
               MessageKey key = MessageKey.of(matcher.group("key"));
               matcher.appendReplacement(sb, Matcher.quoteReplacement(this.getMessage(issuer, key)));
            }

            matcher.appendTail(sb);
            return sb.toString();
         }
      }
   }

   public boolean addBundleClassLoader(ClassLoader classLoader) {
      return !this.registeredClassLoaders.contains(classLoader) && this.registeredClassLoaders.add(classLoader);
   }
}
