package com.buuz135.findme.mixin;

import com.buuz135.findme.client.HighlightRenderer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(at = @At("TAIL"), method = "renderLevel")
    private void findme_onRenderLevel(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker,
                                      boolean bl, CameraRenderState cameraRenderState, Matrix4fc matrix4fc,
                                      GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl2,
                                      ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci) {
        HighlightRenderer.render(deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}