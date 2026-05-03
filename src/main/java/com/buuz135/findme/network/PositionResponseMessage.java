package com.buuz135.findme.network;


import com.buuz135.findme.FindMeMod;
import com.buuz135.findme.client.ClientTickHandler;
import com.buuz135.findme.client.ParticlePosition;
import com.buuz135.findme.tracking.TrackingList;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PositionResponseMessage implements CustomPacketPayload {

    public static CustomPacketPayload.Type<PositionResponseMessage> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FindMeMod.MOD_ID, "position_response"));
    public static StreamCodec<? super RegistryFriendlyByteBuf, PositionResponseMessage> CODEC = new StreamCodec<>() {
        @Override
        public PositionResponseMessage decode(RegistryFriendlyByteBuf object) {
            List<BlockPos> positions = new ArrayList<>();
            int amount = object.readInt();
            while (amount > 0) {
                positions.add(object.readBlockPos());
                --amount;
            }
            return new PositionResponseMessage(positions);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, PositionResponseMessage positionRequestMessage) {
            registryFriendlyByteBuf.writeInt(positionRequestMessage.positions.size());
            for (BlockPos position : positionRequestMessage.positions) {
                registryFriendlyByteBuf.writeBlockPos(position);
            }
        }
    };

    private List<BlockPos> positions;

    public PositionResponseMessage(List<BlockPos> positions) {
        this.positions = positions;
    }

    public PositionResponseMessage() {
    }


    public void handle(ClientPlayNetworking.Context context) {
        Minecraft.getInstance().execute(() -> {
            FindMeMod.LOGGER.info("[FindMe Debug] PositionResponse received: {} positions, level={}, player={}",
                positions.size(),
                Minecraft.getInstance().level != null,
                Minecraft.getInstance().player != null);
            if (positions.size() > 0) {
                Minecraft.getInstance().player.closeContainer();
                Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
                if (FindMeMod.CONFIG.CLIENT.CONTAINER_TRACKING) {
                    TrackingList.beginTracking();
                    ClientTickHandler.addRunnable(TrackingList::clear, FindMeMod.CONFIG.CLIENT.CONTAINER_TRACK_TIME);
                }
                FindMeMod.LOGGER.info("[FindMe Debug] Spawning particles for {} positions...", positions.size());
                for (BlockPos position : positions) {
                    for (int i = 0; i < 2; ++i)
                        addParticle(position);
                }
                FindMeMod.LOGGER.info("[FindMe Debug] Done spawning particles. Engine exists: {}",
                    Minecraft.getInstance().particleEngine != null);

                if (FindMeMod.CONFIG.CLIENT.SNAP_TO_CONTAINER) {
                    // Find the nearest container to the player
                    BlockPos nearest = null;
                    double nearestDistance = Double.MAX_VALUE;
                    BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
                    for (BlockPos pos : positions) {
                        double dist = playerPos.distSqr(pos);
                        if (dist < nearestDistance) {
                            nearestDistance = dist;
                            nearest = pos;
                        }
                    }
                    if (nearest != null) {
                        Vec3 eyePos = Minecraft.getInstance().player.getEyePosition();
                        Vec3 targetPos = Vec3.atCenterOf(nearest);
                        double dx = targetPos.x - eyePos.x;
                        double dy = targetPos.y - eyePos.y;
                        double dz = targetPos.z - eyePos.z;

                        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

                        Minecraft.getInstance().player.setYRot(yaw);
                        Minecraft.getInstance().player.setXRot(pitch);
                    }
                }
            } else {
                FindMeMod.LOGGER.info("[FindMe Debug] No positions to highlight (empty result)");
            }
        });
        //context.get().setPacketHandled(true);
    }

    @Environment(EnvType.CLIENT)
    public void addParticle(BlockPos position) {
        Minecraft.getInstance().particleEngine.add(
            new ParticlePosition(
                Minecraft.getInstance().level, 
                position.getX() + 0.75 - Minecraft.getInstance().player.level().getRandom().nextDouble() / 2D,
                position.getY() + 0.75 - Minecraft.getInstance().player.level().getRandom().nextDouble() / 2D,
                position.getZ() + 0.75 - Minecraft.getInstance().player.level().getRandom().nextDouble() / 2D,
                0, 0, 0));
        //Minecraft.getInstance().particleEngine.add(new AshParticle((ClientLevel) Minecraft.getInstance().player.level(), position.getX() + 0.75 - Minecraft.getInstance().player.level().random.nextDouble() / 2D, 1 + position.getY() + 0.75 - Minecraft.getInstance().player.level().random.nextDouble() / 2D, position.getZ() + 0.75 - Minecraft.getInstance().player.level().random.nextDouble() / 2D, 0, 0, 0));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
