package com.magia;

import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class StaffItem extends Item {
    public static final float FIREBALL_COST = 20f;
    public static final int FIREBALL_COOLDOWN = 15; // ticks

    public StaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }

        ServerPlayerEntity player = (ServerPlayerEntity) user;

        if (!ManaManager.spend(player, FIREBALL_COST)) {
            player.sendMessage(Text.literal("Mana insuficiente!"), true);
            return TypedActionResult.fail(stack);
        }

        Vec3d look = user.getRotationVec(1.0f);
        FireballEntity fireball = new FireballEntity(world, user, look.x, look.y, look.z, 1);
        fireball.setPosition(user.getX() + look.x, user.getEyeY() - 0.1 + look.y, user.getZ() + look.z);
        world.spawnEntity(fireball);

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        user.getItemCooldownManager().set(this, FIREBALL_COOLDOWN);

        return TypedActionResult.success(stack, false);
    }
}
