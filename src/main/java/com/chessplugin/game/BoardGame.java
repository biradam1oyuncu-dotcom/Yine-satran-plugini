package com.chessplugin.game;

import com.chessplugin.chess.ChessBoard;
import com.chessplugin.chess.ChessMove;
import com.chessplugin.chess.ChessPiece;
import com.chessplugin.chess.PieceColor;
import com.chessplugin.chess.PieceType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Belirli bir Satranc Masasi blogunun konumuna bagli, o bloktaki oyunun
 * tum mantigini yoneten sinif. Tahta artik dunyada (world-space) ortak/
 * paylasilan bir gorsel oldugu icin secim durumu (hangi tas secili,
 * hangi kareler hedef) oyuncuya ozel degil, doğrudan bu sinifta (paylasimli)
 * tutulur.
 */
public class BoardGame {

    private final Location location;
    private UUID entityId;
    private final ChessBoard board = new ChessBoard();

    private UUID whiteId;
    private UUID blackId;
    private String whiteName;
    private String blackName;

    private PieceColor currentTurn = PieceColor.WHITE;
    private GameStatus status = GameStatus.WAITING_FOR_OPPONENT;

    private int selectedFile = -1;
    private int selectedRank = -1;
    private List<ChessMove> legalMoves = null;

    private ChessMove pendingPromotionMove;
    private UUID pendingPromotionPlayer;

    public BoardGame(Location location, Player host) {
        this(location, host.getUniqueId(), host.getName());
    }

    public BoardGame(Location location, UUID whiteId, String whiteName) {
        this.location = location;
        this.whiteId = whiteId;
        this.whiteName = whiteName;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public ChessBoard getBoard() {
        return board;
    }

    public GameStatus getStatus() {
        return status;
    }

    public UUID getWhiteId() {
        return whiteId;
    }

    public UUID getBlackId() {
        return blackId;
    }

    public String getWhiteName() {
        return whiteName;
    }

    public String getBlackName() {
        return blackName;
    }

    public PieceColor getCurrentTurn() {
        return currentTurn;
    }

    public boolean isSelfPlay() {
        return blackId != null && whiteId != null && whiteId.equals(blackId);
    }

    public boolean isParticipant(UUID id) {
        return id.equals(whiteId) || (blackId != null && id.equals(blackId));
    }

    /**
     * Kendi kendine oynama (self-play) durumunda beyaz ve siyah ayni
     * oyuncudur; bu durumda rengi her zaman "su anki sira" olarak
     * dondururuz ki oyuncu hem beyazi hem siyahi oynatabilsin.
     */
    public PieceColor colorOf(UUID id) {
        if (isSelfPlay() && id.equals(whiteId)) {
            return currentTurn;
        }
        if (id.equals(whiteId)) return PieceColor.WHITE;
        if (blackId != null && id.equals(blackId)) return PieceColor.BLACK;
        return null;
    }

    public void joinAsBlack(Player player) {
        this.blackId = player.getUniqueId();
        this.blackName = player.getName();
        this.status = GameStatus.ACTIVE;
        this.currentTurn = PieceColor.WHITE;
    }

    /** Kendi kendine oynama: davet/kabul akisi olmadan dogrudan baslat. */
    public void startSelfPlay() {
        this.blackId = whiteId;
        this.blackName = whiteName;
        this.status = GameStatus.ACTIVE;
        this.currentTurn = PieceColor.WHITE;
    }

    // ---------------------------------------------------------------
    // Rovans (rematch)
    // ---------------------------------------------------------------

    /** Rovans icin tahtayi baslangic dizilimine sifirlar, renkler yer degistirir. */
    public void resetForRematch(UUID newWhiteId, String newWhiteName, UUID newBlackId, String newBlackName) {
        board.setupInitialPosition();
        this.whiteId = newWhiteId;
        this.whiteName = newWhiteName;
        this.blackId = newBlackId;
        this.blackName = newBlackName;
        this.currentTurn = PieceColor.WHITE;
        this.status = (newBlackId != null) ? GameStatus.ACTIVE : GameStatus.WAITING_FOR_OPPONENT;
        this.selectedFile = -1;
        this.selectedRank = -1;
        this.legalMoves = null;
        this.pendingPromotionMove = null;
        this.pendingPromotionPlayer = null;
    }

    public boolean isFinished() {
        return status == GameStatus.WHITE_WON || status == GameStatus.BLACK_WON || status == GameStatus.DRAW;
    }

    // ---------------------------------------------------------------
    // Kayitli durumdan geri yukleme icin dogrudan ayarlayicilar
    // ---------------------------------------------------------------

    public void restoreBlack(UUID blackId, String blackName) {
        this.blackId = blackId;
        this.blackName = blackName;
    }

    public void restoreCurrentTurn(PieceColor turn) {
        this.currentTurn = turn;
    }

    public void restoreStatus(GameStatus status) {
        this.status = status;
    }

    public boolean hasPendingPromotion() {
        return pendingPromotionMove != null;
    }

    public UUID getPendingPromotionPlayer() {
        return pendingPromotionPlayer;
    }

    public ChessMove getPendingPromotionMove() {
        return pendingPromotionMove;
    }

    public int getSelectedFile() {
        return selectedFile;
    }

    public int getSelectedRank() {
        return selectedRank;
    }

    public List<ChessMove> getLegalMoves() {
        return legalMoves;
    }

    // ---------------------------------------------------------------
    // Hamle akisi
    // ---------------------------------------------------------------

    /**
     * @return true ise gorsel (grid) yeniden cizilmeli.
     */
    public boolean handleSquareClick(Player clicker, int file, int rank) {
        if (status != GameStatus.ACTIVE) {
            clicker.sendMessage(ChatColor.YELLOW + "Bu oyun aktif degil.");
            return false;
        }
        PieceColor clickerColor = colorOf(clicker.getUniqueId());
        if (clickerColor == null) return false;
        if (pendingPromotionMove != null) {
            clicker.sendMessage(ChatColor.YELLOW + "Once terfi seciminin tamamlanmasi bekleniyor.");
            return false;
        }
        if (clickerColor != currentTurn) {
            clicker.sendMessage(ChatColor.RED + "Sira sizde degil!");
            return false;
        }

        if (selectedFile == -1) {
            return trySelect(clicker, file, rank, clickerColor);
        } else {
            return handleSecondClick(clicker, file, rank, clickerColor);
        }
    }

    private boolean trySelect(Player clicker, int file, int rank, PieceColor clickerColor) {
        ChessPiece piece = board.getPiece(file, rank);
        if (piece == null || piece.getColor() != clickerColor) {
            return false;
        }
        List<ChessMove> legal = board.getLegalMovesFrom(file, rank);
        if (legal.isEmpty()) {
            clicker.sendMessage(ChatColor.YELLOW + "Bu tas icin gecerli hamle yok.");
            return false;
        }
        selectedFile = file;
        selectedRank = rank;
        legalMoves = legal;
        return true;
    }

    private boolean handleSecondClick(Player clicker, int file, int rank, PieceColor clickerColor) {
        if (file == selectedFile && rank == selectedRank) {
            clearSelection();
            return true;
        }

        ChessPiece clickedPiece = board.getPiece(file, rank);
        if (clickedPiece != null && clickedPiece.getColor() == clickerColor) {
            clearSelection();
            return trySelect(clicker, file, rank, clickerColor);
        }

        ChessMove chosen = null;
        for (ChessMove m : legalMoves) {
            if (m.toFile == file && m.toRank == rank) {
                chosen = m;
                break;
            }
        }
        if (chosen == null) {
            clicker.sendMessage(ChatColor.RED + "Gecersiz hamle.");
            return false;
        }

        int fromFile = selectedFile, fromRank = selectedRank;
        clearSelection();

        if (board.isPromotionMove(fromFile, fromRank, chosen.toFile, chosen.toRank)) {
            pendingPromotionMove = chosen;
            pendingPromotionPlayer = clicker.getUniqueId();
            return true;
        }

        executeMove(chosen);
        return true;
    }

    private void clearSelection() {
        selectedFile = -1;
        selectedRank = -1;
        legalMoves = null;
    }

    public void resolvePromotion(PieceType chosenType) {
        if (pendingPromotionMove == null) return;
        pendingPromotionMove.promotion = chosenType;
        ChessMove move = pendingPromotionMove;
        pendingPromotionMove = null;
        pendingPromotionPlayer = null;
        executeMove(move);
    }

    private void executeMove(ChessMove move) {
        board.applyMove(move);
        broadcast(ChatColor.GRAY + nameOf(currentTurn) + " oynadi: " + ChatColor.WHITE + move.toString());
        switchTurnAndCheckEnd();
    }

    private void switchTurnAndCheckEnd() {
        currentTurn = currentTurn.opposite();

        if (board.isCheckmate(currentTurn)) {
            status = currentTurn == PieceColor.WHITE ? GameStatus.BLACK_WON : GameStatus.WHITE_WON;
            String winner = currentTurn == PieceColor.WHITE ? blackName : whiteName;
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "SAH MAT! Kazanan: " + winner);
            return;
        }
        if (board.isStalemate(currentTurn)) {
            status = GameStatus.DRAW;
            broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "PAT! Oyun berabere bitti.");
            return;
        }
        if (board.isInCheck(currentTurn)) {
            broadcast(ChatColor.RED + "" + ChatColor.BOLD + "SAH! Sira: " + nameOf(currentTurn));
        } else {
            broadcast(ChatColor.YELLOW + "Sira: " + nameOf(currentTurn));
        }
    }

    private String nameOf(PieceColor color) {
        if (isSelfPlay()) {
            return whiteName + " (" + (color == PieceColor.WHITE ? "Beyaz" : "Siyah") + ")";
        }
        return color == PieceColor.WHITE ? (whiteName + " (Beyaz)") : (blackName + " (Siyah)");
    }

    public void resign(Player resigner) {
        PieceColor c = colorOf(resigner.getUniqueId());
        if (c == null || status != GameStatus.ACTIVE) return;
        status = c == PieceColor.WHITE ? GameStatus.BLACK_WON : GameStatus.WHITE_WON;
        String winner = c == PieceColor.WHITE ? blackName : whiteName;
        broadcast(ChatColor.GOLD + resigner.getName() + " oyunu terk etti. Kazanan: " + winner);
    }

    public void broadcast(String message) {
        Player w = whiteId != null ? Bukkit.getPlayer(whiteId) : null;
        Player b = blackId != null ? Bukkit.getPlayer(blackId) : null;
        if (w != null) w.sendMessage(message);
        if (b != null && !b.equals(w)) b.sendMessage(message);
    }
}
