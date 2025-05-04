package org.ch4rlesexe.cqueuesystem;

public class ServerConfig {
    public final String panelUrl;
    public final String apiKey;
    public final String serverId;
    public final int shutdownTimeout; // minutes of idleness

    public ServerConfig(String panelUrl, String apiKey, String serverId, int shutdownTimeout) {
        this.panelUrl       = panelUrl;
        this.apiKey         = apiKey;
        this.serverId       = serverId;
        this.shutdownTimeout= shutdownTimeout;
    }
}
