package apple.excursion.discord.reactions.messages;

import apple.excursion.database.objects.OldSubmission;
import apple.excursion.database.objects.player.PlayerData;
import apple.excursion.database.objects.player.PlayerLeaderboardEntry;
import apple.excursion.discord.commands.Commands;
import apple.excursion.discord.data.Task;
import apple.excursion.database.objects.guild.GuildLeaderboardEntry;
import apple.excursion.discord.reactions.AllReactables;
import apple.excursion.discord.reactions.ReactableMessage;
import apple.excursion.sheets.SheetsTasks;
import apple.excursion.utils.PostcardDisplay;
import apple.excursion.utils.Pretty;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ProfileMessage implements ReactableMessage {
    private static final Color BOT_COLOR = new Color(0x4e80f7);
    private static final int TOP_TASKS_SIZE = 4;
    public static final int SUBMISSIONS_PER_PAGE = 5;

    private final Message message;
    private final PlayerLeaderboardEntry playerLeaderboardEntry;
    private final PlayerData player;
    private final GuildLeaderboardEntry guild;
    private final Map<String, List<Task>> topTasks = new HashMap<>();
    private long lastUpdated = System.currentTimeMillis();
    private final List<Task> allTasks = SheetsTasks.getTasks();
    private int countTasksDone = 0;
    private int page = 0;

    public ProfileMessage(PlayerLeaderboardEntry playerLeaderboardEntry, PlayerData player, GuildLeaderboardEntry guild, MessageChannel channel) {
        this.playerLeaderboardEntry = playerLeaderboardEntry;
        this.player = player;
        this.guild = guild;

        Set<Task> tasksDone = new HashSet<>();
        for (Task task : allTasks) {
            if (player.containsSubmission(task)) {
                if (tasksDone.add(task)) {
                    countTasksDone++;
                }
            }
        }
        for (Task task : allTasks) {
            String category = task.category.toUpperCase();
            topTasks.putIfAbsent(category, new ArrayList<>());
            List<Task> tasksToDoByCategory = topTasks.get(category);
            if (!tasksDone.contains(task))
                tasksToDoByCategory.add(task);
        }
        for (List<Task> tasks : topTasks.values()) {
            tasks.sort((o1, o2) -> o2.ep - o1.ep);
        }

        message = channel.sendMessage(makeMessage()).complete();
        message.addReaction(AllReactables.Reactable.LEFT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.RIGHT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.TOP.getFirstEmoji()).queue();

        int i = 0;
        for (List<Task> tasks : topTasks.values()) {
            for (int j = 0; j < TOP_TASKS_SIZE; j++) {
                message.addReaction(AllReactables.emojiAlphabet.get(i++)).queue();
            }
        }
        AllReactables.add(this);

    }

    private MessageEmbed makeMessage() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(player.name);
        StringBuilder description = new StringBuilder();
        description.append(String.format("Soul juice: %d\n\n", player.getSoulJuice()));
        // put guild info
        if (guild == null) {
            description.append(String.format("Not in a guild. To join a guild use %s", Commands.GUILD.getUsageMessage()));
        } else {
            description.append(String.format("Member of %s [%s]\n", playerLeaderboardEntry.guildName, playerLeaderboardEntry.guildTag));
            description.append(String.format("Guild rank: #%d\n", guild.rank));
            description.append(Pretty.getProgressBar(guild.getProgress()));
            description.append('\n');
            description.append(String.format("Guild EP: %d EP", guild.score));
            description.append('\n');
        }
        description.append('\n');

        // put player info
        description.append(String.format("Player rank #%d\n", playerLeaderboardEntry.rank));
        description.append(Pretty.getProgressBar(playerLeaderboardEntry.getProgress()));
        description.append('\n');
        description.append(String.format("%d out of %d tasks done\n", countTasksDone, allTasks.size()));
        description.append(String.format("Total EP: %d EP\n", playerLeaderboardEntry.score));
        description.append('\n');
        int emojiAt = 0;
        for (Map.Entry<String, List<Task>> topTaskCategory : topTasks.entrySet()) {
            description.append(String.format("**Uncompleted %s**\n", Pretty.upperCaseFirst(topTaskCategory.getKey())));
            List<String> taskNames = new ArrayList<>();
            List<Task> tasks = topTaskCategory.getValue();
            int upper = Math.min(((page + 1) * TOP_TASKS_SIZE), tasks.size());
            for (int lower = page * TOP_TASKS_SIZE; lower < upper; lower++) {
                Task task = tasks.get(lower);
                taskNames.add(String.format("%s %s (%d EP)",
                        AllReactables.emojiAlphabet.get(emojiAt++),
                        task.taskName,
                        task.ep
                ));
            }
            description.append(String.join("\n", taskNames));
            description.append('\n');
            description.append('\n');
        }
        description.append("**Submission record:** \n");
        if (player.submissions.isEmpty()) {
            description.append("There is no submission history");
        } else {
            List<OldSubmission> submissions = player.submissions;
            int upper = Math.min(((page + 1) * SUBMISSIONS_PER_PAGE), submissions.size());
            for (int lower = page * SUBMISSIONS_PER_PAGE; lower < upper; lower++) {
                description.append(submissions.get(lower).makeSubmissionHistoryMessage());
                description.append('\n');
            }
        }
        embed.setDescription(description);
        embed.setColor(BOT_COLOR);
        return embed.build();
    }

    @Override
    public void dealWithReaction(AllReactables.Reactable reactable, String reaction, MessageReactionAddEvent event) {
        User user = event.getUser();
        if (user == null) return;
        switch (reactable) {
            case ALPHABET:
                final int size = AllReactables.emojiAlphabet.size();
                for (int i = 0; i < size; i++) {
                    if (AllReactables.emojiAlphabet.get(i).equals(event.getReactionEmote().getName())) {
                        // we found the emote
                        int j = 0;
                        for (List<Task> tasks : topTasks.values()) {
                            if (tasks.size() + j > i) {
                                Task taskFound = tasks.get(i - j);
                                if (taskFound != null) {
                                    message.editMessage(PostcardDisplay.getMessage(taskFound)).queue();
                                }
                                break;
                            }
                            j += tasks.size();
                        }
                        event.getReaction().removeReaction(user).queue();
                    }
                }
                lastUpdated = System.currentTimeMillis();
                break;
            case TOP:
                page = 0;
                message.editMessage(makeMessage()).queue();
                event.getReaction().removeReaction(user).queue();
                break;
            case LEFT:
                backward();
                event.getReaction().removeReaction(user).queue();
                break;
            case RIGHT:
                forward();
                event.getReaction().removeReaction(user).queue();
                break;
        }
    }

    private void forward() {
        int maxTopTasks = 0;
        for (List<Task> category : topTasks.values()) {
            maxTopTasks = Math.max(category.size(), maxTopTasks);
        }
        if ((player.submissions.size() - 1) / SUBMISSIONS_PER_PAGE >= page + 1 ||
                (maxTopTasks - 1) / SUBMISSIONS_PER_PAGE >= page + 1) {
            page++;
            message.editMessage(makeMessage()).queue();
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    private void backward() {
        if (page != 0) {
            page--;
            message.editMessage(makeMessage()).queue();
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public Long getId() {
        return message.getIdLong();
    }

    @Override
    public long getLastUpdated() {
        return lastUpdated;
    }
}
