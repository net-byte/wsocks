package org.netbyte.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * Cipher tool
 */
public class Cipher {
    /**
     * data xor
     *
     * @param src
     * @param key
     * @return
     */
    public static byte[] xor(byte[] src, byte[] key) {
        int keyLength = key.length;
        for (int i = 0; i < src.length; i++) {
            src[i] ^= key[i % keyLength];
        }
        return src;
    }

    /**
     * Generate 32 length key
     *
     * @param data
     * @return
     */
    public static byte[] generateKey(String data) {
        String sha256 = DigestUtils.sha256Hex(data.getBytes(StandardCharsets.UTF_8));
        return sha256.substring(0, 32).getBytes(StandardCharsets.UTF_8);
    }
}
