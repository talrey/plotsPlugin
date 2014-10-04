/**
 * Msg.java
 * Static class that stores all the various messages printed to a user.
 * Has no methods to call. Essentially just a message dictionary.
 * @author Morios (Mark Talrey)
 * @version RC.3.1.5 for Minecraft 1.7.10
 */

package plots;

public final class Msg
{
	protected static final String FORMAT = "§d§r"; //minecraft color & format codes to set plugin style
	protected static final String CRFORM = "§d§r";
	protected static final String PREFIX = FORMAT + "[ArcanePlots]: ";
	protected static final String CRPREF = CRFORM + "";
	
	// error messages
	protected static final String ERR_ARG_MISSING = "This command requires an argument.";
	protected static final String ERR_ARG_INVALID = "Invalid argument.";
	protected static final String ERR_ARGS_INVALID = "Invalid arguments.";
	
	protected static final String ERR_NO_PERM = "You do not have permission to do that.";
	protected static final String ERR_NO_PLOT = "There is no Plot here.";
	protected static final String ERR_NO_PLOTS = "There are no plots at all.";
	protected static final String ERR_NO_TARGET = "Specify a player for this command.";
	protected static final String ERR_NO_TEMP = "You have no action waiting to be confirmed or cancelled.";
	
	protected static final String ERR_NOT_EDITOR = "You cannot modify this Plot.";
	protected static final String ERR_NOT_FOUND = "No player was found with that name.";
	protected static final String ERR_NOT_PLAYER = "You must be a player to use this command.";
	protected static final String ERR_PLAYER_OFFLINE = "That player is currently offline!";
	
	protected static final String ERR_REMOVE_PLAYER = "A Plot requires at least one editor.";
	protected static final String ERR_REMOVE_OWNER = "You cannot remove this plot's owner.";
	protected static final String ERR_NAME_MISMATCH = "A player's stored name was wrong. Corrected.";
	
	protected static final String ERR_PLOT_EXISTS = "There is already a Plot here.";
	protected static final String ERR_PLOT_OVERLAP = "This plot overlaps with an existing Plot.";
	
	protected static final String ERR_RADIUS_NUM = "Radius must be an integer value.";
	protected static final String ERR_RADIUS_SIZE = "Radius is outside permitted range.";
	
	//protected static final String ERR_HELP_ARG = "Use /plot help [command] for more information.";
	
	protected static final String ERR_CRED_POOR = "sorry, you don't have enough credits.";
	protected static final String ERR_CRED_REPUBLIC = "No good out here. I need something more real.";
	protected static final String ERR_CRED_ISNEG = "hey, that's stealing! Use a positive number.";
	protected static final String ERR_CRED_SELF = "Sending yourself money won't do anything.";
	
	// I/O messages
	protected static final String ERR_LOAD = "Failed to load plot data!";
	protected static final String ERR_SAVE = "Failed to save plot data!";
	
	// success messages
	protected static final String DONE_PLOT_SET = "Plot created.";
	protected static final String DONE_PLOT_REMOVE = "Plot removed.";
	protected static final String DONE_PLOT_CANCEL = "Plot action cancelled.";
	
	protected static final String DONE_EDITOR_ADD = /* playername */" can now edit this Plot.";
	protected static final String DONE_EDITOR_REMOVE = /* playername */" can no longer edit this Plot.";
	
	protected static final String DONE_EDIT_ON = "This Plot is now active.";
	protected static final String DONE_EDIT_OFF = "This Plot is now inactive.";
	
	protected static final String DONE_CRED_BAL = "Your current balance: "; /* number */
	protected static final String DONE_CRED_SENT = "Credits successfully transferred.";
	protected static final String DONE_CRED_CANCEL = "Credit tranfer cancelled.";
	
	// status messages
	protected static final String STAT_SEL_ACTIVE = "Area selection active. Punch a block.";
	protected static final String STAT_SEL_SECOND = "Now punch another block to finish.";
	protected static final String STAT_SEL_RESET = "Area selection reset. Punch a block.";
	protected static final String STAT_SEL_CANCEL = "Area selection cancelled.";
	
	protected static final String STAT_PLOT_SET = "Please type /confirm to buy a Plot at: "; /* location */
	protected static final String STAT_PLOT_REM = "Please type /confirm to sell the Plot here.";
	protected static final String STAT_PLOT_CANCEL = " or type /cancel to abort."; // use after above.
	protected static final String STAT_PLOT_ACTIVE = "This plot is active and cannot be edited.";

	protected static final String STAT_CRED_TRANS = "Transferring "; /* credits */
	protected static final String STAT_CRED_TRAN2 = " credits to "; /* name */
	protected static final String STAT_CRED_TRAN3 = ". Please type /confirm or /cancel.";
	protected static final String STAT_CRED_PRICE = "Credit cost of this action: "; /* number */
	protected static final String STAT_CRED_REFUND = "Credits refunded from this action: "; /* number */
	
	protected static final String STAT_LISTALL = "Listing all Plots' owners...";
	
	// help messages
	protected static final String HELP_PLOT ="plot: Arcane Plots' main command. Defaults to 'plot list'.";
	protected static final String HELP_BUY = "  buy: create a plot. Options are [corners | radius #]";
	protected static final String HELP_SELL ="  sell: delete a plot. You must be the owner.";
	protected static final String HELP_ADD = "  add: add a player to the editors list.";
	protected static final String HELP_REM = "  remove: take a player off the editors list.";
	protected static final String HELP_LIST ="  list: check the editor list. Must be in a plot.";
	protected static final String HELP_YES = "  confirm: complete an action. Aliased to /confirm.";
	protected static final String HELP_NO  = "  cancel: abort an action. Aliased to /cancel.";
	protected static final String HELP_CRED ="credits: See your current balance.";
	protected static final String HELP_TRAN ="  give: transfer credits to another player.";
	
}
