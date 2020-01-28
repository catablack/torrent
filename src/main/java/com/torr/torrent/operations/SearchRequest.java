package com.torr.torrent.operations;

import com.google.gen.Protobuf;
import com.torr.torrent.Config;
import com.torr.torrent.service.SenderService;
import com.torr.torrent.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * Created by catablack.
 */
@Component
public class SearchRequest {

    private static Logger LOG = LoggerFactory
            .getLogger(SearchRequest.class);

    @Autowired
    private StorageService storageService;

    @Autowired
    private SenderService senderService;

    @Autowired
    private Config config;

    @Autowired
    private SubnetRequest subnetRequest;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public Protobuf.SearchResponse searchRequest(Protobuf.SearchRequest searchRequest) {

        String regex = searchRequest.getRegex();
        Pattern pattern = Pattern.compile(regex);
        if (pattern == null) {
            return getResponse(Protobuf.Status.MESSAGE_ERROR);
        }

        Protobuf.SubnetResponse subnetResponse = subnetRequest.subnetRequest(searchRequest.getSubnetId());
//        LOG.info("SUUBNET:" + subnetResponse.getNodesList().toString());
        if (subnetResponse == null || subnetResponse.getStatus() != Protobuf.Status.SUCCESS) {
            return getResponse(Protobuf.Status.PROCESSING_ERROR);
        }

        List<Protobuf.NodeSearchResult> res = searchOnNodes(regex, subnetResponse);

        return Protobuf.SearchResponse.newBuilder()
                .setStatus(Protobuf.Status.SUCCESS)
                .addAllResults(res)
                .build();

    }

    private List<Protobuf.NodeSearchResult> searchOnNodes(String regex, Protobuf.SubnetResponse subnetResponse) {
        List<Protobuf.NodeSearchResult> nodeSearchResults = new ArrayList<>();
        List<Callable<Protobuf.NodeSearchResult>> callableList = new ArrayList<>();

        for (Protobuf.NodeId nodeId : subnetResponse.getNodesList()) {
            if (nodeId.getOwner() == config.getNodeName() && nodeId.getIndex() == config.getNodeNumber()) {
                List<Protobuf.FileInfo> localSearch = storageService.getByRegex(regex);
                nodeSearchResults.add(
                        Protobuf.NodeSearchResult.newBuilder()
                                .setNode(nodeId)
                                .setStatus(Protobuf.Status.SUCCESS)
                                .addAllFiles(localSearch)
                                .build());
            } else {
                callableList.add(invokeSearchOnNode(regex, nodeId));
            }
        }

        try {
            List<Future<Protobuf.NodeSearchResult>> futures = executorService.invokeAll(callableList);
            for (Future<Protobuf.NodeSearchResult> future : futures) {
                nodeSearchResults.add(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return nodeSearchResults;
    }

    private Callable<Protobuf.NodeSearchResult> invokeSearchOnNode(String regex, Protobuf.NodeId nodeId) {
        return () -> {
            Protobuf.NodeSearchResult.Builder response = Protobuf.NodeSearchResult.newBuilder();
            response.setNode(nodeId);

            Protobuf.Message message = senderService.sendMessage(
                    Protobuf.Message.newBuilder()
                            .setType(Protobuf.Message.Type.LOCAL_SEARCH_REQUEST)
                            .setLocalSearchRequest(
                                    Protobuf.LocalSearchRequest
                                            .newBuilder()
                                            .setRegex(regex)
                                            .build())
                            .build(),
                    nodeId.getHost(),
                    nodeId.getPort());
            Protobuf.LocalSearchResponse localSearchResponse = message.getLocalSearchResponse();
            if (response.getStatus() != Protobuf.Status.SUCCESS) {
                LOG.info("Local search for node " + nodeId.getOwner() + "-" + nodeId.getIndex() + " returned with status " + localSearchResponse.getStatus().name());
            } else {
                response.addAllFiles(localSearchResponse.getFileInfoList());
            }
            response.setStatus(localSearchResponse.getStatus());

            return response.build();
        };
    }

    private Protobuf.SearchResponse getResponse(Protobuf.Status status) {
        return Protobuf.SearchResponse.newBuilder()
                .setStatus(status)
                .build();
    }
}
