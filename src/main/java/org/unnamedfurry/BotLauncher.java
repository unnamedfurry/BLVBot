package org.unnamedfurry;

import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.client.event.EmittedEvent;
import dev.arbjerg.lavalink.client.event.StatsEvent;
import dev.arbjerg.lavalink.client.event.TrackEndEvent;
import dev.arbjerg.lavalink.client.event.TrackStartEvent;
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import dev.arbjerg.lavalink.client.player.Track;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class BotLauncher extends ListenerAdapter {
    private static JDA bot;
    final static Logger logger = LoggerFactory.getLogger(BotLauncher.class);

    public static String lavalinkPassword() {
        String password = "";
        try {
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            password = Files.readString(jarDir.resolve("lavalink_password.txt")).trim();
            //password = Files.readString(Path.of("lavalink_password.txt")).trim();
        } catch (Exception e) {
            logger.error("Caught an error while parsing lavalink password!: {}", e.getMessage());
        }
        return password;
    }

    private static String botToken() {
        String token = "";
        try {
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            token = Files.readString(jarDir.resolve("bot_token.txt")).trim();
            //token = Files.readString(Path.of("bot_token.txt")).trim();
        } catch (Exception e) {
            logger.error("Caught an error while parsing bot token!: {}", e.getMessage());
        }
        return token;
    }

    public static final LavalinkClient client = new LavalinkClient(Helpers.getUserIdFromToken(botToken()));

    public static void main(String[] args) throws Exception {
        client.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());
        registerLavalinkListeners(client);
        registerLavalinkNodes(client);
        bot = JDABuilder.createDefault(botToken()).addEventListeners(new EventListener())
                .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.AUTO_MODERATION_CONFIGURATION, GatewayIntent.AUTO_MODERATION_EXECUTION, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_TYPING, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_WEBHOOKS)
                .setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(client)).build().awaitReady();
    }

    public boolean presentInChannel(Guild guild){
        if (bot.getGuilds().contains(guild)){
            return true;
        } else {
            return false;
        }
    }

    public MessageChannel getNewMessageChannel(String channelId){
        return bot.getTextChannelById(channelId);
    }

    public TextChannel getTextChannel(String channelId){
        return bot.getTextChannelById(channelId);
    }

    static int oldBitrate = 0;

    public static void connect(Message message) {
        try {
            VoiceChannel memberChannel = (VoiceChannel) Objects.requireNonNull(Objects.requireNonNull(message.getMember()).getVoiceState()).getChannel();
            bot.getDirectAudioController().connect(Objects.requireNonNull(memberChannel));
            oldBitrate = memberChannel.getBitrate();
            logger.info("Old channel's bitrate: {}", oldBitrate);
            memberChannel.getManager().setBitrate(96000).queue();
        } catch (Exception e) {
            logger.error("Caught an error while connecting to the channel!: {}", e.getMessage());
            message.getChannel().sendMessage("Возникла непредвиденная ошибка во время подключения к войсу.").queue();
        }
    }

    public static void disconnect(Message message) {
        try {
            VoiceChannel memberChannel = (VoiceChannel) Objects.requireNonNull(Objects.requireNonNull(message.getMember()).getVoiceState()).getChannel();
            Objects.requireNonNull(memberChannel).getManager().setBitrate(oldBitrate).queue();
            bot.getDirectAudioController().disconnect(message.getGuild());
        } catch (Exception e) {
            logger.error("Caught an error while leaving to the channel!: {}", e.getMessage());
            message.getChannel().sendMessage("Возникла непредвиденная ошибка во время отключения от войса.").queue();
        }
    }

    public static Link getOrCreateLink(long guildId) {
        return client.getOrCreateLink(guildId);
    }

    public static Link getLinkIfCashed(long guildId) {
        return client.getLinkIfCached(guildId);
    }

    private static void registerLavalinkNodes(LavalinkClient client) {
        NodeOptions optionsRemote = new NodeOptions.Builder()
                .setName("remote-node")
                .setServerUri("ws://127.0.0.1:2333")
                .setPassword(lavalinkPassword().trim())
                .setRegionFilter(RegionGroup.EUROPE)
                .build();
        List.of(client.addNode(optionsRemote)).forEach((node) -> {
            node.on(TrackStartEvent.class).subscribe((event) -> {
                final LavalinkNode node1 = event.getNode();

                logger.info(
                        "{}: track started: {}",
                        node1.getName(),
                        event.getTrack().getInfo()
                );
            });
        });
    }

    private static void registerLavalinkListeners(LavalinkClient client) {
        client.on(dev.arbjerg.lavalink.client.event.ReadyEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();

            logger.info(
                    "Node '{}' is ready, session id is '{}'!",
                    node.getName(),
                    client.getNodes().getFirst().getSessionId()
            );
        });

        /*client.on(StatsEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();

            logger.info(
                    "Node '{}' has stats, current players: {}/{} (link count {})",
                    node.getName(),
                    event.getPlayingPlayers(),
                    event.getPlayers(),
                    client.getLinks().size()
            );
        });*/

        client.on(EmittedEvent.class).subscribe((event) -> {
            if (event instanceof TrackStartEvent) {
                logger.info("Is a track start event!");
            }

            final var node = event.getNode();

            logger.info(
                    "Node '{}' emitted event: {}",
                    node.getName(),
                    event
            );
        });

        client.on(TrackEndEvent.class).subscribe((event) -> {
            long guildId = event.getGuildId();
            QueueManager qm = MusicBot.getQueueManager();
            Link link = client.getLinkIfCached(guildId);

            if (qm.hasNext(guildId)) {
                Track next = qm.getNext(guildId);
                Objects.requireNonNull(Objects.requireNonNull(link).getPlayer().block()).setTrack(next)
                        .doOnSuccess(p -> logger.info("Next track started: {}", next.getInfo().getTitle()))
                        .subscribe();
            } else {
                logger.info("Queue is empty for guild: {}", guildId);
            }
        });
    }
}