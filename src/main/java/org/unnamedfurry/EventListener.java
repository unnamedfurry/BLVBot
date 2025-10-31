package org.unnamedfurry;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListener extends ListenerAdapter {
    Commands commands = new Commands();

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (content.equals("!ping")) {
            MessageChannel channel = event.getChannel();
            channel.sendMessage("Pong!").queue();
        } else if (content.startsWith("!avatar")) {
            MessageChannel channel = event.getChannel();
            String[] contentFormatted = content.split(" ");
            if (contentFormatted[1] != null){
                try {
                    commands.avatarCommand(contentFormatted, channel, message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                channel.sendMessage("Отправлена неверная команда! Проверьте синтаксис команды при помощи `!help`.").queue();
            }
        } else if(content.startsWith("!ban")){
            MessageChannel channel = event.getChannel();
            commands.banCommand(channel, message, content);
        } else if(content.startsWith("!unban")){
            MessageChannel channel = event.getChannel();
            commands.unbanCommand(channel, message, content);
        }else if (content.startsWith("!help") || content.startsWith("!usage")) {
            MessageChannel channel = event.getChannel();
            commands.HelpCommand(channel, message);
        }
    }
}