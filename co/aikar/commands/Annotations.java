package co.aikar.commands;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;

class Annotations<M extends CommandManager> extends AnnotationLookups {
   public static final int NOTHING = 0;
   public static final int REPLACEMENTS = 1;
   public static final int LOWERCASE = 2;
   public static final int UPPERCASE = 4;
   public static final int NO_EMPTY = 8;
   public static final int DEFAULT_EMPTY = 16;
   private final M manager;
   private final Map<Class<? extends Annotation>, Method> valueMethods = new IdentityHashMap<>();
   private final Map<Class<? extends Annotation>, Void> noValueAnnotations = new IdentityHashMap<>();

   Annotations(M manager) {
      this.manager = manager;
   }

   @Override
   String getAnnotationValue(AnnotatedElement object, Class<? extends Annotation> annoClass, int options) {
      Annotation annotation = getAnnotationRecursive(object, annoClass, new HashSet<>());
      if (annotation == null) {
         if (object instanceof Class) {
            annotation = getAnnotationFromParentClasses((Class<?>)object, annoClass);
         } else if (object instanceof Method) {
            annotation = getAnnotationFromParentMethods((Method)object, annoClass);
         } else if (object instanceof Parameter) {
            annotation = getAnnotationFromParentParameters((Parameter)object, annoClass);
         }
      }

      String value = null;
      if (annotation != null) {
         Method valueMethod = this.valueMethods.get(annoClass);
         if (this.noValueAnnotations.containsKey(annoClass)) {
            value = "";
         } else {
            try {
               if (valueMethod == null) {
                  valueMethod = annoClass.getMethod("value");
                  valueMethod.setAccessible(true);
                  this.valueMethods.put(annoClass, valueMethod);
               }

               value = (String)valueMethod.invoke(annotation);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException var8) {
               if (!(var8 instanceof NoSuchMethodException)) {
                  this.manager.log(LogLevel.ERROR, "Error getting annotation value", var8);
               }

               this.noValueAnnotations.put(annoClass, null);
               value = "";
            }
         }
      }

      if (value == null) {
         if (!hasOption(options, 16)) {
            return null;
         }

         value = "";
      }

      if (hasOption(options, 1)) {
         value = this.manager.getCommandReplacements().replace(value);
      }

      if (hasOption(options, 2)) {
         value = value.toLowerCase(this.manager.getLocales().getDefaultLocale());
      } else if (hasOption(options, 4)) {
         value = value.toUpperCase(this.manager.getLocales().getDefaultLocale());
      }

      if (value.isEmpty() && hasOption(options, 8)) {
         value = null;
      }

      return value;
   }

   private static Annotation getAnnotationFromParentClasses(Class<?> clazz, Class<? extends Annotation> annoClass) {
      for (Class<?> parent = clazz.getSuperclass();
         parent != null && !parent.equals(BaseCommand.class) && !parent.equals(Object.class);
         parent = parent.getSuperclass()
      ) {
         Annotation annotation = getAnnotationRecursive(parent, annoClass, new HashSet<>());
         if (annotation != null) {
            return annotation;
         }
      }

      return null;
   }

   private static Annotation getAnnotationFromParentMethods(Method method, Class<? extends Annotation> annoClass) {
      for (Class<?> clazz = method.getDeclaringClass().getSuperclass();
         clazz != null && !clazz.equals(BaseCommand.class) && !clazz.equals(Object.class);
         clazz = clazz.getSuperclass()
      ) {
         try {
            Method parentMethod = clazz.getDeclaredMethod(method.getName(), method.getParameterTypes());
            Annotation parentAnnotation = getAnnotationRecursive(parentMethod, annoClass, new HashSet<>());
            if (parentAnnotation != null) {
               return parentAnnotation;
            }
         } catch (NoSuchMethodException var5) {
            return null;
         }
      }

      return null;
   }

   private static Annotation getAnnotationFromParentParameters(Parameter parameter, Class<? extends Annotation> annoClass) {
      for (Class<?> clazz = parameter.getDeclaringExecutable().getDeclaringClass().getSuperclass();
         clazz != null && !clazz.equals(BaseCommand.class) && !clazz.equals(Object.class);
         clazz = clazz.getSuperclass()
      ) {
         try {
            Method parentMethod = clazz.getDeclaredMethod(parameter.getDeclaringExecutable().getName(), parameter.getDeclaringExecutable().getParameterTypes());
            Annotation parentAnnotation = Arrays.stream(parentMethod.getParameters())
               .filter(parentParameter -> parentParameter.getName().equals(parameter.getName()) && parentParameter.getType().equals(parameter.getType()))
               .findFirst()
               .map(parentParameter -> getAnnotationRecursive(parentParameter, annoClass, new HashSet<>()))
               .orElse(null);
            if (parentAnnotation != null) {
               return parentAnnotation;
            }
         } catch (NoSuchMethodException var5) {
            return null;
         }
      }

      return null;
   }

   private static Annotation getAnnotationRecursive(AnnotatedElement object, Class<? extends Annotation> annoClass, Collection<Annotation> checked) {
      if (object.isAnnotationPresent(annoClass)) {
         return object.getAnnotation(annoClass);
      } else {
         for (Annotation otherAnnotation : object.getDeclaredAnnotations()) {
            if (!otherAnnotation.annotationType().getPackage().getName().startsWith("java.")) {
               if (checked.contains(otherAnnotation)) {
                  return null;
               }

               checked.add(otherAnnotation);
               Annotation foundAnnotation = getAnnotationRecursive(otherAnnotation.annotationType(), annoClass, checked);
               if (foundAnnotation != null) {
                  return foundAnnotation;
               }
            }
         }

         return null;
      }
   }

   private static boolean hasOption(int options, int option) {
      return (options & option) == option;
   }
}
