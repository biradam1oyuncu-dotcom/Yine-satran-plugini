package com.chessplugin.world;

import com.chessplugin.game.BoardGame;
import com.chessplugin.game.GameStateCodec;
import com.chessplugin.items.ChessItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Gercek bir Minecraft blogu KULLANMADAN, "Satranc Masasi"ni dunyada
 * temsil eder: gorunum icin bir ItemDisplay (ozel modelimizle), tiklama /
 * kirma / veri saklama icin bir Interaction varligi. Interaction, tum
 * durumun "sahibi" kabul edilir; oyun verisi onun
 * PersistentDataContainer'inda saklanir ve boylece sunucu yeniden
 * baslasa bile (varliklar Minecraft tarafindan otomatik kaydedildigi
 * icin) korunur.
 */
public class ChessBoardEntity {

    public final NamespacedKey isBoardKey;
    public final NamespacedKey hasPiecesKey;
    public final NamespacedKey gameStateKey;
    private final NamespacedKey displayUuidKey;

    public ChessBoardEntity(Plugin plugin) {
        this.isBoardKey = new NamespacedKey(plugin, "is_chessboard");
        this.hasPiecesKey = new NamespacedKey(plugin, "has_pieces");
        this.gameStateKey = new NamespacedKey(plugin, "game_state");
        this.displayUuidKey = new NamespacedKey(plugin, "display_uuid");
    }

    /**
     * Verilen "anchor" konumunda (bir blogun tam konumu) yeni bir Satranc
     * Masasi varlik cifti olusturur ve etkilesim varligini dondurur.
     */
    public Interaction spawn(Location anchor, ChessItems items) {
        World world = anchor.getWorld();
        Location blockLoc = anchor.getBlock().getLocation();

        ItemDisplay display = (ItemDisplay) world.spawnEntity(blockLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(items.createBoardDisplayItem());
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        display.setPersistent(true);

        Location center = blockLoc.clone().add(0.5, 0.0, 0.5);
        Interaction interaction = (Interaction) world.spawnEntity(center, EntityType.INTERACTION);
        interaction.setInteractionWidth(1.0f);
        interaction.setInteractionHeight(1.0f);
        interaction.setPersistent(true);

        interaction.getPersistentDataContainer().set(isBoardKey, PersistentDataType.BYTE, (byte) 1);
        interaction.getPersistentDataContainer().set(hasPiecesKey, PersistentDataType.BYTE, (byte) 0);
        interaction.getPersistentDataContainer().set(displayUuidKey, PersistentDataType.STRING, display.getUniqueId().toString());

        return interaction;
    }

    public boolean isBoard(Entity entity) {
        if (!(entity instanceof Interaction)) return false;
        Byte val = entity.getPersistentDataContainer().get(isBoardKey, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public boolean isLoaded(Interaction interaction) {
        Byte val = interaction.getPersistentDataContainer().get(hasPiecesKey, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public void setLoaded(Interaction interaction, boolean loaded) {
        interaction.getPersistentDataContainer().set(hasPiecesKey, PersistentDataType.BYTE, (byte) (loaded ? 1 : 0));
    }

    public Entity getPairedDisplay(Interaction interaction) {
        String raw = interaction.getPersistentDataContainer().get(displayUuidKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return Bukkit.getEntity(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public void remove(Interaction interaction) {
        Entity display = getPairedDisplay(interaction);
        if (display != null) {
            display.remove();
        }
        interaction.remove();
    }

    public void saveState(Interaction interaction, BoardGame game) {
        String encoded = GameStateCodec.encode(game);
        interaction.getPersistentDataContainer().set(gameStateKey, PersistentDataType.STRING, encoded);
    }

    public BoardGame loadState(Interaction interaction, Location location) {
        String data = interaction.getPersistentDataContainer().get(gameStateKey, PersistentDataType.STRING);
        if (data == null) return null;
        return GameStateCodec.decode(data, location);
    }
}
