package me.rbrickis.ssuploader.utils;

import java.util.Base64;

public class EncodingUtils {

    /**
     * @param image - The bytes of the image
     *
     * @return A base64 encoded string.
     */
    public static String toBase64(byte[] image) {
       return Base64.getEncoder().encodeToString(image);
    }

}
