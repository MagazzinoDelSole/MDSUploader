package it.magazzinodelsole.mdsuploader;

import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFileUploader {

    /**
     * URL to call to upload the fle
     */
    private final String url;

    /**
     * Shared (between this client and the serial) use for the challenge response
     */
    private final String sharedKey;

    /**
     * Create a new fileuploader given the url and the key
     * @param url URL to call to upload the file
     * @param sharedKey Shared key for the challenge response
     */
    public MyFileUploader (String url, String sharedKey) {

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
        HttpURLConnection conn = (HttpURLConnection) new URL(createAuthorizedUrl()).openConnection();

        // Create the multipart
        MultipartEntity multipartEntity = new MultipartEntity();

        // Create the file body from the file to upload
        FileBody fileBody = new FileBody(fileToUpload);

        // Add the file
        multipartEntity.addPart("risultati", fileBody);

        // Set the content type
        conn.setRequestProperty("Content-Type", multipartEntity.getContentType().getValue());

        // Get the output stream
        OutputStream toServer = conn.getOutputStream();

        // Write the multipart
        multipartEntity.writeTo(toServer);

        // Flush and close the stream
        toServer.close();

        // Get the response
        int response = conn.getResponseCode();

        if (response != 200)
            throw new ChallengeFailedException();
    }

    public static class ChallengeFailedException extends Exception {

    }

    private String createAuthorizedUrl () {

        // Generate a new random string
        String random = Utils.generateRandomString();

        // Calculate the resulting key
        String resultKey = Utils.SHA1(random + sharedKey);

        return new StringBuilder().append(url)
                .append("?random=")
                .append(random)
                .append("&result")
                .append(resultKey)
                .toString();
    }
}