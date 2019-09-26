package me.conorthedev.mediamod.gui;

import me.conorthedev.mediamod.Settings;
import me.conorthedev.mediamod.gui.util.DynamicTextureWrapper;
import me.conorthedev.mediamod.gui.util.IMediaGui;
import me.conorthedev.mediamod.media.base.IMediaHandler;
import me.conorthedev.mediamod.media.base.ServiceHandler;
import me.conorthedev.mediamod.media.spotify.api.playing.CurrentlyPlayingObject;
import me.conorthedev.mediamod.media.spotify.api.track.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerOverlay {
    /**
     * An instance of this class
     */
    public static final PlayerOverlay INSTANCE = new PlayerOverlay();

    /**
     * Average Color Cache
     */
    private static final HashMap<BufferedImage, Color> avgColorCache = new HashMap<>();

    /**
     * If the current tick is the first tick being called
     */
    private boolean first = true;

    /**
     * The current song
     *
     * @see me.conorthedev.mediamod.media.spotify.api.playing.CurrentlyPlayingObject
     */
    private CurrentlyPlayingObject currentlyPlayingObject = null;

    /**
     * The length of the concatenated song name
     */
    private int concatNameCount = 0;
    private int concatArtistCount = 0;
    private boolean artistFirstRun = true;
    private boolean firstRun = true;

    private static Color averageColor(BufferedImage bi, int w, int h) {
        final Color[] color = {Color.gray};
        if (avgColorCache.containsKey(bi)) {
            return avgColorCache.get(bi);
        } else {
            new Thread(() -> {
                long sumr = 0, sumg = 0, sumb = 0;
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        Color pixel = new Color(bi.getRGB(x, y));
                        sumr += pixel.getRed();
                        sumg += pixel.getGreen();
                        sumb += pixel.getBlue();
                    }
                }
                int num = w * h;
                color[0] = new Color((int) sumr / num, (int) sumg / num, (int) sumb / num);

                avgColorCache.put(bi, color[0]);
            }).start();

            return color[0];
        }
    }

    /**
     * Fired when a game overlay is being rendered
     *
     * @param event - RenderGameOverlayEvent
     * @see RenderGameOverlayEvent
     */
    @SubscribeEvent
    public void onRender(RenderGameOverlayEvent.Post event) {

        // Get a Minecraft Instance
        Minecraft mc = FMLClientHandler.instance().getClient();

        if (event.type.equals(RenderGameOverlayEvent.ElementType.EXPERIENCE) && Settings.SHOW_PLAYER && Settings.ENABLED) {
            if (this.first) {
                // Make sure that this is never ran again
                this.first = false;

                // Setup a ScheduledExecutorService to run every 3 seconds
                ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                exec.scheduleAtFixedRate(() -> {
                    try {
                        // Check if we are ready
                        if (ServiceHandler.INSTANCE.getCurrentMediaHandler() != null) {
                            if (ServiceHandler.INSTANCE.getCurrentMediaHandler().handlerReady()) {
                                this.currentlyPlayingObject = ServiceHandler.INSTANCE.getCurrentMediaHandler().getCurrentTrack();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 0, 3, TimeUnit.SECONDS);
            }

            // Make sure that a MediaHandler exists and is ready
            IMediaHandler currentHandler = ServiceHandler.INSTANCE.getCurrentMediaHandler();
            if (currentHandler != null && currentHandler.handlerReady() && currentlyPlayingObject != null) {
                // Make sure there's no GUI screen being displayed
                if (mc.currentScreen == null && !mc.gameSettings.showDebugInfo) {
                    this.drawPlayer(Settings.PLAYER_X, Settings.PLAYER_Y, Settings.MODERN_PLAYER_STYLE, false, Settings.PLAYER_ZOOM);
                }
            }
        }
    }

    /**
     * Renders the media player on the screen
     *
     * @param cornerX   - the x coordinate of the top left corner
     * @param cornerY   - the y coordinate of the top right corner
     * @param isModern  - if the player should be rendered as the modern style
     * @param testing   - if it is a testing player i.e. in the settings menu
     * @param scale     - the scale to use
     */
    void drawPlayer(int cornerX, int cornerY, boolean isModern, boolean testing, double scale) {

        // Get a Minecraft Instance
        Minecraft mc = FMLClientHandler.instance().getClient();

        // Establish a FontRenderer
        FontRenderer fontRenderer = mc.fontRendererObj;

        // Track Metadata
        Track track = null;
        if (this.currentlyPlayingObject != null) {
            track = this.currentlyPlayingObject.item;
        }

        // Track Name
        String trackName = "Song Name";
        // Track Artist
        String trackArtist = null;
        // URL of album art
        URL url = null;
        // Color of the album art
        Color color = Color.gray;

        if (!testing && track != null) {
            // Get the track metadata
            trackName = track.name;
            if (track.album != null) {
                if (track.album.artists != null && track.album.artists.length > 0) {
                    trackArtist = track.album.artists[0].name;
                }
                if (track.album.images != null && track.album.images.length > 0) {
                    try {
                        url = new URL(track.album.images[0].url);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, scale);

        // Set the X Position for the text to be rendered at
        int textXPosition = cornerX + 10;
        if (Settings.SHOW_ALBUM_ART && url != null) {
            // If the album art is being rendered we must move the text to the right
            textXPosition = cornerX + 50;
        }

        if (isModern) {
            // Draw the outline of the player
            Gui.drawRect(cornerX + 151, cornerY + 4, cornerX + 4, cornerY + 51, new Color(0, 0, 0, 75).getRGB());
        }

        // Background
        if (Settings.AUTO_COLOR_SELECTION && Settings.SHOW_ALBUM_ART && !testing) {
            if (url != null) {
                BufferedImage image = DynamicTextureWrapper.getImage(url);

                color = averageColor(image, image.getWidth(), image.getHeight());

                if (color.equals(Color.black)) {
                    color = Color.gray;
                }
            }

            // Draw the background of the player
            Gui.drawRect(cornerX + 150, cornerY + 5, cornerX + 5, cornerY + 50, color.darker().getRGB());
        } else {
            // Draw the background of the player
            Gui.drawRect(cornerX + 150, cornerY + 5, cornerX + 5, cornerY + 50, Color.darkGray.getRGB());
        }

        // Draw the metadata of the track (title, artist, album art)
        if (!Settings.SHOW_ALBUM_ART) {
            if (trackName.length() >= 28) {
                String concatName = trackName + "    " + trackName;
                AtomicInteger concatNameCount2 = new AtomicInteger(concatNameCount + 26);

                if ((concatNameCount + 26) >= concatName.length()) {
                    concatNameCount = 0;
                    concatNameCount2.set(concatNameCount + 26);
                }

                if (firstRun) {
                    // Set the firstRun variable to false
                    firstRun = false;

                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                    exec.scheduleAtFixedRate(() -> {
                        concatNameCount++;
                        concatNameCount2.set(concatNameCount + 26);
                    }, 0, 500, TimeUnit.MILLISECONDS);
                }

                // String concatenation for tracks
                fontRenderer.drawString(concatName.substring(concatNameCount, concatNameCount2.get()), textXPosition, cornerY + 11, -1);
            } else {
                // Draw the track name normally
                fontRenderer.drawString(trackName, textXPosition, cornerY + 11, -1);
            }
        } else {
            if (trackName.length() >= 19) {
                String concatName = trackName + "    " + trackName;
                AtomicInteger concatNameCount2 = new AtomicInteger(concatNameCount + 17);

                if ((concatNameCount + 16) >= concatName.length()) {
                    concatNameCount = 0;
                    concatNameCount2.set(concatNameCount + 17);
                }

                if (firstRun) {
                    // Set the firstRun variable to false
                    firstRun = false;

                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                    exec.scheduleAtFixedRate(() -> {
                        concatNameCount++;
                        concatNameCount2.set(concatNameCount + 16);
                    }, 0, 500, TimeUnit.MILLISECONDS);
                }

                // String concatenation for tracks
                fontRenderer.drawString(concatName.substring(concatNameCount, concatNameCount2.get()), textXPosition, cornerY + 11, -1);
            } else {
                // Draw the track name normally
                fontRenderer.drawString(trackName, textXPosition, cornerY + 11, -1);
            }
        }

        if (trackArtist != null) {
            if (("by" + trackArtist).length() >= 18) {
                String concatName = ("by" + trackArtist) + "    " + ("by " + trackArtist);
                AtomicInteger concatArtistCount2 = new AtomicInteger(concatArtistCount + 17);

                if ((concatArtistCount + 16) >= concatName.length()) {
                    concatArtistCount = 0;
                    concatArtistCount2.set(concatArtistCount + 17);
                }

                if (artistFirstRun) {
                    // Set the artistFirstRun variable to false
                    artistFirstRun = false;

                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                    exec.scheduleAtFixedRate(() -> {
                        concatArtistCount++;
                        concatArtistCount2.set(concatArtistCount + 16);
                    }, 0, 500, TimeUnit.MILLISECONDS);
                }

                // String concatenation for tracks
                fontRenderer.drawString(concatName.substring(concatArtistCount, concatArtistCount2.get()), textXPosition, cornerY + 20, Color.white.darker().getRGB());
            } else {
                fontRenderer.drawString("by " + trackArtist, textXPosition, cornerY + 20, Color.white.darker().getRGB());
            }
        }

        if (currentlyPlayingObject != null) {
            if (currentlyPlayingObject.item.duration_ms > 0 && currentlyPlayingObject.progress_ms >= 0) {
                int right = textXPosition + 91;
                int progressMultiplier = 90;
                if (!Settings.SHOW_ALBUM_ART) {
                    right = textXPosition + 135;
                    progressMultiplier = 135;
                }
                // Draw the progress bar
                if (Settings.MODERN_PLAYER_STYLE) {
                    // Draw outline
                    Gui.drawRect(textXPosition - 1, cornerY + 32, right, cornerY + 42, new Color(0, 0, 0, 75).getRGB());
                }

                // Draw background
                Gui.drawRect(textXPosition, cornerY + 33, right - 1, cornerY + 41, Color.darkGray.darker().getRGB());

                // Get the percent complete
                float percentComplete = (float) 0.75;
                if (track != null && !testing) {
                    percentComplete = (float) currentlyPlayingObject.progress_ms / (float) track.duration_ms;
                }

                Color displayColor = Settings.AUTO_COLOR_SELECTION && Settings.SHOW_ALBUM_ART ? color : Color.green;

                if (Settings.MODERN_PLAYER_STYLE) {
                    // Draw the gradient styled progress bar
                    drawGradientRect(textXPosition, cornerY + 33, (int) (textXPosition + (progressMultiplier * percentComplete)), cornerY + 41, displayColor.getRGB(), displayColor.darker().getRGB());
                } else {
                    // Draw the normal progress bar
                    Gui.drawRect(textXPosition, cornerY + 33, (int) (textXPosition + (progressMultiplier * percentComplete)), cornerY + 41, displayColor.getRGB());
                }
            }


            // Draw the album art
            if (Settings.SHOW_ALBUM_ART && currentlyPlayingObject.item.album != null && currentlyPlayingObject.item.album.images.length > 0) {
                if (Settings.MODERN_PLAYER_STYLE) {
                    // Draw outline
                    Gui.drawRect(cornerX + 46, cornerY + 9, cornerX + 9, cornerY + 46, new Color(0, 0, 0, 75).getRGB());
                }

                // Setup OpenGL
                GlStateManager.pushMatrix();
                GlStateManager.color(1, 1, 1, 1);

                // Bind the texture for rendering
                if (testing) {
                    // Since it's a testing player we bind the MediaMod Logo
                    mc.getTextureManager().bindTexture(IMediaGui.iconResource);
                } else {
                    if (url != null) {
                        mc.getTextureManager().bindTexture(DynamicTextureWrapper.getTexture(url));
                    }
                }

                // Render the album art as 35x35
                Gui.drawModalRectWithCustomSizedTexture(cornerX + 10, cornerY + 10, 0, 0, 35, 35, 35, 35);
                GlStateManager.popMatrix();
            }
        }

        GlStateManager.popMatrix();
    }

    /**
     * Draws a rectangle with a vertical gradient between the specified colors (ARGB format).
     * Args: x1, y1, x2, y2, topColor, bottomColor
     *
     * @author ScottehBoeh
     */
    private void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor) {
        float f = (float) (startColor >> 24 & 255) / 255.0F;
        float f1 = (float) (startColor >> 16 & 255) / 255.0F;
        float f2 = (float) (startColor >> 8 & 255) / 255.0F;
        float f3 = (float) (startColor & 255) / 255.0F;
        float f4 = (float) (endColor >> 24 & 255) / 255.0F;
        float f5 = (float) (endColor >> 16 & 255) / 255.0F;
        float f6 = (float) (endColor >> 8 & 255) / 255.0F;
        float f7 = (float) (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 1).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, 1).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 1).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, 1).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}
