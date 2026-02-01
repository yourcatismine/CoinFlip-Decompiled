package com.kstudio.ultracoinflip.database;

import com.kstudio.ultracoinflip.KStudio;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class HikariCPFactory {
   private static final int JAVA_VERSION = detectJavaVersion();
   private static final boolean IS_JAVA_8 = JAVA_VERSION == 8;
   private static final String HIKARICP_JAVA8_PACKAGE = "com.kstudio.ultracoinflip.libs.hikaricp.java8";
   private static final String HIKARI_CONFIG_CLASS = "HikariConfig";
   private static final String HIKARI_DATA_SOURCE_CLASS = "HikariDataSource";
   private static Class<?> hikariConfigClass;
   private static Class<?> hikariDataSourceClass;

   private static int detectJavaVersion() {
      String version = System.getProperty("java.version");
      if (version == null) {
         return 8;
      } else if (version.startsWith("1.")) {
         if (version.startsWith("1.8")) {
            return 8;
         } else {
            return version.startsWith("1.7") ? 7 : 8;
         }
      } else {
         try {
            int dotIndex = version.indexOf(46);
            return dotIndex > 0 ? Integer.parseInt(version.substring(0, dotIndex)) : Integer.parseInt(version);
         } catch (NumberFormatException var2) {
            return 8;
         }
      }
   }

   public static Object createHikariConfig() {
      try {
         Constructor<?> constructor = hikariConfigClass.getDeclaredConstructor();
         return constructor.newInstance();
      } catch (Exception var1) {
         throw new RuntimeException("Failed to create HikariConfig instance", var1);
      }
   }

   public static Object createHikariDataSource(Object config) {
      try {
         Constructor<?> constructor = hikariDataSourceClass.getConstructor(hikariConfigClass);
         return constructor.newInstance(config);
      } catch (Exception var2) {
         throw new RuntimeException("Failed to create HikariDataSource instance", var2);
      }
   }

   public static Class<?> getHikariConfigClass() {
      return hikariConfigClass;
   }

   public static Class<?> getHikariDataSourceClass() {
      return hikariDataSourceClass;
   }

   public static boolean isJava8() {
      return IS_JAVA_8;
   }

   public static int getJavaVersion() {
      return JAVA_VERSION;
   }

   public static void setConfigProperty(Object config, String propertyName, Object value) {
      try {
         String setterName = "set" + capitalize(propertyName);
         Method setter = findSetterMethod(hikariConfigClass, setterName, value.getClass());
         if (setter != null) {
            setter.invoke(config, value);
         } else {
            if (value instanceof Integer) {
               setter = findSetterMethod(hikariConfigClass, setterName, int.class);
               if (setter != null) {
                  setter.invoke(config, value);
                  return;
               }
            } else if (value instanceof Long) {
               setter = findSetterMethod(hikariConfigClass, setterName, long.class);
               if (setter != null) {
                  setter.invoke(config, value);
                  return;
               }
            } else if (value instanceof Boolean) {
               setter = findSetterMethod(hikariConfigClass, setterName, boolean.class);
               if (setter != null) {
                  setter.invoke(config, value);
                  return;
               }
            }

            throw new NoSuchMethodException("Setter not found: " + setterName + " for type " + value.getClass());
         }
      } catch (Exception var5) {
         throw new RuntimeException("Failed to set HikariConfig property: " + propertyName, var5);
      }
   }

   public static Object getDataSourceProperty(Object dataSource, String propertyName) {
      try {
         String getterName = "get" + capitalize(propertyName);
         Method getter = hikariDataSourceClass.getMethod(getterName);
         return getter.invoke(dataSource);
      } catch (Exception var4) {
         throw new RuntimeException("Failed to get HikariDataSource property: " + propertyName, var4);
      }
   }

   public static void closeDataSource(Object dataSource) {
      try {
         Method closeMethod = hikariDataSourceClass.getMethod("close");
         closeMethod.invoke(dataSource);
      } catch (Exception var2) {
         throw new RuntimeException("Failed to close HikariDataSource", var2);
      }
   }

   public static boolean isDataSourceClosed(Object dataSource) {
      try {
         Method isClosedMethod = hikariDataSourceClass.getMethod("isClosed");
         return (Boolean)isClosedMethod.invoke(dataSource);
      } catch (Exception var2) {
         return false;
      }
   }

   private static String capitalize(String str) {
      return str != null && !str.isEmpty() ? str.substring(0, 1).toUpperCase() + str.substring(1) : str;
   }

   private static Method findSetterMethod(Class<?> clazz, String methodName, Class<?> paramType) {
      try {
         return clazz.getMethod(methodName, paramType);
      } catch (NoSuchMethodException var9) {
         for (Method method : clazz.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
               Class<?> methodParamType = method.getParameterTypes()[0];
               if (methodParamType.isAssignableFrom(paramType)
                  || paramType.isPrimitive() && isCompatiblePrimitive(paramType, methodParamType)
                  || methodParamType.isPrimitive() && isCompatiblePrimitive(methodParamType, paramType)) {
                  return method;
               }
            }
         }

         return null;
      }
   }

   private static boolean isCompatiblePrimitive(Class<?> from, Class<?> to) {
      if (from == int.class && to == Integer.class) {
         return true;
      } else if (from == long.class && to == Long.class) {
         return true;
      } else if (from == boolean.class && to == Boolean.class) {
         return true;
      } else if (from == Integer.class && to == int.class) {
         return true;
      } else {
         return from == Long.class && to == long.class ? true : from == Boolean.class && to == boolean.class;
      }
   }

   static {
      String loadedVersion = null;
      String loadedSource = null;

      try {
         hikariConfigClass = Class.forName("com.kstudio.ultracoinflip.libs.hikaricp.java8.HikariConfig");
         hikariDataSourceClass = Class.forName("com.kstudio.ultracoinflip.libs.hikaricp.java8.HikariDataSource");

         try {
            hikariDataSourceClass.getMethod("getMetrics");
            loadedVersion = "5.1.0";
         } catch (NoSuchMethodException var5) {
            loadedVersion = "3.4.5";
         }

         loadedSource = "standard package (from plugin.yml libraries)";
      } catch (ClassNotFoundException var6) {
         try {
            hikariConfigClass = Class.forName("com.kstudio.ultracoinflip.libs.hikaricp.java8.HikariConfig");
            hikariDataSourceClass = Class.forName("com.kstudio.ultracoinflip.libs.hikaricp.java8.HikariDataSource");
            loadedVersion = "3.4.5";
            loadedSource = "shaded package (bundled)";
         } catch (ClassNotFoundException var4) {
            throw new RuntimeException(
               "Failed to load HikariCP. Neither standard nor shaded package found. Java version: "
                  + JAVA_VERSION
                  + ". Please ensure HikariCP is available in plugin.yml libraries or bundled in JAR.",
               var4
            );
         }
      }

      if (KStudio.getInstance() != null) {
         KStudio.getInstance().getLogger().info(String.format("HikariCP %s loaded from %s (Java %d)", loadedVersion, loadedSource, JAVA_VERSION));
         if (!IS_JAVA_8 && "3.4.5".equals(loadedVersion) && "shaded package (bundled)".equals(loadedSource)) {
            KStudio.getInstance().getLogger().info("Tip: For better performance on Java 11+, add HikariCP 5.1.0 to plugin.yml libraries");
         }
      }
   }
}
