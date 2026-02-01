package co.aikar.commands;

import java.util.concurrent.atomic.AtomicInteger;

public class MessageType {
   private static final AtomicInteger counter = new AtomicInteger(1);
   public static final MessageType INFO = new MessageType();
   public static final MessageType SYNTAX = new MessageType();
   public static final MessageType ERROR = new MessageType();
   public static final MessageType HELP = new MessageType();
   private final int id = counter.getAndIncrement();

   @Override
   public int hashCode() {
      return this.id;
   }

   @Override
   public boolean equals(Object o) {
      return this == o;
   }
}
