package com.torr.torrent.operations;

import com.google.gen.Protobuf;
import com.torr.torrent.Config;
import com.torr.torrent.model.ChunkRequestTrace;
import com.torr.torrent.model.ChunkResponseLog;
import com.torr.torrent.service.SenderService;
import com.torr.torrent.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by catablack.
 */
@Component
public class ReplicateRequest {

    private static Logger LOG = LoggerFactory.getLogger(ReplicateRequest.class);

    @Autowired
    private StorageService storageService;

    @Autowired
    private SubnetRequest subnetRequest;

    @Autowired
    private SenderService senderService;

    @Autowired
    private Config config;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public Protobuf.ReplicateResponse replicateFile(Protobuf.ReplicateRequest request) {

        Protobuf.FileInfo fileInfo = request.getFileInfo();

        if (fileInfo == null || fileInfo.getFilename() == null || fileInfo.getFilename().isEmpty()) {
            return makeResponse(Protobuf.Status.MESSAGE_ERROR);
        }

        if (storageService.fileExists(fileInfo.getFilename())) {
            return makeResponse(Protobuf.Status.SUCCESS);
        }

        Protobuf.SubnetResponse subnetResponse = subnetRequest.subnetRequest(request.getSubnetId());
        if (subnetResponse == null || subnetResponse.getStatus() != Protobuf.Status.SUCCESS) {
            return makeResponse(Protobuf.Status.PROCESSING_ERROR);
        }

        List<ChunkRequestTrace> requestResult = getChunks(fileInfo, subnetResponse);

        boolean allChunksFound = rebuildFile(fileInfo, requestResult);

        return makeResponse(requestResult, allChunksFound);
    }


    private List<ChunkRequestTrace> getChunks(Protobuf.FileInfo fileInfo, Protobuf.SubnetResponse subnetResponse) {
        List<ChunkRequestTrace> chunkRequestTraces = new ArrayList<>();
        List<Callable<ChunkRequestTrace>> callableList = new ArrayList<>();

        LOG.info("SUBNET: " + subnetResponse.getNodesList().toString());

        for (Protobuf.ChunkInfo chunkInfo : fileInfo.getChunksList()) {
            callableList.add(fetchChunk(subnetResponse, chunkInfo, fileInfo));
        }

        try {
            List<Future<ChunkRequestTrace>> futures = executorService.invokeAll(callableList);

            for (Future<ChunkRequestTrace> traceFuture : futures) {
                chunkRequestTraces.add(traceFuture.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return chunkRequestTraces;
    }

    private Callable<ChunkRequestTrace> fetchChunk(Protobuf.SubnetResponse subnetResponse, Protobuf.ChunkInfo chunkInfo, Protobuf.FileInfo fileInfo) {
        return () -> {
            ChunkRequestTrace chunkRequestTrace = new ChunkRequestTrace();
            List<ChunkResponseLog> chunkResponseLogs = new ArrayList<>();

            Protobuf.ChunkResponse.Builder chunkResponse = Protobuf.ChunkResponse.newBuilder();
            chunkResponse.setStatus(Protobuf.Status.UNABLE_TO_COMPLETE);

            for (Protobuf.NodeId nodeId : subnetResponse.getNodesList()) {
                // Skip the nodes that belong to this subnet
                if (nodeId.getOwner().equals(config.getNodeName()) && nodeId.getIndex() == config.getNodeNumber()) {
                    continue;
                }

                try {
                    Protobuf.Message response = senderService.sendMessage(getChunkRequestMessage(chunkInfo, fileInfo), nodeId.getHost(), nodeId.getPort());
                    Protobuf.ChunkResponse chunkResult = response.getChunkResponse();

                    chunkResponseLogs.add(new ChunkResponseLog(nodeId, chunkResult, chunkInfo));

                    if (chunkResult.getStatus() == Protobuf.Status.SUCCESS) {
                        chunkResponse.setStatus(Protobuf.Status.SUCCESS);
                        chunkResponse.setData(chunkResult.getData());
                        chunkRequestTrace.setChunkResponse(chunkResponse.build());
                        chunkRequestTrace.setChunkResponseLogList(chunkResponseLogs);
                        return chunkRequestTrace;

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    LOG.info("Node does not respond", nodeId.getOwner());
                }

            }

            chunkRequestTrace.setChunkResponse(chunkResponse.build());
            chunkRequestTrace.setChunkResponseLogList(chunkResponseLogs);
            return chunkRequestTrace;
        };
    }

    private Protobuf.Message getChunkRequestMessage(Protobuf.ChunkInfo chunkInfo, Protobuf.FileInfo fileInfo) {
        return Protobuf.Message.newBuilder()
                .setType(Protobuf.Message.Type.CHUNK_REQUEST)
                .setChunkRequest(
                        Protobuf.ChunkRequest
                                .newBuilder()
                                .setChunkIndex(chunkInfo.getIndex())
                                .setFileHash(fileInfo.getHash())
                                .build())
                .build();
    }

    private Protobuf.ReplicateResponse makeResponse(Protobuf.Status status) {
        return Protobuf.ReplicateResponse
                .newBuilder()
                .setStatus(status)
                .build();
    }

    private Protobuf.ReplicateResponse makeResponse(List<ChunkRequestTrace> requestResult, boolean allChunksFound) {
        List<Protobuf.NodeReplicationStatus> statuses = new ArrayList<>();
        Protobuf.ReplicateResponse.Builder builder = Protobuf.ReplicateResponse.newBuilder();
        LOG.info("All chunks found: " + allChunksFound);

        builder.setStatus(allChunksFound ? Protobuf.Status.SUCCESS : Protobuf.Status.UNABLE_TO_COMPLETE);

        for (ChunkRequestTrace res : requestResult) {
            for (ChunkResponseLog responseLog : res.getChunkResponseLogList()) {
                Protobuf.NodeReplicationStatus replicationStatus = Protobuf.NodeReplicationStatus.newBuilder()
                        .setNode(responseLog.getNodeId())
                        .setChunkIndex(responseLog.getChunkInfo().getIndex())
                        .setStatus(responseLog.getChunkResponse().getStatus())
                        .build();
                statuses.add(replicationStatus);
            }
        }

        builder.addAllNodeStatusList(statuses);
        Protobuf.ReplicateResponse replicateResponse = builder.build();
        LOG.info("Chunks_req: " + replicateResponse);
        return replicateResponse;
    }


    private boolean rebuildFile(Protobuf.FileInfo fileInfo, List<ChunkRequestTrace> requestResult) {
        byte[] data = new byte[fileInfo.getSize()];
        boolean allChunksFound = true;

        for (int i = 0; i < requestResult.size(); i++) {
            Protobuf.ChunkInfo chunkInfo = fileInfo.getChunksList().get(i);
            Protobuf.ChunkResponse chunkResponse = requestResult.get(i).getChunkResponse();
            if (chunkResponse.getStatus() != Protobuf.Status.SUCCESS) {
                allChunksFound = false;
            } else {
                byte[] responseData = chunkResponse.getData().toByteArray();
                if (allChunksFound) {
                    for (int c = i * 1024; c < i * 1024 + chunkInfo.getSize(); c++) {
                        data[c] = responseData[c - i * 1024];
                    }
                }
            }
        }

        if (allChunksFound) {
            storageService.storeFile(fileInfo.getFilename(), data);
        }

        return allChunksFound;
    }

}
