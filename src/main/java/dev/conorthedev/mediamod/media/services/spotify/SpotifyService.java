package dev.conorthedev.mediamod.media.services.spotify;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.conorthedev.mediamod.MediaMod;
import dev.conorthedev.mediamod.config.Settings;
import dev.conorthedev.mediamod.media.core.IServiceHandler;
import dev.conorthedev.mediamod.media.core.api.MediaInfo;
import dev.conorthedev.mediamod.util.*;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The Spotify Service Handler
 */
public class SpotifyService implements IServiceHandler {
    public static SpotifyAPI spotifyAPI;
    private final boolean isSnooperEnabled = Minecraft.getMinecraft().isSnooperEnabled();
    private int lastTimestamp = 0;
    private long lastEstimationUpdate = 0;
    private MediaInfo cachedMediaInfo = null;

    /**
     * A pass through boolean for SpotifyAPI#isLoggedIn as that doesn't need to be public
     *
     * @see SpotifyAPI#isLoggedIn()
     */
    public static boolean isLoggedIn() {
        return spotifyAPI.isLoggedIn();
    }

    /**
     * A pass through method for SpotifyAPI#logout as that doesn't need to be public
     *
     * @see SpotifyAPI#logout()
     */
    public static void logout() {
        spotifyAPI.logout();
    }

    /**
     * The name to be shown in MediaMod Menus
     */
    public String displayName() {
        return "Spotify";
    }

    /**
     * Initialises our API Wrapper if snooper is enabled
     */
    public boolean load() {
        if (isSnooperEnabled) {
            spotifyAPI = new SpotifyAPI();
        }

        return isSnooperEnabled;
    }

    /**
     * Retrieves the current track information from Spotify's Web API
     */
    @Nullable
    public MediaInfo getCurrentMediaInfo() {
        MediaInfo info = spotifyAPI.getUserPlaybackInfo();
        cachedMediaInfo = info;

        if (info != null) {
            this.lastEstimationUpdate = System.currentTimeMillis();
            this.lastTimestamp = info.timestamp;
        }

        return info;
    }

    /**
     * Returns an estimation of the current progress of the track
     */
    public int getEstimatedProgress() {
        if (cachedMediaInfo == null) return 0;

        if (cachedMediaInfo.isPlaying) {
            int estimate = (int) (lastTimestamp + (System.currentTimeMillis() - lastEstimationUpdate));
            if (estimate > cachedMediaInfo.track.duration) {
                estimate = cachedMediaInfo.track.duration;
            }
            return estimate;
        } else {
            return lastTimestamp;
        }
    }

    /**
     * This indicates if the handler is ready for usage
     */
    public boolean isReady() {
        return isSnooperEnabled && spotifyAPI.isLoggedIn();
    }
}

/**
 * A simple Spotify Web API wrapper
 */
class SpotifyAPI {
    private String accessToken = "";
    private String refreshToken;

    SpotifyAPI() {
        // Initialise callback server
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 9103), 0);
            server.createContext("/callback", new SpotifyCallbackHandler());
            server.start();
        } catch (IOException ignored) {
            MediaMod.INSTANCE.LOGGER.warn("Failed to create Spotify callback server! Is the port already in use?");
        }

        // Refresh existing token
        refreshToken = Settings.REFRESH_TOKEN;

        if (!refreshToken.equals("")) {
            refresh();
        }
    }

    /**
     * Completes the authorisation flow by contacting the MediaMod API with an authorisation code which will be exchanged for an access and refresh code
     *
     * @param authCode: The code provided by the Spotify callback
     */
    public void login(String authCode) {
        MediaMod.INSTANCE.LOGGER.info("Logging into Spotify...");
        PlayerMessager.sendMessage(ChatColor.GRAY + "Logging into Spotify...", true);

        JsonObject body = new JsonObject();
        body.addProperty("code", authCode);
        body.addProperty("uuid", MediaMod.INSTANCE.coreMod.getUUID());
        body.addProperty("secret", MediaMod.INSTANCE.coreMod.secret);

        try {
            SpotifyTokenResponse response = WebRequest.requestToMediaMod(WebRequestType.POST, "api/spotify/token", body, SpotifyTokenResponse.class);

            if (response == null) {
                MediaMod.INSTANCE.LOGGER.error("An error occurred when exchanging authorisation code for a token: response was null");
                return;
            }

            accessToken = response.accessToken;
            refreshToken = response.refreshToken;

            Settings.REFRESH_TOKEN = refreshToken;
            Multithreading.runAsync(Settings::saveConfig);

            MediaMod.INSTANCE.LOGGER.info("Logged in!");
            PlayerMessager.sendMessage(ChatColor.GRAY + "Logged in!", true);
        } catch (IOException e) {
            MediaMod.INSTANCE.LOGGER.error("An error occurred when exchanging authorisation code for a token: ", e);
        }
    }

    /**
     * Contacts the MediaMod API to exchange a refresh token for a new access token
     */
    public void refresh() {
        MediaMod.INSTANCE.LOGGER.info("Refreshing token...");
        PlayerMessager.sendMessage(ChatColor.GRAY + "Refreshing token...", true);

        if (refreshToken == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("refresh_token", refreshToken);
        body.addProperty("uuid", MediaMod.INSTANCE.coreMod.getUUID());
        body.addProperty("secret", MediaMod.INSTANCE.coreMod.secret);

        try {
            SpotifyTokenResponse response = WebRequest.requestToMediaMod(WebRequestType.POST, "api/spotify/refresh", body, SpotifyTokenResponse.class);

            if (response == null) {
                MediaMod.INSTANCE.LOGGER.error("An error occurred when exchanging refresh token for a new token: response was null");
                return;
            }

            accessToken = response.accessToken;
            refreshToken = response.refreshToken;

            Settings.REFRESH_TOKEN = refreshToken;
            Multithreading.runAsync(Settings::saveConfig);

            MediaMod.INSTANCE.LOGGER.info("Refreshed token");
            PlayerMessager.sendMessage(ChatColor.GRAY + "Refreshed token", true);
        } catch (IOException e) {
            MediaMod.INSTANCE.LOGGER.error("An error occurred when exchanging refresh token for a new auth token: ", e);
        }
    }

    /**
     * Discards of the current tokens and saves the configuration file
     */
    public void logout() {
        accessToken = null;
        refreshToken = null;
        Settings.REFRESH_TOKEN = "";

        Multithreading.runAsync(Settings::saveConfig);
    }

    @Nullable
    public MediaInfo getUserPlaybackInfo() {
        MediaInfo info = null;
        try {
            info = WebRequest.makeRequest(WebRequestType.GET, new URL("https://api.spotify.com/v1/me/player/currently-playing"), MediaInfo.class, new HashMap<String, String>() {{
                put("Authorization", "Bearer " + accessToken);
            }});
        } catch (IOException e) {
            MediaMod.INSTANCE.LOGGER.error("An error occurred when getting playback info: ", e);
        }

        return info;
    }

    /**
     * @return if the client is logged in or not
     */
    public boolean isLoggedIn() {
        return accessToken != null && !accessToken.equals("");
    }
}

class SpotifyCallbackHandler implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
            String code = query.get("code");

            String title = "Success";
            String message = "You can now close this window and go back into Minecraft!";

            if (!code.equals("")) {
                SpotifyService.spotifyAPI.login(code);
            } else {
                MediaMod.INSTANCE.LOGGER.warn("Received null code from Spotify callback?");
                title = "Failure";
                message = "Please go back to Minecraft and attempt login again!";
            }

            String response = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "  <head>\n" +
                    "    <meta charset=\"utf-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                    "    <title>MediaMod</title>\n" +
                    "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.5/css/bulma.min.css\">\n" +
                    "    <script defer src=\"https://use.fontawesome.com/releases/v5.3.1/js/all.js\"></script>\n" +
                    "  </head>\n" +
                    "  <body class=\"hero is-dark is-fullheight\">\n" +
                    "  <section class=\"section has-text-centered\">\n" +
                    "    <div class=\"container\">\n" +
                    "      <img src=\"https://raw.githubusercontent.com/MediaModMC/MediaMod/master/src/main/resources/assets/mediamod/header.png\" width=\"400px\">" + "\n" +
                    "      <h1 class=\"title\">\n" +
                    "        " + title + "\n" +
                    "      </h1>\n" +
                    "      <p class=\"subtitle\">\n" +
                    "        " + message + "\n" +
                    "      </p>\n" +
                    "    </div>\n" +
                    "  </section>\n" +
                    "  </body>\n" +
                    "</html>";

            exchange.sendResponseHeaders(200, response.length());

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }

    /**
     * Parses a http query string into a java Map
     *
     * @param query: http query
     * @see SpotifyCallbackHandler#handle(HttpExchange)
     */
    Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();

        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            result.put(entry[0], entry.length > 1 ? entry[1] : "");
        }

        return result;
    }
}

class SpotifyTokenResponse {
    @SerializedName("access_token")
    final String accessToken;
    @SerializedName("refresh_token")
    final String refreshToken;

    SpotifyTokenResponse(String access_token, String refresh_token) {
        this.accessToken = access_token;
        this.refreshToken = refresh_token;
    }
}
