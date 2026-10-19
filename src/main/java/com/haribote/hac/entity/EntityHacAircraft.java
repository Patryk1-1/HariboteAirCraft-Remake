package com.haribote.hac.entity;

import com.haribote.hac.block.BlockPyxis;
import net.minecraft.block.state.IBlockState;
import com.haribote.hac.network.HacPacketHandler;
import com.haribote.hac.network.PacketMountAircraft;
import com.haribote.hac.scanner.CapturedBlock;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Rotation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.ArrayList;
import java.util.List;

public class EntityHacAircraft extends Entity implements IEntityAdditionalSpawnData {
    private List<CapturedBlock> blocks = new ArrayList<>();
    private AircraftBlockAccess blockAccess;

    // Pilot movement inputs
    public float forwardInput = 0.0F;
    public float strafeInput = 0.0F;
    public float upInput = 0.0F;
    public float turnInput = 0.0F;

    public float rotorAngle = 0.0F;
    public float prevRotorAngle = 0.0F;

    // Bounds relative to (posX, posY, posZ)
    public double minBoundX = -0.5D, maxBoundX = 0.5D;
    public double minBoundY = 0.0D,  maxBoundY = 0.5D;
    public double minBoundZ = -0.5D, maxBoundZ = 0.5D;

    public EntityHacAircraft(World worldIn) {
        super(worldIn);
        this.setSize(1.5F, 0.5F);
        this.preventEntitySpawning = true;
        this.noClip = true;
        this.stepHeight = 1.0F;
    }

    public EntityHacAircraft(World worldIn, BlockPos origin, List<CapturedBlock> capturedBlocks) {
        this(worldIn);
        this.blocks = capturedBlocks;
        this.setPosition(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        updateBounds();
    }

    public List<CapturedBlock> getBlocks() {
        return this.blocks;
    }

    public void updateBounds() {
        if (this.blocks == null || this.blocks.isEmpty()) {
            minBoundX = -0.5D; maxBoundX = 0.5D;
            minBoundY = 0.0D;  maxBoundY = 0.5D;
            minBoundZ = -0.5D; maxBoundZ = 0.5D;
            return;
        }

        double maxXZDist = 1.0D;
        double minY = 0, maxY = 0;

        for (CapturedBlock cb : blocks) {
            double dist = Math.sqrt(cb.dx * cb.dx + cb.dz * cb.dz);
            if (dist > maxXZDist) maxXZDist = dist;
            if (cb.dy < minY) minY = cb.dy;
            if (cb.dy > maxY) maxY = cb.dy;
        }

        this.minBoundX = -maxXZDist - 0.5D;
        this.maxBoundX =  maxXZDist + 0.5D;
        this.minBoundY = minY;
        this.maxBoundY = maxY + 1.0D;
        this.minBoundZ = -maxXZDist - 0.5D;
        this.maxBoundZ =  maxXZDist + 0.5D;

        float width = (float) ((maxXZDist + 0.5D) * 2.0D);
        float height = (float) (maxY - minY + 1.0D);
        this.setSize(Math.min(width, 64.0F), Math.min(height, 32.0F));
        this.setEntityBoundingBox(new AxisAlignedBB(
                this.posX + minBoundX, this.posY + minBoundY, this.posZ + minBoundZ,
                this.posX + maxBoundX, this.posY + maxBoundY, this.posZ + maxBoundZ
        ));
        this.blockAccess = new AircraftBlockAccess(this);
    }

    public AircraftBlockAccess getBlockAccess() {
        if (this.blockAccess == null) {
            this.blockAccess = new AircraftBlockAccess(this);
        }
        return this.blockAccess;
    }

    @Override
    protected void entityInit() {
    }

    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        // Return null so moving backwards or walking on deck is never blocked by phantom hitboxes!
        return null;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entityIn) {
        // Return null so players and passengers can move with 100% freedom!
        return null;
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return new AxisAlignedBB(
                this.posX + minBoundX, this.posY + minBoundY, this.posZ + minBoundZ,
                this.posX + maxBoundX, this.posY + maxBoundY, this.posZ + maxBoundZ
        );
    }

    @Override
    public void applyEntityCollision(Entity entityIn) {
        // Do NOT push passengers! Completely prevents jitter/vibrations!
        if (this.isPassenger(entityIn) || this.getPassengers().contains(entityIn)) {
            return;
        }
        super.applyEntityCollision(entityIn);
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return true; // Allow multiple passengers in multiplayer!
    }

    @Override
    public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
        if (!this.world.isRemote) {
            if (!this.isPassenger(player)) {
                player.startRiding(this, true);
                if (player instanceof EntityPlayerMP) {
                    HacPacketHandler.INSTANCE.sendTo(new PacketMountAircraft(this.getEntityId()), (EntityPlayerMP) player);
                }
                return true;
            }
        }
        return true;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.prevRotationYaw = this.rotationYaw;

        if (!this.world.isRemote) {
            Entity controllingPassenger = this.getControllingPassenger();
            if (controllingPassenger instanceof EntityPlayer) {
                EntityPlayer pilot = (EntityPlayer) controllingPassenger;

                // Smooth yaw banking towards pilot look angle
                float targetYaw = pilot.rotationYaw;
                float yawDiff = MathHelper.wrapDegrees(targetYaw - this.rotationYaw);
                if (turnInput != 0.0F) {
                    this.rotationYaw += turnInput * 3.5F;
                } else if (Math.abs(yawDiff) > 1.0F) {
                    this.rotationYaw += yawDiff * 0.20F; // Smooth banking & turning
                }

                double yawRad = Math.toRadians(this.rotationYaw);
                double fwdX = -Math.sin(yawRad);
                double fwdZ = Math.cos(yawRad);
                double strX = -Math.cos(yawRad);
                double strZ = -Math.sin(yawRad);

                double maxSpeed = 0.50D;
                double targetMotionX = (fwdX * forwardInput + strX * strafeInput) * maxSpeed;
                double targetMotionZ = (fwdZ * forwardInput + strZ * strafeInput) * maxSpeed;
                double targetMotionY = upInput * 0.40D;

                // Smooth acceleration and deceleration for luxurious, silky flight physics
                double accelH = (forwardInput != 0.0F || strafeInput != 0.0F) ? 0.20D : 0.12D;
                double accelV = (upInput != 0.0F) ? 0.25D : 0.15D;

                this.motionX += (targetMotionX - this.motionX) * accelH;
                this.motionZ += (targetMotionZ - this.motionZ) * accelH;
                this.motionY += (targetMotionY - this.motionY) * accelV;

                if (forwardInput == 0.0F && strafeInput == 0.0F) {
                    if (Math.abs(this.motionX) < 0.005D) this.motionX = 0.0D;
                    if (Math.abs(this.motionZ) < 0.005D) this.motionZ = 0.0D;
                }
                if (upInput == 0.0F && Math.abs(this.motionY) < 0.005D) {
                    this.motionY = 0.0D;
                }
            } else {
                this.motionX *= 0.8D;
                this.motionY *= 0.8D;
                this.motionZ *= 0.8D;
            }

            // Move aircraft on server
            this.setPosition(this.posX + this.motionX, this.posY + this.motionY, this.posZ + this.motionZ);
            this.setEntityBoundingBox(new AxisAlignedBB(
                    this.posX + minBoundX, this.posY + minBoundY, this.posZ + minBoundZ,
                    this.posX + maxBoundX, this.posY + maxBoundY, this.posZ + maxBoundZ
            ));

            // Carry players standing on the deck (multiplayer walking on deck support!)
            if (!this.blocks.isEmpty()) {
                AxisAlignedBB deckArea = this.getEntityBoundingBox().grow(1.5D, 2.5D, 1.5D);
                List<EntityPlayer> nearby = this.world.getEntitiesWithinAABB(EntityPlayer.class, deckArea);
                double yawRad = Math.toRadians(this.rotationYaw);
                double cos = Math.cos(yawRad);
                double sin = Math.sin(yawRad);

                for (EntityPlayer p : nearby) {
                    if (this.isPassenger(p)) continue;

                    double highestDeckY = Double.NEGATIVE_INFINITY;
                    boolean onDeck = false;

                    for (CapturedBlock cb : blocks) {
                        double rx = cb.dx * cos - cb.dz * sin;
                        double rz = cb.dx * sin + cb.dz * cos;
                        double bx = this.posX + rx;
                        double by = this.posY + cb.dy;
                        double bz = this.posZ + rz;

                        if (p.posX >= bx - 0.75D && p.posX <= bx + 0.75D &&
                            p.posZ >= bz - 0.75D && p.posZ <= bz + 0.75D) {
                            double blockHeight = (cb.state != null && cb.state.getBlock() instanceof BlockPyxis) ? 0.5D : 1.0D;
                            double blockTop = by + blockHeight;
                            if (blockTop > highestDeckY) {
                                highestDeckY = blockTop;
                                onDeck = true;
                            }
                        }
                    }

                    if (onDeck && p.posY >= highestDeckY - 0.5D && p.posY <= highestDeckY + 2.5D) {
                        p.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

                        if (p.posY < highestDeckY) {
                            p.setPosition(p.posX, highestDeckY, p.posZ);
                            p.motionY = Math.max(0.0D, this.motionY);
                            p.onGround = true;
                            p.fallDistance = 0.0F;
                        }
                    }
                }
            }
        }
    }

    @Override
    public double getMountedYOffset() {
        return 0.5D;
    }

    @Override
    public boolean canPassengerSteer() {
        // Return false so Minecraft NetHandlerPlayServer never spams 'moved wrongly' or resets vehicle position!
        return false;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            List<Entity> passengers = this.getPassengers();
            int index = passengers.indexOf(passenger);

            double offX = 0.0D;
            double offY = 0.5D; // Pyxis surface
            double offZ = 0.0D;

            if (index == 0) {
                // Pilot sits directly on Pyxis
                offX = 0.0D;
                offY = 0.5D;
                offZ = 0.0D;
            } else {
                // Passengers: assign distinct separate blocks resting cleanly on top surface
                int blockIdx = index;
                if (blockIdx < blocks.size() && (blocks.get(blockIdx).dx != 0 || blocks.get(blockIdx).dz != 0)) {
                    CapturedBlock cb = blocks.get(blockIdx);
                    offX = cb.dx;
                    offY = cb.dy + ((cb.state.getBlock() instanceof BlockPyxis) ? 0.5D : 1.0D);
                    offZ = cb.dz;
                } else {
                    offX = (index % 2 == 1) ? 1.0D : -1.0D;
                    offY = 1.0D;
                    offZ = -1.0D * ((index + 1) / 2);
                }
            }

            double yawRad = Math.toRadians(this.rotationYaw);
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            double rx = offX * cos - offZ * sin;
            double rz = offX * sin + offZ * cos;

            passenger.setPosition(this.posX + rx, this.posY + offY, this.posZ + rz);
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return true;
    }

    @Override
    public Entity getControllingPassenger() {
        List<Entity> list = this.getPassengers();
        return list.isEmpty() ? null : list.get(0);
    }

    public void restoreToWorld() {
        if (this.world.isRemote || this.isDead) return;

        int targetX = (int) Math.round(this.posX - 0.5D);
        int targetY = (int) Math.round(this.posY);
        int targetZ = (int) Math.round(this.posZ - 0.5D);
        BlockPos basePos = new BlockPos(targetX, targetY, targetZ);

        int quadrant = Math.floorMod((int) Math.round(this.rotationYaw / 90.0D), 4);
        Rotation rotation = Rotation.NONE;
        if (quadrant == 1) rotation = Rotation.CLOCKWISE_90;
        else if (quadrant == 2) rotation = Rotation.CLOCKWISE_180;
        else if (quadrant == 3) rotation = Rotation.COUNTERCLOCKWISE_90;

        // Check if destination positions are free or replaceable
        for (CapturedBlock cb : blocks) {
            int rx = cb.dx;
            int rz = cb.dz;
            if (quadrant == 1) { rx = -cb.dz; rz = cb.dx; }
            else if (quadrant == 2) { rx = -cb.dx; rz = -cb.dz; }
            else if (quadrant == 3) { rx = cb.dz; rz = -cb.dx; }

            BlockPos targetPos = basePos.add(rx, cb.dy, rz);
            if (!this.world.isAirBlock(targetPos) && !this.world.getBlockState(targetPos).getBlock().isReplaceable(this.world, targetPos)) {
                Entity controlling = getControllingPassenger();
                if (controlling instanceof EntityPlayer) {
                    ((EntityPlayer) controlling).sendMessage(new TextComponentTranslation("message.hac.blocked"));
                }
                return;
            }
        }

        // Place blocks back into the world
        for (CapturedBlock cb : blocks) {
            int rx = cb.dx;
            int rz = cb.dz;
            if (quadrant == 1) { rx = -cb.dz; rz = cb.dx; }
            else if (quadrant == 2) { rx = -cb.dx; rz = -cb.dz; }
            else if (quadrant == 3) { rx = cb.dz; rz = -cb.dx; }

            BlockPos targetPos = basePos.add(rx, cb.dy, rz);
            IBlockState stateToPlace = cb.state;
            if (rotation != Rotation.NONE && stateToPlace != null) {
                try {
                    stateToPlace = stateToPlace.withRotation(rotation);
                } catch (Exception ignored) {}
            }
            this.world.setBlockState(targetPos, stateToPlace, 3);
            if (cb.tileNBT != null) {
                TileEntity te = TileEntity.create(this.world, cb.tileNBT);
                if (te != null) {
                    te.setPos(targetPos);
                    this.world.setTileEntity(targetPos, te);
                }
            }
        }

        List<Entity> passengers = new ArrayList<>(this.getPassengers());
        this.removePassengers();

        for (Entity p : passengers) {
            if (p instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) p;
                player.setPositionAndUpdate(basePos.getX() + 0.5D, basePos.getY() + 1.5D, basePos.getZ() + 0.5D);
                player.sendMessage(new TextComponentTranslation("message.hac.restored", blocks.size()));
            }
        }

        this.setDead();
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        NBTTagList list = compound.getTagList("blocks", 10);
        this.blocks.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            this.blocks.add(CapturedBlock.readFromNBT(list.getCompoundTagAt(i)));
        }
        updateBounds();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        NBTTagList list = new NBTTagList();
        for (CapturedBlock cb : blocks) {
            list.appendTag(cb.writeToNBT());
        }
        compound.setTag("blocks", list);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        PacketBuffer packetBuffer = new PacketBuffer(buffer);
        packetBuffer.writeInt(blocks.size());
        for (CapturedBlock cb : blocks) {
            cb.writeToBuf(packetBuffer);
        }
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        PacketBuffer packetBuffer = new PacketBuffer(additionalData);
        int size = packetBuffer.readInt();
        this.blocks.clear();
        for (int i = 0; i < size; i++) {
            this.blocks.add(CapturedBlock.readFromBuf(packetBuffer));
        }
        updateBounds();
    }
}
