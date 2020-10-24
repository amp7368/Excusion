package apple.excursion.database.queries;

import apple.excursion.database.VerifyDB;
import apple.excursion.database.objects.guild.GuildHeader;
import apple.excursion.database.objects.OldSubmission;
import apple.excursion.database.objects.player.PlayerData;
import apple.excursion.database.objects.guild.GuildLeaderboardEntry;
import apple.excursion.database.objects.guild.LeaderboardOfGuilds;
import apple.excursion.database.objects.player.PlayerHeader;
import apple.excursion.database.objects.player.PlayerLeaderboard;
import apple.excursion.database.objects.player.PlayerLeaderboardEntry;
import apple.excursion.discord.commands.general.postcard.CommandSubmit;
import apple.excursion.discord.data.TaskSimple;
import apple.excursion.discord.data.answers.HistoryLeaderboardOfGuilds;
import apple.excursion.discord.data.answers.HistoryPlayerLeaderboard;
import apple.excursion.discord.data.answers.SubmissionData;
import apple.excursion.utils.GetColoredName;
import apple.excursion.utils.Pair;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import static apple.excursion.discord.reactions.messages.benchmark.CalendarMessage.EPOCH_START_OF_EXCURSION;
import static apple.excursion.discord.reactions.messages.benchmark.CalendarMessage.EPOCH_START_OF_SUBMISSION_HISTORY;

public class GetDB {
    public static PlayerData getPlayerData(Pair<Long, String> id, int submissionSize) throws SQLException {
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetPlayerAll(id.getKey());
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            String playerName;
            // if the player doesn't exist
            if (response.isClosed() || (playerName = response.getString(2)) == null) {
                // the player definitely has no score. decide whether they don't exist of they only have no score
                sql = GetSql.getSqlGetPlayerGuild(id.getKey());
                response = statement.executeQuery(sql);
                String guildName;
                if (response.isClosed() || (guildName = response.getString(1)) == null) {
                    // add the player
                    InsertDB.insertPlayer(id, null, null);
                    statement.close();
                    return new PlayerData(id.getKey(), id.getValue(), null, null, new ArrayList<>(), 0, 0);
                } else {
                    // the player exists
                    String guildTag = response.getString(2);
                    response.close();
                    statement.close();
                    return new PlayerData(id.getKey(), id.getValue(), guildName, guildTag, new ArrayList<>(), 0, 0);
                }

            }
            // if the player has the wrong playerName
            if (!playerName.equals(id.getValue())) {
                sql = GetSql.updatePlayerName(id.getKey(), id.getValue());
                statement.execute(sql);
                playerName = id.getValue();
                // reget the playerdata because the response closes somehow. probably because statement.execute();
                sql = GetSql.getSqlGetPlayerAll(id.getKey());
                response = statement.executeQuery(sql);
            }

            int score = response.getInt(1);
            String guildTag = response.getString(3);
            String guildName = response.getString(4);
            int soulJuice = response.getInt(5);
            response.close();
            List<OldSubmission> submissions = new ArrayList<>();
            sql = GetSql.getSqlGetPlayerSubmissionHistory(id.getKey(), submissionSize);
            response = statement.executeQuery(sql);
            while (response.next()) {
                submissions.add(new OldSubmission(response));
            }

            statement.close();
            response.close();

            return new PlayerData(id.getKey(), playerName, guildName, guildTag, submissions, score, soulJuice);
        }
    }


    public static LeaderboardOfGuilds getGuildLeaderboard() throws SQLException {
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetGuilds();
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<GuildLeaderboardEntry> guilds = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    int guildScore = response.getInt(1);
                    String guildTag = response.getString(2);
                    String guildName = response.getString(3);
                    String playerName = response.getString(4);
                    long playerUid = response.getLong(5);
                    int playerScore = response.getInt(6);
                    guilds.add(new GuildLeaderboardEntry(guildTag, guildName, guildScore, playerName, playerUid, playerScore));
                }
            response.close();
            statement.close();
            return new LeaderboardOfGuilds(guilds);
        }
    }

    public static List<PlayerData> getPlayersInGuild(String tag, long startTime, long endTime) throws SQLException {
        synchronized (VerifyDB.syncDB) {
            String sql;
            if (startTime == -1 || endTime == -1)
                sql = GetSql.getSqlGetPlayersInGuild(tag);
            else
                sql = GetSql.getSqlGetPlayersInGuild(tag, startTime, endTime);

            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<PlayerData> players = new ArrayList<>();
            response.next();
            while (!response.isClosed()) {
                long id = response.getLong(1);
                String playerName = response.getString(2);
                String guildTag = response.getString(3);
                String guildName = response.getString(4);
                int score = response.getInt(5);
                int soulJuice = response.getInt(6);
                players.add(new PlayerData(
                        id,
                        playerName,
                        guildName,
                        guildTag,
                        null,
                        score,
                        soulJuice
                )); // submissions is normally not null, but for this situation we don't need them
                response.next();
            }
            response.close();
            statement.close();
            return players;
        }
    }

    public static List<GuildHeader> getGuildNameList() throws SQLException {
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetGuildNames();
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<GuildHeader> guilds = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    String guildTag = response.getString(1);
                    String guildName = response.getString(2);
                    guilds.add(new GuildHeader(guildTag, guildName));
                }
            response.close();
            statement.close();
            return guilds;
        }
    }

    public static List<OldSubmission> getGuildSubmissions(String guildTag, long startTime, long endTime) throws SQLException {
        List<OldSubmission> submissions = new ArrayList<>();
        String sql;
        if (startTime == -1 || endTime == -1)
            sql = GetSql.getSqlGetGuildSubmissionHistory(guildTag);
        else
            sql = GetSql.getSqlGetGuildSubmissionHistory(guildTag, startTime, endTime);
        Statement statement = VerifyDB.database.createStatement();
        ResultSet response = statement.executeQuery(sql);
        while (response.next()) {
            submissions.add(new OldSubmission(response));
        }

        statement.close();
        response.close();
        return submissions;
    }

    public static PlayerLeaderboard getPlayerLeaderboard() throws SQLException {
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetPlayerLeaderboard();
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<PlayerLeaderboardEntry> leaderboard = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    leaderboard.add(new PlayerLeaderboardEntry(response));
                }
            return new PlayerLeaderboard(leaderboard);
        }
    }

    public static List<PlayerHeader> getPlayerHeaders() throws SQLException {
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetPlayerHeaders();
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<PlayerHeader> players = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    players.add(new PlayerHeader(response.getLong(1),
                            response.getString(2),
                            response.getInt(3),
                            response.getInt(4),
                            response.getString(5),
                            response.getString(6)
                    ));
                }
            sql = GetSql.getSqlGetPlayerHeadersNoScore();
            response = statement.executeQuery(sql);
            if (!response.isClosed())
                while (response.next()) {
                    long id = response.getLong(1);
                    boolean contains = false;
                    for (PlayerHeader header : players)
                        if (header.id == id) {
                            contains = true;
                            break;
                        }
                    if (!contains)
                        players.add(new PlayerHeader(id,
                                response.getString(2),
                                response.getInt(3),
                                0,
                                response.getString(4),
                                response.getString(5)
                        ));
                }
            statement.close();
            return players;
        }

    }

    public static HistoryPlayerLeaderboard getPlayerLeaderboard(int timeField, int timeInterval, Calendar timeLookingAt) throws SQLException {
        Pair<Long, Long> times = getTimes(timeField, timeInterval, timeLookingAt);
        long startTime = times.getKey();
        if (startTime < EPOCH_START_OF_SUBMISSION_HISTORY) startTime = EPOCH_START_OF_EXCURSION;
        long endTime = times.getValue();
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetPlayerLeaderboard(startTime, endTime);
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<PlayerLeaderboardEntry> leaderboard = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    leaderboard.add(new PlayerLeaderboardEntry(response));
                }
            return new HistoryPlayerLeaderboard(new PlayerLeaderboard(leaderboard), startTime, endTime);
        }
    }

    public static HistoryLeaderboardOfGuilds getGuildLeaderboard(int timeField, int timeInterval, Calendar timeLookingAt) throws SQLException {
        long startTime;
        long endTime;
        Pair<Long, Long> times = getTimes(timeField, timeInterval, timeLookingAt);
        startTime = times.getKey();
        if (startTime < EPOCH_START_OF_SUBMISSION_HISTORY) startTime = EPOCH_START_OF_EXCURSION;
        endTime = times.getValue();
        synchronized (VerifyDB.syncDB) {
            String sql = GetSql.getSqlGetGuilds(startTime, endTime);
            Statement statement = VerifyDB.database.createStatement();
            ResultSet response = statement.executeQuery(sql);
            List<GuildLeaderboardEntry> guilds = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    int guildScore = response.getInt(1);
                    String guildTag = response.getString(2);
                    String guildName = response.getString(3);
                    long playerUid = response.getLong(4);
                    String playerName = response.getString(5);
                    int playerScore = response.getInt(6);
                    guilds.add(new GuildLeaderboardEntry(guildTag, guildName, guildScore, playerName, playerUid, playerScore));
                }
            response.close();
            statement.close();
            return new HistoryLeaderboardOfGuilds(new LeaderboardOfGuilds(guilds), startTime, endTime);
        }
    }

    private static Pair<Long, Long> getTimes(int timeField, int timeInterval, Calendar timeLookingAt) {
        long startTime = 0;
        long endTime = 0;
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(timeLookingAt.getTimeInMillis());
        switch (timeField) {
            case Calendar.MONTH:
                int dayOfMonthInCalendar = timeLookingAt.get(Calendar.DAY_OF_MONTH); // what day of the month it is
                int firstDayOfMonth = timeLookingAt.get(Calendar.DAY_OF_YEAR) - dayOfMonthInCalendar; // the first day of the month for this month
                start.set(Calendar.DAY_OF_YEAR, firstDayOfMonth + 1);
                start.add(Calendar.DAY_OF_YEAR, -timeInterval + 1); // +1 because we already got to the beginning of the month
                setStartOfDay(start);
                startTime = start.getTimeInMillis();
                start.add(Calendar.MONTH, timeInterval); // add to get the end date exclusive
                start.add(Calendar.DAY_OF_YEAR, -1); // subtract 1 day to go back to the previous month
                setEndOfDay(start);
                endTime = start.getTimeInMillis();
                break;
            case Calendar.WEEK_OF_YEAR:
                timeLookingAt.setFirstDayOfWeek(Calendar.MONDAY);
                start.setFirstDayOfWeek(Calendar.MONDAY);
                int dayOfWeekInCalendar = (timeLookingAt.get(Calendar.DAY_OF_WEEK) - 2) % 7;
                if (dayOfWeekInCalendar < 0) dayOfWeekInCalendar += 7;
                start.add(Calendar.DAY_OF_MONTH, -dayOfWeekInCalendar);
                setStartOfDay(start);
                startTime = start.getTimeInMillis();
                start.add(Calendar.WEEK_OF_YEAR, timeInterval);
                start.add(Calendar.DAY_OF_YEAR, -1); // subtract 1 day to go back to the previous month
                setEndOfDay(start);
                endTime = start.getTimeInMillis();
                break;
            case Calendar.DAY_OF_YEAR:
                setStartOfDay(start);
                startTime = start.getTimeInMillis();
                start.add(Calendar.DAY_OF_YEAR, timeInterval);
                start.add(Calendar.DAY_OF_YEAR, -1);
                setEndOfDay(start);
                endTime = start.getTimeInMillis();
                break;
        }
        return new Pair<>(startTime, endTime);
    }

    private static void setEndOfDay(Calendar start) {
        start.set(Calendar.SECOND, 59);
        start.set(Calendar.MINUTE, 59);
        start.set(Calendar.HOUR, 11);
        start.set(Calendar.AM_PM, Calendar.PM);
    }

    private static void setStartOfDay(Calendar start) {
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.HOUR, 0);
        start.set(Calendar.AM_PM, Calendar.AM);
    }

    public static List<String> executeSql(String sql) throws SQLException {
        Statement statement = VerifyDB.database.createStatement();
        ResultSet response;
        try {
            response = statement.executeQuery(sql);
        } catch (SQLException e) {
            if (e.getErrorCode() == 101) {
                return Collections.singletonList("done");
            }
            throw e;
        }

        List<String> returnVal = new ArrayList<>();
        if (!response.isClosed()) {
            int i = 1;
            while (response.next()) {
                try {
                    returnVal.add(response.getString(i++));
                } catch (SQLException e) {
                    break;
                }
            }
        }
        return returnVal;
    }

    @Nullable
    public static Pair<Integer, SubmissionData> getSubmissionData(long channelId, long messageId) throws SQLException {
        Statement statement = VerifyDB.database.createStatement();
        String sql = GetSql.getSqlGetResponseSubmissionData(channelId, messageId);
        ResultSet response = statement.executeQuery(sql);
        if (!response.isClosed()) {
            int responseId = response.getInt(1);

            boolean isAccepted = response.getBoolean(2);
            boolean isCompleted = response.getBoolean(3);
            long epochTimeOfSubmission = response.getTimestamp(4).getTime();
            String attachmentsUrl = response.getString(5);
            List<String> links = Arrays.asList(response.getString(6).split("`"));
            TaskSimple task = new TaskSimple(
                    response.getInt(9),
                    response.getString(8),
                    response.getString(7)
            );
            SubmissionData.TaskSubmissionType taskSubmissionType = SubmissionData.TaskSubmissionType.valueOf(response.getString(10));
            String submitterName = response.getString(11);
            long submitterId = response.getLong(12);
            response.close();

            // get the idToNames
            sql = GetSql.getSqlGetResponseSubmissionNames(responseId);
            response = statement.executeQuery(sql);
            List<Pair<Long, String>> idToNames = new ArrayList<>();
            if (!response.isClosed())
                while (response.next()) {
                    idToNames.add(new Pair<>(
                            response.getLong(1),
                            response.getString(2)
                    ));
                }

            // get the playersdata for submissionHistory
            List<PlayerData> players = new ArrayList<>();
            for (Pair<Long, String> idToName : idToNames) {
                players.add(GetDB.getPlayerData(idToName, CommandSubmit.SUBMISSION_HISTORY_SIZE));
            }
            response.close();
            statement.close();
            // get the color
            return new Pair<>(
                    responseId,
                    new SubmissionData(isAccepted, isCompleted, epochTimeOfSubmission, attachmentsUrl, links, task,
                            taskSubmissionType, submitterName, submitterId, idToNames, players, GetColoredName.get(submitterId).getColor()));
        }
        return null;
    }

    public static List<Pair<Long, Long>> getResponseMessages(int responseId) throws SQLException {
        String sql = GetSql.getSqlGetResponseReviewerMessages(responseId);
        Statement statement = VerifyDB.database.createStatement();
        ResultSet response = statement.executeQuery(sql);
        List<Pair<Long, Long>> messages = new ArrayList<>();
        if (!response.isClosed()) {
            while (response.next()) {
                messages.add(new Pair<>(response.getLong(1), response.getLong(2)));
            }
        }
        response.close();
        statement.close();
        return messages;
    }
}
