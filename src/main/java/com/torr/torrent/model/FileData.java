package com.torr.torrent.model;

import com.google.gen.Protobuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Created by catablack.
 */
@Getter
@Setter
@AllArgsConstructor
@Builder
public class FileData {

    private Protobuf.FileInfo fileInfo;
    private List<FileChunkInfo> chunkInfos;
}
