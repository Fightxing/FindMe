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

    // ========== 调试开关 ==========
    private static final boolean DEBUG = true;
    private boolean debugFirstTick = true;
    private boolean debugFirstGetGroup = true;
    private boolean debugFirstGetQuadSize = true;

    // ========== 1. 自定义渲染管线 ==========
    // 基于 PARTICLE_SNIPPET 构建，继承着色器/Sampler0/Sampler2/DynamicTransforms/Fog 等全部配置
    // particle.json 仅覆盖：NO_DEPTH_TEST（穿墙）+ TRANSLUCENT（半透明）+ 不剔除 + 不写深度
    // JSON 中不指定着色器/顶点格式等，全部从 PARTICLE_SNIPPET 继承
    private static final RenderPipeline FINDME_PARTICLE_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(new RenderPipeline.Snippet[]{RenderPipelines.PARTICLE_SNIPPET})
            .withLocation(Identifier.fromNamespaceAndPath("findme", "pipeline/particle"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withCull(false)
            .withDepthWrite(false)
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

        if (DEBUG) {
            FindMeMod.LOGGER.info("[FindMe Debug] Particle created: pos=({}, {}, {}), " +
                "velocity=({}, {}, {}), quadSize={}, sprite={}, atlasSprite={}, " +
                "color=rgb({}, {}, {}), lifetime={}, alpha={}",
                String.format("%.2f", x), String.format("%.2f", y), String.format("%.2f", z),
                xd, yd, zd, this.quadSize,
                this.sprite != null ? this.sprite.contents().name() : "NULL",
                this.sprite != null ? this.sprite.atlasLocation() : "NULL",
                String.format("%.2f", rCol), String.format("%.2f", gCol), String.format("%.2f", bCol),
                this.lifetime, this.alpha);
        }
    }

    // ========== 5. 实现抽象方法 getLayer() ==========
    @Override
    protected Layer getLayer() {
        return FINDME_LAYER;
    }

    // ========== 6. getGroup() 替代旧 getRenderType() ==========
    @Override
    public ParticleRenderType getGroup() {
        if (DEBUG && debugFirstGetGroup) {
            debugFirstGetGroup = false;
            var order = net.minecraft.client.particle.ParticleEngine.RENDER_ORDER;
            FindMeMod.LOGGER.info("[FindMe Debug] getGroup() first call: group={}, " +
                "in RENDER_ORDER={}, RENDER_ORDER size={}",
                CUSTOM, order.contains(CUSTOM), order.size());
            for (int i = 0; i < order.size(); i++) {
                FindMeMod.LOGGER.info("[FindMe Debug]   RENDER_ORDER[{}] = {}", i, order.get(i));
            }
        }
        return CUSTOM;
    }

    // ========== 7. UV 方法已删除 — 父类自动从 sprite 获取 ==========
    // 新版 SingleQuadParticle 中 getU0/U1/V0/V1 不再是抽象方法，
    // 父类默认返回 this.sprite.getU0() 等，自动处理图集子区域。

    @Override
    public float getQuadSize(float partialTicks) {
        float result = this.quadSize * Mth.clamp(
            ((float) this.age + partialTicks) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
        if (DEBUG && debugFirstGetQuadSize) {
            debugFirstGetQuadSize = false;
            FindMeMod.LOGGER.info("[FindMe Debug] getQuadSize() first call: age={}, partialTicks={}, " +
                "quadSize={}, lifetime={}, scaleFactor={}, result={}",
                this.age, partialTicks, this.quadSize, this.lifetime,
                ((float) this.age + partialTicks) / (float) this.lifetime * 32.0F, result);
        }
        return result;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (DEBUG && debugFirstTick) {
            debugFirstTick = false;
            FindMeMod.LOGGER.info("[FindMe Debug] Particle first tick: age={}, alive={}, " +
                "pos=({}, {}, {})",
                this.age, this.isAlive(),
                String.format("%.2f", this.x), String.format("%.2f", this.y), String.format("%.2f", this.z));
        }
        if (this.age++ >= this.lifetime) {
            if (DEBUG) {
                FindMeMod.LOGGER.info("[FindMe Debug] Particle removed: age={}, maxLifetime={}",
                    this.age, this.lifetime);
            }
            this.remove();
        }
    }

    @Override
    protected int getLightColor(float partialTicks) {
        return 15728880;
    }
}