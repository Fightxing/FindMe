package com.buuz135.findme.client;

import com.buuz135.findme.FindMeMod;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;

import java.util.Optional;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.awt.*;

@Environment(EnvType.CLIENT)
public class ParticlePosition extends SingleQuadParticle {

    // ========== 1. 渲染管线：基于原版 PARTICLE_SNIPPET + NO_DEPTH_TEST ==========
    // 使用 Access Widener 暴露的 RenderPipelines.PARTICLE_SNIPPET 作为基础，
    // 覆盖深度测试为 NO_DEPTH_TEST 以实现穿墙渲染。
    // PARTICLE_SNIPPET 已正确声明 DynamicTransforms 等 uniform，避免运行时警告。
    private static final RenderPipeline FINDME_PIPELINE;
    static {
        FINDME_PIPELINE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("findme", "pipeline/particle"))
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false, 0f, 0f))
            .withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), 15))
            .withCull(false)
            .build();
    }

    // ========== 2. Layer：使用自定义管线 + 原版粒子图集纹理 ==========
    // textureAtlasLocation 必须是已加载的图集纹理 ID（非目录）
    private static final Layer FINDME_LAYER = new Layer(true,
        Identifier.withDefaultNamespace("textures/atlas/particles.png"),
        FINDME_PIPELINE);

    // ========== 3. ParticleRenderType（新版 record） ==========
    public static final ParticleRenderType CUSTOM = new ParticleRenderType("custom2");

    // ========== 4. 构造器（使用 5 参版本，手动设置速度） ==========
    public ParticlePosition(ClientLevel world, double x, double y, double z,
                            double motionX, double motionY, double motionZ) {
        // 5 参构造器：仅位置 + 精灵图，速度由父类设为 0
        super(world, x, y, z,
              Minecraft.getInstance().getAtlasManager()
                  .getAtlasOrThrow(Identifier.withDefaultNamespace("particles"))
                  .getSprite(Identifier.withDefaultNamespace("glitter_4")));
        // 原逻辑：xd *= 0.1（=0）后 += motion → 直接赋值为 motion
        this.xd = motionX;
        this.yd = motionY;
        this.zd = motionZ;

        float colorOffset = (float) (Math.random() * 0.30000001192092896D);
        Color c = FindMeMod.CONFIG.CLIENT.getParticleColor();
        this.rCol = ((float) c.getRed()) / 255f - colorOffset;
        this.gCol = ((float) c.getGreen()) / 255f - colorOffset;
        this.bCol = ((float) c.getBlue()) / 255f - colorOffset;
        this.lifetime = 20 * 5;
        this.hasPhysics = false;
    }

    // ========== 5. 实现抽象方法 getLayer() ==========
    @Override
    protected Layer getLayer() {
        return FINDME_LAYER;
    }

    // ========== 6. getGroup() 替代旧 getRenderType() ==========
    @Override
    public ParticleRenderType getGroup() {
        return CUSTOM;
    }

    // ========== 7. UV 方法已删除 — 父类自动从 sprite 获取 ==========
    // 新版 SingleQuadParticle 中 getU0/U1/V0/V1 不再是抽象方法，
    // 父类默认返回 this.sprite.getU0() 等，自动处理图集子区域。

    @Override
    public float getQuadSize(float partialTicks) {
        return this.quadSize * Mth.clamp(
            ((float) this.age + partialTicks) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    protected int getLightCoords(float partialTicks) {
        return 15728880;
    }

    // ========== 8. extract() — 穿墙渲染由管线 NO_DEPTH_TEST 保证 ==========
    // 粒子在真实世界位置以正常大小渲染，无需近裁剪面投影。
    // 自定义管线已设置 NO_DEPTH_TEST，确保粒子始终通过深度测试。
    @Override
    public void extract(QuadParticleRenderState state, Camera camera, float partialTicks) {
        super.extract(state, camera, partialTicks);
    }
}