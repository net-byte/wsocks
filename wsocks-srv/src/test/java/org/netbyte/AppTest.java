package org.netbyte;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.netbyte.utils.Cipher;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void testCipher() {
        String encode = new String(Cipher.xor("{\"name\":\"123\"}".getBytes(StandardCharsets.UTF_8), "123456".getBytes(StandardCharsets.UTF_8)));
        String decode = new String(Cipher.xor(encode.getBytes(StandardCharsets.UTF_8), "123456".getBytes(StandardCharsets.UTF_8)));
        assertTrue(Objects.equals("{\"name\":\"123\"}", decode));
    }

    @Test
    public void testCipher2() {
        String encode = new String(Cipher.xor("{\"name\":\"123\"}".getBytes(StandardCharsets.UTF_8), Cipher.generateKey("123456")));
        String decode = new String(Cipher.xor(encode.getBytes(StandardCharsets.UTF_8), Cipher.generateKey("123456")));
        assertTrue(Objects.equals("{\"name\":\"123\"}", decode));
    }

}
