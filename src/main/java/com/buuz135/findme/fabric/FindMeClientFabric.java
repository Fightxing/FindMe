package com.buuz135.findme.fabric;

import com.buuz135.findme.FindMeModClient;
import net.fabricmc.api.ClientModInitializer;
import com.buuz135.findme.network.PositionResponseMessage;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class FindMeClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new FindMeModClient();
        ClientPlayNetworking.registerGlobalReceiver(PositionResponseMessage.TYPE, PositionResponseMessage::handle);
    }
}
