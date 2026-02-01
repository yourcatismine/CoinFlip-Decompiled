package com.kstudio.ultracoinflip.refund;

public enum RefundResult {
   SUCCESS("SUCCESS"),
   ROLLBACK("ROLLBACK"),
   FAILED("FAILED"),
   COOLDOWN("COOLDOWN"),
   BLOCKED("BLOCKED"),
   NOT_FOUND("NOT_FOUND");

   private final String displayName;

   private RefundResult(String displayName) {
      this.displayName = displayName;
   }

   public String getDisplayName() {
      return this.displayName;
   }

   public boolean isSuccess() {
      return this == SUCCESS;
   }

   public boolean isBlocked() {
      return this == COOLDOWN || this == BLOCKED;
   }
}
