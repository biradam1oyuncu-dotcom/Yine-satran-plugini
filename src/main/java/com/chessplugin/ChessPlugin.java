package com.chessplugin;

import com.chessplugin.commands.ChessCommand;
import com.chessplugin.game.GameManager;
import com.chessplugin.items.ChessItems;
import com.chessplugin.listeners.ChessEntityListener;
import com.chessplugin.listeners.ChessGuiListener;
import com.chessplugin.recipes.RecipeRegistrar;
import com.chessplugin.world.ChessBoardEntity;
import com.chessplugin.world.GridBoardRenderer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class ChessPlugin extends JavaPlugin {

    private GameManager gameManager;
    private ChessItems items;
    private ChessBoardEntity boardEntity;
    private GridBoardRenderer gridRenderer;

    @Override
    public void onEnable() {
        this.items = new ChessItems(this);
        this.gameManager = new GameManager();
        this.boardEntity = new ChessBoardEntity(this);
        this.gridRenderer = new GridBoardRenderer(this);

        new RecipeRegistrar(this, items).registerAll();

        getServer().getPluginManager().registerEvents(
                new ChessEntityListener(gameManager, boardEntity, gridRenderer, items), this);
        getServer().getPluginManager().registerEvents(
                new ChessGuiListener(gameManager, boardEntity, gridRenderer, this), this);

        PluginCommand cmd = getCommand("chess");
        if (cmd != null) {
            ChessCommand executor = new ChessCommand(gameManager, boardEntity, gridRenderer);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        getLogger().info("ChessPlugin etkinlestirildi. Tarifler kayitli, /chess komutu hazir.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChessPlugin devre disi birakildi.");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ChessItems getItems() {
        return items;
    }

    public ChessBoardEntity getBoardEntity() {
        return boardEntity;
    }

    public GridBoardRenderer getGridRenderer() {
        return gridRenderer;
    }
}
