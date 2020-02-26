package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.FactionsPlugin;
import com.massivecraft.factions.integration.Econ;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class CmdTop extends FCommand {

    /**
     * @author FactionsUUID Team
     * Edited by NightyNight
     */

    public CmdTop() {
        super();
        this.aliases.addAll(Aliases.top);

        this.requirements = new CommandRequirements.Builder(Permission.TOP)
                .build();
    }


    @Override
    public void perform(CommandContext context)
    {

    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_TOP_DESCRIPTION;
    }
}
