package apple.excursion.discord.reactions;


import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AllReactables {
    private static final long STOP_WATCHING_DIFFERENCE = 1000 * 60 * 20; // 20 minutes
    private static final Map<Long, ReactableMessage> reactableMessages = new HashMap<>();
    private static final Object mapSyncObject = new Object();

    public final static List<String> emojiAlphabet = Arrays.asList("\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9", "\uD83C\uDDEA", "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED",
            "\uD83C\uDDEE", "\uD83C\uDDEF", "\uD83C\uDDF0", "\uD83C\uDDF1", "\uD83C\uDDF2", "\uD83C\uDDF3", "\uD83C\uDDF4", "\uD83C\uDDF5", "\uD83C\uDDF6", "\uD83C\uDDF7", "\uD83C\uDDF8", "\uD83C\uDDF9", "\uD83C\uDDFA"
            , "\uD83C\uDDFB", "\uD83C\uDDFC", "\uD83C\uDDFD", "\uD83C\uDDFE", "\uD83C\uDDFF");

    public static void add(ReactableMessage message) {
        synchronized (mapSyncObject) {
            reactableMessages.put(message.getId(), message);
        }
    }

    public static void remove(long id) {
        synchronized (mapSyncObject) {
            reactableMessages.remove(id);
        }
    }

    public static void dealWithReaction(@NotNull MessageReactionAddEvent event) {
        String reaction = event.getReactionEmote().getName();
        synchronized (mapSyncObject) {
            for (Reactable reactable : Reactable.values()) {
                if (reactable.isEmoji(reaction)) {
                    ReactableMessage message = reactableMessages.get(event.getMessageIdLong());
                    if (message != null) {
                        message.dealWithReaction(reactable, reaction, event);
                        trimOldMessages();
                        return;
                    }
                }
            }
            trimOldMessages();
        }
    }

    private static void trimOldMessages() {
        synchronized (mapSyncObject) {
            Iterator<Map.Entry<Long, ReactableMessage>> iterator = reactableMessages.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, ReactableMessage> msg = iterator.next();
                if (System.currentTimeMillis() - msg.getValue().getLastUpdated() > STOP_WATCHING_DIFFERENCE) {
                    msg.getValue().dealWithOld();
                    iterator.remove();
                }
            }
        }
    }

    public enum Reactable {
        LEFT(Collections.singletonList("\u2B05")),
        RIGHT(Collections.singletonList("\u27A1")),
        CLOCK_LEFT(Collections.singletonList("\uD83D\uDD59")),
        CLOCK_RIGHT(Collections.singletonList("\uD83D\uDD51")),
        TOP(Collections.singletonList("\u21A9")),
        ACCEPT(Collections.singletonList("\u2705")),
        REJECT(Collections.singletonList("\u274C")),
        DARES(Collections.singletonList("dareemooji"), Collections.singletonList(765315908683825183L)),
        EXCURSIONS(Collections.singletonList("excursionemoji"), Collections.singletonList(765315908738482176L)),
        MISSIONS(Collections.singletonList("missionemoji"), Collections.singletonList(765315910806011914L)),
        ALL_CATEGORIES(Collections.singletonList("\u274C")),
        ALPHABET(emojiAlphabet),
        WORKING(Collections.singletonList("\uD83D\uDEE0")),
        RESPOND(Collections.singletonList("\uD83D\uDCE8"));

        private final List<String> emojis;
        private final List<Long> ids;

        Reactable(List<String> emojis) {
            this.emojis = emojis;
            this.ids = new ArrayList<>();
        }

        Reactable(List<String> emojis, List<Long> ids) {
            this.emojis = emojis;
            this.ids = ids;
        }

        public boolean isEmoji(String reaction) {
            return emojis.contains(reaction);
        }

        public String getFirstEmoji() {
            return emojis.get(0); // it should always have at least one emoji. otherwise it would be useless
        }

        public Long getFirstId() {
            return ids.get(0); // this won't always work because i don't always provide ids
        }
    }
}
