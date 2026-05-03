package com.buuz135.findme.client;

import com.buuz135.findme.FindMeMod;
import com.buuz135.findme.tracking.HighlightCache;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

import static net.minecraft.gizmos.Gizmos.cuboid;
import static net.minecraft.gizmos.Gizmos.line;
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
        BlockPos pos = entry.getBlockPos();
        if (pos == null) return;
        Color c = FindMeMod.CONFIG.CLIENT.getBlockLaserColor();
        int color = (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        float width = LINE_WIDTH * FindMeMod.CONFIG.CLIENT.LASER_WIDTH;
        var gizmo = cuboid(pos, stroke(color, width));
        if (alwaysOnTop) gizmo.setAlwaysOnTop();
    }

    @SuppressWarnings("null")
    private static void renderItemEntityHighlight(HighlightCache.Entry entry, int alpha,
                                                   net.minecraft.client.multiplayer.ClientLevel level,
                                                   float partialTick, boolean alwaysOnTop) {
        Entity entity = level.getEntity(entry.getEntityId());
        if (!(entity instanceof ItemEntity item)) return;

        Color c = FindMeMod.CONFIG.CLIENT.getItemLaserColor();
        int color = (alpha << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();

        Vec3 pos = item.getPosition(partialTick);
        // Raise the box slightly above the item's center to match its visual position
        double yOffset = 0.4;
        double half = ITEM_BOX_SIZE / 2.0;

        // Rotation angle matching item entity's natural spin (~3 degrees per tick)
        float age = (float) item.getAge() + partialTick;
        float angle = age * 0.0523598776f; // 3 degrees in radians

        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);

        double cx = pos.x;
        double cy = pos.y + yOffset;
        double cz = pos.z;
        double h = half;

        // 8 vertices of a cube centered at origin (before rotation)
        double[][] verts = {
            {-h, -h, -h}, { h, -h, -h}, {-h, -h,  h}, { h, -h,  h},
            {-h,  h, -h}, { h,  h, -h}, {-h,  h,  h}, { h,  h,  h}
        };

        // Rotate around Y axis and translate to position
        Vec3[] v = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            double x = verts[i][0] * cos - verts[i][2] * sin;
            double z = verts[i][0] * sin + verts[i][2] * cos;
            v[i] = new Vec3(cx + x, cy + verts[i][1], cz + z);
        }

        // 12 edges of the cube wireframe
        int[][] edges = {
            {0, 1}, {1, 3}, {3, 2}, {2, 0},  // bottom face
            {4, 5}, {5, 7}, {7, 6}, {6, 4},  // top face
            {0, 4}, {1, 5}, {2, 6}, {3, 7}   // vertical edges
        };

        float lineWidth = LINE_WIDTH * FindMeMod.CONFIG.CLIENT.LASER_WIDTH;
        for (int[] edge : edges) {
            var gizmo = line(v[edge[0]], v[edge[1]], color, lineWidth);
            if (alwaysOnTop) gizmo.setAlwaysOnTop();
        }
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

        float width = LINE_WIDTH * FindMeMod.CONFIG.CLIENT.LASER_WIDTH;
        var gizmo = cuboid(bb, stroke(color, width));
        if (alwaysOnTop) gizmo.setAlwaysOnTop();
    }
}