package com.torr.torrent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by catablack.
 */
@Configuration
@ConfigurationProperties(prefix = "prop")

public class Config {

    private String nodeName;
    private int nodePort;
    private int nodeNumber;
    private String hubIp;
    private int hubPort;

    public int getHubPort() {
        return hubPort;
    }

    public void setHubPort(int hubPort) {
        this.hubPort = hubPort;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public int getNodeNumber() {
        return nodeNumber;
    }

    public void setNodeNumber(int nodeNumber) {
        this.nodeNumber = nodeNumber;
    }

    public String getHubIp() {
        return hubIp;
    }

    public void setHubIp(String hubIp) {
        this.hubIp = hubIp;
    }
}
