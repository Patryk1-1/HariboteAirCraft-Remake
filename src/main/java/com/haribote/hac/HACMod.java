package com.haribote.hac;

import com.haribote.hac.block.BlockPyxis;
import com.haribote.hac.entity.EntityHacAircraft;
import com.haribote.hac.network.HacPacketHandler;
import com.haribote.hac.proxy.CommonProxy;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = HACMod.MODID, name = HACMod.NAME, version = HACMod.VERSION)
@Mod.EventBusSubscriber
public class HACMod {
    public static final String MODID = "hac";
    public static final String NAME = "HariboteAirCraft Remake";
    public static final String VERSION = "1.0.10";

    @Mod.Instance(MODID)
    public static HACMod instance;

    @SidedProxy(clientSide = "com.haribote.hac.proxy.ClientProxy", serverSide = "com.haribote.hac.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static BlockPyxis BLOCK_PYXIS;
    public static Item ITEM_PYXIS;

    public static final CreativeTabs TAB_HAC = new CreativeTabs("hac") {
        @Override
        @SideOnly(Side.CLIENT)
        public ItemStack createIcon() {
            return new ItemStack(ITEM_PYXIS);
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        HacPacketHandler.registerPackets();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    public void serverStarting(net.minecraftforge.fml.common.event.FMLServerStartingEvent event) {
        event.registerServerCommand(new com.haribote.hac.command.CommandHariboteAirCraft());
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        BLOCK_PYXIS = new BlockPyxis();
        event.getRegistry().register(BLOCK_PYXIS);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        ITEM_PYXIS = new ItemBlock(BLOCK_PYXIS).setRegistryName(BLOCK_PYXIS.getRegistryName());
        event.getRegistry().register(ITEM_PYXIS);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        EntityEntry aircraftEntry = EntityEntryBuilder.create()
                .entity(EntityHacAircraft.class)
                .id(new ResourceLocation(MODID, "aircraft"), 1)
                .name("hac_aircraft")
                .tracker(128, 1, true)
                .build();
        event.getRegistry().register(aircraftEntry);
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        proxy.registerItemRenderer(ITEM_PYXIS, 0, "pyxis");
    }
}
