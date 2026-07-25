package com.chessplugin.chess;

/**
 * Tahta uzerindeki tek bir tasi temsil eder. hasMoved bilgisi rok ve
 * piyonun ilk hamlede iki kare gidebilmesi kurallari icin tutulur.
 */
public class ChessPiece {

    private PieceType type;
    private final PieceColor color;
    private boolean hasMoved;

    public ChessPiece(PieceType type, PieceColor color) {
        this.type = type;
        this.color = color;
        this.hasMoved = false;
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        this.type = type;
    }

    public PieceColor getColor() {
        return color;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public ChessPiece copy() {
        ChessPiece p = new ChessPiece(type, color);
        p.hasMoved = this.hasMoved;
        return p;
    }
}
