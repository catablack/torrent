package com.torr.torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(Config.class)
public class TorrentApplication {

    private static Logger LOG = LoggerFactory
            .getLogger(TorrentApplication.class);

    public static void main(String[] args) {
        LOG.info("Starting server...");
        SpringApplication.run(TorrentApplication.class, args);
    }
}
