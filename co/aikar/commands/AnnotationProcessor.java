package co.aikar.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

@Deprecated
public interface AnnotationProcessor<T extends Annotation> {
   @Nullable
   default Set<Class<?>> getApplicableParameters() {
      return null;
   }

   default void onBaseCommandRegister(BaseCommand command, T annotation) {
   }

   default void onCommandRegistered(RegisteredCommand command, T annotation) {
   }

   default void onParameterRegistered(RegisteredCommand command, int parameterIndex, Parameter p, T annotation) {
   }

   default void onPreComand(CommandOperationContext context) {
   }

   default void onPostComand(CommandOperationContext context) {
   }

   default void onPreContextResolution(CommandExecutionContext context) {
   }

   default void onPostContextResolution(CommandExecutionContext context, Object resolvedValue) {
   }
}
