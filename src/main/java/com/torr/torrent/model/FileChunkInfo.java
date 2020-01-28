package com.torr.torrent.model;

import com.google.gen.Protobuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by catablack.
 */
@AllArgsConstructor
@Getter
@Setter
public class FileChunkInfo {

    private Protobuf.ChunkInfo chunkInfo;
    private byte[] chunkData;
}
