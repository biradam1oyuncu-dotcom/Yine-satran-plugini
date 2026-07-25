package com.chessplugin.commands;

import com.chessplugin.game.BoardGame;
import com.chessplugin.game.GameManager;
import com.chessplugin.game.GameStatus;
import com.chessplugin.world.ChessBoardEntity;
import com.chessplugin.world.GridBoardRenderer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChessCommand implements CommandExecutor, TabCompleter {

    private static final double REACH = 6.0;

    private final GameManager gameManager;
    private final ChessBoardEntity boardEntity;
    private final GridBoardRenderer gridRenderer;

    public ChessCommand(GameManager gameManager, ChessBoardEntity boardEntity, GridBoardRenderer gridRenderer) {
        this.gameManager = gameManager;
        this.boardEntity = boardEntity;
        this.gridRenderer = gridRenderer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Bu komut yalnizca oyun icinden kullanilabilir.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player);
                break;
            case "decline":
                handleDecline(player);
                break;
            case "resign":
                handleResign(player);
                break;
            case "rematch":
                handleRematch(player);
                break;
            case "help":
            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private Interaction findTargetedBoard(Player player) {
        Vector dir = player.getEyeLocation().getDirection();
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), dir, REACH, e -> boardEntity.isBoard(e));
        if (result == null) return null;
        Entity hit = result.getHitEntity();
        if (hit instanceof Interaction) return (Interaction) hit;
        return null;
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Kullanim: Bir Satranc Masasina bakarken /chess invite <oyuncu>"
                    + " (kendinizle oynamak icin kendi adinizi yazabilirsiniz)");
            return;
        }

        Interaction interaction = findTargetedBoard(player);
        if (interaction == null) {
            player.sendMessage(ChatColor.RED + "Bir Satranc Masasina bakmiyorsunuz. Davet etmek icin tahtaya bakin.");
            return;
        }
        if (!boardEntity.isLoaded(interaction)) {
            player.sendMessage(ChatColor.RED + "Once tahtaya 'Taslar' kesesiyle sag tiklayip taslari yerlestirin.");
            return;
        }
        UUID entityId = interaction.getUniqueId();
        if (gameManager.hasGame(entityId)) {
            player.sendMessage(ChatColor.RED + "Bu tahtada zaten bir oyun var.");
            return;
        }
        if (gameManager.getActiveOrWaitingGameFor(player) != null) {
            player.sendMessage(ChatColor.RED + "Zaten baska bir oyunda/beklemedesiniz. Once /chess resign kullanin.");
            return;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[1]);
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Oyuncu bulunamadi: " + args[1]);
            return;
        }
        boolean selfPlay = targetPlayer.equals(player);
        if (!selfPlay && gameManager.getActiveOrWaitingGameFor(targetPlayer) != null) {
            player.sendMessage(ChatColor.RED + targetPlayer.getName() + " zaten baska bir oyunda/beklemede.");
            return;
        }

        BoardGame game = new BoardGame(interaction.getLocation(), player.getUniqueId(), player.getName());
        game.setEntityId(entityId);
        gameManager.registerGame(entityId, game);

        if (selfPlay) {
            game.startSelfPlay();
            player.sendMessage(ChatColor.GREEN + "Kendi kendinize oynamaya basladiniz! Sira: " + ChatColor.AQUA + "Beyaz" + ChatColor.GREEN + ".");
        } else {
            gameManager.invite(targetPlayer, entityId);
            player.sendMessage(ChatColor.GREEN + targetPlayer.getName() + " davet edildi. Siz " + ChatColor.AQUA + "BEYAZ" + ChatColor.GREEN + " oynayacaksiniz.");
            targetPlayer.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW
                    + " sizi bir satranc partisine davet etti! Kabul etmek icin: " + ChatColor.WHITE + "/chess accept");
        }

        gridRenderer.ensureGrid(interaction, game);
        boardEntity.saveState(interaction, game);
    }

    private void handleAccept(Player player) {
        if (!gameManager.hasInvite(player)) {
            player.sendMessage(ChatColor.RED + "Bekleyen bir davetiniz yok.");
            return;
        }
        UUID entityId = gameManager.consumeInvite(player);
        BoardGame game = entityId != null ? gameManager.getGame(entityId) : null;
        if (game == null || game.getStatus() != GameStatus.WAITING_FOR_OPPONENT) {
            player.sendMessage(ChatColor.RED + "Bu davet artik gecerli degil.");
            return;
        }

        boolean selfInvite = game.getWhiteId().equals(player.getUniqueId());
        if (!selfInvite && gameManager.getActiveOrWaitingGameFor(player) != null) {
            player.sendMessage(ChatColor.RED + "Zaten baska bir oyunda/beklemedesiniz.");
            return;
        }

        game.joinAsBlack(player);
        player.sendMessage(ChatColor.GREEN + "Oyuna katildiniz! Siz " + ChatColor.RED + "SIYAH" + ChatColor.GREEN + " oynuyorsunuz.");
        game.broadcast(ChatColor.GREEN + "Oyun basladi! Tahtadaki karelere tiklayarak oynayabilirsiniz. Sira: Beyaz.");

        Entity entity = Bukkit.getEntity(entityId);
        if (entity instanceof Interaction) {
            Interaction interaction = (Interaction) entity;
            gridRenderer.ensureGrid(interaction, game);
            boardEntity.saveState(interaction, game);
        }
    }

    private void handleDecline(Player player) {
        if (!gameManager.hasInvite(player)) {
            player.sendMessage(ChatColor.RED + "Bekleyen bir davetiniz yok.");
            return;
        }
        gameManager.declineInvite(player);
        player.sendMessage(ChatColor.YELLOW + "Davet reddedildi.");
    }

    private void handleResign(Player player) {
        BoardGame game = gameManager.getActiveOrWaitingGameFor(player);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Aktif veya bekleyen bir oyununuz yok.");
            return;
        }
        if (game.getStatus() == GameStatus.WAITING_FOR_OPPONENT) {
            gameManager.removeGame(game.getEntityId());
            player.sendMessage(ChatColor.YELLOW + "Bekleyen oyun/davet iptal edildi. Tahta bos kaldi (yeniden davet edilebilir).");
            return;
        }
        game.resign(player);
        Entity entity = Bukkit.getEntity(game.getEntityId());
        if (entity instanceof Interaction) {
            Interaction interaction = (Interaction) entity;
            gridRenderer.refresh(interaction, game);
            boardEntity.saveState(interaction, game);
        }
    }

    private void handleRematch(Player player) {
        Interaction interaction = findTargetedBoard(player);
        if (interaction == null) {
            player.sendMessage(ChatColor.RED + "Bir Satranc Masasina bakmiyorsunuz.");
            return;
        }
        BoardGame game = gameManager.getOrLoadGame(interaction, boardEntity);
        if (game == null) {
            player.sendMessage(ChatColor.RED + "Bu tahtada bir oyun bulunamadi.");
            return;
        }
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
            gridRenderer.refresh(interaction, game);
            boardEntity.saveState(interaction, game);
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
        gameManager.invite(opponent, interaction.getUniqueId());
        gridRenderer.refresh(interaction, game);
        boardEntity.saveState(interaction, game);

        player.sendMessage(ChatColor.GREEN + "Rovans teklif edildi: " + opponent.getName());
        opponent.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW
                + " sizinle rovans yapmak istiyor! Kabul icin: " + ChatColor.WHITE + "/chess accept");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "== ChessPlugin ==");
        player.sendMessage(ChatColor.GRAY + "1) " + ChatColor.WHITE + "1 Kuvars Blogu + 1 Kayrak Tasi" + ChatColor.GRAY + " -> " + ChatColor.WHITE + "Taslar" + ChatColor.GRAY + " kesesi");
        player.sendMessage(ChatColor.GRAY + "2) Tarifle " + ChatColor.WHITE + "Satranc Masasi" + ChatColor.GRAY + " esyasini craftleyip bir bloğa sag tiklayarak yerlestirin");
        player.sendMessage(ChatColor.GRAY + "3) Tahtaya " + ChatColor.WHITE + "Taslar" + ChatColor.GRAY + " kesesiyle sag tiklayin");
        player.sendMessage(ChatColor.YELLOW + "/chess invite <oyuncu>" + ChatColor.GRAY + " - Rakip davet eder. Kendi adinizi yazarsaniz kendi kendinize oynarsiniz.");
        player.sendMessage(ChatColor.YELLOW + "/chess accept" + ChatColor.GRAY + " - Daveti kabul edip Siyah olarak katilir");
        player.sendMessage(ChatColor.YELLOW + "/chess decline" + ChatColor.GRAY + " - Daveti reddeder");
        player.sendMessage(ChatColor.YELLOW + "/chess resign" + ChatColor.GRAY + " - Oyunu terk eder / bekleyen daveti iptal eder");
        player.sendMessage(ChatColor.YELLOW + "/chess rematch" + ChatColor.GRAY + " - Bitmis bir oyunda rovans teklif eder (tahtadaki butona da tiklayabilirsiniz)");
        player.sendMessage(ChatColor.GRAY + "Oynamak icin: tahtanin ustundeki karelere dogrudan sag tiklayin.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("invite", "accept", "decline", "resign", "rematch", "help");
            List<String> result = new ArrayList<>();
            for (String o : options) {
                if (o.startsWith(args[0].toLowerCase())) result.add(o);
            }
            return result;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            List<String> result = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(p.getName());
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
}
