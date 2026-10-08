package com.magia;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;

public class MagiaClient implements ClientModInitializer {
    public static int mana = 100;
    public static int maxMana = 100;
    public static int magiaAtual = 0;

    private static KeyBinding teclaMagia;
    private static KeyBinding teclaEscola;

    @Override
    public void onInitializeClient() {
        teclaMagia = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magiamod.trocar_magia",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.magiamod"));

        teclaEscola = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.magiamod.trocar_escola",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.magiamod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (teclaMagia.wasPressed()) {
                enviarTroca(false);
            }
            while (teclaEscola.wasPressed()) {
                enviarTroca(true);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(MagiaMod.MANA_PACKET, (client, handler, buf, sender) -> {
            int m = buf.readInt();
            int mx = buf.readInt();
            int magia = buf.readInt();
            client.execute(() -> {
                mana = m;
                maxMana = mx;
                magiaAtual = magia;
            });
        });

        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.options.hudHidden) {
                return;
            }

            int w = mc.getWindow().getScaledWidth();
            int h = mc.getWindow().getScaledHeight();

            int barWidth = 81;
            int x = w / 2 + 10;
            int y = h - 62;

            int filled = maxMana > 0 ? (int) ((barWidth - 2) * (mana / (float) maxMana)) : 0;

            ctx.fill(x, y, x + barWidth, y + 7, 0xFF000000);
            ctx.fill(x + 1, y + 1, x + 1 + filled, y + 6, 0xFF3A7BFF);
            ctx.drawText(mc.textRenderer, "Mana " + mana + "/" + maxMana, x, y - 10, 0xFF7FB0FF, true);

            Spell[] todas = Spell.values();
            Spell atual = todas[Math.min(magiaAtual, todas.length - 1)];
            ctx.drawText(mc.textRenderer,
                    atual.escola.nome + ": " + atual.nome + " (" + (int) atual.custo + ")",
                    x, y - 20, atual.escola.cor, true);
        });
    }

    private static void enviarTroca(boolean trocarEscola) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(trocarEscola);
        ClientPlayNetworking.send(MagiaMod.CYCLE_PACKET, buf);
    }
}
