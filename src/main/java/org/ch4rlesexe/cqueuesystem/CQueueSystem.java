package org.ch4rlesexe.cqueuesystem;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class CQueueSystem extends Plugin implements Listener {
    private Configuration config;
    private final Map<String, Queue> queues = new HashMap<>();
    private final Map<String, ServerConfig> serverConfigs = new HashMap<>();
    private final Set<String> startedServers = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Long> lastEmptyTime = new HashMap<>();
    private final Set<String> stoppedServers = Collections.synchronizedSet(new HashSet<>());
    private PteroAPI ptero;
    private Timer idleTimer;

    @Override
    public void onEnable() {
        ptero = new PteroAPI(getLogger());
        loadConfig();

        ProxyServer.getInstance().getPluginManager().registerCommand(this, new QueueCommand(this));
        ProxyServer.getInstance().getPluginManager().registerCommand(this, new AdminCommand(this));
        ProxyServer.getInstance().getPluginManager().registerListener(this, this);

        ProxyServer.getInstance().registerChannel("CQueue");
        getLogger().info("Registered incoming channel: CQueue");

        idleTimer = new Timer(true);
        idleTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                checkIdleServers();
            }
        }, 0, 60_000);
    }

    @Override
    public void onDisable() {
        if (idleTimer != null) idleTimer.cancel();
    }

    public void loadConfig() {
        try {
            File cfgFile = new File(getDataFolder(), "config.yml");
            if (!cfgFile.exists()) {
                getDataFolder().mkdirs();
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, cfgFile.toPath());
                }
            }
            config = YamlConfiguration.getProvider(YamlConfiguration.class).load(cfgFile);
        } catch (IOException e) {
            getLogger().severe("Failed to load config: " + e.getMessage());
            return;
        }

        serverConfigs.clear();
        Configuration servSec = config.getSection("servers");
        if (servSec != null) {
            for (String key : servSec.getKeys()) {
                Configuration s = servSec.getSection(key);
                serverConfigs.put(key, new ServerConfig(
                        s.getString("panelUrl"),
                        s.getString("apiKey"),
                        s.getString("serverId"),
                        s.getInt("shutdownTimeout", -1)
                ));
            }
        }

        queues.clear();
        Configuration qSec = config.getSection("queues");
        if (qSec != null) {
            for (String qname : qSec.getKeys()) {
                queues.put(qname, new Queue(qname, qSec.getSection(qname)));
            }
        }

        getLogger().info("Loaded " + queues.size() + " queues and " +
                serverConfigs.size() + " servers.");
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (!"CQueue".equals(event.getTag())) return;
        event.setCancelled(true);

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String action    = in.readUTF();   // "join" or "leave"
        String queueName = in.readUTF();
        String playerName= in.readUTF();

        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(playerName);
        if (player == null) return;

        if ("join".equals(action)) {
            Queue q = queues.get(queueName);
            if (q != null) attemptJoin(player, q);
            else player.sendMessage(new TextComponent("§cUnknown queue: " + queueName));
        } else if ("leave".equals(action)) {
            leaveQueue(player);
        }
    }

    /**
     * If pool-mode & forceQueue, and all servers have been started once already,
     * immediately inform no servers available. Otherwise proceed as before.
     */
    public void attemptJoin(ProxiedPlayer player, Queue queue) {
        // Check "no available servers" condition
        if (queue.isPool() && queue.isForceQueue()
                && startedServers.containsAll(queue.getAllServers())
                && !queue.isStarting()) {

            player.sendMessage(new TextComponent(
                    "§cNo available servers, please try queueing again later."
            ));
            return;
        }

        String peek = queue.peekNextServer();
        ServerInfo info = ProxyServer.getInstance().getServerInfo(peek);

        info.ping((ping, error) -> {
            // offline/unreachable -> start or connect immediately
            if (error != null) {
                connectOrStart(player, queue);
                return;
            }

            int max     = ping.getPlayers().getMax();
            int current = info.getPlayers().size();
            boolean forced = false;
            if (queue.isForceQueue()) {
                for (String srv : queue.getAllServers()) {
                    ServerInfo si = ProxyServer.getInstance().getServerInfo(srv);
                    if (si.getPlayers().size() > 0 || queue.isStarting()) {
                        forced = true;
                        break;
                    }
                }
            }

            if (current >= max || forced) {
                enqueue(player, queue);
            } else {
                connectOrStart(player, queue);
            }
        });
    }

    private void enqueue(ProxiedPlayer player, Queue queue) {
        synchronized (queue) {
            List<ProxiedPlayer> list = queue.getPlayers();
            if (!list.contains(player)) {
                list.add(player);
                list.forEach(p ->
                        p.sendMessage(new TextComponent(queue.formatQueued(list.size())))
                );
            }
        }
    }

    private void connectOrStart(ProxiedPlayer player, Queue queue) {
        synchronized (queue) {
            String peek = queue.peekNextServer();
            ServerInfo info = ProxyServer.getInstance().getServerInfo(peek);

            // if running, connect immediately
            if (!queue.isStarting() && !info.getPlayers().isEmpty()) {
                player.sendMessage(new TextComponent(queue.formatConnecting(peek)));
                player.connect(info);
                return;
            }

            // else queue and maybe start
            List<ProxiedPlayer> list = queue.getPlayers();
            if (!list.contains(player)) {
                list.add(player);
                list.forEach(p ->
                        p.sendMessage(new TextComponent(queue.formatQueued(list.size())))
                );
            }

            if (list.size() >= queue.getThreshold() && !queue.isStarting()) {
                queue.setStarting(true);
                String target = queue.getNextServer();
                ServerConfig sc = serverConfigs.get(target);

                // mark as started
                startedServers.add(target);
                ptero.startServer(sc);

                list.forEach(p ->
                        p.sendMessage(new TextComponent(queue.formatStarting(target)))
                );

                new Timer().scheduleAtFixedRate(new TimerTask() {
                    @Override public void run() {
                        ServerInfo srv = ProxyServer.getInstance().getServerInfo(target);
                        srv.ping((res, err) -> {
                            if (err == null) {
                                long delay = 0;
                                for (ProxiedPlayer pl : new ArrayList<>(list)) {
                                    ProxyServer.getInstance().getScheduler().schedule(
                                            CQueueSystem.this,
                                            () -> pl.connect(srv),
                                            delay,
                                            TimeUnit.SECONDS
                                    );
                                    delay += 1;
                                }
                                list.clear();
                                queue.setStarting(false);
                                cancel();
                            }
                        });
                    }
                }, 0, queue.getCheckInterval() * 1000);
            }
        }
    }

    public void leaveQueue(ProxiedPlayer player) {
        for (Queue queue : queues.values()) {
            synchronized (queue) {
                if (queue.getPlayers().remove(player)) {
                    player.sendMessage(new TextComponent(queue.formatLeave()));
                    queue.getPlayers().forEach(p ->
                            p.sendMessage(new TextComponent(queue.formatQueued(queue.getPlayers().size())))
                    );
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent e) {
        handleQueueSlotOpen(e.getTarget().getName());
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent e) {
        handleQueueSlotOpen(e.getPlayer().getServer().getInfo().getName());
    }

    /**
     * When a server goes offline or a slot frees, either restart (if offline)
     * or admit the next in queue.
     */
    private void handleQueueSlotOpen(String serverName) {
        for (Queue queue : queues.values()) {
            if (!queue.getAllServers().contains(serverName)) continue;
            List<ProxiedPlayer> list = queue.getPlayers();
            if (list.isEmpty()) return;

            ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
                ServerInfo info = ProxyServer.getInstance().getServerInfo(serverName);
                info.ping((ping, err) -> {
                    if (err != null) {
                        // offline => allow new start
                        attemptJoin(list.get(0), queue);
                    } else {
                        int max     = ping.getPlayers().getMax();
                        int current = info.getPlayers().size();
                        if (current < max) {
                            ProxiedPlayer next = list.remove(0);
                            next.sendMessage(new TextComponent(queue.formatConnecting(serverName)));
                            ProxyServer.getInstance().getScheduler().schedule(
                                    this,
                                    () -> next.connect(info),
                                    1,
                                    TimeUnit.SECONDS
                            );
                        }
                    }
                });
            });
            return;
        }
    }

    private void checkIdleServers() {
        long now = System.currentTimeMillis();
        serverConfigs.forEach((name, sc) -> {
            int timeout = sc.shutdownTimeout;
            if (timeout < 0) {
                lastEmptyTime.remove(name);
                stoppedServers.remove(name);
                return;
            }
            if (stoppedServers.contains(name)) return;

            ServerInfo info = ProxyServer.getInstance().getServerInfo(name);
            if (info == null) return;
            if (!info.getPlayers().isEmpty()) {
                lastEmptyTime.remove(name);
                return;
            }
            Long first = lastEmptyTime.getOrDefault(name, now);
            if (timeout == 0 || now - first >= timeout * 60_000L) {
                scheduleStop(name, sc);
            } else {
                lastEmptyTime.put(name, first);
            }
        });
    }

    private void scheduleStop(String name, ServerConfig sc) {
        getLogger().info("Shutting down server: " + name);
        stoppedServers.add(name);
        ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
            ptero.stopServer(sc);
            // free up for next cycle
            startedServers.remove(name);
        });
    }

    public Map<String, Queue> getQueues() { return queues; }
    public Map<String, ServerConfig> getServerConfigs() { return serverConfigs; }
    public Configuration getConfig() { return config; }
}
