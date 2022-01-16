package org.netbyte.model;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.io.Serializable;

/**
 * The proxy addr object
 */
public class ProxyAddr implements Serializable {
    /**
     * The host of proxy address
     */
    @JsonAlias("Host")
    private String host;
    /**
     * The port of proxy address
     */
    @JsonAlias("Port")
    private String port;
    /**
     * The encryption key
     */
    @JsonAlias("Key")
    private String key;
    /**
     * The network of proxy address(tcp or udp)
     */
    @JsonAlias("Network")
    private String network;
    /**
     * The connection timestamp string
     */
    @JsonAlias("Timestamp")
    private String timestamp;
    /**
     * The connection random string
     */
    @JsonAlias("Random")
    private String random;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }

    @Override
    public String toString() {
        return "ProxyAddr{" +
                "host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", key='" + key + '\'' +
                ", network='" + network + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", random='" + random + '\'' +
                '}';
    }
}
