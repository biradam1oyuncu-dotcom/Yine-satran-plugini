package com.chessplugin.world;

import com.chessplugin.chess.ChessBoard;
import com.chessplugin.chess.ChessMove;
import com.chessplugin.chess.ChessPiece;
import com.chessplugin.chess.PieceColor;
import com.chessplugin.game.BoardGame;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Aktif bir oyunun tum 8x8 tahtasini, envanter GUI'si KULLANMADAN,
 * dogrudan dunyada (world-space) tek parca halinde, gercek boyutlu ve
 * gercek tas gorselleriyle gosterir. Her kare ve her tas ayri birer
 * ItemDisplay varligidir; tiklama algilama icin her karenin ustunde
 * kucuk bir Interaction varligi bulunur. Boylece 8x8=64 kare TEK SEFERDE
 * gorunur (vanilla envanter GUI'lerinin 6 satir siniri buraya uygulanmaz,
 * cunku bu bir envanter degil, gercek dunya varliklaridir).
 */
public class GridBoardRenderer {

    private static final float CELL = 0.5f;
    private static final double SQUARE_Y = 1.02;
    private static final double PIECE_Y = 1.08;
    private static final double BUTTON_Y = 1.08;

    private final NamespacedKey isGridCellKey;
    private final NamespacedKey cellFileKey;
    private final NamespacedKey cellRankKey;
    private final NamespacedKey isRematchButtonKey;
    private final NamespacedKey parentBoardKey;

    private final Map<UUID, GridState> grids = new HashMap<>();

    private static class GridState {
        Location anchor;
        List<UUID> interactionIds = new ArrayList<>();
        List<UUID> visualIds = new ArrayList<>();
        UUID rematchInteractionId;
    }

    public GridBoardRenderer(Plugin plugin) {
        this.isGridCellKey = new NamespacedKey(plugin, "grid_cell");
        this.cellFileKey = new NamespacedKey(plugin, "cell_file");
        this.cellRankKey = new NamespacedKey(plugin, "cell_rank");
        this.isRematchButtonKey = new NamespacedKey(plugin, "rematch_button");
        this.parentBoardKey = new NamespacedKey(plugin, "parent_board");
    }

    public boolean hasGrid(UUID boardId) {
        return grids.containsKey(boardId);
    }

    /** Grid yoksa olusturur (tiklama hitbox'lariyla), varsa sadece gorseli tazeler. */
    public void ensureGrid(Interaction board, BoardGame game) {
        if (grids.containsKey(board.getUniqueId())) {
            refresh(board, game);
            return;
        }

        // Sunucu yeniden baslatildiysa bu bellek haritasi bostur ama eski
        // oturumdan kalma fiziksel varliklar hala dunyada olabilir; once
        // onlari temizleyelim ki ust uste binmesin.
        cleanupOrphans(board);

        GridState state = new GridState();
        state.anchor = board.getLocation().clone();
        grids.put(board.getUniqueId(), state);

        World world = state.anchor.getWorld();
        double originX = state.anchor.getX() - (CELL * 8) / 2.0;
        double originZ = state.anchor.getZ() - (CELL * 8) / 2.0;
        double baseY = state.anchor.getBlockY();

        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                double cx = originX + f * CELL + CELL / 2.0;
                double cz = originZ + r * CELL + CELL / 2.0;
                Location cellLoc = new Location(world, cx, baseY + SQUARE_Y, cz);

                Interaction inter = (Interaction) world.spawnEntity(cellLoc, EntityType.INTERACTION);
                inter.setInteractionWidth(CELL * 0.95f);
                inter.setInteractionHeight(0.35f);
                inter.setPersistent(true);
                inter.getPersistentDataContainer().set(isGridCellKey, PersistentDataType.BYTE, (byte) 1);
                inter.getPersistentDataContainer().set(cellFileKey, PersistentDataType.INTEGER, f);
                inter.getPersistentDataContainer().set(cellRankKey, PersistentDataType.INTEGER, r);
                inter.getPersistentDataContainer().set(parentBoardKey, PersistentDataType.STRING, board.getUniqueId().toString());
                state.interactionIds.add(inter.getUniqueId());
            }
        }

        double bx = originX + 8 * CELL + 0.4;
        double bz = originZ + 4 * CELL;
        Location btnLoc = new Location(world, bx, baseY + BUTTON_Y, bz);
        Interaction btn = (Interaction) world.spawnEntity(btnLoc, EntityType.INTERACTION);
        btn.setInteractionWidth(0.6f);
        btn.setInteractionHeight(0.6f);
        btn.setPersistent(true);
        btn.getPersistentDataContainer().set(isRematchButtonKey, PersistentDataType.BYTE, (byte) 1);
        btn.getPersistentDataContainer().set(parentBoardKey, PersistentDataType.STRING, board.getUniqueId().toString());
        state.rematchInteractionId = btn.getUniqueId();

        refresh(board, game);
    }

    /** Sadece gorsel katmani (kareler+taslar+buton) yeniler; hitbox'lara dokunmaz. */
    public void refresh(Interaction board, BoardGame game) {
        GridState state = grids.get(board.getUniqueId());
        if (state == null) return;

        for (UUID id : state.visualIds) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        state.visualIds.clear();

        World world = state.anchor.getWorld();
        double originX = state.anchor.getX() - (CELL * 8) / 2.0;
        double originZ = state.anchor.getZ() - (CELL * 8) / 2.0;
        double baseY = state.anchor.getBlockY();

        ChessBoard cb = game.getBoard();
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                double cx = originX + f * CELL + CELL / 2.0;
                double cz = originZ + r * CELL + CELL / 2.0;

                String squareTex = squareTextureFor(game, f, r);
                ItemDisplay bg = spawnFlatDisplay(world, cx, baseY + SQUARE_Y, cz, squareTex, CELL, board.getUniqueId());
                state.visualIds.add(bg.getUniqueId());

                ChessPiece piece = cb.getPiece(f, r);
                if (piece != null) {
                    String pieceTex = "piece_" + (piece.getColor() == PieceColor.WHITE ? "white" : "black")
                            + "_" + piece.getType().name().toLowerCase();
                    ItemDisplay pd = spawnFlatDisplay(world, cx, baseY + PIECE_Y, cz, pieceTex, CELL * 0.8f, board.getUniqueId());
                    state.visualIds.add(pd.getUniqueId());
                }
            }
        }

        if (game.isFinished()) {
            double bx = originX + 8 * CELL + 0.4;
            double bz = originZ + 4 * CELL;
            ItemDisplay btnDisp = spawnFlatDisplay(world, bx, baseY + BUTTON_Y, bz, "rematch_button", 0.5f, board.getUniqueId());
            state.visualIds.add(btnDisp.getUniqueId());
        }
    }

    private void cleanupOrphans(Interaction board) {
        String targetId = board.getUniqueId().toString();
        Location center = board.getLocation();
        for (Entity e : center.getWorld().getNearbyEntities(center, 6, 4, 6)) {
            if (e instanceof Interaction || e instanceof ItemDisplay) {
                String pid = e.getPersistentDataContainer().get(parentBoardKey, PersistentDataType.STRING);
                if (targetId.equals(pid)) {
                    e.remove();
                }
            }
        }
    }

    private String squareTextureFor(BoardGame game, int file, int rank) {
        if (file == game.getSelectedFile() && rank == game.getSelectedRank()) {
            return "square_selected";
        }
        List<ChessMove> legal = game.getLegalMoves();
        if (legal != null) {
            for (ChessMove m : legal) {
                if (m.toFile == file && m.toRank == rank) {
                    return m.isCapture ? "square_capture" : "square_move";
                }
            }
        }
        return ((file + rank) % 2 == 0) ? "square_dark" : "square_light";
    }

    private ItemDisplay spawnFlatDisplay(World world, double x, double y, double z, String textureKey, float size, UUID parentBoardId) {
        Location loc = new Location(world, x, y, z);
        ItemDisplay disp = (ItemDisplay) world.spawnEntity(loc, EntityType.ITEM_DISPLAY);
        disp.setItemStack(iconItem(textureKey));
        disp.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        disp.setPersistent(true);
        disp.getPersistentDataContainer().set(parentBoardKey, PersistentDataType.STRING, parentBoardId.toString());
        try {
            Transformation t = new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f((float) (-Math.PI / 2), 1f, 0f, 0f),
                    new Vector3f(size, size, size),
                    new AxisAngle4f(0f, 0f, 0f, 1f)
            );
            disp.setTransformation(t);
        } catch (Throwable ignored) {
            // Cok eski bir API'ye karsi calisiliyorsa transform ayarlanamayabilir;
            // varlik yine de gorunur, sadece boyut/aci varsayilan kalir.
        }
        return disp;
    }

    private ItemStack iconItem(String key) {
        ItemStack item = new ItemStack(Material.PAPER, 1);
        ItemMeta meta = item.getItemMeta();
        try {
            meta.setItemModel(new NamespacedKey("chessplugin", key));
        } catch (Throwable ignored) {
        }
        item.setItemMeta(meta);
        return item;
    }

    public void removeGrid(UUID boardId) {
        GridState state = grids.remove(boardId);
        if (state == null) return;
        for (UUID id : state.interactionIds) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        for (UUID id : state.visualIds) {
            Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        if (state.rematchInteractionId != null) {
            Entity e = Bukkit.getEntity(state.rematchInteractionId);
            if (e != null) e.remove();
        }
    }

    // ---------------------------------------------------------------
    // Tiklanan varligin ne oldugunu cozmek icin yardimcilar
    // ---------------------------------------------------------------

    public boolean isGridCell(Entity entity) {
        Byte v = entity.getPersistentDataContainer().get(isGridCellKey, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }

    public boolean isRematchButton(Entity entity) {
        Byte v = entity.getPersistentDataContainer().get(isRematchButtonKey, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }

    public int[] cellOf(Entity entity) {
        Integer f = entity.getPersistentDataContainer().get(cellFileKey, PersistentDataType.INTEGER);
        Integer r = entity.getPersistentDataContainer().get(cellRankKey, PersistentDataType.INTEGER);
        if (f == null || r == null) return null;
        return new int[]{f, r};
    }

    public UUID parentBoardOf(Entity entity) {
        String raw = entity.getPersistentDataContainer().get(parentBoardKey, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
