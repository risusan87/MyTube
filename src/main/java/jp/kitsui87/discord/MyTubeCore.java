package jp.kitsui87.discord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jp.kitsui87.discord.audio.AudioPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class MyTubeCore {

    public static final Map<String, AudioPlayer> AUDIO_PLAYER = new HashMap<>();
    private static String botkey, ytkey;

    public static void main(String[] args) throws Exception {

        // load keys
        final String secretLocation = "./secret.txt";
        try (
            FileReader fr = new FileReader(new File(secretLocation));
            BufferedReader br = new BufferedReader(fr);
        ) {
            botkey = br.readLine();
            ytkey = br.readLine();
            
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.printf("\nbot key read: %s\nyt key read: %s\n", botkey, ytkey);

        JDA jda = JDABuilder.createDefault(getBotKey())
                .addEventListeners(new EventHandler())
                // looks like bots require to enable MESSAGE_CONTENT intent to recognize messages
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.updateCommands().addCommands(
                Commands.slash(
                    "checkconnection", 
                    "checks the current connection to the audio"
                ),
                Commands.slash(
                    "join", 
                    "Join"
                ),
                Commands.slash(
                    "addsong", 
                    "What an obvious command name"
                    ).addOption(
                        OptionType.STRING, 
                        "youtube_url", 
                        "What an ovbious arg name"
                ),
                Commands.slash(
                    "skip", 
                    "Fuck off"
                ),
                Commands.slash(
                    "playpause", 
                    "Toggles play/pause state"
                ),
                Commands.slash(
                    "repeat", 
                    "Configures repeat mode"
                    ).addOption(
                        OptionType.STRING, 
                        "mode", 
                        "Repeat mode of this playlist: none, list, or song. Case not sensitive."
                )
        ).queue();

    }

    public static final String getGoogleKey() {
        return ytkey;
    }

    public static final String getBotKey() {
        return botkey;
    }

}
