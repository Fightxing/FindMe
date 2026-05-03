package com.buuz135.findme;

import com.buuz135.findme.client.ClientTickHandler;
import com.buuz135.findme.client.ParticlePosition;
import com.buuz135.findme.network.PositionRequestMessage;
import com.buuz135.findme.network.PullItemRequestMessage;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;


import java.util.ArrayList;

import static net.minecraft.client.particle.ParticleEngine.RENDER_ORDER;

public class FindMeModClient {

    public static final KeyMapping.Category FINDME_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("findme", "findme"));

    public static KeyMapping KEY = new KeyMapping("key.findme.search", InputConstants.getKey("key.keyboard.y").getValue(), FINDME_CATEGORY);
    public static KeyMapping PULL_ONE = new KeyMapping("key.findme.pull_one", InputConstants.getKey("key.keyboard.keypad.0").getValue(), FINDME_CATEGORY);
    public static KeyMapping PULL_STACK = new KeyMapping("key.findme.pull_stack", InputConstants.getKey("key.keyboard.keypad.1").getValue(), FINDME_CATEGORY);



    public static long lastTooltipTime = 0;
    public static ItemStack lastRenderedStack = ItemStack.EMPTY;

    public static boolean keySearchPressed = false;
    public static boolean keyPullOnePressed = false;
    public static boolean keyPullStackPressed = false;

    public FindMeModClient() {
        init();
    }

    private static void init() {

        KeyBindingHelper.registerKeyBinding(KEY);
        KeyBindingHelper.registerKeyBinding(PULL_ONE);
        KeyBindingHelper.registerKeyBinding(PULL_STACK);
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientTickHandler.clientTick());
        ItemTooltipCallback.EVENT.register((stack, context,flag, lines) -> {
            if (!stack.isEmpty() && Minecraft.getInstance().level != null) {
                lastRenderedStack = stack.copyWithCount(1);
                lastTooltipTime = Minecraft.getInstance().level.getGameTime();
            }
        });
        
        ParticleFactoryRegistry.getInstance().register(
            FindMeMod.FIND_ME_PARTICLE_TYPE,
            new ParticleProvider<>() {
                @Override
                public net.minecraft.client.particle.Particle createParticle(
                        net.minecraft.core.particles.ParticleOptions particleOptions,
                        net.minecraft.client.multiplayer.ClientLevel level,
                        double x, double y, double z,
                        double xSpeed, double ySpeed, double zSpeed) {
                    // 从粒子图集获取精灵图
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                        .getAtlasManager()
                        .getAtlasOrThrow(TextureAtlas.LOCATION_PARTICLES)
                        .getSprite(Identifier.withDefaultNamespace("particle/glitter_4"));
                    return new ParticlePosition(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite);
                }
            }
        );



        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            if (lastRenderedStack.isEmpty() || client.level.getGameTime() - lastTooltipTime >= 3) {
                keySearchPressed = false;
                keyPullOnePressed = false;
                keyPullStackPressed = false;
                return;
            }
            if (keySearchPressed){
                    keySearchPressed = false;
                    ClientPlayNetworking.send(new PositionRequestMessage(lastRenderedStack));
            }
                if (keyPullOnePressed){
                    keyPullOnePressed = false;
                    ClientPlayNetworking.send(new PullItemRequestMessage(lastRenderedStack, 1));
                }
                if (keyPullStackPressed){
                    keyPullStackPressed = false;
                    ClientPlayNetworking.send(new PullItemRequestMessage(lastRenderedStack, lastRenderedStack.getMaxStackSize()));
                }
            });
        if (!RENDER_ORDER.contains(ParticlePosition.CUSTOM)) {
            RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
            RENDER_ORDER.add(ParticlePosition.CUSTOM);
        }
    }

}
