package org.netbyte.utils;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.DatagramPacket;
import org.netbyte.model.UdpPacket;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class SocksServerUtils {

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    public static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * joinByteArray
     *
     * @param byte1
     * @param byte2
     * @return
     */
    public static byte[] joinByteArray(byte[] byte1, byte[] byte2) {
        return ByteBuffer.allocate(byte1.length + byte2.length)
                .put(byte1)
                .put(byte2)
                .array();

    }

    /**
     * Parse the UDP packet
     * +----+------+------+----------+----------+----------+
     * |RSV | FRAG | ATYP | DST.ADDR | DST.PORT |   DATA   |
     * +----+------+------+----------+----------+----------+
     * |  2 |   1  |   1  | Variable |     2    | Variable |
     * +----+------+------+----------+----------+----------+
     *
     * @param packet
     * @return
     */
    public static UdpPacket parseUdpPacket(DatagramPacket packet) throws Exception {
        byte[] b = ByteBufUtil.getBytes(packet.content());
        if (b[2] != 0x00) {
            // not support frag
            return null;
        }
        UdpPacket udpPacket = new UdpPacket();
        switch (b[3]) {
            case 0x01:
                InetAddress ipv4 = InetAddress.getByAddress(Arrays.copyOfRange(b, 4, 8));
                udpPacket.setToHost(ipv4.getHostAddress());
                udpPacket.setToPort((b[8]) << 8 | (b[9]));
                udpPacket.setHeader(Arrays.copyOfRange(b, 0, 10));
                udpPacket.setData(Arrays.copyOfRange(b, 10, b.length));
                udpPacket.setFromHost(packet.sender().getHostString());
                udpPacket.setFromPort(packet.sender().getPort());
                break;
            case 0x03:
                int domainLength = b[4];
                InetAddress domain = InetAddress.getByAddress(Arrays.copyOfRange(b, 5, 5 + domainLength));
                udpPacket.setToHost(domain.getHostAddress());
                udpPacket.setToPort((b[5 + domainLength]) << 8 | (b[6 + domainLength]));
                udpPacket.setHeader(Arrays.copyOfRange(b, 0, 7 + domainLength));
                udpPacket.setData(Arrays.copyOfRange(b, 7 + domainLength, b.length));
                udpPacket.setFromHost(packet.sender().getHostString());
                udpPacket.setFromPort(packet.sender().getPort());
                break;
            case 0x04:
                InetAddress ipv6 = InetAddress.getByAddress(Arrays.copyOfRange(b, 4, 20));
                udpPacket.setToHost(ipv6.getHostAddress());
                udpPacket.setToPort((b[20]) << 8 | (b[21]));
                udpPacket.setHeader(Arrays.copyOfRange(b, 0, 22));
                udpPacket.setData(Arrays.copyOfRange(b, 22, b.length));
                udpPacket.setFromHost(packet.sender().getHostString());
                udpPacket.setFromPort(packet.sender().getPort());
                break;
            default:
                return null;
        }
        return udpPacket;
    }

    private SocksServerUtils() {
    }
}