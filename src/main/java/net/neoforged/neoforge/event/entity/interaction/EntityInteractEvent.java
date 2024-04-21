package net.neoforged.neoforge.event.entity.interaction;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The event fired when a player interacts with an entity, before {@link Entity#interactAt(Player, Vec3, InteractionHand)}
 * (and subsequently, {@link Entity#interact(Player, InteractionHand)}) is called.
 * <p>
 * Cancelling this event will prevent any subsequent interactions (such as {@link Item#use(Level, Player, InteractionHand)}),
 * unless cancelled with {@link InteractionResult#PASS}, which will only prevent {@linkplain Entity#interactAt(Player, Vec3, InteractionHand) Entity#interactAt} and {@linkplain Entity#interact(Player, InteractionHand) Entity#interact}
 * from being called.
 */
public class EntityInteractEvent extends Event implements ICancellableEvent {
    @Nullable
    private InteractionResult interactionResult;

    private final Player player;
    private final Entity entity;
    private final InteractionHand hand;
    private final Vec3 hitVec;

    @ApiStatus.Internal
    public EntityInteractEvent(Player player, Entity entity, InteractionHand hand, Vec3 hitVec) {
        this.player = player;
        this.entity = entity;
        this.hand = hand;
        this.hitVec = hitVec;
    }

    /**
     * {@return the level this interaction occurred in}
     */
    public Level getLevel() {
        return player.level();
    }

    /**
     * {@return the player that interacted with the entity}
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * {@return the entity that was interacted with}
     */
    public Entity getInteractionEntity() {
        return entity;
    }

    /**
     * {@return the hand the player used to interact}
     */
    public InteractionHand getHand() {
        return hand;
    }

    /**
     * {@return the local interaction position}
     * This is a 3D vector, where (0, 0, 0) is centered exactly at the
     * center of the entity's bounding box at their feet. This means the X and Z values will be in the range
     * [-width / 2, width / 2] while Y values will be in the range [0, height]
     */
    public Vec3 getHitVec() {
        return hitVec;
    }

    /**
     * {@return the interaction result}
     */
    @Nullable
    public InteractionResult getInteractionResult() {
        return interactionResult;
    }

    /**
     * Sets the interaction result of this event.
     * <p>
     * A non-null result will also {@link #setCanceled(boolean) cancel} this event, and a {@code null} one will un-cancel it.
     *
     * @param result the result
     */
    public void setInteractionResult(@javax.annotation.Nullable InteractionResult result) {
        this.interactionResult = result;
        setCanceled(result != null);
    }

    /**
     * Cancel this interaction, as a success. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#PLAYER_INTERACTED_WITH_ENTITY player interacted with entity trigger} will be invoked,
     * and a client-side swing will be triggered.
     *
     * @see InteractionResult#sidedSuccess(boolean)
     */
    public void cancelWithSwing() {
        setInteractionResult(InteractionResult.sidedSuccess(getLevel().isClientSide));
    }

    /**
     * Cancel and consume this interaction. Any subsequent interactions will be aborted, but the {@link net.minecraft.advancements.CriteriaTriggers#PLAYER_INTERACTED_WITH_ENTITY player interacted with entity trigger} will be invoked.
     *
     * @see InteractionResult#CONSUME
     */
    public void consume() {
        setInteractionResult(InteractionResult.CONSUME);
    }

    /**
     * Cancel this event, and abort any subsequent interactions.
     */
    public void fail() {
        setInteractionResult(InteractionResult.FAIL);
    }

    /**
     * Cancel this event, preventing {@linkplain Entity#interactAt(Player, Vec3, InteractionHand) Entity#interactAt} and {@linkplain Entity#interact(Player, InteractionHand) Entity#interact}
     * from being called, but allowing the rest of the pipeline ({@link Item#use(Level, Player, InteractionHand) Item#use}) to continue.
     */
    public void pass() {
        setInteractionResult(InteractionResult.PASS);
    }

    /**
     * Cancel this event. If the {@link #setInteractionResult result} is not changed, it will default to
     * {@link InteractionResult#PASS}, preventing only {@linkplain Entity#interactAt(Player, Vec3, InteractionHand) Entity#interactAt} and {@linkplain Entity#interact(Player, InteractionHand) Entity#interact} from being called.
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
        ICancellableEvent.super.setCanceled(canceled);
    }
}
