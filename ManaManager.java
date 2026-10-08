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

    public static void tick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID id = player.getUuid();
            float current = Math.min(MAX_MANA, get(id) + REGEN_PER_TICK);
            MANA.put(id, current);

            int shown = (int) current;
            Integer last = LAST_SENT.get(id);
            if (last == null || last != shown) {
                LAST_SENT.put(id, shown);
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(shown);
                buf.writeInt((int) MAX_MANA);
                ServerPlayNetworking.send(player, MagiaMod.MANA_PACKET, buf);
            }
        }
    }

    public static void forgetSync(UUID id) {
        LAST_SENT.remove(id);
    }
}
