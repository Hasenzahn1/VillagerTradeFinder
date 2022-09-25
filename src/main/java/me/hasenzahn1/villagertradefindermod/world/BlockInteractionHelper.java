package me.hasenzahn1.villagertradefindermod.world;

import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BlockInteractionHelper {


    public static boolean place(BlockPos blockPos){
        Vec3d hitpos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        BlockPos neighbor;
        Direction side = BlockInteractionHelper.getPlaceSide(blockPos);

        if(side == null){
            side = Direction.UP;
            neighbor = blockPos;
        }else{
            neighbor = blockPos.offset(side.getOpposite());
            hitpos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
        }
        Direction s = side;

        return BlockInteractionHelper.place(blockPos, new BlockHitResult(hitpos, s, neighbor, false));
    }

    private static boolean place(BlockPos blockPos, BlockHitResult blockHitResult){
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;

        boolean wasSneaking = player.input.sneaking;
        player.input.sneaking = false;

        ActionResult result = MinecraftClient.getInstance().interactionManager.interactBlock(player, Hand.MAIN_HAND, blockHitResult);

        if(result.shouldSwingHand()){
            player.swingHand(Hand.MAIN_HAND);
        }

        player.input.sneaking = wasSneaking;
        return result.isAccepted();
    }

    public static void interactWithVillager(VillagerEntity entity){
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert MinecraftClient.getInstance().interactionManager != null;
        assert player != null;

        ActionResult result = MinecraftClient.getInstance().interactionManager.interactEntity(player, entity, Hand.MAIN_HAND);
        if(result.isAccepted()){
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static boolean breakBlock(BlockPos blockPos){
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert MinecraftClient.getInstance().interactionManager != null;
        assert player != null;


        boolean value = MinecraftClient.getInstance().interactionManager.updateBlockBreakingProgress(blockPos, Direction.DOWN);
        player.swingHand(Hand.MAIN_HAND);
        return value;
    }

    private static Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            BlockState state = MinecraftClient.getInstance().world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            return side2;
        }

        return null;
    }

    private static boolean isClickable(Block block) {
        return block instanceof CraftingTableBlock
                || block instanceof AnvilBlock
                || block instanceof AbstractButtonBlock
                || block instanceof AbstractPressurePlateBlock
                || block instanceof BlockWithEntity
                || block instanceof BedBlock
                || block instanceof FenceGateBlock
                || block instanceof DoorBlock
                || block instanceof NoteBlock
                || block instanceof TrapdoorBlock;
    }

}
