package it.magazzinodelsole.mdsuploader;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Utils {

    /**
     * Convert to hex
     * @param data Data to convert
     * @return Result
     */
    private static String convertToHex(byte[] data) {

        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < data.length; i++) {

            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {

                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Compute the sha-1 of a specified string
     * @param text String to hash
     * @return hash value of the string
     */
    public static String SHA1(String text){

        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");

        } catch(NoSuchAlgorithmException ex){

            return "";
        }
        byte[] sha1hash = new byte[40];
        try {

            md.update(text.getBytes("iso-8859-1"), 0, text.length());
        } catch (Exception ex){

            return "";
        }
        sha1hash = md.digest();

        return convertToHex(sha1hash);
    }

    /**
     * Gnerate a new random string
     * @return Random string
     */
    public static String generateRandomString(){
        String str = "";
        for(int i = 0;i < 10;i++)
            str += new Random().nextInt();
        return str;
    }
}
