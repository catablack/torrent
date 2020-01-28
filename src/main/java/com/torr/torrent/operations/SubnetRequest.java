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
public class SubnetRequest {

    private static Logger LOG = LoggerFactory.getLogger(ReplicateRequest.class);

    @Autowired
    private SenderService senderService;

    @Autowired
    private Config config;

    public Protobuf.SubnetResponse subnetRequest(int subnetId) {
        Protobuf.SubnetResponse subnetResponse = null;
        try {
            Protobuf.Message response = senderService.sendMessage(getSubnetRequest(subnetId), config.getHubIp(), config.getHubPort());
            subnetResponse = response.getSubnetResponse();
            if (subnetResponse.getStatus() != Protobuf.Status.SUCCESS) {
                LOG.info("Subnet request error:" + subnetResponse.getErrorMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return subnetResponse;
    }

    private Protobuf.Message getSubnetRequest(int subnetId) {
        Protobuf.SubnetRequest request = Protobuf.SubnetRequest.getDefaultInstance()
                .newBuilderForType()
                .setSubnetId(subnetId)
                .build();
        return Protobuf.Message.newBuilder()
                .setType(Protobuf.Message.Type.SUBNET_REQUEST)
                .setSubnetRequest(request)
                .build();
    }

}
