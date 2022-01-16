package org.netbyte.model;

import org.netbyte.utils.Cipher;

import java.io.Serializable;

/**
 * The config object
 */
public class Config implements Serializable {
    private String key;
    private byte[] cipherKey;
    private Boolean obfs;
    private String path;

    public Config(String key, Boolean obfs, String path) {
        this.key = key;
        this.cipherKey = Cipher.generateKey(key);
        this.obfs = obfs;
        this.path = path;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getCipherKey() {
        return cipherKey;
    }

    public void setCipherKey(byte[] cipherKey) {
        this.cipherKey = cipherKey;
    }

    public Boolean getObfs() {
        return obfs;
    }

    public void setObfs(Boolean obfs) {
        this.obfs = obfs;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}