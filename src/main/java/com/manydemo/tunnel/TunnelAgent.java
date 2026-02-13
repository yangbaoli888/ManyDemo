package com.manydemo.tunnel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TunnelAgent {
    private final String serverHost;
    private final int controlPort;
    private final int dataPort;
    private final String tunnelId;
    private final String targetHost;
    private final int targetPort;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TunnelAgent(String serverHost, int controlPort, int dataPort, String tunnelId, String targetHost, int targetPort) {
        this.serverHost = serverHost;
        this.controlPort = controlPort;
        this.dataPort = dataPort;
        this.tunnelId = tunnelId;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    public void start() throws IOException {
        try (Socket controlSocket = new Socket(serverHost, controlPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            writer.write("HELLO " + tunnelId + "\n");
            writer.flush();

            String ack = reader.readLine();
            if (!"OK".equals(ack)) {
                throw new IOException("register failed: " + ack);
            }

            System.out.printf("[A] Tunnel registered. id=%s target=%s:%d%n", tunnelId, targetHost, targetPort);
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("CONNECT ")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length < 2) {
                    continue;
                }
                String sessionId = parts[1];
                executor.submit(() -> serveSession(sessionId));
            }
        }
    }

    private void serveSession(String sessionId) {
        try {
            Socket relayDataSocket = new Socket(serverHost, dataPort);
            Socket localTargetSocket = new Socket(targetHost, targetPort);

            BufferedWriter relayWriter = new BufferedWriter(new OutputStreamWriter(relayDataSocket.getOutputStream(), StandardCharsets.UTF_8));
            relayWriter.write("DATA " + sessionId + "\n");
            relayWriter.flush();

            System.out.printf("[A] Session connected. id=%s%n", sessionId);
            RelayUtil.bridge(relayDataSocket, localTargetSocket, executor);
        } catch (IOException e) {
            System.err.printf("[A] Session failed. id=%s err=%s%n", sessionId, e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            System.out.println("Usage: TunnelAgent <serverHost> <controlPort> <dataPort> <tunnelId> <targetHost> <targetPort>");
            return;
        }
        TunnelAgent agent = new TunnelAgent(
                args[0],
                Integer.parseInt(args[1]),
                Integer.parseInt(args[2]),
                args[3],
                args[4],
                Integer.parseInt(args[5])
        );
        agent.start();
    }
}
