package com.chessplugin.listeners;

import com.chessplugin.chess.PieceType;
import com.chessplugin.game.BoardGame;
import com.chessplugin.game.GameManager;
import com.chessplugin.gui.PromotionGuiHolder;
import com.chessplugin.gui.PromotionGuiRenderer;
import com.chessplugin.world.ChessBoardEntity;
import com.chessplugin.world.GridBoardRenderer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Sadece terfi (promotion) secim menusunu yonetir. Ana 8x8 tahta artik
 * bir envanter GUI'si degil, dogrudan dunyada (GridBoardRenderer) oldugu
 * icin burada ayrica ele alinmaz.
 */
public class ChessGuiListener implements Listener {

    private final GameManager gameManager;
    private final ChessBoardEntity boardEntity;
    private final GridBoardRenderer gridRenderer;
    private final Plugin plugin;

    public ChessGuiListener(GameManager gameManager, ChessBoardEntity boardEntity, GridBoardRenderer gridRenderer, Plugin plugin) {
        this.gameManager = gameManager;
        this.boardEntity = boardEntity;
        this.gridRenderer = gridRenderer;
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PromotionGuiHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        PieceType type = PromotionGuiRenderer.typeForSlot(event.getRawSlot());
        if (type == null) return;

        BoardGame game = ((PromotionGuiHolder) holder).getGame();
        gameManager.clearPendingPromotionView(player);
        player.closeInventory();
        game.resolvePromotion(type);

        Entity entity = game.getEntityId() != null ? Bukkit.getEntity(game.getEntityId()) : null;
        if (entity instanceof Interaction) {
            Interaction board = (Interaction) entity;
            gridRenderer.refresh(board, game);
            boardEntity.saveState(board, game);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PromotionGuiHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID entityId = gameManager.getPendingPromotionEntity(player);
        if (entityId == null) return;
        BoardGame game = gameManager.getGame(entityId);
        if (game != null && game.hasPendingPromotion() && player.getUniqueId().equals(game.getPendingPromotionPlayer())) {
            player.sendMessage(ChatColor.RED + "Terfi secimi zorunludur, menu tekrar aciliyor...");
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (game.hasPendingPromotion() && player.isOnline()) {
                    PromotionGuiRenderer.open(player, game, game.colorOf(player.getUniqueId()));
                }
            });
        }
    }
}
