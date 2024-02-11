package net.neoforged.neoforge.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;

public record SetCustomContainerDataPayload(int containerId, int dataId, byte[] value) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation(NeoForgeVersion.MOD_ID, "container_set_custom_data");

    public SetCustomContainerDataPayload(FriendlyByteBuf buf) {
        this(buf.readByte(), buf.readShort(), buf.readByteArray());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(containerId);
        buffer.writeShort(dataId);
        buffer.writeByteArray(value);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
