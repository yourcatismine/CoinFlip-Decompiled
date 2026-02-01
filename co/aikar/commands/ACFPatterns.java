package co.aikar.commands;

import co.aikar.commands.lib.expiringmap.ExpirationPolicy;
import co.aikar.commands.lib.expiringmap.ExpiringMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

final class ACFPatterns {
   public static final Pattern COMMA = Pattern.compile(",");
   public static final Pattern PERCENTAGE = Pattern.compile("%", 16);
   public static final Pattern NEWLINE = Pattern.compile("\n");
   public static final Pattern DASH = Pattern.compile("-");
   public static final Pattern UNDERSCORE = Pattern.compile("_");
   public static final Pattern SPACE = Pattern.compile(" ");
   public static final Pattern SEMICOLON = Pattern.compile(";");
   public static final Pattern COLON = Pattern.compile(":");
   public static final Pattern COLONEQUALS = Pattern.compile("([:=])");
   public static final Pattern PIPE = Pattern.compile("\\|");
   public static final Pattern NON_ALPHA_NUMERIC = Pattern.compile("[^a-zA-Z0-9]");
   public static final Pattern INTEGER = Pattern.compile("^[0-9]+$");
   public static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_$]{1,16}$");
   public static final Pattern NON_PRINTABLE_CHARACTERS = Pattern.compile("[^\\x20-\\x7F]");
   public static final Pattern EQUALS = Pattern.compile("=");
   public static final Pattern FORMATTER = Pattern.compile("<c(?<color>\\d+)>(?<msg>.*?)</c\\1>", 2);
   public static final Pattern I18N_STRING = Pattern.compile("\\{@@(?<key>.+?)}", 2);
   public static final Pattern REPLACEMENT_PATTERN = Pattern.compile("%\\{.[^\\s]*}");
   static final Map<String, Pattern> patternCache = ExpiringMap.builder()
      .maxSize(200)
      .expiration(1L, TimeUnit.HOURS)
      .expirationPolicy(ExpirationPolicy.ACCESSED)
      .build();

   private ACFPatterns() {
   }

   public static Pattern getPattern(String pattern) {
      return patternCache.computeIfAbsent(pattern, s -> Pattern.compile(pattern));
   }
}
