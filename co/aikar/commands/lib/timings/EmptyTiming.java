package co.aikar.commands.lib.timings;

class EmptyTiming extends MCTiming {
   @Override
   public final MCTiming startTiming() {
      return this;
   }

   @Override
   public final void stopTiming() {
   }
}
