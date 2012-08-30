package net.mysticrealms.fireworks.triviamaster;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class TriviaMasterListener implements Listener {

	private TriviaMaster plugin;

	public TriviaMasterListener(TriviaMaster tm) {
		plugin = tm;
	}

	@EventHandler
	public synchronized void onPlayerChat(AsyncPlayerChatEvent event) {
		String message = event.getMessage();
		final Player p = event.getPlayer();
		
		if (plugin.isRunning) {
			
			if(message.equalsIgnoreCase(plugin.answers.get(plugin.currentQuestion)) && p.hasPermission("triviamaster.participate")){
				plugin.points.put(p, plugin.getPoints(p) + 1);
				plugin.isRunning = false;
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
					public void run(){
						Bukkit.broadcastMessage(plugin.convertMessage(plugin.answered, p, null, null));
						plugin.nextQuestion();
					}
				}, 1);
			}
			
		}
	}
}
