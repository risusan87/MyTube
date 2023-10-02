package jp.kitsui87.discord.audio;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;

public class YoutubeClient {
    
    private final YouTube clientApi;
    
    public YoutubeClient() throws GeneralSecurityException, IOException {
        this.clientApi = new YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(), 
            JacksonFactory.getDefaultInstance(), 
            null).setApplicationName("app"
        ).build();
    }   

}
