package org.unnamedfurry;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class SlashCommands extends ListenerAdapter {
    final static Logger logger = LoggerFactory.getLogger(SlashCommands.class);
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }

    public void commandRegistration(Message message){
        Guild guild = message.getGuild();
        guild.updateCommands().addCommands(Commands.slash("help", "Выводит список доступных команд.")).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("help")){
            event.reply(HelpCommand(event)).queue();
        }
    }

    public String HelpCommand(SlashCommandInteractionEvent event){
        try {
            /*Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            Path filePath = jarDir.resolve("help-menu.txt");*/
            Path filePath = Path.of("help-menu.txt");

            String aboutText = Files.readString(filePath);
            return aboutText + "\n-# Запрошено пользователем: " + event.getUser().getName() + ", " + getTime();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

class SlashVerification{
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }
    final static Logger logger = LoggerFactory.getLogger(SlashVerification.class);
    public boolean allowedExecAdminCommands(Message message, MessageChannel channel){
        boolean bypassedVerification = false;
        Member member = message.getMember();
        try {
            if (member.getId().equals("897054945889644564") || member.hasPermission(Permission.ADMINISTRATOR) || checkRoles(message)){
                bypassedVerification = true;
            } else {
                channel.sendMessage("<@" + member.getId() + "> , у вас нет прав для выполнения этого действия! Проверьте наличие обязательных прав для выполнения или обратитесь к администратору/овнеру сервера.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                logger.info("Запрошена команда (" + message.getContentRaw() + ") участником (" + message.getAuthor() + ") без следующих прав: Администратор, Овнер или Создатель бота.");
            }
        } catch (Exception e) {
            channel.sendMessage("Произошла неивзестная ошибка при обработке команды. Обратитесь к создателю бота @unnamed_furry.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            throw new RuntimeException(e);
        }
        return bypassedVerification;
    }
    public static boolean checkRoles(Message message) throws Exception {
        /*Path jarDir = Paths.get(
                BotLauncher.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParent();
        File file = new File(String.valueOf(jarDir.resolve("whitelistedRoles.json")));*/
        File file = new File("whitelistedRoles.json");

        if (file.exists()){
            String json = Files.readString(file.toPath());
            Guild guild = message.getGuild();;
            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray(guild.getId());
            String[] existingRolesArr = new String[array.length()];
            for (int i=0; i<array.length(); i++){
                existingRolesArr[i] = array.getString(i);
            }
            Member member = message.getMember();
            assert member != null;
            List<Role> roles = member.getRoles();
            List<String> roleIdsAsString = roles.stream()
                    .map(Role::getId)
                    .toList();

            return Arrays.stream(existingRolesArr).anyMatch(roleIdsAsString::contains);
        } else {
            return false;
        }
    }
}