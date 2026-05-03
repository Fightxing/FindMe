package com.buuz135.findme.mixin;

import com.buuz135.findme.FindMeModClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注入到 {@link KeyboardHandler#keyPress} 的 HEAD，在任何 GUI / Screen 处理之前截获按键，
 * 解决了打开 GUI（如 JEI 搜索框、创造模式物品栏）时 KeyMapping.consumeClick() 无法触发的问题。
 */
@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    /**
     * 在按键处理流程的最开头拦截。
     * 仅处理按下事件（action == GLFW_PRESS），且只响应 Minecraft 主窗口的按键。
     *
     * @param windowPointer GLFW 窗口指针
     * @param key           GLFW 键码
     * @param scanCode      平台相关扫描码
     * @param action        动作：0=释放, 1=按下, 2=重复
     * @param modifiers     修饰键位掩码
     */
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void findme_onKeyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        // 只处理按下事件（GLFW_PRESS = 1），忽略释放和重复
        if (action != 1) return;

        // 仅处理 Minecraft 主窗口的按键，避免在其他弹出窗口中误触发
        if (windowPointer != Minecraft.getInstance().getWindow().handle()) return;
        if (Minecraft.getInstance().screen != null) {
            if (Minecraft.getInstance().screen.getFocused() instanceof EditBox) {
                return;
            }
        }
        KeyEvent keyEvent = new KeyEvent(key, scanCode, modifiers);
        if (FindMeModClient.KEY.matches(keyEvent)) {
            FindMeModClient.keySearchPressed = true;
        }   
        if (FindMeModClient.PULL_ONE.matches(keyEvent)) {
            FindMeModClient.keyPullOnePressed = true;
        }
        if (FindMeModClient.PULL_STACK.matches(keyEvent)) {
            FindMeModClient.keyPullStackPressed = true;
        }
    }
}