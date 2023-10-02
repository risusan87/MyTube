package jp.kitsui87.discord.audio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

public class MusicTest {
    
    public static void main(String[] args) throws Exception {

        //String musicURL = "https://rr1---sn-gvbxgn-tvf6.googlevideo.com/videoplayback?expire=1694950433&ei=wY8GZZXUIa6P_9EP7vyDkAk&ip=2607%3Afea8%3Abe1a%3A4900%3A199b%3A1e44%3A723e%3A215&id=o-APmxQBWrnW_ikQ-io1Dkjz0aP2ygHRMfOtDtQUAXqEgM&itag=251&source=youtube&requiressl=yes&mh=NO&mm=31%2C29&mn=sn-gvbxgn-tvf6%2Csn-gvbxgn-tt1ee&ms=au%2Crdu&mv=m&mvi=1&pl=41&initcwndbps=2082500&spc=UWF9f0wz3cermjKC4Gr3rtd43Y6B-Sc&vprv=1&svpuc=1&mime=audio%2Fwebm&gir=yes&clen=3509294&dur=225.781&lmt=1681758817828389&mt=1694928567&fvip=5&keepalive=yes&fexp=24007246&beids=24350018&c=ANDROID&txp=4532434&sparams=expire%2Cei%2Cip%2Cid%2Citag%2Csource%2Crequiressl%2Cspc%2Cvprv%2Csvpuc%2Cmime%2Cgir%2Cclen%2Cdur%2Clmt&sig=AOq0QJ8wRgIhAMtqSOVrTx9opaObSPYictIkIKxFI5-VZvFAaGTvJ9VQAiEAne0TdBoBwkvgG1M9FVZxtGJIGFRYC_KBtl4WXP0twQs%3D&lsparams=mh%2Cmm%2Cmn%2Cms%2Cmv%2Cmvi%2Cpl%2Cinitcwndbps&lsig=AG3C_xAwRgIhAK3Uqr7LcxRFP_cn2wI140e9yHNY0jAl1SonwP6x4rpVAiEAkMjL-yF8CS0Fk76AIXM1hj4M_yKWLGf8e8R8THC01dU%3D";
        
        ProcessBuilder builder = new ProcessBuilder("yt-dlp", "-g", "https://www.youtube.com/watch?v=ZRtdQ81jPUQ");
        String musicURL = null;
        Process ytdlp = null;
        System.out.println("Converting YouTube URL...");
        try {
            ytdlp = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (
            InputStreamReader isr = new InputStreamReader(ytdlp.getInputStream());
            BufferedReader br = new BufferedReader(isr);
        ) {
            String line;
            while ((line = br.readLine()) != null)
                musicURL = line;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ytdlp.destroy();
        }
        System.out.println("YT conversion done with exit code: " + ytdlp.waitFor());

        URL url = new URL(musicURL);
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(new File("out.webm"));
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();

    }
}
