package com.magia;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public class MagiaClient implements ClientModInitializer {
    public static int mana = 100;
    public static int maxMana = 100;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MagiaMod.MANA_PACKET, (client, handler, buf, sender) -> {
            int m = buf.readInt();
            int mx = buf.readInt();
            client.execute(() -> {
                mana = m;
                maxMana = mx;
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
        });
    }
}
