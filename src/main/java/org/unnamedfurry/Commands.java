package org.unnamedfurry;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.entities.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Commands {
    final static Logger logger = LoggerFactory.getLogger(Commands.class);
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }

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
                        channel.sendMessage("Аватар пользвателя <@"+contentFormatted[1]+">: \n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).addFiles(upload).queue();
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
            String[] messageArr = content.split(" ");
            Guild guild = message.getGuild();
            UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
            if (messageArr.length == 2){
                guild.ban(snowflake, 7, TimeUnit.DAYS).reason(message.getAuthor().getName() + " не указал причину бана.").queue(
                        (v) -> channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно забанен " + message.getAuthor().getName() + " по причине: не указано\n-# " + getTime()).queue(),
                        (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось забанить: <@" + messageArr[1] + ">, причина: этого пользователя нельзя забанить.\n-# ").queue()
                );
            } else if (messageArr.length == 3){
                guild.ban(snowflake, 7, TimeUnit.DAYS).reason(messageArr[2]).queue(
                        (v) -> channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно забанен " + message.getAuthor().getName() + " по причине: " + messageArr[2] + "\n-# " + getTime()).queue(),
                        (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось забанить: <@" + messageArr[1] + ">, причина: этого пользователя нельзя забанить.\n-# " + getTime()).queue()
                );
            }
        }
    }

    public void unbanCommand(MessageChannel channel, Message message, String content){
        if (Verification.allowedExecAdminCommands(message, channel)){
            String[] messageArr = content.split(" ");
            Guild guild = message.getGuild();
            UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
            guild.unban(snowflake).queue();
            channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно разбанен " + message.getAuthor().getName() + ".\n-# " + getTime()).queue();
        }
    }

    public void whitelistRole(Message message, MessageChannel channel, String content){
        if (Verification.allowedExecAdminCommands(message, channel)){
            String[] contentArr = content.split(" ");
            if (contentArr.length == 3){
                try {
                    Path path = Path.of("src/main/resources/whitelistedRoles.txt");
                    List<String> lines = Files.readAllLines(path);

                    if (contentArr[1].equals("add")){
                        if (lines.stream().anyMatch(line -> line.contains(contentArr[2]))){
                            channel.sendMessage("Роль <@&" + contentArr[2] + "> уже присутствует в вайтлисте верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                        } else {
                            Files.write(path, (contentArr[2]+" ").getBytes(), StandardOpenOption.APPEND);
                            channel.sendMessage("Роль <@&" + contentArr[2] + "> успешно добавлена в вайтлист верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                        }
                    } else if (contentArr[1].equals("remove")){
                        if (lines.stream().noneMatch(line -> line.contains(contentArr[2]))){
                            channel.sendMessage("Роль <@&" + contentArr[2] + "> уже отсутствует в вайтлисте верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                        } else {
                            String allRoles = String.join(" ", lines).replace(contentArr[2] + " ", "");
                            Files.write(path, allRoles.getBytes());
                            channel.sendMessage("Роль <@&" + contentArr[2] + "> успешно удалена из вайтлиста верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                        }
                    } else {
                        channel.sendMessage("Неправильное использование команды! Проверьте синтаксис команды (правильный синтаксис: `!whitelistRole add/remove <role_id>`) и повторите попытку. Код ошибки: R-1\n-# Запрошено пользвателем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                    }
                } catch (Exception e){
                    logger.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            } else {
                channel.sendMessage("Неправильное использование команды! Проверьте синтаксис команды (правильный синтаксис: `!whitelistRole add/remove <role_id>`) и повторите попытку. Код ошибки: R-2\n-# Запрошено пользвателем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        }
    }

    public void clearMessages(MessageChannel channel, Message message, String content){
        if (Verification.allowedExecAdminCommands(message, channel)){
            String[] contentArr = content.split(" ");
            int length = Integer.parseInt(contentArr[1]);
            if (length<=49){
                channel.getIterableHistory().takeAsync(length+1).thenAccept(messages -> {
                    channel.purgeMessages(messages);
                    channel.sendMessage("Успешно очищено " + length + " сообщений.").queue(msg -> {msg.delete().queueAfter(3, TimeUnit.SECONDS);});
                }).exceptionally(error -> {logger.error("Произошла ошибка при удалении сообщений."); error.printStackTrace(); channel.sendMessage("Произошла ошибка при удалении сообщений.").queue(); return null;});
            } else {
                channel.sendMessage("<@" + message.getAuthor().getId() + ">, вы не можете удалить больше 50 сообщений за раз.\n-# Запрошено пользователем: " + message.getAuthor().getName() + getTime()).queue();
            }
        }
    }

    public void HelpCommand (MessageChannel channel, Message message){
        try {
            Path filePath = Path.of("src/main/resources/help-menu.txt");
            String aboutText = Files.readString(filePath);
            channel.sendMessage(aboutText + "\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

class Verification{
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }
    final static Logger logger = LoggerFactory.getLogger(Verification.class);
    public static boolean allowedExecAdminCommands(Message message, MessageChannel channel){
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
        File file = new File("src/main/resources/whitelistedRoles.txt");
        if (file.exists()){
            FileReader reader = new FileReader(file);
            StringBuilder existingRoles = new StringBuilder();
            int a = reader.read();
            while (a != -1){
                String b = String.valueOf((char) a);
                existingRoles.append(b);
                a = reader.read();
            }
            String[] existingRolesBuf = existingRoles.toString().split(" ");
            Member member = message.getMember();
            List<Role> roles = member.getRoles();
            List<String> roleIdsAsString = roles.stream()
                    .map(Role::getId)
                    .toList();

            return Arrays.stream(existingRolesBuf).anyMatch(roleIdsAsString::contains);
        } else {
            return false;
        }
    }
}