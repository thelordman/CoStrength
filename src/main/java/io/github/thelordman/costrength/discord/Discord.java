package io.github.thelordman.costrength.discord;

import io.github.thelordman.costrength.CoStrength;
import io.github.thelordman.costrength.discord.jdalisteners.MessageReceivedListener;
import io.github.thelordman.costrength.discord.jdalisteners.SlashCommandInteractionListener;
import io.github.thelordman.costrength.utilities.Methods;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import static org.bukkit.Bukkit.getLogger;

public class Discord {
    public static JDA jda;

    public static TextChannel minecraftChatChannel;
    public static TextChannel commandLogChannel;

    public static ArrayList<String> slashCommands;

    public static void loadJDA() {
        String botToken = CoStrength.get().getConfig().getString("botToken");
        String minecraftChatChannelId = CoStrength.get().getConfig().getString("minecraftChatChannelId");
        String minecraftLogChannelId = CoStrength.get().getConfig().getString("minecraftLogChannelId");
        String guildId = CoStrength.get().getConfig().getString("guildId");

        slashCommands = new ArrayList<>();
        slashCommands.add("ping");
        slashCommands.add("online");
        slashCommands.add("cmd");

        try {
            jda = JDABuilder.createLight(botToken, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new MessageReceivedListener())
                    .addEventListeners(new SlashCommandInteractionListener())
                    .setActivity(Activity.playing("CoStrength.minehut.gg"))
                    .build();
            jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }

        Guild guild = jda.getGuildById(guildId);
        minecraftChatChannel = jda.getTextChannelById(minecraftChatChannelId);
        commandLogChannel = jda.getTextChannelById(minecraftLogChannelId);
        assert guild != null;
        guild.updateCommands()
                .addCommands(
                        Commands.slash("ping", "Calculate the ping of the bot. Useful for troubleshooting."),
                        Commands.slash("online", "Send a list of all online players along with some useful information about the server."),
                        Commands.slash("cmd", "Execute a console command. Only available for users with 'Administrator' permission.")
                                .addOption(OptionType.STRING, "command", "The command you want to execute. Do not include '/', unless it's a FAWE command."))
                .queue();


        if (minecraftChatChannel != null) {
            minecraftChatChannel.sendMessage(":green_circle: **Server Online**").queue();
            getLogger().info("Discord integration enabled successfully");
        }
        else {
            getLogger().severe(Methods.cStr("&cDiscord integration enabling failed: minecraftChatChannel is null"));
        }
    }

    public static void shutdownJDA() {
        minecraftChatChannel.sendMessage(":red_circle: **Server Offline**").queue();
        jda.shutdown();
        getLogger().info("Discord integration disabled successfully");
    }
}
