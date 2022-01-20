package org.netbyte.model;

import io.netty.handler.traffic.TrafficCounter;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Counter for traffic
 */
public class Counter {
    /**
     * The netty traffic counter
     */
    public static TrafficCounter trafficCounter;
    /**
     * The total connections
     */
    public static AtomicLong totalConnections = new AtomicLong(0);
    /**
     * The current connections
     */
    public static AtomicLong currentConnections = new AtomicLong(0);

    /**
     * Format byte
     *
     * @param bytes
     * @return
     */
    public static String formatByte(long bytes) {
        double kiloByte = Long.valueOf(bytes).doubleValue() / 1024;
        if (kiloByte < 1) {
            return bytes + " Byte";
        }
        BigDecimal result;
        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            result = new BigDecimal(Double.toString(kiloByte));
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            result = new BigDecimal(Double.toString(megaByte));
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            result = new BigDecimal(Double.toString(gigaByte));
            return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " GB";
        }
        result = new BigDecimal(teraBytes);
        return result.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " TB";
    }
}
