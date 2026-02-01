package co.aikar.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.help.GenericCommandHelpTopic;

public class ACFBukkitHelpTopic extends GenericCommandHelpTopic {
   public ACFBukkitHelpTopic(BukkitCommandManager manager, BukkitRootCommand command) {
      super(command);
      final List<String> messages = new ArrayList<>();
      BukkitCommandIssuer captureIssuer = new BukkitCommandIssuer(manager, Bukkit.getConsoleSender()) {
         @Override
         public void sendMessageInternal(String message) {
            messages.add(message);
         }
      };
      manager.generateCommandHelp(captureIssuer, command).showHelp(captureIssuer);
      this.fullText = ACFUtil.join(messages, "\n");
   }
}
