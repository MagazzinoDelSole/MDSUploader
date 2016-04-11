package it.magazzinodelsole.mdsuploader;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

public class MyFileUploader {

    /**
     * URL to call to upload the fle
     */
    private final URL url;

    /**
     * Shared (between this client and the serial) use for the challenge response
     */
    private final String sharedKey;

    /**
     * Create a new fileuploader given the url and the key
     * @param url URL to call to upload the file
     * @param sharedKey Shared key for the challenge response
     */
    public MyFileUploader (URL url, String sharedKey) {

        this.url = url;
        this.sharedKey = sharedKey;
    }

    /**
     * Upload the specified file to the server
     * @param fileToUpload File to upload
     * @throws IOException Thrown if the file read or the connection fails
     * @throws ChallengeFailedException Thrown if the shared key isn't valid
     */
    public void uploadFile (File fileToUpload) throws IOException, ChallengeFailedException {

        // Open the connection
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        // Prepare the connection
        conn.setDoInput(true);
        conn.setDoOutput(true);

        // Set the header for the multi part request
        conn.setRequestProperty("ENCTYPE", "multipart/form-data");
        //conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + BOUNDARY);

        // Get the output stream
        OutputStream toServer = conn.getOutputStream();

        // Flush and close the stream
        toServer.close();

        // Get the response from the server
        int response = conn.getResponseCode();

        if (response != 200)
            throw new ChallengeFailedException();

        // Close the connection
        conn.disconnect();
    }

    public static Class ChallengeFailedException extends Exception { }

}