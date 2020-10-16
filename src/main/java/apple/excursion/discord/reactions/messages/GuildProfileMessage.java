package apple.excursion.discord.reactions.messages;

import apple.excursion.database.objects.GuildData;
import apple.excursion.database.objects.OldSubmission;
import apple.excursion.database.objects.PlayerData;
import apple.excursion.discord.data.answers.GuildLeaderboardProfile;
import apple.excursion.discord.reactions.AllReactables;
import apple.excursion.discord.reactions.ReactableMessage;
import apple.excursion.utils.Pretty;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.List;

public class GuildProfileMessage implements ReactableMessage {
    private static final int ENTRIES_PER_PAGE = 10;
    private final GuildData matchedGuild;
    private final List<PlayerData> players;
    private final GuildLeaderboardProfile guildProfile;
    private final Message message;
    private int page = 0;
    private long lastUpdated = System.currentTimeMillis();

    public GuildProfileMessage(GuildData matchedGuild, List<PlayerData> players, GuildLeaderboardProfile guildProfile, MessageChannel channel) {
        this.matchedGuild = matchedGuild;
        this.players = players;
        this.guildProfile = guildProfile;
        players.sort((o1, o2) -> o2.score - o1.score);
        this.message = channel.sendMessage(makeMessage()).complete();
        message.addReaction(AllReactables.Reactable.LEFT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.RIGHT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.TOP.getFirstEmoji()).queue();
        AllReactables.add(this);
    }

    private MessageEmbed makeMessage() {



        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(String.format("%s [%s]", matchedGuild.name, matchedGuild.tag));
        StringBuilder header = new StringBuilder();
        header.append(String.format("Guild rank : #%d\n", guildProfile.getRank()));
        header.append(Pretty.getProgressBar(guildProfile.getProgress()));
        header.append("\n\n");
        header.append(String.format("Guild EP: %d EP", guildProfile.getTotalEp()));

        StringBuilder body = new StringBuilder();
        body.append("```glsl\n");
        body.append(String.format("%s [%s] Leaderboards Page (%d)\n", matchedGuild.name, matchedGuild.tag, page));
        body.append(getDash());
        body.append(String.format("|%3s| %-31s| %-8s|\n", "", "Name", "Total EP"));
        int upper = Math.min(((page + 1) * ENTRIES_PER_PAGE), players.size());
        for (int place = page * ENTRIES_PER_PAGE; place < upper; place++) {
            if (place % 5 == 0) {
                body.append(getDash());
            }
            PlayerData player = players.get(place);
            body.append(String.format("|%3s| %-31s| %-8s|", place + 1, player.name, player.score));
            body.append("\n");
        }
        body.append("```");

        StringBuilder footer = new StringBuilder();
        if (matchedGuild.submissions.isEmpty()) {
            footer.append("**There is no submission history**\n");
        } else {
            footer.append("**Submission History:** \n");
            for (OldSubmission submission : matchedGuild.submissions) {
                footer.append(submission.makeSubmissionHistoryMessage());
                footer.append('\n');
            }
        }
        embed.setDescription(header.toString() + "\n\n" + body.toString() + "\n\n" + footer.toString());
        return embed.build();
    }

    private String getDash() {
        return "-".repeat(48) + "\n";
    }

    public void forward() {
        if ((players.size() - 1) / ENTRIES_PER_PAGE >= page + 1) {
            page++;
            message.editMessage(makeMessage()).queue();
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    public void backward() {
        if (page != 0) {
            page--;
            message.editMessage(makeMessage()).queue();
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    private void top() {
        page = 0;
        message.editMessage(makeMessage()).queue();
        this.lastUpdated = System.currentTimeMillis();
    }

    @Override
    public void dealWithReaction(AllReactables.Reactable reactable, String reaction, MessageReactionAddEvent event) {
        final User user = event.getUser();
        if (user == null) return;
        switch (reactable) {
            case LEFT:
                backward();
                event.getReaction().removeReaction(user).queue();
                break;
            case RIGHT:
                forward();
                event.getReaction().removeReaction(user).queue();
                break;
            case TOP:
                top();
                event.getReaction().removeReaction(user).queue();
                break;
        }
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