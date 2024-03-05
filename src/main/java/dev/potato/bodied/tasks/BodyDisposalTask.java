package dev.potato.bodied.tasks;

import dev.potato.bodied.Bodied;
import dev.potato.bodied.models.Body;
import dev.potato.bodied.utilities.BodyUtilities;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Iterator;

public class BodyDisposalTask extends BukkitRunnable {
    private final Bodied plugin = Bodied.getPlugin();

    @Override
    public void run() {
        BodyUtilities bodyManager = BodyUtilities.getBodyManager();

        Iterator<Body> bodyIterator = bodyManager.getSpawnedBodies().iterator(); // <-- Avoids concurrent modification exception

        while (bodyIterator.hasNext()) {
            Body body = bodyIterator.next();
            long currentTime = System.currentTimeMillis();
            if (currentTime - body.getTimeOfDeath() >= 10000) {
                bodyIterator.remove();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        // Body Data
                        ServerPlayer npc = body.getNpc();
                        Location currentNPCLocation = npc.getBukkitEntity().getLocation();

                        Bukkit.getOnlinePlayers().forEach(player -> {
                            // Player Connection
                            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

                            // NPC Movement (Descending Into Ground)
                            npc.setPos(currentNPCLocation.getX(), currentNPCLocation.getY() - 0.01, currentNPCLocation.getZ());

                            // NPC Movement Packet
                            connection.send(new ClientboundTeleportEntityPacket(npc));
                        });

                        // Stop Body From Forever Descending
                        if (!currentNPCLocation.add(0, 1, 0).getBlock().isPassable()) {
                            // Removing NPC Visibility
                            bodyManager.removeDeadNPC(body);

                            // Cancel Task
                            this.cancel();
                        }
                    }
                }.runTaskTimerAsynchronously(plugin, 0, 5);

                Player playerWhoDied = Bukkit.getPlayer(body.getDeadPlayer());

                if (playerWhoDied != null) {
                    // Notify the player the body and items were not claimed
                    playerWhoDied.sendMessage(ChatColor.GREEN + "[BODIED] Your body has not been claimed in the past 10 seconds.");
                    playerWhoDied.sendMessage(ChatColor.GREEN + "[BODIED] Your items have been returned to you!");
                }

                // Give items back to the player
                Inventory playerInventory = playerWhoDied.getInventory();
                playerInventory.addItem(body.getInventoryContents()).values()
                        .forEach(item -> playerWhoDied.getWorld().dropItem(playerWhoDied.getLocation(), item));
            }
        }
    }
}
