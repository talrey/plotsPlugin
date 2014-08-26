//ctrl-F "IMPL" for implementation comments/questions I might have.

/**
 * ArcanePlotsPlugin.java
 * Land-protection plugin for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version RC.2 for Minecraft 1.7.10
 */

package plots;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;

public final class ArcanePlotsPlugin extends JavaPlugin
{
	protected static boolean WARN_NAME_MISMATCH = true; // warns console if a player's name is changed.
														// Plots store by UUID, so this can happen safely.
	
	private static final int DEFAULT_RADIUS = 10; // IMPL default radius for plot if no args are given.
	private static final int MAX_RADIUS = 100; // don't want plots getting TOO huge, eh?
	
	private static final String FILEPATH = "./";
	private static final String FILENAME = "plots.ser";
	
	// temporary data holders for Plot-building
	private HashMap<String, Location> playerSelections = new HashMap<>();
	private HashMap<UUID, Plot> tempPlots = new HashMap<>();
	
	// the master list of all loaded plots
	private HashMap<Plot, Boolean> plotList = new HashMap<>();
	
	@Override
	public boolean onCommand (CommandSender sender, Command cmd, String label, String[] args)
	{
		Set<Plot> plotSet = plotList.keySet();
		
		if (cmd.getName().equals("plot confirm"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_PLAYER);
				return true;
			}
			Player pl = (Player)sender;
			return plotConfirm(args, sender, plotSet, pl);
		}
		else if (cmd.getName().equals("plot cancel"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_PLAYER);
				return true;
			}
			Player pl = (Player)sender;
			return plotCancel(args, sender, plotSet, pl);
		}
		else if (cmd.getName().equals("plot"))
		{
			if (!(sender instanceof Player))
			{
				// "plot remove all" was deemed too dangerous, and has been removed.
				/*
				if ( (args.length == 2) && ((args[0]+" "+args[1]).equals("unset all")) )
				{
					if (plotSet.isEmpty())
					{
						getLogger().info(Msg.ERR_NO_PLOTS);
						return true;
					}
					plotList.clear();
					getLogger().info(Msg.DONE_PLOT_REMALL);
					return true;
				}
				else */if ( (args.length == 2) && ((args[0]+" "+args[1]).equals("list all")) )
				{
					if (plotSet.isEmpty())
					{
						getLogger().info(Msg.ERR_NO_PLOTS);
						return true;
					}
					getLogger().info(Msg.STAT_LISTALL);
					for (Plot plot : plotSet)
					{
						getLogger().info(plot.listPlayers());
					}
					return true;
				}
				else
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_PLAYER);
					return true;
				}
			}
			Player pl = (Player)sender;
			
			if (args.length < 1) // default behavior goes in here
			{
				String[] temp = { "list" };
				return plotList(temp, sender, plotSet, pl);
			}
				
			switch (args[0])
			{
				case "create":
				case "set": return plotSet(args, sender, plotSet, pl);
				case "remove":
				case "unset": return plotUnset(args, sender, plotSet, pl);
				case "allow": return plotAllow(args, sender, plotSet, pl);
				case "block":
				case "unallow": return plotUnallow(args, sender, plotSet, pl);
				case "list": return plotList(args, sender, plotSet, pl);
				case "edit": return plotEdit(args, sender, plotSet, pl);
				case "confirm": return plotConfirm(args, sender, plotSet, pl);
				case "cancel": return plotCancel(args, sender, plotSet, pl);
				//case "help": return plotHelp(args, sender, plotSet, pl);
				
				default: sender.sendMessage(Msg.PREFIX + Msg.ERR_ARG_INVALID);
			}
		}
		return false;
	}
	
	private boolean plotSet (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length == 1)
		{
			Plot attempt = new Plot(pl.getLocation(), DEFAULT_RADIUS, pl.getUniqueId());
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation()))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_PLOT_EXISTS);
					return true;
				}
				if (plot.intersects(attempt))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_PLOT_OVERLAP);
					return true;
				}
			}
			tempPlots.put(pl.getUniqueId(), attempt);
			sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_SET + attempt.listCoords());
			return true;
		}
		else if ( (args.length == 2) && (args[1].equals("corners")) )
		{
			// note: actual Plot is built in the Listener below, in this case.
			Set<String> players = playerSelections.keySet();
			for (String name : players)
			{
				// if this person's already set a location, selection is active already...
				if (name.equals(pl.getName()))
				{
					if (playerSelections.get(name) == null) //they haven't selected a block.
					{
						sender.sendMessage(Msg.PREFIX + Msg.STAT_SEL_ACTIVE);
						return true;
					}
					// IMPL should re-sending the command reset, or should it cancel instead?
					playerSelections.put(name,null);
					sender.sendMessage(Msg.PREFIX + Msg.STAT_SEL_RESET);
					// OR
					//player.Selections.remove(name);
					//sender.sendMessage(Msg.PREFIX + Msg.STAT_SEL_CANCEL);
					return true;
				}
			}
			// this player isn't on the list yet. Let's put them there.
			playerSelections.put(pl.getName(),null);
			sender.sendMessage(Msg.PREFIX + Msg.STAT_SEL_ACTIVE);
			return true;
		}
		else if ( (args.length == 3) && (args[1].equals("radius")) )
		{
			int rad = 0;
			
			try
			{
				rad = Integer.parseInt(args[2]);
			}
			catch (Exception e)
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_RADIUS_NUM);
				return true;
			}
			if ((rad <= 0) || (rad >= MAX_RADIUS))
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_RADIUS_SIZE);
				return true;
			}
			
			Plot attempt = new Plot(pl.getLocation(), rad, pl.getUniqueId());
			
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation()))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_PLOT_EXISTS);
					return true;
				}
				if (plot.intersects(attempt))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_PLOT_OVERLAP);
					return true;
				}
			}
			plotList.put(attempt, true);
			sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_SET);
			return true;
		}
		return false;
	}
	
	private boolean plotUnset (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		/* //removed because danger
		if ( (args.length == 2) && (args[1].equals("all")) )
		{
			if (pl.hasPermission("arcanePlotsPlugin.console"))
			{
				if (plotList.isEmpty())
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOTS);
					return true;
				}
				sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_REMALL);
				plotList.clear();
				return true;
			}
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PERM);
			return true;
		}
		else */ if (args.length >= 2)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_ARGS_INVALID);
			return true;
		}
	
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation() ))
			{
				if (!plot.permitsPlayer(pl.getUniqueId()) && !pl.hasPermission("arcanePlotsPlugin.admin"))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
					return true;
				}
				tempPlots.put(pl.getUniqueId(), plot);
				sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_REM);
				return true;
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotAllow (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length < 2)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TARGET);
			return false;
		}
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation()))
			{
				if (!plot.permitsPlayer(pl.getUniqueId()) && !pl.hasPermission("arcanePlotsPlugin.admin"))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
					return true;
				}
				Player target = Bukkit.getPlayer(args[1]);
				if (target != null)
				{
					plot.addPlayer(target.getUniqueId()); // we prefer knowing the UUID here...
				}
				else
				{
					plot.addPlayer(args[1]); // but we CAN work with a string if they're offline.
				}
				sender.sendMessage(Msg.PREFIX + args[1] + Msg.DONE_EDITOR_ADD);
				return true;
			}
		}
		return false;
	}
	
	private boolean plotUnallow (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length < 2)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TARGET);
			return false;
		}
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation()))
			{
				if (!plot.permitsPlayer(pl.getUniqueId()) && !pl.hasPermission("arcanePlotsPlugin.admin"))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
					return true;
				}
				Player target = Bukkit.getPlayer(args[1]);
				if (target != null)
				{
					if (! (plot.removePlayer(target.getUniqueId()) ))
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_REMOVE_PLAYER);
						// note we don't need a "NOT FOUND" here b/c if we can't find them...
						// we try the block below anyway!
						return true;
					}
				}
				else
				{
					if (! (plot.removePlayer(args[1])) )
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_FOUND);
						return true;
					}
				}
				sender.sendMessage(Msg.PREFIX + args[1] + Msg.DONE_EDITOR_REMOVE);
				return true;
			}
		}
		return false;
	}
	
	private boolean plotList (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		// IMPL you don't need modify rights to see who has them- makes sense, right?
		if ( (args.length == 2) && (args[1].equals("all")) )
		{
			if (pl.hasPermission("arcanePlotsPlugin.console"))
			{
				if (plotSet.isEmpty())
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOTS);
					return true;
				}
				for (Plot plot : plotSet)
				{
					sender.sendMessage(plot.listPlayers());
				}
				return true;
			}
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PERM);
			return true;
		}
		else if (args.length >= 2)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_ARGS_INVALID);
			return true;
		}
		else // IMPL no argument given, assuming *this* plot
		{
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation()))
				{
					sender.sendMessage(plot.listPlayers());
					return true;
				}
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotEdit (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if ( (args.length == 2) && (args[1].equals("on")) )
		{
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation())
				&& (plot.permitsPlayer(pl.getUniqueId()) || pl.hasPermission("arcanePlotsPlugin.admin")) )
				{
					plotList.put(plot, true);
					sender.sendMessage(Msg.PREFIX + Msg.DONE_EDIT_ON);
					return true;
				}
				else if (plot.contains(pl.getLocation()))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
					return true;
				}
			}
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
			return true;
		}
		else if ( (args.length == 2) && (args[1].equals("off")) )
		{
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation())
				&& (plot.permitsPlayer(pl.getUniqueId()) || pl.hasPermission("arcanePlotsPlugin.admin")) )
				{
					plotList.put(plot, false);
					sender.sendMessage(Msg.PREFIX + Msg.DONE_EDIT_OFF);
					return true;
				}
				else if (plot.contains(pl.getLocation()))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
					return true;
				}
			}
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
			return true;
		}
		else if (args.length >= 2)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_ARGS_INVALID);
			return false;
		}
		// IMPL else there's no arg and we assume they mean toggle.
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation())
			&& (plot.permitsPlayer(pl.getUniqueId()) || pl.hasPermission("arcanePlotsPlugin.admin")) )
			{
				if (plotList.get(plot))
				{
					plotList.put(plot, false);
					sender.sendMessage(Msg.PREFIX + Msg.DONE_EDIT_OFF);
					return true;
				}
				else
				{
					plotList.put(plot, true);
					sender.sendMessage(Msg.PREFIX + Msg.DONE_EDIT_ON);
					return true;
				}
			}
			else if (plot.contains(pl.getLocation()))
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
				return true;
			}
			else {}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotConfirm (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		Set<UUID> affirmSet = tempPlots.keySet();
		
		for (UUID id : affirmSet)
		{
			if (pl.getUniqueId().equals(id)) // first confirm the player's queued at all
			{
				for (Plot plot : plotSet)
				{
					if (plot.equals(tempPlots.get(id))) // it's a match! that means remove it.
					{
						plotList.remove(tempPlots.get(id));
						tempPlots.remove(id);
						sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_REMOVE);
						return true;
					}
				}
				// no match, that means add it to the real list and clear it from the temp.
				plotList.put(tempPlots.get(id), true);
				tempPlots.remove(id);
				sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_SET);
				return true;
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TEMP);
		return true;
	}
	
	private boolean plotCancel (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		Set<UUID> affirmSet = tempPlots.keySet();
		
		for (UUID id : affirmSet)
		{
			if (pl.getUniqueId().equals(id))
			{
				tempPlots.remove(id);
				sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_CANCEL);
				return true;
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TEMP);
		return true;
	}
	
	// DEPRECATED because subcommands seem to work in plugin.yml
	@Deprecated
	private boolean plotHelp (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length == 1)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_HELP_ARG);
			return true;
		}
		else if (args.length == 2)
		{
			switch (args[1])
			{
				case "set":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_SET); break;
				case "unset":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_UNSET); break;
				case "allow":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_ALLOW); break;
				case "unallow":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_UNALLOW); break;
				case "list":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_LIST); break;
				case "edit":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_EDIT); break;
				case "help":
					sender.sendMessage(Msg.PREFIX + Msg.PLOT_HELP_HELP); break;
				default: sender.sendMessage(Msg.PREFIX + Msg.ERR_ARG_INVALID);
			}
			return true;
		}
		return false;
	}
	
	// Saving and loading code slightly modified from Tomsik68's SLAPI code on wiki.bukkit.org
	private boolean saveAllPlots ()
	{
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILEPATH + FILENAME));
			oos.writeObject(plotList);
			oos.flush();
			oos.close();
		}
		catch (Exception e)
		{
			getLogger().info(e.getMessage());
			return false;
		}
		return true;
	}
	private boolean loadAllPlots ()
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILEPATH + FILENAME));
			plotList.putAll( (HashMap<Plot, Boolean>)ois.readObject());
			ois.close();
		}
		catch (Exception e)
		{
			getLogger().info(e.getMessage());
			return false;
		}
		return true;
	}
	
	public boolean wardIsActive (Location loc)
	{
		Set<Plot> plotSet = plotList.keySet();
		for (Plot plot : plotSet)
		{
			if (plot.contains(loc))
			{
				return plotList.get(plot);
			}
		}
		return false;
	}
		
	@Override
	public void onEnable ()
	{
		getServer().getPluginManager().registerEvents(new PlotsListener(), this);
		if (! (loadAllPlots()) )
		{
			getLogger().info(Msg.PREFIX + Msg.ERR_LOAD);
		}
	}
	
	@Override public void onDisable()
	{
		HashMap<Plot, Boolean> plotTest = new HashMap<>();
		plotTest.putAll(plotList);
		if (! (saveAllPlots()) )
		{
			getLogger().info(Msg.PREFIX + Msg.ERR_SAVE);
		}
	}
	
	public final class PlotsListener implements Listener
	{
		@EventHandler
		public void detectSelection (PlayerInteractEvent e)
		{
			Set<String> players = playerSelections.keySet();
			for (String name : players)
			{
				if (e.getPlayer().getName() == name) // if you ain't on the list, you ain't in the club.
				{
					if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
					
					Set<Plot> plotSet = plotList.keySet();
					for (Plot plot : plotSet)
					{
						if (plot.contains(e.getClickedBlock().getLocation()))
						{
							e.getPlayer().sendMessage(Msg.PREFIX + Msg.ERR_PLOT_EXISTS);
							return;
						}
					}
					
					if (playerSelections.get(name) == null) // first corner is being selected...
					{
						playerSelections.put(name,e.getClickedBlock().getLocation());
						e.getPlayer().sendMessage(Msg.PREFIX + Msg.STAT_SEL_SECOND);
					}
					else // second corner is being selected...
					{
						Plot attempt = new Plot(
							playerSelections.get(name).getWorld().getUID(),
							Plot.buildRect(playerSelections.get(name), e.getClickedBlock().getLocation()),
							e.getPlayer().getUniqueId()
						);
						for (Plot plot : plotSet)
						{
							if (plot.intersects(attempt))
							{
								e.getPlayer().sendMessage(Msg.PREFIX + Msg.ERR_PLOT_OVERLAP);
								playerSelections.remove(name);
								return;
							}
						}
						plotList.put(attempt, true);
						playerSelections.remove(name);
						e.getPlayer().sendMessage(Msg.PREFIX + Msg.DONE_PLOT_SET);
					}
				}
			}
		}
		@EventHandler
		public void stopDestruct (BlockDamageEvent e)
		{
			if (wardIsActive(e.getBlock().getLocation()) ) 
			{
				Set<Plot> plotSet = plotList.keySet();
				for (Plot plot : plotSet)
				{
					if (plot.contains(e.getBlock().getLocation()) )
					{
						if (plot.permitsPlayer(e.getPlayer().getUniqueId()) )
						{
							// carry on, citizen.
							return;
						}
						else e.setCancelled(true);
						//getLogger().info("[PlotsPlugin]: block damage prevented");
					}
				}
			}
		}
		
		@EventHandler
		public void stopConstruct (BlockPlaceEvent e)
		{
			if (wardIsActive(e.getBlock().getLocation()) ) 
			{
				Set<Plot> plotSet = plotList.keySet();
				for (Plot plot : plotSet)
				{
					if (plot.contains(e.getBlock().getLocation()) )
					{
						if (plot.permitsPlayer(e.getPlayer().getUniqueId()) )
						{
							// nothing to see here.
							return;
						}
						else e.setCancelled(true);
						//getLogger().info("[PlotsPlugin]: block placement prevented");
					}
				}
			}
		}
	}
}
