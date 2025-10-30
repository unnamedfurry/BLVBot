package org.unnamedfurry;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class BotLauncher {
    public static void main(String[] args) throws Exception{
        JDA bot = JDABuilder.createDefault("").addEventListeners(new EventListener()).enableIntents(GatewayIntent.MESSAGE_CONTENT).build().awaitReady();
    }
}