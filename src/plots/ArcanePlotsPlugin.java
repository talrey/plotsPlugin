/**
 * ArcanePlotsPlugin.java
 * Land-protection plugin for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version RC.3.2.0 for Minecraft 1.7.10
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
	
	// temporary data holders for Plot-building etc
	private HashMap<String, Location> playerSelections = new HashMap<>();
	private HashMap<UUID, Plot> tempPlots = new HashMap<>();
	private HashMap<UUID, String> transfers = new HashMap<>();
	private HashMap<Plot, String> escrow = new HashMap<>();
	
	// the master list of all loaded plots
	private HashMap<Plot, String> plotList = new HashMap<>();
	
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
					if (plotList.get(plot).equals(null) || plotList.get(plot).isEmpty())
					{
						getLogger().info(plot.listPlayers(null));
					}
					else
					{
						getLogger().info(plot.listPlayers(plotList.get(plot)));
					}
				}
				return true;
			}
			if ( (args.length == 3) && ((cmd.getName()+" "+args[0]).equals("credits give")) )
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
		else if (cmd.getName().equals("cr"))
		{
			pl.performCommand("credits " + listArrayNoDecor(args));
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
				case "name": return plotName(args, sender, plotSet, pl);
				case "add": return plotAdd(args, sender, plotSet, pl);
				case "remove": return plotRem(args, sender, plotSet, pl);
				case "list": return plotList(args, sender, plotSet, pl);
				case "confirm": return plotConfirm(args, sender, plotSet, pl);
				case "cancel": return plotCancel(args, sender, plotSet, pl);
	/*DEBUG*/	case "test": return testFunction(args, sender, plotSet, pl);
				case "help": return plotHelp(args, sender, plotSet, pl);
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
				if (Integer.parseInt(args[2]) < 0)
				{
					sender.sendMessage(Msg.CRPREF + Msg.ERR_CRED_ISNEG);
					return true;
				}
				if (Integer.parseInt(args[2]) > creditGet(pl))
				{
					sender.sendMessage(Msg.CRPREF + Msg.ERR_CRED_POOR);
					return true;
				}
				transfers.put(pl.getUniqueId(), args[1] + " " + args[2]);
				
				sender.sendMessage(
					Msg.CRPREF + Msg.STAT_CRED_TRANS + Integer.parseInt(args[2])
					+ Msg.STAT_CRED_TRAN2 + args[1] + Msg.STAT_CRED_TRAN3
				);
				return true;
			}
			else if (args.length == 0)
			{
				sender.sendMessage(Msg.CRPREF + Msg.DONE_CRED_BAL + creditGet(pl) );
				return true;
			}
			else
			{
				sender.sendMessage(Msg.CRPREF + Msg.ERR_ARGS_INVALID);
				return false;
			}
		}
		return false;
	}
	
	// DEBUG //
	private boolean testFunction (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		sender.sendMessage("Credits earned per Block: " + getEPB() );
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
			sender.sendMessage(
				Msg.PREFIX + Msg.STAT_PLOT_SET + attempt.listCoords() + Msg.STAT_PLOT_CANCEL
			);
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
			tempPlots.put(pl.getUniqueId(), attempt);
			sender.sendMessage(Msg.PREFIX + Msg.STAT_CRED_PRICE + (attempt.getArea()*getCPB()) );
			sender.sendMessage(
				Msg.PREFIX + Msg.STAT_PLOT_SET + attempt.listCoords() + Msg.STAT_PLOT_CANCEL
			);
			return true;
		}
		return false;
	}
	
	private boolean plotUnset (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		Player buyer;
		int price;
		
		if (args.length > 3)
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_ARGS_INVALID);
			return true;
		}
		else if (args.length == 3)
		{
			buyer = Bukkit.getPlayer(args[1]);
			
			if (buyer == null)
			{
				// they're offline or nonexistent, what do?
				sender.sendMessage(Msg.PREFIX + Msg.ERR_PLAYER_OFFLINE);
				return true;
			}
			try 
			{
				price = Integer.parseInt(args[2]);
			}
			catch (NumberFormatException e)
			{
				sender.sendMessage(Msg.PREFIX + Msg.ERR_CRED_INVALID);
				return true;
			}
			
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation() ))
				{
					if (!plot.isOwner(pl.getUniqueId()) && !pl.hasPermission("arcanePlotsPlugin.admin"))
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_OWNER);
						return true;
					}
					escrow.put(plot, pl.getName() + "§_TO_§" + buyer.getName() + "§_FOR_§" + price);
					sender.sendMessage(
						Msg.PREFIX + Msg.STAT_PLOT_TRANS + buyer.getName() + " for " + price
					);
					sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_REM + Msg.STAT_PLOT_CANCEL);
					return true;
				}
			}
		}
		else
		{
			for (Plot plot : plotSet)
			{
				if (plot.contains(pl.getLocation() ))
				{
					if (!plot.isOwner(pl.getUniqueId()) && !pl.hasPermission("arcanePlotsPlugin.admin"))
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_OWNER);
						return true;
					}
					tempPlots.put(pl.getUniqueId(), plot);
					// put in a check for warranty time-out?
					sender.sendMessage(Msg.PREFIX + Msg.STAT_CRED_REFUND + (plot.getArea()*getRPB()) );
					sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_REM + Msg.STAT_PLOT_CANCEL);
					return true;
				}
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotName (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation()) )
			{
				if (args.length < 2)
				{
					if (plotList.get(plot).trim().length() == 0)
					{
						sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_NAME);
						return true;
					}
					sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_NAME + plotList.get(plot) + "\"");
					return true;
				}
				if ( !plot.isOwner(pl.getUniqueId()) )
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_OWNER);
					return true;
				}
				StringBuilder stb = new StringBuilder();
				
				stb.append(args[1]);
				for (int i=1; i<(args.length-1); i++)
				{
					stb.append(" " + args[i]);
				}
				
				if (stb.toString().startsWith("\""))
				{
					stb.delete(0,1);
				}
				if (stb.toString().endsWith("\""))
				{
					stb.delete(stb.length()-1, stb.length());
				}
				if (stb.toString().trim().length() == 0)
				{
					sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_UNNAME);
					plotList.put(plot, "");
					return true;
				}
				
				sender.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_RENAME + plotList.get(plot) + "\"");
				sender.sendMessage(Msg.DONE_PLOT_RENAME + stb.toString() + "\"");
				plotList.put(plot, stb.toString());
				return true;
			}
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotAdd (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if ( (args.length < 2) || (args[1].isEmpty()) )
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TARGET);
			return false;
		}
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation()) )
			{
				if (!plot.isOwner(pl.getUniqueId())
				&& !pl.hasPermission("arcanePlotsPlugin.admin"))
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
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
	}
	
	private boolean plotRem (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		if ( (args.length < 2) || (args[1].isEmpty()) )
		{
			sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TARGET);
			return false;
		}
		for (Plot plot : plotSet)
		{
			if (plot.contains(pl.getLocation()))
			{
				if (!plot.isOwner(pl.getUniqueId())
				&& !pl.hasPermission("arcanePlotsPlugin.admin"))
				{
					sender.sendMessage(Msg.PREFIX + Msg.ERR_NOT_OWNER);
					return true;
				}
				Player target = Bukkit.getPlayer(args[1]);
				if (target != null)
				{
					if (! (plot.removePlayer(target.getUniqueId())) )
					{
						if (plot.isOwner(target.getUniqueId()))
						{
							sender.sendMessage(Msg.PREFIX + Msg.ERR_REMOVE_OWNER);
							return true;
						}
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
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_PLOT);
		return true;
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
					if (plotList.get(plot).equals(null) || plotList.get(plot).isEmpty())
					{
						sender.sendMessage(plot.listPlayers(null));
					}
					else
					{
						sender.sendMessage(plot.listPlayers(plotList.get(plot)));
					}
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
					if (plotList.get(plot).equals(null) || plotList.get(plot).isEmpty())
					{
						sender.sendMessage(plot.listPlayers(null));
					}
					else
					{
						sender.sendMessage(plot.listPlayers(plotList.get(plot)));
					}
					return true;
				}
			}
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
					if (plot.equals(tempPlots.get(id))) // it's a match!
					{
						if ( !plot.isOwner(id) ) // it's being transferred, not deleted.
						{
							creditTrans(
								Bukkit.getPlayer(id), Bukkit.getPlayer(plot.getOwner()), plot.getPrice()
							);
							plot.setOwner(id);
							tempPlots.remove(id);
							sender.sendMessage(Msg.PREFIX + Msg.DONE_OFFER_RECV + plot.listCoords());
							return true;
						}
						else
						{
							// remove it!
							creditAdd( (Player)sender, plot.getArea()*getRPB() );
							plotList.remove(tempPlots.get(id));
							tempPlots.remove(id);
							sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_REMOVE);
							return true;
						}
					}
				}
				// no match, that means add it to the real list and clear it from the temp.
				if ( creditGet( (Player)sender) >= (tempPlots.get(id).getArea()*getCPB()) )
				{
					creditRem( (Player)sender, tempPlots.get(id).getArea()*getCPB() );
					plotList.put(tempPlots.get(id), "");
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
		// getting to here means they're doing a credit transfer, not a purchase / sellback
		Set<UUID> transferSet = transfers.keySet();
		for (UUID id : transferSet)
		{
			if (pl.getUniqueId().equals(id) && (transfers.get(id)!=null) && !transfers.get(id).isEmpty() )
			{
				Player him = Bukkit.getPlayer((transfers.get(id).split(" "))[0]);
				int amount = Integer.parseInt( (transfers.get(id).split(" "))[1]);
				
				if (pl.equals(him))
				{
					sender.sendMessage(Msg.CRPREF + Msg.ERR_CRED_SELF);
					transfers.remove(id);
					return true;
				}
				
				if (creditTrans(pl, him, amount))
				{
					sender.sendMessage(Msg.CRPREF + Msg.DONE_CRED_SENT);
					transfers.remove(id);
					return true;
				}
			}
		}
		//getting to here means they're transferring a plot to another player.
		Set<Plot> escrowSet = escrow.keySet();
		for (Plot plot : escrowSet)
		{
			if ( !(escrow.get(plot).startsWith(pl.getName())) ) continue;
			
			String[] order = escrow.get(plot).split("§_.+_§",3);
			Player seller = Bukkit.getPlayer(order[0]);
			Player buyer = Bukkit.getPlayer(order[1]);
			int price = Integer.parseInt(order[2]);
			
			plot.setPrice(price);
			escrow.remove(plot);
			tempPlots.put(buyer.getUniqueId(), plot);
			
			seller.sendMessage(Msg.PREFIX + Msg.DONE_OFFER_SENT);
			buyer.sendMessage(
				Msg.PREFIX + 
				Msg.STAT_PLOT_OFFER + plotList.get(plot) + 
				Msg.STAT_PLOT_OFFER2 + price
			);
			buyer.sendMessage(Msg.PREFIX + Msg.STAT_PLOT_OFFER3);
			return true;
		}
				
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TEMP);
		return true;
	}
	
	private boolean plotCancel (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		Set<UUID> affirmSet = tempPlots.keySet();
		for (UUID id : affirmSet)
		{
			if (pl.getUniqueId().equals(id) && (transfers.get(id)!=null) && !transfers.get(id).isEmpty() )
			{
				tempPlots.remove(id);
				sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_CANCEL);
				return true;
			}
		}
		Set<UUID> transferSet = transfers.keySet();
		for (UUID id : transferSet)
		{
			if (pl.getUniqueId().equals(id) && (transfers.get(id)!=null) && !transfers.get(id).isEmpty() )
			{
				Player him = Bukkit.getPlayer((transfers.get(id).split(" "))[0]);
				int amount = Integer.parseInt( (transfers.get(id).split(" "))[1]);
				
				if (pl.equals(him))
				{
					sender.sendMessage(Msg.CRPREF + Msg.ERR_CRED_SELF);
					transfers.remove(id);
					return true;
				}
				if (creditTrans(pl, him, amount))
				{
					sender.sendMessage(Msg.CRPREF + Msg.DONE_CRED_SENT);
					transfers.remove(id);
					return true;
				}
			}
		}
		Set<Plot> escrowSet = escrow.keySet();
		for (Plot plot : escrowSet)
		{
			if ( !((escrow.get(plot).split("§_TO_§"))[0].equals(pl.getName())) ) continue;
			
			escrow.remove(plot);
			sender.sendMessage(Msg.PREFIX + Msg.DONE_PLOT_CANCEL);
			return true;
		}
		sender.sendMessage(Msg.PREFIX + Msg.ERR_NO_TEMP);
		return true;
	}
	
	private boolean plotHelp (String[] args, CommandSender sender, Set<Plot> plotSet, Player pl)
	{
		sender.sendMessage(Msg.PREFIX + Msg.HELP_PLOT);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_BUY);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_SELL);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_ADD);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_REM);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_LIST);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_YES);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_NO);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_CRED);
		sender.sendMessage(Msg.FORMAT + Msg.HELP_TRAN);
		return true;
	}
	
	private boolean creditAdd (Player pl, long amount)
	{
		if (bank.get(pl.getUniqueId()) != null)
		{
			bank.put(pl.getUniqueId(), bank.get(pl.getUniqueId()) + amount);
			return true;
		}
		else
		{
			bank.put(pl.getUniqueId(), amount);
			return true;
		}
	}
	
	private boolean creditRem (Player pl, long amount)
	{
		if (bank.get(pl.getUniqueId()) == null) return false;
		
		bank.put(pl.getUniqueId(), bank.get(pl.getUniqueId()) - amount);
		return true;
	}
	
	private boolean creditTrans (Player from, Player to, long amount)
	{
		boolean success = false;
		
		success = creditAdd(to, amount) && creditRem(from, amount);
		
		return success;
	}
	
	private long creditGet (Player me)
	{
		if (bank.get(me.getUniqueId()) == null) return 0L;
		
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
			plotList.putAll( (HashMap<Plot, String>)ois.readObject());
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
	
	private String listArrayNoDecor (String[] array)
	{
		StringBuilder ret = new StringBuilder();
		for (int i=0; i< array.length; i++)
		{
			ret.append(array[i]+" ");
		}
		return ret.toString();
	}
	
	@Override
	public void onEnable ()
	{
		getServer().getPluginManager().registerEvents(new PlotsListener(), this);
		if (! (loadAllPlots() && loadBank()) )
		{
			getLogger().info(Msg.ERR_LOAD);
		}
	}
	
	@Override public void onDisable()
	{
		//HashMap<Plot, Boolean> plotTest = new HashMap<>();
		//plotTest.putAll(plotList);
		if (! (saveAllPlots() && saveBank()) )
		{
			getLogger().info(Msg.ERR_SAVE);
		}
	}
	
	public final class PlotsListener implements Listener
	{
		@EventHandler
		public void detectSelection (PlayerInteractEvent e)
		{
			if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
			
			Set<String> players = playerSelections.keySet();
			for (String name : players)
			{
				if (e.getPlayer().getName() == name) // if you ain't on the list, you ain't in the club.
				{
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
						plotList.put(attempt, "");
						playerSelections.remove(name);
						e.getPlayer().sendMessage(Msg.PREFIX + Msg.DONE_PLOT_SET);
					}
				}
			}
		}
		
		@EventHandler
		public void stopDestruct (BlockBreakEvent e)
		{ 
			Set <Plot> plotSet = plotList.keySet();
			for (Plot plot : plotSet)
			{
				if (plot.contains(e.getBlock().getLocation()) )
				{
					if (plot.permitsPlayer(e.getPlayer().getUniqueId()) )
					{
						// carry on, citizen.
						creditAdd(e.getPlayer(), getEPB());
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
					if (plot.permitsPlayer(e.getPlayer().getUniqueId()) )
					{
						// carry on, citizen.
						creditAdd(e.getPlayer(), getEPB());
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
