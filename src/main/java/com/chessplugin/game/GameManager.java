package com.chessplugin.game;

import com.chessplugin.world.ChessBoardEntity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    // Satranc Masasi (Interaction) varlik UUID'si -> o masadaki oyun
    private final Map<UUID, BoardGame> gamesByEntity = new HashMap<>();

    // Davet edilen oyuncu UUID -> davet eden masanin varlik UUID'si
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();

    // Terfi menusu acik olan oyuncu UUID -> ilgili masanin varlik UUID'si
    private final Map<UUID, UUID> pendingPromotionViews = new HashMap<>();

    public BoardGame getGame(UUID entityId) {
        return gamesByEntity.get(entityId);
    }

    public void registerGame(UUID entityId, BoardGame game) {
        gamesByEntity.put(entityId, game);
    }

    public void removeGame(UUID entityId) {
        gamesByEntity.remove(entityId);
    }

    public boolean hasGame(UUID entityId) {
        return gamesByEntity.containsKey(entityId);
    }

    /**
     * Bellekte oyun yoksa, verilen Interaction varliginin kalici verisinden
     * (sunucu yeniden baslatildiktan sonraki ilk erisimde) geri yukler.
     */
    public BoardGame getOrLoadGame(Interaction interaction, ChessBoardEntity boardEntity) {
        BoardGame game = gamesByEntity.get(interaction.getUniqueId());
        if (game != null) return game;

        BoardGame restored = boardEntity.loadState(interaction, interaction.getLocation());
        if (restored != null) {
            gamesByEntity.put(interaction.getUniqueId(), restored);
        }
        return restored;
    }

    public BoardGame getActiveOrWaitingGameFor(Player player) {
        for (BoardGame g : gamesByEntity.values()) {
            if (g.isParticipant(player.getUniqueId())
                    && (g.getStatus() == GameStatus.ACTIVE || g.getStatus() == GameStatus.WAITING_FOR_OPPONENT)) {
                return g;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------
    // Davetler
    // ---------------------------------------------------------------

    public void invite(Player target, UUID hostEntityId) {
        pendingInvites.put(target.getUniqueId(), hostEntityId);
    }

    public boolean hasInvite(Player target) {
        return pendingInvites.containsKey(target.getUniqueId());
    }

    public UUID consumeInvite(Player target) {
        return pendingInvites.remove(target.getUniqueId());
    }

    public void declineInvite(Player target) {
        pendingInvites.remove(target.getUniqueId());
    }

    // ---------------------------------------------------------------
    // Terfi menusu takibi
    // ---------------------------------------------------------------

    public void registerPendingPromotionView(Player player, UUID entityId) {
        pendingPromotionViews.put(player.getUniqueId(), entityId);
    }

    public UUID getPendingPromotionEntity(Player player) {
        return pendingPromotionViews.get(player.getUniqueId());
    }

    public void clearPendingPromotionView(Player player) {
        pendingPromotionViews.remove(player.getUniqueId());
    }
}
