package co.aikar.commands;

public class InvalidCommandContextException extends RuntimeException {
   InvalidCommandContextException(String message) {
      super(message);
   }
}
