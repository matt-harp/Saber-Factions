package com.massivecraft.factions.discord;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.FactionsPlugin;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.HierarchyException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.logging.Level;

/**
 * @author SaberTeam
 */

public class Discord {
    //We dont want waitingLink to reset during reload so we are going to set it here
    public static HashMap<Integer, FPlayer> waitingLink;
    public static HashMap<FPlayer, Integer> waitingLinkk;
    //We want to track the amount of times setup has been tried and the result may be useful for determining issues
    public static HashSet<DiscordSetupAttempt> setupLog;
    public static Boolean confUseDiscord;
    public static String botToken;
    public static String mainGuildID;
    public static Boolean useDiscord;
    public static java.awt.Color roleColor;
    public static Guild mainGuild;
    public static Role leader;
    public static JDA jda;
    public static Boolean useEmotes;
    public static Emote positive;
    public static Emote negative;
    private static FactionsPlugin plugin;

    public Discord(FactionsPlugin plugin) {
        Discord.plugin = plugin;
        setupLog = new HashSet<>();
        waitingLink = new HashMap<>();
        waitingLinkk = new HashMap<>();
        setupDiscord();
    }

    /**
     * Called to reload variables and if needed start JDA
     */
    public static void setupDiscord() {
        if (jda == null) {
            if (startBot()) {
                varSetup();
                jda.addEventListener(new FactionChatHandler(plugin));
                jda.addEventListener(new DiscordListener(plugin));
                return;
            }
        }
        varSetup();
    }

    private static Boolean startBot() {
        if (!Conf.useDiscordSystem) {
            return false;
        }
        try {
            jda = new JDABuilder(AccountType.BOT).setToken(Conf.discordBotToken).buildBlocking();
        } catch (LoginException | InterruptedException e) {
            FactionsPlugin.getInstance().getLogger().log(Level.WARNING, "Discord bot was unable to start! Please verify the bot token is correct.");
            setupLog.add(new DiscordSetupAttempt(e.getMessage(), System.currentTimeMillis()));
            return false;
        }
        setupLog.add(new DiscordSetupAttempt(System.currentTimeMillis()));
        return true;
    }

    private static void varSetup() {
        try {
            confUseDiscord = Conf.useDiscordSystem;
            botToken = Conf.discordBotToken;
            if (jda != null && Conf.leaderRoles || Conf.factionDiscordTags) {
                mainGuild = jda.getGuildById(Conf.mainGuildID);
            } else {
                mainGuild = null;
            }
            mainGuildID = Conf.mainGuildID;
            useDiscord = !botToken.equals("<token here>") && !mainGuildID.equals("<Discord Server ID here>") && confUseDiscord;
            roleColor = new java.awt.Color(Conf.factionRoleColor.get(0), Conf.factionRoleColor.get(1), Conf.factionRoleColor.get(2));
            if (jda != null) {
                try {
                    positive = jda.getEmoteById(Conf.positiveReaction);
                    negative = jda.getEmoteById(Conf.negativeReaction);
                    if (positive == null | negative == null) {
                        useEmotes = false;
                    }
                } catch (NumberFormatException e) {
                    FactionsPlugin.getInstance().getLogger().log(Level.WARNING, "Invalid Emote(s) disabling them.");
                    useEmotes = false;
                }
                if (mainGuild != null) {
                    leader = mainGuild.getRoleById(Conf.leaderRole);
                } else {
                    leader = null;
                }
            } else {
                useEmotes = false;
                leader = null;
            }
        } catch (NullPointerException e) {
            setupLog.add(new DiscordSetupAttempt("Threw an NPE while setting up variables", System.currentTimeMillis()));
        }
    }

    /**
     * Get the nickname that would be assigned to a player
     *
     * @param f Target as FPlayer
     * @return Translated nickname for Discord as a String
     */
    public static String getNicknameString(FPlayer f) {
        if (useDiscord) {
            String temp = Conf.factionTag;
            if (temp.contains("NAME")) {
                temp = temp.replace("NAME", f.getName());
            }
            if (temp.contains("DiscordName")) {
                temp = temp.replace("DiscordName", (f.discordUser() == null) ? (f.getName()) : (f.discordUser().getName()));
            }
            if (temp.contains("FACTION")) {
                temp = temp.replace("FACTION", f.getFaction().getTag());
            }
            if (temp.contains("FactionRole")) {
                temp = temp.replace("FactionRole", f.getRole().getRoleCapitalized());
            }
            if (temp.contains("FactionRolePrefix")) {
                temp = temp.replace("FactionRolePrefix", f.getRole().getPrefix());
            }
            return temp;
        }
        return null;
    }

    /**
     * Check if a faction Role exist
     *
     * @param s String target Faction tag
     * @return
     */
    public static Boolean doesFactionRoleExist(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append(Conf.factionRolePrefix);
        sb.append(s);
        sb.append(Conf.factionRoleSuffix);
        return getRoleFromName(sb.toString()) != null;
    }

    public static Role getRoleFromName(String s) {
        if (useDiscord && mainGuild != null) {
            for (Role r : mainGuild.getRoles()) {
                if (r.getName().equals(s)) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * Creates a role in Discord for a faction and returns it
     *
     * @param s String Faction Tag
     * @return Role generated faction role
     */
    public static Role createFactionRole(String s) {
        if (!useDiscord) {
            return null;
        }
        if (mainGuild == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Conf.factionRolePrefix);
        sb.append(s);
        sb.append(Conf.factionRoleSuffix);
        if (!doesFactionRoleExist(sb.toString())) {
            try {
                Role newRole = mainGuild.getController().createRole()
                        .setName(sb.toString())
                        .setColor(roleColor)
                        .setPermissions(Permission.EMPTY_PERMISSIONS)
                        .complete(true);
                return newRole;
            } catch (RateLimitedException e) {
                System.out.print(e.getMessage());
            }
        } else {
            return getRoleFromName(sb.toString());
        }
        return null;
    }

    /**
     * Get the name of the Faction Role that would be generated with the tag
     *
     * @param tag Faction Name/Tag
     * @return Name of would be Role
     */
    public static String getFactionRoleName(String tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(Conf.factionRolePrefix);
        sb.append(tag);
        sb.append(Conf.factionRoleSuffix);
        return sb.toString();
    }

    /**
     * Check if the Discord user is in the main Guild/Server
     *
     * @param u User
     * @return Boolean
     */
    public static Boolean isInMainGuild(User u) {
        if (mainGuild == null) return false;
        return mainGuild.getMember(u) == null ? Boolean.FALSE : Boolean.TRUE;
    }

    /**
     * Reset the players nickname in Discord
     *
     * @param f FPlayer target
     */
    public static void resetNick(FPlayer f) {
        if (mainGuild == null) {
            return;
        }
        if (mainGuild.getMember(f.discordUser()) == null) {
            return;
        }
        mainGuild.getController().setNickname(mainGuild.getMember(f.discordUser()), f.discordUser().getName()).queue();
    }

    public static void changeFactionTag(Faction f, String oldTag) {
        if (!useDiscord | mainGuild == null) {
            return;
        }
        for (FPlayer fp : f.getFPlayers()) {
            if (fp.discordSetup() && isInMainGuild(fp.discordUser())) {
                try {
                    Member m = mainGuild.getMember(fp.discordUser());
                    if (Conf.factionDiscordTags) {
                        mainGuild.getController().setNickname(m, Discord.getNicknameString(fp)).queue();
                    }
                    if (Conf.factionRoles) {
                        mainGuild.getController().removeSingleRoleFromMember(m, Objects.requireNonNull(getRoleFromName(oldTag))).queue();
                        mainGuild.getController().addSingleRoleToMember(m, Objects.requireNonNull(createFactionRole(f.getTag()))).queue();
                    }
                } catch (HierarchyException e) {
                    System.out.print(e.getMessage());
                }
            }
        }

    }
}
