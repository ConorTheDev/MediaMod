package me.conorthedev.mediamod.media.spotify;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.SpotifyApi;
import me.conorthedev.mediamod.MediaMod;
import me.conorthedev.mediamod.util.Multithreading;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * The main class for all Spotify-related things
 */
public class SpotifyHandler {
    private static HttpServer server = null;
    public static SpotifyApi spotifyApi = null;
    public static boolean logged = false;

    public static void connectSpotify() {
        if (server != null) {
            // Stop the server
            server.stop(0);

            server = null;

        }
        try {
            // Create a HTTP Server for the Spotify API to call back to (http://localhost:1337)
            server = HttpServer.create(new InetSocketAddress(1337), 0);
            server.createContext("/callback", new SpotifyCallbackHandler());
            server.setExecutor(null);

            // Start the server
            server.start();

            // Open the URL
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI("https://accounts.spotify.com/authorize?client_id=4d33df7152bb4e2dac57167eeaafdf45&response_type=code&redirect_uri=http%3A%2F%2Flocalhost:1337%2Fcallback%2F&scope=user-read-playback-state%20user-read-currently-playing%20user-modify-playback-state&state=34fFs29kd09"));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(String code) {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        ses.scheduleAtFixedRate(() -> {
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "MediaMod" + EnumChatFormatting.RED + "] "
                            + EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.BOLD + "INFO: " + EnumChatFormatting.RESET + EnumChatFormatting.RED + "Spotify Token Expired! Please login again in the GUI!"));
            logged = false;
        }, 1, 1, TimeUnit.HOURS);

        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(
                new ChatComponentText(EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "MediaMod" + EnumChatFormatting.RED + "] "
                        + EnumChatFormatting.GRAY + "Exchanging authorization code for access token, this may take a moment..."));
        try {
            // Create a conncetion
            URL url = new URL("https://api.conorthedev.me/api/mediamod/spotify/token/" + code);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            // Set the request method
            con.setRequestMethod("GET");
            // Set the user agent
            con.setRequestProperty("user-agent", "SpotifyMod/1.0");
            // Connect to the API
            con.connect();

            // Read the output
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String content = in.lines().collect(Collectors.joining());

            // Close the input reader & the conncetion
            in.close();
            con.disconnect();

            // Parse JSON
            Gson g = new Gson();
            TokenAPIResponse tokenAPIResponse = g.fromJson(content, TokenAPIResponse.class);

            // Put into the Spotify API
            spotifyApi = SpotifyApi.builder().setAccessToken(tokenAPIResponse.access_token).setRefreshToken(tokenAPIResponse.refresh_token).build();

            if (spotifyApi.getRefreshToken() != null) {
                logged = true;
                // Tell the user that they were logged in
                Minecraft.getMinecraft().thePlayer.addChatComponentMessage(
                        new ChatComponentText(EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "MediaMod" + EnumChatFormatting.RED + "] "
                                + EnumChatFormatting.GREEN.toString() + EnumChatFormatting.BOLD + "SUCCESS! " + EnumChatFormatting.RESET + EnumChatFormatting.WHITE + "Logged into Spotify!"));
                if (MediaMod.INSTANCE.DEVELOPMENT_ENVIRONMENT) {
                    Minecraft.getMinecraft().thePlayer.addChatComponentMessage(
                            new ChatComponentText(EnumChatFormatting.RED + "[" + EnumChatFormatting.WHITE + "MediaMod" + EnumChatFormatting.RED + "] "
                                    + EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.BOLD + "DEBUG: " + EnumChatFormatting.RESET + "Current Song: " + spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute().getItem().getName() + " by " + spotifyApi.getInformationAboutUsersCurrentPlayback().build().execute().getItem().getArtists()[0].getName()));
                }
            }
        } catch (Exception e) {
            MediaMod.INSTANCE.LOGGER.error("Error: " + e);
        }
    }

    private static void setLogged(boolean toSet) {
        logged = toSet;
    }

    private static class SpotifyCallbackHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            // Handle the req
            Multithreading.runAsync(() -> handleRequest(t.getRequestURI().toString().replace("/callback/?code=", "").substring(0, t.getRequestURI().toString().replace("/callback/?code=", "").length() - 18)));

            String response = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "  <head>\n" +
                    "    <meta charset=\"utf-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                    "    <title>MediaMod</title>\n" +
                    "    <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/bulma/0.7.5/css/bulma.min.css\">\n" +
                    "    <script defer src=\"https://use.fontawesome.com/releases/v5.3.1/js/all.js\"></script>\n" +
                    "  </head>\n" +
                    "  <body>\n" +
                    "  <section class=\"section\">\n" +
                    "    <div class=\"container\">\n" +
                    "      <h1 class=\"title\">\n" +
                    "        Success!\n" +
                    "      </h1>\n" +
                    "      <p class=\"subtitle\">\n" +
                    "        Please close this window and go back into Minecraft!\n" +
                    "      </p>\n" +
                    "    </div>\n" +
                    "  </section>\n" +
                    "  </body>\n" +
                    "</html>";

            t.sendResponseHeaders(200, response.length());

            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();

            // Stop the server
            server.stop(0);
        }
    }

    private static class TokenAPIResponse {
        String access_token;
        int expires_in;
        String refresh_token;

        TokenAPIResponse(String access_token, int expires_in, String refresh_token) {
            this.access_token = access_token;
            this.expires_in = expires_in;
            this.refresh_token = refresh_token;
        }
    }
}
