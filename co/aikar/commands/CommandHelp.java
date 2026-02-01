package co.aikar.commands;

import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandHelp {
   private final CommandManager manager;
   private final CommandIssuer issuer;
   private final List<HelpEntry> helpEntries = new ArrayList<>();
   private final String commandName;
   final String commandPrefix;
   private int page = 1;
   private int perPage;
   List<String> search;
   private Set<HelpEntry> selectedEntry = new HashSet<>();
   private int totalResults;
   private int totalPages;
   private boolean lastPage;

   public CommandHelp(CommandManager manager, RootCommand rootCommand, CommandIssuer issuer) {
      this.manager = manager;
      this.issuer = issuer;
      this.perPage = manager.defaultHelpPerPage;
      this.commandPrefix = manager.getCommandPrefix(issuer);
      this.commandName = rootCommand.getCommandName();
      SetMultimap<String, RegisteredCommand> subCommands = rootCommand.getSubCommands();
      Set<RegisteredCommand> seen = new HashSet<>();
      if (!rootCommand.getDefCommand().hasHelpCommand) {
         RegisteredCommand defCommand = rootCommand.getDefaultRegisteredCommand();
         if (defCommand != null) {
            this.helpEntries.add(new HelpEntry(this, defCommand));
            seen.add(defCommand);
         }
      }

      subCommands.entries().forEach(e -> {
         String key = (String)e.getKey();
         if (!key.equals("__default") && !key.equals("__catchunknown")) {
            RegisteredCommand regCommand = (RegisteredCommand)e.getValue();
            if (!regCommand.isPrivate && regCommand.hasPermission(issuer) && !seen.contains(regCommand)) {
               this.helpEntries.add(new HelpEntry(this, regCommand));
               seen.add(regCommand);
            }
         }
      });
   }

   @UnstableAPI
   protected void updateSearchScore(HelpEntry help) {
      if (this.search != null && !this.search.isEmpty()) {
         RegisteredCommand<?> cmd = help.getRegisteredCommand();
         int searchScore = 0;

         for (String word : this.search) {
            Pattern pattern = Pattern.compile(".*" + Pattern.quote(word) + ".*", 2);

            for (String subCmd : cmd.registeredSubcommands) {
               Pattern subCmdPattern = Pattern.compile(".*" + Pattern.quote(subCmd) + ".*", 2);
               if (pattern.matcher(subCmd).matches()) {
                  searchScore += 3;
               } else if (subCmdPattern.matcher(word).matches()) {
                  searchScore++;
               }
            }

            if (pattern.matcher(help.getDescription()).matches()) {
               searchScore += 2;
            }

            if (pattern.matcher(help.getParameterSyntax(this.issuer)).matches()) {
               searchScore++;
            }

            if (help.getSearchTags() != null && pattern.matcher(help.getSearchTags()).matches()) {
               searchScore += 2;
            }
         }

         help.setSearchScore(searchScore);
      } else {
         help.setSearchScore(1);
      }
   }

   public CommandManager getManager() {
      return this.manager;
   }

   public boolean testExactMatch(String command) {
      this.selectedEntry.clear();

      for (HelpEntry helpEntry : this.helpEntries) {
         if (helpEntry.getCommand().endsWith(" " + command)) {
            this.selectedEntry.add(helpEntry);
         }
      }

      return !this.selectedEntry.isEmpty();
   }

   public void showHelp() {
      this.showHelp(this.issuer);
   }

   public void showHelp(CommandIssuer issuer) {
      CommandHelpFormatter formatter = this.manager.getHelpFormatter();
      if (!this.selectedEntry.isEmpty()) {
         HelpEntry first = ACFUtil.getFirstElement(this.selectedEntry);
         formatter.printDetailedHelpHeader(this, issuer, first);

         for (HelpEntry helpEntry : this.selectedEntry) {
            formatter.showDetailedHelp(this, helpEntry);
         }

         formatter.printDetailedHelpFooter(this, issuer, first);
      } else {
         List<HelpEntry> helpEntries = this.getHelpEntries().stream().filter(HelpEntry::shouldShow).collect(Collectors.toList());
         Iterator<HelpEntry> results = helpEntries.stream().sorted(Comparator.comparingInt(helpEntry -> helpEntry.getSearchScore() * -1)).iterator();
         if (!results.hasNext()) {
            issuer.sendMessage(MessageType.ERROR, MessageKeys.NO_COMMAND_MATCHED_SEARCH, "{search}", ACFUtil.join(this.search, " "));
            helpEntries = this.getHelpEntries();
            results = helpEntries.iterator();
         }

         this.totalResults = helpEntries.size();
         int min = (this.page - 1) * this.perPage;
         int max = min + this.perPage;
         this.totalPages = (int)Math.ceil((float)this.totalResults / this.perPage);
         int i = 0;
         if (min >= this.totalResults) {
            issuer.sendMessage(MessageType.HELP, MessageKeys.HELP_NO_RESULTS);
         } else {
            List<HelpEntry> printEntries = new ArrayList<>();

            while (results.hasNext()) {
               HelpEntry e = results.next();
               if (i >= max) {
                  break;
               }

               if (i++ >= min) {
                  printEntries.add(e);
               }
            }

            this.lastPage = max >= this.totalResults;
            if (this.search == null) {
               formatter.showAllResults(this, printEntries);
            } else {
               formatter.showSearchResults(this, printEntries);
            }
         }
      }
   }

   public List<HelpEntry> getHelpEntries() {
      return this.helpEntries;
   }

   public void setPerPage(int perPage) {
      this.perPage = perPage;
   }

   public void setPage(int page) {
      this.page = page;
   }

   public void setPage(int page, int perPage) {
      this.setPage(page);
      this.setPerPage(perPage);
   }

   public void setSearch(List<String> search) {
      this.search = search;
      this.getHelpEntries().forEach(this::updateSearchScore);
   }

   public CommandIssuer getIssuer() {
      return this.issuer;
   }

   public String getCommandName() {
      return this.commandName;
   }

   public String getCommandPrefix() {
      return this.commandPrefix;
   }

   public int getPage() {
      return this.page;
   }

   public int getPerPage() {
      return this.perPage;
   }

   public List<String> getSearch() {
      return this.search;
   }

   public Set<HelpEntry> getSelectedEntry() {
      return this.selectedEntry;
   }

   public int getTotalResults() {
      return this.totalResults;
   }

   public int getTotalPages() {
      return this.totalPages;
   }

   public boolean isOnlyPage() {
      return this.page == 1 && this.lastPage;
   }

   public boolean isLastPage() {
      return this.lastPage;
   }
}
