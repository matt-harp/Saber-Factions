package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.cmd.audit.FLogType;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.CC;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import mkremins.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

public class CmdKick extends FCommand {

    /**
     * @author FactionsUUID Team
     */

    public CmdKick() {
        super();
        this.aliases.addAll(Aliases.kick);
        this.optionalArgs.put("player name", "player name");

        this.requirements = new CommandRequirements.Builder(Permission.KICK)
                .playerOnly()
                .withAction(PermissableAction.KICK)
                .memberOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        FPlayer toKick = context.argIsSet(0) ? context.argAsBestFPlayerMatch(0) : null;
        if (toKick == null) {
            FancyMessage msg = new FancyMessage(TL.COMMAND_KICK_CANDIDATES.toString()).color(ChatColor.GOLD);
            for (FPlayer player : context.faction.getFPlayersWhereRole(Role.NORMAL)) {
                String s = player.getName();
                msg.then(s + " ").color(ChatColor.WHITE).tooltip(TL.COMMAND_KICK_CLICKTOKICK.toString() + s).command("/" + Conf.baseCommandAliases.get(0) + " kick " + s);
            }
            if (context.fPlayer.getRole().isAtLeast(Role.COLEADER)) {
                // For both coleader and admin, add mods.
                for (FPlayer player : context.faction.getFPlayersWhereRole(Role.MODERATOR)) {
                    String s = player.getName();
                    msg.then(s + " ").color(ChatColor.GRAY).tooltip(TL.COMMAND_KICK_CLICKTOKICK.toString() + s).command("/" + Conf.baseCommandAliases.get(0) + " kick " + s);
                }
                if (context.fPlayer.getRole() == Role.LEADER) {
                    // Only add coleader to this for the leader.
                    for (FPlayer player : context.faction.getFPlayersWhereRole(Role.COLEADER)) {
                        String s = player.getName();
                        msg.then(s + " ").color(ChatColor.RED).tooltip(TL.COMMAND_KICK_CLICKTOKICK.toString() + s).command("/" + Conf.baseCommandAliases.get(0) + " kick " + s);
                    }
                }
            }

            context.sendFancyMessage(msg);
            return;
        }

        if (context.fPlayer == toKick) {
            context.msg(TL.COMMAND_KICK_SELF);
            context.msg(TL.GENERIC_YOUMAYWANT.toString() + FactionsPlugin.getInstance().cmdBase.cmdLeave.getUsageTemplate(context));
            return;
        }

        Faction toKickFaction = toKick.getFaction();

        if (toKickFaction.isWilderness()) {
            context.sender.sendMessage(TL.COMMAND_KICK_NONE.toString());
            return;
        }

        // This permission check has been cleaned to be more understandable and logical
        // Unless is admin,
        // - Check for the kick permission.
        // - Make sure the player is in the faction.
        // - Make sure the kicked player has lower rank than the kicker.
        if (!context.fPlayer.isAdminBypassing()) {
            if (toKickFaction != context.faction) {
                context.msg(TL.COMMAND_KICK_NOTMEMBER, toKick.describeTo(context.fPlayer, true), context.faction.describeTo(context.fPlayer));
                return;
            }
            if (toKick.getRole().value >= context.fPlayer.getRole().value) {
                context.msg(TL.COMMAND_KICK_INSUFFICIENTRANK);
                return;
            }
            if (!Conf.canLeaveWithNegativePower && toKick.getPower() < 0) {
                context.msg(TL.COMMAND_KICK_NEGATIVEPOWER);
                return;
            }
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make sure they can pay
        if (!context.canAffordCommand(Conf.econCostKick, TL.COMMAND_KICK_TOKICK.toString())) {
            return;
        }

        // trigger the leave event (cancellable) [reason:kicked]
        FPlayerLeaveEvent event = new FPlayerLeaveEvent(toKick, toKick.getFaction(), FPlayerLeaveEvent.PlayerLeaveReason.KICKED);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        // then make 'em pay (if applicable)
        if (!context.payForCommand(Conf.econCostKick, TL.COMMAND_KICK_TOKICK.toString(), TL.COMMAND_KICK_FORKICK.toString())) {
            return;
        }

        toKickFaction.msg(TL.COMMAND_KICK_FACTION, context.fPlayer.describeTo(toKickFaction, true), toKick.describeTo(toKickFaction, true));

        toKick.msg(TL.COMMAND_KICK_KICKED, context.fPlayer.describeTo(toKick, true), toKickFaction.describeTo(toKick));

        if (toKickFaction != context.faction) {
            context.fPlayer.msg(TL.COMMAND_KICK_KICKS, toKick.describeTo(context.fPlayer), toKickFaction.describeTo(context.fPlayer));
        }
        if (Conf.logFactionKick) {
            FactionsPlugin.getInstance().log((context.sender instanceof ConsoleCommandSender ? "A console command" : context.fPlayer.getName()) + " kicked " + toKick.getName() + " from the faction: " + toKickFaction.getTag());
        }
        if (toKick.getRole() == Role.LEADER) {
            toKickFaction.promoteNewLeader();
        }
        FactionsPlugin.instance.logFactionEvent(toKickFaction, FLogType.INVITES, context.fPlayer.getName(), CC.Red + "kicked", toKick.getName());
        toKickFaction.deinvite(toKick);
        toKick.resetFactionData();
        if (!CmdFly.checkBypassPerms(toKick, toKick.getPlayer(), toKickFaction, false)) {
            CmdFly.disableFlight(toKick);
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_KICK_DESCRIPTION;
    }

}