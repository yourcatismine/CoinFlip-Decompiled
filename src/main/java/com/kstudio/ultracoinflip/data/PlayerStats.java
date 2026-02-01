package com.kstudio.ultracoinflip.data;

import java.util.HashMap;
import java.util.Map;
import lombok.Generated;

public class PlayerStats {
   private int wins;
   private int defeats;
   private double profitMoney;
   private double profitPlayerPoints;
   private double profitTokenManager;
   private double profitBeastTokens;
   private double lossMoney;
   private double lossPlayerPoints;
   private double lossTokenManager;
   private double lossBeastTokens;
   private int winsMoney;
   private int defeatsMoney;
   private int winsPlayerPoints;
   private int defeatsPlayerPoints;
   private int winsTokenManager;
   private int defeatsTokenManager;
   private int winsBeastTokens;
   private int defeatsBeastTokens;
   private Map<String, Integer> winsPlaceholder = new HashMap<>();
   private Map<String, Integer> defeatsPlaceholder = new HashMap<>();
   private int winstreak = 0;

   public PlayerStats(PlayerStats other) {
      if (other != null) {
         this.wins = other.wins;
         this.defeats = other.defeats;
         this.profitMoney = other.profitMoney;
         this.profitPlayerPoints = other.profitPlayerPoints;
         this.profitTokenManager = other.profitTokenManager;
         this.profitBeastTokens = other.profitBeastTokens;
         this.lossMoney = other.lossMoney;
         this.lossPlayerPoints = other.lossPlayerPoints;
         this.lossTokenManager = other.lossTokenManager;
         this.lossBeastTokens = other.lossBeastTokens;
         this.winsMoney = other.winsMoney;
         this.defeatsMoney = other.defeatsMoney;
         this.winsPlayerPoints = other.winsPlayerPoints;
         this.defeatsPlayerPoints = other.defeatsPlayerPoints;
         this.winsTokenManager = other.winsTokenManager;
         this.defeatsTokenManager = other.defeatsTokenManager;
         this.winsBeastTokens = other.winsBeastTokens;
         this.defeatsBeastTokens = other.defeatsBeastTokens;
         this.winsPlaceholder = new HashMap<>(other.winsPlaceholder);
         this.defeatsPlaceholder = new HashMap<>(other.defeatsPlaceholder);
         this.winstreak = other.winstreak;
      }
   }

   public double getWinPercentage() {
      int totalGames = this.wins + this.defeats;
      return totalGames == 0 ? 0.0 : (double)this.wins / totalGames * 100.0;
   }

   public int getTotalGames() {
      return this.wins + this.defeats;
   }

   public double getNetProfitMoney() {
      return this.profitMoney - this.lossMoney;
   }

   public double getNetProfitPlayerPoints() {
      return this.profitPlayerPoints - this.lossPlayerPoints;
   }

   public double getNetProfitTokenManager() {
      return this.profitTokenManager - this.lossTokenManager;
   }

   public double getNetProfitBeastTokens() {
      return this.profitBeastTokens - this.lossBeastTokens;
   }

   public double getWinPercentageMoney() {
      int totalGames = this.winsMoney + this.defeatsMoney;
      return totalGames == 0 ? 0.0 : (double)this.winsMoney / totalGames * 100.0;
   }

   public double getWinPercentagePlayerPoints() {
      int totalGames = this.winsPlayerPoints + this.defeatsPlayerPoints;
      return totalGames == 0 ? 0.0 : (double)this.winsPlayerPoints / totalGames * 100.0;
   }

   public double getWinPercentageTokenManager() {
      int totalGames = this.winsTokenManager + this.defeatsTokenManager;
      return totalGames == 0 ? 0.0 : (double)this.winsTokenManager / totalGames * 100.0;
   }

   public double getWinPercentageBeastTokens() {
      int totalGames = this.winsBeastTokens + this.defeatsBeastTokens;
      return totalGames == 0 ? 0.0 : (double)this.winsBeastTokens / totalGames * 100.0;
   }

   public int getTotalGamesMoney() {
      return this.winsMoney + this.defeatsMoney;
   }

   public int getTotalGamesPlayerPoints() {
      return this.winsPlayerPoints + this.defeatsPlayerPoints;
   }

   public int getTotalGamesTokenManager() {
      return this.winsTokenManager + this.defeatsTokenManager;
   }

   public int getTotalGamesBeastTokens() {
      return this.winsBeastTokens + this.defeatsBeastTokens;
   }

   public double getWinPercentagePlaceholder(String currencyId) {
      int wins = this.winsPlaceholder.getOrDefault(currencyId, 0);
      int defeats = this.defeatsPlaceholder.getOrDefault(currencyId, 0);
      int totalGames = wins + defeats;
      return totalGames == 0 ? 0.0 : (double)wins / totalGames * 100.0;
   }

   public int getTotalGamesPlaceholder(String currencyId) {
      int wins = this.winsPlaceholder.getOrDefault(currencyId, 0);
      int defeats = this.defeatsPlaceholder.getOrDefault(currencyId, 0);
      return wins + defeats;
   }

   public void incrementWinsPlaceholder(String currencyId) {
      this.winsPlaceholder.put(currencyId, this.winsPlaceholder.getOrDefault(currencyId, 0) + 1);
   }

   public void incrementDefeatsPlaceholder(String currencyId) {
      this.defeatsPlaceholder.put(currencyId, this.defeatsPlaceholder.getOrDefault(currencyId, 0) + 1);
   }

   @Generated
   public int getWins() {
      return this.wins;
   }

   @Generated
   public int getDefeats() {
      return this.defeats;
   }

   @Generated
   public double getProfitMoney() {
      return this.profitMoney;
   }

   @Generated
   public double getProfitPlayerPoints() {
      return this.profitPlayerPoints;
   }

   @Generated
   public double getProfitTokenManager() {
      return this.profitTokenManager;
   }

   @Generated
   public double getProfitBeastTokens() {
      return this.profitBeastTokens;
   }

   @Generated
   public double getLossMoney() {
      return this.lossMoney;
   }

   @Generated
   public double getLossPlayerPoints() {
      return this.lossPlayerPoints;
   }

   @Generated
   public double getLossTokenManager() {
      return this.lossTokenManager;
   }

   @Generated
   public double getLossBeastTokens() {
      return this.lossBeastTokens;
   }

   @Generated
   public int getWinsMoney() {
      return this.winsMoney;
   }

   @Generated
   public int getDefeatsMoney() {
      return this.defeatsMoney;
   }

   @Generated
   public int getWinsPlayerPoints() {
      return this.winsPlayerPoints;
   }

   @Generated
   public int getDefeatsPlayerPoints() {
      return this.defeatsPlayerPoints;
   }

   @Generated
   public int getWinsTokenManager() {
      return this.winsTokenManager;
   }

   @Generated
   public int getDefeatsTokenManager() {
      return this.defeatsTokenManager;
   }

   @Generated
   public int getWinsBeastTokens() {
      return this.winsBeastTokens;
   }

   @Generated
   public int getDefeatsBeastTokens() {
      return this.defeatsBeastTokens;
   }

   @Generated
   public Map<String, Integer> getWinsPlaceholder() {
      return this.winsPlaceholder;
   }

   @Generated
   public Map<String, Integer> getDefeatsPlaceholder() {
      return this.defeatsPlaceholder;
   }

   @Generated
   public int getWinstreak() {
      return this.winstreak;
   }

   @Generated
   public void setWins(int wins) {
      this.wins = wins;
   }

   @Generated
   public void setDefeats(int defeats) {
      this.defeats = defeats;
   }

   @Generated
   public void setProfitMoney(double profitMoney) {
      this.profitMoney = profitMoney;
   }

   @Generated
   public void setProfitPlayerPoints(double profitPlayerPoints) {
      this.profitPlayerPoints = profitPlayerPoints;
   }

   @Generated
   public void setProfitTokenManager(double profitTokenManager) {
      this.profitTokenManager = profitTokenManager;
   }

   @Generated
   public void setProfitBeastTokens(double profitBeastTokens) {
      this.profitBeastTokens = profitBeastTokens;
   }

   @Generated
   public void setLossMoney(double lossMoney) {
      this.lossMoney = lossMoney;
   }

   @Generated
   public void setLossPlayerPoints(double lossPlayerPoints) {
      this.lossPlayerPoints = lossPlayerPoints;
   }

   @Generated
   public void setLossTokenManager(double lossTokenManager) {
      this.lossTokenManager = lossTokenManager;
   }

   @Generated
   public void setLossBeastTokens(double lossBeastTokens) {
      this.lossBeastTokens = lossBeastTokens;
   }

   @Generated
   public void setWinsMoney(int winsMoney) {
      this.winsMoney = winsMoney;
   }

   @Generated
   public void setDefeatsMoney(int defeatsMoney) {
      this.defeatsMoney = defeatsMoney;
   }

   @Generated
   public void setWinsPlayerPoints(int winsPlayerPoints) {
      this.winsPlayerPoints = winsPlayerPoints;
   }

   @Generated
   public void setDefeatsPlayerPoints(int defeatsPlayerPoints) {
      this.defeatsPlayerPoints = defeatsPlayerPoints;
   }

   @Generated
   public void setWinsTokenManager(int winsTokenManager) {
      this.winsTokenManager = winsTokenManager;
   }

   @Generated
   public void setDefeatsTokenManager(int defeatsTokenManager) {
      this.defeatsTokenManager = defeatsTokenManager;
   }

   @Generated
   public void setWinsBeastTokens(int winsBeastTokens) {
      this.winsBeastTokens = winsBeastTokens;
   }

   @Generated
   public void setDefeatsBeastTokens(int defeatsBeastTokens) {
      this.defeatsBeastTokens = defeatsBeastTokens;
   }

   @Generated
   public void setWinsPlaceholder(Map<String, Integer> winsPlaceholder) {
      this.winsPlaceholder = winsPlaceholder;
   }

   @Generated
   public void setDefeatsPlaceholder(Map<String, Integer> defeatsPlaceholder) {
      this.defeatsPlaceholder = defeatsPlaceholder;
   }

   @Generated
   public void setWinstreak(int winstreak) {
      this.winstreak = winstreak;
   }

   @Generated
   public PlayerStats() {
   }
}
