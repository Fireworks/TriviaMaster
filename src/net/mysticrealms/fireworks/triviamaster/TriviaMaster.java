package net.mysticrealms.fireworks.triviamaster;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class TriviaMaster extends JavaPlugin {

	public Configuration config;
	public List<String> questions;
	public List<String> answers;
	public String startMessage, timeUp, endMessage, oneWinner, tiedWinners,
			noWinners, answered;
	public Map<Player, Integer> points = new ConcurrentHashMap<Player, Integer>();
	public List<ItemStack> rewards = new ArrayList<ItemStack>();
	public int timeBetween, currentQuestion, duration;
	public long end;
	public double moneyReward;
	public boolean isRunning;
	public Integer skipTask;
	public Economy economy;

	@Override
	public void onEnable() {

		Bukkit.getPluginManager().registerEvents(new TriviaMasterListener(this), this);
		setupEconomy();

		if (!loadConfig()) {
			getLogger().severe("Something is wrong with the config! Disabling!");
			setEnabled(false);
			return;
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (cmd.getName().equalsIgnoreCase("triviamaster") && args.length == 0) {
			sender.sendMessage(ChatColor.GOLD + "Current commands: " + ChatColor.DARK_RED + "/tm stop, /tm start, /tm reload");
			return true;
		}

		if (cmd.getName().equalsIgnoreCase("triviamaster") && args[0].equalsIgnoreCase("start")) {
			runQuiz();
		}

		if (cmd.getName().equalsIgnoreCase("triviamaster") && args[0].equalsIgnoreCase("stop")) {
			stopQuiz();
		}

		if (cmd.getName().equalsIgnoreCase("triviamaster") && args[0].equalsIgnoreCase("reload")) {
			if (loadConfig()) {
				sender.sendMessage(ChatColor.GOLD + "Config reloaded!");
			} else {
				sender.sendMessage(ChatColor.GOLD + "Config failed to reload!");
			}
		}
		return true;
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") != null) {
			RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			if (economyProvider != null) {
				economy = economyProvider.getProvider();
			}
			getLogger().info("Vault found and loaded.");
			return economy != null;
		}
		economy = null;
		getLogger().info("Vault not found - money reward will not be used.");
		return false;
	}

	public String convertMessage(String s, Player p, List<Player> l, Integer max) {
		s = ChatColor.translateAlternateColorCodes('&', s);
		if (p != null)
			s = s.replace("[player]", p.getName());
		if (l != null)
			s = s.replace("[winner]", (l.get(0).getName()));
		if (max != null)
			s = s.replace("[points]", Integer.toString(max));
		return s;
	}

	public boolean loadConfig() {
		rewards.clear();
		reloadConfig();
		config = this.getConfig();

		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}

		if (config.isString("startMessage")) {
			startMessage = config.getString("startMessage");
		} else {
			return false;
		}

		if (config.isString("timeUp")) {
			timeUp = config.getString("timeUp");
		} else {
			return false;
		}

		if (config.isString("endMessage")) {
			endMessage = config.getString("endMessage");
		} else {
			return false;
		}

		if (config.isString("oneWinner")) {
			oneWinner = config.getString("oneWinner");
		} else {
			return false;
		}

		if (config.isString("tiedWinners")) {
			tiedWinners = config.getString("tiedWinners");
		} else {
			return false;
		}

		if (config.isString("noWinners")) {
			noWinners = config.getString("noWinners");
		} else {
			return false;
		}

		if (config.isString("answered")) {
			answered = config.getString("answered");
		} else {
			return false;
		}

		if (config.isList("questions")) {
			questions = config.getStringList("questions");
		} else {
			return false;
		}

		if (config.isList("answers")) {
			answers = config.getStringList("answers");
		} else {
			return false;
		}

		if (config.isInt("timeBetween")) {
			timeBetween = config.getInt("timeBetween");
		} else {
			return false;
		}

		if (config.isInt("duration")) {
			duration = config.getInt("duration");
		} else {
			return false;
		}

		if (config.isDouble("moneyReward") || config.isInt("moneyReward")) {
			moneyReward = config.getDouble("moneyReward");
		} else {
			return false;
		}

		if (config.isList("itemRewards")) {
			for (Object i : config.getList("itemRewards", new ArrayList<String>())) {
				if (i instanceof String) {
					final String[] parts = ((String) i).split(" ");
					final int[] intParts = new int[parts.length];
					for (int e = 0; e < parts.length; e++) {
						try {
							intParts[e] = Integer.parseInt(parts[e]);
						} catch (final NumberFormatException exception) {
							return false;
						}
					}
					if (parts.length == 1) {
						rewards.add(new ItemStack(intParts[0], 1));
					} else if (parts.length == 2) {
						rewards.add(new ItemStack(intParts[0], intParts[1]));
					} else if (parts.length == 3) {
						rewards.add(new ItemStack(intParts[0], intParts[1], (short) intParts[2]));
					}
				} else {
					return false;
				}
			}
		} else {
			return false;
		}

		return true;
	}

	public void runQuiz() {
		isRunning = true;
		currentQuestion = 0;
		getServer().broadcastMessage(convertMessage(startMessage, null, null, null));
		showQuestion();
	}

	public int currentQuestion() {
		return currentQuestion;
	}

	public void showQuestion() {
		isRunning = true;
		BukkitScheduler s = Bukkit.getScheduler();
		getServer().broadcastMessage("Question " + (currentQuestion() + 1) + " :");
		getServer().broadcastMessage(ChatColor.GREEN + questions.get(currentQuestion()));
		skipTask = s.scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				Bukkit.broadcastMessage(convertMessage(timeUp, null, null, null));
				nextQuestion();
			}
		}, duration * 20);
	}

	public void nextQuestion() {
		isRunning = false;

		BukkitScheduler s = Bukkit.getScheduler();
		s.cancelTask(skipTask);
		currentQuestion++;

		if (currentQuestion == questions.size()) {
			stopQuiz();
			return;
		}
		s.scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				showQuestion();
			}
		}, timeBetween * 20);

	}

	public int getPoints(Player p) {
		if (points.get(p) == null) {
			return 0;
		} else {
			return points.get(p);
		}
	}

	public void stopQuiz() {
		BukkitScheduler s = Bukkit.getScheduler();
		s.cancelTask(skipTask);
		getServer().broadcastMessage(convertMessage(endMessage, null, null, null));
		List<Player> winners = new ArrayList<Player>();
		int max = 0;

		for (Map.Entry<Player, Integer> m : points.entrySet()) {
			if (m.getValue() > max) {
				max = m.getValue();
				winners.clear();
				winners.add(m.getKey());
			} else if (m.getValue() == max) {
				winners.add(m.getKey());
			}
		}

		if (winners.size() == 1)
			Bukkit.broadcastMessage(convertMessage(oneWinner, null, winners, max));
		else if (winners.size() > 1) {
			Bukkit.broadcastMessage(convertMessage(tiedWinners, null, winners, max));
			for (Player p : winners) {
				Bukkit.broadcastMessage(ChatColor.GOLD + "* " + ChatColor.DARK_RED + p.getName());
			}
		} else
			Bukkit.broadcastMessage(convertMessage(noWinners, null, null, null));

		for (Player p : winners) {
			Inventory i = p.getInventory();
			for (ItemStack j : rewards) {
				i.addItem(j);
			}
			if (economy != null) {
				economy.depositPlayer(p.getName(), moneyReward);
			}
		}
		currentQuestion = 0;
		isRunning = false;
		winners.clear();
		points.clear();
	}
}
