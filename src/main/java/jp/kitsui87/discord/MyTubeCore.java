package jp.kitsui87.discord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.kitsui87.discord.audio.AudioPlayer;
import jp.kitsui87.discord.audio.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class MyTubeCore implements EventListener {

    public static final Map<String, AudioPlayer> AUDIO_PLAYER = new HashMap<>();
    private static String botkey, ytkey;

    public static void main(String[] args) throws Exception {

        // load keys
        String[] keys = _loadKeys();
        botkey = keys[0];
        ytkey = keys[1];
        System.out.printf("\nbot key read: %s\nyt key read: %s\n", keys[0], keys[1]);

        JDA jda = JDABuilder.createDefault(getBotKey())
                .addEventListeners(new MyTubeCore())
                // looks like bots require to enable MESSAGE_CONTENT intent to recognize messages
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.updateCommands().addCommands(
                Commands.slash("test", "Test Command to call @Redcale"),
                Commands.slash("join", "Join"),
                Commands.slash("addsong", "What an obvious command name")
                    .addOption(OptionType.STRING, "youtube_url", "What an ovbious arg name"),
                Commands.slash("skip", "Fuck off"),
                Commands.slash("playpause", "Toggles play/pause state"),
                Commands.slash("repeat", "Configures repeat mode")
                    .addOption(OptionType.STRING, "mode", "Repeat mode of this playlist: none, list, or song. Case not sensitive.")
        ).queue();

    }

    @Override
    public void onEvent(GenericEvent event) {

        // Ready Event
        if (event instanceof ReadyEvent)
            System.out.println("API setup is successful and ready.");


        // Message Received Event
        else if (event instanceof MessageReceivedEvent) {

            MessageReceivedEvent mevent = (MessageReceivedEvent) event;
            if (mevent.getAuthor().isBot())
                return;
        

        // Slash Command Event
        } else if (event instanceof SlashCommandInteractionEvent) {

            SlashCommandInteractionEvent commandEvent = (SlashCommandInteractionEvent) event;
            Guild server = commandEvent.getGuild();
            if (server == null) {
                commandEvent.reply("Commands in Private / Group DMs are not supported.").queue();
                return;
            }
            String serverID = commandEvent.getGuild().getId();
            if (!AUDIO_PLAYER.containsKey(serverID)) {
                AUDIO_PLAYER.put(serverID, new AudioPlayer());
            }
            AudioPlayer dedicatedPlayer = AUDIO_PLAYER.get(serverID);

            switch (commandEvent.getName()) {
                case "join":
                    User caller = commandEvent.getUser();
                    VoiceChannel targetVc = null;

                    for (VoiceChannel vc : server.getVoiceChannels()) {
                        for (Member m : vc.getMembers()) {
                            if (m.getId().equals(caller.getId())) {
                                targetVc = vc;
                            }
                        }
                    }
                    // The command invoker is not in a visible vc
                    if (targetVc == null) {
                        commandEvent.reply("Please join in a visible voice channel in the server you run the command.").queue();
                        return;
                    }

                    try {
                        AudioSendHandler handler = new AudioSendHandler() {

                            ByteBuffer audioDataChunk = null;

                            @Override
                            public boolean canProvide() {
                                if (dedicatedPlayer.getState() == AudioPlayer.PAUSE)
                                    return false;
                                Music m = dedicatedPlayer.getMusic((TextChannel)commandEvent.getChannel());
                                if (m == null)
                                    return false;
                                if (dedicatedPlayer.getState() == AudioPlayer.PLAY) {
                                    int bytePerMillis = (int) (m.getAudioBitrate() / 8 / 1000);
                                    int _20msDataLength = bytePerMillis * 20;
                                    audioDataChunk = m.getAudioData(_20msDataLength);
                                    return true;
                                } else
                                    return false;
                            }

                            @Override
                            public ByteBuffer provide20MsAudio() {
                                return audioDataChunk;
                            }
                            
                        };

                        server.getAudioManager().openAudioConnection(targetVc);
                        server.getAudioManager().setSendingHandler(handler);

                        EmbedBuilder embed = new EmbedBuilder()
                                .setTitle("Now playing:")
                                .setDescription("Click a button to \nperform an action.")
                                .setColor(java.awt.Color.BLUE)
                                .setFooter("Powered by JDA");


                        commandEvent.replyEmbeds(embed.build()).queue();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "addsong":
                    String url = commandEvent.getOption("youtube_url").getAsString();
                    List<String> ids = Music.getYouTubeLinks(url);
                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle("Song Added");
                    InteractionHook action = commandEvent.replyEmbeds(builder.build()).complete();
                    for (String id : ids) {
                        dedicatedPlayer.add("https://www.youtube.com/watch?v=" + id, action, builder);
                    }
                    break;
                case "skip":
                    Music m = dedicatedPlayer.getMusic((TextChannel)commandEvent.getChannel());
                    if (m == null) {
                        commandEvent.reply("No music is playing to skip you dumbass").queue();
                        break;
                    }
                    commandEvent.reply("Skipping " + m.title).queue();
                    dedicatedPlayer.skip();
                    break;
                case "playpause":
                    int state = dedicatedPlayer.getState();
                    commandEvent.reply(state == AudioPlayer.PAUSE ? "Resuming..." : "Paused.").queue();
                    dedicatedPlayer.setState(dedicatedPlayer.getState() == AudioPlayer.PAUSE ? AudioPlayer.PLAY : AudioPlayer.PAUSE);
                    break;

                case "repeat":
                    String mode = "";
                    OptionMapping modeOp = commandEvent.getOption("mode");
                    if (modeOp != null)
                        mode = modeOp.getAsString();
                    String response = "Repeat mode has been set to: " + mode.toUpperCase();
                    switch (mode.toLowerCase()) {
                        case "none":
                        dedicatedPlayer.setRepeat(AudioPlayer.REP_ONCE);
                            break;
                        case "list":
                        dedicatedPlayer.setRepeat(AudioPlayer.REP_LIST);
                            break;
                        case "song":
                        dedicatedPlayer.setRepeat(AudioPlayer.REP_SONG);
                            break;
                        default:
                            response = "Unknown ardument: \"list\", \"once\", or \"song\" expected.";
                    }
                    commandEvent.reply(response).queue();
                    break;

                case "test":
                    commandEvent.reply("Calling Red Scale.").queue();
                    commandEvent.getChannel().sendMessage("@Red Scale#2156").queue();
                    break;
            }

        }
    }

    /**
     * load key
     * @return [0]: jda, [1]: yt
     */
    private static String[] _loadKeys() {

        final String path = "src/main/resources/mytube.secret";
        try (
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
        ) {
            String[] keys = new String[2];
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null)
                sb.append(line);
            JsonNode keyJson = new ObjectMapper().readTree(sb.toString());
            keys[0] = keyJson.get("discord_key").asText();
            keys[1] = keyJson.get("ytapi_key").asText();
            return keys;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return null;

    }
    
    public static final String getGoogleKey() {
        return ytkey;
    }

    public static final String getBotKey() {
        return botkey;
    }

}
