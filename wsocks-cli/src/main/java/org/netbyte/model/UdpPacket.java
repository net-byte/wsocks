package org.netbyte.model;

import java.io.Serializable;
import java.util.Arrays;

/**
 * UDP packet for proxying
 */
public class UdpPacket implements Serializable {
    private byte[] header;
    private byte[] data;
    private String fromHost;
    private Integer fromPort;
    private String toHost;
    private Integer toPort;

    public UdpPacket(){

    }

    public UdpPacket(byte[] header, byte[] data, String fromHost, Integer fromPort, String toHost, Integer toPort) {
        this.header = header;
        this.data = data;
        this.fromHost = fromHost;
        this.fromPort = fromPort;
        this.toHost = toHost;
        this.toPort = toPort;
    }

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public String getFromHost() {
        return fromHost;
    }

    public void setFromHost(String fromHost) {
        this.fromHost = fromHost;
    }

    public Integer getFromPort() {
        return fromPort;
    }

    public void setFromPort(Integer fromPort) {
        this.fromPort = fromPort;
    }

    public String getToHost() {
        return toHost;
    }

    public void setToHost(String toHost) {
        this.toHost = toHost;
    }

    public Integer getToPort() {
        return toPort;
    }

    public void setToPort(Integer toPort) {
        this.toPort = toPort;
    }

    @Override
    public String toString() {
        return "UdpPacket{" +
                "header=" + Arrays.toString(header) +
                ", data=" + Arrays.toString(data) +
                ", fromHost='" + fromHost + '\'' +
                ", fromPort=" + fromPort +
                ", toHost='" + toHost + '\'' +
                ", toPort=" + toPort +
                '}';
    }
}
