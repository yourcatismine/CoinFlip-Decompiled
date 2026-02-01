package co.aikar.commands.bukkit.contexts;

import java.util.Objects;
import org.bukkit.entity.Player;

public class OnlinePlayer {
   public final Player player;

   public OnlinePlayer(Player player) {
      this.player = player;
   }

   public Player getPlayer() {
      return this.player;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         OnlinePlayer that = (OnlinePlayer)o;
         return Objects.equals(this.player, that.player);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.player);
   }

   @Override
   public String toString() {
      return "OnlinePlayer{player=" + this.player + '}';
   }
}
