package com.buuz135.findme.network;

import com.buuz135.findme.FindMeMod;
import com.buuz135.findme.tracking.HighlightCache;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PositionResponseMessage implements CustomPacketPayload {

    public static CustomPacketPayload.Type<PositionResponseMessage> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FindMeMod.MOD_ID, "position_response"));
    public static StreamCodec<? super RegistryFriendlyByteBuf, PositionResponseMessage> CODEC = new StreamCodec<>() {
        @Override
        public PositionResponseMessage decode(RegistryFriendlyByteBuf buf) {
            List<BlockPos> blockPositions = new ArrayList<>();
            int blockCount = buf.readInt();
            for (int i = 0; i < blockCount; i++) {
                blockPositions.add(buf.readBlockPos());
            }
            List<Integer> itemEntityIds = new ArrayList<>();
            int itemCount = buf.readInt();
            for (int i = 0; i < itemCount; i++) {
                itemEntityIds.add(buf.readInt());
            }
            List<Integer> entityIds = new ArrayList<>();
            int entityCount = buf.readInt();
            for (int i = 0; i < entityCount; i++) {
                entityIds.add(buf.readInt());
            }
            return new PositionResponseMessage(blockPositions, itemEntityIds, entityIds);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, PositionResponseMessage msg) {
            buf.writeInt(msg.blockPositions.size());
            for (BlockPos pos : msg.blockPositions) {
                buf.writeBlockPos(pos);
            }
            buf.writeInt(msg.itemEntityIds.size());
            for (int id : msg.itemEntityIds) {
                buf.writeInt(id);
            }
            buf.writeInt(msg.entityIds.size());
            for (int id : msg.entityIds) {
                buf.writeInt(id);
            }
        }
    };

    private List<BlockPos> blockPositions;
    private List<Integer> itemEntityIds;
    private List<Integer> entityIds;

    public PositionResponseMessage(List<BlockPos> blockPositions, List<Integer> itemEntityIds, List<Integer> entityIds) {
        this.blockPositions = blockPositions;
        this.itemEntityIds = itemEntityIds;
        this.entityIds = entityIds;
    }

    public PositionResponseMessage() {
        this.blockPositions = new ArrayList<>();
        this.itemEntityIds = new ArrayList<>();
        this.entityIds = new ArrayList<>();
    }

    public void handle(ClientPlayNetworking.Context context) {
        Minecraft.getInstance().execute(() -> {
            int total = blockPositions.size() + itemEntityIds.size() + entityIds.size();
            if (total > 0) {
                Minecraft.getInstance().player.closeContainer();
                Minecraft.getInstance().player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

                int duration = FindMeMod.CONFIG.CLIENT.LASER_DURATION;
                for (BlockPos pos : blockPositions) {
                    HighlightCache.addBlockHighlight(pos, duration);
                }
                for (int id : itemEntityIds) {
                    HighlightCache.addItemEntityHighlight(id, duration);
                }
                for (int id : entityIds) {
                    HighlightCache.addEntityHighlight(id, duration);
                }

                if (FindMeMod.CONFIG.CLIENT.SNAP_TO_CONTAINER && total > 0) {
                    var level = Minecraft.getInstance().level;
                    Vec3 eyePos = Minecraft.getInstance().player.getEyePosition();
                    double nearestDistance = Double.MAX_VALUE;
                    Vec3 targetPos = null;

                    for (BlockPos pos : blockPositions) {
                        Vec3 center = Vec3.atCenterOf(pos);
                        double dist = center.distanceToSqr(eyePos);
                        if (dist < nearestDistance) {
                            nearestDistance = dist;
                            targetPos = center;
                        }
                    }

                    for (int id : itemEntityIds) {
                        Entity entity = level.getEntity(id);
                        if (entity != null) {
                            Vec3 pos = entity.position();
                            double dist = pos.distanceToSqr(eyePos);
                            if (dist < nearestDistance) {
                                nearestDistance = dist;
                                targetPos = pos;
                            }
                        }
                    }

                    for (int id : entityIds) {
                        Entity entity = level.getEntity(id);
                        if (entity != null) {
                            Vec3 pos = entity.position();
                            double dist = pos.distanceToSqr(eyePos);
                            if (dist < nearestDistance) {
                                nearestDistance = dist;
                                targetPos = pos;
                            }
                        }
                    }

                    if (targetPos != null) {
                        double dx = targetPos.x - eyePos.x;
                        double dy = targetPos.y - eyePos.y;
                        double dz = targetPos.z - eyePos.z;

                        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));

                        Minecraft.getInstance().player.setYRot(yaw);
                        Minecraft.getInstance().player.setXRot(pitch);
                    }
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
