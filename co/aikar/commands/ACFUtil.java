package co.aikar.commands;

import co.aikar.commands.apachecommonslang.ApacheCommonsLangUtil;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public final class ACFUtil {
   public static final Random RANDOM = new Random();

   private ACFUtil() {
   }

   public static String padRight(String s, int n) {
      return String.format("%1$-" + n + "s", s);
   }

   public static String padLeft(String s, int n) {
      return String.format("%1$" + n + "s", s);
   }

   public static String formatNumber(Integer balance) {
      return NumberFormat.getInstance().format(balance);
   }

   public static <T extends Enum> T getEnumFromName(T[] types, String name) {
      return getEnumFromName(types, name, null);
   }

   public static <T extends Enum> T getEnumFromName(T[] types, String name, T def) {
      for (T type : types) {
         if (type.name().equalsIgnoreCase(name)) {
            return type;
         }
      }

      return def;
   }

   public static <T extends Enum> T getEnumFromOrdinal(T[] types, int ordinal) {
      for (T type : types) {
         if (type.ordinal() == ordinal) {
            return type;
         }
      }

      return null;
   }

   public static String ucfirst(String str) {
      return ApacheCommonsLangUtil.capitalizeFully(str);
   }

   public static Double parseDouble(String var) {
      return parseDouble(var, null);
   }

   public static Double parseDouble(String var, Double def) {
      if (var == null) {
         return def;
      } else {
         try {
            return Double.parseDouble(var);
         } catch (NumberFormatException var3) {
            return def;
         }
      }
   }

   public static Float parseFloat(String var) {
      return parseFloat(var, null);
   }

   public static Float parseFloat(String var, Float def) {
      if (var == null) {
         return def;
      } else {
         try {
            return Float.parseFloat(var);
         } catch (NumberFormatException var3) {
            return def;
         }
      }
   }

   public static Long parseLong(String var) {
      return parseLong(var, null);
   }

   public static Long parseLong(String var, Long def) {
      if (var == null) {
         return def;
      } else {
         try {
            return Long.parseLong(var);
         } catch (NumberFormatException var3) {
            return def;
         }
      }
   }

   public static Integer parseInt(String var) {
      return parseInt(var, null);
   }

   public static Integer parseInt(String var, Integer def) {
      if (var == null) {
         return def;
      } else {
         try {
            return Integer.parseInt(var);
         } catch (NumberFormatException var3) {
            return def;
         }
      }
   }

   public static boolean randBool() {
      return RANDOM.nextBoolean();
   }

   public static <T> T nullDefault(Object val, Object def) {
      return (T)(val != null ? val : def);
   }

   public static String join(Collection<String> args) {
      return ApacheCommonsLangUtil.join(args, " ");
   }

   public static String join(Collection<String> args, String sep) {
      return ApacheCommonsLangUtil.join(args, sep);
   }

   public static String join(String[] args) {
      return join(args, 0, ' ');
   }

   public static String join(String[] args, String sep) {
      return ApacheCommonsLangUtil.join(args, sep);
   }

   public static String join(String[] args, char sep) {
      return join(args, 0, sep);
   }

   public static String join(String[] args, int index) {
      return join(args, index, ' ');
   }

   public static String join(String[] args, int index, char sep) {
      return ApacheCommonsLangUtil.join((Object[])args, sep, index, args.length);
   }

   public static String simplifyString(String str) {
      return str == null ? null : ACFPatterns.NON_ALPHA_NUMERIC.matcher(str.toLowerCase(Locale.ENGLISH)).replaceAll("");
   }

   public static double round(double x, int scale) {
      try {
         return new BigDecimal(Double.toString(x)).setScale(scale, 4).doubleValue();
      } catch (NumberFormatException var4) {
         return Double.isInfinite(x) ? x : Double.NaN;
      }
   }

   public static int roundUp(int num, int multiple) {
      if (multiple == 0) {
         return num;
      } else {
         int remainder = num % multiple;
         return remainder == 0 ? num : num + multiple - remainder;
      }
   }

   public static String limit(String str, int limit) {
      return str.length() > limit ? str.substring(0, limit) : str;
   }

   public static String replace(String string, Pattern pattern, String repl) {
      return pattern.matcher(string).replaceAll(Matcher.quoteReplacement(repl));
   }

   public static String replacePattern(String string, Pattern pattern, String repl) {
      return pattern.matcher(string).replaceAll(repl);
   }

   public static String replace(String string, String pattern, String repl) {
      return replace(string, ACFPatterns.getPattern(Pattern.quote(pattern)), repl);
   }

   public static String replacePattern(String string, String pattern, String repl) {
      return replace(string, ACFPatterns.getPattern(pattern), repl);
   }

   public static String replacePatternMatch(String string, Pattern pattern, String repl) {
      return pattern.matcher(string).replaceAll(repl);
   }

   public static String replacePatternMatch(String string, String pattern, String repl) {
      return replacePatternMatch(string, ACFPatterns.getPattern(pattern), repl);
   }

   public static String replaceStrings(String string, String... replacements) {
      if (replacements.length >= 2 && replacements.length % 2 == 0) {
         for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            if (value == null) {
               value = "";
            }

            string = replace(string, key, value);
         }

         return string;
      } else {
         throw new IllegalArgumentException("Invalid Replacements");
      }
   }

   public static String replacePatterns(String string, String... replacements) {
      if (replacements.length >= 2 && replacements.length % 2 == 0) {
         for (int i = 0; i < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1];
            if (value == null) {
               value = "";
            }

            string = replacePattern(string, key, value);
         }

         return string;
      } else {
         throw new IllegalArgumentException("Invalid Replacements");
      }
   }

   public static String capitalize(String str, char[] delimiters) {
      return ApacheCommonsLangUtil.capitalize(str, delimiters);
   }

   private static boolean isDelimiter(char ch, char[] delimiters) {
      return ApacheCommonsLangUtil.isDelimiter(ch, delimiters);
   }

   public static <T> T random(List<T> arr) {
      return arr != null && !arr.isEmpty() ? arr.get(RANDOM.nextInt(arr.size())) : null;
   }

   public static <T> T random(T[] arr) {
      return arr != null && arr.length != 0 ? arr[RANDOM.nextInt(arr.length)] : null;
   }

   @Deprecated
   public static <T extends Enum<?>> T random(Class<? extends T> enm) {
      return random((T[])enm.getEnumConstants());
   }

   public static String normalize(String s) {
      return s == null ? null : ACFPatterns.NON_PRINTABLE_CHARACTERS.matcher(Normalizer.normalize(s, Form.NFD)).replaceAll("");
   }

   public static int indexOf(String arg, String[] split) {
      for (int i = 0; i < split.length; i++) {
         if (arg == null) {
            if (split[i] == null) {
               return i;
            }
         } else if (arg.equals(split[i])) {
            return i;
         }
      }

      return -1;
   }

   public static String capitalizeFirst(String name) {
      return capitalizeFirst(name, '_');
   }

   public static String capitalizeFirst(String name, char separator) {
      name = name.toLowerCase(Locale.ENGLISH);
      String[] split = name.split(Character.toString(separator));
      StringBuilder total = new StringBuilder(3);

      for (String s : split) {
         total.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1)).append(' ');
      }

      return total.toString().trim();
   }

   public static String ltrim(String s) {
      int i = 0;

      while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
         i++;
      }

      return s.substring(i);
   }

   public static String rtrim(String s) {
      int i = s.length() - 1;

      while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
         i--;
      }

      return s.substring(0, i + 1);
   }

   public static List<String> enumNames(Enum<?>[] values) {
      return Stream.of(values).map(Enum::name).collect(Collectors.toList());
   }

   public static List<String> enumNames(Class<? extends Enum<?>> cls) {
      return enumNames((Enum<?>[])cls.getEnumConstants());
   }

   public static String combine(String[] args) {
      return combine(args, 0);
   }

   public static String combine(String[] args, int start) {
      int size = 0;

      for (int i = start; i < args.length; i++) {
         size += args[i].length();
      }

      StringBuilder sb = new StringBuilder(size);

      for (int i = start; i < args.length; i++) {
         sb.append(args[i]);
      }

      return sb.toString();
   }

   @Nullable
   public static <E extends Enum<E>> E simpleMatch(Class<? extends Enum<?>> list, String item) {
      if (item == null) {
         return null;
      } else {
         item = simplifyString(item);

         for (Enum<?> s : list.getEnumConstants()) {
            String simple = simplifyString(s.name());
            if (item.equals(simple)) {
               return (E)s;
            }
         }

         return null;
      }
   }

   public static boolean isTruthy(String test) {
      switch (test) {
         case "t":
         case "true":
         case "on":
         case "y":
         case "yes":
         case "1":
            return true;
         default:
            return false;
      }
   }

   public static Number parseNumber(String num, boolean suffixes) {
      if (ACFPatterns.getPattern("^0x([0-9A-Fa-f]*)$").matcher(num).matches()) {
         return Long.parseLong(num.substring(2), 16);
      } else if (ACFPatterns.getPattern("^0b([01]*)$").matcher(num).matches()) {
         return Long.parseLong(num.substring(2), 2);
      } else {
         ACFUtil.ApplyModifierToNumber applyModifierToNumber = new ACFUtil.ApplyModifierToNumber(num, suffixes).invoke();
         num = applyModifierToNumber.getNum();
         double mod = applyModifierToNumber.getMod();
         return Double.parseDouble(num) * mod;
      }
   }

   public static BigDecimal parseBigNumber(String num, boolean suffixes) {
      ACFUtil.ApplyModifierToNumber applyModifierToNumber = new ACFUtil.ApplyModifierToNumber(num, suffixes).invoke();
      num = applyModifierToNumber.getNum();
      double mod = applyModifierToNumber.getMod();
      BigDecimal big = new BigDecimal(num);
      return mod == 1.0 ? big : big.multiply(new BigDecimal(mod));
   }

   public static <T> boolean hasIntersection(Collection<T> list1, Collection<T> list2) {
      for (T t : list1) {
         if (list2.contains(t)) {
            return true;
         }
      }

      return false;
   }

   public static <T> Collection<T> intersection(Collection<T> list1, Collection<T> list2) {
      List<T> list = new ArrayList<>();

      for (T t : list1) {
         if (list2.contains(t)) {
            list.add(t);
         }
      }

      return list;
   }

   public static int rand(int min, int max) {
      return min + RANDOM.nextInt(max - min + 1);
   }

   public static int rand(int min1, int max1, int min2, int max2) {
      return randBool() ? rand(min1, max1) : rand(min2, max2);
   }

   public static double rand(double min, double max) {
      return RANDOM.nextDouble() * (max - min) + min;
   }

   public static boolean isNumber(String str) {
      return ApacheCommonsLangUtil.isNumeric(str);
   }

   public static String intToRoman(int integer) {
      if (integer == 1) {
         return "I";
      } else if (integer == 2) {
         return "II";
      } else if (integer == 3) {
         return "III";
      } else if (integer == 4) {
         return "IV";
      } else if (integer == 5) {
         return "V";
      } else if (integer == 6) {
         return "VI";
      } else if (integer == 7) {
         return "VII";
      } else if (integer == 8) {
         return "VIII";
      } else if (integer == 9) {
         return "IX";
      } else {
         return integer == 10 ? "X" : null;
      }
   }

   public static boolean isInteger(String string) {
      return ACFPatterns.INTEGER.matcher(string).matches();
   }

   public static boolean isFloat(String string) {
      try {
         Float.parseFloat(string);
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   public static boolean isDouble(String string) {
      try {
         Double.parseDouble(string);
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   public static boolean isBetween(float num, double min, double max) {
      return num >= min && num <= max;
   }

   public static double precision(double x, int p) {
      double pow = Math.pow(10.0, p);
      return Math.round(x * pow) / pow;
   }

   public static void sneaky(Throwable t) {
      throw (RuntimeException)superSneaky(t);
   }

   private static <T extends Throwable> T superSneaky(Throwable t) throws T {
      throw t;
   }

   public static <T> List<T> preformOnImmutable(List<T> list, Consumer<List<T>> action) {
      try {
         action.accept(list);
      } catch (UnsupportedOperationException var3) {
         list = new ArrayList<>(list);
         action.accept(list);
      }

      return list;
   }

   public static <T> T getFirstElement(Iterable<T> iterable) {
      if (iterable == null) {
         return null;
      } else {
         Iterator<T> iterator = iterable.iterator();
         return iterator.hasNext() ? iterator.next() : null;
      }
   }

   private static class ApplyModifierToNumber {
      private String num;
      private boolean suffixes;
      private double mod;

      public ApplyModifierToNumber(String num, boolean suffixes) {
         this.num = num;
         this.suffixes = suffixes;
      }

      public String getNum() {
         return this.num;
      }

      public double getMod() {
         return this.mod;
      }

      public ACFUtil.ApplyModifierToNumber invoke() {
         this.mod = 1.0;
         if (this.suffixes) {
            switch (this.num.charAt(this.num.length() - 1)) {
               case 'K':
               case 'k':
                  this.mod = 1000.0;
                  this.num = this.num.substring(0, this.num.length() - 1);
                  break;
               case 'M':
               case 'm':
                  this.mod = 1000000.0;
                  this.num = this.num.substring(0, this.num.length() - 1);
            }
         }

         return this;
      }
   }
}
