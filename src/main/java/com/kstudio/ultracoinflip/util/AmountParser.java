package com.kstudio.ultracoinflip.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmountParser {
   private static final Pattern FORMATTED_AMOUNT_PATTERN = Pattern.compile("^([0-9]+\\.?[0-9]*)\\s*([kmbtKMBT])$");
   private static final Pattern PLAIN_NUMBER_PATTERN = Pattern.compile("^[0-9]+\\.?[0-9]*$");

   public static double parseFormattedAmount(String input) throws IllegalArgumentException {
      if (input != null && !input.trim().isEmpty()) {
         String trimmed = input.trim();
         if (PLAIN_NUMBER_PATTERN.matcher(trimmed).matches()) {
            try {
               double value = Double.parseDouble(trimmed);
               if (value < 0.0) {
                  throw new IllegalArgumentException("Amount cannot be negative");
               } else if (!Double.isInfinite(value) && !Double.isNaN(value)) {
                  return value;
               } else {
                  throw new IllegalArgumentException("Amount must be a valid number");
               }
            } catch (NumberFormatException var11) {
               throw new IllegalArgumentException("Invalid number format: " + trimmed);
            }
         } else {
            Matcher matcher = FORMATTED_AMOUNT_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
               throw new IllegalArgumentException(
                  "Invalid amount format: "
                     + trimmed
                     + ". Valid formats: plain number (e.g., 100) or formatted (e.g., 1k, 1.5M, 2B, 1T). Supported suffixes: k (thousand), M (million), B (billion), T (trillion)"
               );
            } else {
               String numberStr = matcher.group(1);
               String suffix = matcher.group(2).toUpperCase();

               try {
                  double baseAmount = Double.parseDouble(numberStr);
                  if (baseAmount < 0.0) {
                     throw new IllegalArgumentException("Amount cannot be negative");
                  } else if (!Double.isInfinite(baseAmount) && !Double.isNaN(baseAmount)) {
                     double multiplier;
                     switch (suffix) {
                        case "K":
                           multiplier = 1000.0;
                           break;
                        case "M":
                           multiplier = 1000000.0;
                           break;
                        case "B":
                           multiplier = 1.0E9;
                           break;
                        case "T":
                           multiplier = 1.0E12;
                           break;
                        default:
                           throw new IllegalArgumentException("Unsupported suffix: " + suffix + ". Supported: k, M, B, T");
                     }

                     double result = baseAmount * multiplier;
                     if (!Double.isInfinite(result) && !(result > 8.988465674311579E307)) {
                        return result;
                     } else {
                        throw new IllegalArgumentException("Amount is too large (maximum: ~1.7e308)");
                     }
                  } else {
                     throw new IllegalArgumentException("Amount must be a valid number");
                  }
               } catch (NumberFormatException var12) {
                  throw new IllegalArgumentException("Invalid number format: " + numberStr);
               }
            }
         }
      } else {
         throw new IllegalArgumentException("Amount cannot be null or empty");
      }
   }

   public static boolean isValidFormattedAmount(String input) {
      if (input != null && !input.trim().isEmpty()) {
         String trimmed = input.trim();
         return PLAIN_NUMBER_PATTERN.matcher(trimmed).matches() || FORMATTED_AMOUNT_PATTERN.matcher(trimmed).matches();
      } else {
         return false;
      }
   }

   public static String formatAmount(double amount) {
      if (amount < 0.0) {
         return String.format("%.2f", amount);
      } else if (amount >= 1.0E12) {
         double value = amount / 1.0E12;
         return value == Math.floor(value) ? String.format("%.0fT", value) : String.format("%.2fT", value);
      } else if (amount >= 1.0E9) {
         double value = amount / 1.0E9;
         return value == Math.floor(value) ? String.format("%.0fB", value) : String.format("%.2fB", value);
      } else if (amount >= 1000000.0) {
         double value = amount / 1000000.0;
         return value == Math.floor(value) ? String.format("%.0fM", value) : String.format("%.2fM", value);
      } else if (amount >= 10000.0) {
         double value = amount / 1000.0;
         return value == Math.floor(value) ? String.format("%.0fk", value) : String.format("%.2fk", value);
      } else {
         return amount == Math.floor(amount) ? String.format("%.0f", amount) : String.format("%.2f", amount);
      }
   }
}
