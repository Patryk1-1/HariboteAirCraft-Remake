package com.haribote.hac.entity;

import com.haribote.hac.block.BlockPyxis;
import com.haribote.hac.scanner.CapturedBlock;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.event.world.GetCollisionBoxesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

public class CollisionHandler {

    @SubscribeEvent
    public void onGetCollisionBoxes(GetCollisionBoxesEvent event) {
        Entity entity = event.getEntity();
        World world = event.getWorld();
        if (world == null) return;

        AxisAlignedBB queryBox = event.getAabb();
        if (queryBox == null) return;

        // Find any aircraft near the query box
        List<EntityHacAircraft> aircraftList = world.getEntitiesWithinAABB(EntityHacAircraft.class, queryBox.grow(2.5D));
        if (aircraftList.isEmpty()) return;

        for (EntityHacAircraft aircraft : aircraftList) {
            if (aircraft.isDead) continue;
            // The aircraft itself does not collide with its own blocks
            if (entity == aircraft) continue;
            // Sitting passengers do not collide with blocks while mounted in the seat
            if (entity != null && aircraft.isPassenger(entity)) continue;

            List<CapturedBlock> blocks = aircraft.getBlocks();
            if (blocks == null || blocks.isEmpty()) continue;

            double craftX = aircraft.posX;
            double craftY = aircraft.posY;
            double craftZ = aircraft.posZ;

            double yawRad = Math.toRadians(aircraft.rotationYaw);
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);

            for (CapturedBlock cb : blocks) {
                // Calculate rotated block center in world coordinates (matching RenderHacAircraft)
                double rx = cb.dx * cos - cb.dz * sin;
                double rz = cb.dx * sin + cb.dz * cos;

                double bx = craftX + rx;
                double by = craftY + cb.dy;
                double bz = craftZ + rz;

                double height = 1.0D;
                if (cb.state != null && cb.state.getBlock() instanceof BlockPyxis) {
                    height = 0.5D;
                }

                AxisAlignedBB blockBox = new AxisAlignedBB(
                        bx - 0.5D, by, bz - 0.5D,
                        bx + 0.5D, by + height, bz + 0.5D
                );

                if (blockBox.intersects(queryBox)) {
                    event.getCollisionBoxesList().add(blockBox);
                }
            }
        }
    }
}
