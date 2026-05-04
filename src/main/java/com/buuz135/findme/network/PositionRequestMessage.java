package com.buuz135.findme.network;

import com.buuz135.findme.FindMeMod;
import com.buuz135.findme.tracking.TrackingList;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;


import java.util.ArrayList;
import java.util.List;

public class PositionRequestMessage implements CustomPacketPayload {

    public static CustomPacketPayload.Type<PositionRequestMessage> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(FindMeMod.MOD_ID, "position_request"));
    public static StreamCodec<? super RegistryFriendlyByteBuf, PositionRequestMessage> CODEC = new StreamCodec<>() {
        @Override
        public PositionRequestMessage decode(RegistryFriendlyByteBuf object) {
            return new PositionRequestMessage(ItemStack.OPTIONAL_STREAM_CODEC.decode(object));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, PositionRequestMessage positionRequestMessage) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(registryFriendlyByteBuf, positionRequestMessage.stack);
        }
    };

    private ItemStack stack;

    public PositionRequestMessage(ItemStack stack) {
        this.stack = stack;
        TrackingList.trackItem(stack);
    }

    public PositionRequestMessage() {
    }

    public static List<BlockPos> getBlockPosInAABB(AABB axisAlignedBB) {
        List<BlockPos> blocks = new ArrayList<BlockPos>();
        for (double y = axisAlignedBB.minY; y < axisAlignedBB.maxY; ++y) {
            for (double x = axisAlignedBB.minX; x < axisAlignedBB.maxX; ++x) {
                for (double z = axisAlignedBB.minZ; z < axisAlignedBB.maxZ; ++z) {
                    blocks.add(new BlockPos((int) x, (int) y, (int) z));
                }
            }
        }
        return blocks;
    }

    public static boolean compareItems(ItemStack first, ItemStack second) {
        if (FindMeMod.CONFIG.COMMON.IGNORE_ITEM_DAMAGE)
            return ItemStack.isSameItem(first, second);
        return ItemStack.isSameItemSameComponents(first, second);
    }

    @SuppressWarnings("null")
    public void handle(ServerPlayNetworking.Context context) {
        var player = context.player();
        var level = player.level();
        context.server().execute(() -> {
            AABB box = new AABB(player.blockPosition()).inflate(FindMeMod.CONFIG.COMMON.RADIUS_RANGE);
            List<BlockPos> blockPosList = new ArrayList<>();
            List<Integer> itemEntityIds = new ArrayList<>();
            List<Integer> entityIds = new ArrayList<>();
            for (BlockPos blockPos : getBlockPosInAABB(box)) {
                BlockEntity tileEntity = level.getBlockEntity(blockPos);
                if (tileEntity != null && FindMeMod.BLOCK_CHECKERS.stream().anyMatch(predicate -> predicate.test(tileEntity, stack))) {
                    blockPosList.add(blockPos);
                }
            }
            if (FindMeMod.CONFIG.COMMON.SEARCH_ITEM_ENTITIES) {
                for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, box)) {
                    if (compareItems(stack, itemEntity.getItem())) {
                        itemEntityIds.add(itemEntity.getId());
                    }
                }
            }
            if (FindMeMod.CONFIG.COMMON.SEARCH_ENTITY_INVENTORIES) {
                for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
                    if (entity == player) continue;
                    boolean found = false;
                    if (entity instanceof Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            if (!container.getItem(i).isEmpty() && compareItems(stack, container.getItem(i))) {
                                entityIds.add(entity.getId());
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found && entity instanceof InventoryCarrier carrier) {
                        Container inv = carrier.getInventory();
                        for (int i = 0; i < inv.getContainerSize(); i++) {
                            if (!inv.getItem(i).isEmpty() && compareItems(stack, inv.getItem(i))) {
                                entityIds.add(entity.getId());
                                found = true;
                                break;
                            }
                        }
                    }
                    if (!found && entity instanceof LivingEntity livingEntity) {
                        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
                            if (compareItems(stack, livingEntity.getItemBySlot(slot))) {
                                entityIds.add(entity.getId());
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                                if (compareItems(stack, livingEntity.getItemBySlot(slot))) {
                                    entityIds.add(entity.getId());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            if (!blockPosList.isEmpty() || !itemEntityIds.isEmpty() || !entityIds.isEmpty()) {
                ServerPlayNetworking.send(player, new PositionResponseMessage(blockPosList, itemEntityIds, entityIds));
            }


        });
        //context.player().setPacketHandled(true);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
