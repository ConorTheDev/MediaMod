package me.conorthedev.mediamod.gui.util;

import cc.hyperium.utils.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

public interface IMediaGui {
    ResourceLocation iconResource = new ResourceLocation("textures/mediamod.png");
    ResourceLocation headerResource = new ResourceLocation( "textures/header.png");

    default String getSuffix(boolean option, String label) {
        return option ? (label + ": " + ChatColor.GREEN + "YES") : (label + ": " + ChatColor.RED + "NO");
    }

    default int getRowPos(int rowNumber) {
        return 60 + rowNumber * 23;
    }

    default void drawHeader(int width, int height) {
        GlStateManager.pushMatrix();
        GlStateManager.color(1, 1, 1, 1);

        Minecraft.getMinecraft().getTextureManager().bindTexture(headerResource);

        Gui.drawModalRectWithCustomSizedTexture(width / 2 - 111, height / 2 - 110, 0, 0, 222, 55, 222, 55);
        GlStateManager.popMatrix();
    }
}
