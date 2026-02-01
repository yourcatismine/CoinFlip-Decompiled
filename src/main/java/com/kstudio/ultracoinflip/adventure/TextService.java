package com.kstudio.ultracoinflip.adventure;

public class TextService {
   private final TextEngine engine;

   public TextService() {
      if (this.isModernPaper()) {
         this.engine = new AdventureTextEngine();
      } else {
         this.engine = new LegacyTextEngine();
      }
   }

   private boolean isModernPaper() {
      try {
         Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
         return true;
      } catch (Throwable var2) {
         return false;
      }
   }

   public TextEngine getEngine() {
      return this.engine;
   }

   public boolean supportsInteractive() {
      return this.engine.supportsInteractive();
   }
}
