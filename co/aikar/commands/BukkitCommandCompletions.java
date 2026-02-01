package co.aikar.commands;

import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.StringUtil;

public class BukkitCommandCompletions extends CommandCompletions<BukkitCommandCompletionContext> {
   public BukkitCommandCompletions(BukkitCommandManager manager) {
      super(manager);
      this.registerAsyncCompletion("mobs", c -> {
         Stream<String> normal = Stream.of(EntityType.values()).map(entityType -> ACFUtil.simplifyString(entityType.getName()));
         return normal.collect(Collectors.toList());
      });
      this.registerAsyncCompletion("chatcolors", c -> {
         Stream<ChatColor> colors = Stream.of(ChatColor.values());
         if (c.hasConfig("colorsonly")) {
            colors = colors.filter(color -> color.ordinal() <= 15);
         }

         String filter = c.getConfig("filter");
         if (filter != null) {
            Set<String> filters = Arrays.stream(ACFPatterns.COLON.split(filter)).map(ACFUtil::simplifyString).collect(Collectors.toSet());
            colors = colors.filter(color -> filters.contains(ACFUtil.simplifyString(color.name())));
         }

         return colors.<String>map(color -> ACFUtil.simplifyString(color.name())).collect(Collectors.toList());
      });
      this.registerAsyncCompletion("dyecolors", c -> ACFUtil.enumNames(DyeColor.values()));
      this.registerCompletion("worlds", c -> Bukkit.getWorlds().stream().<String>map(WorldInfo::getName).collect(Collectors.toList()));
      this.registerCompletion("players", c -> {
         CommandSender sender = c.getSender();
         Validate.notNull(sender, "Sender cannot be null");
         Player senderPlayer = sender instanceof Player ? (Player)sender : null;
         ArrayList<String> matchedPlayers = new ArrayList<>();

         for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if ((senderPlayer == null || senderPlayer.canSee(player)) && StringUtil.startsWithIgnoreCase(name, c.getInput())) {
               matchedPlayers.add(name);
            }
         }

         matchedPlayers.sort(String.CASE_INSENSITIVE_ORDER);
         return matchedPlayers;
      });
      this.setDefaultCompletion("players", OnlinePlayer.class, co.aikar.commands.contexts.OnlinePlayer.class, Player.class);
      this.setDefaultCompletion("worlds", World.class);
   }
}
