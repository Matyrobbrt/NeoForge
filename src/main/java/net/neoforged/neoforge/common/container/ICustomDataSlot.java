package net.neoforged.neoforge.common.container;

import com.mojang.serialization.Codec;

public interface ICustomDataSlot<T> {
    boolean isDirty();

    void updateCache();

    Codec<T> getNetworkCodec();
    T getValue();
    void updateValue(T value);
}
