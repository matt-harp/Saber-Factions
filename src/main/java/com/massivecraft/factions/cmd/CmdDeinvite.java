package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.TL;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;

public class CmdDeinvite extends FCommand {

    /**
     * @author FactionsUUID Team
     */

    public CmdDeinvite() {
        super();
        this.aliases.addAll(Aliases.deinvite);

        this.optionalArgs.put("player name", "name");

        this.requirements = new CommandRequirements.Builder(Permission.DEINVITE)
                .withAction(PermissableAction.INVITE)
                .memberOnly()
                .build();
    }

    @Override
    public void perform(CommandContext context) {
        FPlayer you = context.argAsBestFPlayerMatch(0);
        if (you == null) {
            FancyMessage msg = new FancyMessage(TL.COMMAND_DEINVITE_CANDEINVITE.toString()).color(ChatColor.GOLD);
            for (String id : context.faction.getInvites()) {
                FPlayer fp = FPlayers.getInstance().getById(id);
                String name = fp != null ? fp.getName() : id;
                msg.then(name + " ").color(ChatColor.WHITE).tooltip(TL.COMMAND_DEINVITE_CLICKTODEINVITE.format(name)).command("/" + Conf.baseCommandAliases.get(0) + " deinvite " + name);
            }
            context.sendFancyMessage(msg);
            return;
        }

        if (you.getFaction() == context.faction) {
            context.msg(TL.COMMAND_DEINVITE_ALREADYMEMBER, you.getName(), context.faction.getTag());
            context.msg(TL.COMMAND_DEINVITE_MIGHTWANT, FCmdRoot.instance.cmdKick.getUsageTemplate(context));
            return;
        }

        context.faction.deinvite(you);

        you.msg(TL.COMMAND_DEINVITE_REVOKED, context.fPlayer.describeTo(you), context.faction.describeTo(you));

        context.faction.msg(TL.COMMAND_DEINVITE_REVOKES, context.fPlayer.describeTo(context.faction), you.describeTo(context.faction));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_DEINVITE_DESCRIPTION;
    }

}

