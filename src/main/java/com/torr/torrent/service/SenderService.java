package com.torr.torrent.service;

import com.google.gen.Protobuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by catablack.
 */
@Service
public class SenderService {

    private static Logger LOG = LoggerFactory
            .getLogger(SenderService.class);

    public Protobuf.Message sendMessage(Protobuf.Message message, String ip, int port) throws IOException {
        byte[] m = message.toByteArray();
        int len = m.length; // 32-bit integer

        DataOutputStream outputStream = null;
        DataInputStream inputStream = null;
        Socket clientSocket = null;
        Protobuf.Message response = null;
        try {
            LOG.info("Sending message to " + ip + ":" + port);
            clientSocket = new Socket(ip, port);
            outputStream = new DataOutputStream(clientSocket.getOutputStream());
            outputStream.writeInt(len);
            outputStream.write(m);
            LOG.info("Sent message " + message.toString());

            inputStream = new DataInputStream(clientSocket.getInputStream());
            int length = inputStream.readInt();
            byte[] mb = new byte[length];
            inputStream.readFully(mb, 0, mb.length); // read the message
            response = Protobuf.Message.parseFrom(mb);
            LOG.info("Received message " + response.toString());
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
        return response;
    }
}
