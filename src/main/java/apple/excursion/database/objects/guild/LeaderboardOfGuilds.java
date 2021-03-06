package apple.excursion.database.objects.guild;

import java.util.*;
import java.util.regex.Pattern;

public class LeaderboardOfGuilds {
    private final List<GuildLeaderboardEntry> leaderboard;
    private GuildLeaderboardEntry noGuildsEntry;
    private long totalEp;

    public LeaderboardOfGuilds(List<GuildLeaderboardEntry> guilds) {
        this.leaderboard = guilds;
        initialize();
    }

    public GuildLeaderboardEntry add(GuildHeader header) {
        GuildLeaderboardEntry thisEntry = new GuildLeaderboardEntry(header.name, header.tag);
        leaderboard.add(thisEntry);
        initialize();
        return thisEntry;
    }

    private void initialize() {
        Iterator<GuildLeaderboardEntry> iterator = leaderboard.iterator();
        while (iterator.hasNext()) {
            GuildLeaderboardEntry guild = iterator.next();
            if (guild.isDefault()) {
                noGuildsEntry = guild;
                iterator.remove();
                break; // only 1 default guild entry
            }
        }
        int topScore;
        if (leaderboard.isEmpty()) {
            topScore = Integer.MAX_VALUE;
        } else {
            leaderboard.sort((o1, o2) -> o2.score - o1.score);
            topScore = leaderboard.get(0).score;
        }
        int totalEp = 0;
        final int size = leaderboard.size();
        for (int i = 0; i < size; i++) {
            GuildLeaderboardEntry guild = leaderboard.get(i);
            guild.rank = i + 1;
            guild.topGuildScore = topScore;
            totalEp += guild.score;
        }
        this.totalEp = totalEp;
    }

    public long getTotalEp() {
        return totalEp;
    }

    public long getNoGuildsEp() {
        return noGuildsEntry == null ? 0 : noGuildsEntry.score;
    }

    public int size() {
        return leaderboard.size();
    }

    public GuildLeaderboardEntry get(int i) {
        return leaderboard.get(i);
    }

    public GuildLeaderboardEntry get(String tag, String name) {
        for (GuildLeaderboardEntry guildLeaderboardEntry : leaderboard) {
            if (guildLeaderboardEntry.guildTag.equals(tag)) {
                return guildLeaderboardEntry;
            }
        }
        for (GuildLeaderboardEntry guildLeaderboardEntry : leaderboard) {
            if (guildLeaderboardEntry.guildTag.equalsIgnoreCase(tag)) {
                return guildLeaderboardEntry;
            }
        }
        Pattern pattern = Pattern.compile(".*(" + name + ").*", Pattern.CASE_INSENSITIVE);
        for (GuildLeaderboardEntry guild : leaderboard) {
            if (pattern.matcher(guild.guildName).matches()) {
                return guild;
            }
        }
        return null;
    }
}
