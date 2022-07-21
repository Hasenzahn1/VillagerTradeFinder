package me.hasenzahn1.villagertradefindermod.listeners;

import me.hasenzahn1.villagertradefindermod.VillagerTradeFinderMod;
import me.hasenzahn1.villagertradefindermod.others.ClothConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyPressListener {

    private static final KeyBinding menu = new KeyBinding(
            "villagertradefindermod.key.config",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            I18n.translate("villagertradefindermod.category.title")
    );

    private static final KeyBinding start = new KeyBinding(
            "villagertradefindermod.key.start",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_DIVIDE,
            I18n.translate("villagertradefindermod.category.title")
    );

    private boolean finderRunning;
    private final MinecraftClient minecraftClient;

    public KeyPressListener(MinecraftClient minecraftClient){
        KeyBindingHelper.registerKeyBinding(menu);
        KeyBindingHelper.registerKeyBinding(start);
        this.minecraftClient = minecraftClient;
        finderRunning = false;


        ClientTickEvents.END_CLIENT_TICK.register(client -> onProcessKey());
    }

    public void onProcessKey(){
        if(menu.wasPressed()){
            MinecraftClient.getInstance().setScreen(ClothConfig.openConfigScreen(MinecraftClient.getInstance().currentScreen));
        }else if(start.wasPressed()){
            finderRunning = !finderRunning;
            if(!finderRunning){
                VillagerTradeFinderMod.getInstance().getWorldManager().reset();
            }
        }
        if(finderRunning){

            finderRunning = VillagerTradeFinderMod.getInstance().getWorldManager().tick();
            if(!finderRunning){
                VillagerTradeFinderMod.getInstance().getWorldManager().reset();
                //minecraftClient.player.sendMessage(new LiteralText("Reset WorldManager"), false);
            }

            //Break Block
            /*assert minecraftClient.interactionManager != null;
            assert minecraftClient.player != null;
            finderRunning = minecraftClient.interactionManager.updateBlockBreakingProgress(minecraftClient.player.getBlockPos().add(0, -2, 0), Direction.DOWN);
            minecraftClient.player.swingHand(Hand.MAIN_HAND);

             */

            //minecraftClient.interactionManager.attackBlock(minecraftClient.player.getBlockPos().add(0, -1, 0), Direction.DOWN);

        }
    }
}
