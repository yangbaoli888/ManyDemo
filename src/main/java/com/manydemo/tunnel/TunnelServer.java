package com.manydemo.tunnel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * B 机器：中心协调与转发服务。
 *
 * 协议：
 * 1) 客户端控制连接： CLIENT <clientId>      -> OK
 * 2) 主动发起方数据入口： OPEN <targetClientId> -> WAIT <sessionId> | ERR <reason>
 * 3) 被访问方回连数据： DATA <sessionId>
 */
public class TunnelServer {
    private final int controlPort;
    private final int relayPort;
    private final int dataPort;

    private final Map<String, ControlSession> clients = new ConcurrentHashMap<>();
    private final Map<String, Socket> pendingInitiators = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TunnelServer(int controlPort, int relayPort, int dataPort) {
        this.controlPort = controlPort;
        this.relayPort = relayPort;
        this.dataPort = dataPort;
    }

    public void start() {
        System.out.printf("[B] server started control=%d relay=%d data=%d%n", controlPort, relayPort, dataPort);
        executor.submit(this::startControlListener);
        executor.submit(this::startRelayListener);
        executor.submit(this::startDataListener);
    }

    private void startControlListener() {
        try (ServerSocket serverSocket = new ServerSocket(controlPort)) {
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(() -> handleControlConnection(socket));
            }
        } catch (IOException e) {
            throw new RuntimeException("control listener stopped", e);
        }
    }

    private void handleControlConnection(Socket socket) {
        String clientId = null;
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine();
            if (line == null || !line.startsWith("CLIENT ")) {
                return;
            }

            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                return;
            }

            clientId = parts[1];
            clients.put(clientId, new ControlSession(clientId, writer));
            writer.write("OK\n");
            writer.flush();
            System.out.printf("[B] client online: %s%n", clientId);

            while (reader.readLine() != null) {
                // 心跳可扩展
            }
        } catch (IOException ignored) {
            // disconnected
        } finally {
            if (clientId != null) {
                clients.remove(clientId);
                System.out.printf("[B] client offline: %s%n", clientId);
            }
        }
    }

    private void startRelayListener() {
        try (ServerSocket serverSocket = new ServerSocket(relayPort)) {
            while (true) {
                Socket initiatorSocket = serverSocket.accept();
                executor.submit(() -> handleOpenRequest(initiatorSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("relay listener stopped", e);
        }
    }

    private void handleOpenRequest(Socket initiatorSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(initiatorSocket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(initiatorSocket.getOutputStream(), StandardCharsets.UTF_8));

            String line = reader.readLine();
            if (line == null || !line.startsWith("OPEN ")) {
                RelayUtil.closeQuietly(initiatorSocket);
                return;
            }

            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                RelayUtil.closeQuietly(initiatorSocket);
                return;
            }

            String targetClientId = parts[1];
            ControlSession target = clients.get(targetClientId);
            if (target == null) {
                writer.write("ERR target_offline\n");
                writer.flush();
                RelayUtil.closeQuietly(initiatorSocket);
                return;
            }

            String sessionId = UUID.randomUUID().toString();
            pendingInitiators.put(sessionId, initiatorSocket);
            writer.write("WAIT " + sessionId + "\n");
            writer.flush();

            target.sendConnect(sessionId);
            System.out.printf("[B] request routed target=%s session=%s%n", targetClientId, sessionId);
        } catch (IOException e) {
            RelayUtil.closeQuietly(initiatorSocket);
        }
    }

    private void startDataListener() {
        try (ServerSocket serverSocket = new ServerSocket(dataPort)) {
            while (true) {
                Socket dataSocket = serverSocket.accept();
                executor.submit(() -> handleDataSocket(dataSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("data listener stopped", e);
        }
    }

    private void handleDataSocket(Socket dataSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.UTF_8));
            String line = reader.readLine();
            if (line == null || !line.startsWith("DATA ")) {
                RelayUtil.closeQuietly(dataSocket);
                return;
            }

            String[] parts = line.split("\\s+");
            if (parts.length < 2) {
                RelayUtil.closeQuietly(dataSocket);
                return;
            }

            String sessionId = parts[1];
            Socket initiatorSocket = pendingInitiators.remove(sessionId);
            if (initiatorSocket == null) {
                RelayUtil.closeQuietly(dataSocket);
                return;
            }

            System.out.printf("[B] relay established session=%s%n", sessionId);
            RelayUtil.bridge(initiatorSocket, dataSocket, executor);
        } catch (IOException e) {
            RelayUtil.closeQuietly(dataSocket);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: TunnelServer <controlPort> <relayPort> <dataPort>");
            return;
        }

        TunnelServer server = new TunnelServer(
                Integer.parseInt(args[0]),
                Integer.parseInt(args[1]),
                Integer.parseInt(args[2])
        );
        server.start();
    }

    private static final class ControlSession {
        private final String clientId;
        private final BufferedWriter writer;

        private ControlSession(String clientId, BufferedWriter writer) {
            this.clientId = clientId;
            this.writer = writer;
        }

        private synchronized void sendConnect(String sessionId) throws IOException {
            writer.write("CONNECT " + sessionId + "\n");
            writer.flush();
            System.out.printf("[B] notified client=%s session=%s%n", clientId, sessionId);
        }
    }
}
