package com.magia;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class StaffItem extends Item {

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
        Spell spell = ManaManager.getSpell(player.getUuid());

        if (!ManaManager.isReady(player, spell)) {
            player.sendMessage(Text.literal("Recarregando..."), true);
            return TypedActionResult.fail(stack);
        }
        if (ManaManager.get(player.getUuid()) < spell.custo) {
            player.sendMessage(Text.literal("Mana insuficiente!"), true);
            return TypedActionResult.fail(stack);
        }

        boolean lancou = switch (spell) {
            case BOLA_DE_FOGO -> lancarBolaDeFogo(world, player);
            case RAIO -> lancarRaio(world, player);
            case CURA -> lancarCura(world, player);
        };

        if (!lancou) {
            return TypedActionResult.fail(stack);
        }

        ManaManager.spend(player, spell.custo);
        ManaManager.startCooldown(player, spell);
        return TypedActionResult.success(stack, false);
    }

    private boolean lancarBolaDeFogo(World world, ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f);
        FireballEntity fireball = new FireballEntity(world, player, look.x, look.y, look.z, 1);
        fireball.setPosition(player.getX() + look.x, player.getEyeY() - 0.1 + look.y, player.getZ() + look.z);
        world.spawnEntity(fireball);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 1.0f);
        return true;
    }

    private boolean lancarRaio(World world, ServerPlayerEntity player) {
        HitResult hit = player.raycast(30.0, 1.0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            player.sendMessage(Text.literal("Sem alvo!"), true);
            return false;
        }
        Vec3d pos = hit.getPos();
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null) {
            return false;
        }
        bolt.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
        bolt.setChanneler(player);
        world.spawnEntity(bolt);
        return true;
    }

    private boolean lancarCura(World world, ServerPlayerEntity player) {
        if (player.getHealth() >= player.getMaxHealth()) {
            player.sendMessage(Text.literal("Vida cheia!"), true);
            return false;
        }
        player.heal(6.0f);
        ((ServerWorld) world).spawnParticles(ParticleTypes.HEART,
                player.getX(), player.getY() + 1.0, player.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.7f, 1.4f);
        return true;
    }
}
