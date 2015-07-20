package me.rbrickis.ssuploader.events;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import me.rbrickis.ssuploader.Keybindings;
import me.rbrickis.ssuploader.utils.ScreenShotHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;

public class KeybindHandler {

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (Keybindings.TAKE_N_UPLOAD.isPressed()) {
            // Uploads or saves the screenshot.
            IChatComponent msg = ScreenShotHelper.saveScreenshot(mc.mcDataDir, null, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            mc.ingameGUI.getChatGUI().printChatMessage(msg);
        }
    }

}
