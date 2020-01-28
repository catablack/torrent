package com.torr.torrent.model;

import com.google.gen.Protobuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Created by catablack.
 */
@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
public class ChunkRequestTrace {
    Protobuf.ChunkResponse chunkResponse;
    List<ChunkResponseLog> chunkResponseLogList;
}
