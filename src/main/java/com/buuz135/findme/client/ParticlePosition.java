package com.buuz135.findme.client;

import com.buuz135.findme.FindMeMod;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.util.Mth;


import java.awt.*;

@Environment(EnvType.CLIENT)
public class ParticlePosition extends SingleQuadParticle {


    // --- 新建 RenderPipeline，替换旧 RenderStateShard 设置 ---
    private static final RenderPipeline FINDME_PARTICLE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath("findme", "pipeline/particle"))
            .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
            // 不使用深度测试（等效于原 NO_DEPTH_TEST，让粒子穿墙可见）
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            // 透明混合（等效于原 TRANSLUCENT_TRANSPARENCY）
            .withBlend(BlendFunction.TRANSLUCENT)
            // 不剔除背面（等效于原 NO_CULL）
            .withCull(false)
            // 不写入深度缓冲（保证隔墙可见且不遮挡其他物体）
            .withDepthWrite(false)
            // 正常写入颜色，不写入 alpha 蒙版
            .withColorWrite(true, false)
            // 采样器，对应 shader 中的 Sampler0（纹理）
            .withSampler("Sampler0")
            .build()
    );

    // --- RenderType 只保留纹理 + 光照贴图，其余全部由 Pipeline 管理 ---
    private static final RenderType FINDME_PARTICLE_RENDER_TYPE = RenderType.create(
        "findme_particle",
        256,
        false,   // affects crumbling
        true,    // sort on upload
        FINDME_PARTICLE_PIPELINE,
        RenderType.CompositeState.builder()
            .setTextureState(new RenderStateShard.TextureStateShard(
                ResourceLocation.withDefaultNamespace("textures/particle/glitter_4.png"),
                false
            ))
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .createCompositeState(false)
    );

    public static final ParticleRenderType CUSTOM = new ParticleRenderType("CUSTOM2", FINDME_PARTICLE_RENDER_TYPE);

    public ParticlePosition(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.xd *= 0.10000000149011612D;
        this.yd *= 0.10000000149011612D;
        this.zd *= 0.10000000149011612D;
        this.xd += motionX;
        this.yd += motionY;
        this.zd += motionZ;
        float colorOffset = (float) (Math.random() * 0.30000001192092896D);
        Color c = FindMeMod.CONFIG.CLIENT.getParticleColor();
        this.rCol = ((float)c.getRed()) / 255f - colorOffset;
        this.gCol = ((float)c.getGreen()) / 255f - colorOffset;
        this.bCol = ((float)c.getBlue()) / 255f - colorOffset;
        //this.particleScale *= 1.5F;
        this.lifetime = 20 * 5;
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return CUSTOM;
    }

    @Override
    public float getQuadSize(float p_217561_1_) {
        return this.quadSize * Mth.clamp(((float) this.age + p_217561_1_) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    protected float getU0() {
        return 0;
    }

    @Override
    protected float getU1() {
        return 1f;
    }

    @Override
    protected float getV0() {
        return 0;
    }

    @Override
    protected float getV1() {
        return 1f;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        super.render(buffer, renderInfo, partialTicks);
    }

    @Override
    protected int getLightColor(float f) {
        return 15728880;
    }
}
