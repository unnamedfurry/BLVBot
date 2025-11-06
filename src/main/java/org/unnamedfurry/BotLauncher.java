package org.unnamedfurry;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class BotLauncher extends ListenerAdapter {
    static JDA bot;
    public static void main(String[] args) throws Exception{
        /*Path jarDir = Paths.get(
                BotLauncher.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParent();
        String token = Files.readString(jarDir.resolve("bot_token.txt")).trim();*/
        String token = Files.readString(Path.of("bot_token.txt")).trim();
        bot = JDABuilder.createDefault(token).addEventListeners(new EventListener()).enableIntents(GatewayIntent.MESSAGE_CONTENT).build().awaitReady();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event){
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        MessageChannel channel = bot.getTextChannelById("1433490601487368192");
        assert channel != null;
        channel.sendMessage("Бот включился.\n-# Время: " + time).queue();
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event){
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        MessageChannel channel = bot.getTextChannelById("1433490601487368192");
        assert channel != null;
        channel.sendMessage("Бот выключился.\n-# Время: " + time).queue();
    }
}