package com.norcode.bukkit.bounties;

import java.util.List;

public interface IPersistence { 
	List<Bounty> getAllOpenBounties();
	List<Bounty> getHighestBounties(int page);
	List<Bounty> getNewestBounties(int page);
	List<Bounty> getOldestBounties(int page);
	List<Bounty> getPlayerBountiesPlaced(String playerName, int page);
	List<Bounty> getPlayerBountiesOn(String playerName, int page);
	Bounty getBounty(String playerName);
	void saveBounty(Bounty bounty);
	void shutdown();
}