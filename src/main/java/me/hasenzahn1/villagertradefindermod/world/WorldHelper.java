package me.hasenzahn1.villagertradefindermod.world;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.registry.Registry;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

import java.util.stream.Collectors;

public class WorldHelper {

    public static boolean isValidWorkstation(BlockState blockState){
        for(PointOfInterestType p : Registry.VILLAGER_PROFESSION.stream().map(VillagerProfession::getWorkStation).collect(Collectors.toList())){
            if (p.contains(blockState)) return true;
        }
        return false;
    }

    public static VillagerEntity getNearestVillager(VillagerEntity nearestVillager){
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        assert minecraftClient.world != null;
        assert minecraftClient.player != null;

        //Test Closest Villager in Reach
        VillagerEntity nearest = (nearestVillager != null && nearestVillager.getPos().distanceTo(minecraftClient.player.getPos()) < 5) ? nearestVillager : null;
        float distance = 100;
        if(nearestVillager == null) {
            for (Entity e : minecraftClient.world.getEntities()) {
                if (e instanceof VillagerEntity) {
                    VillagerEntity villagerEntity = (VillagerEntity) e;
                    if (villagerEntity.getPos().distanceTo(minecraftClient.player.getPos()) < 5 && villagerEntity.getPos().distanceTo(minecraftClient.player.getPos()) < distance) {
                        nearest = villagerEntity;
                        distance = (float) villagerEntity.getPos().distanceTo(minecraftClient.player.getPos());
                    }
                }
            }
        }
        return nearest;
    }
}
