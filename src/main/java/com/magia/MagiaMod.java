package com.magia;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class MagiaMod implements ModInitializer {
    public static final String MOD_ID = "magiamod";
    public static final Identifier MANA_PACKET = new Identifier(MOD_ID, "mana_sync");
    public static final Identifier CYCLE_PACKET = new Identifier(MOD_ID, "trocar_magia");

    public static final Item CAJADO = Registry.register(
            Registries.ITEM,
            new Identifier(MOD_ID, "cajado_magico"),
            new StaffItem(new FabricItemSettings().maxCount(1)));

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(CAJADO));
        ServerTickEvents.END_SERVER_TICK.register(ManaManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ManaManager.forgetSync(handler.getPlayer().getUuid()));
        ServerPlayNetworking.registerGlobalReceiver(CYCLE_PACKET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> ManaManager.cycleSpell(player)));
    }
}
