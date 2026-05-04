package com.buuz135.findme;

import com.buuz135.findme.client.ClientTickHandler;
import com.buuz135.findme.network.PositionRequestMessage;
import com.buuz135.findme.network.PullItemRequestMessage;
import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

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
        KeyMappingHelper.registerKeyMapping(KEY);
        KeyMappingHelper.registerKeyMapping(PULL_ONE);
        KeyMappingHelper.registerKeyMapping(PULL_STACK);
        ClientTickEvents.START_CLIENT_TICK.register(client -> ClientTickHandler.clientTick());

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (!stack.isEmpty() && Minecraft.getInstance().level != null) {
                lastRenderedStack = stack.copyWithCount(1);
                lastTooltipTime = Minecraft.getInstance().level.getGameTime();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

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
