package com.kstudio.ultracoinflip.adventure;

import java.util.Map;
import org.bukkit.entity.Player;

public interface TextEngine {
   void send(Player var1, String var2);

   void send(Player var1, String var2, Map<String, String> var3);

   String toLegacy(String var1);

   String toLegacy(String var1, Map<String, String> var2);

   boolean supportsInteractive();
}
