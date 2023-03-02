package jp.kitsui87.discord.audio;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

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
    
    public static final int PLAY = 0;
    public static final int PAUSE = 1;
    public static final int REP_ONCE = 2;
    public static final int REP_LIST = 3;
    public static final int REP_SONG = 4;

    public AudioPlayer() {
        musicPool = new ArrayList<>();
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Music getMusic(TextChannel channel) {

        if (this.musicPool.size() == 0)
            return null;

        // this is true only when it is being played at the first time
        if (this.current == null) {
            this.current = this.musicPool.get(index);
            this.current.loadMusic();
        }
        
        // this is true when each music has reached the end of the stream
        else if (this.current.hasFinished) {
            this.index++;
            switch (this.repeatMode) {
                case REP_ONCE:
                    this.current.disposeStream();
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
                        this.current.loadMusic();
                    }
                    break;
                case REP_LIST:
                    this.current.disposeStream();
                    if (this.index == this.musicPool.size())
                        this.index = 0;
                    this.current = this.musicPool.get(index);
                    this.current.loadMusic();
                    break;
                case REP_SONG:
                    this.current.rewind();
            }
            channel.sendMessage("Now playing " + this.current.title).queue();
        }
        // skips above to here when the music is in middle of play
        
        return this.current;
    }

    public Music add(String URL) {
        Music m;
        try {
            m = Music.getYouTubeAudio(URL).get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        if (m == null)
            return null;
        this.musicPool.add(m);
        return m;
    }

    public void skip() {
        this.current.hasFinished = true;
    }

    public void play() {

    }

    public void pause() {

    }

    

}