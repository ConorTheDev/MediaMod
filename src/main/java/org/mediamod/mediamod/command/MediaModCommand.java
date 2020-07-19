package org.mediamod.mediamod.command;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.mediamod.mediamod.MediaMod;
import org.mediamod.mediamod.gui.GuiMediaModSettings;
import org.mediamod.mediamod.media.services.spotify.SpotifyService;
import org.mediamod.mediamod.parties.PartyManager;
import org.mediamod.mediamod.parties.responses.PartyJoinResponse;
import org.mediamod.mediamod.parties.responses.PartyStartResponse;
import org.mediamod.mediamod.util.ChatColor;
import org.mediamod.mediamod.util.Multithreading;
import org.mediamod.mediamod.util.PlayerMessenger;
import org.mediamod.mediamod.util.TickScheduler;

import java.util.Arrays;
import java.util.List;

/**
 * The client-side command to open the MediaMod GUI
 *
 * @see net.minecraft.command.ICommand
 */
public class MediaModCommand extends CommandBase {

    @Override
    public String getName() {
        return "mediamod";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/mediamod";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("media", "mm");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length <= 0) {
            TickScheduler.INSTANCE.schedule(1, () -> FMLClientHandler.instance().getClient().displayGuiScreen(new GuiMediaModSettings()));
        } else {
            String subcmd = args[0];
            if (subcmd.equalsIgnoreCase("party")) {
                if (!Minecraft.getMinecraft().isSnooperEnabled()) {
                    PlayerMessenger.sendMessage(ChatColor.RED + "You must enable Snooper in Minecraft Settings and restart your client to participate in a party!", true);
                    return;
                }

                if (!MediaMod.INSTANCE.authenticatedWithAPI) {
                    PlayerMessenger.sendMessage(ChatColor.RED + "An error occurred when contacting the MediaMod API, Please click 'reconnect'. If this issue persists please contact us!", true);
                    return;
                }

                Multithreading.runAsync(() -> {
                    if (args.length >= 2) {
                        String function = args[1];
                        switch (function.toLowerCase()) {
                            case "start":
                                if(SpotifyService.isLoggedOut()) {
                                    PlayerMessenger.sendMessage(ChatColor.RED + "You must be logged into Spotify to join a party!", true);
                                    return;
                                }

                                if (PartyManager.instance.isInParty()) {
                                    PlayerMessenger.sendMessage(ChatColor.RED + "You are already in a party!", true);
                                    break;
                                }

                                PlayerMessenger.sendMessage(ChatColor.GRAY + "Creating MediaMod Party... " + "(note: this only works with spotify at the moment)", true);
                                PartyStartResponse response = PartyManager.instance.startParty();

                                if (response.code.equals("")) {
                                    PlayerMessenger.sendMessage(ChatColor.RED + "An error occurred whilst creating your MediaMod party!", true);
                                } else {
                                    ITextComponent urlComponent = new TextComponentString(ChatColor.WHITE + "" + ChatColor.BOLD + response.code);
                                    urlComponent.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, response.code));
                                    urlComponent.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Copy Code")));
                                    PlayerMessenger.sendMessage(new TextComponentString(ChatColor.GRAY + "Share this code with your friends to invite them to your party: ").appendSibling(urlComponent), true);
                                }
                                break;
                            case "leave":
                                if (PartyManager.instance.isInParty()) {
                                    PlayerMessenger.sendMessage((ChatColor.GRAY + "Leaving party..."), true);
                                    boolean success = PartyManager.instance.leaveParty();

                                    if (success) {
                                        PlayerMessenger.sendMessage((ChatColor.GREEN + "You have left the party"), true);
                                    } else {
                                        PlayerMessenger.sendMessage(ChatColor.RED + "An error occurred whilst trying to leave the party!", true);
                                    }
                                } else {
                                    PlayerMessenger.sendMessage(ChatColor.RED + "You are not in a party!");
                                }
                                break;
                            case "join":
                                if (args.length >= 3) {
                                    if(SpotifyService.isLoggedOut()) {
                                        PlayerMessenger.sendMessage(ChatColor.RED + "You must be logged into Spotify to join a party!", true);
                                        return;
                                    }

                                    String inputCode = args[2];
                                    if (inputCode.length() != 6) {
                                        PlayerMessenger.sendMessage(ChatColor.RED + "Invalid code!");
                                    } else {
                                        if (!PartyManager.instance.isInParty()) {
                                            PartyJoinResponse joinResponse = PartyManager.instance.joinParty(inputCode);

                                            if (joinResponse.success) {
                                                PlayerMessenger.sendMessage((ChatColor.GREEN + "You have joined " + joinResponse.host + "'s party"), true);
                                            } else {
                                                PlayerMessenger.sendMessage(ChatColor.RED + "An error occurred whilst trying to join the party!", true);
                                            }
                                        } else {
                                            PlayerMessenger.sendMessage(ChatColor.RED + "You must leave your current party before you join a new one!");
                                        }
                                    }
                                } else {
                                    PlayerMessenger.sendMessage(ChatColor.RED + "Incorrect syntax! Usage: /mm party <start/invite/info/join/leave>");
                                }
                                break;
                            default:
                                PlayerMessenger.sendMessage(ChatColor.RED + "Incorrect syntax! Usage: /mm party <start/invite/info/join/leave>");
                        }
                    } else {
                        PlayerMessenger.sendMessage(ChatColor.RED + "Incorrect syntax! Usage: /mm party <start/invite/info/join/leave>");
                    }
                });
            }
        }

    }

    @Override
    public int getRequiredPermissionLevel() {
        return -1;
    }
}
