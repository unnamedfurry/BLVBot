package org.unnamedfurry;

import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.client.Link;
import net.dv8tion.jda.api.entities.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

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
    private static final QueueManager queueManager = new QueueManager();
    public static QueueManager getQueueManager() {return queueManager;}
    static boolean playlisted = false;
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
        GuildVoiceState memberState = Objects.requireNonNull(message.getMember()).getVoiceState();
        HttpClient http = HttpClient.newHttpClient();
        if (messageArr.length == 1){
            message.getChannel().sendMessage("Вы не можете указать пустые аргументы. Добавьте название или ссылку на желаемую музыку.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            return;
        }
        if (!Objects.requireNonNull(memberState).inAudioChannel()){
            message.getChannel().sendMessage("Вам нужно находиться в голосовом канале!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            return;
        }
        try {
            if (!Objects.requireNonNull(selfState).inAudioChannel()){
                BotLauncher.connect(message);
            }
            String requestedSong = content.substring(6);
            if (!isURL(requestedSong)){
                requestedSong = "ytsearch:" + requestedSong + " music";
                requestedSong = requestedSong.replaceAll(" ", "-");
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
                JSONObject jsonObject = new JSONObject();
                if ("track".equals(loadType)) {
                    JSONObject data = json.getJSONObject("data");
                    encodedTrack = data.getString("encoded");
                    json = data.getJSONObject("info");
                } else if (json.has("tracks")) {
                    JSONArray tracks = json.getJSONArray("tracks");
                    jsonObject = tracks.getJSONObject(0);
                    encodedTrack = jsonObject.getString("track");
                    json = jsonObject.getJSONObject("info");
                } else if ("search".equals(loadType)) {
                    JSONArray searchResults = json.getJSONArray("data");
                    if (searchResults.isEmpty()){
                        message.getChannel().sendMessage("Поиск не дал результатов.").queue();
                        return;
                    }
                    JSONObject firstTrack = searchResults.getJSONObject(0);
                    encodedTrack = firstTrack.getString("encoded");
                    json = firstTrack.getJSONObject("info");
                } else {
                    message.getChannel().sendMessage("Ответ Lavalink не содержит треков.").queue();
                    return;
                }

                String title = json.optString("title", "Unknown");
                long guildID = message.getGuildIdLong();
                Link link = BotLauncher.getOrCreateLink(guildID);
                encodedTrack = encodedTrack.trim();
                encodedTrack = encodedTrack.replaceAll("\\s", "");
                encodedTrack = URLEncoder.encode(encodedTrack, StandardCharsets.UTF_8);
                logger.info("Encoded and cleared track: " + encodedTrack);
                link.getNode().decodeTrack(encodedTrack)
                        .flatMap(track -> {
                            QueueManager qm = getQueueManager();
                            qm.addToQueue(guildID, track);
                            var player = link.getPlayer().block();
                            if (Objects.requireNonNull(player).getTrack() == null){
                                Track next = qm.getNext(guildID);
                                if (next != null){
                                    return player.setTrack(next);
                                }
                            }

                            message.getChannel().sendMessage(
                                    "Добавлено в очередь: `" + title + "`" +
                                            (qm.hasNext(guildID) ? " (Очередь: " + qm.getQueueList(guildID).size() + ")" : "") +
                                            ".\n-# Запрошено: " + message.getAuthor().getName() + ", " + getTime()
                            ).queue();

                            playlisted = true;

                            return Mono.empty();
                        })
                        .doOnSuccess(p -> {
                            if (!playlisted){
                                try {
                                    message.getChannel().sendMessage("Сейчас играет: `" + title + "`.\nДля паузы отправьте команду `!pause`, для остановки произведения отправьте команду `!stop`, для перехода к следующей песне отправьте команду `!skip`.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                                } catch (Exception e) {
                                    logger.error("Error in success handler: {}", e.getMessage());
                                    message.getChannel().sendMessage("Произошла ошибка. Код ошибки: P-1.").queue();
                                    return;
                                }
                            }
                            playlisted = false;
                        })
                        .doOnError(err -> {
                            message.getChannel().sendMessage("Не получилось загрузить новую музыку.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                            logger.error("Error loading track: {}", err.getMessage());
                        })
                        .subscribe();
            } catch (Exception e) {
                logger.error("Caught an unexpected error while processing Lavalink music request! Error code: M-1: {}", e.getMessage());
                message.getChannel().sendMessage("Произошла ошибка при обработке команды! Код ошибки: M-1.").queue();
            }
        } catch (Exception e) {
            logger.error("Caught an unexpected error while processing User music request! Error code: M-2: {}", e.getMessage());
            message.getChannel().sendMessage("Произошла ошибка при обработке команды! Код ошибки: M-2.").queue();
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
        GuildVoiceState memberState = Objects.requireNonNull(message.getMember()).getVoiceState();
        long guildID = message.getGuildIdLong();
        Optional<Link> maybeLink = Optional.ofNullable(BotLauncher.getLinkIfCashed(guildID));
        if (!Objects.requireNonNull(memberState).inAudioChannel()){
            message.getChannel().sendMessage("Вам нужно находиться в голосовом канале!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            return;
        }
        if (maybeLink.isEmpty()){
            message.getChannel().sendMessage("Мне нужно находиться в голосовом канале для этого!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            return;
        }
        try {
            if (Objects.equals(memberState.getChannel(), Objects.requireNonNull(selfState).getChannel())){
                Link link = maybeLink.get();
                Objects.requireNonNull(link.getPlayer().block()).stopTrack().subscribe();
                link.getNode().destroyPlayerAndLink(guildID).subscribe();
                QueueManager qm = getQueueManager();
                qm.clear(guildID);
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
        Objects.requireNonNull(link.getPlayer().block()).setPaused(!Objects.requireNonNull(link.getPlayer().block()).getPaused()).subscribe(p -> {
            boolean paused = Objects.requireNonNull(link.getPlayer().block()).getPaused();
            if (paused) {
                message.getChannel().sendMessage("Трек приостановлен.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            } else {
                message.getChannel().sendMessage("Трек возобновлен.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        }, err -> {
            message.getChannel().sendMessage("Не удалось переключить паузу.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            logger.warn("Can't skip user's track!: {}", err.getMessage());
        });
    }

    public void queue(Message message){
        long guildId = message.getGuildIdLong();
        QueueManager qm = getQueueManager();
        List<Track> q = qm.getQueueList(guildId);
        Track current = qm.getCurrent(guildId);

        if (q.isEmpty() && current == null){
            message.getChannel().sendMessage("Очередь пуста.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            return;
        }

        StringBuilder sb = new StringBuilder("**Текущий: **" + (current != null ? current.getInfo().getTitle() : "Нет") + "\n**Очередь (" + q.size() + "):**\n");
        for (int i=0; i<Math.min(10, q.size()); i++){
            sb.append((i+1)).append(". ").append(q.get(i).getInfo().getTitle()).append("\n");
        }
        message.getChannel().sendMessage(sb.toString()).queue();
    }

    public void skip(Message message){
        if (queueManager.hasNext(message.getGuildIdLong())){
            long guildId = message.getGuildIdLong();
            Link link = BotLauncher.getOrCreateLink(guildId);
            Objects.requireNonNull(link.getPlayer().block()).stopTrack().subscribe();
            getQueueManager().skip(guildId);
            message.getChannel().sendMessage("Трек пропущен").queue();
        } else {
            message.getChannel().sendMessage("Очередь пуста.").queue();
        }
    }

    public void clear(Message message){
        long guildId = message.getGuildIdLong();
        getQueueManager().clear(guildId);
        message.getChannel().sendMessage("Очередь очищена.").queue();
    }
}

class QueueManager{
    private final Map<Long, Deque<Track>> queues = new HashMap<>();
    private final Map<Long, Track> currentTracks = new HashMap<>();

    public void addToQueue(long guildId, Track track){
        queues.computeIfAbsent(guildId, k -> new ArrayDeque<>()).add(track);
    }

    public Track getNext(long guildId){
        Track next = queues.get(guildId).pollFirst();
        if (next != null){
            currentTracks.put(guildId, next);
        }
        return next;
    }

    public boolean hasNext(long guildId){
        return queues.containsKey(guildId) && !queues.get(guildId).isEmpty();
    }

    public void setCurrent(long guildId, Track track){
        currentTracks.put(guildId, track);
    }

    public Track getCurrent(long guildId){
        return currentTracks.get(guildId);
    }

    public void clear(long guildId){
        queues.remove(guildId);
        currentTracks.remove(guildId);
    }

    public List<Track> getQueueList(long guildId){
        return new ArrayList<>(queues.getOrDefault(guildId, new ArrayDeque<>()));
    }

    public void skip(long guildId){
        currentTracks.remove(guildId);
    }
}