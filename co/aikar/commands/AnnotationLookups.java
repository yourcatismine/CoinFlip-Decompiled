package co.aikar.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.regex.Pattern;

abstract class AnnotationLookups {
   boolean hasAnnotation(AnnotatedElement object, Class<? extends Annotation> annoClass) {
      return this.getAnnotationValue(object, annoClass, 0) != null;
   }

   boolean hasAnnotation(AnnotatedElement object, Class<? extends Annotation> annoClass, boolean allowEmpty) {
      return this.getAnnotationValue(object, annoClass, 0 | (allowEmpty ? 0 : 8)) != null;
   }

   String[] getAnnotationValues(AnnotatedElement object, Class<? extends Annotation> annoClass) {
      return this.getAnnotationValues(object, annoClass, ACFPatterns.PIPE, 1);
   }

   String[] getAnnotationValues(AnnotatedElement object, Class<? extends Annotation> annoClass, Pattern pattern) {
      return this.getAnnotationValues(object, annoClass, pattern, 1);
   }

   String[] getAnnotationValues(AnnotatedElement object, Class<? extends Annotation> annoClass, int options) {
      return this.getAnnotationValues(object, annoClass, ACFPatterns.PIPE, options);
   }

   String[] getAnnotationValues(AnnotatedElement object, Class<? extends Annotation> annoClass, Pattern pattern, int options) {
      String value = this.getAnnotationValue(object, annoClass, options);
      return value == null ? null : pattern.split(value);
   }

   String getAnnotationValue(AnnotatedElement object, Class<? extends Annotation> annoClass) {
      return this.getAnnotationValue(object, annoClass, 1);
   }

   abstract String getAnnotationValue(AnnotatedElement object, Class<? extends Annotation> annoClass, int options);

   <T extends Annotation> T getAnnotationFromClass(Class<?> clazz, Class<T> annoClass) {
      while (clazz != null && BaseCommand.class.isAssignableFrom(clazz)) {
         T annotation = clazz.getAnnotation(annoClass);
         if (annotation != null) {
            return annotation;
         }

         for (Class<?> superClass = clazz.getSuperclass();
            superClass != null && BaseCommand.class.isAssignableFrom(superClass);
            superClass = superClass.getSuperclass()
         ) {
            annotation = superClass.getAnnotation(annoClass);
            if (annotation != null) {
               return annotation;
            }
         }

         clazz = clazz.getEnclosingClass();
      }

      return null;
   }
}
