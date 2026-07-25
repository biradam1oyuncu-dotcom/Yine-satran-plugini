package com.chessplugin.gui;

import com.chessplugin.game.BoardGame;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PromotionGuiHolder implements InventoryHolder {

    private final BoardGame game;
    private Inventory inventory;

    public PromotionGuiHolder(BoardGame game) {
        this.game = game;
    }

    public BoardGame getGame() {
        return game;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
