package io.github.thelordman.costrength.listeners;

import io.github.thelordman.costrength.discord.Discord;
import io.github.thelordman.costrength.economy.EconomyManager;
import io.github.thelordman.costrength.economy.LevelHandler;
import io.github.thelordman.costrength.items.ItemManager;
import io.github.thelordman.costrength.scoreboard.ScoreboardHandler;
import io.github.thelordman.costrength.utilities.data.Data;
import io.github.thelordman.costrength.utilities.Methods;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlayerDeathListener implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        Player killer = event.getPlayer().getKiller();
        String originalMsg = event.getDeathMessage();

        if (killer != null && killer != victim) {
            Data.combatTag.remove(killer);
            Data.combatTag.remove(victim);

            killRewards(killer, victim);

            if (Methods.hasDonatorPermission(killer.getUniqueId(), 1)) event.setDeathMessage(Methods.cStr("&cDeath &8| &f" + event.getDeathMessage()));
            else event.setDeathMessage(Methods.cStr("&7Death &8| &7" + event.getDeathMessage()));

            if (EconomyManager.getBounty(victim.getUniqueId()) != 0d) {
                EconomyManager.setBalance(killer.getUniqueId(), EconomyManager.getBalance(killer.getUniqueId()) + EconomyManager.getBounty(victim.getUniqueId()));
                Bukkit.broadcastMessage(Methods.cStr(killer.getDisplayName() + " &6collected the bounty of &f$" + Methods.rStr(EconomyManager.getBounty(victim.getUniqueId())) + " &6on " + victim.getDisplayName() + "&6."));
                EconomyManager.setBounty(victim.getUniqueId(), 0d);
                Methods.updateDisplayName(victim);
            }

            ScoreboardHandler.updateBoard(killer);
        }
        else {
            victim.sendTitle(ChatColor.RED + "You Died", "");
            event.setDeathMessage(Methods.cStr("&7Death &8| &7" + event.getDeathMessage()));
        }

        EconomyManager.setKillstreak(victim.getUniqueId(), 0);
        ScoreboardHandler.updateBoard(victim);
        LevelHandler.xp(victim);

        Data.lastHitData.remove(victim);
        Data.lastHitData.forEach((p, o) -> {
            if (o.getValue0() == victim) Data.lastHitData.remove(p);
        });

        killDrops(victim);

        //Discord
        EmbedBuilder builder = new EmbedBuilder();
        builder.setAuthor(Methods.replaceColorCodes(event.getPlayer().getDisplayName() + " Died", '??'), null, "https://crafatar.com/avatars/" + event.getPlayer().getUniqueId());
        builder.setColor(Color.RED);
        if (originalMsg != null) {
            builder.setDescription(Methods.replaceColorCodes(originalMsg, '&'));
        }
        else {
            builder.setDescription("They just died");
        }

        Discord.minecraftChatChannel.sendMessageEmbeds(builder.build()).queue();
    }

    private void killRewards(Player killer, Player victim) {
        //Victim
        int killerKillstreak = EconomyManager.getKillstreak(killer.getUniqueId());
        double victimBalance = EconomyManager.getBalance(victim.getUniqueId());
        double takeFromVictim = ItemManager.getCELevel(killer.getInventory().getItemInMainHand(), ItemManager.swordEnchantments[1]) == 1 && EconomyManager.getLevel(victim.getUniqueId()) > EconomyManager.getLevel(killer.getUniqueId())
                ? victimBalance == 0d ? 0d : victimBalance / 5d : victimBalance == 0d ? 0d : victimBalance / 100d;

        EconomyManager.setBalance(victim.getUniqueId(), victimBalance - takeFromVictim);

        victim.sendTitle(ChatColor.RED + "You Died", ChatColor.GOLD + killer.getDisplayName() + " stole " + ChatColor.WHITE + "$" + Methods.rStr(takeFromVictim));

        //Killer Rewards
        double reward = 1000d + killerKillstreak * 100 + EconomyManager.getKillstreak(victim.getUniqueId()) * 100;

        double levelBonus = Math.max((EconomyManager.getLevel(victim.getUniqueId()) - EconomyManager.getLevel(killer.getUniqueId())) / 10d, -0.75);
        double kdrBonus = levelBonus > 0 ? 1 + Methods.getKdr(killer) + Methods.getKdr(victim) : Methods.getKdr(killer) + Methods.getKdr(victim);
        double killBonus = victim.getStatistic(Statistic.PLAYER_KILLS) > killer.getStatistic(Statistic.PLAYER_KILLS)
                ? ((double) victim.getStatistic(Statistic.PLAYER_KILLS)) / ((double) killer.getStatistic(Statistic.PLAYER_KILLS)) / 10d : 0d;

        double multi = 1 + levelBonus + kdrBonus + killBonus;

        double money = reward * multi + takeFromVictim, xp = reward * multi;
        if (Double.isNaN(money) | Double.isInfinite(money)) money = 100d;

        killer.sendMessage(Methods.cStr("&6You killed " + victim.getDisplayName() + " &6."));
        killer.sendActionBar(Methods.cStr("&f+$" + Methods.rStr(money) + " &7(" + Methods.rStr(multi) + "x) &8| &f+" + Methods.rStr(xp) + "xp &7(" + Methods.rStr(multi) + "x) &8| &6Streak&7: &f" + Methods.rStr((double) killerKillstreak)));
        EconomyManager.setBalance(killer.getUniqueId(), EconomyManager.getBalance(killer.getUniqueId()) + money);
        EconomyManager.setXp(killer.getUniqueId(), EconomyManager.getXp(killer.getUniqueId()) + xp);
        EconomyManager.setKillstreak(killer.getUniqueId(), killerKillstreak + 1);
        LevelHandler.xp(killer);
    }

    private void killDrops(Player victim) {
        ItemStack[][] items = {victim.getInventory().getStorageContents(), victim.getInventory().getArmorContents()};

        for (ItemStack[] itemStacks : items) {
            for (ItemStack item : itemStacks) {
                if (item == null) continue;
                if (item.getType().isEmpty() | item.getType().name().contains("PICKAXE")) continue;
                Objects.requireNonNull(Bukkit.getWorld("world")).dropItemNaturally(victim.getLocation(), item);
                victim.getInventory().remove(item);
            }
        }
    }
}