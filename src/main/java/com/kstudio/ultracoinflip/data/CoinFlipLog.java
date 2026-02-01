package com.kstudio.ultracoinflip.data;

import java.util.UUID;
import lombok.Generated;

public class CoinFlipLog {
   private long id;
   private UUID player1UUID;
   private String player1Name;
   private UUID player2UUID;
   private String player2Name;
   private UUID winnerUUID;
   private String winnerName;
   private UUID loserUUID;
   private String loserName;
   private CoinFlipGame.CurrencyType currencyType;
   private String currencyId;
   private double amount;
   private double totalPot;
   private double taxRate;
   private double taxAmount;
   private double winnerAmount;
   private long timestamp;
   private CoinFlipLog.GameType gameType;

   public CoinFlipLog(
      UUID player1UUID,
      String player1Name,
      UUID player2UUID,
      String player2Name,
      UUID winnerUUID,
      String winnerName,
      UUID loserUUID,
      String loserName,
      CoinFlipGame.CurrencyType currencyType,
      String currencyId,
      double amount,
      double totalPot,
      double taxRate,
      double taxAmount,
      double winnerAmount,
      long timestamp
   ) {
      this(
         player1UUID,
         player1Name,
         player2UUID,
         player2Name,
         winnerUUID,
         winnerName,
         loserUUID,
         loserName,
         currencyType,
         currencyId,
         amount,
         totalPot,
         taxRate,
         taxAmount,
         winnerAmount,
         timestamp,
         CoinFlipLog.GameType.PLAYER
      );
   }

   public CoinFlipLog(
      UUID player1UUID,
      String player1Name,
      UUID player2UUID,
      String player2Name,
      UUID winnerUUID,
      String winnerName,
      UUID loserUUID,
      String loserName,
      CoinFlipGame.CurrencyType currencyType,
      String currencyId,
      double amount,
      double totalPot,
      double taxRate,
      double taxAmount,
      double winnerAmount,
      long timestamp,
      CoinFlipLog.GameType gameType
   ) {
      this.id = -1L;
      this.player1UUID = player1UUID;
      this.player1Name = player1Name;
      this.player2UUID = player2UUID;
      this.player2Name = player2Name;
      this.winnerUUID = winnerUUID;
      this.winnerName = winnerName;
      this.loserUUID = loserUUID;
      this.loserName = loserName;
      this.currencyType = currencyType;
      this.currencyId = currencyId;
      this.amount = amount;
      this.totalPot = totalPot;
      this.taxRate = taxRate;
      this.taxAmount = taxAmount;
      this.winnerAmount = winnerAmount;
      this.timestamp = timestamp;
      this.gameType = gameType;
   }

   @Generated
   public long getId() {
      return this.id;
   }

   @Generated
   public UUID getPlayer1UUID() {
      return this.player1UUID;
   }

   @Generated
   public String getPlayer1Name() {
      return this.player1Name;
   }

   @Generated
   public UUID getPlayer2UUID() {
      return this.player2UUID;
   }

   @Generated
   public String getPlayer2Name() {
      return this.player2Name;
   }

   @Generated
   public UUID getWinnerUUID() {
      return this.winnerUUID;
   }

   @Generated
   public String getWinnerName() {
      return this.winnerName;
   }

   @Generated
   public UUID getLoserUUID() {
      return this.loserUUID;
   }

   @Generated
   public String getLoserName() {
      return this.loserName;
   }

   @Generated
   public CoinFlipGame.CurrencyType getCurrencyType() {
      return this.currencyType;
   }

   @Generated
   public String getCurrencyId() {
      return this.currencyId;
   }

   @Generated
   public double getAmount() {
      return this.amount;
   }

   @Generated
   public double getTotalPot() {
      return this.totalPot;
   }

   @Generated
   public double getTaxRate() {
      return this.taxRate;
   }

   @Generated
   public double getTaxAmount() {
      return this.taxAmount;
   }

   @Generated
   public double getWinnerAmount() {
      return this.winnerAmount;
   }

   @Generated
   public long getTimestamp() {
      return this.timestamp;
   }

   @Generated
   public CoinFlipLog.GameType getGameType() {
      return this.gameType;
   }

   @Generated
   public void setId(long id) {
      this.id = id;
   }

   @Generated
   public void setPlayer1UUID(UUID player1UUID) {
      this.player1UUID = player1UUID;
   }

   @Generated
   public void setPlayer1Name(String player1Name) {
      this.player1Name = player1Name;
   }

   @Generated
   public void setPlayer2UUID(UUID player2UUID) {
      this.player2UUID = player2UUID;
   }

   @Generated
   public void setPlayer2Name(String player2Name) {
      this.player2Name = player2Name;
   }

   @Generated
   public void setWinnerUUID(UUID winnerUUID) {
      this.winnerUUID = winnerUUID;
   }

   @Generated
   public void setWinnerName(String winnerName) {
      this.winnerName = winnerName;
   }

   @Generated
   public void setLoserUUID(UUID loserUUID) {
      this.loserUUID = loserUUID;
   }

   @Generated
   public void setLoserName(String loserName) {
      this.loserName = loserName;
   }

   @Generated
   public void setCurrencyType(CoinFlipGame.CurrencyType currencyType) {
      this.currencyType = currencyType;
   }

   @Generated
   public void setCurrencyId(String currencyId) {
      this.currencyId = currencyId;
   }

   @Generated
   public void setAmount(double amount) {
      this.amount = amount;
   }

   @Generated
   public void setTotalPot(double totalPot) {
      this.totalPot = totalPot;
   }

   @Generated
   public void setTaxRate(double taxRate) {
      this.taxRate = taxRate;
   }

   @Generated
   public void setTaxAmount(double taxAmount) {
      this.taxAmount = taxAmount;
   }

   @Generated
   public void setWinnerAmount(double winnerAmount) {
      this.winnerAmount = winnerAmount;
   }

   @Generated
   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   @Generated
   public void setGameType(CoinFlipLog.GameType gameType) {
      this.gameType = gameType;
   }

   @Generated
   public CoinFlipLog(
      long id,
      UUID player1UUID,
      String player1Name,
      UUID player2UUID,
      String player2Name,
      UUID winnerUUID,
      String winnerName,
      UUID loserUUID,
      String loserName,
      CoinFlipGame.CurrencyType currencyType,
      String currencyId,
      double amount,
      double totalPot,
      double taxRate,
      double taxAmount,
      double winnerAmount,
      long timestamp,
      CoinFlipLog.GameType gameType
   ) {
      this.id = id;
      this.player1UUID = player1UUID;
      this.player1Name = player1Name;
      this.player2UUID = player2UUID;
      this.player2Name = player2Name;
      this.winnerUUID = winnerUUID;
      this.winnerName = winnerName;
      this.loserUUID = loserUUID;
      this.loserName = loserName;
      this.currencyType = currencyType;
      this.currencyId = currencyId;
      this.amount = amount;
      this.totalPot = totalPot;
      this.taxRate = taxRate;
      this.taxAmount = taxAmount;
      this.winnerAmount = winnerAmount;
      this.timestamp = timestamp;
      this.gameType = gameType;
   }

   public static enum GameType {
      PLAYER,
      HOUSE;
   }
}
