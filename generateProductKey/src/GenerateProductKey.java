import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.PublicKey;

public class GenerateProductKey {
    public static void main(String[] args) throws Exception {
        final Profanities prof = new Profanities();
        ProductKeyGen pkg = new ProductKeyGen();

        AsymmetricCryptography ac = new AsymmetricCryptography();
//        PrivateKey privateKey = ac.getPrivate("KeyPair/privateKey");
        PublicKey publicKey = ac.getPublic("publicKey");

        String key = "";
        do {
            key = pkg.generate(16, '-', 4);
        } while (prof.containsAny(key));

        String encrypted_msg = ac.encryptText(key, publicKey);
//        String decrypted_msg = ac.decryptText(encrypted_msg, privateKey);
        System.out.println("Product key: " + key);

        // Write the encrypted product key to the prodKey file
        Files.write(new File("/etc/security-cam/prodKey").toPath(), encrypted_msg.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);



 //               + "\nDecrypted key: " + decrypted_msg);

//        if (new File("KeyPair/text.txt").exists()) {
//            ac.encryptFile(ac.getFileInBytes(new File("KeyPair/text.txt")),
//                    new File("KeyPair/text_encrypted.txt"),publicKey);
//            ac.decryptFile(ac.getFileInBytes(new File("KeyPair/text_encrypted.txt")),
//                    new File("KeyPair/text_decrypted.txt"), privateKey);
//        } else {
//            System.out.println("Create a file text.txt under folder KeyPair");
//        }
    }
}
