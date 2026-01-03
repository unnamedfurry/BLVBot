package org.unnamedfurry;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EmbedBot {
    private static final Logger log = LoggerFactory.getLogger(EmbedBot.class);
    BotLauncher bot = new BotLauncher();

    public void saveEmbed(SlashCommandInteractionEvent event, String templateName, User user, String mainLargeText, String mainDefaultText, Message.Attachment mainFile, String embedBotName, String embedLargeText, String embedDefaultText, Message.Attachment embedFile){
        JSONArray array = new JSONArray();

        if (!mainLargeText.isBlank() && !mainDefaultText.isBlank()){
            mainDefaultText = "# " + mainLargeText + "\n" + mainDefaultText;
            array.put(mainDefaultText);
        } else if (!mainLargeText.isBlank()) {
            mainDefaultText = mainLargeText;
            array.put(mainDefaultText);
        } else if (!mainDefaultText.isBlank()) {
            array.put(mainDefaultText);
        } else {
            array.put("null");
        }

        if (mainFile != null){
            try {
                byte[] data = mainFile.getProxy().download().get().readAllBytes();
                String encodedMainFile = Base64.getEncoder().encodeToString(data);
                array.put(encodedMainFile);
                array.put(mainFile.getFileName());
            } catch (Exception e) {
                log.error("Caught an unexpected error while downloading mainFile file: {}", e.getMessage());
            }
        } else {
            array.put("null");
            array.put("null");
        }

        if (!embedBotName.isBlank()){
            array.put(embedBotName);
        } else {
            array.put("null");
        }

        if (!embedLargeText.isBlank()){
            array.put(embedLargeText);
        } else {
            array.put("null");
        }

        if (!embedDefaultText.isBlank()){
            array.put(embedDefaultText);
        } else {
            array.put("null");
        }

        if (embedFile != null){
            try {
                String url = embedFile.getUrl();
                array.put(url);
            } catch (Exception e) {
                log.error("Caught an unexpected error while downloading embedFile file: {}", e.getMessage());
            }
        } else {
            array.put("null");
            array.put("null");
        }

        try {
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            Path path = jarDir.resolve("embedTemplates.json");
            //Path path = Path.of("embedTemplates.json");

            templateName = user.getId()+templateName;
            String content = Files.readString(path);
            JSONObject json = new JSONObject(content);
            if (json.has(templateName)){
                event.reply("Шаблон с таким названием уже существует.").setEphemeral(true).queue();
                return;
            }

            json.put(templateName, array);
            FileWriter writer = new FileWriter(path.toFile());
            json.write(writer);
            writer.close();
            event.reply("Шаблон успешно сохранен.").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("Caught an unexpected error while saving embed: \n {} \n {}", e.getMessage(), e.getStackTrace());
        }
    }

    public void sendEmbed(SlashCommandInteractionEvent event, MessageChannel channel, String templateName, String channelId, User user){
        try {
            if (!channelId.isBlank()){
                channel = bot.getNewMessageChannel(channelId);
            }
            templateName = user.getId()+templateName;
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            Path path = jarDir.resolve("embedTemplates.json");
            //Path path = Path.of("embedTemplates.json");

            String content = Files.readString(path);
            JSONObject object = new JSONObject(content);

            if (!object.has(templateName)){
                event.reply("Шаблона с таким названием не существует.").setEphemeral(true).queue();
                return;
            }

            JSONArray array = object.getJSONArray(templateName);

            int idx = 0;
            String mainDefaultText = array.optString(idx++);
            String encodedMainFile = array.optString(idx++);
            String mainFileFilename = array.optString(idx++);
            String embedBotName = array.optString(idx++);
            String embedLargeText = array.optString(idx++);
            String embedDefaultText = array.optString(idx++);
            String embedFileUrl = array.optString(idx);

            EmbedBuilder embedBuilder = new EmbedBuilder();
            if (!embedBotName.equals("null")) embedBuilder.setAuthor(embedBotName);
            if (!embedLargeText.equals("null")) embedBuilder.setTitle(embedLargeText);
            if (!embedDefaultText.equals("null")) embedBuilder.setDescription(embedDefaultText);
            if (!embedFileUrl.equals("null")) embedBuilder.setImage(embedFileUrl);


            MessageCreateBuilder messageBuilder = new MessageCreateBuilder();
            if (!mainDefaultText.equals("null")) messageBuilder.setContent(mainDefaultText);
            if (!encodedMainFile.equals("null") && !mainFileFilename.equals("null")){
                byte[] mainFile = Base64.getDecoder().decode(encodedMainFile);
                FileUpload upload = FileUpload.fromData(mainFile, mainFileFilename);
                messageBuilder.setFiles(upload);
            }

            if (!embedBuilder.isEmpty()){
                MessageEmbed embed = embedBuilder.build();
                messageBuilder.addEmbeds(embed);
            }
            MessageCreateData message = messageBuilder.build();

            channel.sendMessage(message).queue();
            event.reply("Успешно.").setEphemeral(true).queue();
        } catch (Exception e){
            log.error("Caught an unexpected error while sending embed message: \n {} \n {}", e.getMessage(), e.getStackTrace());
            event.reply("Не удалось отправить ваше embed-сообщение.").setEphemeral(true).queue();
        }
    }

    public void deleteSentEmbed(SlashCommandInteractionEvent event, String templateName){
        try{
            templateName = event.getUser().getId()+templateName;
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            Path path = jarDir.resolve("embedTemplates.json");
            //Path path = Path.of("embedTemplates.json");

            String content = Files.readString(path);
            JSONObject object = new JSONObject(content);

            if (!object.has(templateName)){
                event.reply("Шаблона с таким названием не существует.").setEphemeral(true).queue();
                return;
            }

            object.remove(templateName);
            FileWriter writer = new FileWriter(path.toFile());
            object.write(writer);
            writer.close();
            event.reply("Шаблон успешно удален.").setEphemeral(true).queue();
        } catch (Exception e) {
            log.error("Caught an unexpected error while deleting embed message: \n {} \n {}", e.getMessage(), e.getStackTrace());
            event.reply("Не удалось удалить ваше embed-шаблон.").setEphemeral(true).queue();
        }
    }
}

class TicketVerification{
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }
    final static Logger logger = LoggerFactory.getLogger(TextVerification.class);
    public static boolean allowedExecAdminCommands(Message message, MessageChannel channel){
        boolean bypassedVerification = false;
        Member member = message.getMember();
        try {
            if (Objects.requireNonNull(member).getId().equals("897054945889644564") || member.hasPermission(Permission.ADMINISTRATOR) || checkRoles(message)){
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
        Path jarDir = Paths.get(
                BotLauncher.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        ).getParent();
        File file = new File(String.valueOf(jarDir.resolve("whitelistedRoles.json")));
        //File file = new File("whitelistedRoles.json");

        if (file.exists()){
            String json = Files.readString(file.toPath());
            Guild guild = message.getGuild();
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