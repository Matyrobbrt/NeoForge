package net.neoforged.neoforge.event.entity.interaction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The event fired before {@link Item#use(Level, Player, InteractionHand)} is called.
 * <p>
 * This event is fired when a player right-clicks an item in air, or when the block interactions all pass.
 *
 * @apiNote Vanilla only accounts for a {@link InteractionResult#shouldSwing() success} result to trigger a swing.
 * {@linkplain InteractionResult#FAIL FAIL} and {@linkplain InteractionResult#CONSUME CONSUME} have identical outcomes.
 */
public class RightClickItemEvent extends PlayerEvent implements ICancellableEvent {
    @Nullable
    private InteractionResultHolder<ItemStack> interactionResult;
    private final Level level;
    private final ItemStack stack;
    private final InteractionHand hand;

    @ApiStatus.Internal
    public RightClickItemEvent(Player player, Level level, ItemStack stack, InteractionHand hand) {
        super(player);
        this.level = level;
        this.stack = stack;
        this.hand = hand;
    }

    /**
     * {@return the level in which this interaction took place}
     */
    public Level getLevel() {
        return level;
    }

    /**
     * {@return the used stack}
     */
    public ItemStack getUseStack() {
        return stack;
    }

    /**
     * {@return the hand used by the player}
     */
    public InteractionHand getHand() {
        return hand;
    }

    /**
     * {@return the interaction result}
     */
    @Nullable
    public InteractionResultHolder<ItemStack> getInteractionResult() {
        return interactionResult;
    }

    /**
     * Sets the interaction result of this event.
     * <p>
     * A non-null result will also {@link #setCanceled(boolean) cancel} this event, and a {@code null} one will un-cancel it.
     *
     * @param result the result
     */
    public void setInteractionResult(@javax.annotation.Nullable InteractionResultHolder<ItemStack> result) {
        this.interactionResult = result;
        setCanceled(result != null);
    }

    /**
     * Cancel this interaction, as a success. Any subsequent interactions will be aborted, but a client-side swing will be triggered.
     *
     * @see InteractionResult#sidedSuccess(boolean)
     */
    public void cancelWithSwing() {
        setInteractionResult(InteractionResultHolder.sidedSuccess(getUseStack(), getLevel().isClientSide));
    }

    /**
     * Cancel and consume this interaction. Any subsequent interactions will be aborted.
     *
     * @see InteractionResult#CONSUME
     */
    public void consume() {
        setInteractionResult(InteractionResultHolder.consume(getUseStack()));
    }

    /**
     * Cancel this event. If the {@link #setInteractionResult result} is not changed, it will default to
     * {@link InteractionResult#PASS}, preventing {@link Item#use(Level, Player, InteractionHand)} from being called.
     *
     * @param canceled whether this event should be cancelled
     * @deprecated Use {@link #setInteractionResult(InteractionResultHolder)} or similar methods instead, to set the result too.
     */
    @Override
    @Deprecated
    public void setCanceled(boolean canceled) {
        if (canceled && interactionResult != null) {
            interactionResult = InteractionResultHolder.pass(getUseStack());
        } else if (!canceled) {
            interactionResult = null;
        }
        ICancellableEvent.super.setCanceled(canceled);
    }
}
