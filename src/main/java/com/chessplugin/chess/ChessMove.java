package com.chessplugin.chess;

/**
 * file: 0-7 (a-h), rank: 0-7 (1-8) seklinde kare koordinatlari kullanilir.
 */
public class ChessMove {

    public final int fromFile, fromRank;
    public final int toFile, toRank;

    public boolean isCapture = false;
    public boolean isEnPassant = false;
    public boolean isCastleKingSide = false;
    public boolean isCastleQueenSide = false;
    public PieceType promotion = null; // null ise terfi yok

    public ChessMove(int fromFile, int fromRank, int toFile, int toRank) {
        this.fromFile = fromFile;
        this.fromRank = fromRank;
        this.toFile = toFile;
        this.toRank = toRank;
    }

    public static String toAlgebraic(int file, int rank) {
        char f = (char) ('a' + file);
        int r = rank + 1;
        return "" + f + r;
    }

    @Override
    public String toString() {
        String s = toAlgebraic(fromFile, fromRank) + toAlgebraic(toFile, toRank);
        if (promotion != null) {
            s += "=" + promotion.name().charAt(0);
        }
        return s;
    }
}
