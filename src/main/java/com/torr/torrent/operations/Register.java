package com.torr.torrent.operations;

import com.google.gen.Protobuf;
import com.torr.torrent.Config;
import com.torr.torrent.service.SenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by catablack.
 */
@Component
public class Register {

    private static Logger LOG = LoggerFactory
            .getLogger(Register.class);

    @Autowired
    private SenderService sender;

    @Autowired
    private Config config;

    public void init() {
        LOG.info("Started REGISTRATION at " + config.getHubIp() + " on port " + config.getHubPort());
        try {
            Protobuf.RegistrationRequest request = Protobuf.RegistrationRequest.getDefaultInstance()
                    .newBuilderForType()
                    .setIndex(config.getNodeNumber())
                    .setOwner(config.getNodeName())
                    .setPort(config.getNodePort())
                    .build();
            Protobuf.Message registrationMessage = Protobuf.Message.newBuilder()
                    .setType(Protobuf.Message.Type.REGISTRATION_REQUEST)
                    .setRegistrationRequest(request).build();

            Protobuf.Message message = sender.sendMessage(registrationMessage, config.getHubIp(), config.getHubPort());
            Protobuf.RegistrationResponse response = message.getRegistrationResponse();
            if (response.getStatus() != Protobuf.Status.SUCCESS) {
                LOG.info("Status for register : " + response.getStatus());
                throw new RuntimeException("Application could not start");
            }
        } catch (IOException exc) {
            LOG.info("Could not send register message. " + exc.getMessage(), exc);
            throw new RuntimeException("Application could not start");
        }
    }
}
