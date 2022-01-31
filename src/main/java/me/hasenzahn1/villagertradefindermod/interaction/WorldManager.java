package me.hasenzahn1.villagertradefindermod.interaction;

import me.hasenzahn1.villagertradefindermod.config.Config;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class WorldManager {

    public enum Task{
        SCANNING,
        BREAKING,
        PLACING,
        CHECK_VILLAGER,
        SWITCH_TO_TOOL,
        SWITCH_TO_WORKSTATION
    }

    private ClientPlayerEntity player;
    private final MinecraftClient minecraftClient;
    private Task currentTask;
    private VillagerProfession villProf;
    private Item workStation;
    private BlockPos blockPos;
    private final ArrayList<PointOfInterestType> validWorkStationPOIs;
    private final Config config;
    private int minCost;
    private VillagerEntity nearestVillager;

    public WorldManager(Config config){
        minecraftClient = MinecraftClient.getInstance();
        this.config = config;

        validWorkStationPOIs = new ArrayList<>();
        validWorkStationPOIs.addAll(Registry.VILLAGER_PROFESSION.stream().map(VillagerProfession::getWorkStation).collect(Collectors.toList()));
    }

    public boolean tick(){
        switch (currentTask){
            case SCANNING -> {
                player = minecraftClient.player;
                assert player != null;

                boolean result = scanWorkstation();
                if(blockPos == null){
                    return false;
                }
                if(result){ //true when looking on workstation
                    currentTask = Task.CHECK_VILLAGER;
                    //if(!config.assumeAutoTool) currentTask = Task.SWITCH_TO_TOOL;
                    //else currentTask = Task.BREAKING;
                }else{
                    currentTask = Task.SWITCH_TO_WORKSTATION;
                }
            }

            case PLACING -> {
                //player.sendMessage(new LiteralText("PLACING"), false);
                //boolean success = interactWithBlock();

                Vec3d hitpos = new Vec3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
                BlockPos neighbor;
                Direction side = getPlaceSide(blockPos);

                if(side == null){
                    side = Direction.UP;
                    neighbor = blockPos;
                }else{
                    neighbor = blockPos.offset(side.getOpposite());
                    hitpos.add(side.getOffsetX() * 0.5, side.getOffsetY() * 0.5, side.getOffsetZ() * 0.5);
                }
                Direction s = side;

                boolean success = place(new BlockHitResult(hitpos, s, neighbor, false), Hand.MAIN_HAND, true);

                if(success) currentTask = Task.CHECK_VILLAGER;
            }
            case BREAKING -> {
                //player.sendMessage(new LiteralText("BREAKING"), false);
                if(!breakBlock()){
                    currentTask = Task.SWITCH_TO_WORKSTATION;
                }
            }
            case SWITCH_TO_TOOL -> {
                //player.sendMessage(new LiteralText("SWITCH_TO_TOOL"), false);
                assert minecraftClient.interactionManager != null;

                int bestTool = getBestTool();
                int selectedSlot = player.getInventory().selectedSlot + 36;
                //player.sendMessage(new LiteralText(bestTool + ": "), false);
                InventoryScreen inv = new InventoryScreen(player);


                if(bestTool != -1) { //No Best Tool
                    if(bestTool >= 36){
                        if(PlayerInventory.isValidHotbarIndex(bestTool - 36))
                            player.getInventory().selectedSlot = bestTool - 36;

                    }else {
                        //player.getInventory().swapSlotWithHotbar(bestTool);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, bestTool, 0, SlotActionType.PICKUP, player);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, selectedSlot, 0, SlotActionType.PICKUP, player);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, bestTool, 0, SlotActionType.PICKUP, player);
                    }

                }
                currentTask = Task.BREAKING;
            }
            case SWITCH_TO_WORKSTATION -> {
                //player.sendMessage(new LiteralText("SWITCH_TO_WORKSTATION"), false);
                assert minecraftClient.interactionManager != null;

                int slot = getWorkstationSlot();
                InventoryScreen inv = new InventoryScreen(player);
                int selectedSlot = player.getInventory().selectedSlot + 36;
                //player.sendMessage(new LiteralText(slot + ": " + selectedSlot), false);

                if(slot != -1){
                    if(slot >= 36){
                        if(PlayerInventory.isValidHotbarIndex(slot - 36))
                            player.getInventory().selectedSlot = slot - 36;
                    }else {

                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, player);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, selectedSlot, 0, SlotActionType.PICKUP, player);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, slot, 0, SlotActionType.PICKUP, player);
                    }
                    currentTask = Task.PLACING;
                }else{
                    player.sendMessage(new TranslatableText("villagertradefindermod.error.noworkstation"), true);
                }
            }

            case CHECK_VILLAGER -> {
                //player.sendMessage(new LiteralText("CHECK_VILLAGER"), false);

                VillagerEntity villagerEntity = getNearestVillager();
                if(villagerEntity == null){
                    player.sendMessage(new TranslatableText("villagertradefindermod.error.novillager"), true);
                    return true;
                }

                interactWithVillager(villagerEntity);

                Screen s = minecraftClient.currentScreen;
                if(s instanceof MerchantScreen){
                    MerchantScreen screen = (MerchantScreen) s;
                    StringBuilder sb = new StringBuilder();
                    for(TradeOffer offer : screen.getScreenHandler().getRecipes()){
                        //player.sendMessage(new LiteralText(offer.getSellItem().getItem().getName().getString() + ""), false);
                        if(sb.length() != 0) sb.append(" | ");
                        if(offer.getSellItem().getItem() == Items.ENCHANTED_BOOK){
                            NbtCompound enchantNBT = ((NbtCompound)((NbtList) offer.getSellItem().getNbt().get("StoredEnchantments")).get(0));
                            String id = enchantNBT.get("id").asString();
                            int lvl = Integer.parseInt(enchantNBT.get("lvl").asString().replace("s", ""));
                            sb.append(Registry.ENCHANTMENT.get(new Identifier(id)).getName(lvl).getString());
                            //player.sendMessage(new LiteralText(Registry.ENCHANTMENT.get(new Identifier(id)).getName(lvl).getString() + ""), false);
                        }
                        else sb.append(offer.getSellItem().getItem().getName().getString());
                    }
                    if(config.enableDebug) player.sendMessage(new LiteralText(sb.toString()), true);

                    for(TradeOffer offer : screen.getScreenHandler().getRecipes()){
                        //player.sendMessage(new LiteralText(offer.getOriginalFirstBuyItem().getItem() + " + " + offer.getSecondBuyItem().getItem() + " = " + offer.getSellItem().getItem()), false);
                        if(offer.getSellItem().getItem() == config.itemToSearch){
                            //Found Item
                            if(config.itemToSearch == Items.ENCHANTED_BOOK){
                                NbtCompound enchantNBT = ((NbtCompound)((NbtList) offer.getSellItem().getNbt().get("StoredEnchantments")).get(0));
                                String id = enchantNBT.get("id").asString();
                                int lvl = Integer.parseInt(enchantNBT.get("lvl").asString().replace("s", ""));
                                //player.sendMessage(new LiteralText("Enchanted Book: " + id + "; " + lvl), false);
                                if(Objects.equals(Registry.ENCHANTMENT.getKey(config.enchantment).get().getValue().toString(), id) && (config.ignoreLevel || config.enchantment.getMaxLevel() == lvl)) {
                                    if (config.perfectTrade) {
                                        if (offer.getOriginalFirstBuyItem().getCount() == minCost) {
                                            onFinish();
                                            return false;
                                        }
                                        onFail();
                                        return true;
                                    }else{
                                        onFinish();
                                        return false;
                                    }
                                }else{
                                    onFail();
                                    return true;
                                }
                            }
                            onFinish();
                            return false;

                        }
                    }
                }else{
                    return true;
                }
                onFail();
                return true;
            }
        }

        return true;
    }

    private void onFail(){
        if(!config.assumeAutoTool) currentTask = Task.SWITCH_TO_TOOL;
        else currentTask = Task.BREAKING;
        player.closeHandledScreen();
    }

    private void onFinish(){
        minecraftClient.inGameHud.setTitle(new TranslatableText("villagertradefindermod.title.success"));
        minecraftClient.inGameHud.setTitleTicks(2, 20 * 2, 2);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1, 0);
        player.closeHandledScreen();

    }

    private boolean place(BlockHitResult blockHitResult, Hand hand, boolean swing){
        boolean wasSneaking = player.input.sneaking;
        player.input.sneaking = false;

        ActionResult result = minecraftClient.interactionManager.interactBlock(player, minecraftClient.world, hand, blockHitResult);

        if(result.shouldSwingHand()){
            if(swing) player.swingHand(hand);
            else minecraftClient.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
        }

        player.input.sneaking = wasSneaking;
        return result.isAccepted();
    }

    private void interactWithVillager(VillagerEntity entity){
        assert minecraftClient.interactionManager != null;

        ActionResult result = minecraftClient.interactionManager.interactEntity(player, entity, Hand.MAIN_HAND);
        if(result.isAccepted()){
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    private VillagerEntity getNearestVillager(){
        assert minecraftClient.world != null;

        //Test Closest Villager in Reach
        VillagerEntity nearest = (nearestVillager != null && nearestVillager.getPos().distanceTo(player.getPos()) < 5) ? nearestVillager : null;
        float distance = 100;
        if(nearestVillager == null) {
            for (Entity e : minecraftClient.world.getEntities()) {
                if (e instanceof VillagerEntity) {
                    VillagerEntity villagerEntity = (VillagerEntity) e;
                    if (villagerEntity.getPos().distanceTo(player.getPos()) < 5 && villagerEntity.getPos().distanceTo(player.getPos()) < distance) {
                        nearest = villagerEntity;
                        distance = (float) villagerEntity.getPos().distanceTo(player.getPos());
                    }
                }
            }
            nearestVillager = nearest;
        }
        return nearest;
    }

    private boolean breakBlock(){
        assert minecraftClient.interactionManager != null;
        boolean value = minecraftClient.interactionManager.updateBlockBreakingProgress(blockPos, Direction.DOWN);
        player.swingHand(Hand.MAIN_HAND);
        return value;
    }

    private boolean scanWorkstation(){
        assert minecraftClient.world != null;

        //Get Villager Profession
        for(Map.Entry<VillagerProfession, TreeSet<Item>> profToItem : config.getItemToWorkstationMap().entrySet()){
            for(Item i : profToItem.getValue()){
                if(i.equals(config.itemToSearch)){
                    villProf = profToItem.getKey();
                    break;
                }
            }
        }

        //GetEnchantment Cost
        if(config.itemToSearch == Items.ENCHANTED_BOOK){
            minCost = 2 + config.enchantment.getMaxLevel() * 3;
            if(config.enchantment.isTreasure()) minCost *= 2;
        }

        BlockHitResult h = (BlockHitResult) player.raycast(5, 0, false);
        if(h.getType() == HitResult.Type.BLOCK){
            BlockState state = minecraftClient.world.getBlockState(h.getBlockPos());
            blockPos = h.getBlockPos();
            //player.sendMessage(new LiteralText(blockPos + ""), false);
            if(!isValidWorkstation(state)) blockPos = blockPos.add(h.getSide().getOffsetX(), h.getSide().getOffsetY(), h.getSide().getOffsetZ());
            //player.sendMessage(new LiteralText(state.getBlock() + " | " + minecraftClient.world.getBlockState(blockPos).getBlock() + " | " + h.getSide().getOffsetX() +
            //        " | " + h.getSide().getOffsetY() + " | " + h.getSide().getOffsetZ() + " | " + blockPos), false);
            return isValidWorkstation(state);
        }else{
            player.sendMessage(new TranslatableText("villagertradefindermod.action.noground"), false);
        }
        return false;
    }

    private boolean isValidWorkstation(BlockState blockState){
        for(PointOfInterestType p : validWorkStationPOIs){
            if (p.contains(blockState)) return true;
        }
        return false;
    }

    public void reset(){
        currentTask = Task.SCANNING;
        blockPos = null;
        nearestVillager = null;
    }

    private int getWorkstationSlot(){
        assert player != null;
        assert villProf != null;

        PlayerScreenHandler playerScreenHandler = new InventoryScreen(player).getScreenHandler();

        for(int i = 0; i < playerScreenHandler.slots.size(); i++){
            ItemStack stack = playerScreenHandler.getSlot(i).getStack();
            Block block = Block.getBlockFromItem(stack.getItem());
            BlockState state = block.getDefaultState();
            if(villProf.getWorkStation().contains(state)){
               // player.sendMessage(new LiteralText(block + ""), false);
                workStation = stack.getItem();
                return i;
            }
        }
        return -1;
    }

    private int getBestTool(){
        assert minecraftClient.world != null;
        assert minecraftClient.player != null;
        assert blockPos != null;

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

    public Direction getPlaceSide(BlockPos blockPos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = blockPos.offset(side);
            Direction side2 = side.getOpposite();

            BlockState state = minecraftClient.world.getBlockState(neighbor);

            // Check if neighbour isn't empty
            if (state.isAir() || isClickable(state.getBlock())) continue;

            // Check if neighbour is a fluid
            if (!state.getFluidState().isEmpty()) continue;

            return side2;
        }

        return null;
    }

    public static boolean isClickable(Block block) {
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
