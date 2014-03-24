package at.pavlov.cannons.scheduler;

import at.pavlov.cannons.Cannons;
import at.pavlov.cannons.projectile.FlyingProjectile;
import at.pavlov.cannons.projectile.Projectile;
import at.pavlov.cannons.projectile.ProjectileProperties;
import at.pavlov.cannons.utils.DelayedTask;
import at.pavlov.cannons.utils.FireTaskWrapper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.Iterator;


public class ProjectileObserver {
    private final Cannons plugin;


    /**
     * Constructor
     * @param plugin - Cannons instance
     */
    public ProjectileObserver(Cannons plugin)
    {
        this.plugin = plugin;
    }

    /**
     * starts the scheduler of the teleporter
     */
    public void setupScheduler()
    {
        //changing angles for aiming mode
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
                //get projectiles
                Iterator<FlyingProjectile> iter = plugin.getProjectileManager().getFlyingProjectiles().iterator();
                while(iter.hasNext())
                {
                    FlyingProjectile fproj = iter.next();
                    //remove an not valid projectile
                    if (fproj.getProjectileEntity() == null)
                    {
                        iter.remove();
                        continue;
                    }

                    //update the cannonball
                    checkWaterImpact(fproj);
                    updateTeleporter(fproj);

                }

            }
        }, 1L, 1L);
    }

    /**
     * if cannonball enters water it will spawn a splash effect
     * @param fproj the projectile to check
     */
    private void checkWaterImpact(FlyingProjectile fproj)
    {

        //the projectile has passed the water surface, make a splash
        if (fproj.updateWaterSurfaceCheck())
        {
            //go up until there is air and place the same liquid
            Material liquid = fproj.getProjectileEntity().getLocation().getBlock().getType();
            Location startLoc = fproj.getProjectileEntity().getLocation().clone();
            Vector vel = fproj.getProjectileEntity().getVelocity().clone();

            for (int i = 0; i<5; i++)
            {
                Block block = startLoc.subtract(vel.clone().multiply(i)).getBlock();
                if (block != null && block.getType().equals(Material.AIR))
                {
                    //found a free block - make the splash
                    block.setType(liquid);
                    //revert it after some time
                    Location loc = block.getLocation();
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new DelayedTask(loc)
                    {
                        public void run(Object object)
                        {
                            Location loc = (Location) object;
                            Block block = loc.getBlock();
                            if (block.isLiquid())
                                block.setType(Material.AIR);
                        }
                    }, 60);
                    //we are done now
                    break;
                }
            }
        }

    }

    /**
     * teleports the player to new position
     * @param fproj the FlyingProjectile to check
     */
    private void updateTeleporter(FlyingProjectile fproj)
    {
        //if projectile has teleporter - update player position
        Projectile projectile = fproj.getProjectile();
        if (projectile.hasProperty(ProjectileProperties.TELEPORT) || projectile.hasProperty(ProjectileProperties.OBSERVER))
        {
            LivingEntity shooter = fproj.getShooter();
            if(shooter == null)
                return;

            org.bukkit.entity.Projectile ball = fproj.getProjectileEntity();
            //set some distance to the snowball to prevent a collision
            Location optiLoc = ball.getLocation().clone().subtract(ball.getVelocity().normalize().multiply(20.0));

            Vector distToOptimum = optiLoc.toVector().subtract(shooter.getLocation().toVector());
            Vector playerVel = ball.getVelocity().add(distToOptimum.multiply(0.1));
            //cap for maximum speed
            if (playerVel.getX() > 5.0)
                playerVel.setX(5.0);
            if (playerVel.getY() > 5.0)
                playerVel.setY(5.0);
            if (playerVel.getZ() > 5.0)
                playerVel.setZ(5.0);
            shooter.setVelocity(playerVel);
            shooter.setFallDistance(0.0f);


            //teleport if the player is behind
            if (distToOptimum.length() > 30)
            {
                optiLoc.setYaw(shooter.getLocation().getYaw());
                optiLoc.setPitch(shooter.getLocation().getPitch());
                shooter.teleport(optiLoc);
            }
        }
    }

}