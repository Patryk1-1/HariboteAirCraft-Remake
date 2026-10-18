package com.haribote.hac.proxy;

import com.haribote.hac.HACMod;
import com.haribote.hac.client.ClientInputHandler;
import com.haribote.hac.client.KeyBindings;
import com.haribote.hac.client.RenderHacAircraft;
import com.haribote.hac.entity.EntityHacAircraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        RenderingRegistry.registerEntityRenderingHandler(EntityHacAircraft.class, RenderHacAircraft::new);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        KeyBindings.init();
        MinecraftForge.EVENT_BUS.register(new ClientInputHandler());
        MinecraftForge.EVENT_BUS.register(new com.haribote.hac.client.ClientMountHelper());
        net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new com.haribote.hac.command.CommandHariboteAirCraft());
    }

    @Override
    public void registerItemRenderer(Item item, int meta, String id) {
        ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(HACMod.MODID + ":" + id, "inventory"));
    }
}
