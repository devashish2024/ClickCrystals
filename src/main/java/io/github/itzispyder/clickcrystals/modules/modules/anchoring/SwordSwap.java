package io.github.itzispyder.clickcrystals.modules.modules.anchoring;

import io.github.itzispyder.clickcrystals.events.EventHandler;
import io.github.itzispyder.clickcrystals.events.Listener;
import io.github.itzispyder.clickcrystals.events.events.PlayerAttackEntityEvent;
import io.github.itzispyder.clickcrystals.modules.Categories;
import io.github.itzispyder.clickcrystals.modules.Module;
import io.github.itzispyder.clickcrystals.util.HotbarUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;

public class SwordSwap extends Module implements Listener {

    public SwordSwap() {
        super("SwordSwap", Categories.ANCHORING, "Switch to sword after hitting a shielding opponent with an axe.");
    }

    @Override
    protected void onEnable() {
        system.addListener(this);
    }

    @Override
    protected void onDisable() {
        system.removeListener(this);
    }

    @EventHandler
    private void onAttack(PlayerAttackEntityEvent e) {
        if (e.getEntity() instanceof PlayerEntity p && !p.isBlocking() && isHoldingShield(p)) {
            if (HotbarUtils.nameContains("axe") && HotbarUtils.has(item -> item.getItem() instanceof SwordItem)) {
                HotbarUtils.search(item -> item.getItem() instanceof SwordItem);
            }
        }
    }

    private boolean isHoldingShield(PlayerEntity p) {
        boolean mainhand = p.getMainHandStack() != null && p.getMainHandStack().getItem() instanceof ShieldItem;
        boolean offhand = p.getOffHandStack() != null && p.getOffHandStack().getItem() instanceof ShieldItem;
        return mainhand || offhand;
    }
}
