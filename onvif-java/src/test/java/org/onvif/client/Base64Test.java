package org.onvif.client;

import java.util.Base64;

public class Base64Test {


    public static void main(String[] args) {
        String base64String = "4AUTXXJ214OBnsdeYBXatQ==";

        // decode Base64
        byte[] decodedBytes = Base64.getDecoder().decode(base64String);

        // turn byte[] to hex
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : decodedBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexStringBuilder.append('0');
            }
            hexStringBuilder.append(hex);
        }
        String hexString = hexStringBuilder.toString();

        System.out.println("hex：" + hexString);
    }

}
