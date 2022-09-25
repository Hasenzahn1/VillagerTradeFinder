package me.hasenzahn1.villagertradefindermod.world;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;

public class PlayerHelper {

    public static int getWorkstationSlot(VillagerProfession villProf){
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;

        PlayerScreenHandler playerScreenHandler = new InventoryScreen(player).getScreenHandler();

        for(int i = 0; i < playerScreenHandler.slots.size(); i++){
            ItemStack stack = playerScreenHandler.getSlot(i).getStack();
            Block block = Block.getBlockFromItem(stack.getItem());
            BlockState state = block.getDefaultState();
            if(WorldHelper.isVillagerWorkstation(villProf, state)){
                return i;
            }
            /*
            if(villProf.getWorkStation().contains(state)){
                return i;
            }

             */
        }
        return -1;
    }



    public static int getBestTool(BlockPos blockPos){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        assert minecraftClient.world != null;
        assert minecraftClient.player != null;

        PlayerScreenHandler playerScreenHandler = new InventoryScreen(minecraftClient.player).getScreenHandler();
        BlockState state = minecraftClient.world.getBlockState(blockPos);

        int airIndex = -1, bestToolIndex = -1;
        float bestMiningSpeed = 1;

        for(int i = 0; i < playerScreenHandler.slots.size(); i++){
            ItemStack stack = playerScreenHandler.getSlot(i).getStack();
            if(stack.getMiningSpeedMultiplier(state) > bestMiningSpeed) {
                bestMiningSpeed = stack.getMiningSpeedMultiplier(state);
                bestToolIndex = i;
            }else if(stack.equals(ItemStack.EMPTY)){
                airIndex = i;
            }
        }
        return bestToolIndex > 1 ? bestToolIndex : airIndex;
    }

}
