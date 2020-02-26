package com.massivecraft.factions.util;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.tag.FactionTag;
import com.massivecraft.factions.tag.Tag;
import com.massivecraft.factions.zcore.util.TL;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ClipPlaceholderAPIManager extends PlaceholderExpansion implements Relational {

    // Identifier for this expansion
    @Override
    public String getIdentifier() {
        return "factionsuuid";
    }

    @Override
    public String getAuthor() {
        return "drtshock";
    }

    // Since we are registering this expansion from the dependency, this can be null
    @Override
    public String getPlugin() {
        return null;
    }

    // Return the plugin version since this expansion is bundled with the dependency
    @Override
    public String getVersion() {
        return FactionsPlugin.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    // Relational placeholders
    @Override
    public String onPlaceholderRequest(Player p1, Player p2, String placeholder) {
        if (p1 == null || p2 == null || placeholder == null) {
            return "";
        }

        FPlayer fp1 = FPlayers.getInstance().getByPlayer(p1);
        FPlayer fp2 = FPlayers.getInstance().getByPlayer(p2);
        if (fp1 == null || fp2 == null) {
            return "";
        }

        switch (placeholder) {
            case "relation":
                String relationName = fp1.getRelationTo(fp2).nicename;
                return relationName != null ? relationName : "";
            case "relation_color":
                ChatColor color = fp1.getColorTo(fp2);
                return color != null ? color.toString() : "";
        }

        return null;
    }

    @Override
    public String onPlaceholderRequest(Player player, String placeholder) {
        if (player == null || placeholder == null) {
            return "";
        }

        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        if (placeholder.contains("faction_territory")) {
            faction = Board.getInstance().getFactionAt(fPlayer.getLastStoodAt());
            placeholder = placeholder.replace("_territory", "");
        }
        switch (placeholder) {
            // First list player stuff
            case "player_name":
                return fPlayer.getName();
            case "player_lastseen":
                String humanized = DurationFormatUtils.formatDurationWords(System.currentTimeMillis() - fPlayer.getLastLoginTime(), true, true) + TL.COMMAND_STATUS_AGOSUFFIX;
                return fPlayer.isOnline() ? ChatColor.GREEN + TL.COMMAND_STATUS_ONLINE.toString() : (System.currentTimeMillis() - fPlayer.getLastLoginTime() < 432000000 ? ChatColor.YELLOW + humanized : ChatColor.RED + humanized);
            case "player_group":
                return FactionsPlugin.getInstance().getPrimaryGroup(Bukkit.getOfflinePlayer(UUID.fromString(fPlayer.getId())));
            case "player_balance":
                return Econ.isSetup() ? Econ.getFriendlyBalance(fPlayer) : TL.ECON_OFF.format("balance");
            case "player_power":
                return String.valueOf(fPlayer.getPowerRounded());
            case "player_maxpower":
                return String.valueOf(fPlayer.getPowerMaxRounded());
            case "player_kills":
                return String.valueOf(fPlayer.getKills());
            case "player_deaths":
                return String.valueOf(fPlayer.getDeaths());
            case "player_role_prefix":
                return String.valueOf(fPlayer.getRolePrefix());
            case "player_role":
                return fPlayer.hasFaction() ? fPlayer.getRole().getPrefix() : "";
            case "player_role_name":
                return fPlayer.hasFaction() ? fPlayer.getRole().getTranslation().toString() : TL.PLACEHOLDER_ROLE_NAME.toString();
            // Then Faction stuff
            case "faction_name":
                return fPlayer.hasFaction() ? faction.getTag() : TL.NOFACTION_PREFIX.toString();
            case "faction_name_blanknofaction":
                return fPlayer.hasFaction() ? faction.getTag() : "";
            case "faction_name_custom":
                return fPlayer.hasFaction() ? Tag.parsePlain(fPlayer, TL.PLACEHOLDER_CUSTOM_FACTION.toString()) : "";
            case "faction_only_space":
                return fPlayer.hasFaction() ? " " : "";
            case "faction_power":
                return String.valueOf(faction.getPowerRounded());
            case "faction_powermax":
                return String.valueOf(faction.getPowerMaxRounded());
            case "faction_description":
                return faction.getDescription();
            case "faction_claims":
            return fPlayer.hasFaction() ? String.valueOf(faction.getAllClaims().size()) : "0";
            case "faction_maxclaims":
                return String.valueOf(Conf.claimedLandsMax);
            case "faction_founded":
                return TL.sdf.format(faction.getFoundedDate());
            case "faction_joining":
                return (faction.getOpen() ? TL.COMMAND_SHOW_UNINVITED.toString() : TL.COMMAND_SHOW_INVITATION.toString());
            case "faction_strikes":
                return fPlayer.hasFaction() ? String.valueOf(faction.getStrikes()) : "0";
            case "faction_peaceful":
                return faction.isPeaceful() ? Conf.colorNeutral + TL.COMMAND_SHOW_PEACEFUL.toString() : "";
            case "faction_tntbank_balance":
                return String.valueOf(faction.getTnt());
            case "faction_tnt_max_balance":
                return FactionTag.TNT_MAX.replace(FactionTag.TNT_MAX.getTag(), faction);
            case "faction_points":
                return fPlayer.hasFaction() ? String.valueOf(faction.getPoints()) : "0";
            case "discord_acount":
                return fPlayer.discordSetup() ? String.valueOf(fPlayer.discordUserID()) : "Not Linked";
            case "faction_discord":
                return fPlayer.hasFaction() ? String.valueOf(faction.getDiscord()) : "Not Linked";
            case "faction_powerboost":
                double powerBoost = faction.getPowerBoost();
                return (powerBoost == 0.0) ? "" : (powerBoost > 0.0 ? TL.COMMAND_SHOW_BONUS.toString() : TL.COMMAND_SHOW_PENALTY.toString()) + powerBoost + ")";
            case "faction_leader":
                FPlayer fAdmin = faction.getFPlayerAdmin();
                return fAdmin == null ? TL.GENERIC_SERVER.toString() : fAdmin.getName().substring(0, fAdmin.getName().length() > 14 ? 13 : fAdmin.getName().length());
            case "faction_warps":
                return String.valueOf(faction.getWarps().size());
            case "faction_raidable":
                boolean raid = FactionsPlugin.getInstance().getConfig().getBoolean("hcf.raidable", false) && faction.getLandRounded() >= faction.getPowerRounded();
                return raid ? TL.RAIDABLE_TRUE.toString() : TL.RAIDABLE_FALSE.toString();
            case "faction_home_world":
                return faction.hasHome() ? faction.getHome().getWorld().getName() : "";
            case "faction_home_x":
                return faction.hasHome() ? String.valueOf(faction.getHome().getBlockX()) : "";
            case "faction_home_y":
                return faction.hasHome() ? String.valueOf(faction.getHome().getBlockY()) : "";
            case "faction_home_z":
                return faction.hasHome() ? String.valueOf(faction.getHome().getBlockZ()) : "";
            case "faction_land_value":
                return Econ.shouldBeUsed() ? Econ.moneyString(Econ.calculateTotalLandValue(faction.getLandRounded())) : TL.ECON_OFF.format("value");
            case "faction_land_refund":
                return Econ.shouldBeUsed() ? Econ.moneyString(Econ.calculateTotalLandRefund(faction.getLandRounded())) : TL.ECON_OFF.format("refund");
            case "faction_bank_balance":
                return Econ.shouldBeUsed() ? Econ.moneyString(Econ.getBalance(faction.getAccountId())) : TL.ECON_OFF.format("balance");
            case "faction_allies":
                return String.valueOf(faction.getRelationCount(Relation.ALLY));
            case "faction_enemies":
                return String.valueOf(faction.getRelationCount(Relation.ENEMY));
            case "faction_truces":
                return String.valueOf(faction.getRelationCount(Relation.TRUCE));
            case "faction_online":
                return String.valueOf(faction.getOnlinePlayers().size());
            case "faction_offline":
                return String.valueOf(faction.getFPlayers().size() - faction.getOnlinePlayers().size());
            case "faction_size":
                return String.valueOf(faction.getFPlayers().size());
            case "faction_announcement":
                return String.valueOf(faction.getAnnouncements());
            case "faction_kills":
                return String.valueOf(faction.getKills());
            case "faction_deaths":
                return String.valueOf(faction.getDeaths());
            case "faction_maxvaults":
                return String.valueOf(faction.getMaxVaults());
            case "faction_grace":
                return String.valueOf(Conf.gracePeriod);
            case "faction_name_at_location":
                Faction factionAtLocation = Board.getInstance().getFactionAt(new FLocation(player.getLocation()));
                return factionAtLocation != null ? factionAtLocation.getTag() : Factions.getInstance().getWilderness().getTag();
        }

        return null;
    }
}