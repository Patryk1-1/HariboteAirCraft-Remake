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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraft.entity.EntityLivingBase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EntityHacAircraft extends Entity implements IEntityAdditionalSpawnData {
    private List<CapturedBlock> blocks = new ArrayList<>();
    private AircraftBlockAccess blockAccess;
    private final Map<UUID, Integer> recentlyDismounted = new HashMap<>();

    // Pilot movement inputs
    public float forwardInput = 0.0F;
    public float strafeInput = 0.0F;
    public float upInput = 0.0F;
    public float turnInput = 0.0F;

    public float rotorAngle = 0.0F;
    public float prevRotorAngle = 0.0F;
    public float initialYaw = 0.0F;

    // Bounds relative to (posX, posY, posZ)
    public double minBoundX = -0.5D, maxBoundX = 0.5D;
    public double minBoundY = 0.0D,  maxBoundY = 0.5D;
    public double minBoundZ = -0.5D, maxBoundZ = 0.5D;

    // Client-side smooth lerping fields to eliminate camera jitter/vibration
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYaw;
    private double lerpPitch;

    public EntityHacAircraft(World worldIn) {
        super(worldIn);
        this.setSize(1.5F, 0.5F);
        this.preventEntitySpawning = true;
        this.noClip = true;
        this.stepHeight = 1.0F;
    }

    public EntityHacAircraft(World worldIn, BlockPos origin, List<CapturedBlock> capturedBlocks) {
        this(worldIn, origin, capturedBlocks, 0.0F);
    }

    public EntityHacAircraft(World worldIn, BlockPos origin, List<CapturedBlock> capturedBlocks, float initialYaw) {
        this(worldIn);
        this.blocks = capturedBlocks;
        this.setPosition(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        this.initialYaw = initialYaw;
        this.rotationYaw = initialYaw;
        this.prevRotationYaw = initialYaw;
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

        // Keep physical entity size at 1.5x1.0 so dismounting never ejects players to the roof!
        this.setSize(1.5F, 1.0F);
        this.setEntityBoundingBox(new AxisAlignedBB(
                this.posX + minBoundX, this.posY + minBoundY, this.posZ + minBoundZ,
                this.posX + maxBoundX, this.posY + maxBoundY, this.posZ + maxBoundZ
        ));
        this.blockAccess = new AircraftBlockAccess(this);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(
                this.posX + minBoundX, this.posY + minBoundY, this.posZ + minBoundZ,
                this.posX + maxBoundX, this.posY + maxBoundY, this.posZ + maxBoundZ
        );
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

        if (this.world.isRemote) {
            if (this.lerpSteps > 0) {
                double d0 = this.posX + (this.lerpX - this.posX) / (double) this.lerpSteps;
                double d1 = this.posY + (this.lerpY - this.posY) / (double) this.lerpSteps;
                double d2 = this.posZ + (this.lerpZ - this.posZ) / (double) this.lerpSteps;
                double d3 = MathHelper.wrapDegrees(this.lerpYaw - (double) this.rotationYaw);
                this.rotationYaw = (float) ((double) this.rotationYaw + d3 / (double) this.lerpSteps);
                this.rotationPitch = (float) ((double) this.rotationPitch + (this.lerpPitch - (double) this.rotationPitch) / (double) this.lerpSteps);
                --this.lerpSteps;
                this.setPosition(d0, d1, d2);
                this.setRotation(this.rotationYaw, this.rotationPitch);
            }
        } else {
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

            // Prevent dismounted players from being launched into the roof/sky by vanilla dismountEntity
            if (!this.world.isRemote && !recentlyDismounted.isEmpty()) {
                double yawRad = Math.toRadians(this.rotationYaw);
                double rx = 0.85D * Math.sin(yawRad);
                double rz = -0.85D * Math.cos(yawRad);
                List<UUID> toRemove = new ArrayList<>();
                for (Map.Entry<UUID, Integer> entry : recentlyDismounted.entrySet()) {
                    EntityPlayer p = this.world.getPlayerEntityByUUID(entry.getKey());
                    if (p != null) {
                        p.setPositionAndUpdate(this.posX + rx, this.posY, this.posZ + rz);
                        p.motionX = 0; p.motionY = 0; p.motionZ = 0; p.fallDistance = 0;
                    }
                    if (entry.getValue() <= 1) {
                        toRemove.add(entry.getKey());
                    } else {
                        entry.setValue(entry.getValue() - 1);
                    }
                }
                for (UUID u : toRemove) recentlyDismounted.remove(u);
            }

            // Carry players and mobs standing on the deck (multiplayer walking & mob transport!)
            if (!this.blocks.isEmpty()) {
                AxisAlignedBB deckArea = this.getEntityBoundingBox().grow(1.5D, 2.5D, 1.5D);
                List<EntityLivingBase> nearby = this.world.getEntitiesWithinAABB(EntityLivingBase.class, deckArea);
                double deckDeltaYaw = this.rotationYaw - this.initialYaw;
                double yawRad = Math.toRadians(deckDeltaYaw);
                double cos = Math.cos(yawRad);
                double sin = Math.sin(yawRad);

                double dYaw = this.rotationYaw - this.prevRotationYaw;
                double dYawRad = Math.toRadians(-dYaw);
                double dCos = Math.cos(dYawRad);
                double dSin = Math.sin(dYawRad);

                for (EntityLivingBase entity : nearby) {
                    if (this.isPassenger(entity)) continue;

                    double highestDeckY = Double.NEGATIVE_INFINITY;
                    boolean onDeck = false;

                    for (CapturedBlock cb : blocks) {
                        double rx = cb.dx * cos - cb.dz * sin;
                        double rz = cb.dx * sin + cb.dz * cos;
                        double bx = this.posX + rx;
                        double by = this.posY + cb.dy;
                        double bz = this.posZ + rz;

                        if (entity.posX >= bx - 0.75D && entity.posX <= bx + 0.75D &&
                            entity.posZ >= bz - 0.75D && entity.posZ <= bz + 0.75D) {
                            double blockHeight = (cb.state != null && cb.state.getBlock() instanceof BlockPyxis) ? 0.5D : 1.0D;
                            double blockTop = by + blockHeight;
                            if (blockTop > highestDeckY) {
                                highestDeckY = blockTop;
                                onDeck = true;
                            }
                        }
                    }

                    if (onDeck && entity.posY >= highestDeckY - 0.5D && entity.posY <= highestDeckY + 2.5D) {
                        // Rotational displacement relative to ship center
                        double relX = entity.posX - this.posX;
                        double relZ = entity.posZ - this.posZ;
                        double rotRelX = relX * dCos - relZ * dSin;
                        double rotRelZ = relX * dSin + relZ * dCos;
                        double pushX = this.motionX + (rotRelX - relX);
                        double pushY = this.motionY;
                        double pushZ = this.motionZ + (rotRelZ - relZ);

                        entity.move(MoverType.SELF, pushX, pushY, pushZ);
                        entity.rotationYaw += dYaw;

                        if (entity.posY < highestDeckY) {
                            entity.setPosition(entity.posX, highestDeckY, entity.posZ);
                            entity.motionY = Math.max(0.0D, this.motionY);
                            entity.onGround = true;
                            entity.fallDistance = 0.0F;
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

    @SideOnly(Side.CLIENT)
    @Override
    public void setPositionAndRotationDirect(double x, double y, double z, float yaw, float pitch, int posRotationIncrements, boolean teleport) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYaw = (double) yaw;
        this.lerpPitch = (double) pitch;
        this.lerpSteps = posRotationIncrements;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            List<Entity> passengers = this.getPassengers();
            int index = passengers.indexOf(passenger);

            double offX = 0.0D;
            double offY = 0.0D;  // Standing/sitting on floor level
            double offZ = -0.6D; // Standing behind Pyxis helm station

            if (index == 0) {
                // Pilot stands at helm station right behind Pyxis on deck level
                double yawRad = Math.toRadians(this.rotationYaw);
                double rx = 0.75D * Math.sin(yawRad);
                double rz = -0.75D * Math.cos(yawRad);
                passenger.setPosition(this.posX + rx, this.posY, this.posZ + rz);

                double prevYawRad = Math.toRadians(this.prevRotationYaw);
                double prevRx = 0.75D * Math.sin(prevYawRad);
                double prevRz = -0.75D * Math.cos(prevYawRad);

                passenger.prevPosX = this.prevPosX + prevRx;
                passenger.prevPosY = this.prevPosY;
                passenger.prevPosZ = this.prevPosZ + prevRz;

                passenger.lastTickPosX = passenger.prevPosX;
                passenger.lastTickPosY = passenger.prevPosY;
                passenger.lastTickPosZ = passenger.prevPosZ;
                return;
            } else {
                // Other passengers: assign distinct separate blocks resting cleanly on top surface
                int blockIdx = index;
                if (blockIdx < blocks.size() && (blocks.get(blockIdx).dx != 0 || blocks.get(blockIdx).dz != 0)) {
                    CapturedBlock cb = blocks.get(blockIdx);
                    offX = cb.dx;
                    offY = cb.dy + ((cb.state.getBlock() instanceof BlockPyxis) ? 0.5D : 1.0D);
                    offZ = cb.dz;
                } else {
                    offX = (index % 2 == 1) ? 1.0D : -1.0D;
                    offY = 0.0D;
                    offZ = -1.0D * ((index + 1) / 2);
                }
            }

            double deltaYaw = this.rotationYaw - this.initialYaw;
            double yawRad = Math.toRadians(deltaYaw);
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            double rx = offX * cos - offZ * sin;
            double rz = offX * sin + offZ * cos;

            passenger.setPosition(this.posX + rx, this.posY + offY, this.posZ + rz);

            // Interpolate previous positions perfectly with aircraft to eliminate 100% of camera/flight jitter
            double prevDeltaYaw = this.prevRotationYaw - this.initialYaw;
            double prevYawRad = Math.toRadians(prevDeltaYaw);
            double prevCos = Math.cos(prevYawRad);
            double prevSin = Math.sin(prevYawRad);
            double prevRx = offX * prevCos - offZ * prevSin;
            double prevRz = offX * prevSin + offZ * prevCos;

            passenger.prevPosX = this.prevPosX + prevRx;
            passenger.prevPosY = this.prevPosY + offY;
            passenger.prevPosZ = this.prevPosZ + prevRz;

            passenger.lastTickPosX = passenger.prevPosX;
            passenger.lastTickPosY = passenger.prevPosY;
            passenger.lastTickPosZ = passenger.prevPosZ;
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!this.world.isRemote && passenger instanceof EntityPlayer) {
            recentlyDismounted.put(passenger.getUniqueID(), 3);
            double yawRad = Math.toRadians(this.rotationYaw);
            double rx = 0.85D * Math.sin(yawRad);
            double rz = -0.85D * Math.cos(yawRad);
            passenger.setPositionAndUpdate(this.posX + rx, this.posY, this.posZ + rz);
        }
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

        float netTurn = this.rotationYaw - this.initialYaw;
        int quadrant = Math.floorMod((int) Math.round(netTurn / 90.0D), 4);
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
        // PASS 1: Place all blocks using flag 2 (FLAG_NOTIFY_CLIENTS without neighbor update cascade)
        // Sort by Y ascending so floor and lower door halves are placed before upper door halves & attachments
        List<CapturedBlock> sortedBlocks = new ArrayList<>(blocks);
        sortedBlocks.sort(java.util.Comparator.comparingInt(b -> b.dy));

        List<BlockPos> placedPositions = new ArrayList<>();

        for (CapturedBlock cb : sortedBlocks) {
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
            this.world.setBlockState(targetPos, stateToPlace, 2);
            placedPositions.add(targetPos);
        }

        // PASS 2: Restore all TileEntities with rotation (e.g. skulls, banners, chests)
        for (CapturedBlock cb : blocks) {
            if (cb.tileNBT != null) {
                int rx = cb.dx;
                int rz = cb.dz;
                if (quadrant == 1) { rx = -cb.dz; rz = cb.dx; }
                else if (quadrant == 2) { rx = -cb.dx; rz = -cb.dz; }
                else if (quadrant == 3) { rx = cb.dz; rz = -cb.dx; }

                BlockPos targetPos = basePos.add(rx, cb.dy, rz);
                TileEntity te = TileEntity.create(this.world, cb.tileNBT);
                if (te != null) {
                    te.setPos(targetPos);
                    te.setWorld(this.world);
                    if (rotation != Rotation.NONE) {
                        try {
                            te.rotate(rotation);
                        } catch (Exception ignored) {}
                    }
                    this.world.setTileEntity(targetPos, te);
                }
            }
        }

        // PASS 3: Notify blocks so client rendering and lighting update cleanly
        for (BlockPos pos : placedPositions) {
            IBlockState st = this.world.getBlockState(pos);
            this.world.notifyBlockUpdate(pos, st, st, 3);
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
        this.initialYaw = compound.getFloat("initialYaw");
        NBTTagList list = compound.getTagList("blocks", 10);
        this.blocks.clear();
        for (int i = 0; i < list.tagCount(); i++) {
            this.blocks.add(CapturedBlock.readFromNBT(list.getCompoundTagAt(i)));
        }
        updateBounds();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound compound) {
        compound.setFloat("initialYaw", this.initialYaw);
        NBTTagList list = new NBTTagList();
        for (CapturedBlock cb : blocks) {
            list.appendTag(cb.writeToNBT());
        }
        compound.setTag("blocks", list);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        PacketBuffer packetBuffer = new PacketBuffer(buffer);
        packetBuffer.writeFloat(this.initialYaw);
        packetBuffer.writeFloat(this.rotationYaw);
        packetBuffer.writeInt(blocks.size());
        for (CapturedBlock cb : blocks) {
            cb.writeToBuf(packetBuffer);
        }
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        PacketBuffer packetBuffer = new PacketBuffer(additionalData);
        this.initialYaw = packetBuffer.readFloat();
        this.rotationYaw = packetBuffer.readFloat();
        this.prevRotationYaw = this.rotationYaw;
        int size = packetBuffer.readInt();
        this.blocks.clear();
        for (int i = 0; i < size; i++) {
            this.blocks.add(CapturedBlock.readFromBuf(packetBuffer));
        }
        updateBounds();
    }
}
