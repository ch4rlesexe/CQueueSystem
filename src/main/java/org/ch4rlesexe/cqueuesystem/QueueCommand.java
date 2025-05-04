package org.ch4rlesexe.cqueuesystem;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QueueCommand extends Command {
    private final CQueueSystem plugin;

    public QueueCommand(CQueueSystem plugin) {
        super(
                plugin.getConfig().getString("commands.join.name", "queue"),
                null,
                plugin.getConfig().getStringList("commands.join.aliases").toArray(new String[0])
        );
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("§cOnly players can use this."));
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;

        if (args.length < 1) {
            player.sendMessage(new TextComponent("§eUsage: /" + getName() + " <join|leave|list> [queueName]"));
            return;
        }

        String action = args[0].toLowerCase();
        switch (action) {
            case "join":
                if (args.length != 2) {
                    player.sendMessage(new TextComponent("§eUsage: /" + getName() + " join <queueName>"));
                    return;
                }
                String queueName = args[1];
                Queue q = plugin.getQueues().get(queueName);
                if (q == null) {
                    player.sendMessage(new TextComponent("§cUnknown queue: " + queueName));
                } else {
                    plugin.attemptJoin(player, q);
                }
                break;

            case "leave":
                plugin.leaveQueue(player);
                break;

            case "list":
                player.sendMessage(new TextComponent("§6=== Queues ==="));
                for (Map.Entry<String, Queue> e : plugin.getQueues().entrySet()) {
                    String name = e.getKey();
                    Queue queue = e.getValue();
                    List<String> members = queue.getPlayers().stream()
                            .map(ProxiedPlayer::getName)
                            .collect(Collectors.toList());
                    String memList = members.isEmpty() ? "(empty)" : String.join(", ", members);
                    player.sendMessage(new TextComponent(
                            "§7" + name + " §f(" + queue.getPlayers().size() + "/" + queue.getThreshold() + "): " + memList
                    ));
                }
                player.sendMessage(new TextComponent("§6=== Active Servers ==="));
                for (Map.Entry<String, ServerConfig> e : plugin.getServerConfigs().entrySet()) {
                    String srvName = e.getKey();
                    ServerInfo info = ProxyServer.getInstance().getServerInfo(srvName);
                    int count = info.getPlayers().size();
                    if (count > 0) {
                        player.sendMessage(new TextComponent(
                                "§7" + srvName + " §f(" + count + " players)"
                        ));
                    }
                }
                break;

            default:
                player.sendMessage(new TextComponent("§cUnknown action. Use join, leave, or list."));
        }
    }
}
