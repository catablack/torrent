package com.torr.torrent.model;

import com.google.gen.Protobuf;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by catablack.
 */
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ChunkResponseLog {
    Protobuf.NodeId nodeId;
    Protobuf.ChunkResponse chunkResponse;
    Protobuf.ChunkInfo chunkInfo;

}
