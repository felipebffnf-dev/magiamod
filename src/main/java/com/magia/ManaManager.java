package com.magia;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager {
    public static final float MAX_MANA = 100f;
    public static final float REGEN_PER_TICK = 0.1f; // 2 de mana por segundo

    private static final Map<UUID, Float> MANA = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SENT = new HashMap<>();
    private static final Map<UUID, Spell> SELECTED = new HashMap<>();
    private static final Map<UUID, Map<Spell, Long>> READY_AT = new HashMap<>();

    public static float get(UUID id) {
        return MANA.getOrDefault(id, MAX_MANA);
    }

    public static boolean spend(ServerPlayerEntity player, float cost) {
        float current = get(player.getUuid());
        if (current < cost) {
            return false;
        }
        MANA.put(player.getUuid(), current - cost);
        return true;
    }

    public static Spell getSpell(UUID id) {
        return SELECTED.getOrDefault(id, Spell.BOLA_DE_FOGO);
    }

    public static void cycleSpell(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        SELECTED.put(id, getSpell(id).proxima());
        LAST_SENT.remove(id); // força reenviar para o cliente
    }

    public static boolean isReady(ServerPlayerEntity player, Spell spell) {
        Map<Spell, Long> map = READY_AT.get(player.getUuid());
        if (map == null || !map.containsKey(spell)) {
            return true;
        }
        return player.getWorld().getTime() >= map.get(spell);
    }

    public static void startCooldown(ServerPlayerEntity player, Spell spell) {
        READY_AT.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                .put(spell, player.getWorld().getTime() + spell.cooldown);
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            float current = Math.min(MAX_MANA, get(id) + REGEN_PER_TICK);
            MANA.put(id, current);

            int shown = (int) current;
            Spell spell = getSpell(id);
            int key = shown * 10 + spell.ordinal();
            Integer last = LAST_SENT.get(id);
            if (last == null || last != key) {
                LAST_SENT.put(id, key);
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(shown);
                buf.writeInt((int) MAX_MANA);
                buf.writeInt(spell.ordinal());
                ServerPlayNetworking.send(player, MagiaMod.MANA_PACKET, buf);
            }
        }
    }

    public static void forgetSync(UUID id) {
        LAST_SENT.remove(id);
    }
}
