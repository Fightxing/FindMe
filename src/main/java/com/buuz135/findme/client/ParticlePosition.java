package com.buuz135.findme.client;

import com.buuz135.findme.FindMeMod;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.awt.*;

@Environment(EnvType.CLIENT)
public class ParticlePosition extends SingleQuadParticle {

    private static final RenderPipeline FINDME_PARTICLE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("findme", "pipeline/particle"))
            .withVertexShader("core/particle")
            .withFragmentShader("core/particle")
            .withVertexFormat(DefaultVertexFormat.PARTICLE, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)   // 穿墙可见
            .withBlend(BlendFunction.TRANSLUCENT)                     // 透明混合
            .withCull(false)                                          // 不剔除
            .withDepthWrite(false)                                    // 不写深度
            .withColorWrite(true, false)
            .withSampler("Sampler0")
            .build()
    );

    // ========== 2. Layer：定义粒子所属图层 ==========
    private static final Layer FINDME_LAYER = new Layer(
        true,                            // translucent = true（半透明）
        Identifier.withDefaultNamespace("particles"), // 粒子图集
        FINDME_PARTICLE_PIPELINE         // 自定义管线
    );

    // ========== 3. ParticleRenderType（新版 record） ==========
    public static final ParticleRenderType CUSTOM = new ParticleRenderType("custom2");

    // ========== 4. 构造器（使用 5 参版本，手动设置速度） ==========
    public ParticlePosition(ClientLevel world, double x, double y, double z,
                            double motionX, double motionY, double motionZ) {
        // 5 参构造器：仅位置 + 精灵图，速度由父类设为 0
        super(world, x, y, z,
              Minecraft.getInstance().getAtlasManager()
                  .getAtlasOrThrow(Identifier.withDefaultNamespace("particles"))
                  .getSprite(Identifier.withDefaultNamespace("particle/glitter_4")));
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
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return 15728880;
    }
}