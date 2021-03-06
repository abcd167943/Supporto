package com.andre601.suggesto.commands.tickets;

import com.andre601.suggesto.utils.Database;
import com.andre601.suggesto.utils.EmbedUtil;
import com.andre601.suggesto.utils.LogUtil;
import com.github.rainestormee.jdacommand.Command;
import com.github.rainestormee.jdacommand.CommandAttribute;
import com.github.rainestormee.jdacommand.CommandDescription;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.awt.*;
import java.text.MessageFormat;

@CommandDescription(
        name = "Remove",
        description =
                "Removes a user or role from a ticket.\n" +
                "Can only be run in a ticket!\n" +
                "\n" +
                "`member <memberID>` Removes a member from the ticket.\n" +
                "`role <roleID>` Removes a role from the ticket",
        triggers = {"remove"},
        attributes = {
                @CommandAttribute(key = "staff+"),
                @CommandAttribute(key = "tickets")
        }
)

public class CmdRemove implements Command {

    private Member getMember(Guild guild, String id){
        Member member;
        try{
            member = guild.getMemberById(id);
        }catch(Exception ex){
            member = null;
        }

        return member;
    }

    private Role getRole(Guild guild, String id){
        Role role;
        try{
            role = guild.getRoleById(id);
        }catch(Exception ex){
            role = null;
        }

        return role;
    }

    @Override
    public void execute(Message msg, String s) {
        Guild guild = msg.getGuild();
        TextChannel tc = msg.getTextChannel();

        if(!Database.hasTicket(tc.getId())){
            EmbedUtil.sendError(msg, "Please run this command inside a ticket!");
            return;
        }

        String[] args = s.split(" ");
        if(args.length <= 1){
            EmbedUtil.sendError(msg, MessageFormat.format(
                    "You need to provide more arguments!\n" +
                    "Usage: `{0}remove <member <ID>|role <ID>>`",
                    Database.getPrefix(guild)
            ));
            return;
        }

        switch(args[0].toLowerCase()){
            case "member":
                Member member = getMember(guild, args[1]);
                if(member == null){
                    EmbedUtil.sendError(msg,
                            "The provided ID was invalid!\n" +
                            "Make sure you have the right ID."
                    );
                    return;
                }
                if(tc.getPermissionOverride(member) == null){
                    EmbedUtil.sendError(msg, MessageFormat.format(
                            "The member {0} is not added to this ticket!",
                            member.getAsMention()
                    ));
                    return;
                }
                if(member == tc.getGuild().getSelfMember()){
                    EmbedUtil.sendError(msg, MessageFormat.format(
                            "You aren't allowed to remove me from the ticket!",
                            member.getAsMention()
                    ));
                    return;
                }

                tc.getPermissionOverride(member).delete().reason(MessageFormat.format(
                    "Removed member {0}",
                    member.getEffectiveName()
            )).queue();

                EmbedBuilder memberSuccess = EmbedUtil.getEmbed(msg.getAuthor())
                        .setColor(Color.GREEN)
                        .setDescription(MessageFormat.format(
                                "Successfully removed the member {0}!",
                                member.getAsMention()
                        ));
                tc.sendMessage(memberSuccess.build()).queue();

                LogUtil.memberRemove(tc, guild.getMember(msg.getAuthor()), member);
                break;

            case "role":
                Role role = getRole(guild, args[1]);
                if(role == null){
                    EmbedUtil.sendError(msg, MessageFormat.format(
                            "The provided roleID was invalid!\n" +
                            "Make sure, that the provided id is correct.\n" +
                            "You can use `{0}roles` to get a list of roles and their ids.",
                            Database.getPrefix(guild)
                    ));
                    return;
                }
                if(tc.getPermissionOverride(role) == null){
                    EmbedUtil.sendError(msg, "This role is not added to this ticket!");
                    return;
                }

                try {
                    if (role.isPublicRole()) {
                        tc.putPermissionOverride(role).setDeny(Permission.VIEW_CHANNEL).reason(
                                "Removed @everyone"
                        ).queue();
                    } else {
                        tc.getPermissionOverride(role).delete().reason(MessageFormat.format(
                                "Removed role {0}",
                                role.getName()
                        )).queue();
                    }
                }catch(Exception ex){
                    EmbedUtil.sendError(msg, "Couldn't remove/override the role! Is it higher than my own?");
                    return;
                }

                EmbedBuilder roleSuccess = EmbedUtil.getEmbed(msg.getAuthor())
                        .setColor(Color.GREEN)
                        .setDescription(MessageFormat.format(
                                "Successfully removed the role {0}!",
                                role.getAsMention()
                        ));
                tc.sendMessage(roleSuccess.build()).queue();

                LogUtil.roleRemove(tc, guild.getMember(msg.getAuthor()), role);
                break;

            default:
                EmbedUtil.sendError(msg, MessageFormat.format(
                        "You need to provide valid arguments!\n" +
                        "Usage: `{0}remove <member <ID>|role <ID>>`",
                        Database.getPrefix(guild)
                ));
        }
    }
}
