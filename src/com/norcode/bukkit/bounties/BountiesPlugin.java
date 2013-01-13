package com.norcode.bukkit.bounties;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlUpdate;

public class BountiesPlugin extends JavaPlugin implements Listener {
    public static Economy economy = null;
    private IPersistence persistence;
    private Set<String> watchedPlayers;
    
	@Override
	public void onEnable() {
		FileConfiguration config = getConfig();
		config.options().copyDefaults(true);
		saveConfig();
		if (!setupVault()) {
			getLogger().severe("Bounties disabled, failed to hook vault.");
		} else {
			getLogger().info("Bounties loaded.");
		}
		persistence = new DBPersistence(this);
		watchedPlayers = new HashSet<String>();
		
		for (Bounty b: persistence.getAllOpenBounties()) {
			watchedPlayers.add(b.getTarget());
		}
		
		getServer().getPluginManager().registerEvents(this, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equalsIgnoreCase("bounty")) {
			if (!sender.hasPermission("bounties.use")) { 
				sender.sendMessage(getMsg("no_permission"));
				return true;
			}
			if (args.length == 0) {
				sender.sendMessage(getMsg("usage"));
				return true;
			}
			
			String[] params = Arrays.copyOfRange(args, 1, args.length);
			switch(args[0].toLowerCase()) {
			case "mine":
				myBounty(sender, params);
				break;
			case "add":
				addBounty(sender, params);
				break;
			case "list":
				listBounties(sender, params);
				break;
			case "locate":
				if (getConfig().getBoolean("allow_locate")) {
					locateBounty(sender, params);
				} else {
					sender.sendMessage(getMsg("locate_disabled"));
				}
				break;
			case "help":
				help(sender, params);
				break;
			case "cancel":
				cancelBounty(sender, params);
				break;
				
			case "reload":
				if (sender.hasPermission("bounties.admin")) {
					reloadConfig();
					sender.sendMessage(getMsg("config_reloaded"));
					break;
				} else {
					return false;
				}
				
			default:
				sender.sendMessage(getMsg("usage"));
			}
			return true;
		}
		return false;
	}
	
	public OfflinePlayer findPlayer(String partialName) {
		List<Player> matches = getServer().matchPlayer(partialName);
		OfflinePlayer p = null;
		if (matches.size() == 0) {
			return getServer().getOfflinePlayer(partialName);
		} else if (matches.size() == 1) {
			return matches.get(0);
		}
		return null;
	}
	
	private void myBounty(CommandSender sender, String[] params) {
		Bounty b = persistence.getBounty(sender.getName());
		if (b == null) {
			sender.sendMessage(getMsg("no_bounty", "target", "you"));
		} else {
			sender.sendMessage(getMsg("my_bounty", "added_by", b.getAddedBy(), "added", b.getAdded().toLocaleString(), 
					                  "expires", b.getExpires().toLocaleString(), "total", NumberFormat.getCurrencyInstance().format(b.getTotal()),
					                  "num_contributors", Integer.toString(b.getNumContributors())));
			return;
					                    
		}
	}
	
	private void cancelBounty(CommandSender sender, String[] params) {
		if (params.length < 1) {
			sender.sendMessage(getMsg("cancel_usage"));
			return;
		}
		OfflinePlayer target = findPlayer(params[0]);
		Bounty b = persistence.getBounty(target.getName());
		if (b == null) {
			sender.sendMessage(getMsg("no_bounty"));
			return;
		}
		if (sender.hasPermission("bounties.admin") || b.getAddedBy().equals(sender.getName())) {
			b.setExpires(new Date());
			persistence.saveBounty(b);
			notifyCancelledBounty(b, (Player)sender);
		}
	}
	
	private void addBounty(CommandSender sender, String[] params) {
		String usage = getMsg("add_usage");
		if (params.length < 2) {
			sender.sendMessage(usage);
			return;
		}
		String target = params[0];
		BigDecimal amount;
		try {
			amount = new BigDecimal(params[1]);
		} catch (IllegalArgumentException e) {
			sender.sendMessage(usage);
			return;
		}
		double min = getConfig().getDouble("minimum_bounty");
		double max = getConfig().getDouble("maximum_bounty");
		if (amount.doubleValue() < min) {
			sender.sendMessage(getMsg("minimum_bounty", "min", 
					NumberFormat.getCurrencyInstance().format(min)));
			return;
		}
		if (sender instanceof Player) { 
			EconomyResponse resp = getEconomy().withdrawPlayer(sender.getName(), amount.doubleValue());
			if (!resp.transactionSuccess()) {
				sender.sendMessage(getMsg("insufficient_funds", "amount", NumberFormat.getCurrencyInstance().format(amount)));
				return;
			}
		}
		OfflinePlayer p = findPlayer(target);
		if (p == null) {
			sender.sendMessage(getMsg("no_player_match", "target", target));
			return;
		} else {
			Bounty b = persistence.getBounty(p.getName());
			
			if (b == null) {
				if (max != -1 && amount.doubleValue() > max) {
					sender.sendMessage(getMsg("max_bounty_exceeded"));
					getEconomy().depositPlayer(sender.getName(), amount.doubleValue());
					return;
				}
				b = new Bounty();
				b.setTarget(p.getName());
				b.setAdded(new Date());
				b.setAddedBy(sender.getName());
				b.setContributors("");
				b.setExpires(addDays(new Date(), getConfig().getInt("expiry_days")));
				b.setClaimed(null);
				b.setClaimedBy(null);
				b.setTotal(removeTax(amount));
				persistence.saveBounty(b);
				notifyNewBounty(b);
			} else {
				BigDecimal newTotal = b.getTotal().add(removeTax(amount));
				if (max != -1 && newTotal.doubleValue() > max) {
					sender.sendMessage(getMsg("max_bounty_exceeded"));
					getEconomy().depositPlayer(sender.getName(), amount.doubleValue());
					return;
				}
				b.setTotal(newTotal);
				if (!sender.getName().equals(b.getAddedBy())) {
					Set<String> contribs = new HashSet<String>();
					String cs = sender.getName() + ",";
					for (String s: b.getContributors().split(",")) {
						s = s.trim();
						if (s.length() > 0 && !s.equals(sender.getName())) {
							cs += s + ",";
						}
					}
					if (cs.endsWith(",")) cs = cs.substring(0,cs.length()-1);
					b.setContributors(cs);
				}
				b.setExpires(addDays(new Date(), getConfig().getInt("expiry_days")));
				persistence.saveBounty(b);
				notifyUpdatedBounty(b, sender.getName());
			}
			watchedPlayers.add(b.getTarget());

		}
	}
	
	public BigDecimal removeTax(BigDecimal amt) {
		int percentage = getConfig().getInt("tax_percentage");
		BigDecimal tax = amt.multiply(new BigDecimal(percentage/100.0));
		return amt.subtract(tax);
	}

   public String getMsg(String key, String... params) {
		String t = getConfig().getConfigurationSection("messages").getString(key);
		String k;
		String v;
		for (int i=0;i<params.length-1;i+=2) {
			k = params[i];
			v = params[i+1];
			
			t = t.replaceAll("\\{\\s*" + Pattern.quote(k) + "\\s*\\}", Matcher.quoteReplacement(v));
			
		}
		return t;
	}

   public void notifyCancelledBounty(Bounty b, Player p) {
	   getServer().broadcastMessage(getMsg("cancelled_bounty", "target", b.getTarget(), 
               "player", p.getName()));
   }
   
   public void notifyUpdatedBounty(Bounty b, String playerName) {
	   getServer().broadcastMessage(getMsg("updated_bounty", "target", b.getTarget(), 
			                               "total", NumberFormat.getCurrencyInstance().format(b.getTotal()), 
			                               "added_by", playerName, 
			                               "expires", b.getExpires().toLocaleString()));	   
   }
	
   
   public void notifyNewBounty(Bounty b) {
	   getServer().broadcastMessage(getMsg("new_bounty", "target", b.getTarget(), 
			   							   "total", NumberFormat.getCurrencyInstance().format(b.getTotal()), 
			   							   "added_by", b.getAddedBy(),
			   							   "expires", b.getExpires().toLocaleString()));
   }

   public void notifyClaimedBounty(Bounty b) {
	   getServer().broadcastMessage(getMsg("claimed_bounty", "target", b.getTarget(), 
				   "total", NumberFormat.getCurrencyInstance().format(b.getTotal()), 
				   "claimed_by", b.getClaimedBy()));
   }
   
   public void help(CommandSender sender, String[] params) {
	   List<String> helpLines = getConfig().getStringList("messages.help");
	   for (String line: helpLines) {
		   sender.sendMessage(line);
	   }
   }
   
   public void locateBounty(CommandSender sender, String[] params) {
	   if (!(sender.hasPermission("bounties.locate")&&sender.hasPermission("bounties.use"))) { 
			sender.sendMessage(getMsg("no_locate_permission"));
			return;
	   }
	   if (params.length < 1) {
		   sender.sendMessage(getMsg("locate_usage"));
		   return;
	   }
	   Player target = null;
	   List<Player> matches = getServer().matchPlayer(params[0]);
	   if (matches.size() == 0) {
		   OfflinePlayer op = getServer().getOfflinePlayer(params[0]);
		   if (op != null) {
			   sender.sendMessage(getMsg("player_offline", "target", op.getName()));
			   return;
		   }
		   sender.sendMessage(getMsg("no_player_match", "target", params[0]));
		   return;   
	   } else if (matches.size() > 1) {
		   sender.sendMessage(getMsg("multiple_player_match", "target", params[0]));
		   return;
	   }
	   target = matches.get(0);
	   String playerName = target.getName();
	   Bounty b = persistence.getBounty(playerName);
	   if (b == null) {
		   sender.sendMessage(getMsg("no_bounty", "target", playerName));
		   return;
	   } 
	   
	   if (sender instanceof Player) {
		   EconomyResponse resp = getEconomy().withdrawPlayer(sender.getName(), getConfig().getDouble("locate_cost"));
		   if (!resp.transactionSuccess()) {
			   sender.sendMessage(getMsg("insufficient_funds", "amount", 
					   NumberFormat.getCurrencyInstance().format(getConfig().getDouble("locate_cost"))));
			   return;
		   }
	   }
	   getLocation((Player)sender, target);
   }
   
   public void getLocation(Player hunter, Player target) {
	   int distance = (int)Math.floor(hunter.getLocation().distance(target.getLocation()));
	   hunter.sendMessage(getMsg("locate", "target", target.getName(), "distance", Integer.toString(distance)));
   }
   
   public void listBounties(CommandSender sender, String[] params) {
       int page = 1;
       String type = "newest";
       boolean online = false;
       
       if (params.length > 0) {
    	   if (params.length > 3) {
    		   sender.sendMessage(getMsg("list_usage"));
    		   return;
    	   }
    	   int i = params.length-1;
    	   try {
    		   page = Integer.parseInt(params[i].trim());
    		   i -= 1;
    	   } catch (IllegalArgumentException ex) {
    	   }
    	   if (i >= 0) {
    		   if (params[i].toLowerCase().equals("online")) {
    			   online = true;
    			   i -= 1;
    		   }
    	   }
    	   if (i >= 0) {
    		   type = params[i];
    	   }
       }
	   
	   List<Bounty> bounties;
	   switch (type.toLowerCase()) {
	   case "highest":
		   bounties = persistence.getHighestBounties(online, page);
		   break;
	   case "newest":
		   bounties = persistence.getNewestBounties(online, page);
		   break;
	   case "oldest":
		   bounties = persistence.getOldestBounties(online, page);
		   break;
	   case "mine":
		   bounties = persistence.getPlayerBountiesOn(sender.getName(), online, page);
		   break;
	   case "placed":
		   bounties = persistence.getPlayerBountiesPlaced(sender.getName(), online, page);
		   break;
	   default:
		   sender.sendMessage(getMsg("list_usage"));
		   return;
	   }
	   sender.sendMessage(getMsg("list_head_"+type.toLowerCase()));
	   if (bounties.size() == 0) {
		   sender.sendMessage(getMsg("no_results"));
		   return;
	   }
	   int i = getConfig().getInt("per_page") * (page-1);
	   for (Bounty b: bounties) {
		   i++;
		   sender.sendMessage(getMsg("list_item", "number", Integer.toString(i),
				   "target", b.getTarget(), "total", NumberFormat.getCurrencyInstance().format(b.getTotal()), 
                   "added_by", b.getAddedBy(), 
                   "expires", b.getExpires().toLocaleString()));
	   }
	   if (bounties.size() == getConfig().getInt("per_page")) {
		   sender.sendMessage(getMsg("next_page", "page", Integer.toString(page+1), "type", type.toLowerCase()));
	   }
   }
   
   public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, days); 
        return cal.getTime();
	}
	
   public Economy getEconomy() {
		return this.economy;
	}

	private boolean setupVault() {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }
        return (economy != null);
    }
	
	@Override
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> classes = new LinkedList<Class<?>>(); 
		classes.add(Bounty.class);
		return classes;
	}
	
	public void doInstallDDL() {
		this.installDDL();
	}
	
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (watchedPlayers.contains(event.getPlayer().getName())) {
			final Bounty b = persistence.getBounty(event.getPlayer().getName());
			if (b != null) {
				event.getPlayer().sendMessage(getMsg("login_warning", "added_by", b.getAddedBy(), 
						"total", NumberFormat.getCurrencyInstance().format(b.getTotal()), "expires",
						b.getExpires().toLocaleString()));
			}
		}
	}
	
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		Player killed = event.getEntity();
		Player killer = event.getEntity().getKiller();
		if (killer == null) return;
		if (!killer.hasPermission("bounties.use")) return;
		if (killer.getName().equals(event.getEntity().getName())) {
			killer.sendMessage("cannot-self-claim");
			return;
		}
		
		if (watchedPlayers.contains(killed.getName())) {
			Bounty b = persistence.getBounty(killed.getName());
			if (b != null) {
				b.setClaimedBy(killer.getName());
				b.setClaimed(new Date());
				persistence.saveBounty(b);
				getEconomy().depositPlayer(killer.getName(), b.getTotal().doubleValue());
				notifyClaimedBounty(b);
			}
			watchedPlayers.remove(killed.getName());
			return;
		}
	}
	
}
