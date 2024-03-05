package dev.potato.bodied.models;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Body {
    private ServerPlayer npc;
    private UUID deadPlayer;
    private ItemStack[] inventoryContents;
    private long timeOfDeath; // Miliseconds since epoch time (January 1st, 1970)
    private List<ArmorStand> armorStands;

    public Body(ServerPlayer npc, UUID deadPlayer, ItemStack[] inventoryContents, long timeOfDeath) {
        this.npc = npc;
        this.deadPlayer = deadPlayer;
        this.inventoryContents = inventoryContents;
        this.timeOfDeath = timeOfDeath;
        this.armorStands = new ArrayList<>();
    }

    public ServerPlayer getNpc() {
        return npc;
    }

    public void setNpc(ServerPlayer npc) {
        this.npc = npc;
    }

    public UUID getDeadPlayer() {
        return deadPlayer;
    }

    public void setDeadPlayer(UUID deadPlayer) {
        this.deadPlayer = deadPlayer;
    }

    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public void setInventoryContents(ItemStack[] inventoryContents) {
        this.inventoryContents = inventoryContents;
    }

    public long getTimeOfDeath() {
        return timeOfDeath;
    }

    public void setTimeOfDeath(long timeOfDeath) {
        this.timeOfDeath = timeOfDeath;
    }

    public List<ArmorStand> getArmorStands() {
        return armorStands;
    }
}