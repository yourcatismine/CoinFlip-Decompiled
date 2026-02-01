package co.aikar.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public abstract class MessageFormatter<FT> {
   private final List<FT> colors = new ArrayList<>();

   @SafeVarargs
   public MessageFormatter(FT... colors) {
      this.colors.addAll(Arrays.asList(colors));
   }

   public FT setColor(int index, FT color) {
      if (index > 0) {
         index--;
      } else {
         index = 0;
      }

      if (this.colors.size() <= index) {
         int needed = index - this.colors.size();
         if (needed > 0) {
            this.colors.addAll(Collections.nCopies(needed, null));
         }

         this.colors.add(color);
         return null;
      } else {
         return this.colors.set(index, color);
      }
   }

   public FT getColor(int index) {
      if (index > 0) {
         index--;
      } else {
         index = 0;
      }

      FT color = this.colors.get(index);
      if (color == null) {
         color = this.getDefaultColor();
      }

      return color;
   }

   public FT getDefaultColor() {
      return this.getColor(1);
   }

   abstract String format(FT color, String message);

   public String format(int index, String message) {
      return this.format(this.getColor(index), message);
   }

   public String format(String message) {
      String def = this.format(1, "");
      Matcher matcher = ACFPatterns.FORMATTER.matcher(message);
      StringBuffer sb = new StringBuffer(message.length());

      while (matcher.find()) {
         Integer color = ACFUtil.parseInt(matcher.group("color"), 1);
         String msg = this.format(color.intValue(), matcher.group("msg")) + def;
         matcher.appendReplacement(sb, Matcher.quoteReplacement(msg));
      }

      matcher.appendTail(sb);
      return def + sb.toString();
   }
}
