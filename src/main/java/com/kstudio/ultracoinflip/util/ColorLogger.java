package com.kstudio.ultracoinflip.util;

import java.lang.reflect.Method;
import org.bukkit.plugin.Plugin;

public class ColorLogger {
   public static final String RESET = "\u001b[0m";
   public static final String BLACK = "\u001b[30m";
   public static final String RED = "\u001b[31m";
   public static final String GREEN = "\u001b[32m";
   public static final String YELLOW = "\u001b[33m";
   public static final String BLUE = "\u001b[34m";
   public static final String PURPLE = "\u001b[35m";
   public static final String CYAN = "\u001b[36m";
   public static final String WHITE = "\u001b[37m";
   public static final String BRIGHT_BLACK = "\u001b[90m";
   public static final String BRIGHT_RED = "\u001b[91m";
   public static final String BRIGHT_GREEN = "\u001b[92m";
   public static final String BRIGHT_YELLOW = "\u001b[93m";
   public static final String BRIGHT_BLUE = "\u001b[94m";
   public static final String BRIGHT_PURPLE = "\u001b[95m";
   public static final String BRIGHT_CYAN = "\u001b[96m";
   public static final String BRIGHT_WHITE = "\u001b[97m";
   public static final String BOLD = "\u001b[1m";
   public static final String UNDERLINE = "\u001b[4m";
   private final Plugin plugin;
   private final boolean colorSupported;

   public ColorLogger(Plugin plugin) {
      this.plugin = plugin;
      this.colorSupported = this.detectColorSupport();
   }

   private boolean detectColorSupport() {
      try {
         Class<?> appenderClass = Class.forName("net.minecrell.terminalconsole.TerminalConsoleAppender");
         Method method = appenderClass.getDeclaredMethod("isAnsiSupported");
         Object result = method.invoke(null);
         if (result instanceof Boolean && (Boolean)result) {
            return true;
         }
      } catch (Exception var4) {
      }

      String osName = System.getProperty("os.name", "").toLowerCase();
      return System.console() != null || osName.contains("win");
   }

   public boolean isColorSupported() {
      return this.colorSupported;
   }

   private String color(String text, String color) {
      return this.colorSupported ? color + text + "\u001b[0m" : text;
   }

   public void info(String message) {
      this.plugin.getLogger().info(message);
   }

   public void success(String message) {
      this.plugin.getLogger().info(this.color(message, "\u001b[92m"));
   }

   public void warning(String message) {
      this.plugin.getLogger().warning(this.color(message, "\u001b[93m"));
   }

   public void error(String message) {
      this.plugin.getLogger().severe(this.color(message, "\u001b[91m"));
   }

   public void highlight(String message) {
      this.plugin.getLogger().info(this.color(message, "\u001b[96m"));
   }

   public void subtitle(String message) {
      this.plugin.getLogger().info(this.color(message, "\u001b[95m"));
   }

   public void log(String message, String color) {
      this.plugin.getLogger().info(this.color(message, color));
   }

   public void banner(String... lines) {
      for (String line : lines) {
         this.plugin.getLogger().info(this.color(line, "\u001b[96m\u001b[1m"));
      }
   }

   public void separator() {
      this.plugin.getLogger().info(this.color("========================================", "\u001b[94m"));
   }

   public void separatorYellow() {
      this.plugin.getLogger().info(this.color("========================================", "\u001b[93m"));
   }

   public String green(String text) {
      return this.colorSupported ? "\u001b[32m" + text + "\u001b[0m" : text;
   }

   public String brightGreen(String text) {
      return this.colorSupported ? "\u001b[92m" + text + "\u001b[0m" : text;
   }

   public String red(String text) {
      return this.colorSupported ? "\u001b[31m" + text + "\u001b[0m" : text;
   }

   public String brightRed(String text) {
      return this.colorSupported ? "\u001b[91m" + text + "\u001b[0m" : text;
   }

   public String yellow(String text) {
      return this.colorSupported ? "\u001b[33m" + text + "\u001b[0m" : text;
   }

   public String brightYellow(String text) {
      return this.colorSupported ? "\u001b[93m" + text + "\u001b[0m" : text;
   }

   public String cyan(String text) {
      return this.colorSupported ? "\u001b[36m" + text + "\u001b[0m" : text;
   }

   public String brightCyan(String text) {
      return this.colorSupported ? "\u001b[96m" + text + "\u001b[0m" : text;
   }

   public String purple(String text) {
      return this.colorSupported ? "\u001b[35m" + text + "\u001b[0m" : text;
   }

   public String brightPurple(String text) {
      return this.colorSupported ? "\u001b[95m" + text + "\u001b[0m" : text;
   }

   public String gray(String text) {
      return this.colorSupported ? "\u001b[90m" + text + "\u001b[0m" : text;
   }

   public String white(String text) {
      return this.colorSupported ? "\u001b[37m" + text + "\u001b[0m" : text;
   }

   public String bold(String text) {
      return this.colorSupported ? "\u001b[1m" + text + "\u001b[0m" : text;
   }

   public String translateConsoleColors(String text) {
      if (text != null && !text.isEmpty()) {
         if (!this.colorSupported) {
            return stripColorCodes(text);
         } else {
            StringBuilder builder = new StringBuilder();
            char[] chars = text.toCharArray();
            boolean appliedFormatting = false;

            for (int i = 0; i < chars.length; i++) {
               char current = chars[i];
               if ((current == '&' || current == 167) && i + 1 < chars.length) {
                  char code = Character.toLowerCase(chars[++i]);
                  String ansi = this.getAnsiForCode(code);
                  if (ansi != null) {
                     builder.append(ansi);
                     appliedFormatting = true;
                  }
               } else {
                  builder.append(current);
               }
            }

            if (appliedFormatting) {
               builder.append("\u001b[0m");
            }

            return builder.toString();
         }
      } else {
         return "";
      }
   }

   private String getAnsiForCode(char code) {
      switch (code) {
         case '0':
            return "\u001b[30m";
         case '1':
            return "\u001b[34m";
         case '2':
            return "\u001b[32m";
         case '3':
            return "\u001b[36m";
         case '4':
            return "\u001b[31m";
         case '5':
            return "\u001b[35m";
         case '6':
            return "\u001b[33m";
         case '7':
            return "\u001b[37m";
         case '8':
            return "\u001b[90m";
         case '9':
            return "\u001b[94m";
         case ':':
         case ';':
         case '<':
         case '=':
         case '>':
         case '?':
         case '@':
         case 'A':
         case 'B':
         case 'C':
         case 'D':
         case 'E':
         case 'F':
         case 'G':
         case 'H':
         case 'I':
         case 'J':
         case 'K':
         case 'L':
         case 'M':
         case 'N':
         case 'O':
         case 'P':
         case 'Q':
         case 'R':
         case 'S':
         case 'T':
         case 'U':
         case 'V':
         case 'W':
         case 'X':
         case 'Y':
         case 'Z':
         case '[':
         case '\\':
         case ']':
         case '^':
         case '_':
         case '`':
         case 'g':
         case 'h':
         case 'i':
         case 'j':
         case 'k':
         case 'm':
         case 'o':
         case 'p':
         case 'q':
         default:
            return null;
         case 'a':
            return "\u001b[92m";
         case 'b':
            return "\u001b[96m";
         case 'c':
            return "\u001b[91m";
         case 'd':
            return "\u001b[95m";
         case 'e':
            return "\u001b[93m";
         case 'f':
            return "\u001b[97m";
         case 'l':
            return "\u001b[1m";
         case 'n':
            return "\u001b[4m";
         case 'r':
            return "\u001b[0m";
      }
   }

   public static String stripColorCodes(String text) {
      if (text != null && !text.isEmpty()) {
         text = text.replaceAll("(?i)&[0-9a-fk-or]", "");
         text = text.replaceAll("(?i)&#[0-9A-Fa-f]{6}", "");
         text = text.replaceAll("<[^>]+>", "");
         return text.trim();
      } else {
         return text;
      }
   }
}
