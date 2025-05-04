package org.ch4rlesexe.cqueuesystem;

import java.util.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.config.Configuration;

public class Queue {
    private final String name;
    private final boolean isPool;
    private final String singleServer;
    private final List<String> serverList;
    private int nextIndex = 0;

    private final int threshold;
    private final int checkInterval;
    private final boolean forceQueue;

    private final String msgQueued;
    private final String msgStarting;
    private final String msgConnecting;
    private final String msgLeave;

    private final List<ProxiedPlayer> players = new ArrayList<>();
    private boolean starting = false;

    public Queue(String name, Configuration sec) {
        this.name = name;

        // Determine single‑server vs pool mode
        if (sec.contains("servers")) {
            this.isPool       = true;
            this.serverList   = sec.getStringList("servers");
            this.singleServer = null;
        } else {
            this.isPool       = false;
            this.singleServer = sec.getString("server");
            this.serverList   = Collections.singletonList(this.singleServer);
        }

        this.threshold     = sec.getInt("threshold", 1);
        this.checkInterval= sec.getInt("checkInterval", 3);
        this.forceQueue    = sec.getBoolean("forceQueue", false);

        Configuration m    = sec.getSection("messages");
        this.msgQueued     = m.getString("queued");
        this.msgStarting   = m.getString("starting");
        this.msgConnecting = m.getString("connecting");
        this.msgLeave      = m.getString("leave");
    }

    /** Queue name. */
    public String getName() {
        return name;
    }

    /** True if pool‑mode. */
    public boolean isPool() {
        return isPool;
    }

    /** Threshold to auto‑start. */
    public int getThreshold() {
        return threshold;
    }

    /** Seconds between readiness checks. */
    public int getCheckInterval() {
        return checkInterval;
    }

    /** If true, once someone is queued, all new joins queue even if server has space. */
    public boolean isForceQueue() {
        return forceQueue;
    }

    /** Players currently in queue. */
    public List<ProxiedPlayer> getPlayers() {
        return players;
    }

    /** Whether a start request is in flight. */
    public boolean isStarting() {
        return starting;
    }

    /** Mark start in flight. */
    public void setStarting(boolean b) {
        starting = b;
    }

    /** Peek next server (round‑robin or single). */
    public String peekNextServer() {
        return isPool ? serverList.get(nextIndex) : singleServer;
    }

    /** Get and advance next server. */
    public String getNextServer() {
        if (!isPool) return singleServer;
        String s = serverList.get(nextIndex);
        nextIndex = (nextIndex + 1) % serverList.size();
        return s;
    }

    /** All servers this queue can use. */
    public List<String> getAllServers() {
        return Collections.unmodifiableList(serverList);
    }

    /** Format the queued message with size. */
    public String formatQueued(int size) {
        return ChatColor.translateAlternateColorCodes('&',
                msgQueued
                        .replace("{queue}", name)
                        .replace("{size}", String.valueOf(size))
                        .replace("{threshold}", String.valueOf(threshold))
        );
    }

    /** Format the starting message. */
    public String formatStarting(String server) {
        return ChatColor.translateAlternateColorCodes('&',
                msgStarting
                        .replace("{queue}", name)
                        .replace("{server}", server)
                        .replace("{threshold}", String.valueOf(threshold))
        );
    }

    /** Format the connecting message. */
    public String formatConnecting(String server) {
        return ChatColor.translateAlternateColorCodes('&',
                msgConnecting
                        .replace("{queue}", name)
                        .replace("{server}", server)
        );
    }

    /** Format the leave message. */
    public String formatLeave() {
        return ChatColor.translateAlternateColorCodes('&',
                msgLeave
                        .replace("{queue}", name)
        );
    }
}
