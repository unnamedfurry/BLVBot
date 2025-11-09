package org.unnamedfurry;

import dev.arbjerg.lavalink.client.Link;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MusicBot {
    final static Logger logger = LoggerFactory.getLogger(MusicBot.class);
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }

    public void play(Message message){
        String content = message.getContentRaw();
        String[] messageArr = content.split(" ");
        Member selfMember = message.getGuild().getSelfMember();
        GuildVoiceState selfState = selfMember.getVoiceState();
        GuildVoiceState memberState = message.getMember().getVoiceState();
        HttpClient http = HttpClient.newHttpClient();
        if (messageArr.length == 1) message.getChannel().sendMessage("Вы не можете указать пустые аргументы. Добавьте название или ссылку на желаемую музыку.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        if (!memberState.inAudioChannel()) message.getChannel().sendMessage("Вам нужно находиться в голосовом канале!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        try {
            if (!selfState.inAudioChannel()){
                BotLauncher.connect(message);
            }
            String requestedSong = content.substring(6);
            if (!isURL(requestedSong)){
                requestedSong = "ytsearch:" + requestedSong + " music";
            }

            try {
                String uri = "http://127.0.0.1:2333/v4/loadtracks?identifier=" + requestedSong;
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(uri))
                        .header("Authorization", BotLauncher.lavalinkPassword())
                        .GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() / 100 != 2){
                    message.getChannel().sendMessage("Не удалось получить желаемый трек от Lavalink сервера. (HTTP " + resp.statusCode() + ").\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                    return;
                }
                JSONObject json = new JSONObject(resp.body());
                String loadType = json.optString("loadType", "NO_MATCHES");
                logger.info("Response from Lavalink: ");
                logger.info(resp.body());
                if ("NO_MATCHES".equalsIgnoreCase(loadType)) {
                    message.getChannel().sendMessage("Не удалось найти запрашиваемый трек.").queue();
                    return;
                }

                String encodedTrack;
                JSONObject first = new JSONObject();
                if (json.has("data")) {
                    JSONObject data = json.getJSONObject("data");
                    encodedTrack = data.getString("encoded");
                    json = data.getJSONObject("info");
                } else if (json.has("tracks")) {
                    JSONArray tracks = json.getJSONArray("tracks");
                    first = tracks.getJSONObject(0);
                    encodedTrack = first.getString("track");
                    json = first.getJSONObject("info");
                } else {
                    message.getChannel().sendMessage("Ответ Lavalink не содержит треков.").queue();
                    return;
                }

                String title = json.optString("title", "Unknown");
                long guildID = message.getGuildIdLong();
                Link link = BotLauncher.getOrCreateLink(guildID);
                encodedTrack = encodedTrack.trim();
                encodedTrack = encodedTrack.replaceAll("\\s+", "");
                encodedTrack = URLEncoder.encode(encodedTrack, StandardCharsets.UTF_8);
                logger.info("Encoded and cleared track: " + encodedTrack);
                link.getNode().decodeTrack(encodedTrack)
                        .flatMap(track -> link.getPlayer().block().setTrack(track))
                        .doOnSuccess(p -> {
                            try {
                                message.getChannel().sendMessage("Сейчас играет: `" + title + "`.\nДля паузы отправьте команду `!pause`, для остановки произведения отправьте команду `!stop`, для перехода к следующей песне отправьте команду `!skip`.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                            } catch (Exception e) {
                                logger.error("Error in success handler: " + e.getMessage());
                            }
                        })
                        .doOnError(err -> {
                            message.getChannel().sendMessage("Не получилось загрузить новую музыку.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                            logger.error("Error loading track: " + err.getMessage());
                        })
                        .subscribe();
            } catch (Exception e) {
                e.getMessage();
                e.printStackTrace();
            }
        } catch (Exception e) {
            message.getChannel().sendMessage("Произошла ошибка при обработке команды!").queue();
            logger.error(e.toString());
            e.printStackTrace();
        }
    }

    private boolean isURL(String input){
        try {
            new URI(input);
            return true;
        } catch (URISyntaxException e){
            return false;
        }
    }

    public void stop(Message message){
        Member selfMember = message.getGuild().getSelfMember();
        GuildVoiceState selfState = selfMember.getVoiceState();
        GuildVoiceState memberState = message.getMember().getVoiceState();
        long guildID = message.getGuildIdLong();
        Optional<Link> maybeLink = Optional.ofNullable(BotLauncher.getLinkIfCashed(guildID));
        if (!memberState.inAudioChannel()) message.getChannel().sendMessage("Вам нужно находиться в голосовом канале!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        if (maybeLink.isEmpty()) message.getChannel().sendMessage("Мне нужно находиться в голосовом канале для этого!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        try {
            if (Objects.equals(memberState.getChannel(), selfState.getChannel())){
                Link link = maybeLink.get();
                link.getPlayer().block().stopTrack().subscribe();
                link.getNode().destroyPlayerAndLink(guildID).subscribe();
                message.getChannel().sendMessage("Проигрывание было завершено и очередь была очищена.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                BotLauncher.disconnect(message);
            }
        } catch (Exception e) {
            message.getChannel().sendMessage("Произошла ошибка при обработке команды!").queue();
            logger.error(e.toString());
        }

    }

    public void pause(Message message){
        long guildID = message.getGuildIdLong();
        Optional<Link> maybeLink = Optional.ofNullable(BotLauncher.getLinkIfCashed(guildID));
        if (maybeLink.isEmpty()) message.getChannel().sendMessage("В данный момент никакая музыка не воспроизводится.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        Link link = maybeLink.get();
        link.getPlayer().block().setPaused(!link.getPlayer().block().getPaused()).subscribe(p -> {
            boolean paused = link.getPlayer().block().getPaused();
            if (paused) {
                message.getChannel().sendMessage("Трек приостановлен.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            } else {
                message.getChannel().sendMessage("Трек возобновлен.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        }, err -> {
            message.getChannel().sendMessage("Не удалось переключить паузу.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        });
    }
}