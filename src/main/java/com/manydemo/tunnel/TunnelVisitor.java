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

public class TunnelVisitor {
    private final String serverHost;
    private final int relayPort;
    private final String tunnelId;
    private final int localListenPort;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public TunnelVisitor(String serverHost, int relayPort, String tunnelId, int localListenPort) {
        this.serverHost = serverHost;
        this.relayPort = relayPort;
        this.tunnelId = tunnelId;
        this.localListenPort = localListenPort;
    }

    public void start() throws IOException {
        try (ServerSocket localServer = new ServerSocket(localListenPort)) {
            System.out.printf("[C] Listening on 127.0.0.1:%d, forwarding to tunnel=%s via %s:%d%n",
                    localListenPort, tunnelId, serverHost, relayPort);
            while (true) {
                Socket localSocket = localServer.accept();
                executor.submit(() -> handleLocalConnection(localSocket));
            }
        }
    }

    private void handleLocalConnection(Socket localSocket) {
        try {
            Socket relaySocket = new Socket(serverHost, relayPort);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(relaySocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(relaySocket.getInputStream(), StandardCharsets.UTF_8));

            writer.write("OPEN " + tunnelId + "\n");
            writer.flush();

            String response = reader.readLine();
            if (!"WAIT".equals(response)) {
                throw new IOException("open tunnel failed: " + response);
            }

            RelayUtil.bridge(localSocket, relaySocket, executor);
        } catch (IOException e) {
            RelayUtil.closeQuietly(localSocket);
            System.err.printf("[C] Forward failed: %s%n", e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("Usage: TunnelVisitor <serverHost> <relayPort> <tunnelId> <localListenPort>");
            return;
        }
        TunnelVisitor visitor = new TunnelVisitor(
                args[0],
                Integer.parseInt(args[1]),
                args[2],
                Integer.parseInt(args[3])
        );
        visitor.start();
    }
}
