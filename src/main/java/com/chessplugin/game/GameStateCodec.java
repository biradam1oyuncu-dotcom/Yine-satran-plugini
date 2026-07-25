package com.chessplugin.game;

import com.chessplugin.chess.ChessBoard;
import com.chessplugin.chess.ChessPiece;
import com.chessplugin.chess.PieceColor;
import com.chessplugin.chess.PieceType;
import org.bukkit.Location;

import java.util.UUID;

/**
 * BoardGame durumunu tek satirlik bir metne kodlar/coder; bu metin,
 * Interaction varligin PersistentDataContainer'inda saklanir ve boylece
 * sunucu yeniden baslasa bile (varliklarin kendisi Minecraft tarafindan
 * otomatik kaydedildigi icin) devam eden oyunlar buyuk olcude korunur.
 *
 * Format: whiteId|whiteName|blackId|blackName|turn|status|64karelikTahta
 * Not: hasMoved (rok/en passant haklari) bilgisi bu basit kodlamada
 * saklanmaz; sunucu yeniden baslarsa bu haklar sifirlanmis olabilir.
 */
public class GameStateCodec {

    private static final String NONE = "-";
    private static final String SEP = "|";

    public static String encode(BoardGame game) {
        StringBuilder sb = new StringBuilder();
        sb.append(game.getWhiteId() != null ? game.getWhiteId().toString() : NONE).append(SEP);
        sb.append(safe(game.getWhiteName())).append(SEP);
        sb.append(game.getBlackId() != null ? game.getBlackId().toString() : NONE).append(SEP);
        sb.append(safe(game.getBlackName())).append(SEP);
        sb.append(game.getCurrentTurn() == PieceColor.WHITE ? "W" : "B").append(SEP);
        sb.append(game.getStatus().name()).append(SEP);

        ChessBoard board = game.getBoard();
        StringBuilder squares = new StringBuilder(64);
        for (int rank = 7; rank >= 0; rank--) {
            for (int file = 0; file < 8; file++) {
                ChessPiece p = board.getPiece(file, rank);
                squares.append(charFor(p));
            }
        }
        sb.append(squares);
        return sb.toString();
    }

    public static BoardGame decode(String data, Location location) {
        String[] parts = data.split("\\" + SEP, -1);
        if (parts.length < 7) return null;

        UUID whiteId = parts[0].equals(NONE) ? null : UUID.fromString(parts[0]);
        String whiteName = parts[1];
        UUID blackId = parts[2].equals(NONE) ? null : UUID.fromString(parts[2]);
        String blackName = parts[3];
        PieceColor turn = parts[4].equals("W") ? PieceColor.WHITE : PieceColor.BLACK;
        GameStatus status;
        try {
            status = GameStatus.valueOf(parts[5]);
        } catch (IllegalArgumentException e) {
            status = GameStatus.WAITING_FOR_OPPONENT;
        }
        String squares = parts[6];

        if (whiteId == null) return null;
        BoardGame game = new BoardGame(location, whiteId, whiteName);
        if (blackId != null) {
            game.restoreBlack(blackId, blackName);
        }
        game.restoreCurrentTurn(turn);
        game.restoreStatus(status);

        if (squares.length() == 64) {
            ChessBoard board = game.getBoard();
            board.clear();
            int i = 0;
            for (int rank = 7; rank >= 0; rank--) {
                for (int file = 0; file < 8; file++) {
                    ChessPiece piece = pieceFor(squares.charAt(i));
                    if (piece != null) {
                        board.setPiece(file, rank, piece);
                    }
                    i++;
                }
            }
        }
        return game;
    }

    private static String safe(String s) {
        return s == null ? NONE : s;
    }

    private static char charFor(ChessPiece p) {
        if (p == null) return '.';
        char c;
        switch (p.getType()) {
            case KING: c = 'k'; break;
            case QUEEN: c = 'q'; break;
            case ROOK: c = 'r'; break;
            case BISHOP: c = 'b'; break;
            case KNIGHT: c = 'n'; break;
            case PAWN:
            default: c = 'p'; break;
        }
        return p.getColor() == PieceColor.WHITE ? Character.toUpperCase(c) : c;
    }

    private static ChessPiece pieceFor(char c) {
        if (c == '.') return null;
        PieceColor color = Character.isUpperCase(c) ? PieceColor.WHITE : PieceColor.BLACK;
        PieceType type;
        switch (Character.toLowerCase(c)) {
            case 'k': type = PieceType.KING; break;
            case 'q': type = PieceType.QUEEN; break;
            case 'r': type = PieceType.ROOK; break;
            case 'b': type = PieceType.BISHOP; break;
            case 'n': type = PieceType.KNIGHT; break;
            case 'p': type = PieceType.PAWN; break;
            default: return null;
        }
        return new ChessPiece(type, color);
    }
}
