package co.aikar.commands.apachecommonslang;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class ApacheCommonsExceptionUtil {
   private static final String LINE_SEPARATOR = System.getProperty("line.separator");
   static final String WRAPPED_MARKER = " [wrapped] ";
   private static String[] CAUSE_METHOD_NAMES = new String[]{
      "getCause",
      "getNextException",
      "getTargetException",
      "getException",
      "getSourceException",
      "getRootCause",
      "getCausedByException",
      "getNested",
      "getLinkedException",
      "getNestedException",
      "getLinkedCause",
      "getThrowable"
   };
   private static final Method THROWABLE_CAUSE_METHOD;
   private static final Method THROWABLE_INITCAUSE_METHOD;

   public static void addCauseMethodName(String methodName) {
      if (methodName != null && !methodName.isEmpty() && !isCauseMethodName(methodName)) {
         List list = getCauseMethodNameList();
         if (list.add(methodName)) {
            CAUSE_METHOD_NAMES = toArray(list);
         }
      }
   }

   public static void removeCauseMethodName(String methodName) {
      if (methodName != null && !methodName.isEmpty()) {
         List list = getCauseMethodNameList();
         if (list.remove(methodName)) {
            CAUSE_METHOD_NAMES = toArray(list);
         }
      }
   }

   public static boolean setCause(Throwable target, Throwable cause) {
      if (target == null) {
         throw new IllegalArgumentException("target");
      } else {
         Object[] causeArgs = new Object[]{cause};
         boolean modifiedTarget = false;
         if (THROWABLE_INITCAUSE_METHOD != null) {
            try {
               THROWABLE_INITCAUSE_METHOD.invoke(target, causeArgs);
               modifiedTarget = true;
            } catch (IllegalAccessException var8) {
            } catch (InvocationTargetException var9) {
            }
         }

         try {
            Method setCauseMethod = target.getClass().getMethod("setCause", Throwable.class);
            setCauseMethod.invoke(target, causeArgs);
            modifiedTarget = true;
         } catch (NoSuchMethodException var5) {
         } catch (IllegalAccessException var6) {
         } catch (InvocationTargetException var7) {
         }

         return modifiedTarget;
      }
   }

   private static String[] toArray(List list) {
      return list.toArray(new String[list.size()]);
   }

   private static ArrayList getCauseMethodNameList() {
      return new ArrayList<>(Arrays.asList(CAUSE_METHOD_NAMES));
   }

   public static boolean isCauseMethodName(String methodName) {
      return ApacheCommonsLangUtil.indexOf(CAUSE_METHOD_NAMES, methodName) >= 0;
   }

   public static Throwable getCause(Throwable throwable) {
      return getCause(throwable, CAUSE_METHOD_NAMES);
   }

   public static Throwable getCause(Throwable throwable, String[] methodNames) {
      if (throwable == null) {
         return null;
      } else {
         Throwable cause = getCauseUsingWellKnownTypes(throwable);
         if (cause == null) {
            if (methodNames == null) {
               methodNames = CAUSE_METHOD_NAMES;
            }

            for (int i = 0; i < methodNames.length; i++) {
               String methodName = methodNames[i];
               if (methodName != null) {
                  cause = getCauseUsingMethodName(throwable, methodName);
                  if (cause != null) {
                     break;
                  }
               }
            }

            if (cause == null) {
               cause = getCauseUsingFieldName(throwable, "detail");
            }
         }

         return cause;
      }
   }

   public static Throwable getRootCause(Throwable throwable) {
      List list = getThrowableList(throwable);
      return list.size() < 2 ? null : (Throwable)list.get(list.size() - 1);
   }

   private static Throwable getCauseUsingWellKnownTypes(Throwable throwable) {
      if (throwable instanceof ApacheCommonsExceptionUtil.Nestable) {
         return throwable.getCause();
      } else if (throwable instanceof SQLException) {
         return ((SQLException)throwable).getNextException();
      } else {
         return throwable instanceof InvocationTargetException ? ((InvocationTargetException)throwable).getTargetException() : null;
      }
   }

   private static Throwable getCauseUsingMethodName(Throwable throwable, String methodName) {
      Method method = null;

      try {
         method = throwable.getClass().getMethod(methodName, null);
      } catch (NoSuchMethodException var7) {
      } catch (SecurityException var8) {
      }

      if (method != null && Throwable.class.isAssignableFrom(method.getReturnType())) {
         try {
            return (Throwable)method.invoke(throwable);
         } catch (IllegalAccessException var4) {
         } catch (IllegalArgumentException var5) {
         } catch (InvocationTargetException var6) {
         }
      }

      return null;
   }

   private static Throwable getCauseUsingFieldName(Throwable throwable, String fieldName) {
      Field field = null;

      try {
         field = throwable.getClass().getField(fieldName);
      } catch (NoSuchFieldException var6) {
      } catch (SecurityException var7) {
      }

      if (field != null && Throwable.class.isAssignableFrom(field.getType())) {
         try {
            return (Throwable)field.get(throwable);
         } catch (IllegalAccessException var4) {
         } catch (IllegalArgumentException var5) {
         }
      }

      return null;
   }

   public static boolean isThrowableNested() {
      return THROWABLE_CAUSE_METHOD != null;
   }

   public static boolean isNestedThrowable(Throwable throwable) {
      if (throwable == null) {
         return false;
      } else if (throwable instanceof ApacheCommonsExceptionUtil.Nestable) {
         return true;
      } else if (throwable instanceof SQLException) {
         return true;
      } else if (throwable instanceof InvocationTargetException) {
         return true;
      } else if (isThrowableNested()) {
         return true;
      } else {
         Class cls = throwable.getClass();
         int i = 0;

         for (int isize = CAUSE_METHOD_NAMES.length; i < isize; i++) {
            try {
               Method method = cls.getMethod(CAUSE_METHOD_NAMES[i], null);
               if (method != null && Throwable.class.isAssignableFrom(method.getReturnType())) {
                  return true;
               }
            } catch (NoSuchMethodException var7) {
            } catch (SecurityException var8) {
            }
         }

         try {
            Field field = cls.getField("detail");
            if (field != null) {
               return true;
            }
         } catch (NoSuchFieldException var5) {
         } catch (SecurityException var6) {
         }

         return false;
      }
   }

   public static int getThrowableCount(Throwable throwable) {
      return getThrowableList(throwable).size();
   }

   public static Throwable[] getThrowables(Throwable throwable) {
      List list = getThrowableList(throwable);
      return list.toArray(new Throwable[list.size()]);
   }

   public static List getThrowableList(Throwable throwable) {
      List list = new ArrayList();

      while (throwable != null && !list.contains(throwable)) {
         list.add(throwable);
         throwable = getCause(throwable);
      }

      return list;
   }

   public static int indexOfThrowable(Throwable throwable, Class clazz) {
      return indexOf(throwable, clazz, 0, false);
   }

   public static int indexOfThrowable(Throwable throwable, Class clazz, int fromIndex) {
      return indexOf(throwable, clazz, fromIndex, false);
   }

   public static int indexOfType(Throwable throwable, Class type) {
      return indexOf(throwable, type, 0, true);
   }

   public static int indexOfType(Throwable throwable, Class type, int fromIndex) {
      return indexOf(throwable, type, fromIndex, true);
   }

   private static int indexOf(Throwable throwable, Class type, int fromIndex, boolean subclass) {
      if (throwable != null && type != null) {
         if (fromIndex < 0) {
            fromIndex = 0;
         }

         Throwable[] throwables = getThrowables(throwable);
         if (fromIndex >= throwables.length) {
            return -1;
         } else {
            if (subclass) {
               for (int i = fromIndex; i < throwables.length; i++) {
                  if (type.isAssignableFrom(throwables[i].getClass())) {
                     return i;
                  }
               }
            } else {
               for (int ix = fromIndex; ix < throwables.length; ix++) {
                  if (type.equals(throwables[ix].getClass())) {
                     return ix;
                  }
               }
            }

            return -1;
         }
      } else {
         return -1;
      }
   }

   public static void removeCommonFrames(List causeFrames, List wrapperFrames) {
      if (causeFrames != null && wrapperFrames != null) {
         int causeFrameIndex = causeFrames.size() - 1;

         for (int wrapperFrameIndex = wrapperFrames.size() - 1; causeFrameIndex >= 0 && wrapperFrameIndex >= 0; wrapperFrameIndex--) {
            String causeFrame = (String)causeFrames.get(causeFrameIndex);
            String wrapperFrame = (String)wrapperFrames.get(wrapperFrameIndex);
            if (causeFrame.equals(wrapperFrame)) {
               causeFrames.remove(causeFrameIndex);
            }

            causeFrameIndex--;
         }
      } else {
         throw new IllegalArgumentException("The List must not be null");
      }
   }

   public static String getFullStackTrace(Throwable throwable) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      Throwable[] ts = getThrowables(throwable);

      for (int i = 0; i < ts.length; i++) {
         ts[i].printStackTrace(pw);
         if (isNestedThrowable(ts[i])) {
            break;
         }
      }

      return sw.getBuffer().toString();
   }

   public static String getStackTrace(Throwable throwable) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw, true);
      throwable.printStackTrace(pw);
      return sw.getBuffer().toString();
   }

   static List getStackFrameList(Throwable t) {
      String stackTrace = getStackTrace(t);
      String linebreak = LINE_SEPARATOR;
      StringTokenizer frames = new StringTokenizer(stackTrace, linebreak);
      List list = new ArrayList();
      boolean traceStarted = false;

      while (frames.hasMoreTokens()) {
         String token = frames.nextToken();
         int at = token.indexOf("at");
         if (at != -1 && token.substring(0, at).trim().length() == 0) {
            traceStarted = true;
            list.add(token);
         } else if (traceStarted) {
            break;
         }
      }

      return list;
   }

   static {
      Method causeMethod;
      try {
         causeMethod = Throwable.class.getMethod("getCause", null);
      } catch (Exception var3) {
         causeMethod = null;
      }

      THROWABLE_CAUSE_METHOD = causeMethod;

      try {
         causeMethod = Throwable.class.getMethod("initCause", Throwable.class);
      } catch (Exception var2) {
         causeMethod = null;
      }

      THROWABLE_INITCAUSE_METHOD = causeMethod;
   }

   public interface Nestable {
      Throwable getCause();

      String getMessage();

      String getMessage(int index);

      String[] getMessages();

      Throwable getThrowable(int index);

      int getThrowableCount();

      Throwable[] getThrowables();

      int indexOfThrowable(Class type);

      int indexOfThrowable(Class type, int fromIndex);

      void printStackTrace(PrintWriter out);

      void printStackTrace(PrintStream out);

      void printPartialStackTrace(PrintWriter out);
   }
}
