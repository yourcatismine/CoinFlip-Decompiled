package com.kstudio.ultracoinflip.util;

import com.kstudio.ultracoinflip.KStudio;
import com.kstudio.ultracoinflip.libs.bstats.bukkit.Metrics;
import com.kstudio.ultracoinflip.libs.bstats.charts.SimplePie;
import com.kstudio.ultracoinflip.libs.bstats.charts.SingleLineChart;
import org.bukkit.configuration.file.FileConfiguration;

public class UltraCoinFlipMetrics {
   private final KStudio plugin;
   private Metrics metrics;
   private static final int PLUGIN_ID = 28036;

   public UltraCoinFlipMetrics(KStudio plugin) {
      this.plugin = plugin;
   }

   public void initialize() {
      FileConfiguration config = this.plugin.getConfigManager().getConfig();
      if (!config.getBoolean("bstats.enabled", true)) {
         this.plugin.getColorLogger().info("     " + this.plugin.getColorLogger().gray("bStats is disabled in config"));
      } else {
         try {
            this.metrics = new Metrics(this.plugin, 28036);
            this.setupCharts();
            this.plugin
               .getColorLogger()
               .info(
                  "     "
                     + this.plugin.getColorLogger().brightGreen("bStats enabled!")
                     + this.plugin.getColorLogger().gray(" Anonymous statistics collection active")
               );
         } catch (Exception var3) {
            this.plugin.getLogger().warning("Failed to initialize bStats: " + var3.getMessage());
            if (this.plugin.getDebugManager() != null && this.plugin.getDebugManager().isCategoryEnabled(DebugManager.Category.GENERAL)) {
               var3.printStackTrace();
            }
         }
      }
   }

   private void setupCharts() {
      if (this.metrics != null) {
         this.metrics.addCustomChart(new SimplePie("database_type", () -> {
            String dbType = this.plugin.getConfigManager().getConfig().getString("database.type", "SQLITE");
            return dbType != null ? dbType.toUpperCase() : "SQLITE";
         }));
         this.metrics.addCustomChart(new SimplePie("update_checker_enabled", () -> {
            boolean enabled = this.plugin.getConfigManager().getConfig().getBoolean("update-checker.enabled", true);
            return enabled ? "Enabled" : "Disabled";
         }));
         this.metrics.addCustomChart(new SimplePie("input_method", () -> {
            String method = this.plugin.getConfigManager().getConfig().getString("input.method", "CHAT");
            return method != null ? method.toUpperCase() : "CHAT";
         }));
         this.metrics.addCustomChart(new SimplePie("multiple_games_enabled", () -> {
            boolean enabled = this.plugin.getConfigManager().getConfig().getBoolean("game-behavior.multiple-games.enabled", false);
            return enabled ? "Enabled" : "Disabled";
         }));
         this.metrics.addCustomChart(new SimplePie("number_format_type", () -> {
            String format = this.plugin.getConfigManager().getConfig().getString("number-format.type", "COMPACT");
            return format != null ? format.toUpperCase() : "COMPACT";
         }));
         this.metrics.addCustomChart(new SingleLineChart("total_games", () -> {
            if (this.plugin.getCoinFlipManager() != null) {
               try {
                  return this.plugin.getCoinFlipManager().getAllGames().size();
               } catch (Exception var2) {
                  return 0;
               }
            } else {
               return 0;
            }
         }));
      }
   }

   public Metrics getMetrics() {
      return this.metrics;
   }
}
