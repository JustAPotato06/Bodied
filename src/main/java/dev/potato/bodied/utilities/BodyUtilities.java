package dev.potato.bodied.utilities;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.potato.bodied.Bodied;
import dev.potato.bodied.models.Body;
import dev.potato.bodied.tasks.BodyDisposalTask;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;

public class BodyUtilities {
    private static final BodyUtilities bodyManager = new BodyUtilities();

    private final Bodied plugin = Bodied.getPlugin();
    private List<Body> spawnedBodies;
    private BodyDisposalTask bodyDisposalTask;

    public BodyUtilities() {
        this.spawnedBodies = new ArrayList<>();
        this.bodyDisposalTask = new BodyDisposalTask();
        bodyDisposalTask.runTaskTimerAsynchronously(plugin, 0, 20);
    }

    public static BodyUtilities getBodyManager() {
        return bodyManager;
    }

    public List<Body> getSpawnedBodies() {
        return spawnedBodies;
    }

    public void spawnDeadNPC(Player p) {
        // Player NMS Data
        CraftPlayer craftPlayer = (CraftPlayer) p;
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        GameProfile serverPlayerProfile = serverPlayer.getGameProfile();

        // NPC Data
        MinecraftServer server = serverPlayer.getServer();
        ServerLevel level = serverPlayer.serverLevel().getLevel();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), " ");

        // NPC Creation
        ServerPlayer npc = new ServerPlayer(server, level, gameProfile, ClientInformation.createDefault());

        // Location and Position
        Location playerLocation = p.getLocation();
        playerLocation.setY(playerLocation.getBlockY());
        while (playerLocation.getBlock().getType() == Material.AIR) {
            playerLocation.subtract(0, 1, 0);
        }
        npc.setPos(playerLocation.getX(), playerLocation.getY() + 1, playerLocation.getZ());
        npc.setPose(Pose.SLEEPING);

        // Armor Stands for Detecting NPC Interaction
        Location npcLocation = npc.getBukkitEntity().getLocation();
        ArmorStand armorStand1 = (ArmorStand) p.getWorld().spawnEntity(npcLocation, EntityType.ARMOR_STAND);
        armorStand1.setSmall(true);
        armorStand1.setInvisible(true);
        armorStand1.setInvulnerable(true);
        armorStand1.setGravity(false);
        ArmorStand armorStand2 = (ArmorStand) p.getWorld().spawnEntity(npcLocation.subtract(1, 0, 0), EntityType.ARMOR_STAND);
        armorStand2.setSmall(true);
        armorStand2.setInvisible(true);
        armorStand2.setInvulnerable(true);
        armorStand2.setGravity(false);
        ArmorStand armorStand3 = (ArmorStand) p.getWorld().spawnEntity(npcLocation.subtract(1, 0, 0), EntityType.ARMOR_STAND);
        armorStand3.setSmall(true);
        armorStand3.setInvisible(true);
        armorStand3.setInvulnerable(true);
        armorStand3.setGravity(false);

        // Team Setup (Hides Display Name)
        PlayerTeam team = new PlayerTeam(new Scoreboard(), npc.displayName);
        team.getPlayers().add(" ");
        team.setNameTagVisibility(Team.Visibility.NEVER);

        // NPC Skin
        Property property = (Property) serverPlayerProfile.getProperties().get("textures").toArray()[0];
        String signature = property.signature();
        String texture = property.value();
        gameProfile.getProperties().put("textures", new Property("textures", texture, signature));

        // NPC Connection
        SynchedEntityData npcData = npc.getEntityData();
        npcData.set(new EntityDataAccessor<>(17, EntityDataSerializers.BYTE), (byte) 127);
        ServerPlayer serverPlayerToCopy = ((CraftPlayer) p).getHandle();
        try {
            Field field = npc.getClass().getDeclaredField("c");
            field.setAccessible(true);
            field.set(npc, serverPlayerToCopy.connection);
        } catch (Exception exception) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "There was an error establishing a non-null connection to the NPC!");
        }

        // Packet Sending
        Bukkit.getOnlinePlayers().forEach(player -> {
            // Player NMS Data
            CraftPlayer craftPlayer1 = (CraftPlayer) player;
            ServerPlayer serverPlayer1 = craftPlayer1.getHandle();

            // Player Connection
            ServerGamePacketListenerImpl connection = serverPlayer1.connection;

            // NPC Show Packets
            connection.send(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER, npc));
            connection.send(new ClientboundAddEntityPacket(npc));

            // NPC Position Packet
            connection.send(new ClientboundSetEntityDataPacket(npc.getId(), npcData.getNonDefaultValues()));

            // NPC Team Packets
            connection.send(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
            connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));

            new BukkitRunnable() {
                @Override
                public void run() {
                    // NPC Remove Tab Packet
                    connection.send(new ClientboundPlayerInfoRemovePacket(Collections.singletonList(npc.getUUID())));
                }
            }.runTaskLaterAsynchronously(plugin, 20);
        });

        // Store Body in Manager
        ItemStack[] intentoryContents = Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).toArray(ItemStack[]::new);
        Body body = new Body(npc, p.getUniqueId(), intentoryContents, System.currentTimeMillis());
        body.getArmorStands().add(armorStand1);
        body.getArmorStands().add(armorStand2);
        body.getArmorStands().add(armorStand3);
        spawnedBodies.add(body);
    }

    public void removeDeadNPC(Body body) {
        ServerPlayer npc = body.getNpc();

        Bukkit.getOnlinePlayers().forEach(player -> {
            // Player Connection
            ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;

            // Removing NPC Visibility Packet
            connection.send(new ClientboundRemoveEntitiesPacket(npc.getId()));
        });
    }
}