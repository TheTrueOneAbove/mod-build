package com.thetrueoneabove.mod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TimeStopPacket {
    private final int ticks;
    private final boolean caster;

    public TimeStopPacket(int ticks, boolean caster) {
        this.ticks = ticks;
        this.caster = caster;
    }

    public static void encode(TimeStopPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.ticks);
        buffer.writeBoolean(message.caster);
    }

    public static TimeStopPacket decode(FriendlyByteBuf buffer) {
        return new TimeStopPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(TimeStopPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final int ticks = message.ticks;
        final boolean caster = message.caster;
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientWhiteFlash.startTimeStop(ticks, caster)));
        context.setPacketHandled(true);
    }
}
