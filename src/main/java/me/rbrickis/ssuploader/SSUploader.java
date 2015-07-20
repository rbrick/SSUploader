package me.rbrickis.ssuploader;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import me.rbrickis.ssuploader.events.KeybindHandler;
import me.rbrickis.ssuploader.proxy.CommonProxy;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

@Mod(
        modid = "ssuploader",
        name = "SSUploader",
        version = "0.0.1"
)
public class SSUploader {

    // Our mod instance
    @Mod.Instance("ssuploader")
    public static SSUploader instance;

    @SidedProxy(clientSide = "me.rbrickis.ssuploader.proxy.ClientProxy", serverSide = "me.rbrickis.ssuploader.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static String client_id = "none";


    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        proxy.register(); // REGISTER SHIT BITCH
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        Property clientid = config.get("imgur_settings", "Client-ID", "none");
        SSUploader.client_id = clientid.getString();
        config.save();

    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        FMLCommonHandler.instance().bus().register(new KeybindHandler());
        ClientRegistry.registerKeyBinding(Keybindings.TAKE_N_UPLOAD);

    }

    @Mod.EventHandler
    public void onPostInit(FMLPostInitializationEvent event) {

    }

}
