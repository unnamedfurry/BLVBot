package org.unnamedfurry;

import dev.arbjerg.lavalink.client.*;
import dev.arbjerg.lavalink.client.event.EmittedEvent;
import dev.arbjerg.lavalink.client.event.StatsEvent;
import dev.arbjerg.lavalink.client.event.TrackStartEvent;
import dev.arbjerg.lavalink.client.loadbalancing.RegionGroup;
import dev.arbjerg.lavalink.client.loadbalancing.builtin.VoiceRegionPenaltyProvider;
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BotLauncher extends ListenerAdapter {
    private static JDA bot;
    final static Logger LOG = LoggerFactory.getLogger(BotLauncher.class);
    public static String lavalinkPassword(){
        String password = "";
        try {
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            password = Files.readString(jarDir.resolve("lavalink_password.txt")).trim();
            //String password = Files.readString(Path.of("lavalink_password.txt")).trim();
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }
        return password;
    }

    private static String botToken(){
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
        } catch (Exception e){
            e.getMessage();
            e.printStackTrace();
        }
        return token;
    }

    public static final LavalinkClient client = new LavalinkClient(Helpers.getUserIdFromToken(botToken()));
    public static void main(String[] args) throws Exception{
        client.getLoadBalancer().addPenaltyProvider(new VoiceRegionPenaltyProvider());
        registerLavalinkListeners(client);
        registerLavalinkNodes(client);
        bot = JDABuilder.createDefault(botToken()).addEventListeners(new EventListener()).addEventListeners(new SlashCommands()).enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.AUTO_MODERATION_CONFIGURATION, GatewayIntent.AUTO_MODERATION_EXECUTION, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_TYPING, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_WEBHOOKS).setVoiceDispatchInterceptor(new JDAVoiceUpdateListener(client)).build().awaitReady();
    }

    private static int oldBitrate = 0;

    public static void connect(Message message){
        VoiceChannel memberChannel = (VoiceChannel) message.getMember().getVoiceState().getChannel();
        bot.getDirectAudioController().connect(memberChannel);
        oldBitrate = memberChannel.getBitrate();
        memberChannel.getManager().setBitrate(96);
    }

    public static void disconnect(Message message){
        VoiceChannel memberChannel = (VoiceChannel) message.getMember().getVoiceState().getChannel();
        memberChannel.getManager().setBitrate(oldBitrate);
        bot.getDirectAudioController().disconnect(message.getGuild());
    }

    public static Link getOrCreateLink(long guildId){
        return client.getOrCreateLink(guildId);
    }

    public static Link getLinkIfCashed(long guildId){
        return client.getLinkIfCached(guildId);
    }

    private static void registerLavalinkNodes(LavalinkClient client) {
        NodeOptions optionsRemote = new NodeOptions.Builder()
                .setName("remote-node")
                .setServerUri("ws://")
                .setPassword(lavalinkPassword().trim())
                .setRegionFilter(RegionGroup.EUROPE)
                .build();
        List.of(client.addNode(optionsRemote)).forEach((node) -> {
            node.on(TrackStartEvent.class).subscribe((event) -> {
                final LavalinkNode node1 = event.getNode();

                LOG.info(
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

            LOG.info(
                    "Node '{}' is ready, session id is '{}'!",
                    node.getName(),
                    client.getNodes().get(0).getSessionId()
            );
        });

        client.on(StatsEvent.class).subscribe((event) -> {
            final LavalinkNode node = event.getNode();

            LOG.info(
                    "Node '{}' has stats, current players: {}/{} (link count {})",
                    node.getName(),
                    event.getPlayingPlayers(),
                    event.getPlayers(),
                    client.getLinks().size()
            );
        });

        client.on(EmittedEvent.class).subscribe((event) -> {
            if (event instanceof TrackStartEvent) {
                LOG.info("Is a track start event!");
            }

            final var node = event.getNode();

            LOG.info(
                    "Node '{}' emitted event: {}",
                    node.getName(),
                    event
            );
        });
    }

    @Override
    public void onReady(@NotNull ReadyEvent event){
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        MessageChannel channel = bot.getTextChannelById("1433490601487368192");
        assert channel != null;
        channel.sendMessage("Бот включился.\n-# Время: " + time).queue();
        System.out.println("Finished loading!");
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event){
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        MessageChannel channel = bot.getTextChannelById("1433490601487368192");
        assert channel != null;
        channel.sendMessage("Бот выключился.\n-# Время: " + time).queue();
        System.out.println("Shutting down!");
    }
}