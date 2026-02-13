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

public class TunnelServer {
    private final int controlPort;
    private final int relayPort;
    private final int dataPort;

    private final Map<String, ControlSession> tunnels = new ConcurrentHashMap<>();
    private final Map<String, Socket> pendingVisitors = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TunnelServer(int controlPort, int relayPort, int dataPort) {
        this.controlPort = controlPort;
        this.relayPort = relayPort;
        this.dataPort = dataPort;
    }

    public void start() throws IOException {
        System.out.printf("[B] TunnelServer started. control=%d relay=%d data=%d%n", controlPort, relayPort, dataPort);
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
        String tunnelId = null;
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            String hello = reader.readLine();
            if (hello == null || !hello.startsWith("HELLO ")) {
                return;
            }

            String[] parts = hello.split("\\s+");
            if (parts.length < 2) {
                return;
            }
            tunnelId = parts[1];
            tunnels.put(tunnelId, new ControlSession(tunnelId, writer));
            writer.write("OK\n");
            writer.flush();
            System.out.printf("[B] Tunnel registered: %s%n", tunnelId);

            while (reader.readLine() != null) {
                // keep alive; no extra command for now
            }
        } catch (IOException ignored) {
            // disconnected
        } finally {
            if (tunnelId != null) {
                tunnels.remove(tunnelId);
                System.out.printf("[B] Tunnel removed: %s%n", tunnelId);
            }
        }
    }

    private void startRelayListener() {
        try (ServerSocket serverSocket = new ServerSocket(relayPort)) {
            while (true) {
                Socket visitorSocket = serverSocket.accept();
                executor.submit(() -> handleVisitor(visitorSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException("relay listener stopped", e);
        }
    }

    private void handleVisitor(Socket visitorSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(visitorSocket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(visitorSocket.getOutputStream(), StandardCharsets.UTF_8))) {

            String openLine = reader.readLine();
            if (openLine == null || !openLine.startsWith("OPEN ")) {
                RelayUtil.closeQuietly(visitorSocket);
                return;
            }

            String[] parts = openLine.split("\\s+");
            if (parts.length < 2) {
                RelayUtil.closeQuietly(visitorSocket);
                return;
            }

            String tunnelId = parts[1];
            ControlSession controlSession = tunnels.get(tunnelId);
            if (controlSession == null) {
                writer.write("ERR tunnel_not_found\n");
                writer.flush();
                RelayUtil.closeQuietly(visitorSocket);
                return;
            }

            String sessionId = UUID.randomUUID().toString();
            pendingVisitors.put(sessionId, visitorSocket);
            writer.write("WAIT\n");
            writer.flush();

            controlSession.sendConnect(sessionId);
            System.out.printf("[B] Visitor waiting for tunnel=%s session=%s%n", tunnelId, sessionId);
        } catch (IOException e) {
            RelayUtil.closeQuietly(visitorSocket);
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
            String dataLine = reader.readLine();
            if (dataLine == null || !dataLine.startsWith("DATA ")) {
                RelayUtil.closeQuietly(dataSocket);
                return;
            }

            String[] parts = dataLine.split("\\s+");
            if (parts.length < 2) {
                RelayUtil.closeQuietly(dataSocket);
                return;
            }

            String sessionId = parts[1];
            Socket visitorSocket = pendingVisitors.remove(sessionId);
            if (visitorSocket == null) {
                RelayUtil.closeQuietly(dataSocket);
                return;
            }

            System.out.printf("[B] Relay established, session=%s%n", sessionId);
            RelayUtil.bridge(visitorSocket, dataSocket, executor);
        } catch (IOException e) {
            RelayUtil.closeQuietly(dataSocket);
        }
    }

    public static void main(String[] args) throws Exception {
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
        private final String tunnelId;
        private final BufferedWriter writer;

        private ControlSession(String tunnelId, BufferedWriter writer) {
            this.tunnelId = tunnelId;
            this.writer = writer;
        }

        private synchronized void sendConnect(String sessionId) throws IOException {
            writer.write("CONNECT " + sessionId + "\n");
            writer.flush();
            System.out.printf("[B] Notified tunnel=%s to serve session=%s%n", tunnelId, sessionId);
        }
    }
}
