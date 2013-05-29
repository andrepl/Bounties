package com.norcode.bukkit.bounties;

import java.util.List;

public interface IPersistence { 
	List<Bounty> getAllOpenBounties();
	List<Bounty> getHighestBounties(boolean online, int page);
	List<Bounty> getNewestBounties(boolean online, int page);
	List<Bounty> getOldestBounties(boolean online, int page);
	List<Bounty> getPlayerBountiesPlaced(String playerName,boolean online,  int page);
	List<Bounty> getPlayerBountiesOn(String playerName, boolean online, int page);
	Bounty getBounty(String playerName);
	void saveBounty(Bounty bounty);
	void shutdown();
}