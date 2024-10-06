package io.github.itzispyder.clickcrystals.modules.modules.rendering;

import io.github.itzispyder.clickcrystals.modules.Categories;
import io.github.itzispyder.clickcrystals.modules.DummyModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

public class ESP extends DummyModule {

    public ESP() {
        super("esp", Categories.RENDER, "Makes players glow client-side.");
    }

    @Override
    public void onEnable() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    @Override
    public void onDisable() {
        ClientTickEvents.END_CLIENT_TICK.unregister(this::onTick);
        removeGlowEffect();
    }

    private void onTick(MinecraftClient client) {
        if (client.world != null) {
            client.world.getPlayers().forEach(this::applyGlowEffect);
        }
    }

    private void applyGlowEffect(PlayerEntity player) {
        if (player != MinecraftClient.getInstance().player) {
            player.setGlowing(true);
        }
    }

    private void removeGlowEffect() {
        World world = MinecraftClient.getInstance().world;
        if (world != null) {
            world.getPlayers().forEach(player -> player.setGlowing(false));
        }
    }
}
