package org.ch4rlesexe.cqueuesystem;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class AdminCommand extends Command {
    private final CQueueSystem plugin;

    public AdminCommand(CQueueSystem plugin) {
        super(
                plugin.getConfig().getString("commands.admin.name","queueadmin"),
                null,
                plugin.getConfig().getStringList("commands.admin.aliases").toArray(new String[0])
        );
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cqueuesystem.admin")) {
            sender.sendMessage(new TextComponent("§cNo permission."));
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.loadConfig();
            sender.sendMessage(new TextComponent("§aConfig reloaded."));
        } else {
            sender.sendMessage(new TextComponent("Usage: /" + getName() + " reload"));
        }
    }
}
