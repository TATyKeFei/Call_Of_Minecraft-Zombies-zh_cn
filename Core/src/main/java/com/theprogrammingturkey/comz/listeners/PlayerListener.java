package com.theprogrammingturkey.comz.listeners;

import com.theprogrammingturkey.comz.COMZombies;
import com.theprogrammingturkey.comz.config.ConfigManager;
import com.theprogrammingturkey.comz.game.Game;
import com.theprogrammingturkey.comz.game.GameManager;
import com.theprogrammingturkey.comz.game.features.Barrier;
import com.theprogrammingturkey.comz.game.features.DownedPlayer;
import com.theprogrammingturkey.comz.game.features.PerkType;
import com.theprogrammingturkey.comz.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

public class PlayerListener implements Listener
{
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		for(Player pl : Bukkit.getOnlinePlayers())
		{
			if(GameManager.INSTANCE.isPlayerInGame(pl))
				pl.hidePlayer(COMZombies.getPlugin(), player);
			else
				pl.showPlayer(COMZombies.getPlugin(), player);
			player.showPlayer(COMZombies.getPlugin(), pl);
		}

		Game game = GameManager.INSTANCE.getGame(player);
		if(game == null)
			return;
		if(game.wasDisconnected(player))
			CommandUtil.sendClickableMessageToPlayer(player, ChatColor.RED + "You can reconnect to your last game with /z rejoin or ", "CLICK HERE", "/z rejoin");
	}

	@EventHandler
	public void onPlayerLeave(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		if(GameManager.INSTANCE.isPlayerInGame(player))
		{
			Game game = GameManager.INSTANCE.getGame(player);
			game.removePlayer(player);
			player.removePotionEffect(PotionEffectType.BLINDNESS);
			player.removePotionEffect(PotionEffectType.SLOW);
		}
		for(Player pl : Bukkit.getOnlinePlayers())
		{
			player.showPlayer(COMZombies.getPlugin(), pl);
			pl.showPlayer(COMZombies.getPlugin(), player);
		}
	}

	/**
	 * Checks if the player is leaving the arena and takes care of his action.
	 */
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		Game game = GameManager.INSTANCE.getGame(player);
		Location toLoc = event.getTo();
		if(game == null || toLoc == null)
			return;

		if(!game.arena.containsBlock(toLoc))
		{
			if(game.getStatus() == Game.GameStatus.INGAME)
			{
				player.teleport(game.arena.getPlayerTPLocation());
				CommandUtil.sendMessageToPlayer(player, ChatColor.RED + "Please do not leave the arena!");
			}
		}


		// Reviving move check
		Location change = toLoc.clone().subtract(event.getFrom().clone());
		if(player.getGameMode() != GameMode.SPECTATOR && game.downedPlayerManager.isDownedPlayer(player) && change.getY() != 0)
			event.setCancelled(true);

		float DEAD_ZONE = 0.001f;

		DownedPlayer downedPlayer = game.downedPlayerManager.getDownedPlayerForReviver(player);
		if(downedPlayer != null)
		{
			if(downedPlayer.isPlayerDown() && change.lengthSquared() > DEAD_ZONE)
			{
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You Moved! You are no longer reviving " + downedPlayer.getPlayer().getName());
				downedPlayer.cancelRevive();
			}
		}


		//Barriers move check
		for(Barrier barrier : game.barrierManager.getBarriers())
		{
			for(Block b : barrier.getBlocks())
			{
				if((int) toLoc.getX() == b.getX() && (int) toLoc.getY() == b.getY() && (int) toLoc.getZ() == b.getZ())
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler
	public void OnPlayerVelocityEvent(PlayerMoveEvent event)
	{
		Player player = event.getPlayer();
		if(GameManager.INSTANCE.isPlayerInGame(player))
		{
			player.setFoodLevel(20);
			int fallDistance = (int) player.getFallDistance();
			if(fallDistance > 2)
			{
				Game game = GameManager.INSTANCE.getGame(player);
				if(game.perkManager.getPlayersPerks(player).contains(PerkType.PHD_FLOPPER))
				{
					double pHealth = player.getHealth();
					Location loc = player.getLocation();
					loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
					for(int i = 0; i < 30; i++)
					{
						float x = COMZombies.rand.nextFloat(2f);
						float y = COMZombies.rand.nextFloat(2f);
						float z = COMZombies.rand.nextFloat(2f);
						player.getWorld().spawnParticle(Particle.LAVA, loc.getX(), loc.getY(), loc.getZ(), 1, x, y, z, 1);
						player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc.getX(), loc.getY(), loc.getZ(), 1, x, y, z, 1);
					}

					for(Entity e : player.getNearbyEntities(5, 5, 5))
						if(e instanceof Mob)
							game.damageMob((Mob) e, player, 12);

					player.setHealth(pHealth);
				}
			}
		}
	}

	@EventHandler
	public void ProjectileHit(EntityDamageEvent event)
	{
		if(event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
		{
			if(event.getEntity() instanceof Player)
			{
				Player player = (Player) event.getEntity();
				if(GameManager.INSTANCE.isPlayerInGame(player))
					event.setCancelled(true);
			}
		}
		else if(event.getCause() == EntityDamageEvent.DamageCause.FALL)
		{
			if(event.getEntity() instanceof Player)
			{
				Player player = (Player) event.getEntity();
				if(GameManager.INSTANCE.isPlayerInGame(player))
					if(GameManager.INSTANCE.getGame(player).perkManager.hasPerk(player, PerkType.PHD_FLOPPER))
						event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void playerExp(PlayerExpChangeEvent event)
	{
		Player player = event.getPlayer();
		if(GameManager.INSTANCE.isPlayerInGame(player))
			player.setExp(0);
	}

	@EventHandler
	public void interact(PlayerInteractAtEntityEvent event)
	{
		if(!(event.getRightClicked() instanceof Player))
			return;

		Player reviver = event.getPlayer();
		Player clickedPlayer = (Player) event.getRightClicked();

		Game game = GameManager.INSTANCE.getGame(reviver);

		if(game == null || !game.isPlayerPlaying(clickedPlayer))
			return;


		Location clickedPlayerLoc = clickedPlayer.getLocation();
		if(clickedPlayerLoc.getWorld() == null || !clickedPlayerLoc.getWorld().equals(reviver.getWorld()))
			return;
		if(reviver.getLocation().distance(clickedPlayerLoc) > ConfigManager.getMainConfig().reviveRange)
			return;

		if(game.downedPlayerManager.isDownedPlayer(reviver))
			return;

		DownedPlayer downedPlayer = game.downedPlayerManager.getDownedPlayer(clickedPlayer);
		if(downedPlayer == null)
			return;

		if(downedPlayer.isBeingRevived())
			return;

		if(!(game.getPlayersInGame().contains(reviver)))
			return;

		if(GameManager.INSTANCE.isPlayerInGame(reviver))
		{
			reviver.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are reviving " + clickedPlayer.getName());
			clickedPlayer.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You are being revived by " + reviver.getName() + "!");
			downedPlayer.startRevive(reviver);
			event.setCancelled(true);
		}
	}
}
