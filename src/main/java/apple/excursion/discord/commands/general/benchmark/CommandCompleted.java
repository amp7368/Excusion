package apple.excursion.discord.commands.general.benchmark;

import apple.excursion.database.objects.OldSubmission;
import apple.excursion.database.objects.player.PlayerData;
import apple.excursion.database.queries.GetDB;
import apple.excursion.discord.commands.DoCommand;
import apple.excursion.discord.data.Task;
import apple.excursion.sheets.SheetsTasks;
import apple.excursion.utils.ColoredName;
import apple.excursion.utils.GetColoredName;
import apple.excursion.utils.Pair;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.*;

public class CommandCompleted implements DoCommand {
    @Override
    public void dealWithCommand(MessageReceivedEvent event) {
        String[] content = event.getMessage().getContentStripped().split(" ");
        ColoredName coloredName = GetColoredName.get(event.getAuthor().getIdLong());
        Member member = event.getMember();
        if (member == null) return;// the member should always exist
        String name = coloredName.getName() == null ? ColoredName.getGuestName(member.getEffectiveName()) : coloredName.getName();
        PlayerData player;
        try {
            player = GetDB.getPlayerData(new Pair<>(event.getAuthor().getIdLong(), name));
        } catch (SQLException throwables) {
            event.getChannel().sendMessage("There was an SQLException getting your playerdata").queue();
            return;
        }
        Map<String, List<OldSubmission>> taskNameToSubmissions = new HashMap<>();
        List<Task> tasksList = SheetsTasks.getTasks();
        if (content.length < 2) {
            // fill out the taskNameToSubmissions with tasks that only match our name

            // fill out the map with submissions
            List<String> contentList = new ArrayList<>(Arrays.asList(content));
            contentList.remove(0);
            String taskName = String.join(" ", contentList);
        } else {
            // make the info for anything that contains that quest
//            submissions = player.submissions;
        }

    }
}