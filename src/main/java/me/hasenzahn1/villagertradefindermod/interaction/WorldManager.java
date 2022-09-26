package me.hasenzahn1.villagertradefindermod.interaction;

import me.hasenzahn1.villagertradefindermod.config.Config;
import me.hasenzahn1.villagertradefindermod.world.BlockInteractionHelper;
import me.hasenzahn1.villagertradefindermod.world.PlayerHelper;
import me.hasenzahn1.villagertradefindermod.world.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
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
    private BlockPos blockPos;
    private final ArrayList<PointOfInterestType> validWorkStationPOIs;
    private final Config config;
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

                villProf = getVillProf();
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
                boolean success = BlockInteractionHelper.place(blockPos);
                if(success) currentTask = Task.CHECK_VILLAGER;
            }
            case BREAKING -> {
                if(!BlockInteractionHelper.breakBlock(blockPos)) currentTask = Task.SWITCH_TO_WORKSTATION;

            }
            case SWITCH_TO_TOOL -> {
                assert minecraftClient.interactionManager != null;

                int bestTool = PlayerHelper.getBestTool(blockPos);
                int selectedSlot = player.getInventory().selectedSlot + 36;
                InventoryScreen inv = new InventoryScreen(player);

                if(bestTool != -1) { //No Best Tool
                    if(bestTool >= 36){
                        if(PlayerInventory.isValidHotbarIndex(bestTool - 36))
                            player.getInventory().selectedSlot = bestTool - 36;

                    }else {
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, bestTool, 0, SlotActionType.PICKUP, player);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, selectedSlot, 0, SlotActionType.PICKUP, player);
                        minecraftClient.interactionManager.clickSlot(inv.getScreenHandler().syncId, bestTool, 0, SlotActionType.PICKUP, player);
                    }

                }
                currentTask = Task.BREAKING;
            }
            case SWITCH_TO_WORKSTATION -> {
                assert minecraftClient.interactionManager != null;

                int slot = PlayerHelper.getWorkstationSlot(villProf);
                InventoryScreen inv = new InventoryScreen(player);
                int selectedSlot = player.getInventory().selectedSlot + 36;

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
                nearestVillager = WorldHelper.getNearestVillager(nearestVillager);
                if(nearestVillager == null){
                    player.sendMessage(new TranslatableText("villagertradefindermod.error.novillager"), true);
                    return true;
                }

                BlockInteractionHelper.interactWithVillager(nearestVillager);

                Screen s = minecraftClient.currentScreen;
                if(s instanceof MerchantScreen screen){
                    //DEBUG MESSAGES
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
                        sb.append(" (").append(offer.getOriginalFirstBuyItem().getCount()).append(")");
                    }
                    if(config.enableDebug) player.sendMessage(new LiteralText(sb.toString()), true);

                    //TRADE CHECK
                    for(TradeOffer offer : screen.getScreenHandler().getRecipes()){
                        //player.sendMessage(new LiteralText(offer.getOriginalFirstBuyItem().getItem() + " + " + offer.getSecondBuyItem().getItem() + " = " + offer.getSellItem().getItem()), false);
                        if(offer.getSellItem().getItem() == config.itemToSearch){
                            //Found Item
                            if(config.itemToSearch == Items.ENCHANTED_BOOK){
                                NbtCompound enchantNBT = ((NbtCompound)((NbtList) offer.getSellItem().getNbt().get("StoredEnchantments")).get(0));
                                String id = enchantNBT.get("id").asString();
                                int lvl = Integer.parseInt(enchantNBT.get("lvl").asString().replace("s", ""));

                                Enchantment found = Registry.ENCHANTMENT.get(new Identifier(id));
                                if(config.stopAtMaxLevelTrade){
                                    if(lvl == found.getMaxLevel()){
                                        onFinish();
                                        return false;
                                    }
                                }

                                if(config.stopAtPerfectTrade){
                                    if(lvl == found.getMaxLevel() && getMinCost(found) == offer.getOriginalFirstBuyItem().getCount()){
                                        onFinish();
                                        return false;
                                    }else{
                                        onFail();
                                        return true;
                                    }
                                }

                                //player.sendMessage(new LiteralText("Enchanted Book: " + id + "; " + lvl), false);
                                //player.sendMessage(new LiteralText(Registry.ENCHANTMENT.getId(found) + " | " + Registry.ENCHANTMENT.getId(config.enchantment)), false);
                                if(Registry.ENCHANTMENT.getId(found).equals(Registry.ENCHANTMENT.getId(config.enchantment)) && (!config.perfectTrade || config.enchantment.getMaxLevel() == lvl)) {
                                    if (config.perfectTrade) {
                                        if (offer.getOriginalFirstBuyItem().getCount() == getMinCost(config.enchantment)) {
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

    private int getMinCost(Enchantment e){
        int mincost = e.getMaxLevel() * 3 + 2;
        if(e.isTreasure()) mincost *= 2;
        return mincost;
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

    private VillagerProfession getVillProf(){
        for(Map.Entry<VillagerProfession, TreeSet<Item>> profToItem : config.getItemToWorkstationMap().entrySet()){
            for(Item i : profToItem.getValue()){
                if(i.equals(config.itemToSearch)){
                    return profToItem.getKey();
                }
            }
        }
        return null;
    }

    private boolean scanWorkstation(){
        assert minecraftClient.world != null;

        BlockHitResult h = (BlockHitResult) player.raycast(5, 0, false);
        if(h.getType() == HitResult.Type.BLOCK){
            BlockState state = minecraftClient.world.getBlockState(h.getBlockPos());
            blockPos = h.getBlockPos();
            if(!WorldHelper.isValidWorkstation(state)) blockPos = blockPos.add(h.getSide().getOffsetX(), h.getSide().getOffsetY(), h.getSide().getOffsetZ());
            return WorldHelper.isValidWorkstation(state);
        }else{
            player.sendMessage(new TranslatableText("villagertradefindermod.action.noground"), false);
        }
        return false;
    }

    public void reset(){
        currentTask = Task.SCANNING;
        blockPos = null;
        nearestVillager = null;
    }
}
