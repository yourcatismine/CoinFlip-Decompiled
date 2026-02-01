package co.aikar.commands;

import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;

public class ConditionFailedException extends InvalidCommandArgument {
   public ConditionFailedException() {
      super(false);
   }

   public ConditionFailedException(MessageKeyProvider key, String... replacements) {
      super(key, false, replacements);
   }

   public ConditionFailedException(MessageKey key, String... replacements) {
      super(key, false, replacements);
   }

   public ConditionFailedException(String message) {
      super(message, false);
   }
}
