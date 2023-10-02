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
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoListResponse;

import jp.kitsui87.discord.MyTubeCore;

/**
 * Class represents music that provides low level functionality to play an audio data.
 * Basically an instance of this class is an AudioInputStream
 * 
 * @author kistui#8381
 */
public class Music {
    
    /* TODO: Static volatility */
    private static YouTube clientAPI = null;
    static {
        try {
            clientAPI = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(), 
                JacksonFactory.getDefaultInstance(), null).setApplicationName("app").build();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public final long lengthSeconds;
    public final String musicURL;
    public final String title;

    public AudioFormat audioFormat = new AudioFormat(48000f, 16, 2, true, false);
    private AudioInputStream audioStream = null;
    private int currentBytesRead = 0;
    // Flag used to close the download stream
    private boolean streamTerminate = true;
    // Flas used to indicate if stream has reached the end
    public boolean hasFinished = false;

    protected Music(long length, String title, String url) {
        this.lengthSeconds = length;
        this.title = title;
        this.musicURL = url;
    }

    /**
     * Gets the bitrate.
     * @return Bitrate of this music in kbps.
     */
    public int getAudioBitrate() {
        int sampleRate = (int) this.audioFormat.getSampleRate();
        int sampleSize = this.audioFormat.getSampleSizeInBits();
        int channels = this.audioFormat.getChannels();
        return sampleRate * sampleSize * channels;
    }

    /**
     * Initiates the download of this music.
     * This method must get called for getAudioData() to extract data.
     */
    public void openDownloadStream() {

        if (audioStream != null)
            return;
        
        Thread asyncThread = new Thread(() -> {
            ProcessBuilder ffmpegBuilder = new ProcessBuilder(
                "ffmpeg", "-i", musicURL,
                "-f", "s16be",
                "-acodec", "pcm_s16be",
                "-ar", "48000",
                "-ac", "2",
                "pipe:1"
            );
            Process ffmpeg;
            try {
                ffmpeg = ffmpegBuilder.start();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try (
                InputStream is = new BufferedInputStream(ffmpeg.getInputStream());
                AudioInputStream audioStream = new AudioInputStream(is, this.audioFormat, AudioSystem.NOT_SPECIFIED);
            ) {
                Music.this.audioStream = audioStream;
                Music.this.streamTerminate = false;
                Music.this.currentBytesRead = 0;
                Music.this.hasFinished = false;
                while (!Music.this.streamTerminate) {
                    Thread.sleep(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ffmpeg.destroyForcibly();
                Music.this.audioStream = null;
            }
            
        });
        asyncThread.start();

    }

    /**
     * Closes the download stream.
     * By the end of this method process any resources that is nessessary to obtain audio data become void.
     */
    public synchronized void closeDownloadStream() {
       
        if (this.audioStream == null)
            return;
        this.streamTerminate = true;
        while (this.audioStream != null)
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

    }

    /**
     * Retrieves the specified size of audio data contained in a byte buffer from the audio stream.
     * The download stream must first get opened before call of this method.
     * 
     * @param byteLength
     * @return
     */
    public synchronized ByteBuffer getAudioData(int byteLength) {

        if (this.audioStream == null)
            return null;

        byte[] b = new byte[byteLength];
        ByteBuffer buffer = ByteBuffer.allocate(byteLength);
        int bytesRead;
 
        try {
            bytesRead = this.audioStream.read(b);
            System.out.println(bytesRead + " : " + this.getPlayedTime());
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
            return null;
        }

    }

    public float getPlayedTime() {

        int bitrate = this.getAudioBitrate();
        int bytePerSecond = bitrate / 8;
        return (float) this.currentBytesRead / (float) bytePerSecond;

    }
    
    /**
     * Extracts an audio data and creates Music instance from it.
     * 
     * @param ytURL  URL to the single video.
     * @return Music
     */
    public static Music getYouTubeAudio(String ytURL) {
        
        if (!(ytURL.startsWith("https://www.youtube.com") && (ytURL.contains("v="))))
            return null;

        if (clientAPI == null) {
            return null;
        }

        long length = -1;
        String title = null;
        try {
            String id = ytURL.split("v=")[1];
            YouTube.Videos.List videoRequest = clientAPI.videos().list("snippet, contentDetails");
            System.out.println("ID: " + id);
            videoRequest.setId(id);
            videoRequest.setKey(MyTubeCore.getGoogleKey());
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

        ProcessBuilder builder = new ProcessBuilder("yt-dlp", "-g", ytURL);
        String result = null;
        Process ytdlp = null;
        try {
            ytdlp = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        try (
            InputStreamReader isr = new InputStreamReader(ytdlp.getInputStream());
            BufferedReader br = new BufferedReader(isr);
        ) {
            String line;
            while ((line = br.readLine()) != null)
                result = line;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ytdlp.destroy();
        }
        return new Music(length, title, result);

    }

    /**
     * Retrieves the youtube link from the url provided.
     * If the link is direct to a single video, return List is size of 1 and will
     * contain the ID of the video.
     * If the link is to a play list, it will return the List of video IDs inside
     * the playlist.
     */
    public static List<String> getYouTubeLinks(String url) {

        if (clientAPI == null)
            return null;

        String id;
        if (url.startsWith("https://www.youtube.com/"))
            if (url.contains("list=")) {
                id = url.split("list=")[1];
                try {
                    YouTube.PlaylistItems.List playListRequest = clientAPI.playlistItems().list("snippet");
                    playListRequest.setPlaylistId(id);
                    playListRequest.setKey(MyTubeCore.getGoogleKey());
                    String nextPageToken = "";
                    List<String> videoIds = new ArrayList<>();
                    do {
                        playListRequest.setPageToken(nextPageToken);
                        PlaylistItemListResponse playlistItemsResponse = playListRequest.execute();
                        List<PlaylistItem> playlistItems = playlistItemsResponse.getItems();
                        for (PlaylistItem playlistItem : playlistItems) {
                            ResourceId resourceId = playlistItem.getSnippet().getResourceId();
                            if (resourceId.getKind().equals("youtube#video")) {
                                videoIds.add(resourceId.getVideoId());
                            }
                        }
                        nextPageToken = playlistItemsResponse.getNextPageToken();
                    } while (nextPageToken != null);

                    return videoIds;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                List<String> l = new ArrayList<>();
                l.add(url.split("v=")[1]);
                return l;
            }
        else
            return null;
    }

}
