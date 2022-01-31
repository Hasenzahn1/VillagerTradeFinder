package me.hasenzahn1.villagertradefindermod.config;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import me.hasenzahn1.villagertradefindermod.VillagerTradeFinderMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOffers;
import net.minecraft.village.VillagerProfession;

import java.util.*;
import java.util.stream.Collectors;

public class Config {

    public boolean perfectTrade;
    public Item itemToSearch;
    public Enchantment enchantment;
    public boolean assumeAutoTool;
    public boolean ignoreLevel;
    public boolean enableDebug;

    private Set<Item> items;
    private HashMap<VillagerProfession, TreeSet<Item>> itemToWorkstationMap;

    public Config(){

        perfectTrade = false;
        assumeAutoTool = false;
        ignoreLevel = false;
        enableDebug = false;
        enchantment = Enchantments.MENDING;

        //Load add TradeableItems
        Random random = new Random();
        itemToWorkstationMap = new HashMap<>();
        HashSet<Item> hereitems = new HashSet<>();
        //VillagerTradeFinderMod.LOGGER.info(Registry.VILLAGER_PROFESSION.stream().map(VillagerProfession::getGatherableItems).collect(Collectors.toList()) + "");
        for(Map.Entry<VillagerProfession, Int2ObjectMap<TradeOffers.Factory[]>> f : TradeOffers.PROFESSION_TO_LEVELED_TRADE.entrySet()){
            HashSet<Item> current = new HashSet<>();
            for(TradeOffers.Factory factory : f.getValue().get(1)) { // get tradeoffers of first trade
                if(!factory.getClass().getName().contains("SellMap")) {
                    TradeOffer t = factory.create(null, random);
                    if(t!= null) {
                        hereitems.add(t.getSellItem().getItem());
                        current.add(t.getSellItem().getItem());
                    }
                }
            }

            TreeSet<Item> set = new TreeSet<>(Comparator.comparing(item -> item.getName().getString()));
            set.addAll(current);
            itemToWorkstationMap.put(f.getKey(), set);
        }

        itemToSearch = Items.ENCHANTED_BOOK;
        items = new TreeSet<>(Comparator.comparing(item -> item.getName().getString()));
        items.addAll(hereitems);
        //VillagerTradeFinderMod.LOGGER.info(items + "");

    }

    public Set<Item> getItems() {
        return items;
    }

    public HashMap<VillagerProfession, TreeSet<Item>> getItemToWorkstationMap() {
        return itemToWorkstationMap;
    }
}
