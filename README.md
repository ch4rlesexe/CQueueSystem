
---

# CQueueSystem

CQueueSystem is a dynamic queue management and server scaling plugin for **BungeeCord (1.8)** networks using **Pterodactyl**. It automatically queues players, starts servers on demand, and gracefully shuts them down when empty. Perfect for minigames, events, or lobbies that need responsive scaling without manual intervention.

Built for **Java 8** and **Minecraft 1.8 BungeeCord**.

---

## ✅ Features

* **Smart player queue system** with FIFO logic
* **Auto-start and shutdown** of servers via Pterodactyl Client API
* **Single-server and pool support**
* **Live server capacity checks**
* **Optional Spigot integration** using a dedicated addon
* **Color-coded messages** and full config customization
* **Handles full servers with gradual player connection**

---

## 📦 Requirements

* BungeeCord (1.8+) with plugin support
* Java 8
* Pterodactyl panel with **Client API keys**
* (Optional) Spigot plugin support via [CQueueSystemAddon](https://github.com/ch4rlesexe/CQueueSystemAddon)

---

## 🔌 Spigot Bridge Addon

To support NPCs, GUIs (e.g. DeluxeHub), or Spigot-side commands that trigger queueing on the BungeeCord proxy, install the Spigot-side addon:

👉 **[CQueueSystemAddon GitHub Repo](https://github.com/ch4rlesexe/CQueueSystemAddon)**

This communicates over a registered plugin messaging channel and allows players to join or leave queues using in-game triggers or GUI actions.

---

## ⚙️ Configuration Overview

```yaml
commands:
  join:
    name: "queue"
    aliases: ["q"]
  admin:
    name: "queueadmin"
    aliases: ["qa", "qsadmin"]

servers:
  minigame-01:
    panelUrl: "https://panel.example.com"
    apiKey: "Bearer <YOUR_CLIENT_API_KEY>"
    serverId: "uuid-01"
    shutdownTimeout: 10

queues:
  minigames:
    servers:
      - "minigame-01"
    threshold: 5
    checkInterval: 3
    shutdownTimeout: 10
    messages:
      queued: "&eQueued for &6{queue}&e ({size}/{threshold})"
      starting: "&aStarting &6{server}&a..."
      connecting: "&aJoining &6{server}&a now!"
      leave: "&cYou have left the queue &6{queue}&c."
```

* **`threshold`** – Minimum queued players before starting
* **`shutdownTimeout`** – Time (in minutes) to auto-shutdown an empty server
* **`checkInterval`** – Seconds between ping checks during startup

---

## 🔄 Commands

| Command               | Description               |
| --------------------- | ------------------------- |
| `/queue join <queue>` | Add player to a queue     |
| `/queue leave`        | Remove player from queue  |
| `/queueadmin reload`  | Reloads config on the fly |

---

## 🔐 Security Best Practices

* Always use **Client API keys** (not Application API keys)
* Make sure to use a very secure key or limit IPs with access

---

