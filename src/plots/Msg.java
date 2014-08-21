/**
 * Msg.java
 * Static class that stores all the various messages printed to a user.
 * Has no methods to call. Essentially just a message dictionary.
 * @author Morios (Mark Talrey)
 * @version RC.1 for Minecraft 1.7.10
 */

package plots;

public final class Msg
{
	private static final String FORMAT = "§2§O"; //minecraft color & format codes to set plugin info style.
	protected static final String PREFIX = FORMAT + "[ArcanePlotsPlugin]: ";
	
	//help messages
	protected static final String PLOT_HELP_SET =  //IMPL default behavior?
		"Creates a new Plot. Arguments are [blank], 'corners', or 'radius #'." +
		" Leaving blank creates a cube of radius 10.";
	protected static final String PLOT_HELP_UNSET =
		"Removes the current Plot." +
		" 'unset all' removes every Plot and requires elevated permission or the console.";
	protected static final String PLOT_HELP_ALLOW = 
		"Adds a player to the current Plot's editor group. Argument is a player's name.";
	protected static final String PLOT_HELP_UNALLOW =
		"Removes a player from the current Plot's editor group. Argument is a player's name.";
	protected static final String PLOT_HELP_LIST =
		"Lists the members of the current Plot's editor group." +
		" 'list all' will list every Plot's group, and requires elevated permission or the console.";
	protected static final String PLOT_HELP_EDIT =
		"Sets whether the current Plot is active or not. Arguments are 'on' or 'off'." +
		" Leave blank to toggle state. Only active Plots will lock blocks brom being added or removed.";
	protected static final String PLOT_HELP_HELP =
		"Displays help messages like this. What did you expect?" +
		" Arguments are 'set', 'unset', 'allow', 'unallow', 'list', 'edit', and 'help'...";
	
	//error messages
	protected static final String ERR_ARG_MISSING = "This command requires an argument.";
	protected static final String ERR_ARG_INVALID = "Invalid argument.";
	protected static final String ERR_ARGS_INVALID = "Invalid arguments.";
	
	protected static final String ERR_NO_PERM = "You do not have permission to do that.";
	protected static final String ERR_NO_PLOT = "There is no Plot here.";
	protected static final String ERR_NO_PLOTS = "There are no plots at all.";
	protected static final String ERR_NO_TARGET = "Specify a player for this command.";
	
	protected static final String ERR_NOT_EDITOR = "You cannot modify this Plot.";
	protected static final String ERR_NOT_FOUND = "No player was found with that name.";
	protected static final String ERR_NOT_PLAYER = "You must be a player to use this command.";
	
	protected static final String ERR_REMOVE_PLAYER = "A Plot requires at least one editor.";
	
	protected static final String ERR_PLOT_EXISTS = "There is already a Plot here.";
	protected static final String ERR_PLOT_OVERLAP = "This plot overlaps with an existing Plot.";
	
	protected static final String ERR_RADIUS_NUM = "Radius must be an integer value.";
	protected static final String ERR_RADIUS_SIZE = "Radius is outside permitted range.";
	
	protected static final String ERR_HELP_ARG = "Use /plot help [command] for more information.";
	
	// I/O messages
	protected static final String ERR_LOAD = "Failed to load plot data!";
	protected static final String ERR_SAVE = "Failed to save plot data!";
	
	//success messages
	protected static final String DONE_PLOT_SET = "Plot created.";
	protected static final String DONE_PLOT_REMOVE = "Plot removed.";
	protected static final String DONE_PLOT_REMALL = "All Plots have been removed!";
	
	protected static final String DONE_EDITOR_ADD = " can now edit this Plot.";
	protected static final String DONE_EDITOR_REMOVE = " can no longer edit this Plot.";
	
	protected static final String DONE_EDIT_ON = "This Plot is now active.";
	protected static final String DONE_EDIT_OFF = "This Plot is now inactive.";
	
	//status messages
	protected static final String STAT_SEL_ACTIVE = "Area selection active. Punch a block.";
	protected static final String STAT_SEL_SECOND = "Now punch another block to finish.";
	protected static final String STAT_SEL_RESET = "Area selection reset. Punch a block.";
	protected static final String STAT_SEL_CANCEL = "Area selection cancelled.";
	
	protected static final String STAT_LISTALL = "Listing all Plots' editors...";
}
