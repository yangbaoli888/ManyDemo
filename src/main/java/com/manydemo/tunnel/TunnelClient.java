package com.manydemo.tunnel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A/C 通用客户端：
 * 1) 长连到 B 注册 clientId
 * 2) 收到 CONNECT 时回连 B(data) 并连接本地服务 exposedHost:exposedPort
 * 3) 本地监听 localListenPort，接入后向 B 发 OPEN targetClientId
 */
public class TunnelClient {
    private final String serverHost;
    private final int controlPort;
    private final int relayPort;
    private final int dataPort;

    private final String clientId;
    private final String targetClientId;

    private final String exposedHost;
    private final int exposedPort;
    private final int localListenPort;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TunnelClient(String serverHost,
                        int controlPort,
                        int relayPort,
                        int dataPort,
                        String clientId,
                        String targetClientId,
                        String exposedHost,
                        int exposedPort,
                        int localListenPort) {
        this.serverHost = serverHost;
        this.controlPort = controlPort;
        this.relayPort = relayPort;
        this.dataPort = dataPort;
        this.clientId = clientId;
        this.targetClientId = targetClientId;
        this.exposedHost = exposedHost;
        this.exposedPort = exposedPort;
        this.localListenPort = localListenPort;
    }

    public void start() throws IOException {
        executor.submit(this::startLocalListener);
        startControlLoop();
    }

    private void startControlLoop() throws IOException {
        try (Socket controlSocket = new Socket(serverHost, controlPort);
             BufferedReader reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            writer.write("CLIENT " + clientId + "\n");
            writer.flush();

            String ack = reader.readLine();
            if (!"OK".equals(ack)) {
                throw new IOException("register failed: " + ack);
            }

            System.out.printf("[Client-%s] registered, expose %s:%d, local proxy :%d -> %s%n",
                    clientId, exposedHost, exposedPort, localListenPort, targetClientId);

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
                executor.submit(() -> serveIncomingSession(sessionId));
            }
        }
    }

    private void serveIncomingSession(String sessionId) {
        try {
            Socket dataSocket = new Socket(serverHost, dataPort);
            Socket localServiceSocket = new Socket(exposedHost, exposedPort);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.UTF_8));
            writer.write("DATA " + sessionId + "\n");
            writer.flush();

            RelayUtil.bridge(dataSocket, localServiceSocket, executor);
        } catch (IOException e) {
            System.err.printf("[Client-%s] incoming session failed session=%s err=%s%n", clientId, sessionId, e.getMessage());
        }
    }

    private void startLocalListener() {
        try (ServerSocket localServer = new ServerSocket(localListenPort)) {
            while (true) {
                Socket localAppSocket = localServer.accept();
                executor.submit(() -> openToTarget(localAppSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("local listener stopped", e);
        }
    }

    private void openToTarget(Socket localAppSocket) {
        try {
            Socket relaySocket = new Socket(serverHost, relayPort);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(relaySocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(relaySocket.getInputStream(), StandardCharsets.UTF_8));

            writer.write("OPEN " + targetClientId + "\n");
            writer.flush();

            String response = reader.readLine();
            if (response == null || !response.startsWith("WAIT")) {
                throw new IOException("open failed: " + response);
            }

            RelayUtil.bridge(localAppSocket, relaySocket, executor);
        } catch (IOException e) {
            RelayUtil.closeQuietly(localAppSocket);
            System.err.printf("[Client-%s] open target=%s failed err=%s%n", clientId, targetClientId, e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 9) {
            System.out.println("Usage: TunnelClient <serverHost> <controlPort> <relayPort> <dataPort> <clientId> <targetClientId> <exposedHost> <exposedPort> <localListenPort>");
            return;
        }

        TunnelClient client = new TunnelClient(
                args[0],
                Integer.parseInt(args[1]),
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3]),
                args[4],
                args[5],
                args[6],
                Integer.parseInt(args[7]),
                Integer.parseInt(args[8])
        );
        client.start();
    }
}
