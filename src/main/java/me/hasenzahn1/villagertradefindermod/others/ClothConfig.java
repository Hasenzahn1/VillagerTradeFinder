package me.hasenzahn1.villagertradefindermod.others;

import me.hasenzahn1.villagertradefindermod.VillagerTradeFinderMod;
import me.hasenzahn1.villagertradefindermod.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.DropdownMenuBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.stream.Collectors;

public class ClothConfig {

    public static Screen openConfigScreen(Screen parent){
        //VillagerTradeFinderMod.LOGGER.info("Open GUI");
        ConfigBuilder builder = ConfigBuilder.create().setTitle(new TranslatableText("villagertradefindermod.config.title"));
        ConfigCategory scrolling = builder.getOrCreateCategory(new TranslatableText("villagertradefindermod.config.category"));
        ConfigEntryBuilder configEntryBuilder = ConfigEntryBuilder.create();
        Config c = VillagerTradeFinderMod.getInstance().getConfig();
        scrolling.addEntry(configEntryBuilder.startBooleanToggle(new TranslatableText("villagertradefindermod.config.perfecttrade"), c.perfectTrade).setDefaultValue(false).setSaveConsumer(b -> c.perfectTrade = b).build());
        scrolling.addEntry(configEntryBuilder.startBooleanToggle(new TranslatableText("villagertradefindermod.config.assumeautotool"), c.assumeAutoTool).setDefaultValue(false).setSaveConsumer(b -> c.assumeAutoTool = b).build());
        scrolling.addEntry(configEntryBuilder.startBooleanToggle(new TranslatableText("villagertradefindermod.config.ignorelevel"), c.ignoreLevel).setDefaultValue(false).setSaveConsumer(b -> c.ignoreLevel = b).build());
        scrolling.addEntry(configEntryBuilder.startBooleanToggle(new TranslatableText("villagertradefindermod.config.enabledebug"), c.enableDebug).setDefaultValue(false).setSaveConsumer(b -> c.enableDebug = b).build());


        scrolling.addEntry(configEntryBuilder.startDropdownMenu(new TranslatableText("villagertradefindermod.config.itemtosearch"),
                        DropdownMenuBuilder.TopCellElementBuilder.ofItemObject(c.itemToSearch),
                        DropdownMenuBuilder.CellCreatorBuilder.ofItemObject()
                )
                .setDefaultValue(Items.ENCHANTED_BOOK)
                .setSelections(c.getItems())
                .setSaveConsumer(item -> c.itemToSearch = item)
                .build());

        scrolling.addEntry(configEntryBuilder.startStringDropdownMenu(new TranslatableText("villagertradefindermod.config.enchantmenttosearch"), toObjectFunction(c.enchantment))
                .setSelections(Registry.ENCHANTMENT.stream().filter(Enchantment::isAvailableForEnchantedBookOffer).map(ClothConfig::toObjectFunction).collect(Collectors.toList()))
                .setDefaultValue(toObjectFunction(Enchantments.MENDING))
                .setSaveConsumer(ench -> c.enchantment = Registry.ENCHANTMENT.get(new Identifier(ench)))
                .build());

        return builder.setParentScreen(parent).build();
    }

    public static String toObjectFunction(Enchantment e){
        return Registry.ENCHANTMENT.getKey(e).get().getValue().toString();
    }
}
