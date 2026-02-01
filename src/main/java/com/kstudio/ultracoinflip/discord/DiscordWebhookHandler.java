package com.kstudio.ultracoinflip.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kstudio.ultracoinflip.KStudio;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.bukkit.configuration.file.FileConfiguration;

public class DiscordWebhookHandler {
   private final KStudio plugin;

   public DiscordWebhookHandler(KStudio plugin) {
      this.plugin = plugin;
   }

   public void sendGameResult(String winner, String loser, double amount, double taxedAmount, String currencyDisplayName, String currencySymbol) {
      FileConfiguration config = this.plugin.getConfig();
      if (config.getBoolean("discord.webhook.enabled", false)) {
         String webhookUrl = config.getString("discord.webhook.url", "");
         if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN")) {
            this.plugin.getLogger().warning("Discord webhook URL is not configured! Please set a valid webhook URL in config.yml");
         } else if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") && !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            this.plugin.getLogger().warning("Invalid Discord webhook URL format! URL must start with https://discord.com/api/webhooks/");
         } else {
            double minAmount = config.getDouble("discord.webhook.min-amount", 0.0);
            if (!(amount < minAmount)) {
               String username = config.getString("discord.webhook.username", "Coin Flip");
               String avatar = config.getString("discord.webhook.avatar", "");
               String content = config.getString("discord.message.content", "");
               boolean embedEnabled = config.getBoolean("discord.message.embed.enabled", true);
               content = this.replacePlaceholders(content, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
               JsonObject json = new JsonObject();
               if (username != null && !username.isEmpty()) {
                  json.addProperty("username", username);
               }

               if (avatar != null && !avatar.isEmpty()) {
                  json.addProperty("avatar_url", avatar);
               }

               if (content != null && !content.isEmpty()) {
                  json.addProperty("content", content);
               }

               if (embedEnabled) {
                  JsonObject embed = new JsonObject();
                  String title = config.getString("discord.message.embed.title", "");
                  if (title != null && !title.isEmpty()) {
                     title = this.replacePlaceholders(title, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
                     embed.addProperty("title", title);
                  }

                  String description = config.getString("discord.message.embed.description", "");
                  if (description != null && !description.isEmpty()) {
                     description = this.replacePlaceholders(description, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
                     embed.addProperty("description", description);
                  }

                  int r = config.getInt("discord.message.embed.color.r", 255);
                  int g = config.getInt("discord.message.embed.color.g", 193);
                  int b = config.getInt("discord.message.embed.color.b", 7);
                  r = Math.max(0, Math.min(255, r));
                  g = Math.max(0, Math.min(255, g));
                  b = Math.max(0, Math.min(255, b));
                  int color = (r << 16) + (g << 8) + b;
                  embed.addProperty("color", color);
                  if (config.contains("discord.message.embed.fields") && config.isList("discord.message.embed.fields")) {
                     JsonArray fieldsArray = new JsonArray();

                     for (Object fieldObj : config.getList("discord.message.embed.fields")) {
                        if (fieldObj instanceof Map) {
                           Map<String, Object> fieldMap = (Map<String, Object>)fieldObj;
                           String fieldName = String.valueOf(fieldMap.get("name"));
                           String fieldValue = String.valueOf(fieldMap.get("value"));
                           boolean inline = false;
                           if (fieldMap.containsKey("inline")) {
                              Object inlineObj = fieldMap.get("inline");
                              if (inlineObj instanceof Boolean) {
                                 inline = (Boolean)inlineObj;
                              } else if (inlineObj instanceof String) {
                                 inline = Boolean.parseBoolean((String)inlineObj);
                              }
                           }

                           if (fieldName != null
                              && !fieldName.isEmpty()
                              && !fieldName.equals("null")
                              && fieldValue != null
                              && !fieldValue.isEmpty()
                              && !fieldValue.equals("null")) {
                              fieldName = this.replacePlaceholders(fieldName, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
                              fieldValue = this.replacePlaceholders(fieldValue, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
                              JsonObject field = new JsonObject();
                              field.addProperty("name", fieldName);
                              field.addProperty("value", fieldValue);
                              field.addProperty("inline", inline);
                              fieldsArray.add(field);
                           }
                        }
                     }

                     if (fieldsArray.size() > 0) {
                        embed.add("fields", fieldsArray);
                     }
                  }

                  String footerText = config.getString("discord.message.embed.footer.text", "");
                  if (footerText != null && !footerText.isEmpty()) {
                     footerText = this.replacePlaceholders(footerText, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
                     JsonObject footer = new JsonObject();
                     footer.addProperty("text", footerText);
                     String footerIcon = config.getString("discord.message.embed.footer.icon", "");
                     if (footerIcon != null && !footerIcon.isEmpty()) {
                        footer.addProperty("icon_url", footerIcon);
                     }

                     embed.add("footer", footer);
                  }

                  String thumbnailUrl = config.getString("discord.message.embed.thumbnail", "");
                  if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                     thumbnailUrl = this.replacePlaceholders(thumbnailUrl, winner, loser, amount, taxedAmount, currencyDisplayName, currencySymbol);
                     JsonObject thumbnail = new JsonObject();
                     thumbnail.addProperty("url", thumbnailUrl);
                     embed.add("thumbnail", thumbnail);
                  }

                  if (config.getBoolean("discord.message.embed.timestamp", true)) {
                     embed.addProperty("timestamp", Instant.now().toString());
                  }

                  JsonArray embeds = new JsonArray();
                  embeds.add(embed);
                  json.add("embeds", embeds);
               }

               String finalJson = json.toString();
               CompletableFuture.runAsync(() -> {
                  try {
                     this.sendWebhook(webhookUrl, finalJson);
                  } catch (Exception var4) {
                     this.plugin.getLogger().warning("Failed to send Discord webhook: " + var4.getMessage());
                  }
               });
            }
         }
      }
   }

   private String replacePlaceholders(
      String text, String winner, String loser, double amount, double taxedAmount, String currencyDisplayName, String currencySymbol
   ) {
      if (text == null) {
         return "";
      } else {
         String amountStr = this.plugin.getGuiHelper().formatAmount(amount);
         String taxedAmountStr = this.plugin.getGuiHelper().formatAmount(taxedAmount);
         StringBuilder result = new StringBuilder(text.length() + 50);
         result.append(text);

         int index;
         while ((index = result.indexOf("%amount%")) != -1) {
            result.replace(index, index + 9, amountStr);
         }

         while ((index = result.indexOf("%taxed_amount%")) != -1) {
            result.replace(index, index + 15, taxedAmountStr);
         }

         while ((index = result.indexOf("%winner%")) != -1) {
            result.replace(index, index + 9, winner);
         }

         while ((index = result.indexOf("%loser%")) != -1) {
            result.replace(index, index + 8, loser);
         }

         while ((index = result.indexOf("%currency%")) != -1) {
            result.replace(index, index + 11, currencyDisplayName);
         }

         while ((index = result.indexOf("%symbol%")) != -1) {
            result.replace(index, index + 9, currencySymbol);
         }

         return result.toString();
      }
   }

   private void sendWebhook(String webhookUrl, String json) throws Exception {
      URL url = new URL(webhookUrl);
      HttpURLConnection connection = (HttpURLConnection)url.openConnection();

      try {
         connection.setRequestMethod("POST");
         connection.setRequestProperty("Content-Type", "application/json");
         connection.setRequestProperty("User-Agent", "UltraCoinFlip-Plugin");
         connection.setDoOutput(true);
         connection.setConnectTimeout(15000);
         connection.setReadTimeout(30000);
         connection.setInstanceFollowRedirects(false);
         OutputStream os = connection.getOutputStream();

         try {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
            os.flush();
         } catch (Throwable var32) {
            if (os != null) {
               try {
                  os.close();
               } catch (Throwable var28) {
                  var32.addSuppressed(var28);
               }
            }

            throw var32;
         }

         if (os != null) {
            os.close();
         }

         int responseCode;
         try {
            responseCode = connection.getResponseCode();
         } catch (SocketTimeoutException var30) {
            throw new Exception("Discord webhook connection timeout - check your internet connection and webhook URL");
         } catch (IOException var31) {
            if (var31.getMessage() == null || !var31.getMessage().contains("timed out") && !var31.getMessage().contains("Read timed out")) {
               throw new Exception("Discord webhook connection error: " + var31.getMessage());
            }

            throw new Exception("Discord webhook read timeout - server took too long to respond");
         }

         boolean isSuccess = responseCode >= 200 && responseCode < 300;
         if (!isSuccess) {
            try {
               InputStream errorStream = connection.getErrorStream();
               if (errorStream != null) {
                  BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8));

                  try {
                     StringBuilder errorResponse = new StringBuilder();

                     String line;
                     for (int lineCount = 0; (line = reader.readLine()) != null && lineCount < 50; lineCount++) {
                        errorResponse.append(line);
                        if (errorResponse.length() > 1000) {
                           break;
                        }
                     }

                     if (errorResponse.length() > 0) {
                        this.plugin
                           .getLogger()
                           .warning(
                              "Discord webhook error (HTTP "
                                 + responseCode
                                 + "): "
                                 + errorResponse.toString().substring(0, Math.min(500, errorResponse.length()))
                           );
                     }
                  } catch (Throwable var33) {
                     try {
                        reader.close();
                     } catch (Throwable var29) {
                        var33.addSuppressed(var29);
                     }

                     throw var33;
                  }

                  reader.close();
               }
            } catch (SocketTimeoutException var34) {
               this.plugin.getLogger().warning("Discord webhook failed (HTTP " + responseCode + "), timeout reading error response");
            } catch (Exception var35) {
               this.plugin.getLogger().warning("Discord webhook failed (HTTP " + responseCode + "), could not read error details: " + var35.getMessage());
            }

            throw new Exception("Discord webhook request failed with HTTP " + responseCode);
         }
      } catch (SocketTimeoutException var36) {
         throw new Exception("Discord webhook timeout: " + var36.getMessage());
      } catch (IOException var37) {
         String errorMsg = var37.getMessage();
         if (errorMsg != null && errorMsg.contains("timed out")) {
            throw new Exception("Discord webhook timeout: " + errorMsg);
         }

         throw new Exception("Discord webhook IO error: " + errorMsg);
      } finally {
         try {
            connection.disconnect();
         } catch (Exception var27) {
         }
      }
   }
}
