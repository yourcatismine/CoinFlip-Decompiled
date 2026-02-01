package co.aikar.commands;

import co.aikar.locales.MessageKey;
import co.aikar.locales.MessageKeyProvider;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public interface CommandIssuer {
   <T> T getIssuer();

   CommandManager getManager();

   boolean isPlayer();

   default void sendMessage(String message) {
      this.getManager().sendMessage(this, MessageType.INFO, MessageKeys.INFO_MESSAGE, "{message}", message);
   }

   @NotNull
   UUID getUniqueId();

   boolean hasPermission(String permission);

   default void sendError(MessageKeyProvider key, String... replacements) {
      this.sendMessage(MessageType.ERROR, key.getMessageKey(), replacements);
   }

   default void sendSyntax(MessageKeyProvider key, String... replacements) {
      this.sendMessage(MessageType.SYNTAX, key.getMessageKey(), replacements);
   }

   default void sendInfo(MessageKeyProvider key, String... replacements) {
      this.sendMessage(MessageType.INFO, key.getMessageKey(), replacements);
   }

   default void sendError(MessageKey key, String... replacements) {
      this.sendMessage(MessageType.ERROR, key, replacements);
   }

   default void sendSyntax(MessageKey key, String... replacements) {
      this.sendMessage(MessageType.SYNTAX, key, replacements);
   }

   default void sendInfo(MessageKey key, String... replacements) {
      this.sendMessage(MessageType.INFO, key, replacements);
   }

   default void sendMessage(MessageType type, MessageKeyProvider key, String... replacements) {
      this.sendMessage(type, key.getMessageKey(), replacements);
   }

   default void sendMessage(MessageType type, MessageKey key, String... replacements) {
      this.getManager().sendMessage(this, type, key, replacements);
   }

   @Deprecated
   void sendMessageInternal(String message);
}
