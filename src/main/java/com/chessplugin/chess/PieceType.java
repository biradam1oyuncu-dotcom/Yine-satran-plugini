package com.chessplugin.chess;

public enum PieceType {
    PAWN("Piyon", "\u2659", "\u265F"),
    KNIGHT("At", "\u2658", "\u265E"),
    BISHOP("Fil", "\u2657", "\u265D"),
    ROOK("Kale", "\u2656", "\u265C"),
    QUEEN("Vezir", "\u2655", "\u265B"),
    KING("Sah", "\u2654", "\u265A");

    private final String turkishName;
    private final String whiteGlyph;
    private final String blackGlyph;

    PieceType(String turkishName, String whiteGlyph, String blackGlyph) {
        this.turkishName = turkishName;
        this.whiteGlyph = whiteGlyph;
        this.blackGlyph = blackGlyph;
    }

    public String getTurkishName() {
        return turkishName;
    }

    public String glyph(PieceColor color) {
        return color == PieceColor.WHITE ? whiteGlyph : blackGlyph;
    }
}
