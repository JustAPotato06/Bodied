package dev.potato.bodied;

import dev.potato.bodied.listeners.DeathListeners;
import org.bukkit.plugin.java.JavaPlugin;

public final class Bodied extends JavaPlugin {
    private static Bodied plugin;

    public static Bodied getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Initialization
        plugin = this;

        // Listeners
        registerListeners();
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new DeathListeners(), this);
    }
}