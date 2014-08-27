/**
 * Plot.java
 * Stores information about where a Plot is, and who can modify it.
 * @author Morios (Mark Talrey)
 * @version RC.2.1 for Minecraft 1.7.10
 */

package plots;

import java.awt.Point;
import java.awt.Rectangle;

import java.io.Serializable;

//import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

public final class Plot implements Serializable
{
	private static final long SerialVersionUID = 1710011L;
	
	private UUID world;
	private HashMap<UUID, String> allowedPlayers;
	private int x,z,w,l;
	
	protected static final UUID BLANK = new UUID(0L, 0L);
	
		
	protected Plot (UUID worldUID, Rectangle bounds, UUID player)
	{
		world = worldUID;
		allowedPlayers = new HashMap<>();
		
		String plName = Bukkit.getPlayer(player).getName();
		allowedPlayers.put(player, plName);
		allowedPlayers.put(BLANK, "");
		
		x = (int)Math.floor(bounds.getLocation().getX());
		z = (int)Math.floor(bounds.getLocation().getY());
		w = (int)Math.floor(bounds.getWidth());
		l = (int)Math.floor(bounds.getHeight());
	}
	
	protected Plot (Location locA, Location locB, UUID player)
	{
		this(locA.getWorld().getUID(), buildRect(locA,locB), player);
	}
	
	protected Plot (Location loc, int radius, UUID player)
	{
		world = loc.getWorld().getUID();
		allowedPlayers = new HashMap<>();
		
		String plName = Bukkit.getPlayer(player).getName();
		allowedPlayers.put(player, plName);
		allowedPlayers.put(BLANK, "");
		
		x = (int)Math.floor(loc.getX()) - radius;
		z = (int)Math.floor(loc.getZ()) - radius;
		w = 2*radius;
		l = 2*radius;
	}
	
	protected static Rectangle buildRect (Location locA, Location locB)
	{
		Point min = new Point(0, 0);
		Point max = new Point(1, 1);
		
		if (locA.getBlockX() < locB.getBlockX())
		{
			min.move(locA.getBlockX(), min.y);
			max.move(locB.getBlockX() + 1, max.y);
		}
		else
		{
			min.move(locB.getBlockX(), min.y);
			max.move(locA.getBlockX() + 1, max.y);
		}
		if (locA.getBlockZ() < locB.getBlockZ())
		{
			min.move(min.x, locA.getBlockZ());
			max.move(max.x, locB.getBlockZ() + 1);
		}
		else
		{
			min.move(min.x, locB.getBlockZ());
			max.move(max.x, locA.getBlockZ() + 1);
		}
		return new Rectangle(
			(int)Math.floor(min.getX()), (int)Math.floor(min.getY()), 
			(int)(max.getX() - min.getX()), (int)(max.getY() - min.getY())
		);
	}
	
	protected Rectangle getBounds ()
	{
		return new Rectangle(x,z,w,l);
	}
	
	protected boolean contains (Location loc)
	{
		if (! (loc.getWorld().getUID().equals(world)) ) return false;
		
		Rectangle bounds = new Rectangle(x,z,w,l);
		if (bounds.contains(new Point((int)Math.floor(loc.getX()), (int)Math.floor(loc.getZ()))) )
		{
			return true;
		}
		return false;
	}
	
	protected boolean intersects (Plot plot)
	{
		Rectangle me = new Rectangle(x,z,w,l);
		Rectangle him = plot.getBounds();
		
		if (me.intersects(him)) return true;
		
		return false;
	}
	
	protected String listCoords ()
	{
		return "("+x+", "+z+")-("+(x+w)+", "+(z+l)+")";
	}
	
	protected UUID getPlayerID (String name)
	{
		Set<UUID> uuids = allowedPlayers.keySet();
		
		for (UUID id : uuids)
		{
			if (id == null) continue;
			
			if (allowedPlayers.get(id).contains(name)) // why contains(): one's a list of the queued names
			{
				return id;
			}
		}
		return BLANK;
	}
	
	protected boolean permitsPlayer (UUID player)
	{
		Set<UUID> uuids = allowedPlayers.keySet();
		for (UUID id : uuids)
		{
			if (id == null) continue;
			
			if (id.equals(player))
			{
				String plName = Bukkit.getPlayer(player).getName();
				
				// this corrects the locally-stored player name if it's been changed.
				if (! (allowedPlayers.get(id).equals(plName)) )
				{
					if ( ((ArcanePlotsPlugin)Bukkit.getPluginManager().getPlugin("ArcanePlotsPlugin")).shouldWarnMismatch() )
					{
						Bukkit.getLogger().info(Msg.ERR_NAME_MISMATCH);
					}
					allowedPlayers.put(id, plName);
				}
				return true;
			}
		}
		return false;
	}
	
	protected boolean permitsPlayer (String name) // this version's designed for queued names in null.
	{
		if (! (allowedPlayers.containsValue(name)) )
		{
			String[] queue = allowedPlayers.get(BLANK).split(";");
			for (int i=0; i<queue.length; i++)
			{
				if (name == queue[i]) return true; // they're just waiting in line for their UUID still.
			}
		}
		return false;
	}
	
	protected void addPlayer (UUID player)
	{
		Set<UUID> uuids = allowedPlayers.keySet();
		for (UUID id : uuids)
		{
			if (id == null) continue;
			
			if (id.equals(player))
			{
				String plName = Bukkit.getPlayer(player).getName();
				if (plName == null) return;
				
				// this corrects the locally-stored player name if it's been changed.
				if (! (allowedPlayers.get(id).equals(plName)) )
				{
					if ( ((ArcanePlotsPlugin)Bukkit.getPluginManager().getPlugin("ArcanePlotsPlugin")).shouldWarnMismatch() )
					{
						Bukkit.getLogger().info(Msg.ERR_NAME_MISMATCH);
					}
					allowedPlayers.put(id, plName);
				}
				return; // they're already on the list.
			}
		}
		allowedPlayers.put(player, Bukkit.getPlayer(player).getName());
	}
	
	protected void addPlayer (String name)
	{
		if (getPlayerID(name).equals(BLANK))
		{
			if (allowedPlayers.get(BLANK).contains(name))
			{
				return; // already on the queue, buddy.
			}
			else
			{
				allowedPlayers.put(BLANK, allowedPlayers.get(BLANK).concat(name + ";"));
			}
		}
		// funny, they're already here. Guess nothing more needs to be done.
		return; // well, ALMOST nothing needs to be done.
	}
	
	protected boolean removePlayer (UUID player)
	{
		if ( (allowedPlayers.size() > 2) || (allowedPlayers.get(BLANK) != "") )
		// 2, because BLANK counts as an allowed player.
		{
			allowedPlayers.remove(player);
			return true;
		}
		return false;
	}
	
	protected boolean removePlayer (String name)
	{
		if (getPlayerID(name).equals(BLANK))
		{
			if (allowedPlayers.get(BLANK).contains(name)) // because getPlayerID fail-returns BLANK
			{
				String[] parts = allowedPlayers.get(BLANK).split(name + ";");
				
				if ( (parts == null) || (parts.length == 0) )
				{
					// the removed player is the only entry.
					if (allowedPlayers.size() > 1)
					{
						allowedPlayers.put(BLANK, "");
						return true;
					}
				}
				if (parts[1] == null)
				{
					allowedPlayers.put(BLANK, parts[0]); // the removed player was the last entry.
				}
				else
				{
					allowedPlayers.put(BLANK, parts[0] + parts[1]);
				}
				return true;
			}
		}
		return false;
	}
	
	protected String listPlayers ()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(listCoords() + ": ");
		sb.append("Permitted editors: ");
		
		Set<UUID> uuids = allowedPlayers.keySet();
		for (UUID id : uuids)
		{
			if (id == null)
			{
				continue;
			}
			else if (id.equals(BLANK))
			{
				if (allowedPlayers.get(BLANK) != "")
				{
					String[] queue = allowedPlayers.get(BLANK).split(";");
					for (int i=0; i<queue.length; i++)
					{
						sb.append(queue[i]);
						sb.append(", ");
					}
				}
			}
			else
			{
				sb.append(Bukkit.getPlayer(id).getName());
				sb.append(", ");
			}
		}
		sb.delete(sb.length()-2, sb.length()); //removes last comma
		return sb.toString();
	}
	
	@Override
	public boolean equals (Object ob)
	{
		if (ob == null) return false;
		if ( !(ob instanceof Plot)) return false;
		
		Plot that = (Plot)ob;
		if ( !(this.world.equals(that.world)) ) return false;
		if (this.x != that.x) return false;
		if (this.z != that.z) return false;
		if (this.w != that.w) return false;
		if (this.l != that.l) return false;
		
		return true;
	}
	
	@Override
	public int hashCode ()
	{
		return (x<<8)^z;
	}
}
