package co.aikar.commands;

import java.util.ArrayList;
import java.util.List;

public class ShowCommandHelp extends InvalidCommandArgument {
   List<String> searchArgs = null;
   boolean search = false;

   public ShowCommandHelp() {
   }

   public ShowCommandHelp(boolean search) {
      this.search = search;
   }

   public ShowCommandHelp(List<String> args) {
      this(true);
      this.searchArgs = new ArrayList<>(args);
   }
}
