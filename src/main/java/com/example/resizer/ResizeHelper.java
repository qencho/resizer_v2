package com.example.resizer;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeMod;

public final class ResizeHelper {
    public static final double[] STEPS = {0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0};

    private static final UUID HEALTH = UUID.fromString("5c1f0a10-0001-4a5e-8b11-7e5a1e000001");
    private static final UUID DAMAGE = UUID.fromString("5c1f0a10-0002-4a5e-8b11-7e5a1e000002");
    private static final UUID SPEED = UUID.fromString("5c1f0a10-0003-4a5e-8b11-7e5a1e000003");
    private static final UUID STEP = UUID.fromString("5c1f0a10-0004-4a5e-8b11-7e5a1e000004");
    private static final UUID BLOCK_REACH = UUID.fromString("5c1f0a10-0005-4a5e-8b11-7e5a1e000005");
    private static final UUID ENTITY_REACH = UUID.fromString("5c1f0a10-0006-4a5e-8b11-7e5a1e000006");

    private ResizeHelper() {}

    public static double scale(LivingEntity e) {
        AttributeInstance inst = e.getAttribute(ResizerMod.SIZE.get());
        return inst == null ? 1.0 : inst.getValue();
    }

    /** Радиус куба добычи вокруг ломаемого блока (0 = обычная добыча). x2=1, x3=2, x4=3, x6=4, x8=5. */
    public static int miningRadius(double f) {
        if (f < 2.0 - 1e-6) return 0;
        if (f >= 8.0 - 1e-6) return 5;
        if (f >= 6.0 - 1e-6) return 4;
        return (int) Math.floor(f + 1e-6) - 1;
    }

    /** Вызывается на сервере: игрок user меняет размер цели target (Shift = уменьшить). */
    public static void useOn(Player user, LivingEntity target) {
        if (user.getCooldowns().isOnCooldown(ResizerMod.RESIZER.get())) return;
        boolean grow = !user.isShiftKeyDown();
        if (step(target, grow)) {
            user.displayClientMessage(Component.literal("Размер: x" + scale(target)), true);
            user.getCooldowns().addCooldown(ResizerMod.RESIZER.get(), 10);
        } else {
            user.displayClientMessage(
                    Component.literal(grow ? "Максимальный размер" : "Минимальный размер"), true);
        }
    }

    public static boolean step(LivingEntity e, boolean grow) {
        if (e.getAttribute(ResizerMod.SIZE.get()) == null) return false;
        double cur = scale(e);
        int idx = 0;
        for (int i = 0; i < STEPS.length; i++) {
            if (Math.abs(STEPS[i] - cur) < Math.abs(STEPS[idx] - cur)) idx = i;
        }
        int ni = Math.max(0, Math.min(STEPS.length - 1, idx + (grow ? 1 : -1)));
        if (ni == idx) return false;
        apply(e, STEPS[ni]);
        return true;
    }

    public static void apply(LivingEntity e, double f) {
        AttributeInstance size = e.getAttribute(ResizerMod.SIZE.get());
        if (size == null) return;
        double old = size.getValue();
        float oldHealth = e.getHealth();
        size.setBaseValue(f);

        double sq = Math.sqrt(f);
        mod(e, Attributes.MAX_HEALTH, HEALTH, f - 1, Operation.MULTIPLY_TOTAL);
        mod(e, Attributes.ATTACK_DAMAGE, DAMAGE, f - 1, Operation.MULTIPLY_TOTAL);
        mod(e, Attributes.MOVEMENT_SPEED, SPEED, sq - 1, Operation.MULTIPLY_TOTAL);
        mod(e, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP, (f - 1) * 0.6, Operation.ADDITION);
        mod(e, ForgeMod.BLOCK_REACH.get(), BLOCK_REACH, (f - 1) * 4.5, Operation.ADDITION);
        mod(e, ForgeMod.ENTITY_REACH.get(), ENTITY_REACH, (f - 1) * 3.0, Operation.ADDITION);

        e.setHealth(Math.min(e.getMaxHealth(), (float) (oldHealth * f / old)));
    }

    private static void mod(LivingEntity e, Attribute attr, UUID id, double value, Operation op) {
        AttributeInstance inst = e.getAttribute(attr);
        if (inst == null) return; // у этого существа нет такого атрибута
        inst.removeModifier(id);
        if (Math.abs(value) > 1e-9) {
            inst.addPermanentModifier(new AttributeModifier(id, "resizer", value, op));
        }
    }

    /** Мгновенно убирает блок (без выпадения предметов, но содержимое сундуков и т.п. выпадает). */
    public static void smash(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.isAir() || s.getBlock() instanceof LiquidBlock) return;
        if (s.getDestroySpeed(level, pos) < 0) return; // коренная порода и т.п.
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof Container c) Containers.dropContents(level, pos, c);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
