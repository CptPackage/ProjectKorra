package com.projectkorra.projectkorra.airbending;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.avatar.AvatarState;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.earthbending.EarthBlast;
import com.projectkorra.projectkorra.earthbending.SandSpout;
import com.projectkorra.projectkorra.firebending.BlazeArc;
import com.projectkorra.projectkorra.firebending.Combustion;
import com.projectkorra.projectkorra.firebending.FireBlast;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import com.projectkorra.projectkorra.waterbending.WaterSpout;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class AirShield extends AirAbility {

	private boolean isToggledByAvatarState;
	private double maxRadius;
	private double radius;
	private double speed;
	private int numberOfStreams;
	private int particleCount;
	private Random random;
	private HashMap<Integer, Integer> angles;
	
	public AirShield(Player player) {
		super(player);

		this.maxRadius = getConfig().getDouble("Abilities.Air.AirShield.Radius");
		this.isToggledByAvatarState = getConfig().getBoolean("Abilities.Air.AirShield.IsAvatarStateToggle");
		this.radius = this.maxRadius;
		this.speed = 10;
		this.numberOfStreams = (int) (.75 * this.maxRadius);
		this.particleCount = 5;
		this.random = new Random();
		this.angles = new HashMap<>();

		if (bPlayer.isAvatarState() && CoreAbility.hasAbility(player, AirShield.class) && isToggledByAvatarState) {
			CoreAbility.getAbility(player, AirShield.class).remove();
			return;
		}

		int angle = 0;
		int di = (int) (maxRadius * 2 / numberOfStreams);
		for (int i = -(int) maxRadius + di; i < (int) maxRadius; i += di) {
			angles.put(i, angle);
			angle += 90;
			if (angle == 360) {
				angle = 0;
			}
		}
		
		start();
	}

	public static boolean isWithinShield(Location loc) {
		for (AirShield ashield : CoreAbility.getAbilities(AirShield.class)) {
			if (!ashield.player.getWorld().equals(loc.getWorld())) {
				return false;
			} else if (ashield.player.getLocation().distanceSquared(loc) <= ashield.radius * ashield.radius) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void progress() {
		// AvatarState can use AirShield even when AirShield is not in the bound slot
		if (player.getEyeLocation().getBlock().isLiquid()) {
			remove();
			return;
		} else if (!bPlayer.isAvatarState() || !isToggledByAvatarState) {
			if (!player.isSneaking() || !bPlayer.canBend(this)) {
				remove();
				return;
			}
		} else if (!bPlayer.canBendIgnoreBinds(this)) {
			remove();
			return;
		}
		rotateShield();
	}

	private void rotateShield() {
		Location origin = player.getLocation();
		FireBlast.removeFireBlastsAroundPoint(origin, radius);
		Combustion.removeAroundPoint(origin, radius);
		BlazeArc.removeAroundPoint(origin, radius);
		AirBlast.removeAirBlastsAroundPoint(origin, radius);
		AirSuction.removeAirSuctionsAroundPoint(origin, radius);
		EarthBlast.removeAroundPoint(origin, radius);
		SandSpout.removeSpouts(origin, radius, player);
		WaterSpout.removeSpouts(origin, radius, player);
		WaterManipulation.removeAroundPoint(origin, radius);

		for (Entity entity : GeneralMethods.getEntitiesAroundPoint(origin, radius)) {
			if (GeneralMethods.isRegionProtectedFromBuild(player, "AirShield", entity.getLocation())) {
				continue;
			}
			if (origin.distanceSquared(entity.getLocation()) > 4) {
				double x, z, vx, vz, mag;
				double angle = 50;
				angle = Math.toRadians(angle);

				x = entity.getLocation().getX() - origin.getX();
				z = entity.getLocation().getZ() - origin.getZ();

				mag = Math.sqrt(x * x + z * z);

				vx = (x * Math.cos(angle) - z * Math.sin(angle)) / mag;
				vz = (x * Math.sin(angle) + z * Math.cos(angle)) / mag;

				Vector velocity = entity.getVelocity();
				if (bPlayer.isAvatarState()) {
					velocity.setX(AvatarState.getValue(vx));
					velocity.setZ(AvatarState.getValue(vz));
				} else {
					velocity.setX(vx);
					velocity.setZ(vz);
				}

				if (entity instanceof Player) {
					if (Commands.invincible.contains(((Player) entity).getName())) {
						continue;
					}
				}

				velocity.multiply(radius / maxRadius);
				GeneralMethods.setVelocity(entity, velocity);
				entity.setFallDistance(0);
			}
		}

		for (Block testblock : GeneralMethods.getBlocksAroundPoint(player.getLocation(), radius)) {
			if (testblock.getType() == Material.FIRE) {
				testblock.setType(Material.AIR);
				testblock.getWorld().playEffect(testblock.getLocation(), Effect.EXTINGUISH, 0);
			}
		}

		Set<Integer> keys = angles.keySet();
		for (int i : keys) {
			double x, y, z;
			double factor = radius / maxRadius;
			double angle = (double) angles.get(i);
			angle = Math.toRadians(angle);
			y = origin.getY() + factor * (double) i;
			double f = Math.sqrt(1 - factor * factor * ((double) i / radius) * ((double) i / radius));

			x = origin.getX() + radius * Math.cos(angle) * f;
			z = origin.getZ() + radius * Math.sin(angle) * f;

			Location effect = new Location(origin.getWorld(), x, y, z);
			if (!GeneralMethods.isRegionProtectedFromBuild(this, effect)) {
				playAirbendingParticles(effect, particleCount);
				if (random.nextInt(4) == 0) {
					playAirbendingSound(effect);
				}
			}

			angles.put(i, angles.get(i) + (int) (speed));
		}

		if (radius < maxRadius) {
			radius += .3;
		}
		if (radius > maxRadius) {
			radius = maxRadius;
		}
	}
	
	@Override
	public String getName() {
		return "AirShield";
	}

	@Override
	public Location getLocation() {
		return player != null ? player.getLocation() : null;
	}

	@Override
	public long getCooldown() {
		return 0;
	}
	
	@Override
	public boolean isSneakAbility() {
		return true;
	}

	@Override
	public boolean isHarmlessAbility() {
		return false;
	}

	public boolean isToggledByAvatarState() {
		return isToggledByAvatarState;
	}

	public void setToggledByAvatarState(boolean isToggledByAvatarState) {
		this.isToggledByAvatarState = isToggledByAvatarState;
	}

	public double getMaxRadius() {
		return maxRadius;
	}

	public void setMaxRadius(double maxRadius) {
		this.maxRadius = maxRadius;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public int getNumberOfStreams() {
		return numberOfStreams;
	}

	public void setNumberOfStreams(int numberOfStreams) {
		this.numberOfStreams = numberOfStreams;
	}

	public int getParticleCount() {
		return particleCount;
	}

	public void setParticleCount(int particleCount) {
		this.particleCount = particleCount;
	}

	public HashMap<Integer, Integer> getAngles() {
		return angles;
	}
}
