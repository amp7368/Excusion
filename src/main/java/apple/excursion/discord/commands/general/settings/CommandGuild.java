package apple.excursion.discord.commands.general.settings;

import apple.excursion.database.queries.GetDB;
import apple.excursion.database.queries.UpdateDB;
import apple.excursion.database.objects.guild.GuildHeader;
import apple.excursion.discord.commands.Commands;
import apple.excursion.discord.commands.DoCommand;
import apple.excursion.discord.reactions.messages.settings.CreateGuildMessage;
import apple.excursion.utils.ColoredName;
import apple.excursion.utils.GetColoredName;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandGuild implements DoCommand {
    private static final int GUILD_NAME_LENGTH_LIMIT = 20;

    @Override
    public void dealWithCommand(MessageReceivedEvent event) {
        String[] contentSplit = event.getMessage().getContentStripped().split(" ");
        if (contentSplit.length < 2) {
            event.getChannel().sendMessage(Commands.GUILD.getUsageMessage()).queue();
            return;
        }
        List<GuildHeader> guilds;
        try {
            guilds = GetDB.getGuildNameList();
        } catch (SQLException throwables) {
            event.getChannel().sendMessage("There was an SQLException getting all the guild's names").queue();
            return;
        }
        List<String> guildNameSplit = new ArrayList<>(Arrays.asList(contentSplit));
        guildNameSplit.remove(0);
        String guildName = String.join(" ", guildNameSplit);
        final String guildTag = contentSplit[1];
        if (!guildTag.matches("[a-zA-Z]*")) {
            event.getChannel().sendMessage(String.format("'%s' has invalid characters that cannot be used in tags.", guildTag)).queue();
            return;
        } else if (guildTag.length() != 3) {
            event.getChannel().sendMessage("A guild tag must be 3 characters in length").queue();
            return;
        }
        GuildHeader match = null;
        for (GuildHeader guild : guilds) {
            if (guild.tag.equals(guildTag) || guildName.equals(guild.name)) {
                match = guild;
                break;
            }
        }
        final Member member = event.getMember();
        if (member == null) return;
        final long playerId = event.getAuthor().getIdLong();
        ColoredName coloredName = GetColoredName.get(playerId);
        final String playerName = coloredName.getName() == null ? ColoredName.getGuestName(member.getEffectiveName()) : coloredName.getName();
        if (match == null) {
            if (contentSplit.length < 3) {
                event.getChannel().sendMessage(
                        String.format("'%s' is not a valid guild tag\n" +
                                        "If you're trying to create a guild, add the [guild_name] to the end of the command",
                                guildTag)).queue();
                return;
            }
            guildNameSplit.remove(0); // get rid of the tag
            guildName = String.join(" ", guildNameSplit);
            if (!guildName.matches("[a-zA-Z\\s]*")) {
                event.getChannel().sendMessage(String.format("'%s' has invalid characters that cannot be used in guildName", guildName)).queue();
                return;
            } else if (guildName.length() > GUILD_NAME_LENGTH_LIMIT) {
                event.getChannel().sendMessage(String.format("The character limit for a guild is %d. '%s' is %d characters", GUILD_NAME_LENGTH_LIMIT, guildName, guildName.length())).queue();
                return;
            }

            // ask the player if they want to create the guild
            new CreateGuildMessage(guildName, guildTag, playerId, playerName, event.getChannel());
            return;
        }
        // change the player's guild
        try {
            UpdateDB.updateGuild(match.name, match.tag, playerId, playerName);
            event.getChannel().sendMessage(String.format("You are now in **%s [%s]**", match.name, match.tag)).queue();
        } catch (SQLException throwables) {
            event.getChannel().sendMessage("There has been an SQLException updating your guild").queue();
        } catch (IOException e) {
            event.getChannel().sendMessage("There was an IOException updating your guild. Please try again even if you're in the correct guild.").queue();
        }
    }
}
