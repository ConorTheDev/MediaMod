package me.conorthedev.mediamod;

import cc.hyperium.Hyperium;
import cc.hyperium.event.EventBus;
import cc.hyperium.event.InvokeEvent;
import cc.hyperium.event.client.InitializationEvent;
import cc.hyperium.event.world.WorldChangeEvent;
import cc.hyperium.internal.addons.IAddon;
import me.conorthedev.mediamod.command.MediaModCommand;
import me.conorthedev.mediamod.config.Settings;
import me.conorthedev.mediamod.gui.PlayerOverlay;
import me.conorthedev.mediamod.keybinds.KeybindManager;
import me.conorthedev.mediamod.media.base.ServiceHandler;
import me.conorthedev.mediamod.media.browser.BrowserHandler;
import me.conorthedev.mediamod.media.spotify.SpotifyHandler;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class MediaMod implements IAddon {

    private static final File TOS_ACCEPTED_FILE = new File(Minecraft.getMinecraft().mcDataDir, "mediamod/tosaccepted.lock");
    public static MediaMod INSTANCE;
    public final Logger LOGGER = LogManager.getLogger("MediaMod");
    public final boolean DEVELOPMENT_ENVIRONMENT = Hyperium.INSTANCE.isDevEnv();
    private boolean firstLoad = true;

    @Override
    public void onLoad() {
        EventBus.INSTANCE.register(this);
    }

    @InvokeEvent
    public void init(InitializationEvent event) {
        LOGGER.info("MediaMod starting...");
        INSTANCE = this;

        EventBus.INSTANCE.register(PlayerOverlay.INSTANCE);
        Hyperium.INSTANCE.getHandlers().getCommandHandler().registerCommand(new MediaModCommand());

        File MEDIAMOD_DIRECTORY = new File(Minecraft.getMinecraft().mcDataDir, "mediamod");
        if (!MEDIAMOD_DIRECTORY.exists()) {
            LOGGER.info("Creating necessary directories and files for first launch...");
            boolean mkdir = MEDIAMOD_DIRECTORY.mkdir();

            if (mkdir) {
                LOGGER.info("Created necessary directories and files!");
            } else {
                LOGGER.fatal("Failed to create necessary directories and files!");
            }
        }

        LOGGER.info("Loading Configuration...");
        Hyperium.CONFIG.register(new Settings());

        ServiceHandler serviceHandler = ServiceHandler.INSTANCE;
        serviceHandler.registerHandler(new BrowserHandler());
        serviceHandler.registerHandler(new SpotifyHandler());
        serviceHandler.initializeHandlers();

        KeybindManager.INSTANCE.register();
    }

    @InvokeEvent
    public void worldChange(WorldChangeEvent event) {
        if (firstLoad && Minecraft.getMinecraft().thePlayer != null) {
            firstLoad = false;
        }
    }

    public boolean isTOSAccepted() {
        return TOS_ACCEPTED_FILE.exists();
    }

    public void setTOSAccepted() {
        try {
            boolean created = TOS_ACCEPTED_FILE.createNewFile();

            if (!created) {
                LOGGER.fatal("Failed to create TOSACCEPTED.lock!");
            }
        } catch (IOException e) {
            LOGGER.fatal("Failed to create TOSACCEPTED.lock!", e);
        }
    }

    @Override
    public void onClose() {
        LOGGER.info("Closing MediaMod");
    }
}
