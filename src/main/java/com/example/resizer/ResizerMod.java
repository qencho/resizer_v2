package com.example.resizer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(ResizerMod.MOD_ID)
public class ResizerMod {
    public static final String MOD_ID = "resizer";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, MOD_ID);

    public static final RegistryObject<Item> RESIZER =
            ITEMS.register("resizer", () -> new ResizerItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MEGA_HAMMER =
            ITEMS.register("mega_hammer", () -> new MegaHammerItem(new Item.Properties().stacksTo(1)));

    /** Собственный атрибут размера. Синхронизируется с клиентом и сохраняется в NBT существа. */
    public static final RegistryObject<Attribute> SIZE =
            ATTRIBUTES.register("size",
                    () -> new RangedAttribute("attribute.name.resizer.size", 1.0, 0.1, 16.0).setSyncable(true));

    public ResizerMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        ATTRIBUTES.register(bus);
        bus.addListener(this::addAttributes);
        bus.addListener(this::addToTab);
    }

    private void addAttributes(EntityAttributeModificationEvent event) {
        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            event.add(type, SIZE.get());
        }
    }

    private void addToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RESIZER);
            event.accept(MEGA_HAMMER);
        }
    }
}
