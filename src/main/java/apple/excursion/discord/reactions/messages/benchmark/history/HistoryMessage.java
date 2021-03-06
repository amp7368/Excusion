package apple.excursion.discord.reactions.messages.benchmark.history;

import apple.excursion.database.queries.GetDB;
import apple.excursion.discord.data.answers.HistoryPlayerLeaderboard;
import apple.excursion.discord.reactions.AllReactables;
import apple.excursion.discord.reactions.ReactableMessage;
import apple.excursion.discord.reactions.messages.benchmark.LeaderboardMessage;
import apple.excursion.utils.Pretty;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static apple.excursion.discord.reactions.messages.benchmark.CalendarMessage.EPOCH_START_OF_EXCURSION;
import static apple.excursion.discord.reactions.messages.benchmark.CalendarMessage.EPOCH_START_OF_SUBMISSION_HISTORY;

public class HistoryMessage implements ReactableMessage {
    private final Message message;
    private final Map<Long, HistoryPlayerLeaderboard> leaderboard = new HashMap<>();
    private final Calendar timeLookingAt = Calendar.getInstance();
    private final Calendar later;
    private final int timeInterval;
    private final int timeField;
    private long lastUpdated = System.currentTimeMillis();
    private int page = 0;

    public HistoryMessage(MessageChannel channel, int timeInterval, int timeField) {
        later = Calendar.getInstance();
        later.setTimeInMillis(timeLookingAt.getTimeInMillis());
        later.add(timeField, timeInterval);

        this.timeInterval = timeInterval;
        this.timeField = timeField;
        timeLookingAt.add(timeField, -timeInterval + 1); // make it the last 3 days instead of the next 3
        this.message = channel.sendMessage(makeMessage()).complete();
        message.addReaction(AllReactables.Reactable.LEFT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.RIGHT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.TOP.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.CLOCK_LEFT.getFirstEmoji()).queue();
        message.addReaction(AllReactables.Reactable.CLOCK_RIGHT.getFirstEmoji()).queue();
        AllReactables.add(this);
    }

    private String makeMessage() {
        if (later.getTimeInMillis() <= timeLookingAt.getTimeInMillis()) {
            return "```\nFuture\nThis data for this time is not available yet\n```";
        }
        HistoryPlayerLeaderboard myLeaderboard = leaderboard.get(timeLookingAt.getTimeInMillis());
        if (myLeaderboard == null) {
            try {
                myLeaderboard = GetDB.getPlayerLeaderboard(timeField, timeInterval, timeLookingAt);
                leaderboard.put(timeLookingAt.getTimeInMillis(), myLeaderboard);

            } catch (SQLException throwables) {
                return "There was an SQLException getting the playerLeaderboard";
            }
        }
        long startTime = myLeaderboard.startTime;
        long endTime = myLeaderboard.endTime;
        if (startTime < EPOCH_START_OF_SUBMISSION_HISTORY) {
            startTime = EPOCH_START_OF_EXCURSION;
        }
        String title = String.format("Excursion Guild Leaderboards Page (%d)\n[%s to %s]",
                page + 1,
                Pretty.date(startTime),
                Pretty.date(endTime));
        return LeaderboardMessage.makeMessageStatic(myLeaderboard.leaderboard, page, title);
    }

    @Override
    public void dealWithReaction(AllReactables.Reactable reactable, String reaction, MessageReactionAddEvent event) {
        final User user = event.getUser();
        if (user == null) return;
        switch (reactable) {
            case LEFT:
                left();
                message.removeReaction(reaction, user).queue();
                break;
            case RIGHT:
                right();
                message.removeReaction(reaction, user).queue();
                break;
            case TOP:
                top();
                message.removeReaction(reaction, user).queue();
                break;
            case CLOCK_LEFT:
                timeLeft();
                message.removeReaction(reaction, user).queue();
                break;
            case CLOCK_RIGHT:
                timeRight();
                message.removeReaction(reaction, user).queue();
                break;
        }
    }

    private void timeRight() {
        timeLookingAt.add(timeField, timeInterval);
        message.editMessage(makeMessage()).queue();
        this.lastUpdated = System.currentTimeMillis();
    }

    private void timeLeft() {
        if (timeLookingAt.getTimeInMillis() < EPOCH_START_OF_SUBMISSION_HISTORY)
            return;
        timeLookingAt.add(timeField, -timeInterval);
        message.editMessage(makeMessage()).queue();
        this.lastUpdated = System.currentTimeMillis();
    }

    private void top() {
        page = 0;
        message.editMessage(makeMessage()).queue();
        this.lastUpdated = System.currentTimeMillis();
    }

    private void right() {
        HistoryPlayerLeaderboard myLeaderboard = leaderboard.get(timeLookingAt.getTimeInMillis());
        if ((myLeaderboard.leaderboard.size() - 1) / LeaderboardMessage.ENTRIES_PER_PAGE >= page + 1) {
            page++;
            message.editMessage(makeMessage()).queue();
        }
        this.lastUpdated = System.currentTimeMillis();
    }

    private void left() {
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

    @Override
    public void dealWithOld() {

        message.clearReactions().queue(success -> {
        }, failure -> {
        }); //ignore if we don't have perms. it's really not a bad thing
    }
}
