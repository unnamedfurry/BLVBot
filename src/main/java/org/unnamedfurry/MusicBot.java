package org.unnamedfurry;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
        if (messageArr.length == 1) message.getChannel().sendMessage("Вы не можете указать пустые аргументы. Добавьте название или ссылку на желаемую музыку.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        if (!memberState.inAudioChannel()) message.getChannel().sendMessage("Вам нужно находиться в голосовом канале!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        try {
            if (!selfState.inAudioChannel()){
                final AudioManager audioManager = message.getGuild().getAudioManager();
                final VoiceChannel memberChannel = (VoiceChannel) memberState.getChannel();
                audioManager.openAudioConnection(memberChannel);
            }
            String link = content.substring(6);
            if (!isURL(link)){
                link = "ytsearch:" + String.join(" ", content.substring(6) + " music");
            }
            PlayerManager.getInstance().loadAndPlay(memberState.getChannel().asVoiceChannel(), message, link);
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
        if (!memberState.inAudioChannel()) message.getChannel().sendMessage("Вам нужно находиться в голосовом канале!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        if (!selfState.inAudioChannel()) message.getChannel().sendMessage("Мне нужно находиться в голосовом канале для этого!\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        try {
            if (Objects.equals(memberState.getChannel(), selfState.getChannel())){
                PlayerManager.getInstance().getMusicManager(message.getGuild()).scheduler.player.stopTrack();
                PlayerManager.getInstance().getMusicManager(message.getGuild()).scheduler.queue.clear();
                message.getGuild().getAudioManager().closeAudioConnection();
                message.getChannel().sendMessage("Проигрывание было завершено и очередь была очищена.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        } catch (Exception e) {
            message.getChannel().sendMessage("Произошла ошибка при обработке команды!").queue();
            logger.error(e.toString());
        }
    }

    public void pause(Message message){
        GuildMusicManager manager = PlayerManager.getInstance().getMusicManager(message.getGuild());
        if (manager.scheduler.player.getPlayingTrack() == null) message.getChannel().sendMessage("В данный момент никакая музыка не воспроизводится.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        manager.scheduler.player.setPaused(!manager.scheduler.player.isPaused());
        if (manager.scheduler.player.isPaused()){
            message.getChannel().sendMessage("Трек `" + manager.scheduler.player.getPlayingTrack().getInfo().title + "` приостановлен.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        } else {
            message.getChannel().sendMessage("Трек `" + manager.scheduler.player.getPlayingTrack().getInfo().title + "` возобновлен.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        }
    }
}

class PlayerManager {
    private static PlayerManager INSTANCE;
    private final Map<Long, GuildMusicManager> musicManagers;
    private final AudioPlayerManager audioPlayerManager;
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }

    public PlayerManager(){
        this.musicManagers = new HashMap<>();
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        YoutubeAudioSourceManager yt = new YoutubeAudioSourceManager();
        this.audioPlayerManager.registerSourceManager(yt);
    }

    public GuildMusicManager getMusicManager(Guild guild){
        return this.musicManagers.computeIfAbsent(guild.getIdLong(), (guildId) -> {
            final GuildMusicManager guildMusicManager = new GuildMusicManager(this.audioPlayerManager);
            guild.getAudioManager().setSendingHandler(guildMusicManager.getSendHandler());
            return guildMusicManager;
        });
    }

    public void loadAndPlay(VoiceChannel channel, Message data, String trackURL){
        final GuildMusicManager musicManager = this.getMusicManager(channel.getGuild());
        this.audioPlayerManager.loadItemOrdered(musicManager, trackURL, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack){
                musicManager.scheduler.queue(audioTrack);
                int seconds = Math.toIntExact(audioTrack.getInfo().length / 1000);
                int minutes = Math.toIntExact(audioTrack.getInfo().length / 60000);
                int hours = Math.toIntExact(audioTrack.getInfo().length / 36000000);
                String timeMsg = hours + ":" + minutes + ":" + seconds;
                data.getChannel().sendMessage("Сейчас играет: `" + audioTrack.getInfo().title + "`, автор: `" + audioTrack.getInfo().author + "`, длительность: `" + timeMsg + "`.\nДля паузы отправьте команду `!pause`, для остановки произведения отправьте команду `!stop`, для перехода к следующей песне отправьте команду `!skip`.\n-# Запрошено пользователем: " + data.getAuthor().getName() + ", " + getTime()).queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist){
                final List<AudioTrack> tracks = playlist.getTracks();
                if (!tracks.isEmpty()){
                    AudioTrack audioTrack = tracks.get(0);
                    musicManager.scheduler.queue(audioTrack);
                    int seconds = Math.toIntExact(audioTrack.getInfo().length / 1000);
                    int minutes = Math.toIntExact(audioTrack.getInfo().length / 60000);
                    int hours = Math.toIntExact(audioTrack.getInfo().length / 36000000);
                    String timeMsg = hours + ":" + minutes + ":" + seconds;
                    data.getChannel().sendMessage("Сейчас играет: `" + audioTrack.getInfo().title + "`, автор: `" + audioTrack.getInfo().author + "`, длительность: `" + timeMsg + "`.\nДля паузы отправьте команду `!pause`, для остановки произведения отправьте команду `!stop`, для перехода к следующей песне отправьте команду `!skip`.\n-# Запрошено пользователем: " + data.getAuthor().getName() + ", " + getTime()).queue();
                }
            }

            @Override
            public void noMatches(){
                data.getChannel().sendMessage("Не получилось найти запрашиваемый трек.\n-# Запрошено пользователем: " + data.getAuthor().getName() + ", " + getTime()).queue();
            }

            @Override
            public void loadFailed(FriendlyException e){
                data.getChannel().sendMessage("Не получилось загрузить новую музыку.\n-# Запрошено пользователем: " + data.getAuthor().getName() + ", " + getTime()).queue();
            }
        });
    }

    public static PlayerManager getInstance(){
        if (INSTANCE == null){
            INSTANCE = new PlayerManager();
        }
        return INSTANCE;
    }
}

class GuildMusicManager{
    public final AudioPlayer audioPlayer;
    public final TrackScheduler scheduler;
    private final AudioPlayerSendHandler sendHandler;

    public GuildMusicManager(AudioPlayerManager manager){
        this.audioPlayer = manager.createPlayer();
        this.scheduler = new TrackScheduler(this.audioPlayer);
        this.audioPlayer.addListener(this.scheduler);
        this.sendHandler = new AudioPlayerSendHandler(this.audioPlayer);
    }

    public AudioPlayerSendHandler getSendHandler(){
        return sendHandler;
    }
}

class TrackScheduler extends AudioEventAdapter{
    public final AudioPlayer player;
    public final BlockingQueue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player){
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
    }

    public void queue(AudioTrack track){
        if (!this.player.startTrack(track, true)){
            this.queue.add(track);
        }
    }

    public void nextTrack(){
        this.player.startTrack(this.queue.poll(), false);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason){
        if (endReason.mayStartNext){
            nextTrack();
        }
    }
}

class AudioPlayerSendHandler implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final ByteBuffer buffer;
    private MutableAudioFrame frame;

    public AudioPlayerSendHandler(AudioPlayer audioPlayer){
        this.audioPlayer = audioPlayer;
        this.buffer = ByteBuffer.allocate(1024);
        this.frame = new MutableAudioFrame();
        frame.setBuffer(buffer);
    }

    @Override
    public boolean canProvide() {
        return this.audioPlayer.provide(this.frame);
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        final Buffer tmp = ((Buffer) this.buffer).flip();
        return (ByteBuffer) tmp;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}