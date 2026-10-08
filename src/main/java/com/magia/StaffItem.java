package com.magia;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

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

        boolean lancou = lancar(spell, world, player);
        if (!lancou) {
            return TypedActionResult.fail(stack);
        }

        ManaManager.spend(player, spell.custo);
        ManaManager.startCooldown(player, spell);
        return TypedActionResult.success(stack, false);
    }

    private boolean lancar(Spell spell, World world, ServerPlayerEntity p) {
        return switch (spell) {
            case BOLA_DE_FOGO -> bolaDeFogo(world, p);
            case NOVA_DE_FOGO -> novaDeFogo(world, p);
            case CONGELAR -> congelar(world, p);
            case NEVASCA -> nevasca(world, p);
            case RAIO -> raio(world, p);
            case DESCARGA -> descarga(world, p);
            case RAJADA -> rajada(world, p);
            case IMPULSO -> impulso(world, p);
            case PELE_DE_PEDRA -> peleDePedra(world, p);
            case TERREMOTO -> terremoto(world, p);
            case CURA -> cura(world, p);
            case ESCUDO_DE_LUZ -> escudoDeLuz(world, p);
        };
    }

    // ---------------- FOGO ----------------

    private boolean bolaDeFogo(World world, ServerPlayerEntity p) {
        Vec3d look = p.getRotationVec(1.0f);
        FireballEntity fireball = new FireballEntity(world, p, look.x, look.y, look.z, 1);
        fireball.setPosition(p.getX() + look.x, p.getEyeY() - 0.1 + look.y, p.getZ() + look.z);
        world.spawnEntity(fireball);
        som(world, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.0f);
        return true;
    }

    private boolean novaDeFogo(World world, ServerPlayerEntity p) {
        List<LivingEntity> alvos = vivosPerto(world, p, 6.0);
        if (alvos.isEmpty()) {
            return semAlvo(p);
        }
        for (LivingEntity e : alvos) {
            e.setOnFireFor(5);
            e.damage(world.getDamageSources().playerAttack(p), 4.0f);
        }
        particulas(world, ParticleTypes.FLAME, p.getX(), p.getY() + 1.0, p.getZ(), 80, 2.5, 0.6);
        som(world, p, SoundEvents.ITEM_FIRECHARGE_USE, 0.8f);
        return true;
    }

    // ---------------- GELO ----------------

    private boolean congelar(World world, ServerPlayerEntity p) {
        LivingEntity alvo = alvoNaMira(p, 20.0);
        if (alvo == null) {
            return semAlvo(p);
        }
        alvo.damage(world.getDamageSources().playerAttack(p), 4.0f);
        alvo.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 3));
        alvo.setFrozenTicks(200);
        particulas(world, ParticleTypes.SNOWFLAKE, alvo.getX(), alvo.getY() + 1.0, alvo.getZ(), 40, 0.5, 0.8);
        som(world, p, SoundEvents.BLOCK_GLASS_BREAK, 1.2f);
        return true;
    }

    private boolean nevasca(World world, ServerPlayerEntity p) {
        List<LivingEntity> alvos = vivosPerto(world, p, 8.0);
        if (alvos.isEmpty()) {
            return semAlvo(p);
        }
        for (LivingEntity e : alvos) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 2));
            e.setFrozenTicks(140);
            e.damage(world.getDamageSources().playerAttack(p), 2.0f);
        }
        particulas(world, ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.5, p.getZ(), 150, 4.0, 1.0);
        som(world, p, SoundEvents.BLOCK_GLASS_BREAK, 0.7f);
        return true;
    }

    // ---------------- RAIO ----------------

    private boolean raio(World world, ServerPlayerEntity p) {
        HitResult hit = p.raycast(30.0, 1.0f, false);
        if (hit.getType() == HitResult.Type.MISS) {
            return semAlvo(p);
        }
        Vec3d pos = hit.getPos();
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null) {
            return false;
        }
        bolt.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
        bolt.setChanneler(p);
        world.spawnEntity(bolt);
        return true;
    }

    private boolean descarga(World world, ServerPlayerEntity p) {
        List<LivingEntity> alvos = vivosPerto(world, p, 5.0);
        if (alvos.isEmpty()) {
            return semAlvo(p);
        }
        for (LivingEntity e : alvos) {
            e.damage(world.getDamageSources().playerAttack(p), 6.0f);
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0));
        }
        particulas(world, ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1.0, p.getZ(), 100, 2.5, 0.8);
        som(world, p, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 1.2f);
        return true;
    }

    // ---------------- VENTO ----------------

    private boolean rajada(World world, ServerPlayerEntity p) {
        Vec3d dir = p.getRotationVec(1.0f);
        boolean empurrou = false;
        for (LivingEntity e : vivosPerto(world, p, 8.0)) {
            Vec3d para = e.getPos().subtract(p.getPos()).normalize();
            if (para.dotProduct(dir) > 0.5) {
                empurrar(e, dir.x * 2.0, 0.5, dir.z * 2.0);
                empurrou = true;
            }
        }
        if (!empurrou) {
            return semAlvo(p);
        }
        Vec3d frente = p.getPos().add(dir.multiply(3.0));
        particulas(world, ParticleTypes.CLOUD, frente.x, p.getEyeY(), frente.z, 40, 1.0, 0.5);
        som(world, p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f);
        return true;
    }

    private boolean impulso(World world, ServerPlayerEntity p) {
        Vec3d dir = p.getRotationVec(1.0f);
        empurrar(p, dir.x * 1.8, 0.6, dir.z * 1.8);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 80, 0));
        particulas(world, ParticleTypes.CLOUD, p.getX(), p.getY() + 0.5, p.getZ(), 30, 0.4, 0.2);
        som(world, p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.4f);
        return true;
    }

    // ---------------- TERRA ----------------

    private boolean peleDePedra(World world, ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
        particulas(world, ParticleTypes.CLOUD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.5, 0.8);
        som(world, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.6f);
        return true;
    }

    private boolean terremoto(World world, ServerPlayerEntity p) {
        List<LivingEntity> alvos = vivosPerto(world, p, 6.0);
        if (alvos.isEmpty()) {
            return semAlvo(p);
        }
        for (LivingEntity e : alvos) {
            e.damage(world.getDamageSources().playerAttack(p), 5.0f);
            empurrar(e, 0.0, 0.7, 0.0);
        }
        particulas(world, ParticleTypes.CLOUD, p.getX(), p.getY() + 0.2, p.getZ(), 120, 3.0, 0.2);
        som(world, p, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.7f);
        return true;
    }

    // ---------------- LUZ ----------------

    private boolean cura(World world, ServerPlayerEntity p) {
        if (p.getHealth() >= p.getMaxHealth()) {
            p.sendMessage(Text.literal("Vida cheia!"), true);
            return false;
        }
        p.heal(6.0f);
        particulas(world, ParticleTypes.HEART, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.4, 0.4);
        som(world, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.4f);
        return true;
    }

    private boolean escudoDeLuz(World world, ServerPlayerEntity p) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 1));
        particulas(world, ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.5, 0.8);
        som(world, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.4f);
        return true;
    }

    // ---------------- ajudantes ----------------

    private static boolean semAlvo(ServerPlayerEntity p) {
        p.sendMessage(Text.literal("Sem alvo!"), true);
        return false;
    }

    private static void som(World world, ServerPlayerEntity p, SoundEvent s, float pitch) {
        world.playSound(null, p.getBlockPos(), s, SoundCategory.PLAYERS, 1.0f, pitch);
    }

    private static void particulas(World world, ParticleEffect tipo, double x, double y, double z,
                                   int quantidade, double espalhoXZ, double espalhoY) {
        ((ServerWorld) world).spawnParticles(tipo, x, y, z, quantidade, espalhoXZ, espalhoY, espalhoXZ, 0.05);
    }

    private static List<LivingEntity> vivosPerto(World world, ServerPlayerEntity p, double raio) {
        List<LivingEntity> lista = new ArrayList<>();
        for (Entity e : world.getOtherEntities(p, p.getBoundingBox().expand(raio),
                x -> x instanceof LivingEntity && x.isAlive())) {
            lista.add((LivingEntity) e);
        }
        return lista;
    }

    private static LivingEntity alvoNaMira(ServerPlayerEntity p, double alcance) {
        Vec3d inicio = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d fim = inicio.add(dir.multiply(alcance));
        Box caixa = p.getBoundingBox().stretch(dir.multiply(alcance)).expand(1.0);
        EntityHitResult r = ProjectileUtil.raycast(p, inicio, fim, caixa,
                e -> e instanceof LivingEntity && e != p && e.isAlive() && !e.isSpectator(),
                alcance * alcance);
        return r == null ? null : (LivingEntity) r.getEntity();
    }

    private static void empurrar(LivingEntity e, double x, double y, double z) {
        e.addVelocity(x, y, z);
        e.velocityModified = true;
        if (e instanceof ServerPlayerEntity sp) {
            sp.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(sp));
        }
    }
}
