package jp.kitsui87.discord.audio;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.InteractionHook;

/**
 * Class represents the player for the discord bot.
 * 
 * @author kitsui#8381
 */
public class AudioPlayer {

    private final List<Music> musicPool;
    private Music current = null;
    private int index = 0;
    private int state = PLAY;
    private int repeatMode = REP_ONCE;
    private final ExecutorService asyncExecutor;
    
    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int REP_ONCE = 2;
    public static final int REP_LIST = 3;
    public static final int REP_SONG = 4;

    public AudioPlayer() {
        this.musicPool = Collections.synchronizedList(new ArrayList<>());
        this.asyncExecutor = Executors.newCachedThreadPool();
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getRepeat() {
        return this.repeatMode;
    }

    public void setRepeat(int mode) {
        this.repeatMode = mode;
    }

    public Music getMusic(TextChannel channel) {

        if (this.musicPool.size() == 0)
            return null;

        // this is true only when it is being played at the first time
        if (this.current == null) {
            this.current = this.musicPool.get(index);
            this.current.openDownloadStream();
        }
        
        // this is true when each music has reached the end of the stream
        else if (this.current.hasFinished) {
            this.index++;
            switch (this.repeatMode) {
                case REP_ONCE:
                    this.current.closeDownloadStream();
                    if (this.index == this.musicPool.size()) {
                        this.index = 0;
                        this.state = PAUSE;
                        this.current = null;
                        channel.sendMessage(
                            "Playlist reached the end. use /playpause to play the list." +
                            "You can use /playlist repeat to repeat this playsist without pauses."
                        ).queue();
                        return null;
                    } else {
                        this.current = this.musicPool.get(index);
                        this.current.openDownloadStream();
                    }
                    break;
                case REP_LIST:
                    this.current.closeDownloadStream();
                    if (this.index == this.musicPool.size())
                        this.index = 0;
                    this.current = this.musicPool.get(index);
                    this.current.openDownloadStream();
                    break;
                case REP_SONG:
                    this.current.closeDownloadStream();
                    this.current.openDownloadStream();
            }
        }
        // skips above to here when the music is in middle of play
        
        return this.current;
    }

    public void add(String audioUrl, InteractionHook progressReport, EmbedBuilder progressEmbed) {

        this.asyncExecutor.execute(() -> {
            Music m = Music.getYouTubeAudio(audioUrl);
            
            synchronized (progressEmbed) {
                progressEmbed.getDescriptionBuilder().append((m == null ? "A music failed to add" : "Added " + m.title) + "\n");
                progressReport.editOriginalEmbeds(progressEmbed.build()).queue();
            }
            synchronized (this.musicPool) {
                this.musicPool.add(m);
            }
        });

    }

    public void skip() {
        this.current.hasFinished = true;
    }

    public void play() {

    }

    public void pause() {

    }

    

}