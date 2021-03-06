package apple.excursion.utils;

import apple.excursion.discord.DiscordBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

import java.util.*;

public class GetColoredName {
    private static final Collection<Long> roles = new HashSet<>();
    public static final long EXCURSION_EVIDENCE_CHANNEL = 664337960203714592L;

    static {
        roles.add(555340987086667776L); // Farplane Resident
        roles.add(757738058656252034L); // Masonry Two
        roles.add(744702853398003794L); // Purple
        roles.add(728080120761417828L); // Indigo
        roles.add(728080122258653316L); // Green
        roles.add(744469914508853258L); // Blue
        roles.add(744702664201470022L); // Green
        roles.add(744702524640198727L); // Yellow
        roles.add(728080119012393031L); // Magenta
        roles.add(727021694761566311L); // Dare Red
        roles.add(728080112532324363L); // Mustard
        roles.add(727023199161679912L); // Orange
        roles.add(555342268912107521L); // Masonry One
        roles.add(555342151135920138L); // Excursionist
    }

    public static ColoredName get(long id) {
        Guild guild = DiscordBot.client.getGuildById(DiscordBot.EXCURSION_GUILD_ID);
        if (guild == null) return new ColoredName();
        Member member;
        try {
            member = guild.retrieveMemberById(id).complete();
        } catch (ErrorResponseException e) {
            member = null;
        }
        if (member == null) {
            return new ColoredName();
        } else {
            String name = member.getEffectiveName();
            List<Role> memberRoles = member.getRoles();
            for (Role role : memberRoles) {
                if (roles.contains(role.getIdLong())) {
                    return new ColoredName(name, role.getColorRaw());
                }
            }
            return new ColoredName(name);
        }
    }

    public static List<Long> get(String nameToGet) {
        List<Long> ids = new ArrayList<>();
        GuildChannel guildChannel = DiscordBot.client.getGuildChannelById(EXCURSION_EVIDENCE_CHANNEL);
        if (guildChannel == null) return ids;
        List<Member> members = guildChannel.getMembers();
        nameToGet = nameToGet.toLowerCase();
        for (Member member : members) {
            if (member.getEffectiveName().toLowerCase().contains(nameToGet))
                ids.add(member.getIdLong());
        }
        return ids;
    }
}
