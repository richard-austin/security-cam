package org.onvif.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AuthTest {

    public static void main(String[] args) throws Exception {

        String date = "2024-03-06T06:04:30Z";
        String nonce = "gHV4Y/cV8KE7OSY7+qGv0g==";
        String password = "abcd1234";

        try {
            // 1. Base64 decode the nonce
            byte[] decodedNonce = Base64.getDecoder().decode(nonce);

            // 2. Concatenate decodedNonce, date, and password
            byte[] dateBytes = date.getBytes();
            byte[] passwordBytes = password.getBytes();
            byte[] toBeHashed = new byte[decodedNonce.length + dateBytes.length + passwordBytes.length];

            System.arraycopy(decodedNonce, 0, toBeHashed, 0, decodedNonce.length);
            System.arraycopy(dateBytes, 0, toBeHashed, decodedNonce.length, dateBytes.length);
            System.arraycopy(passwordBytes, 0, toBeHashed, decodedNonce.length + dateBytes.length, passwordBytes.length);

            // 3. SHA-1 hash the concatenated bytes
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] sha1Hash = digest.digest(toBeHashed);

            // 4. Base64 encode the SHA-1 hash
            String digestBase64 = Base64.getEncoder().encodeToString(sha1Hash);

            System.out.println("Digest: " + digestBase64);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
