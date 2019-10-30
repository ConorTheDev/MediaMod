package me.conorthedev.mediamod.gui;

import cc.hyperium.Hyperium;
import me.conorthedev.mediamod.config.Settings;
import me.conorthedev.mediamod.gui.util.ButtonTooltip;
import me.conorthedev.mediamod.gui.util.IMediaGui;
import me.conorthedev.mediamod.media.spotify.SpotifyHandler;
import me.conorthedev.mediamod.util.PlayerMessager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.io.IOException;

class GuiServices extends ButtonTooltip implements IMediaGui {
    @Override
    public void initGui() {
        buttonList.add(new GuiButton(0, width / 2 - 100, height - 50, "Back"));

        if (!SpotifyHandler.logged) {
            buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 - 35, "Login to Spotify"));
        } else {
            buttonList.add(new GuiButton(2, width / 2 - 100, height / 2 - 35, "Logout of Spotify"));
        }

        buttonList.add(new GuiButton(3, width / 2 - 100, height / 2 - 10, getSuffix(Settings.EXTENSION_ENABLED, "Use Browser Extension")));

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawDefaultBackground();

        GlStateManager.pushMatrix();
        GlStateManager.color(1, 1, 1, 1);

        mc.getTextureManager().bindTexture(headerResource);

        drawModalRectWithCustomSizedTexture(width / 2 - 111, height / 2 - 110, 0, 0, 222, 55, 222, 55);
        GlStateManager.popMatrix();

        if (!SpotifyHandler.logged) {
            drawCenteredString(fontRendererObj, "Spotify not logged in! Please login below", width / 2, height / 2 - 53, Color.red.getRGB());
        } else {
            drawCenteredString(fontRendererObj, "Spotify is logged in!", width / 2, height / 2 - 53, Color.green.getRGB());
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected String getButtonTooltip(int buttonId) {
        return buttonId == 3 ? "Disables or Enables the Browser Extension" : null;
    }

    @Override
    public void onGuiClosed() {
        Hyperium.CONFIG.save();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(new GuiMediaModSettings());
                break;

            case 1:
                mc.displayGuiScreen(null);
                PlayerMessager.sendMessage("&cOpening browser with instructions on what to do, when it opens, log in with your Spotify Account and press 'Agree'");
                SpotifyHandler.INSTANCE.connectSpotify();
                break;

            case 2:
                SpotifyHandler.spotifyApi = null;
                SpotifyHandler.logged = false;
                mc.displayGuiScreen(new GuiServices());
                break;
            case 3:
                Settings.EXTENSION_ENABLED = !Settings.EXTENSION_ENABLED;
                button.displayString = getSuffix(Settings.EXTENSION_ENABLED, "Use Browser Extension");
                break;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}