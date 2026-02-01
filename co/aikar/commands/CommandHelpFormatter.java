package co.aikar.commands;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CommandHelpFormatter {
   private final CommandManager manager;

   public CommandHelpFormatter(CommandManager manager) {
      this.manager = manager;
   }

   public void showAllResults(CommandHelp commandHelp, List<HelpEntry> entries) {
      CommandIssuer issuer = commandHelp.getIssuer();
      this.printHelpHeader(commandHelp, issuer);

      for (HelpEntry e : entries) {
         this.printHelpCommand(commandHelp, issuer, e);
      }

      this.printHelpFooter(commandHelp, issuer);
   }

   public void showSearchResults(CommandHelp commandHelp, List<HelpEntry> entries) {
      CommandIssuer issuer = commandHelp.getIssuer();
      this.printSearchHeader(commandHelp, issuer);

      for (HelpEntry e : entries) {
         this.printSearchEntry(commandHelp, issuer, e);
      }

      this.printSearchFooter(commandHelp, issuer);
   }

   public void showDetailedHelp(CommandHelp commandHelp, HelpEntry entry) {
      CommandIssuer issuer = commandHelp.getIssuer();
      this.printDetailedHelpCommand(commandHelp, issuer, entry);

      for (CommandParameter param : entry.getParameters()) {
         String description = param.getDescription();
         if (description != null && !description.isEmpty()) {
            this.printDetailedParameter(commandHelp, issuer, entry, param);
         }
      }
   }

   public void printHelpHeader(CommandHelp help, CommandIssuer issuer) {
      issuer.sendMessage(MessageType.HELP, MessageKeys.HELP_HEADER, this.getHeaderFooterFormatReplacements(help));
   }

   public void printHelpCommand(CommandHelp help, CommandIssuer issuer, HelpEntry entry) {
      String formatted = this.manager.formatMessage(issuer, MessageType.HELP, MessageKeys.HELP_FORMAT, this.getEntryFormatReplacements(help, entry));

      for (String msg : ACFPatterns.NEWLINE.split(formatted)) {
         issuer.sendMessageInternal(ACFUtil.rtrim(msg));
      }
   }

   public void printHelpFooter(CommandHelp help, CommandIssuer issuer) {
      if (!help.isOnlyPage()) {
         issuer.sendMessage(MessageType.HELP, MessageKeys.HELP_PAGE_INFORMATION, this.getHeaderFooterFormatReplacements(help));
      }
   }

   public void printSearchHeader(CommandHelp help, CommandIssuer issuer) {
      issuer.sendMessage(MessageType.HELP, MessageKeys.HELP_SEARCH_HEADER, this.getHeaderFooterFormatReplacements(help));
   }

   public void printSearchEntry(CommandHelp help, CommandIssuer issuer, HelpEntry page) {
      String formatted = this.manager.formatMessage(issuer, MessageType.HELP, MessageKeys.HELP_FORMAT, this.getEntryFormatReplacements(help, page));

      for (String msg : ACFPatterns.NEWLINE.split(formatted)) {
         issuer.sendMessageInternal(ACFUtil.rtrim(msg));
      }
   }

   public void printSearchFooter(CommandHelp help, CommandIssuer issuer) {
      if (!help.isOnlyPage()) {
         issuer.sendMessage(MessageType.HELP, MessageKeys.HELP_PAGE_INFORMATION, this.getHeaderFooterFormatReplacements(help));
      }
   }

   public void printDetailedHelpHeader(CommandHelp help, CommandIssuer issuer, HelpEntry entry) {
      issuer.sendMessage(MessageType.HELP, MessageKeys.HELP_DETAILED_HEADER, "{command}", entry.getCommand(), "{commandprefix}", help.getCommandPrefix());
   }

   public void printDetailedHelpCommand(CommandHelp help, CommandIssuer issuer, HelpEntry entry) {
      String formatted = this.manager
         .formatMessage(issuer, MessageType.HELP, MessageKeys.HELP_DETAILED_COMMAND_FORMAT, this.getEntryFormatReplacements(help, entry));

      for (String msg : ACFPatterns.NEWLINE.split(formatted)) {
         issuer.sendMessageInternal(ACFUtil.rtrim(msg));
      }
   }

   public void printDetailedParameter(CommandHelp help, CommandIssuer issuer, HelpEntry entry, CommandParameter param) {
      String formattedMsg = this.manager
         .formatMessage(issuer, MessageType.HELP, MessageKeys.HELP_DETAILED_PARAMETER_FORMAT, this.getParameterFormatReplacements(help, param, entry));

      for (String msg : ACFPatterns.NEWLINE.split(formattedMsg)) {
         issuer.sendMessageInternal(ACFUtil.rtrim(msg));
      }
   }

   public void printDetailedHelpFooter(CommandHelp help, CommandIssuer issuer, HelpEntry entry) {
   }

   public String[] getHeaderFooterFormatReplacements(CommandHelp help) {
      return new String[]{
         "{search}",
         help.search != null ? String.join(" ", help.search) : "",
         "{command}",
         help.getCommandName(),
         "{commandprefix}",
         help.getCommandPrefix(),
         "{rootcommand}",
         help.getCommandName(),
         "{page}",
         "" + help.getPage(),
         "{totalpages}",
         "" + help.getTotalPages(),
         "{results}",
         "" + help.getTotalResults()
      };
   }

   public String[] getEntryFormatReplacements(CommandHelp help, HelpEntry entry) {
      return new String[]{
         "{command}",
         entry.getCommand(),
         "{commandprefix}",
         help.getCommandPrefix(),
         "{parameters}",
         entry.getParameterSyntax(help.getIssuer()),
         "{separator}",
         entry.getDescription().isEmpty() ? "" : "-",
         "{description}",
         entry.getDescription()
      };
   }

   @NotNull
   public String[] getParameterFormatReplacements(CommandHelp help, CommandParameter param, HelpEntry entry) {
      return new String[]{
         "{name}",
         param.getDisplayName(help.getIssuer()),
         "{syntaxorname}",
         ACFUtil.nullDefault(param.getSyntax(help.getIssuer()), param.getDisplayName(help.getIssuer())),
         "{syntax}",
         ACFUtil.nullDefault(param.getSyntax(help.getIssuer()), ""),
         "{description}",
         ACFUtil.nullDefault(param.getDescription(), ""),
         "{command}",
         help.getCommandName(),
         "{fullcommand}",
         entry.getCommand(),
         "{commandprefix}",
         help.getCommandPrefix()
      };
   }
}
