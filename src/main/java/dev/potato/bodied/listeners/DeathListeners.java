package dev.potato.bodied.listeners;

import dev.potato.bodied.Bodied;
import dev.potato.bodied.models.Body;
import dev.potato.bodied.utilities.BodyUtilities;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;

public class DeathListeners implements Listener {
    private final Bodied plugin = Bodied.getPlugin();
    private final BodyUtilities bodyManager = BodyUtilities.getBodyManager();

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        bodyManager.spawnDeadNPC(p);
        e.getDrops().clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof ArmorStand armorStand) {
            Player playerWhoClicked = e.getPlayer();
            Iterator<Body> bodyIterator = bodyManager.getSpawnedBodies().iterator();

            while (bodyIterator.hasNext()) {
                Body body = bodyIterator.next();

                if (body.getArmorStands().contains(armorStand)) {
                    bodyIterator.remove();

                    // Remove the Body
                    bodyManager.removeDeadNPC(body);

                    // Give Items Back
                    double y = 0.5;
                    for (ItemStack item : body.getInventoryContents()) {
                        playerWhoClicked.getWorld().dropItem(body.getNpc().getBukkitEntity().getLocation().add(0, y, 0), item);
                        y = y + 0.5;
                    }

                    // Notify the Player
                    playerWhoClicked.sendMessage(ChatColor.GREEN + "[BODIED] You have looted someone else's body!");
                    playerWhoClicked.sendMessage(ChatColor.GREEN + "[BODIED] If they had items, they have been dropped at the body's location.");
                    playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.BLOCK_CHEST_OPEN, 1, 1);
                }
            }
        }
    }
}