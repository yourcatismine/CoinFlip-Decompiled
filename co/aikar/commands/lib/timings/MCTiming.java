package co.aikar.commands.lib.timings;

public abstract class MCTiming implements AutoCloseable {
   public abstract MCTiming startTiming();

   public abstract void stopTiming();

   @Override
   public void close() {
      this.stopTiming();
   }
}
