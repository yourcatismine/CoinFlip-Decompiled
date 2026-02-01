package co.aikar.commands;

public class UnresolvedDependencyException extends RuntimeException {
   UnresolvedDependencyException(String message) {
      super(message);
   }
}
