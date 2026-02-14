package com.kstudio.ultracoinflip.commands;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.CommandCompletions;
import com.kstudio.ultracoinflip.KStudio;
import java.util.ArrayList;
import java.util.List;

public class ACFCompletions {
   public static void registerCompletions(KStudio plugin,
         CommandCompletions<BukkitCommandCompletionContext> completions) {
      completions.registerCompletion("currencies", c -> plugin.getCurrencyManager().getAllSyntaxCommands());
      completions.registerCompletion("smart-amounts", c -> {
         List<String> suggestions = new ArrayList<>();
         suggestions.add("100");
         suggestions.add("500");
         suggestions.add("1k");
         suggestions.add("5k");
         suggestions.add("10k");
         suggestions.add("50k");
         suggestions.add("100k");
         suggestions.add("500k");
         suggestions.add("1M");
         suggestions.add("5M");
         suggestions.add("10M");
         return suggestions;
      });
   }
}
