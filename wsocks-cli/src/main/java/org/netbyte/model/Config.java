package org.netbyte.model;

import org.netbyte.utils.Cipher;

import java.io.Serializable;

/**
 * The config object
 */
public class Config implements Serializable {
    private String localhost;
    private Integer localPort;
    private String host;
    private Integer port;
    private String key;
    private byte[] cipherKey;
    private Boolean obfs;
    private String path;
    private String scheme;

    public Config(String localhost, Integer localPort, String host, Integer port, String key, Boolean obfs, String path, String scheme) {
        this.localhost = localhost;
        this.localPort = localPort;
        this.host = host;
        this.port = port;
        this.key = key;
        this.cipherKey = Cipher.generateKey(key);
        this.obfs = obfs;
        this.path = path;
        this.scheme = scheme;
    }

    public String getLocalhost() {
        return localhost;
    }

    public void setLocalhost(String localhost) {
        this.localhost = localhost;
    }

    public Integer getLocalPort() {
        return localPort;
    }

    public void setLocalPort(Integer localPort) {
        this.localPort = localPort;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }
}
