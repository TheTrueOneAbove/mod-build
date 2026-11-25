package com.thetrueoneabove.mod;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {
    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheTrueOneAboveMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private NetworkHandler() {}

    public static void register() {
        CHANNEL.registerMessage(0, WhiteFlashPacket.class,
                WhiteFlashPacket::encode, WhiteFlashPacket::decode, WhiteFlashPacket::handle);
        CHANNEL.registerMessage(1, TimeStopPacket.class,
                TimeStopPacket::encode, TimeStopPacket::decode, TimeStopPacket::handle);
    }
}
