package net.neoforged.neoforge.common.container;

import com.mojang.serialization.Codec;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class ImmutableCustomDataSlot<T> implements ICustomDataSlot<T> {
    private T cached;

    private final Supplier<T> reader;
    private final Consumer<T> writer;
    private final BiPredicate<T, T> comparator;
    private final Codec<T> codec;

    public static ImmutableCustomDataSlot<Integer> integer(IntSupplier reader, IntConsumer writer) {
        return new ImmutableCustomDataSlot<>(reader::getAsInt, writer::accept, Integer::equals, Codec.INT);
    }

    public ImmutableCustomDataSlot(Supplier<T> reader, Consumer<T> writer, BiPredicate<T, T> comparator, Codec<T> codec) {
        this.reader = reader;
        this.writer = writer;
        this.comparator = comparator;
        this.codec = codec;
    }

    @Override
    public boolean isDirty() {
        return cached == null || !comparator.test(cached, reader.get());
    }

    @Override
    public void updateCache() {
        cached = reader.get();
    }

    @Override
    public Codec<T> getNetworkCodec() {
        return codec;
    }

    @Override
    public T getValue() {
        return reader.get();
    }

    @Override
    public void updateValue(T value) {
        writer.accept(value);
    }
}
