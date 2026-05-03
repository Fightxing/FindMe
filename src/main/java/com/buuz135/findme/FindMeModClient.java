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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
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

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!stack.isEmpty() && Minecraft.getInstance().level != null) {
                lastRenderedStack = stack.copyWithCount(1);
                lastTooltipTime = Minecraft.getInstance().level.getGameTime();
            }
        });

        ParticleFactoryRegistry.getInstance().register(
            FindMeMod.FIND_ME_PARTICLE_TYPE,
            (particleOptions, clientLevel, d, e, f, g, h, i, randomSource) -> {
                FindMeMod.LOGGER.info("[FindMe Debug] Particle factory called: type={}, pos=({}, {}, {})",
                    particleOptions.getClass().getSimpleName(),
                    String.format("%.2f", d), String.format("%.2f", e), String.format("%.2f", f));
                return new ParticlePosition(clientLevel, d, e, f, g, h, i);
            }
        );
        FindMeMod.LOGGER.info("[FindMe Debug] Particle factory registered for type: {}",
            BuiltInRegistries.PARTICLE_TYPE.getKey(FindMeMod.FIND_ME_PARTICLE_TYPE));

        if (!RENDER_ORDER.contains(ParticlePosition.CUSTOM)) {
            RENDER_ORDER = new ArrayList<>(RENDER_ORDER);
            RENDER_ORDER.add(ParticlePosition.CUSTOM);
            FindMeMod.LOGGER.info("[FindMe Debug] CUSTOM added to RENDER_ORDER. New size={}",
                RENDER_ORDER.size());
        } else {
            FindMeMod.LOGGER.info("[FindMe Debug] CUSTOM already in RENDER_ORDER. Size={}",
                RENDER_ORDER.size());
        }

        // 粒子图集会在资源重载时自动加载，无需手动预加载

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            // ========== 调试：每 20 tick 在玩家头顶生成测试粒子 ==========
            if (client.level.getGameTime() % 20 == 0) {
                var pos = client.player.blockPosition();
                var testParticle = new ParticlePosition(
                    (ClientLevel) client.level,
                    pos.getX() + 0.5, pos.getY() + 3.0, pos.getZ() + 0.5,
                    0, 0.02, 0  // 轻微上浮
                );
                client.particleEngine.add(testParticle);
                FindMeMod.LOGGER.info("[FindMe Debug] Test particle spawned at player head +3Y. GameTime={}",
                    client.level.getGameTime());
            }

            if (lastRenderedStack.isEmpty() || client.level.getGameTime() - lastTooltipTime >= 3) {
                keySearchPressed = false;
                keyPullOnePressed = false;
                keyPullStackPressed = false;
                return;
            }
            if (keySearchPressed) {
                keySearchPressed = false;
                ClientPlayNetworking.send(new PositionRequestMessage(lastRenderedStack));
            }
            if (keyPullOnePressed) {
                keyPullOnePressed = false;
                ClientPlayNetworking.send(new PullItemRequestMessage(lastRenderedStack, 1));
            }
            if (keyPullStackPressed) {
                keyPullStackPressed = false;
                ClientPlayNetworking.send(new PullItemRequestMessage(lastRenderedStack, lastRenderedStack.getMaxStackSize()));
            }
        });
    }
}