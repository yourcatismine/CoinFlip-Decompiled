package co.aikar.commands;

import java.lang.annotation.Annotation;
import java.util.List;

public class CommandOperationContext<I extends CommandIssuer> {
   private final CommandManager manager;
   private final I issuer;
   private final BaseCommand command;
   private final String commandLabel;
   private final String[] args;
   private final boolean isAsync;
   private RegisteredCommand registeredCommand;
   List<String> enumCompletionValues;

   CommandOperationContext(CommandManager manager, I issuer, BaseCommand command, String commandLabel, String[] args, boolean isAsync) {
      this.manager = manager;
      this.issuer = issuer;
      this.command = command;
      this.commandLabel = commandLabel;
      this.args = args;
      this.isAsync = isAsync;
   }

   public CommandManager getCommandManager() {
      return this.manager;
   }

   public I getCommandIssuer() {
      return this.issuer;
   }

   public BaseCommand getCommand() {
      return this.command;
   }

   public String getCommandLabel() {
      return this.commandLabel;
   }

   public String[] getArgs() {
      return this.args;
   }

   public boolean isAsync() {
      return this.isAsync;
   }

   public void setRegisteredCommand(RegisteredCommand registeredCommand) {
      this.registeredCommand = registeredCommand;
   }

   public RegisteredCommand getRegisteredCommand() {
      return this.registeredCommand;
   }

   @Deprecated
   public <T extends Annotation> T getAnnotation(Class<T> anno) {
      return this.registeredCommand.method.getAnnotation(anno);
   }

   public <T extends Annotation> String getAnnotationValue(Class<T> cls) {
      return this.manager.getAnnotations().getAnnotationValue(this.registeredCommand.method, cls);
   }

   public <T extends Annotation> String getAnnotationValue(Class<T> cls, int options) {
      return this.manager.getAnnotations().getAnnotationValue(this.registeredCommand.method, cls, options);
   }

   public boolean hasAnnotation(Class<? extends Annotation> anno) {
      return this.getAnnotation(anno) != null;
   }
}
