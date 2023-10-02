package jp.kitsui87.discord;

import java.nio.ByteBuffer;
import java.util.List;

import jp.kitsui87.discord.audio.AudioPlayer;
import jp.kitsui87.discord.audio.Music;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class EventHandler implements EventListener {

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof MessageReceivedEvent)
            messageReceivedEvent((MessageReceivedEvent) event);
        else if (event instanceof SlashCommandInteractionEvent)
            commandEvent((SlashCommandInteractionEvent) event);
    }

    public void messageReceivedEvent(MessageReceivedEvent e) {
        if (e.getAuthor().isBot())
            return;
        
        if (e.getMessage().getContentDisplay().equals("connect")) {
            for (VoiceChannel vc : e.getGuild().getVoiceChannels()) {
                for (Member m : vc.getMembers()) {
                    if (m.getId().equals(e.getAuthor().getId())) {
                        e.getGuild().getAudioManager().openAudioConnection(vc);
                    }
                }
            }
        }
    }

    public void commandEvent(SlashCommandInteractionEvent e) {

        Guild server = e.getGuild();
        if (server == null) {
            e.reply("Commands in Private / Group DMs are not supported.").queue();
            return;
        }

        String serverID = e.getGuild().getId();
        if (!MyTubeCore.AUDIO_PLAYER.containsKey(serverID)) {
            MyTubeCore.AUDIO_PLAYER.put(serverID, new AudioPlayer());
        }
        AudioPlayer dedicatedPlayer = MyTubeCore.AUDIO_PLAYER.get(serverID);

        switch (e.getName()) {
        case "join":
            User caller = e.getUser();
            VoiceChannel targetVc = null;

            for (VoiceChannel vc : server.getVoiceChannels()) for (Member m : vc.getMembers())
                if (m.getId().equals(caller.getId())) targetVc = vc;

            // The command invoker is not in a visible vc
            if (targetVc == null) {
                e.reply("Please join in a visible voice channel in the server you run the command.").queue();
                return;
            }

            try {
                AudioSendHandler handler = new AudioSendHandler() {

                    ByteBuffer audioDataChunk = null;

                    @Override
                    public boolean canProvide() {
                        if (dedicatedPlayer.getState() == AudioPlayer.PAUSE)
                            return false;
                        Music m = dedicatedPlayer.getMusic((TextChannel)e.getChannel());
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

                server.getAudioManager().setSendingHandler(handler);
                server.getAudioManager().openAudioConnection(targetVc);

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Now playing:")
                        .setDescription("Click a button to \nperform an action.")
                        .setColor(java.awt.Color.BLUE)
                        .setFooter("Powered by JDA");


                e.replyEmbeds(embed.build()).queue();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            break;
        case "addsong":
            String url = e.getOption("youtube_url").getAsString();
            List<String> ids = Music.getYouTubeLinks(url);
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Song Added");
            InteractionHook action = e.replyEmbeds(builder.build()).complete();
            for (String id : ids) {
                dedicatedPlayer.add("https://www.youtube.com/watch?v=" + id, action, builder);
            }
            break;
        case "skip":
            Music m = dedicatedPlayer.getMusic((TextChannel)e.getChannel());
            if (m == null) {
                e.reply("No music is playing to skip you dumbass").queue();
                break;
            }
            e.reply("Skipping " + m.title).queue();
            dedicatedPlayer.skip();
            break;
        case "playpause":
            int state = dedicatedPlayer.getState();
            e.reply(state == AudioPlayer.PAUSE ? "Resuming..." : "Paused.").queue();
            dedicatedPlayer.setState(dedicatedPlayer.getState() == AudioPlayer.PAUSE ? AudioPlayer.PLAY : AudioPlayer.PAUSE);
            break;

        case "repeat":
            String mode = "";
            OptionMapping modeOp = e.getOption("mode");
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
            e.reply(response).queue();
            break;

        case "checkconnection":
            e.reply(server.getAudioManager().getConnectionStatus().toString()).queue();
            break;
        }
    }

    
}
