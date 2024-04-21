package net.neoforged.neoforge.event.entity.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;

/**
 * A parent class for events in the right click block pipeline.
 * <p>
 * The order of events and methods is as follows:
 * <ul>
 *     <li>{@link UseItem} before {@link Item#onItemUseFirst(ItemStack, UseOnContext)} - item-dictated interaction on the block</li>
 *     <li>{@link ActivateBlock} before {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)} - block-dictated interaction</li>
 *     <li>{@link UseBlockWithoutItem} before {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)} - item-less block-dictated interaction</li>
 *     <li>{@link UseItemOnBlock} before {@link Item#useOn(UseOnContext)} - item-dictated interactions that are after block activation</li>
 * </ul>
 */
public sealed abstract class RightClickBlockEvent extends Event implements ICancellableEvent {

    /**
     * {@return the level in which the interaction took place}
     */
    public abstract Level getLevel();

    /**
     * {@return the position that was clicked}
     */
    public abstract BlockPos getClickedPos();

    public sealed abstract static class WithResult extends RightClickBlockEvent {
        @Nullable
        protected InteractionResult interactionResult;

        /**
         * {@return the interaction to return}
         */
        @Nullable
        public InteractionResult getInteractionResult() {
            return this.interactionResult;
        }

        /**
         * Sets the interaction result of this event.
         * <p>
         * A non-null result will also {@link #setCanceled(boolean) cancel} this event, and a {@code null} one will un-cancel it.
         *
         * @param result the result
         */
        public void setInteractionResult(@Nullable InteractionResult result) {
            this.interactionResult = result;
            setCanceled(result != null);
        }
    }

    /**
     * The first event in the right click pipeline, when the item is used, but <strong>before</strong> the block is {@linkplain net.minecraft.world.level.block.Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) activated}.
     * This event is fired before {@link Item#onItemUseFirst(ItemStack, UseOnContext)}.
     * <p>
     * Cancellation of this event will prevent further processing, unless the result is {@link net.minecraft.world.InteractionResult#PASS PASS},
     * in which case {@link net.minecraft.world.item.Item#onItemUseFirst(ItemStack, UseOnContext)} will not be called but the rest of the pipeline will continue.
     * <p>
     * Example:
     * {@snippet :
     * void onUseItem(RightClickBlockEvent.UseItem event) {
     *    if (event.getLevel().getBlockState(event.getClickedPos()).is(Blocks.CAULDRON) && event.getContext().getItemInHand().is(Items.GLASS_BOTTLE)) {
     *        event.fail(); // Prevent bottles from being filled or filling cauldrons
     *    }
     *    if (event.getContext().getItemInHand().is(Items.APPLE)) {
     *        // Some logic
     *        event.cancelWithSwing(true); // Cancel further interactions, and swing the player's hand
     *    }
     * }
     *}
     */
    public static final class UseItem extends WithResult {
        private final UseOnContext context;

        @ApiStatus.Internal
        public UseItem(UseOnContext context) {
            this.context = context;
        }

        /**
         * {@return the use context}
         */
        public UseOnContext getContext() {
            return context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Level getLevel() {
            return context.getLevel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockPos getClickedPos() {
            return context.getClickedPos();
        }

        /**
         * Cancel this interaction, as a success. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked,
         * and a client-side swing will be triggered.
         *
         * @param awardStat whether to award the {@link net.minecraft.stats.Stats#ITEM_USED item used} stat
         * @see InteractionResult#sidedSuccess(boolean)
         */
        public void cancelWithSwing(boolean awardStat) {
            setInteractionResult(awardStat ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : (context.getLevel().isClientSide ? InteractionResult.SUCCESS_NO_ITEM_USED : InteractionResult.CONSUME_PARTIAL));
        }

        /**
         * Consume this interaction. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked.
         *
         * @param awardStat whether to award the {@link net.minecraft.stats.Stats#ITEM_USED item used} stat
         * @see InteractionResult#CONSUME
         */
        public void consume(boolean awardStat) {
            setInteractionResult(awardStat ? InteractionResult.CONSUME : InteractionResult.CONSUME_PARTIAL);
        }

        /**
         * Fail this interaction. Any subsequent interactions will be aborted.
         * @see InteractionResult#FAIL
         */
        public void fail() {
            setInteractionResult(InteractionResult.FAIL);
        }

        /**
         * Cancels this event, preventing {@link Item#onItemUseFirst(ItemStack, UseOnContext)} from being called,
         * but allowing the rest of the right click pipeline to continue.
         *
         * @see InteractionResult#PASS
         */
        public void pass() {
            setInteractionResult(InteractionResult.PASS);
        }

        /**
         * Cancel this event. If the {@link #setInteractionResult result} is not changed, it will default to
         * {@link InteractionResult#PASS}, preventing only {@link Item#onItemUseFirst(ItemStack, UseOnContext)} from being called.
         *
         * @param canceled whether this event should be cancelled
         * @deprecated Use {@link #setInteractionResult(InteractionResult)} or similar methods instead, to set the result too.
         */
        @Override
        @Deprecated
        public void setCanceled(boolean canceled) {
            if (canceled && interactionResult != null) {
                interactionResult = InteractionResult.PASS;
            }
            super.setCanceled(canceled);
        }
    }

    /**
     * The second event in the right click pipeline, when the block is activated, but <strong>before</strong> the item is {@linkplain Item#useOn(UseOnContext) used on the block}.
     * This event is fired before {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)}.
     * <p>
     * Cancellation of this event will prevent further processing, unless the result is {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION},
     * in which case {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) useItemOn} will not be called but the rest of the pipeline will continue,
     * or {@link ItemInteractionResult#SKIP_DEFAULT_BLOCK_INTERACTION} which will additionally skip {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult) useWithoutItem}.
     * <p>
     * This event can be used to overwrite use behaviour that is implemented by the block.
     */
    public static final class ActivateBlock extends RightClickBlockEvent {
        @Nullable
        private ItemInteractionResult interactionResult;

        private final Player player;
        private final Level level;
        private final BlockHitResult hitResult;
        private final InteractionHand hand;
        private final ItemStack useStack;

        @ApiStatus.Internal
        public ActivateBlock(Player player, Level level, BlockHitResult hitResult, InteractionHand hand, ItemStack useStack) {
            this.player = player;
            this.level = level;
            this.hitResult = hitResult;
            this.hand = hand;
            this.useStack = useStack;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Level getLevel() {
            return level;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockPos getClickedPos() {
            return hitResult.getBlockPos();
        }

        /**
         * {@return the player that is responsible for the interaction}
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * {@return the used hand}
         */
        public InteractionHand getHand() {
            return hand;
        }

        /**
         * {@return the stack used by the player}
         */
        public ItemStack getUseStack() {
            return useStack;
        }

        /**
         * {@return the hit result inside the block}
         */
        public BlockHitResult getHitResult() {
            return hitResult;
        }

        /**
         * Cancel this interaction, as a success. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked,
         * and a client-side swing will be triggered.
         *
         * @see ItemInteractionResult#sidedSuccess(boolean)
         */
        public void cancelWithSwing() {
            setInteractionResult(ItemInteractionResult.sidedSuccess(level.isClientSide));
        }

        /**
         * Consume this interaction. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked.
         * @see ItemInteractionResult#CONSUME
         */
        public void consume() {
            setInteractionResult(ItemInteractionResult.CONSUME);
        }

        /**
         * Fail this interaction. Any subsequent interactions will be aborted.
         * @see ItemInteractionResult#FAIL
         */
        public void fail() {
            setInteractionResult(ItemInteractionResult.FAIL);
        }

        /**
         * Cancels this event, preventing {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)} from being called,
         * but allowing the rest of the right click pipeline to continue to {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)}.
         *
         * @see ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION
         */
        public void passToEmptyBlockInteraction() {
            setInteractionResult(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
        }

        /**
         * Cancels this event, preventing both {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)} and
         * {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)} from being called, but allowing the rest of the pipeline to continue.
         */
        public void skipBlockInteraction() {
            setInteractionResult(ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION);
        }

        /**
         * {@return the interaction to return}
         */
        @Nullable
        public ItemInteractionResult getInteractionResult() {
            return this.interactionResult;
        }

        /**
         * Sets the interaction result of this event.
         * <p>
         * A non-null result will also {@link #setCanceled(boolean) cancel} this event, and a {@code null} one will un-cancel it.
         *
         * @param result the result
         */
        public void setInteractionResult(@Nullable ItemInteractionResult result) {
            this.interactionResult = result;
            setCanceled(result != null);
        }

        /**
         * Cancel this event. If the {@link #setInteractionResult result} is not changed, it will default to
         * {@link InteractionResult#PASS}, preventing only {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)} from being called.
         *
         * @param canceled whether this event should be cancelled
         * @deprecated Use {@link #setInteractionResult(ItemInteractionResult)} or similar methods instead, to set the result too.
         */
        @Override
        @Deprecated
        public void setCanceled(boolean canceled) {
            if (canceled && interactionResult != null) {
                interactionResult = ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            super.setCanceled(canceled);
        }
    }

    /**
     * The third event in the right click pipeline, when the block is used without an item, but <strong>before</strong> the item is {@linkplain Item#useOn(UseOnContext) used on the block}.
     * This event is fired before {@link Item#useOn(UseOnContext)}, <i>only if</i> the result of {@link Block#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult) block activation} is {@link ItemInteractionResult#PASS_TO_DEFAULT_BLOCK_INTERACTION}.
     * <p>
     * Cancellation of this event will prevent further processing, unless the result is {@link net.minecraft.world.InteractionResult#PASS PASS},
     * in which case {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult) useWithoutItem} will not be called but the rest of the pipeline will continue.
     * <p>
     * Use this event when the interaction you're trying to cancel or implement does not depend on the item used on the block (i.e. opening/closing a door).
     */
    public static final class UseBlockWithoutItem extends WithResult {
        private final Level level;
        private final Player player;
        private final BlockHitResult hitResult;

        @ApiStatus.Internal
        public UseBlockWithoutItem(Level level, Player player, BlockHitResult hitResult) {
            this.level = level;
            this.player = player;
            this.hitResult = hitResult;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Level getLevel() {
            return level;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockPos getClickedPos() {
            return hitResult.getBlockPos();
        }

        /**
         * {@return the player that is responsible for the interaction}
         */
        public Player getPlayer() {
            return player;
        }

        /**
         * {@return the hit result inside the block}
         */
        public BlockHitResult getHitResult() {
            return hitResult;
        }

        /**
         * Cancel this interaction, as a success. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked,
         * and a client-side swing will be triggered.
         *
         * @see InteractionResult#sidedSuccess(boolean)
         */
        public void cancelWithSwing() {
            setInteractionResult(InteractionResult.sidedSuccess(level.isClientSide));
        }

        /**
         * Consume this interaction. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked.
         *
         * @see InteractionResult#CONSUME
         */
        public void consume() {
            setInteractionResult(InteractionResult.CONSUME);
        }

        /**
         * Fail this interaction. Any subsequent interactions will be aborted.
         * @see InteractionResult#FAIL
         */
        public void fail() {
            setInteractionResult(InteractionResult.FAIL);
        }

        /**
         * Cancels this event, preventing {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)} from being called,
         * but allowing the rest of the right click pipeline to continue.
         *
         * @see InteractionResult#PASS
         */
        public void pass() {
            setInteractionResult(InteractionResult.PASS);
        }

        /**
         * Cancel this event. If the {@link #setInteractionResult result} is not changed, it will default to
         * {@link InteractionResult#PASS}, preventing only {@link Block#useWithoutItem(BlockState, Level, BlockPos, Player, BlockHitResult)} from being called.
         *
         * @param canceled whether this event should be cancelled
         * @deprecated Use {@link #setInteractionResult(InteractionResult)} or similar methods instead, to set the result too.
         */
        @Override
        @Deprecated
        public void setCanceled(boolean canceled) {
            if (canceled && interactionResult != null) {
                interactionResult = InteractionResult.PASS;
            }
            super.setCanceled(canceled);
        }
    }

    /**
     * The last event in the right click pipeline, when the item is used on the block.
     * This event is fired before {@link Item#useOn(UseOnContext)}.
     * <p>
     * Cancellation of this event will prevent {@link Item#useOn(UseOnContext)} from being called. If cancelled with {@link InteractionResult#PASS}, the pipeline
     * will continue to block-less interactions: {@link Item#use(Level, Player, InteractionHand)}.
     */
    public static final class UseItemOnBlock extends WithResult {
        private final UseOnContext context;

        @ApiStatus.Internal
        public UseItemOnBlock(UseOnContext context) {
            this.context = context;
        }

        /**
         * {@return the use context}
         */
        public UseOnContext getContext() {
            return context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Level getLevel() {
            return context.getLevel();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public BlockPos getClickedPos() {
            return context.getClickedPos();
        }

        /**
         * Cancel this interaction, as a success. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked,
         * and a client-side swing will be triggered.
         *
         * @param awardStat whether to award the {@link net.minecraft.stats.Stats#ITEM_USED item used} stat
         * @see InteractionResult#sidedSuccess(boolean)
         */
        public void cancelWithSwing(boolean awardStat) {
            setInteractionResult(awardStat ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : (context.getLevel().isClientSide ? InteractionResult.SUCCESS_NO_ITEM_USED : InteractionResult.CONSUME_PARTIAL));
        }

        /**
         * Consume this interaction. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#ANY_BLOCK_USE block use trigger} will be invoked.
         *
         * @param awardStat whether to award the {@link net.minecraft.stats.Stats#ITEM_USED item used} stat
         * @see InteractionResult#CONSUME
         */
        public void consume(boolean awardStat) {
            setInteractionResult(awardStat ? InteractionResult.CONSUME : InteractionResult.CONSUME_PARTIAL);
        }

        /**
         * Fail this interaction. Any subsequent interactions will be aborted.
         * @see InteractionResult#FAIL
         */
        public void fail() {
            setInteractionResult(InteractionResult.FAIL);
        }

        /**
         * Cancels this event, preventing {@link Item#useOn(UseOnContext)} from being called,
         * but allowing the interaction to continue to block-less use ({@link Item#use(Level, Player, InteractionHand)}).
         *
         * @see InteractionResult#PASS
         */
        public void pass() {
            setInteractionResult(InteractionResult.PASS);
        }

        /**
         * Cancel this event. If the {@link #setInteractionResult result} is not changed, it will default to
         * {@link InteractionResult#PASS}, preventing only {@link Item#useOn(UseOnContext)} from being called.
         *
         * @param canceled whether this event should be cancelled
         * @deprecated Use {@link #setInteractionResult(InteractionResult)} or similar methods instead, to set the result too.
         */
        @Override
        @Deprecated
        public void setCanceled(boolean canceled) {
            if (canceled && interactionResult != null) {
                interactionResult = InteractionResult.PASS;
            }
            super.setCanceled(canceled);
        }
    }

}
