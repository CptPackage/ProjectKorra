package com.projectkorra.projectkorra.waterbending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.util.TempBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class TorrentWave extends WaterAbility {

	private long time;
	private long interval;
	private long cooldown;
	private double radius;
	private double maxRadius;
	private double knockback;
	private double maxHeight;
	private double growSpeed;
	private Location origin;
	private ArrayList<TempBlock> blocks;
	private ArrayList<Entity> affectedEntities;
	private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> heights;

	public TorrentWave(Player player, double radius) {
		this(player, player.getEyeLocation(), radius);
	}

	public TorrentWave(Player player, Location location, double radius) {
		super(player);
		
		this.radius = radius;
		this.interval = 30;
		this.maxHeight = getConfig().getDouble("Abilities.Water.Torrent.Wave.Height");
		this.maxRadius = getConfig().getDouble("Abilities.Water.Torrent.Wave.Radius");
		this.knockback = getConfig().getDouble("Abilities.Water.Torrent.Wave.Knockback");
		this.cooldown = 0;
		this.growSpeed = 0.5;
		this.origin = location.clone();
		this.time = System.currentTimeMillis();
		this.heights = new ConcurrentHashMap<>();
		this.blocks = new ArrayList<TempBlock>();
		this.affectedEntities = new ArrayList<Entity>();
		
		this.knockback = getNightFactor(knockback);
		this.maxRadius = getNightFactor(maxRadius);
		
		initializeHeightsMap();
		start();
		bPlayer.addCooldown(this);
	}

	private void initializeHeightsMap() {
		for (int i = -1; i <= maxHeight; i++) {
			ConcurrentHashMap<Integer, Double> angles = new ConcurrentHashMap<>();
			double dtheta = Math.toDegrees(1 / (maxRadius + 2));
			int j = 0;
			
			for (double theta = 0; theta < 360; theta += dtheta) {
				angles.put(j, theta);
				j++;
			}
			heights.put(i, angles);
		}
	}

	@Override
	public void progress() {
		if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
			remove();
			return;
			
		}

		if (System.currentTimeMillis() > time + interval) {
			if (radius < maxRadius) {
				radius += growSpeed;
			} else {
				remove();
				returnWater();
				return;
			}
			formBurst();
			time = System.currentTimeMillis();
		}
	}

	private void formBurst() {
		for (TempBlock tempBlock : blocks) {
			tempBlock.revertBlock();
		}

		blocks.clear();
		affectedEntities.clear();
		
		ArrayList<Entity> indexList = new ArrayList<Entity>();
		indexList.addAll(GeneralMethods.getEntitiesAroundPoint(origin, radius + 2));
		ArrayList<Block> torrentBlocks = new ArrayList<Block>();

		if (indexList.contains(player)) {
			indexList.remove(player);
		}

		for (int id : heights.keySet()) {
			ConcurrentHashMap<Integer, Double> angles = heights.get(id);
			for (int index : angles.keySet()) {
				double angle = angles.get(index);
				double theta = Math.toRadians(angle);
				double dx = Math.cos(theta) * radius;
				double dy = id;
				double dz = Math.sin(theta) * radius;
				
				Location location = origin.clone().add(dx, dy, dz);
				Block block = location.getBlock();
				
				if (torrentBlocks.contains(block)) {
					continue;
				}
				
				if (isTransparentToEarthbending(player, block)) {
					TempBlock tempBlock = new TempBlock(block, Material.STATIONARY_WATER, (byte) 8);
					blocks.add(tempBlock);
					torrentBlocks.add(block);
				} else {
					angles.remove(index);
					continue;
				}
				
				for (Entity entity : indexList) {
					if (!affectedEntities.contains(entity)) {
						if (entity.getLocation().distanceSquared(location) <= 4) {
							affectedEntities.add(entity);
							affect(entity);
						}
					}
				}

				Random random = new Random();
				for (Block sound : torrentBlocks) {
					if (random.nextInt(50) == 0) {
						playWaterbendingSound(sound.getLocation());
					}
				}
			}
			if (angles.isEmpty()) {
				heights.remove(id);
			}
		}
		if (heights.isEmpty()) {
			remove();
		}
	}

	private void affect(Entity entity) {
		Vector direction = GeneralMethods.getDirection(origin, entity.getLocation());
		direction.setY(0);
		direction.normalize();
		entity.setVelocity(entity.getVelocity().clone().add(direction.multiply(knockback)));
	}

	@Override
	public void remove() {
		super.remove();
		for (TempBlock block : blocks) {
			block.revertBlock();
		}
	}

	private void returnWater() {
		Location location = new Location(origin.getWorld(), origin.getX() + radius, origin.getY(), origin.getZ());
		if (!location.getWorld().equals(player.getWorld())) {
			return;
		}
		double radiusOffsetSquared = (maxRadius + 5) * (maxRadius + 5);
		if (location.distanceSquared(player.getLocation()) > radiusOffsetSquared) {
			return;
		}
		new WaterReturn(player, location.getBlock());
	}

	@Override
	public String getName() {
		return "Torrent";
	}

	@Override
	public Location getLocation() {
		return origin;
	}

	@Override
	public long getCooldown() {
		return cooldown;
	}
	
	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getMaxRadius() {
		return maxRadius;
	}

	public void setMaxRadius(double maxRadius) {
		this.maxRadius = maxRadius;
	}

	public double getKnockback() {
		return knockback;
	}

	public void setKnockback(double knockback) {
		this.knockback = knockback;
	}

	public double getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(double maxHeight) {
		this.maxHeight = maxHeight;
	}

	public double getGrowSpeed() {
		return growSpeed;
	}

	public void setGrowSpeed(double growSpeed) {
		this.growSpeed = growSpeed;
	}

	public Location getOrigin() {
		return origin;
	}

	public void setOrigin(Location origin) {
		this.origin = origin;
	}

	public ArrayList<TempBlock> getBlocks() {
		return blocks;
	}

	public ArrayList<Entity> getAffectedEntities() {
		return affectedEntities;
	}

	public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Double>> getHeights() {
		return heights;
	}

	public void setCooldown(long cooldown) {
		this.cooldown = cooldown;
	}

}
