package com.kstudio.ultracoinflip.currency;

import lombok.Generated;

public class CurrencySettings {
   private boolean broadcastEnabled;
   private double minBroadcastAmount;
   private double minBid;
   private double maxBid;
   private double minReserveBalance;
   private boolean taxEnabled;
   @Deprecated
   private double taxRate;
   private TaxRateConfig taxRateConfig;
   private String syntaxCommand;
   private CurrencyRestrictions restrictions;

   public CurrencySettings() {
      this.broadcastEnabled = true;
      this.minBroadcastAmount = 100.0;
      this.minBid = 1.0;
      this.maxBid = -1.0;
      this.minReserveBalance = 0.0;
      this.taxEnabled = true;
      this.taxRate = 0.1;
      this.taxRateConfig = null;
      this.syntaxCommand = "";
      this.restrictions = new CurrencyRestrictions();
   }

   public CurrencySettings(boolean broadcastEnabled, double minBroadcastAmount, double minBid, double maxBid, double taxRate) {
      this.broadcastEnabled = broadcastEnabled;
      this.minBroadcastAmount = minBroadcastAmount;
      this.minBid = minBid;
      this.maxBid = maxBid;
      this.minReserveBalance = 0.0;
      this.taxEnabled = true;
      this.taxRate = taxRate;
      this.taxRateConfig = null;
      this.syntaxCommand = "";
   }

   public CurrencySettings(boolean broadcastEnabled, double minBroadcastAmount, double minBid, double maxBid, double taxRate, String syntaxCommand) {
      this.broadcastEnabled = broadcastEnabled;
      this.minBroadcastAmount = minBroadcastAmount;
      this.minBid = minBid;
      this.maxBid = maxBid;
      this.minReserveBalance = 0.0;
      this.taxEnabled = true;
      this.taxRate = taxRate;
      this.taxRateConfig = null;
      this.syntaxCommand = syntaxCommand != null ? syntaxCommand : "";
   }

   public CurrencySettings(
      boolean broadcastEnabled, double minBroadcastAmount, double minBid, double maxBid, boolean taxEnabled, double taxRate, String syntaxCommand
   ) {
      this.broadcastEnabled = broadcastEnabled;
      this.minBroadcastAmount = minBroadcastAmount;
      this.minBid = minBid;
      this.maxBid = maxBid;
      this.minReserveBalance = 0.0;
      this.taxEnabled = taxEnabled;
      this.taxRate = taxRate;
      this.taxRateConfig = null;
      this.syntaxCommand = syntaxCommand != null ? syntaxCommand : "";
   }

   public double getTaxRate() {
      return this.taxRateConfig != null ? this.taxRateConfig.getBaseTaxRate() : this.taxRate;
   }

   public boolean isUsingDynamicTaxRate() {
      return this.taxRateConfig != null;
   }

   public boolean isTaxEnabled() {
      return this.taxEnabled;
   }

   @Generated
   public boolean isBroadcastEnabled() {
      return this.broadcastEnabled;
   }

   @Generated
   public double getMinBroadcastAmount() {
      return this.minBroadcastAmount;
   }

   @Generated
   public double getMinBid() {
      return this.minBid;
   }

   @Generated
   public double getMaxBid() {
      return this.maxBid;
   }

   @Generated
   public double getMinReserveBalance() {
      return this.minReserveBalance;
   }

   @Generated
   public TaxRateConfig getTaxRateConfig() {
      return this.taxRateConfig;
   }

   @Generated
   public String getSyntaxCommand() {
      return this.syntaxCommand;
   }

   @Generated
   public CurrencyRestrictions getRestrictions() {
      return this.restrictions;
   }

   @Generated
   public void setBroadcastEnabled(boolean broadcastEnabled) {
      this.broadcastEnabled = broadcastEnabled;
   }

   @Generated
   public void setMinBroadcastAmount(double minBroadcastAmount) {
      this.minBroadcastAmount = minBroadcastAmount;
   }

   @Generated
   public void setMinBid(double minBid) {
      this.minBid = minBid;
   }

   @Generated
   public void setMaxBid(double maxBid) {
      this.maxBid = maxBid;
   }

   @Generated
   public void setMinReserveBalance(double minReserveBalance) {
      this.minReserveBalance = minReserveBalance;
   }

   @Generated
   public void setTaxEnabled(boolean taxEnabled) {
      this.taxEnabled = taxEnabled;
   }

   @Deprecated
   @Generated
   public void setTaxRate(double taxRate) {
      this.taxRate = taxRate;
   }

   @Generated
   public void setTaxRateConfig(TaxRateConfig taxRateConfig) {
      this.taxRateConfig = taxRateConfig;
   }

   @Generated
   public void setSyntaxCommand(String syntaxCommand) {
      this.syntaxCommand = syntaxCommand;
   }

   @Generated
   public void setRestrictions(CurrencyRestrictions restrictions) {
      this.restrictions = restrictions;
   }
}
