package io.github.afranusmani.urlshortener.service;

/**
 * Stateless Base62 codec.
 *
 * <p>Encoding a positive numeric id yields a compact, URL-safe short code with no
 * collision checks: id {@code 1 -> "1"}, {@code 62 -> "10"}, and so on. Kept as a
 * pure utility so it can be reused by JPA lifecycle callbacks as well as services.
 */
public final class Base62 {

    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62() {
    }

    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("value must be non-negative: " + value);
        }
        if (value == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long remaining = value;
        while (remaining > 0) {
            sb.append(ALPHABET.charAt((int) (remaining % BASE)));
            remaining /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String code) {
        long value = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = ALPHABET.indexOf(code.charAt(i));
            if (digit < 0) {
                throw new IllegalArgumentException("invalid Base62 character: " + code.charAt(i));
            }
            value = value * BASE + digit;
        }
        return value;
    }
}
