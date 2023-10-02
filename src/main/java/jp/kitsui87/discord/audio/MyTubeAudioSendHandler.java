package jp.kitsui87.discord.audio;

import java.nio.ByteBuffer;

import jp.kitsui87.discord.MyTubeCore;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MyTubeAudioSendHandler implements AudioSendHandler {

    private ByteBuffer audioDataChunk = null;
    private final AudioPlayer dedicatedPlayer;

    public MyTubeAudioSendHandler(AudioPlayer player) {
        this.dedicatedPlayer = player;
    }

    @Override
    public boolean canProvide() {
        if (dedicatedPlayer.getState() == AudioPlayer.PAUSE)
            return false;
        Music m = null;//dedicatedPlayer.getMusic((TextChannel)e.getChannel());
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
    
}
