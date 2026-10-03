package com.direwolf20.buildinggadgets2.common.network.data;

import com.direwolf20.buildinggadgets2.BuildingGadgets2;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PowerSettingPayload(
        boolean requirePower
) implements CustomPacketPayload {
    public static final Type<PowerSettingPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(BuildingGadgets2.MODID, "power_setting_payload"));

    @Override
    public Type<PowerSettingPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf, PowerSettingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PowerSettingPayload::requirePower,
            PowerSettingPayload::new
    );
}
