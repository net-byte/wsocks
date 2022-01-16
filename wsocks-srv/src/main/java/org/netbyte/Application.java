package org.netbyte;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.netbyte.proxy.WebSocketServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import javax.annotation.Resource;
import java.text.NumberFormat;


@SpringBootApplication
public class Application {
    private static final Log logger = LogFactory.getLog(Application.class);
    @Resource
    private WebSocketServer webSocketServer;

    public static void main(String[] args) {
        System.setProperty("io.netty.leakDetectionLevel", "advanced");
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener() {
        Runtime runtime = Runtime.getRuntime();
        final NumberFormat format = NumberFormat.getInstance();
        final long maxMemory = runtime.maxMemory();
        final long allocatedMemory = runtime.totalMemory();
        final long freeMemory = runtime.freeMemory();
        final long mb = 1024 * 1024;
        final String mega = " MB";
        logger.info("========================== Memory Info ==========================");
        logger.info("Free memory: " + format.format(freeMemory / mb) + mega);
        logger.info("Allocated memory: " + format.format(allocatedMemory / mb) + mega);
        logger.info("Max memory: " + format.format(maxMemory / mb) + mega);
        logger.info("Total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / mb) + mega);
        logger.info("=================================================================\n");
        return applicationReadyEvent -> webSocketServer.start();
    }
}