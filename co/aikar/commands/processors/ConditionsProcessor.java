package co.aikar.commands.processors;

import co.aikar.commands.AnnotationProcessor;
import co.aikar.commands.CommandExecutionContext;
import co.aikar.commands.CommandOperationContext;
import co.aikar.commands.annotation.Conditions;

@Deprecated
public class ConditionsProcessor implements AnnotationProcessor<Conditions> {
   @Override
   public void onPreComand(CommandOperationContext context) {
   }

   @Override
   public void onPostContextResolution(CommandExecutionContext context, Object resolvedValue) {
   }
}
