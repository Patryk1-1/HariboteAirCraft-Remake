package com.haribote.hac.network;

import com.haribote.hac.entity.EntityHacAircraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketPilotInput implements IMessage {
    public boolean forward;
    public boolean backward;
    public boolean left;      // Strafe Left (A)
    public boolean right;     // Strafe Right (D)
    public boolean up;        // Fly Up (Space)
    public boolean down;      // Fly Down (C / Ctrl)
    public boolean turnLeft;  // Turn Yaw Left (Q / Left Arrow)
    public boolean turnRight; // Turn Yaw Right (E / Right Arrow)
    public boolean stop;      // Hover (X)
    public boolean restore;   // Land (R)

    public PacketPilotInput() {}

    public PacketPilotInput(boolean forward, boolean backward, boolean left, boolean right,
                            boolean up, boolean down, boolean turnLeft, boolean turnRight,
                            boolean stop, boolean restore) {
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
        this.turnLeft = turnLeft;
        this.turnRight = turnRight;
        this.stop = stop;
        this.restore = restore;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        short flags = buf.readShort();
        this.forward   = (flags & (1 << 0)) != 0;
        this.backward  = (flags & (1 << 1)) != 0;
        this.left      = (flags & (1 << 2)) != 0;
        this.right     = (flags & (1 << 3)) != 0;
        this.up        = (flags & (1 << 4)) != 0;
        this.down      = (flags & (1 << 5)) != 0;
        this.turnLeft  = (flags & (1 << 6)) != 0;
        this.turnRight = (flags & (1 << 7)) != 0;
        this.stop      = (flags & (1 << 8)) != 0;
        this.restore   = (flags & (1 << 9)) != 0;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        short flags = 0;
        if (forward)   flags |= (1 << 0);
        if (backward)  flags |= (1 << 1);
        if (left)      flags |= (1 << 2);
        if (right)     flags |= (1 << 3);
        if (up)        flags |= (1 << 4);
        if (down)      flags |= (1 << 5);
        if (turnLeft)  flags |= (1 << 6);
        if (turnRight) flags |= (1 << 7);
        if (stop)      flags |= (1 << 8);
        if (restore)   flags |= (1 << 9);
        buf.writeShort(flags);
    }

    public static class Handler implements IMessageHandler<PacketPilotInput, IMessage> {
        @Override
        public IMessage onMessage(PacketPilotInput message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                Entity riding = player.getRidingEntity();
                if (riding instanceof EntityHacAircraft) {
                    EntityHacAircraft craft = (EntityHacAircraft) riding;
                    if (message.restore) {
                        craft.restoreToWorld();
                    } else if (message.stop) {
                        craft.motionX = 0.0D;
                        craft.motionY = 0.0D;
                        craft.motionZ = 0.0D;
                        craft.forwardInput = 0.0F;
                        craft.strafeInput = 0.0F;
                        craft.upInput = 0.0F;
                        craft.turnInput = 0.0F;
                    } else {
                        // W and S: Forward / Backward
                        craft.forwardInput = (message.forward ? 1.0F : 0.0F) + (message.backward ? -1.0F : 0.0F);

                        // A and D: Strafe Left / Right (Na boki!)
                        craft.strafeInput = (message.right ? 1.0F : 0.0F) + (message.left ? -1.0F : 0.0F);

                        // Space and C/Ctrl: Up / Down (Góra i dół!)
                        craft.upInput = (message.up ? 1.0F : 0.0F) + (message.down ? -1.0F : 0.0F);

                        // Q/E or Left/Right Arrow: Turn
                        craft.turnInput = (message.turnRight ? 1.0F : 0.0F) + (message.turnLeft ? -1.0F : 0.0F);
                    }
                }
            });
            return null;
        }
    }
}
