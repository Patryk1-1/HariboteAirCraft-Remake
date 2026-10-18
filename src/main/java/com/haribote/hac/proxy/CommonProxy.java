package com.haribote.hac.proxy;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {
    public void preInit(FMLPreInitializationEvent event) {}
    public void init(FMLInitializationEvent event) {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.haribote.hac.entity.CollisionHandler());
    }
    public void postInit(FMLPostInitializationEvent event) {}
    public void registerItemRenderer(Item item, int meta, String id) {}
}
