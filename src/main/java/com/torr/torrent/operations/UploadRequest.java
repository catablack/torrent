package com.torr.torrent.operations;

import com.google.gen.Protobuf;
import com.torr.torrent.Config;
import com.torr.torrent.service.SenderService;
import com.torr.torrent.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by catablack.
 */
@Component
public class UploadRequest {

    private static Logger LOG = LoggerFactory
            .getLogger(UploadRequest.class);
    @Autowired
    private SenderService senderService;
    @Autowired
    private Config config;
    @Autowired
    private StorageService storageService;

    public Protobuf.UploadResponse uploadRequest(Protobuf.UploadRequest uploadRequest) {
        LOG.info("Data :" + uploadRequest.getData().size());
        if (uploadRequest.getFilename() == null || uploadRequest.getFilename().isEmpty()) {
            return Protobuf.UploadResponse.getDefaultInstance()
                    .newBuilderForType()
                    .setStatus(Protobuf.Status.MESSAGE_ERROR)
                    .setFileInfo((Protobuf.FileInfo) null)
                    .build();
        }

        Protobuf.FileInfo fileInfo = storageService.storeFile(uploadRequest.getFilename(), uploadRequest.getData().toByteArray());
        LOG.info("STORED FILE: " + fileInfo.toString());
        return
                Protobuf.UploadResponse.getDefaultInstance()
                        .newBuilderForType()
                        .setStatus(Protobuf.Status.SUCCESS)
                        .setFileInfo(fileInfo)
                        .build();
    }
}
