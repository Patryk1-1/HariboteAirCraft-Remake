package com.haribote.hac.client;

import com.haribote.hac.entity.EntityHacAircraft;
import com.haribote.hac.network.HacPacketHandler;
import com.haribote.hac.network.PacketPilotInput;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class ClientInputHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.player == null) return;
        EntityPlayer player = mc.player;

        // Player is piloting/riding the aircraft
        if (player.getRidingEntity() instanceof EntityHacAircraft) {
            if (mc.currentScreen != null) return;

            // W and S: Forward / Backward (Przód / Tył)
            boolean forward   = Keyboard.isKeyDown(Keyboard.KEY_W) || mc.gameSettings.keyBindForward.isKeyDown();
            boolean backward  = Keyboard.isKeyDown(Keyboard.KEY_S) || mc.gameSettings.keyBindBack.isKeyDown();

            // A and D: Strafe Left / Strafe Right (Na boki!)
            boolean left      = Keyboard.isKeyDown(Keyboard.KEY_A) || mc.gameSettings.keyBindLeft.isKeyDown();
            boolean right     = Keyboard.isKeyDown(Keyboard.KEY_D) || mc.gameSettings.keyBindRight.isKeyDown();

            // Space: Fly Up (Góra)
            boolean up        = Keyboard.isKeyDown(Keyboard.KEY_SPACE) || mc.gameSettings.keyBindJump.isKeyDown();

            // C / LControl / RControl / V: Fly Down (Dół)
            boolean down      = Keyboard.isKeyDown(Keyboard.KEY_C)
                    || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
                    || Keyboard.isKeyDown(Keyboard.KEY_V)
                    || (KeyBindings.keyDown != null && KeyBindings.keyDown.isKeyDown());

            // Manual turn with Q and E or Arrow keys
            boolean turnLeft  = Keyboard.isKeyDown(Keyboard.KEY_Q) || Keyboard.isKeyDown(Keyboard.KEY_LEFT);
            boolean turnRight = Keyboard.isKeyDown(Keyboard.KEY_E) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT);

            // Stop / Hover: X
            boolean stop      = Keyboard.isKeyDown(Keyboard.KEY_X) || (KeyBindings.keyStop != null && KeyBindings.keyStop.isPressed());

            // Land / Restore: R
            boolean restore   = (KeyBindings.keyRestore != null && KeyBindings.keyRestore.isPressed()) || Keyboard.isKeyDown(Keyboard.KEY_R);

            HacPacketHandler.INSTANCE.sendToServer(new PacketPilotInput(
                    forward, backward, left, right,
                    up, down, turnLeft, turnRight,
                    stop, restore
            ));
        }
    }
}
