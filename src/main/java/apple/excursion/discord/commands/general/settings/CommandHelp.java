package apple.excursion.discord.commands.general.settings;


import apple.excursion.discord.commands.DoCommand;
import apple.excursion.discord.reactions.messages.settings.HelpMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.time.Instant;

public class CommandHelp implements DoCommand {
    @Override
    public void dealWithCommand(MessageReceivedEvent event) {
        new HelpMessage(event.getChannel());
    }
}
