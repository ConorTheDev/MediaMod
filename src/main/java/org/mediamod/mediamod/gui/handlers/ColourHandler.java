package org.mediamod.mediamod.gui.handlers;

import com.moandjiezana.toml.Toml;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mediamod.mediamod.MediaMod;
import org.mediamod.mediamod.config.Settings;
import org.mediamod.mediamod.gui.handlers.types.Colours;
import org.mediamod.mediamod.gui.handlers.types.Metadata;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;

public class ColourHandler {
    private final Logger logger = LogManager.getLogger(this.getClass());
    public static ColourHandler INSTANCE = new ColourHandler();

    /**
     * Iterates through the themes directory and fishes out .toml files and attempts to parse them.
     *
     * @author ChachyDev
     * @since 2.0.0-beta.2
     * @return Metadata and Colours from File
     */

    public ArrayList<Pair<Metadata, Colours>> getThemes() {
        File[] files = MediaMod.INSTANCE.mediamodThemeDirectory.listFiles();
        ArrayList<Pair<Metadata, Colours>> themes = new ArrayList<>();
        if (files != null) {
            for (File theme : files) {
                if (theme.getName().endsWith(".toml")) {
                    try {
                        Toml toml = new Toml().read(theme);
                        themes.add(
                                Pair.of(
                                        toml.getTable("metadata").to(Metadata.class),
                                        toml.getTable("colours").to(Colours.class)
                                )
                        );
                    } catch (Exception e) {
                        logger.error("Failed to read toml file!");
                    }
                }
            }
        }
        return themes;
    }

    /**
     * Grabs the currently selected theme's background opts from the colour table
     *
     * @author ChachyDev
     * @since 2.0.0-beta.2
     * @return Metadata and Colours from File
     */

    public Color getPlayerColour() {
        Colours colourBlock = new Toml().read(Settings.THEME_FILE).getTable("colours").to(Colours.class);
        if (colourBlock == null) {
            return Color.darkGray.brighter();
        } else {
            return new Color(colourBlock.getPlayerRed(), colourBlock.getPlayerGreen(), colourBlock.getPlayerBlue());
        }
    }

    /**
     * Grabs the currently selected theme's text opts from the colour table
     *
     * @author ChachyDev
     * @since 2.0.0-beta.2
     * @return Metadata and Colours from File
     */

    public Color getPlayerTextColour() {
        Colours colourBlock = new Toml().read(Settings.THEME_FILE).getTable("colours").to(Colours.class);
        if (colourBlock == null) {
            return Color.white;
        } else {
            return new Color(colourBlock.getTextRed(), colourBlock.getTextGreen(), colourBlock.getTextBlue());
        }
    }
}
