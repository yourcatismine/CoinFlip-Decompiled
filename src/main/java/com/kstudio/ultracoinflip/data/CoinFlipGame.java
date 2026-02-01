package com.kstudio.ultracoinflip.data;

import java.util.UUID;
import lombok.Generated;
import org.bukkit.entity.Player;

public class CoinFlipGame {
   private final UUID gameId;
   private final Player host;
   private final UUID hostUuid;
   private final CoinFlipGame.CurrencyType currencyType;
   private final String currencyId;
   private final double amount;
   private final long createdAt;
   private final Boolean headsChoice;

   public CoinFlipGame(Player host, CoinFlipGame.CurrencyType currencyType, double amount) {
      this(UUID.randomUUID(), host, currencyType, null, amount, System.currentTimeMillis(), null);
   }

   public CoinFlipGame(Player host, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount) {
      this(UUID.randomUUID(), host, currencyType, currencyId, amount, System.currentTimeMillis(), null);
   }

   public CoinFlipGame(Player host, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount, Boolean headsChoice) {
      this(UUID.randomUUID(), host, currencyType, currencyId, amount, System.currentTimeMillis(), headsChoice);
   }

   public CoinFlipGame(UUID gameId, Player host, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount, long createdAt) {
      this(gameId, host, currencyType, currencyId, amount, createdAt, null);
   }

   public CoinFlipGame(UUID gameId, Player host, CoinFlipGame.CurrencyType currencyType, String currencyId, double amount, long createdAt, Boolean headsChoice) {
      this.gameId = gameId;
      this.host = host;
      this.hostUuid = host != null ? host.getUniqueId() : null;
      this.currencyType = currencyType;
      this.currencyId = currencyId;
      this.amount = amount;
      this.createdAt = createdAt;
      this.headsChoice = headsChoice;
   }

   @Generated
   public UUID getGameId() {
      return this.gameId;
   }

   @Generated
   public Player getHost() {
      return this.host;
   }

   @Generated
   public UUID getHostUuid() {
      return this.hostUuid;
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
   public long getCreatedAt() {
      return this.createdAt;
   }

   @Generated
   public Boolean getHeadsChoice() {
      return this.headsChoice;
   }

   public static enum CurrencyType {
      MONEY,
      PLAYERPOINTS,
      TOKENMANAGER,
      BEASTTOKENS,
      COINSENGINE,
      PLACEHOLDER;
   }
}
