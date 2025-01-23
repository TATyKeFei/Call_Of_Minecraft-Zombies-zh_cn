package com.theprogrammingturkey.comz.leaderboards;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.theprogrammingturkey.comz.COMZombies;
import com.theprogrammingturkey.comz.config.COMZConfig;
import com.theprogrammingturkey.comz.config.ConfigManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class Leaderboard
{
	private static final List<PlayerStats> ALL_PLAYERS = new ArrayList<>();

	public static void getTopX(StatsCategory cat, int size, Player player)
	{
		String uuid = player.getUniqueId().toString();
		List<PlayerStats> sorted = sort(cat);
		List<PlayerStats> toReturn = sorted.subList(0, Math.min(sorted.size(), Math.min(size, 20)));

		boolean has = false;
		for(int i = 0; i < toReturn.size(); i++)
		{
			PlayerStats stat = toReturn.get(i);
			player.sendRawMessage((i + 1) + ") " + stat.getStat(cat) + "  " + stat.getPlayerDisplay());
			if(stat.getPlayerUUID().equalsIgnoreCase(uuid))
				has = true;
		}

		if(!has)
		{
			for(int i = 0; i < sorted.size(); i++)
			{
				PlayerStats stat = sorted.get(i);
				if(stat.getPlayerUUID().equalsIgnoreCase(uuid))
					player.sendRawMessage((i + 1) + ") " + stat.getStat(cat) + "  " + stat.getPlayerDisplay());
			}
		}
	}

	public static PlayerStats getPosX(StatsCategory cat, int pos)
	{
		List<PlayerStats> sorted = sort(cat);
		return sorted.size() > pos ? sorted.get(pos) : null;
	}

	public static void addPlayerStats(PlayerStats stat)
	{
		ALL_PLAYERS.add(stat);
	}

	public static PlayerStats getPlayerStatFromPlayer(Player p)
	{
		for(PlayerStats ps : ALL_PLAYERS)
			if(ps.getPlayerUUID().equals(p.getUniqueId().toString()))
				return ps;

		PlayerStats stat = PlayerStats.initPlayerStats(p);
		ALL_PLAYERS.add(stat);
		return stat;
	}

	public static PlayerStats getPlayerStatFromPlayer(OfflinePlayer p)
	{
		for(PlayerStats ps : ALL_PLAYERS)
			if(ps.getPlayerUUID().equals(p.getUniqueId().toString()))
				return ps;

		PlayerStats stat = PlayerStats.initPlayerStats(p);
		ALL_PLAYERS.add(stat);
		return stat;
	}

	private static List<PlayerStats> sort(StatsCategory cat)
	{
		List<PlayerStats> sortingList = new ArrayList<>(ALL_PLAYERS);
		sortingList.sort((a, b) -> b.getStat(cat) - a.getStat(cat));
		return sortingList;
	}

	public static void loadLeaderboard()
	{
		JsonElement jsonElement = ConfigManager.getConfig(COMZConfig.STATS).getJson();
		if(jsonElement.isJsonNull())
		{
			COMZombies.log.log(Level.SEVERE, "Failed to load in the stats");
			return;
		}
		JsonObject statsJson = jsonElement.getAsJsonObject();
		for(Map.Entry<String, JsonElement> entry : statsJson.entrySet())
			if(entry.getValue().isJsonObject())
				Leaderboard.addPlayerStats(PlayerStats.loadPlayerStats(entry.getKey(), entry.getValue().getAsJsonObject()));
	}

	public static void saveLeaderboard()
	{
		JsonObject playerStatsJson = new JsonObject();
		for(PlayerStats playerStats : ALL_PLAYERS)
			playerStatsJson.add(playerStats.getPlayerUUID(), playerStats.save());

		ConfigManager.getConfig(COMZConfig.STATS).saveConfig(playerStatsJson);
	}
}
