package com.torr.torrent.operations;

import com.google.gen.Protobuf;
import com.torr.torrent.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by catablack.
 */
@Component
public class LocalSearch {

    @Autowired
    private StorageService storageService;

    public Protobuf.LocalSearchResponse localSearchRequest(Protobuf.LocalSearchRequest localSearchRequest) {
        String regex = localSearchRequest.getRegex();
        if (regex == null || regex.isEmpty()) {
            return Protobuf.LocalSearchResponse.getDefaultInstance()
                    .newBuilderForType()
                    .setStatus(Protobuf.Status.MESSAGE_ERROR)
                    .build();
        }

        List<Protobuf.FileInfo> matches = storageService.getByRegex(regex);
        return Protobuf.LocalSearchResponse.getDefaultInstance()
                .newBuilderForType()
                .setStatus(Protobuf.Status.SUCCESS)
                .addAllFileInfo(matches)
                .build();
    }
}
