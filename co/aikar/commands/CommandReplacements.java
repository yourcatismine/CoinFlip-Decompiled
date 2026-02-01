package co.aikar.commands;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

public class CommandReplacements {
   private final CommandManager manager;
   private final Map<String, Entry<Pattern, String>> replacements = new LinkedHashMap<>();

   CommandReplacements(CommandManager manager) {
      this.manager = manager;
      this.addReplacement0("truthy", "true|false|yes|no|1|0|on|off|t|f");
   }

   public void addReplacements(String... replacements) {
      if (replacements.length != 0 && replacements.length % 2 == 0) {
         for (int i = 0; i < replacements.length; i += 2) {
            this.addReplacement(replacements[i], replacements[i + 1]);
         }
      } else {
         throw new IllegalArgumentException("Must pass a number of arguments divisible by 2.");
      }
   }

   public String addReplacement(String key, String val) {
      return this.addReplacement0(key, val);
   }

   @Nullable
   private String addReplacement0(String key, String val) {
      key = ACFPatterns.PERCENTAGE.matcher(key.toLowerCase(Locale.ENGLISH)).replaceAll("");
      Pattern pattern = Pattern.compile("%\\{" + Pattern.quote(key) + "}|%" + Pattern.quote(key) + "\\b", 2);
      Entry<Pattern, String> entry = new SimpleImmutableEntry<>(pattern, val);
      Entry<Pattern, String> replaced = this.replacements.put(key, entry);
      return replaced != null ? replaced.getValue() : null;
   }

   public String replace(String text) {
      if (text == null) {
         return null;
      } else {
         for (Entry<Pattern, String> entry : this.replacements.values()) {
            text = entry.getKey().matcher(text).replaceAll(entry.getValue());
         }

         Matcher matcher = ACFPatterns.REPLACEMENT_PATTERN.matcher(text);

         while (matcher.find()) {
            this.manager.log(LogLevel.ERROR, "Found unregistered replacement: " + matcher.group());
         }

         return text;
      }
   }
}
