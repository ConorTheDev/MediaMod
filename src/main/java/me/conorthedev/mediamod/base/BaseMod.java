package me.conorthedev.mediamod.base;

import com.google.gson.Gson;
import me.conorthedev.mediamod.util.Metadata;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * A base mod used in all of ConorTheDev's Mods
 *
 * @author ConorTheDev
 */
public class BaseMod {
    public static final String ENDPOINT = "https://api.conorthedev.me";

    /**
     * Registers with api.conorthedev.me
     */
    public static boolean init() {
        try {
            // Create a conncetion
            URL url = new URL(ENDPOINT + "/api/register/" + FMLClientHandler.instance().getClient().getSession().getProfile().getId().toString() + "/" + Metadata.MODID + "/" + Metadata.VERSION);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            // Set the request method
            con.setRequestMethod("POST");
            // Set the user agent
            con.setRequestProperty("user-agent", "MediaMod/1.0");
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
            RegisterResponse registerResponse = g.fromJson(content, RegisterResponse.class);

            return registerResponse.uuid != null;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static class RegisterResponse {
        final String uuid;
        final ModResponse[] mods;

        RegisterResponse(String uuid, ModResponse[] mods) {
            this.uuid = uuid;
            this.mods = mods;
        }
    }

    private static class ModResponse {
        final String id;
        final String version;

        ModResponse(String id, String version) {
            this.id = id;
            this.version = version;
        }
    }
}
