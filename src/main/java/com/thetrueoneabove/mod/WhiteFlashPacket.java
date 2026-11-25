package com.thetrueoneabove.mod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class WhiteFlashPacket {
    public WhiteFlashPacket() {}

    public static void encode(WhiteFlashPacket message, FriendlyByteBuf buffer) {}
    public static WhiteFlashPacket decode(FriendlyByteBuf buffer) { return new WhiteFlashPacket(); }

    public static void handle(WhiteFlashPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> ClientWhiteFlash::start));
        context.setPacketHandled(true);
    }
}
