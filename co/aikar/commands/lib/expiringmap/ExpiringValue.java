package co.aikar.commands.lib.expiringmap;

import java.util.concurrent.TimeUnit;

public final class ExpiringValue<V> {
   private static final long UNSET_DURATION = -1L;
   private final V value;
   private final ExpirationPolicy expirationPolicy;
   private final long duration;
   private final TimeUnit timeUnit;

   public ExpiringValue(V value) {
      this(value, -1L, null, null);
   }

   public ExpiringValue(V value, ExpirationPolicy expirationPolicy) {
      this(value, -1L, null, expirationPolicy);
   }

   public ExpiringValue(V value, long duration, TimeUnit timeUnit) {
      this(value, duration, timeUnit, null);
      if (timeUnit == null) {
         throw new NullPointerException();
      }
   }

   public ExpiringValue(V value, ExpirationPolicy expirationPolicy, long duration, TimeUnit timeUnit) {
      this(value, duration, timeUnit, expirationPolicy);
      if (timeUnit == null) {
         throw new NullPointerException();
      }
   }

   private ExpiringValue(V value, long duration, TimeUnit timeUnit, ExpirationPolicy expirationPolicy) {
      this.value = value;
      this.expirationPolicy = expirationPolicy;
      this.duration = duration;
      this.timeUnit = timeUnit;
   }

   public V getValue() {
      return this.value;
   }

   public ExpirationPolicy getExpirationPolicy() {
      return this.expirationPolicy;
   }

   public long getDuration() {
      return this.duration;
   }

   public TimeUnit getTimeUnit() {
      return this.timeUnit;
   }

   @Override
   public int hashCode() {
      return this.value != null ? this.value.hashCode() : 0;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ExpiringValue<?> that = (ExpiringValue<?>)o;
         return (this.value != null ? this.value.equals(that.value) : that.value == null)
            && this.expirationPolicy == that.expirationPolicy
            && this.duration == that.duration
            && this.timeUnit == that.timeUnit;
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return "ExpiringValue{value="
         + this.value
         + ", expirationPolicy="
         + this.expirationPolicy
         + ", duration="
         + this.duration
         + ", timeUnit="
         + this.timeUnit
         + '}';
   }
}
