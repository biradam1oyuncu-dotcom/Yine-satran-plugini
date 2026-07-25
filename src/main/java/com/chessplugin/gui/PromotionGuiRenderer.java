package com.chessplugin.gui;

import com.chessplugin.chess.PieceColor;
import com.chessplugin.chess.PieceType;
import com.chessplugin.game.BoardGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PromotionGuiRenderer {

    public static final String TITLE = ChatColor.DARK_PURPLE + "Terfi: Bir tas secin";
    public static final int SIZE = 9;

    public static Inventory open(Player player, BoardGame game, PieceColor color) {
        PromotionGuiHolder holder = new PromotionGuiHolder(game);
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.setInventory(inv);

        inv.setItem(2, buildItem(Material.DIAMOND, PieceType.QUEEN, color));
        inv.setItem(3, buildItem(Material.IRON_BLOCK, PieceType.ROOK, color));
        inv.setItem(5, buildItem(Material.ENDER_EYE, PieceType.BISHOP, color));
        inv.setItem(6, buildItem(Material.SADDLE, PieceType.KNIGHT, color));

        player.openInventory(inv);
        return inv;
    }

    private static ItemStack buildItem(Material material, PieceType type, PieceColor color) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + type.glyph(color) + " " + type.getTurkishName() + "'e terfi et");
        item.setItemMeta(meta);
        return item;
    }

    public static PieceType typeForSlot(int slot) {
        switch (slot) {
            case 2: return PieceType.QUEEN;
            case 3: return PieceType.ROOK;
            case 5: return PieceType.BISHOP;
            case 6: return PieceType.KNIGHT;
            default: return null;
        }
    }
}
