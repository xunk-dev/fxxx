import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Verify {
    static final String FP = "53a6d464bb615003eab4706fff8283344677b025df34f2bbaf2b233a84613956";
    static final String KEY = "33b1be7076452abde1397b46778eebc57f38ac03aa6649302de920a58208e896";

    static byte[] derive(byte[] fingerprint) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(fingerprint, "HmacSHA256"));
        mac.update("frameout.pro.payload.v1".getBytes(StandardCharsets.US_ASCII));
        mac.update((byte) 1);
        return mac.doFinal();
    }

    static byte[] open(byte[] blob, byte[] fingerprint) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(derive(fingerprint), "AES"), new GCMParameterSpec(128, blob, 4, 12));
        cipher.updateAAD(Arrays.copyOf(blob, 4));
        return cipher.doFinal(blob, 16, blob.length - 16);
    }

    static void check(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAILED: " + what);
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
        byte[] sealed = Files.readAllBytes(Path.of(args[0]));
        byte[] plain = Files.readAllBytes(Path.of(args[1]));
        List<String> rows = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(args[2]))) if (!line.isBlank()) rows.add(line.trim());

        byte[] fingerprint = HexFormat.of().parseHex(FP);
        check(Arrays.equals(derive(fingerprint), HexFormat.of().parseHex(KEY)), "key derivation vector");
        check(new String(sealed, 0, 4, StandardCharsets.US_ASCII).equals("FXPE"), "sealed magic");
        check(new String(plain, 0, 4, StandardCharsets.US_ASCII).equals("FXPP"), "plain magic");

        byte[] payload = open(sealed, fingerprint);
        check(Arrays.equals(payload, Arrays.copyOfRange(plain, 4, plain.length)), "sealed payload equals plain payload");
        check(new String(sealed, StandardCharsets.ISO_8859_1).indexOf("tealorange") < 0, "no plaintext ids in sealed asset");

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
        check(in.readUnsignedShort() == rows.size(), "entry count");
        for (String row : rows) {
            String[] parts = row.split("\\s+");
            byte[] id = new byte[in.readUnsignedByte()];
            in.readFully(id);
            check(new String(id, StandardCharsets.UTF_8).equals(parts[0]), "id " + parts[0]);
            for (int i = 1; i <= 20; i++) {
                check(Float.floatToIntBits(in.readFloat()) == Float.floatToIntBits(Float.parseFloat(parts[i])), parts[0] + " value " + i);
            }
        }
        check(in.available() == 0, "no trailing bytes");

        byte[] other = fingerprint.clone();
        other[0] ^= 1;
        try {
            open(sealed, other);
            check(false, "different fingerprint must not decrypt");
        } catch (AEADBadTagException expected) {
        }
        System.out.println("OK: Gradle task output matches the runtime key derivation vector");
    }
}
