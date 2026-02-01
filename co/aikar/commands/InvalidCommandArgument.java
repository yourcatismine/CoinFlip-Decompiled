package co.aikar.commands;

import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;

public class InvalidCommandArgument extends RuntimeException {
   final boolean showSyntax;
   final MessageKey key;
   final String[] replacements;

   public InvalidCommandArgument() {
      this(null, true);
   }

   public InvalidCommandArgument(boolean showSyntax) {
      this(null, showSyntax);
   }

   public InvalidCommandArgument(MessageKeyProvider key, String... replacements) {
      this(key.getMessageKey(), replacements);
   }

   public InvalidCommandArgument(MessageKey key, String... replacements) {
      this(key, true, replacements);
   }

   public InvalidCommandArgument(MessageKeyProvider key, boolean showSyntax, String... replacements) {
      this(key.getMessageKey(), showSyntax, replacements);
   }

   public InvalidCommandArgument(MessageKey key, boolean showSyntax, String... replacements) {
      super(key.getKey(), null, false, false);
      this.showSyntax = showSyntax;
      this.key = key;
      this.replacements = replacements;
   }

   public InvalidCommandArgument(String message) {
      this(message, true);
   }

   public InvalidCommandArgument(String message, boolean showSyntax) {
      super(message, null, false, false);
      this.showSyntax = showSyntax;
      this.replacements = null;
      this.key = null;
   }
}
