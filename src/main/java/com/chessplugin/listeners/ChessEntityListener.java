package com.chessplugin.listeners;

import com.chessplugin.game.BoardGame;
import com.chessplugin.game.GameManager;
import com.chessplugin.game.GameStatus;
import com.chessplugin.items.ChessItems;
import com.chessplugin.gui.PromotionGuiRenderer;
import com.chessplugin.world.ChessBoardEntity;
import com.chessplugin.world.GridBoardRenderer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChessEntityListener implements Listener {

    private static final long MINING_RESET_MS = 3000L;

    private final GameManager gameManager;
    private final ChessBoardEntity boardEntity;
    private final GridBoardRenderer gridRenderer;
    private final ChessItems items;

    private final Map<String, MiningProgress> miningProgress = new HashMap<>();

    public ChessEntityListener(GameManager gameManager, ChessBoardEntity boardEntity,
                                GridBoardRenderer gridRenderer, ChessItems items) {
        this.gameManager = gameManager;
        this.boardEntity = boardEntity;
        this.gridRenderer = gridRenderer;
        this.items = items;
    }

    // ---------------------------------------------------------------
    // Yerlestirme
    // ---------------------------------------------------------------

    @EventHandler
    public void onPlaceAttempt(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = event.getItem();
        if (!items.isBoardItem(hand)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null || event.getBlockFace() == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        Block target = clicked.getRelative(event.getBlockFace());
        if (target.getType() != Material.AIR) {
            player.sendMessage(ChatColor.RED + "Buraya yerlestirilemez, o alan bos degil.");
            return;
        }

        // Onceki bir tahtadan kalmis "hayalet" varlik olmadigindan emin ol
        // (kirma sonrasi ayni yere tekrar koyamama sorununu onler).
        clearStrayEntities(target.getLocation());

        boardEntity.spawn(target.getLocation(), items);

        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.sendMessage(ChatColor.GREEN + "Satranc Masasi yerlestirildi. Uzerine "
                + ChatColor.GOLD + "Taslar" + ChatColor.GREEN + " kesesiyle sag tiklayin.");
    }

    private void clearStrayEntities(Location blockLoc) {
        Location center = blockLoc.clone().add(0.5, 0.6, 0.5);
        for (Entity e : center.getWorld().getNearbyEntities(center, 0.9, 1.4, 0.9)) {
            if (e instanceof Interaction || e instanceof ItemDisplay) {
                e.remove();
            }
        }
    }

    // ---------------------------------------------------------------
    // Etkilesim (sag tiklama)
    // ---------------------------------------------------------------

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();

        if (boardEntity.isBoard(clicked)) {
            handleMainBoardInteract(event, (Interaction) clicked);
        } else if (gridRenderer.isGridCell(clicked)) {
            handleGridCellInteract(event, clicked);
        } else if (gridRenderer.isRematchButton(clicked)) {
            handleRematchInteract(event, clicked);
        }
    }

    private void handleMainBoardInteract(PlayerInteractEntityEvent event, Interaction interaction) {
        event.setCancelled(true);
        Player player = event.getPlayer();

        boolean loaded = boardEntity.isLoaded(interaction);
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (items.isPouch(hand)) {
            handlePouchUse(player, interaction, loaded, hand);
            return;
        }

        if (!loaded) {
            player.sendMessage(ChatColor.YELLOW + "Bu Satranc Masasina henuz tas yerlestirilmedi. "
                    + "Elinize " + ChatColor.GOLD + "Taslar" + ChatColor.YELLOW + " kesesini alip sag tiklayin.");
            return;
        }

        BoardGame game = gameManager.getOrLoadGame(interaction, boardEntity);
        if (game == null) {
            player.sendMessage(ChatColor.YELLOW + "Bu tahtada henuz bir oyun yok. Tahtaya bakarken "
                    + ChatColor.WHITE + "/chess invite <oyuncu>" + ChatColor.YELLOW
                    + " yazin (kendinizle oynamak icin kendi adinizi yazabilirsiniz).");
            return;
        }
        game.setEntityId(interaction.getUniqueId());

        if (!game.isParticipant(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Bu oyun size ait degil.");
            return;
        }

        gridRenderer.ensureGrid(interaction, game);

        if (game.getStatus() == GameStatus.WAITING_FOR_OPPONENT) {
            player.sendMessage(ChatColor.YELLOW + "Rakibinizin daveti kabul etmesi bekleniyor.");
        } else if (game.getStatus() == GameStatus.ACTIVE) {
            player.sendMessage(ChatColor.GRAY + "Tahtanin ustundeki karelere tiklayarak oynayin. Sira: "
                    + (game.getCurrentTurn() == com.chessplugin.chess.PieceColor.WHITE ? "Beyaz" : "Siyah"));
        }
    }

    private void handlePouchUse(Player player, Interaction interaction, boolean loaded, ItemStack hand) {
        if (loaded) {
            player.sendMessage(ChatColor.YELLOW + "Bu Satranc Masasina zaten taslar yerlestirilmis.");
            return;
        }

        if (hand.getAmount() > 1) {
            hand.setAmount(hand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        boardEntity.setLoaded(interaction, true);

        Location center = interaction.getLocation().clone().add(0, 0.5, 0);
        interaction.getWorld().spawnParticle(Particle.CLOUD, center, 25, 0.3, 0.2, 0.3, 0.02);
        interaction.getWorld().playSound(interaction.getLocation(), Sound.BLOCK_WOOD_PLACE, 1f, 1.4f);

        player.sendMessage(ChatColor.GREEN + "Taslar tahtaya yerlestirildi! Rakip davet etmek icin "
                + "tahtaya bakarken " + ChatColor.WHITE + "/chess invite <oyuncu>" + ChatColor.GREEN
                + " yazin (kendinizle oynamak icin kendi kullanici adinizi yazabilirsiniz).");
    }

    private void handleGridCellInteract(PlayerInteractEntityEvent event, Entity clicked) {
        UUID parentId = gridRenderer.parentBoardOf(clicked);
        if (parentId == null) return;
        Entity boardEnt = Bukkit.getEntity(parentId);
        if (!(boardEnt instanceof Interaction)) return;
        Interaction board = (Interaction) boardEnt;

        BoardGame game = gameManager.getOrLoadGame(board, boardEntity);
        if (game == null) return;
        game.setEntityId(parentId);

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!game.isParticipant(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Bu oyun size ait degil.");
            return;
        }

        int[] cell = gridRenderer.cellOf(clicked);
        if (cell == null) return;

        boolean changed = game.handleSquareClick(player, cell[0], cell[1]);

        if (game.hasPendingPromotion() && player.getUniqueId().equals(game.getPendingPromotionPlayer())) {
            gameManager.registerPendingPromotionView(player, parentId);
            PromotionGuiRenderer.open(player, game, game.colorOf(player.getUniqueId()));
            return;
        }

        if (changed) {
            gridRenderer.refresh(board, game);
            persist(board, game);
        }
    }

    private void handleRematchInteract(PlayerInteractEntityEvent event, Entity clicked) {
        UUID parentId = gridRenderer.parentBoardOf(clicked);
        if (parentId == null) return;
        Entity boardEnt = Bukkit.getEntity(parentId);
        if (!(boardEnt instanceof Interaction)) return;
        Interaction board = (Interaction) boardEnt;

        BoardGame game = gameManager.getOrLoadGame(board, boardEntity);
        if (game == null) return;
        game.setEntityId(parentId);

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!game.isFinished()) {
            player.sendMessage(ChatColor.YELLOW + "Oyun henuz bitmedi.");
            return;
        }
        if (!game.isParticipant(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Bu oyun size ait degil.");
            return;
        }

        if (game.isSelfPlay()) {
            game.resetForRematch(player.getUniqueId(), player.getName(), player.getUniqueId(), player.getName());
            gridRenderer.refresh(board, game);
            persist(board, game);
            player.sendMessage(ChatColor.GREEN + "Yeni oyun basladi!");
            return;
        }

        UUID opponentId = game.getWhiteId().equals(player.getUniqueId()) ? game.getBlackId() : game.getWhiteId();
        Player opponent = opponentId != null ? Bukkit.getPlayer(opponentId) : null;
        if (opponent == null) {
            player.sendMessage(ChatColor.RED + "Rakibiniz su an cevrimdisi, rovans su an teklif edilemiyor.");
            return;
        }

        game.resetForRematch(player.getUniqueId(), player.getName(), null, null);
        gameManager.invite(opponent, parentId);
        gridRenderer.refresh(board, game);
        persist(board, game);

        player.sendMessage(ChatColor.GREEN + "Rovans teklif edildi: " + opponent.getName());
        opponent.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW
                + " sizinle rovans yapmak istiyor! Kabul icin: " + ChatColor.WHITE + "/chess accept");
    }

    private void persist(Interaction board, BoardGame game) {
        boardEntity.saveState(board, game);
    }

    // ---------------------------------------------------------------
    // Kirma
    // ---------------------------------------------------------------

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Entity victim = event.getEntity();
        if (!boardEntity.isBoard(victim)) return;

        event.setCancelled(true);
        Interaction interaction = (Interaction) victim;
        Player player = (Player) event.getDamager();

        ItemStack tool = player.getInventory().getItemInMainHand();
        int required = requiredHits(tool);

        String key = player.getUniqueId() + ":" + interaction.getUniqueId();
        long now = System.currentTimeMillis();
        MiningProgress progress = miningProgress.get(key);
        if (progress == null || now - progress.lastHitMillis > MINING_RESET_MS) {
            progress = new MiningProgress();
        }
        progress.hits++;
        progress.lastHitMillis = now;

        if (progress.hits >= required) {
            miningProgress.remove(key);
            breakBoard(player, interaction);
        } else {
            miningProgress.put(key, progress);
            int percent = Math.min(99, (int) Math.round(100.0 * progress.hits / required));
            player.sendMessage(ChatColor.GRAY + "Satranc Masasi kiriliyor... " + ChatColor.YELLOW + percent + "%");
        }
    }

    private int requiredHits(ItemStack tool) {
        if (tool == null) return 20;
        String name = tool.getType().name();
        if (!name.endsWith("_PICKAXE")) return 20;
        if (name.equals("NETHERITE_PICKAXE")) return 2;
        if (name.equals("DIAMOND_PICKAXE")) return 3;
        if (name.equals("IRON_PICKAXE")) return 5;
        if (name.equals("STONE_PICKAXE")) return 8;
        return 12;
    }

    private void breakBoard(Player player, Interaction interaction) {
        Location loc = interaction.getLocation();
        boolean loaded = boardEntity.isLoaded(interaction);

        BoardGame game = gameManager.getGame(interaction.getUniqueId());
        if (game != null) {
            game.broadcast(ChatColor.RED + "Satranc Masasi kirildi, oyun sona erdi.");
            gameManager.removeGame(interaction.getUniqueId());
        }

        gridRenderer.removeGrid(interaction.getUniqueId());
        boardEntity.remove(interaction);
        clearStrayEntities(loc.getBlock().getLocation());

        loc.getWorld().dropItemNaturally(loc, items.createBoardItem());
        if (loaded) {
            loc.getWorld().dropItemNaturally(loc, items.createPouch());
        }
        loc.getWorld().playSound(loc, Sound.BLOCK_WOOD_BREAK, 1f, 1f);

        player.sendMessage(ChatColor.GREEN + "Satranc Masasini kirdiniz.");
    }

    private static class MiningProgress {
        int hits = 0;
        long lastHitMillis = 0L;
    }
}
