package co.aikar.commands.apachecommonslang;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Locale;

public class ApacheCommonsLangUtil {
   public static final String EMPTY = "";
   public static final int INDEX_NOT_FOUND = -1;

   public static <T> T[] clone(final T[] array) {
      return (T[])(array == null ? null : (Object[])array.clone());
   }

   public static <T> T[] addAll(final T[] array1, final T... array2) {
      if (array1 == null) {
         return (T[])clone(array2);
      } else if (array2 == null) {
         return (T[])clone(array1);
      } else {
         Class<?> type1 = array1.getClass().getComponentType();
         T[] joinedArray = (T[])Array.newInstance(type1, array1.length + array2.length);
         System.arraycopy(array1, 0, joinedArray, 0, array1.length);

         try {
            System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
            return joinedArray;
         } catch (ArrayStoreException var6) {
            Class<?> type2 = array2.getClass().getComponentType();
            if (!type1.isAssignableFrom(type2)) {
               throw new IllegalArgumentException("Cannot store " + type2.getName() + " in an array of " + type1.getName(), var6);
            } else {
               throw var6;
            }
         }
      }
   }

   public static String capitalizeFully(final String str) {
      return capitalizeFully(str, null);
   }

   public static String capitalizeFully(String str, final char... delimiters) {
      int delimLen = delimiters == null ? -1 : delimiters.length;
      if (str != null && !str.isEmpty() && delimLen != 0) {
         str = str.toLowerCase(Locale.ENGLISH);
         return capitalize(str, delimiters);
      } else {
         return str;
      }
   }

   public static String capitalize(final String str) {
      return capitalize(str, null);
   }

   public static String capitalize(final String str, final char... delimiters) {
      int delimLen = delimiters == null ? -1 : delimiters.length;
      if (str != null && !str.isEmpty() && delimLen != 0) {
         char[] buffer = str.toCharArray();
         boolean capitalizeNext = true;

         for (int i = 0; i < buffer.length; i++) {
            char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
               capitalizeNext = true;
            } else if (capitalizeNext) {
               buffer[i] = Character.toTitleCase(ch);
               capitalizeNext = false;
            }
         }

         return new String(buffer);
      } else {
         return str;
      }
   }

   public static boolean isDelimiter(final char ch, final char[] delimiters) {
      if (delimiters == null) {
         return Character.isWhitespace(ch);
      } else {
         for (char delimiter : delimiters) {
            if (ch == delimiter) {
               return true;
            }
         }

         return false;
      }
   }

   @SafeVarargs
   public static <T> String join(final T... elements) {
      return join(elements, null);
   }

   public static String join(final Object[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final long[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final int[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final short[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final byte[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final char[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final float[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final double[] array, final char separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final Object[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               if (array[i] != null) {
                  buf.append(array[i]);
               }
            }

            return buf.toString();
         }
      }
   }

   public static String join(final long[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final int[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final byte[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final short[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final char[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final double[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final float[] array, final char separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               buf.append(array[i]);
            }

            return buf.toString();
         }
      }
   }

   public static String join(final Object[] array, final String separator) {
      return array == null ? null : join(array, separator, 0, array.length);
   }

   public static String join(final Object[] array, String separator, final int startIndex, final int endIndex) {
      if (array == null) {
         return null;
      } else {
         if (separator == null) {
            separator = "";
         }

         int noOfItems = endIndex - startIndex;
         if (noOfItems <= 0) {
            return "";
         } else {
            StringBuilder buf = new StringBuilder(noOfItems * 16);

            for (int i = startIndex; i < endIndex; i++) {
               if (i > startIndex) {
                  buf.append(separator);
               }

               if (array[i] != null) {
                  buf.append(array[i]);
               }
            }

            return buf.toString();
         }
      }
   }

   public static String join(final Iterator<?> iterator, final char separator) {
      if (iterator == null) {
         return null;
      } else if (!iterator.hasNext()) {
         return "";
      } else {
         Object first = iterator.next();
         if (!iterator.hasNext()) {
            return first != null ? first.toString() : "";
         } else {
            StringBuilder buf = new StringBuilder(256);
            if (first != null) {
               buf.append(first);
            }

            while (iterator.hasNext()) {
               buf.append(separator);
               Object obj = iterator.next();
               if (obj != null) {
                  buf.append(obj);
               }
            }

            return buf.toString();
         }
      }
   }

   public static String join(final Iterator<?> iterator, final String separator) {
      if (iterator == null) {
         return null;
      } else if (!iterator.hasNext()) {
         return "";
      } else {
         Object first = iterator.next();
         if (!iterator.hasNext()) {
            return first != null ? first.toString() : "";
         } else {
            StringBuilder buf = new StringBuilder(256);
            if (first != null) {
               buf.append(first);
            }

            while (iterator.hasNext()) {
               if (separator != null) {
                  buf.append(separator);
               }

               Object obj = iterator.next();
               if (obj != null) {
                  buf.append(obj);
               }
            }

            return buf.toString();
         }
      }
   }

   public static String join(final Iterable<?> iterable, final char separator) {
      return iterable == null ? null : join(iterable.iterator(), separator);
   }

   public static String join(final Iterable<?> iterable, final String separator) {
      return iterable == null ? null : join(iterable.iterator(), separator);
   }

   public static boolean isNumeric(final CharSequence cs) {
      if (cs != null && cs.length() != 0) {
         int sz = cs.length();

         for (int i = 0; i < sz; i++) {
            if (!Character.isDigit(cs.charAt(i))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean startsWith(final CharSequence str, final CharSequence prefix) {
      return startsWith(str, prefix, false);
   }

   public static boolean startsWithIgnoreCase(final CharSequence str, final CharSequence prefix) {
      return startsWith(str, prefix, true);
   }

   private static boolean startsWith(final CharSequence str, final CharSequence prefix, final boolean ignoreCase) {
      if (str != null && prefix != null) {
         return prefix.length() > str.length() ? false : regionMatches(str, ignoreCase, 0, prefix, 0, prefix.length());
      } else {
         return str == null && prefix == null;
      }
   }

   static boolean regionMatches(
      final CharSequence cs, final boolean ignoreCase, final int thisStart, final CharSequence substring, final int start, final int length
   ) {
      if (cs instanceof String && substring instanceof String) {
         return ((String)cs).regionMatches(ignoreCase, thisStart, (String)substring, start, length);
      } else {
         int index1 = thisStart;
         int index2 = start;
         int tmpLen = length;
         int srcLen = cs.length() - thisStart;
         int otherLen = substring.length() - start;
         if (thisStart >= 0 && start >= 0 && length >= 0) {
            if (srcLen >= length && otherLen >= length) {
               while (tmpLen-- > 0) {
                  char c1 = cs.charAt(index1++);
                  char c2 = substring.charAt(index2++);
                  if (c1 != c2) {
                     if (!ignoreCase) {
                        return false;
                     }

                     if (Character.toUpperCase(c1) != Character.toUpperCase(c2) && Character.toLowerCase(c1) != Character.toLowerCase(c2)) {
                        return false;
                     }
                  }
               }

               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static int indexOf(Object[] array, Object objectToFind) {
      return indexOf(array, objectToFind, 0);
   }

   public static int indexOf(Object[] array, Object objectToFind, int startIndex) {
      if (array == null) {
         return -1;
      } else {
         if (startIndex < 0) {
            startIndex = 0;
         }

         if (objectToFind == null) {
            for (int i = startIndex; i < array.length; i++) {
               if (array[i] == null) {
                  return i;
               }
            }
         } else {
            for (int ix = startIndex; ix < array.length; ix++) {
               if (objectToFind.equals(array[ix])) {
                  return ix;
               }
            }
         }

         return -1;
      }
   }
}
