package org.ch4rlesexe.cqueuesystem;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class PteroAPI {
    private final Logger logger;

    public PteroAPI(Logger logger) {
        this.logger = logger;
    }

    /**
     * Powers on the given server via the Client API.
     */
    public void startServer(ServerConfig cfg) {
        try {
            // Strip trailing slash if present
            String base = cfg.panelUrl.endsWith("/")
                    ? cfg.panelUrl.substring(0, cfg.panelUrl.length() - 1)
                    : cfg.panelUrl;

            String urlStr = base
                    + "/api/client/servers/"
                    + cfg.serverId
                    + "/power";

            logger.info("PteroAPI → POST " + urlStr);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", cfg.apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            byte[] body = "{\"signal\":\"start\"}".getBytes();
            conn.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            if (code == 204) {
                logger.info("Started server " + cfg.serverId + " via client API.");
            } else {
                logger.warning("Failed to start "
                        + cfg.serverId + " (HTTP " + code + ").");
            }
        } catch (Exception e) {
            logger.severe("Error starting server "
                    + cfg.serverId + ": " + e.getMessage());
        }
    }

    /**
     * Stops the given server via the Client API.
     */
    public void stopServer(ServerConfig cfg) {
        try {
            String base = cfg.panelUrl.endsWith("/")
                    ? cfg.panelUrl.substring(0, cfg.panelUrl.length() - 1)
                    : cfg.panelUrl;

            String urlStr = base
                    + "/api/client/servers/"
                    + cfg.serverId
                    + "/power";

            logger.info("PteroAPI → POST " + urlStr + " (stop)");

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", cfg.apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            byte[] body = "{\"signal\":\"stop\"}".getBytes();
            conn.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            if (code == 204) {
                logger.info("Stopped server " + cfg.serverId + ".");
            } else {
                logger.warning("Failed to stop "
                        + cfg.serverId + " (HTTP " + code + ").");
            }
        } catch (Exception e) {
            logger.severe("Error stopping server "
                    + cfg.serverId + ": " + e.getMessage());
        }
    }
}
