/**
 * ArcanePlotsPlugin.java
 * Land-protection plugin for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version RC.3.1.2 for Minecraft 1.7.10
 */

package plots;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
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
	// keys for the config file
	private static final String RADIUS_DEF = "radius.default";
	private static final String RADIUS_MAX = "radius.maximum";
	private static final String PLOTPATH = "file.plot.path";
	private static final String PLOTFILE = "file.plot.name";
	private static final String BANKPATH = "file.bank.path";
	private static final String BANKFILE = "file.bank.name";
	private static final String WARN_MISMATCH = "warn_mismatch";
	private static final String CREDIT_CPB = "credit.cpb";
	private static final String CREDIT_RPB = "credit.rpb";
	private static final String CREDIT_WEP = "credit.wep";
	private static final String CREDIT_EPB = "credit.epb";
	
	// temporary data holders for Plot-building
	private HashMap<String, Location> playerSelections = new HashMap<>();
	private HashMap<UUID, Plot> tempPlots = new HashMap<>();
	
	// the master list of all loaded plots
	private HashMap<Plot, Boolean> plotList = new HashMap<>();
	
	// the list of player credit balances
	private HashMap<UUID, Long> bank = new HashMap<>();
	
	@Override
	public boolean onCommand (CommandSender sender, Command cmd, String label, String[] args)
	{
		Set<Plot> plotSet = plotList.keySet();
		
		if (!(sender instanceof Player))
		{
			if ( (args.length == 2) && ((args[0]+" "+args[1]).equals("list all")) )
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
			if ( (args.length == 3) && (args[0].equals("give")) )
			{
				if (Bukkit.getPlayer(args[1]) == null)
				{
					// I need to find a workaround for this.
					sender.sendMessage(Msg.PREFIX + Msg.ERR_PLAYER_OFFLINE);
					return true;
				}
				if (creditAdd(Bukkit.getPlayer(args[1]), Integer.parseInt(args[2])) )
				{
					sender.sendMessage(Msg.PREFIX + Msg.DONE_CRED_SENT);
					return true;
				}
				return false;
			}
			else
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_PLAYER);
				return true;
			}
		}
		Player pl = (Player)sender;
		
		if (cmd.getName().equals("confirm"))
		{
			pl.performCommand("plot confirm");
			return true;
		}
		else if (cmd.getName().equals("cancel"))
		{
			pl.performCommand("plot cancel");
			return true;
		}
		else if (cmd.getName().equals("plot"))
		{	
			if (args.length == 0)
			{
				pl.performCommand("plot list");
				return true;
			}
			switch (args[0])
			{
				case "buy": return plotSet(args, sender, plotSet, pl);
				case "sell": return plotUnset(args, sender, plotSet, pl);
				case "modify": return plotModify(args, sender, plotSet, pl);
				case "list": return plotList(args, sender, plotSet, pl);
				case "edit": return plotEdit(args, sender, plotSet, pl);
				case "confirm": return plotConfirm(args, sender, plotSet, pl);
				case "cancel": return plotCancel(args, sender, plotSet, pl);
	/*DEBUG*/	case "test": return testFunction(args, sender, plotSet, pl);
				default: sender.sendMessage(Msg.PREFIX + Msg.ERR_ARG_INVALID);
			}
		}
		else if (cmd.getName().equals("credits"))
		{
			if ( (args.length == 3) && (args[0].equals("give")) )
			{
				if (Bukkit.getPlayer(args[1]) == null)
				{
					// I need to find a workaround for this.
					sender.sendMessage(Msg.PREFIX + Msg.ERR_PLAYER_OFFLINE);
					return true;
				}
				if (creditTrans(pl, Bukkit.getPlayer(args[1]), Integer.parseInt(args[2])) )
				{
					sender.sendMessage(Msg.PREFIX + Msg.DONE_CRED_SENT);
					return true;
				}
				return false;
			}
			else if (args.length == 0)
			{
				sender.sendMessage(Msg.PREFIX + Msg.DONE_CRED_BAL + creditGet(pl) );
			}
			else
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_ARGS_INVALID);
				return false;
			}
		}
		return false;
	}
	
	private boolean testFunction (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		sender.sendMessage("Fun fact! The maximum area of a plot is ~" + (Integer.MAX_VALUE/getCPB()) );
		return true;
	}
	
	private boolean plotSet (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length == 1)
		{
			Plot attempt = new Plot(pl.getLocation(), getRadiusDef(), pl.getUniqueId());
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
			sender.sendMessage(Msg.PREFIX + Msg.STAT_CRED_PRICE + (attempt.getArea()*getCPB()) );
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
					// resets selection.
					playerSelections.put(name,null);
					sender.sendMessage(Msg.PREFIX + Msg.STAT_SEL_RESET);
					// OR
					// cancels selection.
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
			if ((rad <= 0) || (rad >= getRadiusMax()) )
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
			sender.sendMessage(Msg.PREFIX + Msg.STAT_CRED_PRICE + (attempt.getArea()*getCPB()) );
			sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_SET);
			return true;
		}
		return false;
	}
	
	private boolean plotUnset (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length >= 2)
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
				// put in a check for warranty time-out?
				sender.sendMessage(Msg.PREFIX + Msg.STAT_CRED_PRICE + (plot.getArea()*getRPB()) );
				sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_REM);
				return true;
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotModify (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if (args.length < 3)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TARGET);
			return false;
		}
		if (args[1].equals("+"))
		{
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation()) )
				{
					if (!plot.permitsPlayer(pl.getUniqueId())
					&& !pl.hasPermission("arcanePlotsPlugin.admin"))
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
						return true;
					}
					Player target = Bukkit.getPlayer(args[2]);
					if (target != null)
					{
						plot.addPlayer(target.getUniqueId()); // we prefer knowing the UUID here...
					}
					else
					{
						plot.addPlayer(args[1]); // but we CAN work with a string if they're offline.
					}
					sender.sendMessage(Msg.PREFIX + args[2] + Msg.DONE_EDITOR_ADD);
					return true;
				}
			}
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
			return true;
		}
		else if (args[1].equals("-"))
		{
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation()))
				{
					if (!plot.permitsPlayer(pl.getUniqueId())
					&& !pl.hasPermission("arcanePlotsPlugin.admin"))
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
						return true;
					}
					Player target = Bukkit.getPlayer(args[2]);
					if (target != null)
					{
						if (! (plot.removePlayer(target.getUniqueId())) )
						{
							sender.sendMessage(Msg.PREFIX + Msg.ERR_REMOVE_PLAYER);
							// note we don't need a "NOT FOUND" here b/c if we can't find them...
							// we try the block below anyway!
							return true;
						}
					}
					else
					{
						if (! (plot.removePlayer(args[2])) )
						{
							sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_FOUND);
							return true;
						}
					}
					sender.sendMessage(Msg.PREFIX + args[2] + Msg.DONE_EDITOR_REMOVE);
					return true;
				}
			}
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
			return true;
		}
		return false;
	}
	
	private boolean plotList (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
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
		else
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
						creditAdd( (Player)sender, plot.getArea()*getRPB() );
						plotList.remove(tempPlots.get(id));
						tempPlots.remove(id);
						sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_REMOVE);
						return true;
					}
				}
				// no match, that means add it to the real list and clear it from the temp.
				if ( creditGet( (Player)sender) >= (tempPlots.get(id).getArea()*getCPB()) )
				{
					creditRem( (Player)sender, tempPlots.get(id).getArea()*getCPB() );
					plotList.put(tempPlots.get(id), true);
					tempPlots.remove(id);
					sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_SET);
					return true;
				}
				else // come back when you're a little, mmm... richer.
				{
					tempPlots.remove(id);
					sender.sendMessage(Msg.PREFIX + Msg.ERR_CRED_POOR);
					return true;
				}
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
	
	private boolean creditAdd (Player pl, long amount)
	{
		bank.put(pl.getUniqueId(), bank.get(pl.getUniqueId())+(long)amount);
		return true;
	}
	
	private boolean creditRem (Player pl, long amount)
	{
		bank.put(pl.getUniqueId(), bank.get(pl.getUniqueId())-(long)amount);
		return true;
	}
	
	private boolean creditTrans (Player from, Player to, int amount)
	{
		boolean success = false;
		
		success = creditAdd(to, amount) && creditRem(from, amount);
		
		return success;
	}
	
	private long creditGet (Player me)
	{
		return bank.get(me.getUniqueId());
	}
	
	// Saving and loading code slightly modified from Tomsik68's SLAPI code on wiki.bukkit.org
	// {
	private boolean saveAllPlots ()
	{
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getPlotFile()));
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
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getPlotFile()));
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
	
	private boolean saveBank ()
	{
		try
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getBankFile()));
			oos.writeObject(bank);
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
	
	private boolean loadBank ()
	{
		try
		{
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getBankFile()));
			bank.putAll( (HashMap<UUID, Long>)ois.readObject());
			ois.close();
		}
		catch (Exception e)
		{
			getLogger().info(e.getMessage());
			return false;
		}
		return true;
	}
	// }
	
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
	
	// getters for the config values {
	public int getRadiusDef ()
	{
		return this.getConfig().getInt(RADIUS_DEF);
	}
	
	public int getRadiusMax ()
	{
		return this.getConfig().getInt(RADIUS_MAX);
	}
	
	public String getPlotFile ()
	{
		return (this.getConfig().getString(PLOTPATH) + this.getConfig().getString(PLOTFILE) );
	}
	
	public String getBankFile ()
	{
		return (this.getConfig().getString(BANKPATH) + this.getConfig().getString(BANKFILE) );
	}
	
	public boolean shouldWarnMismatch ()
	{
		return this.getConfig().getBoolean(WARN_MISMATCH);
	}
	
	public long getCPB ()
	{
		return this.getConfig().getLong(CREDIT_CPB);
	}
	
	public long getRPB ()
	{
		return this.getConfig().getLong(CREDIT_RPB);
	}
	
	public long getWEP ()
	{
		return this.getConfig().getLong(CREDIT_WEP);
	}
	
	public long getEPB ()
	{
		return this.getConfig().getLong(CREDIT_EPB);
	}
	// }
	
	@Override
	public void onEnable ()
	{
		getServer().getPluginManager().registerEvents(new PlotsListener(), this);
		if (! (loadAllPlots() && loadBank()) )
		{
			getLogger().info(Msg.PREFIX + Msg.ERR_LOAD);
		}
	}
	
	@Override public void onDisable()
	{
		//HashMap<Plot, Boolean> plotTest = new HashMap<>();
		//plotTest.putAll(plotList);
		if (! (saveAllPlots() && saveBank()) )
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
			Set <Plot> plotSet = plotList.keySet();
			for (Plot plot : plotSet)
			{
				if (plot.contains(e.getBlock().getLocation()) )
				{
					if (!plotList.get(plot) && plot.permitsPlayer(e.getPlayer().getUniqueId()) )
					{
						// carry on, citizen.
						creditAdd(e.getPlayer(), getEPB());
						return;
					}
					else if (plotList.get(plot))
					{
						e.getPlayer().sendRawMessage(Msg.PREFIX + Msg.STAT_PLOT_ACTIVE);
						e.setCancelled(true);
						return;
					}
					else
					{
						e.getPlayer().sendRawMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
						e.setCancelled(true);
						return;
					}
				}
			}
		}
		
		@EventHandler
		public void stopConstruct (BlockPlaceEvent e)
		{ 
			Set <Plot> plotSet = plotList.keySet();
			for (Plot plot : plotSet)
			{
				if (plot.contains(e.getBlock().getLocation()) )
				{
					if (!plotList.get(plot) && plot.permitsPlayer(e.getPlayer().getUniqueId()) )
					{
						// carry on, citizen.
						creditAdd(e.getPlayer(), getEPB());
						return;
					}
					else if (plotList.get(plot))
					{
						e.getPlayer().sendRawMessage(Msg.PREFIX + Msg.STAT_PLOT_ACTIVE);
						e.setCancelled(true);
						return;
					}
					else
					{
						e.getPlayer().sendRawMessage(Msg.PREFIX + Msg.ERR_NOT_EDITOR);
						e.setCancelled(true);
						return;
					}
				}
			}
		}
	}
}
