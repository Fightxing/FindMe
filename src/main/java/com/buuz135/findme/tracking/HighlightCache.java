package com.buuz135.findme.tracking;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Environment(EnvType.CLIENT)
public class HighlightCache {

    public enum HighlightType {
        BLOCK,
        ITEM_ENTITY,
        ENTITY
    }

    public static class Entry {
        private final HighlightType type;
        private final BlockPos blockPos;
        private final int entityId;
        private int remainingTicks;
        private final int initialTicks;

        public Entry(HighlightType type, @Nullable BlockPos blockPos, int entityId, int durationTicks) {
            this.type = type;
            this.blockPos = blockPos;
            this.entityId = entityId;
            this.remainingTicks = durationTicks;
            this.initialTicks = durationTicks;
        }

        public HighlightType getType() { return type; }
        @Nullable
        public BlockPos getBlockPos() { return blockPos; }
        public int getEntityId() { return entityId; }
        public int getRemainingTicks() { return remainingTicks; }
        public int getInitialTicks() { return initialTicks; }
    }

    private static final List<Entry> entries = new ArrayList<>();

    public static void addBlockHighlight(BlockPos pos, int durationTicks) {
        entries.add(new Entry(HighlightType.BLOCK, pos, -1, durationTicks));
    }

    public static void addItemEntityHighlight(int entityId, int durationTicks) {
        entries.add(new Entry(HighlightType.ITEM_ENTITY, null, entityId, durationTicks));
    }

    public static void addEntityHighlight(int entityId, int durationTicks) {
        entries.add(new Entry(HighlightType.ENTITY, null, entityId, durationTicks));
    }

    public static void tick() {
        var level = Minecraft.getInstance().level;

        Iterator<Entry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            entry.remainingTicks--;
            if (entry.remainingTicks <= 0) {
                iterator.remove();
                continue;
            }

            // Cancel highlights when the target no longer exists
            if (level != null) {
                switch (entry.getType()) {
                    case BLOCK -> {
                        BlockPos pos = entry.getBlockPos();
                        if (pos != null && level.getBlockState(pos).isAir()) {
                            iterator.remove();
                        }
                    }
                    case ITEM_ENTITY, ENTITY -> {
                        if (level.getEntity(entry.getEntityId()) == null) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    public static void clear() {
        entries.clear();
    }

    public static List<Entry> getEntries() {
        return entries;
    }
}