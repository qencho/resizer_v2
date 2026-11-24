package com.example.resizer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ResizerMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onRenderPre(RenderLivingEvent.Pre<?, ?> e) {
        float f = (float) ResizeHelper.scale(e.getEntity());
        if (f == 1.0f) return;
        e.getPoseStack().pushPose();
        e.getPoseStack().scale(f, f, f);
    }

    @SubscribeEvent
    public static void onRenderPost(RenderLivingEvent.Post<?, ?> e) {
        float f = (float) ResizeHelper.scale(e.getEntity());
        if (f == 1.0f) return;
        e.getPoseStack().popPose();
    }
}
