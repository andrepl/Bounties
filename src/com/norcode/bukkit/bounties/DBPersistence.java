package com.norcode.bukkit.bounties;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.PersistenceException;

import org.bukkit.entity.Player;

import com.avaje.ebean.Query;

public class DBPersistence implements IPersistence {
	private final BountiesPlugin plugin;
	
	public DBPersistence(BountiesPlugin plugin) {
		this.plugin = plugin;
		checkDDL();
	}
	private void checkDDL() {
		try{
			plugin.getDatabase().find(Bounty.class).findRowCount();
		}catch (PersistenceException e) {
			plugin.doInstallDDL();
		}
	}

	@Override
	public List<Bounty> getHighestBounties(boolean online, int page) {
		List<String> players = new ArrayList<String>();
		Query<Bounty> query;
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		if (online) {
			for (Player p: plugin.getServer().getOnlinePlayers()) {
				players.add(p.getName());
			}
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("total desc")
					.where()
						.gt("expires", new Date())
						.isNull("claimed")
						.in("target", players)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		} else {
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("total desc")
					.where()
						.gt("expires", new Date())
						.isNull("claimed")
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		}
		
		List<Bounty> results = query.findList();
		return results;
	}

	@Override
	public List<Bounty> getNewestBounties(boolean online, int page) {
		List<String> players = new ArrayList<String>();
		Query<Bounty> query;
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		if (online) {
			for (Player p: plugin.getServer().getOnlinePlayers()) {
				players.add(p.getName());
			}
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added desc")
					.where()
						.gt("expires", new Date())
						.isNull("claimed")
						.in("target", players)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		} else {
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added desc")
					.where()
						.gt("expires", new Date())
						.isNull("claimed")
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		}
		
		List<Bounty> results = query.findList();
		return results;
	}

	@Override
	public List<Bounty> getOldestBounties(boolean online, int page) {
		List<String> players = new ArrayList<String>();
		Query<Bounty> query;
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		if (online) {
			for (Player p: plugin.getServer().getOnlinePlayers()) {
				players.add(p.getName());
			}
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added asc")
					.where()
						.gt("expires", new Date())
						.isNull("claimed")
						.in("target", players)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		} else {
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added asc")
					.where()
						.gt("expires", new Date())
						.isNull("claimed")
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		}
		
		List<Bounty> results = query.findList();
		return results;
	}

	@Override
	public List<Bounty> getPlayerBountiesPlaced(String playerName, boolean online, int page) {
		List<String> players = new ArrayList<String>();
		Query<Bounty> query;
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		if (online) {
			for (Player p: plugin.getServer().getOnlinePlayers()) {
				players.add(p.getName());
			}
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added desc")
					.where()
						.gt("expires", new Date())
						.eq("added_by", playerName)
						.in("target", players)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		} else {
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added desc")
					.where()
						.gt("expires", new Date())
						.eq("added_by", playerName)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		}
		
		List<Bounty> results = query.findList();
		return results;
	}

	@Override
	public List<Bounty> getPlayerBountiesOn(String playerName, boolean online, int page) {
		List<String> players = new ArrayList<String>();
		Query<Bounty> query;
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		if (online) {
			for (Player p: plugin.getServer().getOnlinePlayers()) {
				players.add(p.getName());
			}
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added desc")
					.where()
						.gt("expires", new Date())
						.eq("target", playerName)
						.in("added_by", players)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		} else {
			query = plugin.getDatabase().find(Bounty.class)
					.orderBy("added desc")
					.where()
						.gt("expires", new Date())
						.eq("target", playerName)
					.setMaxRows(PER_PAGE)
					.setFirstRow(PER_PAGE*(page-1));
		}
		
		List<Bounty> results = query.findList();
		return results;
	}

	@Override
	public Bounty getBounty(String playerName) {
		return plugin.getDatabase().find(Bounty.class)
				.where()
					.eq("target", playerName)
					.isNull("claimed")
					.gt("expires", new Date())
				.findUnique();
	}

	@Override
	public void saveBounty(Bounty bounty) {
		plugin.getDatabase().save(bounty);
	}

	@Override
	public void shutdown() {
	}
	@Override
	public List<Bounty> getAllOpenBounties() {
		List<Bounty> results = plugin.getDatabase().find(Bounty.class)
				.orderBy("added desc")
				.where()
					.gt("expires", new Date())
					.isNull("claimed")
				.findList();
		return results;
	}

}
