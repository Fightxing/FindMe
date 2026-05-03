package com.buuz135.findme.client;

import com.buuz135.findme.FindMeMod;
import com.buuz135.findme.tracking.HighlightCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

import static net.minecraft.gizmos.Gizmos.cuboid;
import static net.minecraft.gizmos.GizmoStyle.stroke;

@Environment(EnvType.CLIENT)
public class HighlightRenderer {

    private static final float LINE_WIDTH = 2.5f;
    private static final double ITEM_BOX_SIZE = 0.5;

    public static void render(float partialTick) {
        if (HighlightCache.getEntries().isEmpty()) return;

        var config = FindMeMod.CONFIG.CLIENT;
        var level = Minecraft.getInstance().level;
        if (level == null) return;

        boolean alwaysOnTop = config.LASER_THROUGH_WALLS;

        for (HighlightCache.Entry entry : HighlightCache.getEntries()) {
            if (entry.getRemainingTicks() <= 0) continue;

            // Calculate pulsing alpha: 0.4-1.0 range, pulsing
            float elapsed = entry.getInitialTicks() - entry.getRemainingTicks();
            float pulse = 0.5f + 0.5f * (float) Math.sin((elapsed + partialTick) * 0.25f);

            // Fade out in last 10 ticks
            float fade = 1.0f;
            if (entry.getRemainingTicks() < 10) {
                fade = entry.getRemainingTicks() / 10.0f;
            }
            int alpha = Math.min(255, Math.max(4, (int) (pulse * fade * 255)));

            switch (entry.getType()) {
                case BLOCK -> renderBlockHighlight(entry, alpha, alwaysOnTop);
                case ITEM_ENTITY -> renderItemEntityHighlight(entry, alpha, level, partialTick, alwaysOnTop);
                case ENTITY -> renderEntityHighlight(entry, alpha, level, alwaysOnTop);
            }
        }
    }

    private static void renderBlockHighlight(HighlightCache.Entry entry, int alpha, boolean alwaysOnTop) {
        if (entry.getBlockPos() == null) return;
        Color c = FindMeMod.CONFIG.CLIENT.getBlockLaserColor();
        int color = (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        var gizmo = cuboid(entry.getBlockPos(), stroke(color, LINE_WIDTH));
        if (alwaysOnTop) gizmo.setAlwaysOnTop();
    }

    private static void renderItemEntityHighlight(HighlightCache.Entry entry, int alpha,
                                                   net.minecraft.client.multiplayer.ClientLevel level,
                                                   float partialTick, boolean alwaysOnTop) {
        Entity entity = level.getEntity(entry.getEntityId());
        if (!(entity instanceof ItemEntity item)) return;

        Color c = FindMeMod.CONFIG.CLIENT.getItemLaserColor();
        int color = (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();

        Vec3 pos = item.getPosition(partialTick);
        double half = ITEM_BOX_SIZE / 2.0;
        AABB box = new AABB(pos.x - half, pos.y - half, pos.z - half,
                            pos.x + half, pos.y + half, pos.z + half);

        var gizmo = cuboid(box, stroke(color, LINE_WIDTH));
        if (alwaysOnTop) gizmo.setAlwaysOnTop();
    }

    private static void renderEntityHighlight(HighlightCache.Entry entry, int alpha,
                                               net.minecraft.client.multiplayer.ClientLevel level,
                                               boolean alwaysOnTop) {
        Entity entity = level.getEntity(entry.getEntityId());
        if (entity == null) return;

        Color c = FindMeMod.CONFIG.CLIENT.getEntityLaserColor();
        int color = (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();

        AABB bb = entity.getBoundingBox();
        // Expand slightly for visual clarity
        bb = bb.inflate(0.05);

        var gizmo = cuboid(bb, stroke(color, LINE_WIDTH));
        if (alwaysOnTop) gizmo.setAlwaysOnTop();
    }
}