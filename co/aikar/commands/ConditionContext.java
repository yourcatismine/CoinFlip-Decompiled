package co.aikar.commands;

import java.util.HashMap;
import java.util.Map;

public class ConditionContext<I extends CommandIssuer> {
   private final I issuer;
   private final String config;
   private final Map<String, String> configs;

   ConditionContext(I issuer, String config) {
      this.issuer = issuer;
      this.config = config;
      this.configs = new HashMap<>();
      if (config != null) {
         for (String s : ACFPatterns.COMMA.split(config)) {
            String[] v = ACFPatterns.EQUALS.split(s, 2);
            this.configs.put(v[0], v.length > 1 ? v[1] : null);
         }
      }
   }

   public I getIssuer() {
      return this.issuer;
   }

   public String getConfig() {
      return this.config;
   }

   public boolean hasConfig(String flag) {
      return this.configs.containsKey(flag);
   }

   public String getConfigValue(String flag, String def) {
      return this.configs.getOrDefault(flag, def);
   }

   public Integer getConfigValue(String flag, Integer def) {
      return ACFUtil.parseInt(this.configs.get(flag), def);
   }
}
