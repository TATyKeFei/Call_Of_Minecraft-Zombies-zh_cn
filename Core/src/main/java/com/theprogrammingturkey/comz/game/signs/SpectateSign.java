package com.theprogrammingturkey.comz.game.signs;

import com.theprogrammingturkey.comz.game.Game;
import com.theprogrammingturkey.comz.game.GameManager;
import com.theprogrammingturkey.comz.util.CommandUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

public class SpectateSign implements IGameSign
{
	@Override
	public void onBreak(Game game, Player player, Location location)
	{

	}

	@Override
	public void onInteract(Game game, Player player, Location location, String[] lines)
	{
		game = GameManager.INSTANCE.getGame(lines[3]);
		if(game == null)
		{
			CommandUtil.sendMessageToPlayer(player, ChatColor.DARK_RED + "Invalid Arena!");
			return;
		}
		player.performCommand("zombies spec " + game.getName());
	}

	@Override
	public void onChange(Game game, Player player, SignChangeEvent sign)
	{
		String name = sign.getLine(2);
		game = GameManager.INSTANCE.getGame(name);
		if(game == null)
		{
			sign.setLine(0, ChatColor.RED + "" + ChatColor.BOLD + "Arena name is");
			sign.setLine(1, ChatColor.RED + "" + ChatColor.BOLD + "not a valid");
			sign.setLine(2, ChatColor.RED + "" + ChatColor.BOLD + "arena!");
			sign.setLine(3, "");
			return;
		}
		sign.setLine(0, ChatColor.RED + "[Zombies]");
		sign.setLine(1, ChatColor.AQUA + "Spectate");
		sign.setLine(2, ChatColor.RED + "Arena:");
		sign.setLine(3, name);
	}

	@Override
	public boolean requiresGame()
	{
		return false;
	}
}
