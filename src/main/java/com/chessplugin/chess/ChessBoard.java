package com.chessplugin.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * Standart 8x8 satranc kurallarini uygulayan, Bukkit/Spigot'tan tamamen
 * bagimsiz saf bir mantik siniifi. file: 0-7 (a-h), rank: 0-7 (1-8).
 */
public class ChessBoard {

    private final ChessPiece[][] squares = new ChessPiece[8][8];

    // Bir onceki hamlede cift kare ileri giden piyonun "gecebilecegi" (en passant) kare.
    private int enPassantTargetFile = -1;
    private int enPassantTargetRank = -1;

    public ChessBoard() {
        setupInitialPosition();
    }

    public void setupInitialPosition() {
        for (ChessPiece[] col : squares) {
            java.util.Arrays.fill(col, null);
        }
        PieceType[] backRank = {PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK};

        for (int file = 0; file < 8; file++) {
            squares[file][0] = new ChessPiece(backRank[file], PieceColor.WHITE);
            squares[file][1] = new ChessPiece(PieceType.PAWN, PieceColor.WHITE);
            squares[file][6] = new ChessPiece(PieceType.PAWN, PieceColor.BLACK);
            squares[file][7] = new ChessPiece(backRank[file], PieceColor.BLACK);
        }
        enPassantTargetFile = -1;
        enPassantTargetRank = -1;
    }

    /**
     * Tahtayi tamamen bosaltir (baslangic dizilimine SIFIRLAMAZ). Kayitli bir
     * oyun durumunu disaridan (orn. bir varligin PersistentDataContainer'indan)
     * geri yuklerken kullanilir.
     */
    public void clear() {
        for (ChessPiece[] col : squares) {
            java.util.Arrays.fill(col, null);
        }
        enPassantTargetFile = -1;
        enPassantTargetRank = -1;
    }

    public static boolean isInside(int file, int rank) {
        return file >= 0 && file < 8 && rank >= 0 && rank < 8;
    }

    public ChessPiece getPiece(int file, int rank) {
        if (!isInside(file, rank)) return null;
        return squares[file][rank];
    }

    public void setPiece(int file, int rank, ChessPiece piece) {
        squares[file][rank] = piece;
    }

    public ChessBoard copy() {
        ChessBoard b = new ChessBoard();
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                ChessPiece p = squares[f][r];
                b.squares[f][r] = (p == null) ? null : p.copy();
            }
        }
        b.enPassantTargetFile = this.enPassantTargetFile;
        b.enPassantTargetRank = this.enPassantTargetRank;
        return b;
    }

    // ---------------------------------------------------------------
    // Saldiri tespiti (sah cekme / rok guvenligi icin kullanilir)
    // ---------------------------------------------------------------

    public boolean isSquareAttacked(int targetFile, int targetRank, PieceColor byColor) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                ChessPiece p = squares[f][r];
                if (p != null && p.getColor() == byColor && pieceAttacksSquare(p, f, r, targetFile, targetRank)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean pieceAttacksSquare(ChessPiece piece, int pf, int pr, int tf, int tr) {
        int dx = tf - pf;
        int dy = tr - pr;
        switch (piece.getType()) {
            case PAWN: {
                int dir = piece.getColor() == PieceColor.WHITE ? 1 : -1;
                return dy == dir && Math.abs(dx) == 1;
            }
            case KNIGHT: {
                int adx = Math.abs(dx), ady = Math.abs(dy);
                return (adx == 1 && ady == 2) || (adx == 2 && ady == 1);
            }
            case KING: {
                int adx = Math.abs(dx), ady = Math.abs(dy);
                return adx <= 1 && ady <= 1 && !(dx == 0 && dy == 0);
            }
            case ROOK: {
                if (dx != 0 && dy != 0) return false;
                if (dx == 0 && dy == 0) return false;
                return lineOfSightClear(pf, pr, tf, tr);
            }
            case BISHOP: {
                if (Math.abs(dx) != Math.abs(dy) || dx == 0) return false;
                return lineOfSightClear(pf, pr, tf, tr);
            }
            case QUEEN: {
                boolean straight = (dx == 0 || dy == 0) && !(dx == 0 && dy == 0);
                boolean diagonal = Math.abs(dx) == Math.abs(dy) && dx != 0;
                if (!straight && !diagonal) return false;
                return lineOfSightClear(pf, pr, tf, tr);
            }
        }
        return false;
    }

    private boolean lineOfSightClear(int pf, int pr, int tf, int tr) {
        int stepX = Integer.signum(tf - pf);
        int stepY = Integer.signum(tr - pr);
        int x = pf + stepX, y = pr + stepY;
        while (x != tf || y != tr) {
            if (squares[x][y] != null) return false;
            x += stepX;
            y += stepY;
        }
        return true;
    }

    public int[] findKing(PieceColor color) {
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                ChessPiece p = squares[f][r];
                if (p != null && p.getType() == PieceType.KING && p.getColor() == color) {
                    return new int[]{f, r};
                }
            }
        }
        return null;
    }

    public boolean isInCheck(PieceColor color) {
        int[] k = findKing(color);
        if (k == null) return false;
        return isSquareAttacked(k[0], k[1], color.opposite());
    }

    // ---------------------------------------------------------------
    // Sozde-yasal (pseudo-legal) hamle uretimi
    // ---------------------------------------------------------------

    private static final int[][] KNIGHT_OFFSETS = {
            {1, 2}, {2, 1}, {2, -1}, {1, -2}, {-1, -2}, {-2, -1}, {-2, 1}, {-1, 2}
    };
    private static final int[][] ROOK_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[][] BISHOP_DIRS = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    public List<ChessMove> generatePseudoLegalMoves(int file, int rank) {
        List<ChessMove> moves = new ArrayList<>();
        ChessPiece piece = squares[file][rank];
        if (piece == null) return moves;

        switch (piece.getType()) {
            case PAWN:
                generatePawnMoves(piece, file, rank, moves);
                break;
            case KNIGHT:
                for (int[] off : KNIGHT_OFFSETS) {
                    addSimpleMoveIfValid(piece, file, rank, file + off[0], rank + off[1], moves);
                }
                break;
            case BISHOP:
                generateSlidingMoves(piece, file, rank, BISHOP_DIRS, moves);
                break;
            case ROOK:
                generateSlidingMoves(piece, file, rank, ROOK_DIRS, moves);
                break;
            case QUEEN:
                generateSlidingMoves(piece, file, rank, ROOK_DIRS, moves);
                generateSlidingMoves(piece, file, rank, BISHOP_DIRS, moves);
                break;
            case KING:
                generateKingMoves(piece, file, rank, moves);
                break;
        }
        return moves;
    }

    private void generatePawnMoves(ChessPiece piece, int file, int rank, List<ChessMove> moves) {
        PieceColor color = piece.getColor();
        int dir = color == PieceColor.WHITE ? 1 : -1;
        int promotionRank = color == PieceColor.WHITE ? 7 : 0;

        int oneRank = rank + dir;
        if (isInside(file, oneRank) && squares[file][oneRank] == null) {
            ChessMove m = new ChessMove(file, rank, file, oneRank);
            if (oneRank == promotionRank) {
                m.promotion = PieceType.QUEEN; // gercek secim oyun katmaninda yapilir
            }
            moves.add(m);

            if (!piece.hasMoved()) {
                int twoRank = rank + 2 * dir;
                if (isInside(file, twoRank) && squares[file][twoRank] == null) {
                    moves.add(new ChessMove(file, rank, file, twoRank));
                }
            }
        }

        for (int dfile : new int[]{-1, 1}) {
            int tf = file + dfile;
            int tr = rank + dir;
            if (!isInside(tf, tr)) continue;
            ChessPiece target = squares[tf][tr];
            if (target != null && target.getColor() != color) {
                ChessMove m = new ChessMove(file, rank, tf, tr);
                m.isCapture = true;
                if (tr == promotionRank) {
                    m.promotion = PieceType.QUEEN;
                }
                moves.add(m);
            } else if (target == null && tf == enPassantTargetFile && tr == enPassantTargetRank) {
                ChessMove m = new ChessMove(file, rank, tf, tr);
                m.isCapture = true;
                m.isEnPassant = true;
                moves.add(m);
            }
        }
    }

    private void generateSlidingMoves(ChessPiece piece, int file, int rank, int[][] dirs, List<ChessMove> moves) {
        for (int[] dir : dirs) {
            int f = file + dir[0];
            int r = rank + dir[1];
            while (isInside(f, r)) {
                ChessPiece target = squares[f][r];
                if (target == null) {
                    moves.add(new ChessMove(file, rank, f, r));
                } else {
                    if (target.getColor() != piece.getColor()) {
                        ChessMove m = new ChessMove(file, rank, f, r);
                        m.isCapture = true;
                        moves.add(m);
                    }
                    break;
                }
                f += dir[0];
                r += dir[1];
            }
        }
    }

    private void addSimpleMoveIfValid(ChessPiece piece, int file, int rank, int tf, int tr, List<ChessMove> moves) {
        if (!isInside(tf, tr)) return;
        ChessPiece target = squares[tf][tr];
        if (target == null) {
            moves.add(new ChessMove(file, rank, tf, tr));
        } else if (target.getColor() != piece.getColor()) {
            ChessMove m = new ChessMove(file, rank, tf, tr);
            m.isCapture = true;
            moves.add(m);
        }
    }

    private void generateKingMoves(ChessPiece piece, int file, int rank, List<ChessMove> moves) {
        for (int df = -1; df <= 1; df++) {
            for (int dr = -1; dr <= 1; dr++) {
                if (df == 0 && dr == 0) continue;
                addSimpleMoveIfValid(piece, file, rank, file + df, rank + dr, moves);
            }
        }

        if (piece.hasMoved()) return;
        PieceColor color = piece.getColor();
        int homeRank = color == PieceColor.WHITE ? 0 : 7;
        if (rank != homeRank || file != 4) return;
        if (isInCheck(color)) return;
        PieceColor enemy = color.opposite();

        // Sah tarafi rok (kisa rok)
        ChessPiece kingRook = squares[7][homeRank];
        if (kingRook != null && kingRook.getType() == PieceType.ROOK && !kingRook.hasMoved()
                && squares[5][homeRank] == null && squares[6][homeRank] == null
                && !isSquareAttacked(5, homeRank, enemy) && !isSquareAttacked(6, homeRank, enemy)) {
            ChessMove m = new ChessMove(file, rank, 6, homeRank);
            m.isCastleKingSide = true;
            moves.add(m);
        }

        // Vezir tarafi rok (uzun rok)
        ChessPiece queenRook = squares[0][homeRank];
        if (queenRook != null && queenRook.getType() == PieceType.ROOK && !queenRook.hasMoved()
                && squares[1][homeRank] == null && squares[2][homeRank] == null && squares[3][homeRank] == null
                && !isSquareAttacked(3, homeRank, enemy) && !isSquareAttacked(2, homeRank, enemy)) {
            ChessMove m = new ChessMove(file, rank, 2, homeRank);
            m.isCastleQueenSide = true;
            moves.add(m);
        }
    }

    // ---------------------------------------------------------------
    // Yasal hamle uretimi (kendi sahini tehlikeye atan hamleler elenir)
    // ---------------------------------------------------------------

    public List<ChessMove> generateAllLegalMoves(PieceColor color) {
        List<ChessMove> legal = new ArrayList<>();
        for (int f = 0; f < 8; f++) {
            for (int r = 0; r < 8; r++) {
                ChessPiece p = squares[f][r];
                if (p != null && p.getColor() == color) {
                    for (ChessMove m : generatePseudoLegalMoves(f, r)) {
                        ChessBoard clone = this.copy();
                        clone.applyMove(m);
                        if (!clone.isInCheck(color)) {
                            legal.add(m);
                        }
                    }
                }
            }
        }
        return legal;
    }

    public List<ChessMove> getLegalMovesFrom(int file, int rank) {
        ChessPiece p = squares[file][rank];
        if (p == null) return new ArrayList<>();
        List<ChessMove> result = new ArrayList<>();
        for (ChessMove m : generateAllLegalMoves(p.getColor())) {
            if (m.fromFile == file && m.fromRank == rank) {
                result.add(m);
            }
        }
        return result;
    }

    public boolean isCheckmate(PieceColor color) {
        return isInCheck(color) && generateAllLegalMoves(color).isEmpty();
    }

    public boolean isStalemate(PieceColor color) {
        return !isInCheck(color) && generateAllLegalMoves(color).isEmpty();
    }

    // ---------------------------------------------------------------
    // Hamle uygulama
    // ---------------------------------------------------------------

    /**
     * Hamleyi tahtaya uygular. Cagiran taraf, terfi durumunda move.promotion
     * alanini onceden gercek secime gore ayarlamis olmalidir (varsayilan VEZIR).
     */
    public void applyMove(ChessMove move) {
        ChessPiece piece = squares[move.fromFile][move.fromRank];
        if (piece == null) return;

        ChessPiece captured = squares[move.toFile][move.toRank];
        if (move.isEnPassant) {
            squares[move.toFile][move.fromRank] = null;
            move.isCapture = true;
        } else if (captured != null) {
            move.isCapture = true;
        }

        squares[move.toFile][move.toRank] = piece;
        squares[move.fromFile][move.fromRank] = null;
        piece.setHasMoved(true);

        int homeRank = move.fromRank;
        if (move.isCastleKingSide) {
            ChessPiece rook = squares[7][homeRank];
            squares[7][homeRank] = null;
            squares[5][homeRank] = rook;
            if (rook != null) rook.setHasMoved(true);
        } else if (move.isCastleQueenSide) {
            ChessPiece rook = squares[0][homeRank];
            squares[0][homeRank] = null;
            squares[3][homeRank] = rook;
            if (rook != null) rook.setHasMoved(true);
        }

        if (move.promotion != null && piece.getType() == PieceType.PAWN) {
            piece.setType(move.promotion);
        }

        // En passant hakkini guncelle
        enPassantTargetFile = -1;
        enPassantTargetRank = -1;
        if (piece.getType() == PieceType.PAWN && Math.abs(move.toRank - move.fromRank) == 2) {
            enPassantTargetFile = move.toFile;
            enPassantTargetRank = (move.fromRank + move.toRank) / 2;
        }
    }

    public boolean isPromotionMove(int fromFile, int fromRank, int toFile, int toRank) {
        ChessPiece p = squares[fromFile][fromRank];
        if (p == null || p.getType() != PieceType.PAWN) return false;
        int promotionRank = p.getColor() == PieceColor.WHITE ? 7 : 0;
        return toRank == promotionRank;
    }
}
