package com.securitycam.interfaceobjects;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;

public class Asymmetric {

    private static final String RSA
            = "RSA";

    // Generating public and private keys
    // using RSA algorithm.
    public static KeyPair generateRSAKkeyPair()
            throws Exception
    {
        SecureRandom secureRandom
                = new SecureRandom();

        KeyPairGenerator keyPairGenerator
                = KeyPairGenerator.getInstance(RSA);

        keyPairGenerator.initialize(
                2048, secureRandom);

        return keyPairGenerator
                .generateKeyPair();
    }

    public String encrypt(String plainText) {
        String retVal = "";
        try {
            byte[] bytes = plainText.getBytes();
            AsymmetricCryptography ac = new AsymmetricCryptography();
            PublicKey publicKey = ac.getPublic("/etc/security-cam/id_rsa.pub");
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParameterSpecJCE = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParameterSpecJCE);
            byte[] result = cipher.doFinal(bytes, 0, bytes.length);
            byte[] b64 = java.util.Base64.getEncoder().encode(result);
            retVal = new String(b64, StandardCharsets.UTF_8);
        }
        catch (Exception ignore) {
        }
        return retVal;
    }

    public String decrypt(String base64) {
        String retVal = "";
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            AsymmetricCryptography ac = new AsymmetricCryptography();
            PrivateKey privateKey = ac.getPrivate("/etc/security-cam/id_rsa");
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParameterSpecJCE = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepParameterSpecJCE);
            byte[] result = cipher.doFinal(bytes, 0, bytes.length);
            retVal = new String(result, StandardCharsets.UTF_8);
        }
        catch (Exception ignore) {
        }
        return retVal;
    }
}
