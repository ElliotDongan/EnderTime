package net.minecraft.world.entity.projectile;

import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class ProjectileUtil {
    public static final float DEFAULT_ENTITY_HIT_RESULT_MARGIN = 0.3F;

    public static HitResult getHitResultOnMoveVector(Entity p_278228_, Predicate<Entity> p_278315_) {
        Vec3 vec3 = p_278228_.getDeltaMovement();
        Level level = p_278228_.level();
        Vec3 vec31 = p_278228_.position();
        return getHitResult(vec31, p_278228_, p_278315_, vec3, level, computeMargin(p_278228_), ClipContext.Block.COLLIDER);
    }

    public static HitResult getHitResultOnMoveVector(Entity p_311718_, Predicate<Entity> p_311003_, ClipContext.Block p_312093_) {
        Vec3 vec3 = p_311718_.getDeltaMovement();
        Level level = p_311718_.level();
        Vec3 vec31 = p_311718_.position();
        return getHitResult(vec31, p_311718_, p_311003_, vec3, level, computeMargin(p_311718_), p_312093_);
    }

    public static HitResult getHitResultOnViewVector(Entity p_278281_, Predicate<Entity> p_278306_, double p_278293_) {
        Vec3 vec3 = p_278281_.getViewVector(0.0F).scale(p_278293_);
        Level level = p_278281_.level();
        Vec3 vec31 = p_278281_.getEyePosition();
        return getHitResult(vec31, p_278281_, p_278306_, vec3, level, 0.0F, ClipContext.Block.COLLIDER);
    }

    private static HitResult getHitResult(
        Vec3 p_278237_, Entity p_278320_, Predicate<Entity> p_278257_, Vec3 p_278342_, Level p_278321_, float p_310295_, ClipContext.Block p_310049_
    ) {
        Vec3 vec3 = p_278237_.add(p_278342_);
        HitResult hitresult = p_278321_.clipIncludingBorder(new ClipContext(p_278237_, vec3, p_310049_, ClipContext.Fluid.NONE, p_278320_));
        if (hitresult.getType() != HitResult.Type.MISS) {
            vec3 = hitresult.getLocation();
        }

        HitResult hitresult1 = getEntityHitResult(p_278321_, p_278320_, p_278237_, vec3, p_278320_.getBoundingBox().expandTowards(p_278342_).inflate(1.0), p_278257_, p_310295_);
        if (hitresult1 != null) {
            hitresult = hitresult1;
        }

        return hitresult;
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Entity p_37288_, Vec3 p_37289_, Vec3 p_37290_, AABB p_37291_, Predicate<Entity> p_37292_, double p_37293_) {
        Level level = p_37288_.level();
        double d0 = p_37293_;
        Entity entity = null;
        Vec3 vec3 = null;

        for (Entity entity1 : level.getEntities(p_37288_, p_37291_, p_37292_)) {
            AABB aabb = entity1.getBoundingBox().inflate(entity1.getPickRadius());
            Optional<Vec3> optional = aabb.clip(p_37289_, p_37290_);
            if (aabb.contains(p_37289_)) {
                if (d0 >= 0.0) {
                    entity = entity1;
                    vec3 = optional.orElse(p_37289_);
                    d0 = 0.0;
                }
            } else if (optional.isPresent()) {
                Vec3 vec31 = optional.get();
                double d1 = p_37289_.distanceToSqr(vec31);
                if (d1 < d0 || d0 == 0.0) {
                    if (entity1.getRootVehicle() == p_37288_.getRootVehicle() && !entity1.canRiderInteract()) {
                        if (d0 == 0.0) {
                            entity = entity1;
                            vec3 = vec31;
                        }
                    } else {
                        entity = entity1;
                        vec3 = vec31;
                        d0 = d1;
                    }
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, vec3);
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(Level p_37305_, Projectile p_408325_, Vec3 p_37307_, Vec3 p_37308_, AABB p_37309_, Predicate<Entity> p_37310_) {
        return getEntityHitResult(p_37305_, p_408325_, p_37307_, p_37308_, p_37309_, p_37310_, computeMargin(p_408325_));
    }

    public static float computeMargin(Entity p_407474_) {
        return Math.max(0.0F, Math.min(0.3F, (p_407474_.tickCount - 2) / 20.0F));
    }

    @Nullable
    public static EntityHitResult getEntityHitResult(
        Level p_150176_, Entity p_150177_, Vec3 p_150178_, Vec3 p_150179_, AABB p_150180_, Predicate<Entity> p_150181_, float p_150182_
    ) {
        double d0 = Double.MAX_VALUE;
        Optional<Vec3> optional = Optional.empty();
        Entity entity = null;

        for (Entity entity1 : p_150176_.getEntities(p_150177_, p_150180_, p_150181_)) {
            AABB aabb = entity1.getBoundingBox().inflate(p_150182_);
            Optional<Vec3> optional1 = aabb.clip(p_150178_, p_150179_);
            if (optional1.isPresent()) {
                double d1 = p_150178_.distanceToSqr(optional1.get());
                if (d1 < d0) {
                    entity = entity1;
                    d0 = d1;
                    optional = optional1;
                }
            }
        }

        return entity == null ? null : new EntityHitResult(entity, optional.get());
    }

    public static void rotateTowardsMovement(Entity p_37285_, float p_37286_) {
        Vec3 vec3 = p_37285_.getDeltaMovement();
        if (vec3.lengthSqr() != 0.0) {
            double d0 = vec3.horizontalDistance();
            p_37285_.setYRot((float)(Mth.atan2(vec3.z, vec3.x) * 180.0F / (float)Math.PI) + 90.0F);
            p_37285_.setXRot((float)(Mth.atan2(d0, vec3.y) * 180.0F / (float)Math.PI) - 90.0F);

            while (p_37285_.getXRot() - p_37285_.xRotO < -180.0F) {
                p_37285_.xRotO -= 360.0F;
            }

            while (p_37285_.getXRot() - p_37285_.xRotO >= 180.0F) {
                p_37285_.xRotO += 360.0F;
            }

            while (p_37285_.getYRot() - p_37285_.yRotO < -180.0F) {
                p_37285_.yRotO -= 360.0F;
            }

            while (p_37285_.getYRot() - p_37285_.yRotO >= 180.0F) {
                p_37285_.yRotO += 360.0F;
            }

            p_37285_.setXRot(Mth.lerp(p_37286_, p_37285_.xRotO, p_37285_.getXRot()));
            p_37285_.setYRot(Mth.lerp(p_37286_, p_37285_.yRotO, p_37285_.getYRot()));
        }
    }

    @Deprecated
    public static InteractionHand getWeaponHoldingHand(LivingEntity p_37298_, Item p_37299_) {
        return p_37298_.getMainHandItem().is(p_37299_) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static InteractionHand getWeaponHoldingHand(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
        return itemPredicate.test(livingEntity.getMainHandItem().getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static AbstractArrow getMobArrow(LivingEntity p_37301_, ItemStack p_37302_, float p_37303_, @Nullable ItemStack p_342402_) {
        ArrowItem arrowitem = (ArrowItem)(p_37302_.getItem() instanceof ArrowItem ? p_37302_.getItem() : Items.ARROW);
        AbstractArrow abstractarrow = arrowitem.createArrow(p_37301_.level(), p_37302_, p_37301_, p_342402_);
        abstractarrow.setBaseDamageFromMob(p_37303_);
        return abstractarrow;
    }
}
