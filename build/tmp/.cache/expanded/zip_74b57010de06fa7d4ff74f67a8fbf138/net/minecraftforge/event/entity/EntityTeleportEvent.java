/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.event.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.bus.CancellableEventBus;
import net.minecraftforge.eventbus.api.event.characteristic.Cancellable;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * EntityTeleportEvent is fired when an event involving any teleportation of an Entity occurs.<br>
 * If a method utilizes this {@link Event} as its parameter, the method will
 * receive every child event of this class.<br>
 * <br>
 * {@link #getTarget()} contains the target destination.<br>
 * {@link #getPrev()} contains the entity's current position.<br>
 * <br>
 * All children of this event are fired on the {@link MinecraftForge#EVENT_BUS}.<br>
 **/
public sealed class EntityTeleportEvent extends EntityEvent implements Cancellable {
    public static final CancellableEventBus<EntityTeleportEvent> BUS = CancellableEventBus.create(EntityTeleportEvent.class);

    protected double targetX;
    protected double targetY;
    protected double targetZ;

    public EntityTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
        super(entity);
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    public double getTargetX() { return targetX; }
    public void setTargetX(double targetX) { this.targetX = targetX; }
    public double getTargetY() { return targetY; }
    public void setTargetY(double targetY) { this.targetY = targetY; }
    public double getTargetZ() { return targetZ; }
    public void setTargetZ(double targetZ) { this.targetZ = targetZ; }
    public Vec3 getTarget() { return new Vec3(this.targetX, this.targetY, this.targetZ); }
    public double getPrevX() { return getEntity().getX(); }
    public double getPrevY() { return getEntity().getY(); }
    public double getPrevZ() { return getEntity().getZ(); }
    public Vec3 getPrev() { return getEntity().position(); }

    /**
     * EntityTeleportEvent.TeleportCommand is fired before a living entity is teleported
     * from use of {@link net.minecraft.server.commands.TeleportCommand}.
     * <br>
     * This event is {@link Cancelable}.<br>
     * If the event is not canceled, the entity will be teleported.
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     * <br>
     * This event is only fired on the {@link LogicalSide#SERVER} side.<br>
     * <br>
     * If this event is canceled, the entity will not be teleported.
     */
    public static final class TeleportCommand extends EntityTeleportEvent {
        public static final CancellableEventBus<TeleportCommand> BUS = CancellableEventBus.create(TeleportCommand.class);

        public TeleportCommand(Entity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
        }
    }

    /**
     * EntityTeleportEvent.SpreadPlayersCommand is fired before a living entity is teleported
     * from use of {@link net.minecraft.server.commands.SpreadPlayersCommand}.
     * <br>
     * This event is {@link Cancelable}.<br>
     * If the event is not canceled, the entity will be teleported.
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     * <br>
     * This event is only fired on the {@link LogicalSide#SERVER} side.<br>
     * <br>
     * If this event is canceled, the entity will not be teleported.
     */
    public static final class SpreadPlayersCommand extends EntityTeleportEvent {
        public static final CancellableEventBus<SpreadPlayersCommand> BUS = CancellableEventBus.create(SpreadPlayersCommand.class);

        public SpreadPlayersCommand(Entity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
        }
    }

    /**
     * EntityTeleportEvent.EnderEntity is fired before an Enderman or Shulker randomly teleports.
     * <br>
     * This event is {@link Cancelable}.<br>
     * If the event is not canceled, the entity will be teleported.
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     * <br>
     * This event is only fired on the {@link LogicalSide#SERVER} side.<br>
     * <br>
     * If this event is canceled, the entity will not be teleported.
     */
    public static final class EnderEntity extends EntityTeleportEvent {
        public static final CancellableEventBus<EnderEntity> BUS = CancellableEventBus.create(EnderEntity.class);

        private final LivingEntity entityLiving;

        public EnderEntity(LivingEntity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
            this.entityLiving = entity;
        }

        public LivingEntity getEntityLiving() {
            return entityLiving;
        }
    }

    /**
     * EntityTeleportEvent.EnderPearl is fired before an Entity is teleported from an EnderPearlEntity.
     * <br>
     * This event is {@link Cancelable}.<br>
     * If the event is not canceled, the entity will be teleported.
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     * <br>
     * This event is only fired on the {@link LogicalSide#SERVER} side.<br>
     * <br>
     * If this event is canceled, the entity will not be teleported.
     */
    public static final class EnderPearl extends EntityTeleportEvent {
        public static final CancellableEventBus<EnderPearl> BUS = CancellableEventBus.create(EnderPearl.class);

        private final ServerPlayer player;
        private final ThrownEnderpearl pearlEntity;
        private float attackDamage;
        private final HitResult hitResult;

        @ApiStatus.Internal
        public EnderPearl(ServerPlayer entity, double targetX, double targetY, double targetZ, ThrownEnderpearl pearlEntity, float attackDamage, HitResult hitResult) {
            super(entity, targetX, targetY, targetZ);
            this.pearlEntity = pearlEntity;
            this.player = entity;
            this.attackDamage = attackDamage;
            this.hitResult = hitResult;
        }

        public ThrownEnderpearl getPearlEntity() {
            return pearlEntity;
        }

        public ServerPlayer getPlayer() {
            return player;
        }

        @Nullable
        public HitResult getHitResult() {
            return this.hitResult;
        }

        public float getAttackDamage() {
            return attackDamage;
        }

        public void setAttackDamage(float attackDamage) {
            this.attackDamage = attackDamage;
        }
    }

    /**
     * EntityTeleportEvent.ChorusFruit is fired before a LivingEntity is teleported due to consuming Chorus Fruit.
     * <br>
     * This event is {@link Cancelable}.<br>
     * If the event is not canceled, the entity will be teleported.
     * <br>
     * This event does not have a result. {@link HasResult}<br>
     * <br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     * <br>
     * This event is only fired on the {@link LogicalSide#SERVER} side.<br>
     * <br>
     * If this event is canceled, the entity will not be teleported.
     */
    public static final class ChorusFruit extends EntityTeleportEvent {
        public static final CancellableEventBus<ChorusFruit> BUS = CancellableEventBus.create(ChorusFruit.class);

        private final LivingEntity entityLiving;

        public ChorusFruit(LivingEntity entity, double targetX, double targetY, double targetZ) {
            super(entity, targetX, targetY, targetZ);
            this.entityLiving = entity;
        }

        public LivingEntity getEntityLiving() {
            return entityLiving;
        }
    }
}
