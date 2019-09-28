package me.conorthedev.mediamod.gui;

import cc.hyperium.event.InvokeEvent;
import cc.hyperium.event.RenderHUDEvent;
import me.conorthedev.mediamod.config.ProgressStyle;
import me.conorthedev.mediamod.config.Settings;
import me.conorthedev.mediamod.gui.util.DynamicTextureWrapper;
import me.conorthedev.mediamod.gui.util.IMediaGui;
import me.conorthedev.mediamod.media.base.IMediaHandler;
import me.conorthedev.mediamod.media.base.ServiceHandler;
import me.conorthedev.mediamod.media.spotify.SpotifyHandler;
import me.conorthedev.mediamod.media.spotify.api.playing.CurrentlyPlayingObject;
import me.conorthedev.mediamod.media.spotify.api.track.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerOverlay {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("mm:ss");

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

    @InvokeEvent
    public void onRender(RenderHUDEvent event) {

        // Get a Minecraft Instance
        Minecraft mc = Minecraft.getMinecraft();

        if (Settings.SHOW_PLAYER && Settings.ENABLED) {
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
                                if (ServiceHandler.INSTANCE.getCurrentMediaHandler() instanceof SpotifyHandler) {
                                    SpotifyHandler.INSTANCE.updateProgress();
                                }
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
     * @param cornerX  - the x coordinate of the top left corner
     * @param cornerY  - the y coordinate of the top right corner
     * @param isModern - if the player should be rendered as the modern style
     * @param testing  - if it is a testing player i.e. in the settings menu
     * @param scale    - the scale to use
     */
    void drawPlayer(int cornerX, int cornerY, boolean isModern, boolean testing, double scale) {

        // Get a Minecraft Instance
        Minecraft mc = Minecraft.getMinecraft();

        mc.mcProfiler.startSection("mediamod_player");

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
        String trackArtist = "Artist Name";
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
        if (Settings.SHOW_ALBUM_ART && (testing || url != null)) {
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
        if (!(Settings.SHOW_ALBUM_ART && (testing || url != null))) {
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
            if (("by " + trackArtist).length() >= 18) {
                String concatName = ("by " + trackArtist) + "    " + ("by " + trackArtist);
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

        if (testing || currentlyPlayingObject != null) {
            if (testing || (currentlyPlayingObject.item.duration_ms > 0 && currentlyPlayingObject.progress_ms >= 0)) {
                int right = textXPosition + 91;
                int offset = 91;
                int progressMultiplier = 90;
                if (!(Settings.SHOW_ALBUM_ART && (testing || (currentlyPlayingObject.item.album != null && currentlyPlayingObject.item.album.images.length > 0)))) {
                    right = textXPosition + 135;
                    offset = 135;
                    progressMultiplier = 135;
                }
                Color displayColor = Settings.AUTO_COLOR_SELECTION && Settings.SHOW_ALBUM_ART ? color : Color.green;

                if (Settings.PROGRESS_STYLE != ProgressStyle.NUMBERS_ONLY) {
                    int progressTop = cornerY + 30;
                    int progressBottom = Settings.PROGRESS_STYLE == ProgressStyle.BAR_AND_NUMBERS_OLD ? cornerY + 39 : cornerY + 43;
                    // Draw the progress bar
                    if (Settings.MODERN_PLAYER_STYLE) {
                        // Draw outline
                        Gui.drawRect(textXPosition - 1, progressTop, right, progressBottom, new Color(0, 0, 0, 75).getRGB());
                    }

                    // Draw background
                    Gui.drawRect(textXPosition, progressTop + 1, right - 1, progressBottom - 1, Color.darkGray.darker().getRGB());

                    // Get the percent complete
                    float percentComplete = (float) 0.75;
                    if (track != null && ServiceHandler.INSTANCE.getCurrentMediaHandler() != null && !testing) {
                        percentComplete = (float) ServiceHandler.INSTANCE.getCurrentMediaHandler().getEstimatedProgressMs() / (float) track.duration_ms;
                    }
                    if (Settings.MODERN_PLAYER_STYLE) {
                        // Draw the gradient styled progress bar
                        drawGradientRect(textXPosition, progressTop + 1, (textXPosition + (progressMultiplier * percentComplete)),
                                progressBottom - 1, displayColor.getRGB(), displayColor.darker().getRGB());
                    } else {
                        // Draw the normal progress bar
                        drawRect(textXPosition, progressTop + 1, (textXPosition + (progressMultiplier * percentComplete)), progressBottom - 1, displayColor.getRGB());
                    }
                }

                if (Settings.PROGRESS_STYLE != ProgressStyle.BAR_ONLY) {
                    int progressMs = track == null || ServiceHandler.INSTANCE.getCurrentMediaHandler() == null ? 45000 :
                            ServiceHandler.INSTANCE.getCurrentMediaHandler().getEstimatedProgressMs();
                    int durationMs = track == null ? 60000 : track.duration_ms;
                    int color2 = Settings.PROGRESS_STYLE == ProgressStyle.BAR_AND_NUMBERS_NEW ? getComplementaryColor(displayColor) : Color.white.darker().getRGB();
                    int y = Settings.PROGRESS_STYLE == ProgressStyle.BAR_AND_NUMBERS_OLD ? cornerY + 41 : cornerY + 33;
                    if (Settings.PROGRESS_STYLE != ProgressStyle.NUMBERS_ONLY) {
                        String str = formatTime(durationMs);
                        fontRenderer.drawString(formatTime(progressMs), textXPosition + 1, y, color2, false);
                        fontRenderer.drawString(str, right - (fontRenderer.getStringWidth(str) + 2), y, color2, false);
                    } else {
                        String str = formatTime(progressMs) + " / " + formatTime(durationMs);
                        fontRenderer.drawString(str, textXPosition + (offset / 2.f) - (fontRenderer.getStringWidth(str) / 2.f), y, color2, false);
                    }
                }
            }


            // Draw the album art
            if (Settings.SHOW_ALBUM_ART && (testing || (currentlyPlayingObject.item.album != null && currentlyPlayingObject.item.album.images.length > 0))) {
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
        mc.mcProfiler.endSection();
    }

    private static int getComplementaryColor(Color colorToInvert) {
        double y = (299 * colorToInvert.getRed() + 587 * colorToInvert.getGreen() + 114 * colorToInvert.getBlue()) / 1000.0;
        return y >= 128 ? Color.BLACK.getRGB() : Color.WHITE.getRGB();
    }

    private static String formatTime(int milliseconds) {
        return DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(milliseconds)));
    }

    private static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(f, f1, f2, f3);
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        worldrenderer.pos(left, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, bottom, 0.0D).endVertex();
        worldrenderer.pos(right, top, 0.0D).endVertex();
        worldrenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    /**
     * Draws a rectangle with a vertical gradient between the specified colors (ARGB format).
     * Args: x1, y1, x2, y2, topColor, bottomColor
     *
     * @author ScottehBoeh
     */
    private void drawGradientRect(double left, double top, double right, double bottom, int startColor, int endColor) {
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
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(right, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, top, 0).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(left, bottom, 0).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(right, bottom, 0).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}