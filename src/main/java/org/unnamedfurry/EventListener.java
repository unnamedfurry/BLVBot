package org.unnamedfurry;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
    TextCommands textCommands = new TextCommands();
    SlashCommands slashCommands = new SlashCommands();
    MusicBot musicBot = new MusicBot();

    @Override
    public void onMessageReceived(MessageReceivedEvent messageEvent){
        if (messageEvent.getAuthor().isBot()) return;

        Message message = messageEvent.getMessage();
        String content = message.getContentRaw();
        MessageChannel channel = message.getChannel();

        if (content.equals("!ping")) {
            channel.sendMessage("Pong!").queue();
        } else if (content.startsWith("!avatar")) {
            String[] contentFormatted = content.split(" ");
            if (contentFormatted[1] != null){
                try {
                    textCommands.avatarCommand(contentFormatted, channel, message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                channel.sendMessage("Отправлена неверная команда! Проверьте синтаксис команды при помощи `!help`.").queue();
            }
        } else if(content.startsWith("!ban")){
            textCommands.banCommand(channel, message, content);
        } else if(content.startsWith("!unban")){
            textCommands.unbanCommand(channel, message, content);
        } else if (content.startsWith("!clear")){
            textCommands.clearMessages(channel, message, content);
        } else if (content.startsWith("!whitelistRole")){
            textCommands.whitelistRole(message, channel, content);
        } else if (content.startsWith("!kick")){
            textCommands.kickCommand(channel, message, content);
        } else if (content.startsWith("!timeout")) {
            textCommands.timeoutCommand(channel, message, content);
        } else if (content.startsWith("!play")) {
            musicBot.play(message);
        } else if (content.startsWith("!stop")) {
            musicBot.stop(message);
        } else if (content.startsWith("!pause")) {
            musicBot.pause(message);
        } else if (content.startsWith("!help") || content.startsWith("!usage")) {
            textCommands.HelpCommand(channel, message);
        } else if (content.startsWith("!registerCommands")){
            slashCommands.commandRegistration(message);
        }
    }
}