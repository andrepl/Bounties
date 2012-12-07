package com.norcode.bukkit.bounties;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


@Entity
@Table(name="bounties_bounty")
public class Bounty {
	@Id private Long id;
	@Column private String target;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column private Date added;
	@Column(name="added_by") private String addedBy;
	
	@Lob @Column private String contributors;
	
	@Column(scale=2) private BigDecimal total;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable=true) private Date claimed;
	@Column(name="claimed_by", nullable=true) private String claimedBy;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable=true) private Date expires;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public Date getAdded() {
		return added;
	}
	public void setAdded(Date added) {
		this.added = added;
	}
	public String getAddedBy() {
		return addedBy;
	}
	public void setAddedBy(String added_by) {
		this.addedBy = added_by;
	}
	public String getContributors() {
		return contributors;
	}
	public void setContributors(String contributors) {
		this.contributors = contributors;
	}
	public BigDecimal getTotal() {
		return total;
	}
	public void setTotal(BigDecimal total) {
		this.total = total;
	}
	public Date getClaimed() {
		return claimed;
	}
	public void setClaimed(Date claimed) {
		this.claimed = claimed;
	}
	public String getClaimedBy() {
		return claimedBy;
	}
	public void setClaimedBy(String claimed_by) {
		this.claimedBy = claimed_by;
	}
	public Date getExpires() {
		return expires;
	}
	public void setExpires(Date expires) {
		this.expires = expires;
	}
	
	public int getNumContributors() {
		int i = 0;
		for (String s: contributors.split(",")) {
			if (s.trim().length() >= 1) {
				i++;
			}
		}
		return i;
	}
}
