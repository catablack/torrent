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
public class ChunkRequest {

    @Autowired
    StorageService storageService;

    public Protobuf.ChunkResponse chunkRequest(Protobuf.ChunkRequest request) {
        if (request.getFileHash() == null) {
            return getResponse(Protobuf.Status.MESSAGE_ERROR);
        }

        byte[] fileHash = request.getFileHash().toByteArray();

        if (fileHash == null) {
            return getResponse(Protobuf.Status.MESSAGE_ERROR);
        }

        int index = request.getChunkIndex();
        if (index < 0) {
            return getResponse(Protobuf.Status.MESSAGE_ERROR);
        }

        Protobuf.FileInfo fileInfo = storageService.getByHash(fileHash);
        if (fileInfo == null) {
            return getResponse(Protobuf.Status.UNABLE_TO_COMPLETE);
        }

        byte[] chunkData = storageService.getChunkByIndex(fileInfo.getFilename(), index);
        if (chunkData == null) {
            return getResponse(Protobuf.Status.UNABLE_TO_COMPLETE);
        }

        return getResponse(ByteString.copyFrom(chunkData));
    }

    private Protobuf.ChunkResponse getResponse(Protobuf.Status status) {
        return Protobuf.ChunkResponse.newBuilder()
                .setStatus(status)
                .build();
    }

    private Protobuf.ChunkResponse getResponse(ByteString data) {
        return Protobuf.ChunkResponse.newBuilder()
                .setStatus(Protobuf.Status.SUCCESS)
                .setData(data)
                .build();
    }

}
