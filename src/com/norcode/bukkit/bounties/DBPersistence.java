package com.norcode.bukkit.bounties;

import java.util.Date;
import java.util.List;

import javax.persistence.PersistenceException;

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
	public List<Bounty> getHighestBounties(int page) {
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		Query<Bounty> query = plugin.getDatabase().find(Bounty.class)
			.orderBy("total desc")
			.where()
				.gt("expires", new Date())
				.isNull("claimed")
			.setMaxRows(PER_PAGE)
			.setFirstRow(PER_PAGE*(page-1));
		List<Bounty> results = query.findList();
		return results;
	}

	@Override
	public List<Bounty> getNewestBounties(int page) {
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		Query<Bounty> query = plugin.getDatabase().find(Bounty.class)
				.where()
					.gt("expires", new Date())
					.isNull("claimed")
				.orderBy("added desc")
				.setMaxRows(PER_PAGE)
				.setFirstRow(PER_PAGE*(page-1));
			List<Bounty> results = query.findList();
			return results;
	}

	@Override
	public List<Bounty> getOldestBounties(int page) {
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		Query<Bounty> query = plugin.getDatabase().find(Bounty.class)
				.where()
					.gt("expires", new Date())
					.isNull("claimed")
				.orderBy("added asc")
				.setMaxRows(PER_PAGE)
				.setFirstRow(PER_PAGE*(page-1));
			List<Bounty> results = query.findList();
			return results;	
	}

	@Override
	public List<Bounty> getPlayerBountiesPlaced(String playerName, int page) {
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		List<Bounty> results = plugin.getDatabase().find(Bounty.class)
				
				.orderBy("added desc")
				.setMaxRows(PER_PAGE)
				.setFirstRow(PER_PAGE*(page-1))
				.where()
					.eq("added_by", playerName)
					.gt("expires", new Date())
					
				.findList();
		return results;
	}

	@Override
	public List<Bounty> getPlayerBountiesOn(String playerName, int page) {
		int PER_PAGE = plugin.getConfig().getInt("per_page");
		List<Bounty> results = plugin.getDatabase().find(Bounty.class)
				.orderBy("added desc")
				.setMaxRows(PER_PAGE)
				.setFirstRow(PER_PAGE*(page-1))
				.where()
					.eq("target", playerName)
				.findList();
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
