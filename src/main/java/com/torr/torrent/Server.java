package com.torr.torrent;

import com.google.gen.Protobuf;
import com.google.protobuf.InvalidProtocolBufferException;
import com.torr.torrent.operations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by catablack.
 */
@Component
public class Server {

    private static Logger LOG = LoggerFactory
            .getLogger(Server.class);

    @Autowired
    private Config config;

    @Autowired
    private Register register;

    @Autowired
    private UploadRequest uploadRequest;

    @Autowired
    private ReplicateRequest replicateRequest;

    @Autowired
    private ChunkRequest chunkRequest;

    @Autowired
    private LocalSearch localSearch;

    @Autowired
    private DownloadRequest downloadRequest;

    @Autowired
    private SearchRequest searchRequest;

    private ServerSocket server = null;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        try {
            server = new ServerSocket(config.getNodePort());
            LOG.info("Server started on port: " + config.getNodePort());
            register.init();
            while (true) {
                Socket socket = server.accept();
                LOG.info("Client accepted: " + socket.getInetAddress().toString() + ":" + socket.getPort());

                Future<Boolean> res = executorService.submit(handleClient(socket));

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (server != null) {
                server.close();
            }
        }
    }

    private Callable<Boolean> handleClient(Socket socket) throws IOException {

        return () -> {
            DataInputStream inputStream = null;
            DataOutputStream outputStream = null;

            try {
                inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                int length = inputStream.readInt();
                byte[] mb = new byte[length];
                inputStream.readFully(mb, 0, mb.length); // read the message
                Protobuf.Message request = Protobuf.Message.parseFrom(mb);
                LOG.info("Received message " + request.toString());

                processRequest(request, socket, outputStream);
                return true;
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {

                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }

                if (socket != null) {
                    socket.close();
                }
            }
            return false;
        };
    }

    private void processRequest(Protobuf.Message request, Socket socket, DataOutputStream outputStream) {
        Protobuf.Message response = null;

        if (request.getType() == Protobuf.Message.Type.UPLOAD_REQUEST) {
            Protobuf.UploadResponse uploadResponse = uploadRequest.uploadRequest(request.getUploadRequest());
            response = Protobuf.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Protobuf.Message.Type.UPLOAD_RESPONSE)
                    .setUploadResponse(uploadResponse)
                    .build();
        }

        if (request.getType() == Protobuf.Message.Type.REPLICATE_REQUEST) {
            Protobuf.ReplicateResponse replicateResponse = replicateRequest.replicateFile(request.getReplicateRequest());
            response = Protobuf.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Protobuf.Message.Type.REPLICATE_RESPONSE)
                    .setReplicateResponse(replicateResponse)
                    .build();
        }

        if (request.getType() == Protobuf.Message.Type.CHUNK_REQUEST) {
            Protobuf.ChunkResponse chunkResponse = chunkRequest.chunkRequest(request.getChunkRequest());
            response = Protobuf.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Protobuf.Message.Type.CHUNK_RESPONSE)
                    .setChunkResponse(chunkResponse)
                    .build();
        }

        if (request.getType() == Protobuf.Message.Type.LOCAL_SEARCH_REQUEST) {
            Protobuf.LocalSearchResponse localSearchResponse = localSearch.localSearchRequest(request.getLocalSearchRequest());
            response = Protobuf.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Protobuf.Message.Type.LOCAL_SEARCH_RESPONSE)
                    .setLocalSearchResponse(localSearchResponse)
                    .build();
        }

        if (request.getType() == Protobuf.Message.Type.DOWNLOAD_REQUEST) {
            Protobuf.DownloadResponse downloadResponse = downloadRequest.downloadRequest(request.getDownloadRequest());
            response = Protobuf.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Protobuf.Message.Type.DOWNLOAD_RESPONSE)
                    .setDownloadResponse(downloadResponse)
                    .build();
        }

        if (request.getType() == Protobuf.Message.Type.SEARCH_REQUEST) {
            Protobuf.SearchResponse searchResponse = searchRequest.searchRequest(request.getSearchRequest());
            response = Protobuf.Message.getDefaultInstance()
                    .newBuilderForType()
                    .setType(Protobuf.Message.Type.SEARCH_RESPONSE)
                    .setSearchResponse(searchResponse)
                    .build();
        }

        if (response != null) {
            try {

                byte[] m = response.toByteArray();
                int len = m.length;
                outputStream.writeInt(len);
                outputStream.write(m);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
