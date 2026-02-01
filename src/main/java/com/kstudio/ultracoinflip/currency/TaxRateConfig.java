package com.kstudio.ultracoinflip.currency;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;

public class TaxRateConfig {
   private double baseTaxRate = 0.1;
   private List<TaxRateConfig.Tier> tiers = new ArrayList<>();

   public TaxRateConfig() {
      this.baseTaxRate = 0.1;
   }

   @Generated
   public double getBaseTaxRate() {
      return this.baseTaxRate;
   }

   @Generated
   public List<TaxRateConfig.Tier> getTiers() {
      return this.tiers;
   }

   @Generated
   public void setBaseTaxRate(double baseTaxRate) {
      this.baseTaxRate = baseTaxRate;
   }

   @Generated
   public void setTiers(List<TaxRateConfig.Tier> tiers) {
      this.tiers = tiers;
   }

   public static class Tier {
      private double minAmount;
      private double maxAmount;
      private double taxRate;

      public Tier() {
         this.minAmount = 0.0;
         this.maxAmount = -1.0;
         this.taxRate = 0.1;
      }

      public Tier(double minAmount, double maxAmount, double taxRate) {
         this.minAmount = minAmount;
         this.maxAmount = maxAmount;
         this.taxRate = taxRate;
      }

      public boolean matches(double amount) {
         if (amount < 0.0) {
            return false;
         } else {
            return this.maxAmount == -1.0 ? amount >= this.minAmount : amount >= this.minAmount && amount < this.maxAmount;
         }
      }

      @Generated
      public double getMinAmount() {
         return this.minAmount;
      }

      @Generated
      public double getMaxAmount() {
         return this.maxAmount;
      }

      @Generated
      public double getTaxRate() {
         return this.taxRate;
      }

      @Generated
      public void setMinAmount(double minAmount) {
         this.minAmount = minAmount;
      }

      @Generated
      public void setMaxAmount(double maxAmount) {
         this.maxAmount = maxAmount;
      }

      @Generated
      public void setTaxRate(double taxRate) {
         this.taxRate = taxRate;
      }
   }
}
