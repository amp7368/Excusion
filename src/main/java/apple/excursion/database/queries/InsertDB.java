package apple.excursion.database.queries;

import apple.excursion.database.VerifyDB;
import apple.excursion.database.objects.CrossChatId;
import apple.excursion.database.objects.MessageId;
import apple.excursion.discord.DiscordBot;
import apple.excursion.discord.cross_chat.CrossChat;
import apple.excursion.discord.data.answers.SubmissionData;
import apple.excursion.sheets.SheetsPlayerStats;
import apple.excursion.utils.Pair;
import apple.excursion.utils.SendLogs;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static apple.excursion.database.VerifyDB.*;

public class InsertDB {
    private static final int SOUL_JUICE_FOR_DAILY = 1;
    private static final String MODULE = "InsertDB";

    public static int insertSubmission(SubmissionData data) throws SQLException {
        synchronized (VerifyDB.syncDB) {
            VerifyDB.verify();
            if (data.getType() == SubmissionData.TaskSubmissionType.DAILY) {
                VerifyDB.verifyCalendar();
            }
            String insertSql, getSql;
            ResultSet response;

            // insert the new submission in the table
            insertSql = GetSql.getSqlInsertSubmission(data);
            Statement statement = VerifyDB.database.createStatement();
            statement.execute(insertSql);

            int soulJuice;
            if (data.getType() == SubmissionData.TaskSubmissionType.NORMAL)
                soulJuice = 0;
            else if (data.getType() == SubmissionData.TaskSubmissionType.DAILY)
                soulJuice = SOUL_JUICE_FOR_DAILY;
            else
                soulJuice = 0;

            for (Pair<Long, String> id : data.getSubmittersNameAndIds()) {
                try {
                    SheetsPlayerStats.submit(data.getTaskName(), id.getKey(), id.getValue(), soulJuice);
                } catch (IOException e) {
                    SendLogs.error(MODULE, String.format("IOException updating the sheet for <%s,%d> in %s", id.getValue(), id.getKey(), data.getTaskName()));
                    DiscordBot.client.retrieveUserById(id.getKey()).queue(user -> {
                        if ((user != null) && !user.isBot())
                            user.openPrivateChannel().queue(c ->
                                    c.sendMessage("There was an error making your profile. Tell appleptr16 or ojomFox: " + e.getMessage()).queue());
                    }, failure -> {
                        SendLogs.discordError(MODULE, failure.getMessage());
                    });
                }
                getSql = GetSql.getSqlGetPlayerGuild(id.getKey());
                response = statement.executeQuery(getSql);
                String guildTag;
                if (response.isClosed()) {
                    insertPlayer(id, DEFAULT_GUILD_TAG, DEFAULT_GUILD_NAME);
                    guildTag = DEFAULT_GUILD_TAG;
                } else {
                    guildTag = response.getString(2);
                }
                response.close();
                String updateSql = GetSql.getSqlUpdatePlayerSoulJuice(id.getKey(),soulJuice);
                statement.execute(updateSql);
                insertSql = GetSql.getSqlInsertSubmissionLink(currentSubmissionId, id.getKey(), guildTag);
                statement.execute(insertSql);
            }
            statement.executeBatch();
            statement.close();
            return currentSubmissionId++;
        }
    }

    public static int insertIncompleteSubmission(SubmissionData submissionData) throws SQLException {
        int id;
        synchronized (syncDB) {
            Statement statement = database.createStatement();
            String insertSql = GetSql.getSqlInsertResponse(currentResponseId, submissionData);
            statement.execute(insertSql);
            for (Pair<Long, String> idAndName : submissionData.getSubmittersNameAndIds()) {
                insertSql = GetSql.getSqlInsertResponseSubmitters(currentResponseId, idAndName.getKey(), idAndName.getValue());
                statement.execute(insertSql);
            }
            statement.close();
            id = currentResponseId;
            currentResponseId++;
        }
        return id;
    }

    public static void insertIncompleteSubmissionLink(long messageId, long channelId, int responseId) throws SQLException {
        synchronized (syncDB) {
            Statement statement = database.createStatement();
            String sql = GetSql.getSqlInsertResponseLink(messageId, channelId, responseId);
            statement.execute(sql);
            statement.close();
        }
    }

    public static void insertPlayer(Pair<Long, String> id, String guildTag, String guildName) throws SQLException {
        synchronized (syncDB) {
            try {
                SheetsPlayerStats.addProfile(id.getKey(), id.getValue());
            } catch (IOException ignored) { //this is fine because updates will add this player later somehow
            }
            if (guildName != null) {
                try {
                    SheetsPlayerStats.updateGuild(guildName, guildTag, id.getKey(), id.getValue());
                } catch (IOException ignored) { //this is fine because updates will add this player later somehow
                }
            }
            Statement statement = VerifyDB.database.createStatement();
            statement.execute(GetSql.getSqlInsertPlayers(id, guildName, guildTag));
            statement.close();
        }
    }

    public static void insertCrossChat(long serverId, long channelId) throws SQLException {
        synchronized (syncDB) {
            Statement statement = database.createStatement();
            String sql = GetSql.getSqlExistsCrossChat(serverId);
            int exists = statement.executeQuery(sql).getInt(1);
            if (exists == 1) {
                sql = GetSql.getSqlUpdateCrossChat(serverId, channelId);
            } else {
                sql = GetSql.getSqlInsertCrossChat(serverId, channelId);
            }
            statement.execute(sql);
            statement.close();
            CrossChat.add(new CrossChatId(serverId, channelId));
        }
    }

    public static void removeCrossChat(long serverId) throws SQLException {
        synchronized (syncDB) {
            Statement statement = database.createStatement();
            String sql = GetSql.getSqlRemoveCrossChat(serverId);
            statement.execute(sql);
            statement.close();
        }
    }

    public static void insertCrossChatMessage(long myMessageId, List<MessageId> messageIds, long owner, String username, int color, String avatarUrl, String imageUrl, String description) throws SQLException {
        synchronized (syncDB) {
            Statement statement = database.createStatement();
            statement.addBatch(GetSql.getSqlInsertCrossChatSent(myMessageId, owner, username, color, avatarUrl, imageUrl, description));
            for (MessageId messageId : messageIds) {
                statement.addBatch(GetSql.getSqlInsertCrossChatMessages(myMessageId, messageId.serverId, messageId.channelId, messageId.messageId));
            }
            statement.executeBatch();
            statement.close();
        }
    }
}
