package jp.kitsui87.discord.audio;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

/**
 * Class represents music.
 * 
 * To obtain the object of this class, use the static method provided in this class.
 * Music object will be in response of keeping track on current location in stream of data of 
 * the music being played.
 * 
 * Specified chunk of audio data of the music is retrieved by calling Music.getAudioData(int length).
 * This will return ByteBuffer object of containing the chunk of data in requested size at the current
 * location in audio stream offset, or returns null otherwise.
 * 
 * The offset will reset to the first location by calling rewind() method.
 * 
 * Each Music object that loadMusic() has been called must get finalized by calling Music.disposeStream()
 * to avoid resource leaking.
 * 
 * @author kistui#8381
 */
public class Music {
    
    private static YouTube clientAPI = null;

    public final long lengthSeconds;
    public final String musicURL;
    public final String title;

    public AudioFormat audioFormat = new AudioFormat(48000f, 16, 2, true, false);
    private InputStream rawStream = null;
    private AudioInputStream audioStream = null;
    private int currentBytesRead = 0;
    public boolean hasFinished = false;

    protected Music(long length, String title, String url) {
        this.lengthSeconds = length;
        this.title = title;
        this.musicURL = url;
    }

    /**
     * Gets the bitrate.
     * @return Bitrate of this music in kbps(Kilo Bit Per Second).
     */
    public int getAudioBitrate() {
        int sampleRate = (int) this.audioFormat.getSampleRate();
        int sampleSize = this.audioFormat.getSampleSizeInBits();
        int channels = this.audioFormat.getChannels();
        return sampleRate * sampleSize * channels;
    }

    /**
     * Initiates the download of this music.
     * 
     */
    public void loadMusic() {

        ProcessBuilder ffmpegBuilder = new ProcessBuilder(
            "ffmpeg", "-i", musicURL,
            "-f", "s16be",
            "-acodec", "pcm_s16be",
            "-ar", "48000",
            "-ac", "2",
            "pipe:1"
        );
        try {
            this.rawStream = new BufferedInputStream(ffmpegBuilder.start().getInputStream());
            this.audioStream = new AudioInputStream(this.rawStream, audioFormat, AudioSystem.NOT_SPECIFIED);
            this.audioStream.mark(Integer.MAX_VALUE);
            this.hasFinished = false;
        } catch (IOException e) {
            e.printStackTrace();
            this.disposeStream();
            return;
        }
        
    }

    public void rewind() {
        try {
            this.audioStream.reset();
            this.hasFinished = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ByteBuffer getAudioData(int byteLength) {

        byte[] b = new byte[byteLength];
        ByteBuffer buffer = ByteBuffer.allocate(byteLength);
        int bytesRead;
 
        try {
            bytesRead = this.audioStream.read(b);
            if (bytesRead == -1) {
                this.hasFinished = true;
                return null;
            }
            this.currentBytesRead += bytesRead;
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.put(b, 0, bytesRead);
            buffer.flip();
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("An error has been occured while retrieving data from the stream. This most likely to happen when the music has not been loaded.");
            return null;
        }

    }

    public boolean audioClosed() {
        return this.audioStream == null;
    }

    public float getPlayedTime() {

        int bitrate = this.getAudioBitrate();
        int bytePerSecond = bitrate / 8;
        return (float) this.currentBytesRead / (float) bytePerSecond;

    }

    public void disposeStream() {

        if (this.audioStream == null)
            return;

        try {
            this.audioStream.close();
            this.rawStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    
    /**
     * Extracts an audio data and creates Music instance from it.
     * 
     * Object being returned is an instance of Future class which allows the process being
     * done asynchronously, avoiding intrrupting the main process.
     * 
     * To get the result, call Future.get(). This call will block the main process to wait
     * until the retrieval finishes, then returns the Music object.
     * 
     * @param ytURL
     * @return Future object represents asynchrinous task retrieving the url.
     */
    public static Future<Music> getYouTubeAudio(String ytURL) {
        
        if (!ytURL.startsWith("https://www.youtube.com/watch?v="))
            return null;
        
        if (clientAPI == null) {
            try {
                clientAPI = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(), 
                    JacksonFactory.getDefaultInstance(), null).setApplicationName("app").build();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (clientAPI == null) {
                return null;
            }
        }

        long length = -1;
        String title = null;
        try {
            String id = ytURL.split("v=")[1];
            YouTube.Videos.List videoRequest = clientAPI.videos().list("snippet, contentDetails");
            System.out.println("ID: " + id);
            videoRequest.setId(id);
            videoRequest.setKey("AIzaSyCsHtyayRx1PPB14EVlRkOdYQcTR8lXYc8");
            VideoListResponse response = videoRequest.execute();
            if (!response.getItems().isEmpty()) {
                Video video = response.getItems().get(0);
                title = video.getSnippet().getTitle();
                String duration = video.getContentDetails().getDuration();
                length = Duration.parse(duration).getSeconds();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        final long f_length = length;
        final String f_title = title;
        return CompletableFuture.supplyAsync(() -> {

            ProcessBuilder builder = new ProcessBuilder("yt-dlp", "-g", ytURL);
            String result = null;
            try (
                InputStreamReader isr = new InputStreamReader(builder.start().getInputStream());
                BufferedReader br = new BufferedReader(isr);
            ) {
                String line;
                while ((line = br.readLine()) != null) result = line;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new Music(f_length, f_title, result);

        });

    }

}
