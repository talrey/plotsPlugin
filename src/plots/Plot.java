/**
 * Plot.java
 * Stores information about where a Plot is, and who can modify it.
 * @author Morios (Mark Talrey)
 * @version RC.1 for Minecraft 1.7.10
 */

package plots;

import java.awt.Point;
import java.awt.Rectangle;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

public final class Plot implements Serializable
{
	private static final long SerialVersionUID = 1710001L;
	
	private UUID world;
	private ArrayList<UUID> allowedPlayers;
	private int x,z,w,l;
	
		
	public Plot (UUID worldUID, Rectangle bounds, UUID player)
	{
		world = worldUID;
		allowedPlayers = new ArrayList<>();
		allowedPlayers.add(player);
		
		x = (int)Math.floor(bounds.getLocation().getX());
		z = (int)Math.floor(bounds.getLocation().getY());
		w = (int)Math.floor(bounds.getWidth());
		l = (int)Math.floor(bounds.getHeight());
	}
	
	public Plot (Location locA, Location locB, UUID player)
	{
		this(locA.getWorld().getUID(), buildRect(locA,locB), player);
	}
	
	public Plot (Location loc, int radius, UUID player)
	{
		world = loc.getWorld().getUID();
		allowedPlayers = new ArrayList<>();
		allowedPlayers.add(player);
		
		x = (int)Math.floor(loc.getX()) - radius;
		z = (int)Math.floor(loc.getZ()) - radius;
		w = 2*radius;
		l = 2*radius;
	}
	
	public static Rectangle buildRect (Location locA, Location locB)
	{
		Point min = new Point(0, 0);
		Point max = new Point(1, 1);
		
		if (locA.getBlockX() < locB.getBlockX())
		{
			min.move(locA.getBlockX(), min.y);
			max.move(locB.getBlockX(), max.y);
		}
		else
		{
			min.move(locB.getBlockX(), min.y);
			max.move(locA.getBlockX(), max.y);
		}
		if (locA.getBlockZ() < locB.getBlockZ())
		{
			min.move(min.x, locA.getBlockZ());
			max.move(max.x, locB.getBlockZ());
		}
		else
		{
			min.move(min.x, locB.getBlockZ());
			max.move(max.x, locA.getBlockZ());
		}
		return new Rectangle(
			(int)Math.floor(min.getX()), (int)Math.floor(min.getY()), 
			(int)Math.floor(max.getX() - min.getX()), (int)Math.floor(max.getY() - min.getY())
		);
	}
	
	public Rectangle getBounds ()
	{
		return new Rectangle(x,z,w,l);
	}
	
	public boolean contains (Location loc)
	{
		if (! (loc.getWorld().getUID().equals(world)) ) return false;
		
		Rectangle bounds = new Rectangle(x,z,w,l);
		if (bounds.contains(new Point((int)loc.getX(), (int)loc.getZ())) )
		{
			return true;
		}
		return false;
	}
	
	public boolean intersects (Plot plot)
	{
		Rectangle me = new Rectangle(x,z,w,l);
		Rectangle him = plot.getBounds();
		
		if (me.intersects(him)) return true;
		
		return false;
	}
	
	public boolean permitsPlayer (UUID player)
	{
		for (int i=0; i<allowedPlayers.size(); i++)
		{
			if (allowedPlayers.get(i).equals(player)) return true;
		}
		return false;
	}

	private int indexOfPlayer (UUID player)
		{
		for (int i=0; i<allowedPlayers.size(); i++)
		{
			if (allowedPlayers.get(i).equals(player)) return i;
		}
		return -1;
	}
	
	public void addPlayer (UUID player)
	{
		for (int i=0; i<allowedPlayers.size(); i++)
		{
			if (allowedPlayers.get(i).equals(player)) return; // they're already on the list.
		}
		allowedPlayers.add(player);
	}
	
	public boolean removePlayer (UUID player)
	{
		if (indexOfPlayer(player) > 0) //gotta keep somebody on the list! Also prevents Exceptions.
		{
			allowedPlayers.remove(indexOfPlayer(player));
			return true;
		}
		return false;
	}
	
	public String listPlayers ()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("("+x+", "+z+")-("+(x+w)+", "+(z+l)+"): ");
		sb.append("Permitted editors: ");
		for (int i=0; i<allowedPlayers.size(); i++)
		{
			try
			{
				OfflinePlayer op = Bukkit.getOfflinePlayer(allowedPlayers.get(i));
				sb.append(op.getPlayer().getName());
				sb.append(", ");
			}
			catch (NullPointerException npe)
			{
				//I doubt this will come up, but it's safe now.
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
