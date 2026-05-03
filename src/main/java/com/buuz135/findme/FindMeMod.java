package com.buuz135.findme;

import com.buuz135.findme.network.PositionRequestMessage;
import com.buuz135.findme.network.PositionResponseMessage;
import com.buuz135.findme.network.PullItemRequestMessage;
import com.buuz135.findme.particle.CustomParticleType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;


public class FindMeMod {

    public static final String MOD_ID = "findme";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static FindMeConfig CONFIG = new FindMeConfig();

    public static List<BiPredicate<BlockEntity, ItemStack>> BLOCK_CHECKERS = new ArrayList<>();
    public static List<IInventoryPuller> BLOCK_EXTRACTORS = new ArrayList<>();

    public static CustomParticleType FIND_ME_PARTICLE_TYPE = new CustomParticleType(false);
    public static ParticleType<?> FINDME;

    public static void init() {
        FINDME = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "particle"), FIND_ME_PARTICLE_TYPE);
        LOGGER.info("[FindMe Debug] Particle type registered: id={}, class={}", 
            BuiltInRegistries.PARTICLE_TYPE.getKey(FINDME), FINDME.getClass().getName());

        PayloadTypeRegistry.playC2S().register(PositionRequestMessage.TYPE, PositionRequestMessage.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PositionRequestMessage.TYPE, PositionRequestMessage::handle);
        PayloadTypeRegistry.playS2C().register(PositionResponseMessage.TYPE, PositionResponseMessage.CODEC);        PayloadTypeRegistry.playC2S().register(PullItemRequestMessage.TYPE, PullItemRequestMessage.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PullItemRequestMessage.TYPE, PullItemRequestMessage::handle);        BLOCK_CHECKERS.add((blockEntity, itemStack) -> {
            if (blockEntity instanceof Container inventory) {
                if (inventory.isEmpty()) return false;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    if (!inventory.getItem(i).isEmpty() && PositionRequestMessage.compareItems(itemStack, inventory.getItem(i))) {
                        return true;
                    }
                }
            }
            return false;
        });

        File file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json").toFile();
        if (!file.exists()) {
            createConfig(file);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileReader reader = new FileReader(file);
            CONFIG = gson.fromJson(reader, FindMeConfig.class);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            createConfig(file);
        }
    }

    private static void createConfig(File file) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            FileWriter fileWriter = new FileWriter(file);
            gson.toJson(CONFIG, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
