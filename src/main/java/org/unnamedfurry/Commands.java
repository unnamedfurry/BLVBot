package org.unnamedfurry;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Commands {
    final static Logger logger = LoggerFactory.getLogger(Commands.class);

    public void avatarCommand(String[] contentFormatted, MessageChannel channel, Message message){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/users/" + contentFormatted[1]))
                    .header("Authorization", "Bot ")
                    .header("Content-Type", "application/json")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JsonReader jsonReader = Json.createReader(new StringReader(responseBody));
                JsonObject jsonObject = jsonReader.readObject();
                String avatarHash = jsonObject.getString("avatar", null);
                if (avatarHash != null){
                    String avatarURL = String.format("https://cdn.discordapp.com/avatars/%s/%s.webp?size=128", contentFormatted[1], avatarHash);
                    HttpRequest avatarRequest = HttpRequest.newBuilder().uri(URI.create(avatarURL)).GET().build();
                    HttpResponse<byte[]> avatarResponse = client.send(avatarRequest, HttpResponse.BodyHandlers.ofByteArray());
                    if (avatarResponse.statusCode() == 200){
                        byte[] imageData = avatarResponse.body();
                        FileUpload upload = FileUpload.fromData(imageData, "avatar"+contentFormatted[1]+".png");
                        channel.sendMessage("Аватар пользвателя <@"+contentFormatted[1]+">: \n-# Запрошено пользователем: " + message.getAuthor().getName() + ", Дата: " + date + ", Время: " + time).addFiles(upload).queue();
                    }
                } else {
                    logger.warn("Хэш или Айди пользователя неверные! Неудалось отправить embed-сообщение!");
                    channel.sendMessage("Неудалось получить аватар запрашиваемого пользователя. Проверьте id аккаунта и при повторной ошибке свяжитесь с создателем (@unnamed_furry).").queue();
                }
            } else {
                logger.error("Bad response type.");
                logger.error(String.valueOf(response.statusCode()));
            }
        } catch (Exception e) {
            logger.error(e.toString());
            logger.error("Requested command: " + message.getContentRaw());
        }
    }

    public void banCommand(MessageChannel channel, Message message, String content){
        if (Verification.allowedExecAdminCommands(message, channel)){
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String date = LocalDate.now().format(dateFormatter);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String time = LocalTime.now().format(timeFormatter);
            String[] messageArr = content.split(" ");
            Guild guild = message.getGuild();
            UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
            if (messageArr.length == 2){
                guild.ban(snowflake, 7, TimeUnit.DAYS).reason(message.getAuthor().getName() + " не указал причину бана.").queue();
                channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно забанен " + message.getAuthor().getName() + " по причине: не указано\n-# Дата: " + date + ", Время: " + time).queue();
            } else if (messageArr.length == 3){
                guild.ban(snowflake, 7, TimeUnit.DAYS).reason(messageArr[2]).queue();
                channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно забанен " + message.getAuthor().getName() + " по причине: " + messageArr[2] + "\nДата: " + date + ", Время: " + time).queue();
            }
        }
    }

    public void unbanCommand(MessageChannel channel, Message message, String content){
        if (Verification.allowedExecAdminCommands(message, channel)){
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            String date = LocalDate.now().format(dateFormatter);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String time = LocalTime.now().format(timeFormatter);
            String[] messageArr = content.split(" ");
            Guild guild = message.getGuild();
            UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
            guild.unban(snowflake).queue();
            channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно разбанен " + message.getAuthor().getName() + ".\n-# Дата: " + date + ", Время: " + time).queue();
        }
    }

    public void HelpCommand (MessageChannel channel, Message message){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        channel.sendMessage("## Список доступных комманд (находится в стадии активной разработки, функционал будет постепенно добавляться):\n1. Команда help/usage -- отображает список доступных комманд и их использование. Использование: `!help` или `!usage`.\n2. Команда ping -- проверяет работу бота и задержку апи. Использование: `!ping`.\n3. Команда avatar -- позволяет посмотреть и скачать текущий аватар интересующего пользователя. Использование: `!avatar <айди_интересующего_юзера>`, например: `!avatar 897054945889644564`.\n-# Запрошено пользвателем: " + message.getAuthor().getName() + ", Дата: " + date + ", Время: " + time).queue();
    }
}

class Verification{
    final static Logger logger = LoggerFactory.getLogger(Verification.class);
    public static boolean allowedExecAdminCommands(Message message, MessageChannel channel){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        boolean bypassedVerification = false;
        Member member = message.getMember();
        try {
            if (member.getId().equals("897054945889644564") || member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner()){
                bypassedVerification = true;
            } else {
                channel.sendMessage("Ошибка выполнения команды: недостаточно прав! Проверьте наличие обязательных прав для выполнения или обратитесь к администратору/овнеру сервера.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", Дата: " + date + ", Время: " + time).queue();
                logger.info("Запрошена команда (" + message.getContentRaw() + ") участником (" + message.getAuthor() + ") без следующих прав: Администратор, Овнер или Создатель бота.");
            }
        } catch (Exception e) {
            channel.sendMessage("Произошла неивзестная ошибка при обработке команды. Обратитесь к создателю бота @unnamed_furry.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", Дата: " + date + ", Время: " + time).queue();
            throw new RuntimeException(e);
        }
        return bypassedVerification;
    }
}