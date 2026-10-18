package com.haribote.hac.client;

import com.haribote.hac.entity.EntityHacAircraft;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientMountHelper {
    private static int targetEntityId = -1;
    private static int timeoutTicks = 0;

    public static void queueMount(int entityId) {
        targetEntityId = entityId;
        timeoutTicks = 40; // 2 seconds
        tryMount();
    }

    public static void tryMount() {
        if (targetEntityId == -1) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;

        Entity target = mc.world.getEntityByID(targetEntityId);
        if (target instanceof EntityHacAircraft) {
            mc.player.startRiding(target, true);
            targetEntityId = -1;
            timeoutTicks = 0;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (targetEntityId != -1) {
            tryMount();
            timeoutTicks--;
            if (timeoutTicks <= 0) {
                targetEntityId = -1;
            }
        }
    }
}
