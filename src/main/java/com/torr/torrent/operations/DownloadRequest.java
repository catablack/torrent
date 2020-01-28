package com.torr.torrent.operations;

import com.google.gen.Protobuf;
import com.google.protobuf.ByteString;
import com.torr.torrent.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by catablack.
 */
@Component
public class DownloadRequest {

    @Autowired
    private StorageService storageService;

    public Protobuf.DownloadResponse downloadRequest(Protobuf.DownloadRequest downloadRequest) {
        byte[] fileHash = downloadRequest.getFileHash().toByteArray();
        if (fileHash == null) {
            return Protobuf.DownloadResponse.newBuilder()
                    .setStatus(Protobuf.Status.MESSAGE_ERROR)
                    .build();
        }

        Protobuf.FileInfo fileInfo = storageService.getByHash(fileHash);
        if (fileInfo == null) {
            return Protobuf.DownloadResponse.newBuilder()
                    .setStatus(Protobuf.Status.UNABLE_TO_COMPLETE)
                    .build();
        }

        byte[] data = storageService.getFileData(fileInfo.getFilename());
        if (data == null) {
            return Protobuf.DownloadResponse.newBuilder()
                    .setStatus(Protobuf.Status.UNABLE_TO_COMPLETE)
                    .build();
        }

        return Protobuf.DownloadResponse.newBuilder()
                .setStatus(Protobuf.Status.SUCCESS)
                .setData(ByteString.copyFrom(data))
                .build();
    }
}
