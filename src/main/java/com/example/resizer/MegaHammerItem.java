package com.example.resizer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Мега-молот: ПКМ по цели, нужен размер x4 или больше. Сносит шар блоков (дома, деревни). */
public class MegaHammerItem extends Item {
    public static final double MIN_SCALE = 4.0;
    public static final double RANGE = 64.0;

    public MegaHammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(level instanceof ServerLevel sl)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.success(stack);
        }
        double f = ResizeHelper.scale(player);
        if (f < MIN_SCALE - 1e-6) {
            player.displayClientMessage(Component.literal("Нужен размер x4 или больше"), true);
            return InteractionResultHolder.success(stack);
        }
        HitResult hit = player.pick(RANGE, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) {
            player.displayClientMessage(Component.literal("Цель слишком далеко"), true);
            return InteractionResultHolder.success(stack);
        }

        BlockPos c = ((BlockHitResult) hit).getBlockPos();
        int r = (int) f + 2; // x4 -> 6, x6 -> 8, x8 -> 10
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r * r) continue;
                    ResizeHelper.smash(sl, c.offset(dx, dy, dz));
                }
            }
        }
        sl.explode(player, c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 6.0F,
                Level.ExplosionInteraction.NONE);
        player.getCooldowns().addCooldown(this, 60);
        player.displayClientMessage(Component.literal("Мега-удар!"), true);
        return InteractionResultHolder.success(stack);
    }
}
