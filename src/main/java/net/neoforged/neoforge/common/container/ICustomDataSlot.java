package net.neoforged.neoforge.common.container;

import net.minecraft.network.FriendlyByteBuf;

public interface ICustomDataSlot {
    boolean isDirty();

    void update();

    void write(FriendlyByteBuf buffer);

    void read(FriendlyByteBuf buffer);
}
