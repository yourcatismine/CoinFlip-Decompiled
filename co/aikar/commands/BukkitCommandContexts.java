package co.aikar.commands;

import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Contract;

public class BukkitCommandContexts extends CommandContexts<BukkitCommandExecutionContext> {
   public BukkitCommandContexts(BukkitCommandManager manager) {
      super(manager);
      this.registerContext(OnlinePlayer.class, c -> this.getOnlinePlayer(c.getIssuer(), c.popFirstArg(), false));
      this.registerContext(co.aikar.commands.contexts.OnlinePlayer.class, c -> {
         OnlinePlayer onlinePlayer = this.getOnlinePlayer(c.getIssuer(), c.popFirstArg(), false);
         return new co.aikar.commands.contexts.OnlinePlayer(onlinePlayer.getPlayer());
      });
      this.registerContext(OnlinePlayer[].class, c -> {
         BukkitCommandIssuer issuer = c.getIssuer();
         String search = c.popFirstArg();
         boolean allowMissing = c.hasFlag("allowmissing");
         Set<OnlinePlayer> players = new HashSet<>();
         Pattern split = ACFPatterns.COMMA;
         String splitter = c.getFlagValue("splitter", (String)null);
         if (splitter != null) {
            split = Pattern.compile(Pattern.quote(splitter));
         }

         for (String lookup : split.split(search)) {
            OnlinePlayer player = this.getOnlinePlayer(issuer, lookup, allowMissing);
            if (player != null) {
               players.add(player);
            }
         }

         if (players.isEmpty() && !c.hasFlag("allowempty")) {
            issuer.sendError(MinecraftMessageKeys.NO_PLAYER_FOUND_SERVER, "{search}", search);
            throw new InvalidCommandArgument(false);
         } else {
            return players.toArray(new OnlinePlayer[players.size()]);
         }
      });
      this.registerIssuerAwareContext(World.class, c -> {
         String firstArg = c.getFirstArg();
         World world = firstArg != null ? Bukkit.getWorld(firstArg) : null;
         if (world != null) {
            c.popFirstArg();
         }

         if (world == null && c.getSender() instanceof Player) {
            world = ((Entity)c.getSender()).getWorld();
         }

         if (world == null) {
            throw new InvalidCommandArgument(MinecraftMessageKeys.INVALID_WORLD);
         } else {
            return world;
         }
      });
      this.registerIssuerAwareContext(CommandSender.class, BukkitCommandExecutionContext::getSender);
      this.registerIssuerAwareContext(Player.class, c -> {
         boolean isOptional = c.isOptional();
         CommandSender sender = c.getSender();
         boolean isPlayerSender = sender instanceof Player;
         if (!c.hasFlag("other")) {
            Player player = isPlayerSender ? (Player)sender : null;
            if (player == null && !isOptional) {
               throw new InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE, false);
            } else {
               PlayerInventory inventory = player != null ? player.getInventory() : null;
               if (inventory != null && c.hasFlag("itemheld") && !ACFBukkitUtil.isValidItem(inventory.getItem(inventory.getHeldItemSlot()))) {
                  throw new InvalidCommandArgument(MinecraftMessageKeys.YOU_MUST_BE_HOLDING_ITEM, false);
               } else {
                  return player;
               }
            }
         } else {
            String arg = c.popFirstArg();
            if (arg == null && isOptional) {
               if (!c.hasFlag("defaultself")) {
                  return null;
               } else if (isPlayerSender) {
                  return (Player)sender;
               } else {
                  throw new InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE, false);
               }
            } else if (arg == null) {
               throw new InvalidCommandArgument();
            } else {
               OnlinePlayer onlinePlayer = this.getOnlinePlayer(c.getIssuer(), arg, false);
               return onlinePlayer.getPlayer();
            }
         }
      });
      this.registerContext(OfflinePlayer.class, c -> {
         String name = c.popFirstArg();
         OfflinePlayer offlinePlayer;
         if (c.hasFlag("uuid")) {
            UUID uuid;
            try {
               uuid = UUID.fromString(name);
            } catch (IllegalArgumentException var6) {
               throw new InvalidCommandArgument(MinecraftMessageKeys.NO_PLAYER_FOUND_OFFLINE, "{search}", name);
            }

            offlinePlayer = Bukkit.getOfflinePlayer(uuid);
         } else {
            offlinePlayer = Bukkit.getOfflinePlayer(name);
         }

         if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            if (!c.hasFlag("uuid") && !manager.isValidName(name)) {
               throw new InvalidCommandArgument(MinecraftMessageKeys.IS_NOT_A_VALID_NAME, "{name}", name);
            } else {
               throw new InvalidCommandArgument(MinecraftMessageKeys.NO_PLAYER_FOUND_OFFLINE, "{search}", name);
            }
         } else {
            return offlinePlayer;
         }
      });
      this.registerContext(
         ChatColor.class,
         c -> {
            String first = c.popFirstArg();
            Stream<ChatColor> colors = Stream.of(ChatColor.values());
            if (c.hasFlag("colorsonly")) {
               colors = colors.filter(color -> color.ordinal() <= 15);
            }

            String filter = c.getFlagValue("filter", (String)null);
            if (filter != null) {
               filter = ACFUtil.simplifyString(filter);
               colors = colors.filter(color -> filter.equals(ACFUtil.simplifyString(color.name())));
            }

            ChatColor match = ACFUtil.simpleMatch(ChatColor.class, first);
            if (match == null) {
               String valid = colors.<CharSequence>map(color -> "<c2>" + ACFUtil.simplifyString(color.name()) + "</c2>")
                  .collect(Collectors.joining("<c1>,</c1> "));
               throw new InvalidCommandArgument(MessageKeys.PLEASE_SPECIFY_ONE_OF, "{valid}", valid);
            } else {
               return match;
            }
         }
      );
      this.registerContext(Location.class, c -> {
         String input = c.popFirstArg();
         CommandSender sender = c.getSender();
         String[] split = ACFPatterns.COLON.split(input, 2);
         if (split.length == 0) {
            throw new InvalidCommandArgument(true);
         } else if (split.length < 2 && !(sender instanceof Player) && !(sender instanceof BlockCommandSender)) {
            throw new InvalidCommandArgument(MinecraftMessageKeys.LOCATION_PLEASE_SPECIFY_WORLD);
         } else {
            Location sourceLoc = null;
            String world;
            String rest;
            if (split.length == 2) {
               world = split[0];
               rest = split[1];
            } else if (sender instanceof Player) {
               sourceLoc = ((Player)sender).getLocation();
               world = sourceLoc.getWorld().getName();
               rest = split[0];
            } else {
               if (!(sender instanceof BlockCommandSender)) {
                  throw new InvalidCommandArgument(true);
               }

               sourceLoc = ((BlockCommandSender)sender).getBlock().getLocation();
               world = sourceLoc.getWorld().getName();
               rest = split[0];
            }

            boolean rel = rest.startsWith("~");
            split = ACFPatterns.COMMA.split(rel ? rest.substring(1) : rest);
            if (split.length < 3) {
               throw new InvalidCommandArgument(MinecraftMessageKeys.LOCATION_PLEASE_SPECIFY_XYZ);
            } else {
               Double x = ACFUtil.parseDouble(split[0], rel ? 0.0 : null);
               Double y = ACFUtil.parseDouble(split[1], rel ? 0.0 : null);
               Double z = ACFUtil.parseDouble(split[2], rel ? 0.0 : null);
               if (sourceLoc != null && rel) {
                  x = x + sourceLoc.getX();
                  y = y + sourceLoc.getY();
                  z = z + sourceLoc.getZ();
               } else if (rel) {
                  throw new InvalidCommandArgument(MinecraftMessageKeys.LOCATION_CONSOLE_NOT_RELATIVE);
               }

               if (x != null && y != null && z != null) {
                  World worldObj = Bukkit.getWorld(world);
                  if (worldObj == null) {
                     throw new InvalidCommandArgument(MinecraftMessageKeys.INVALID_WORLD);
                  } else if (split.length >= 5) {
                     Float yaw = ACFUtil.parseFloat(split[3]);
                     Float pitch = ACFUtil.parseFloat(split[4]);
                     if (pitch != null && yaw != null) {
                        return new Location(worldObj, x, y, z, yaw, pitch);
                     } else {
                        throw new InvalidCommandArgument(MinecraftMessageKeys.LOCATION_PLEASE_SPECIFY_XYZ);
                     }
                  } else {
                     return new Location(worldObj, x, y, z);
                  }
               } else {
                  throw new InvalidCommandArgument(MinecraftMessageKeys.LOCATION_PLEASE_SPECIFY_XYZ);
               }
            }
         }
      });
      if (manager.mcMinorVersion >= 12) {
         BukkitCommandContexts_1_12.register(this);
      }
   }

   @Contract("_,_,false -> !null")
   OnlinePlayer getOnlinePlayer(BukkitCommandIssuer issuer, String lookup, boolean allowMissing) throws InvalidCommandArgument {
      Player player = ACFBukkitUtil.findPlayerSmart(issuer, lookup);
      if (player == null) {
         if (allowMissing) {
            return null;
         } else {
            throw new InvalidCommandArgument(false);
         }
      } else {
         return new OnlinePlayer(player);
      }
   }
}
