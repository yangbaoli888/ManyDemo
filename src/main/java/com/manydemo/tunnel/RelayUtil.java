package com.manydemo.tunnel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public final class RelayUtil {
    private RelayUtil() {
    }

    public static void bridge(Socket left, Socket right, ExecutorService executor) {
        executor.submit(() -> copy(left, right));
        executor.submit(() -> copy(right, left));
    }

    private static void copy(Socket from, Socket to) {
        try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
            in.transferTo(out);
            out.flush();
        } catch (IOException ignored) {
            // The opposite direction usually closes soon after. Ignore noisy IO errors.
        } finally {
            closeQuietly(from);
            closeQuietly(to);
        }
    }

    public static void closeQuietly(Socket socket) {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // no-op
        }
    }
}
