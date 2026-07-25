package com.chessplugin.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

/**
 * "Taslar" (tas kesesi) ve "Satranc Masasi" esyalarini olusturur.
 *
 * Ikisi de vanilla bir blogu/esyayi TASIMAZ: gorunumleri tamamen
 * "chessplugin:" adiyla ozel bir kaynak paketinde tanimlanan modeller ve
 * dokulardan gelir (ItemMeta#setItemModel). Taban materyal olarak
 * Material.PAPER kullanilir; PAPER'in hicbir varsayilan islevi
 * (yerlestirme, acilma, yenme vb.) olmadigi icin bu tamamen guvenlidir ve
 * vanilla hicbir davranisla catismaz. Satranc Masasi, gercek bir Minecraft
 * blogu OLARAK yerlestirilmez; bunun yerine dunyada bir ItemDisplay +
 * Interaction varlik cifti olarak "sahte blok" seklinde temsil edilir
 * (bkz. world.ChessBoardEntity).
 */
public class ChessItems {

    public static final Material ITEM_BASE_MATERIAL = Material.PAPER;

    private final NamespacedKey pouchMarkerKey;
    private final NamespacedKey boardItemMarkerKey;
    public final NamespacedKey pouchModelKey;
    public final NamespacedKey boardModelKey;

    public ChessItems(Plugin plugin) {
        this.pouchMarkerKey = new NamespacedKey(plugin, "pieces_pouch");
        this.boardItemMarkerKey = new NamespacedKey(plugin, "board_item");
        this.pouchModelKey = new NamespacedKey("chessplugin", "pieces_pouch");
        this.boardModelKey = new NamespacedKey("chessplugin", "chess_table");
    }

    public ItemStack createPouch() {
        ItemStack item = new ItemStack(ITEM_BASE_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Taslar");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Satranc taslarini icerir.",
                ChatColor.GRAY + "Bir Satranc Masasina sag tiklayarak",
                ChatColor.GRAY + "taslari otomatik yerlestirin."
        ));
        trySetItemModel(meta, pouchModelKey);
        meta.getPersistentDataContainer().set(pouchMarkerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isPouch(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte val = item.getItemMeta().getPersistentDataContainer().get(pouchMarkerKey, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    public ItemStack createBoardItem() {
        ItemStack item = new ItemStack(ITEM_BASE_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Satranc Masasi");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Istediginiz bir bloğun ustune",
                ChatColor.GRAY + "sag tiklayarak yerlestirin.",
                ChatColor.GRAY + "Uzerine 'Taslar' kesesiyle sag tiklayin,",
                ChatColor.GRAY + "sonra tekrar sag tiklayarak oynayin."
        ));
        trySetItemModel(meta, boardModelKey);
        meta.getPersistentDataContainer().set(boardItemMarkerKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Dunyada goruntulenecek (ItemDisplay icindeki) kopya; envanterdeki
     * lore/isim gorunmesin diye sade tutulur.
     */
    public ItemStack createBoardDisplayItem() {
        ItemStack item = new ItemStack(ITEM_BASE_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();
        trySetItemModel(meta, boardModelKey);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isBoardItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte val = item.getItemMeta().getPersistentDataContainer().get(boardItemMarkerKey, PersistentDataType.BYTE);
        return val != null && val == (byte) 1;
    }

    private void trySetItemModel(ItemMeta meta, NamespacedKey modelKey) {
        try {
            meta.setItemModel(modelKey);
        } catch (Throwable t) {
            // Cok eski bir API'ye karsi calisiliyorsa sessizce yoksay;
            // esya yine de calisir, sadece gorunumu vanilla PAPER kalir.
        }
    }
}
