package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public interface CrossbowAttackMob extends RangedAttackMob {
    void setChargingCrossbow(boolean p_32339_);

    @Nullable
    LivingEntity getTarget();

    void onCrossbowAttackPerformed();

    default void performCrossbowAttack(LivingEntity p_32337_, float p_32338_) {
        InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(p_32337_, item -> item instanceof CrossbowItem);
        ItemStack itemstack = p_32337_.getItemInHand(interactionhand);
        if (p_32337_.isHolding(is -> is.getItem() instanceof CrossbowItem)) {
            var crossbowitem = (CrossbowItem) itemstack.getItem();
            crossbowitem.performShooting(
                p_32337_.level(), p_32337_, interactionhand, itemstack, p_32338_, 14 - p_32337_.level().getDifficulty().getId() * 4, this.getTarget()
            );
        }

        this.onCrossbowAttackPerformed();
    }
}
