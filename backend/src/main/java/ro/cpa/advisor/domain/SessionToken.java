package ro.cpa.advisor.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Tokenul de sesiune: 256 de biti aleatorii, de neghicit. Baza de date pastreaza doar hash-ul SHA-256, deci o scurgere
 * a bazei nu da acces la sesiuni. Fiind aleatoriu pe 256 de biti, tokenul nu are nevoie de sare sau de un hash lent
 * (cum au parolele alese de oameni).
 */
public final class SessionToken {

    private static final String PREFIX = "cpa_";
    private static final Pattern SHAPE = Pattern.compile("^cpa_[A-Za-z0-9_-]{43}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionToken() {}

    public static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Verificare ieftina de forma, inainte de hash si de interogarea bazei; un sir oarecare nu ajunge in baza. */
    public static boolean hasValidShape(String token) {
        return token != null && SHAPE.matcher(token).matches();
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is guaranteed by the JDK", e);
        }
    }
}
