package com.torr.torrent.service;

import com.google.gen.Protobuf;
import com.google.protobuf.ByteString;
import com.torr.torrent.model.FileChunkInfo;
import com.torr.torrent.model.FileData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by catablack.
 */
@Service
public class StorageService {

    private static Logger LOG = LoggerFactory
            .getLogger(StorageService.class);

    private Map<String, FileData> files = new HashMap<>();

    private List<FileChunkInfo> splitIntoChunks(byte[] data) {

        List<FileChunkInfo> chunks = new ArrayList<>();

        int chunk = 1024; // chunk size to divide
        int index = 0;
        for (int i = 0; i < data.length; i += chunk) {
            byte[] chunkList = Arrays.copyOfRange(data, i, Math.min(data.length, i + chunk));
            Protobuf.ChunkInfo chunkInfo = Protobuf.ChunkInfo.getDefaultInstance()
                    .newBuilderForType()
                    .setIndex(index)
                    .setSize(chunkList.length)
                    .setHash(getHash(chunkList))
                    .build();
            chunks.add(new FileChunkInfo(chunkInfo, chunkList));
            index++;
        }

        return chunks;
    }

    private ByteString getHash(byte[] chunkList) {
        try {
            byte[] md5 = MessageDigest.getInstance("MD5").digest(chunkList);
            return ByteString.copyFrom(md5);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Protobuf.FileInfo storeFile(String name, byte[] data) {
        List<FileChunkInfo> fileChunkInfos = splitIntoChunks(data);
        List<Protobuf.ChunkInfo> chunks = fileChunkInfos.stream().map(chunk -> chunk.getChunkInfo()).collect(Collectors.toList());

        Protobuf.FileInfo fileInfo = Protobuf.FileInfo.getDefaultInstance()
                .newBuilderForType()
                .setFilename(name)
                .setHash(getHash(data))
                .setSize(data.length)
                .addAllChunks(fileChunkInfos.stream().map(chunk -> chunk.getChunkInfo()).collect(Collectors.toList()))
                .build();

        FileData fileData = new FileData(fileInfo, fileChunkInfos);
        files.put(name, fileData);

        return fileInfo;
    }


    public boolean fileExists(String name) {
        return files.containsKey(name);
    }

    public Protobuf.FileInfo getByHash(byte[] fileHash) {
        Protobuf.FileInfo result = null;
        for (Protobuf.FileInfo fileInfo : files.values().stream().map(f -> f.getFileInfo()).collect(Collectors.toList())) {
            if (Arrays.equals(fileInfo.getHash().toByteArray(), fileHash)) {
                result = fileInfo;
                break;
            }
        }

        return result;
    }

    public byte[] getChunkByIndex(String filename, int index) {
        List<FileChunkInfo> chunkInfos = files.get(filename).getChunkInfos();

        if (chunkInfos == null) {
            return null;
        }

        for (FileChunkInfo chunk : chunkInfos) {
            if (chunk.getChunkInfo().getIndex() == index) {
                return chunk.getChunkData();
            }
        }
        return null;
    }

    public List<Protobuf.FileInfo> getByRegex(String regex) {
        List<Protobuf.FileInfo> fileInfos = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        for (String fileName : files.keySet()) {
            Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
                fileInfos.add(files.get(fileName).getFileInfo());
            }
        }
        return fileInfos;
    }

    public byte[] getFileData(String filename) {
        byte[] data = null;
        FileData fileData = files.get(filename);
        if (filename != null) {
            int index = 0;
            data = new byte[fileData.getFileInfo().getSize()];
            for (FileChunkInfo chunkInfo : fileData.getChunkInfos()) {
                for (int i = index * 1024; i < index * 1024 + chunkInfo.getChunkInfo().getSize(); i++) {
                    data[i] = chunkInfo.getChunkData()[i - index * 1024];
                }
                index++;
            }
        }

        return data;
    }
}
