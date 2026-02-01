package co.aikar.commands;

public class HelpEntry {
   private final CommandHelp commandHelp;
   private final RegisteredCommand command;
   private int searchScore = 1;

   HelpEntry(CommandHelp commandHelp, RegisteredCommand command) {
      this.commandHelp = commandHelp;
      this.command = command;
   }

   RegisteredCommand getRegisteredCommand() {
      return this.command;
   }

   public String getCommand() {
      return this.command.command;
   }

   public String getCommandPrefix() {
      return this.commandHelp.getCommandPrefix();
   }

   public String getParameterSyntax() {
      return this.getParameterSyntax(null);
   }

   public String getParameterSyntax(CommandIssuer issuer) {
      String translated = this.command.getSyntaxText(issuer);
      return translated != null ? translated : "";
   }

   public String getDescription() {
      return this.command.getHelpText();
   }

   public void setSearchScore(int searchScore) {
      this.searchScore = searchScore;
   }

   public boolean shouldShow() {
      return this.searchScore > 0;
   }

   public int getSearchScore() {
      return this.searchScore;
   }

   public String getSearchTags() {
      return this.command.helpSearchTags;
   }

   public CommandParameter[] getParameters() {
      return this.command.parameters;
   }
}
