package org.unnamedfurry;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.SplitUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TextCommands {
    final static Logger logger = LoggerFactory.getLogger(TextCommands.class);
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "Дата: " + date + ", Время: " + time;
    }

    public void avatarCommand(String[] contentFormatted, MessageChannel channel, Message message){
        try {
            Path jarDir = Paths.get(
                BotLauncher.class.getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
                ).getParent();
            String token = Files.readString(jarDir.resolve("bot_token.txt")).trim();
            //String token = Files.readString(Path.of("bot_token.txt")).trim();

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://discord.com/api/v10/users/" + contentFormatted[1]))
                    .header("Authorization", "Bot " + token)
                    .header("Content-Type", "application/json")
                    .GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String avatarURL = "";
            if (response.statusCode() == 200) {
                String responseBody = response.body();
                JSONObject object = new JSONObject(responseBody);
                String avatarHash = object.optString("avatar", null);
                if (avatarHash != null){
                    avatarURL = String.format("https://cdn.discordapp.com/avatars/%s/%s.webp?size=1024", contentFormatted[1], avatarHash);
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
                logger.error("Requested command: " + message.getContentRaw() + ", Generated Link: " + avatarURL);
            }
        } catch (Exception e) {
            logger.error("Error while processing avatar command: {}", e.getMessage());
            logger.error("Requested command: {}", message.getContentRaw());
        }
    }

    public void banCommand(MessageChannel channel, Message message, String content){
        if (TextVerification.allowedExecAdminCommands(message, channel)){
            String[] messageArr = content.split(" ");
            Guild guild = message.getGuild();
            UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
            if (messageArr.length == 2){
                guild.ban(snowflake, 7, TimeUnit.DAYS).reason(message.getAuthor().getName() + " не указал причину бана.").queue(
                        (v) -> channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно забанен " + message.getAuthor().getName() + " по причине: не указано\n-# " + getTime()).queue(),
                        (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось забанить: <@" + messageArr[1] + ">, причина: этого пользователя нельзя забанить либо же он уже забанен.\n-# ").queue()
                );
            } else if (messageArr.length == 3){
                guild.ban(snowflake, 7, TimeUnit.DAYS).reason(messageArr[2]).queue(
                        (v) -> channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно забанен " + message.getAuthor().getName() + " по причине: " + messageArr[2] + "\n-# " + getTime()).queue(),
                        (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось забанить: <@" + messageArr[1] + ">, причина: этого пользователя нельзя забанить либо же он уже забанен.\n-# " + getTime()).queue()
                );
            } else {
                channel.sendMessage("Неправильное использование команды! Проверьте синтаксис команды через !help или !usage.").queue();
            }
        }
    }

    public void unbanCommand(MessageChannel channel, Message message, String content){
        if (TextVerification.allowedExecAdminCommands(message, channel)){
            String[] messageArr = content.split(" ");
            if (messageArr.length == 2){
                Guild guild = message.getGuild();
                UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
                guild.unban(snowflake).queue();
                channel.sendMessage("Пользователь <@" + snowflake.getId() + "> был успешно разбанен " + message.getAuthor().getName() + ".\n-# " + getTime()).queue();
            } else {
                channel.sendMessage("Неправильное использование команды! Проверьте синтаксис команды через !help или !usage.").queue();
            }
        }
    }

    public void whitelistRole(Message message, MessageChannel channel, String content){
        if (TextVerification.allowedExecAdminCommands(message, channel)){
            String[] contentArr = content.split(" ");
            if (contentArr.length == 3){
                try {
                    Path jarDir = Paths.get(
                            BotLauncher.class.getProtectionDomain()
                                    .getCodeSource()
                                    .getLocation()
                                    .toURI()
                    ).getParent();
                    Path path = jarDir.resolve("whitelistedRoles.json");
                    //Path path = Path.of("whitelistedRoles.json");

                    String json = Files.readString(path);
                    JSONObject object = new JSONObject(json);
                    Guild guild = message.getGuild();
                    if (contentArr[1].equals("add")){
                        if (object.has(guild.getId())){
                            FileWriter writer = new FileWriter(path.toFile());
                            JSONArray array = object.getJSONArray(guild.getId());
                            if (array.toList().contains(contentArr[2])){
                                channel.sendMessage("Роль <@&" + contentArr[2] + "> уже присутствует в вайтлисте верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                            } else {
                                array.put(contentArr[2]);
                                object.write(writer);
                                writer.close();
                            }
                        } else {
                            FileWriter writer = new FileWriter(path.toFile());
                            JSONArray array = new JSONArray();
                            array.put(contentArr[2]);
                            object.put(guild.getId(), array);
                            object.write(writer);
                            writer.close();
                        }
                        channel.sendMessage("Роль <@&" + contentArr[2] + "> успешно добавлена в вайтлист верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                    } else if (contentArr[1].equals("remove")){
                        if (object.has(guild.getId())){
                            FileWriter writer = new FileWriter(path.toFile());
                            JSONArray array = object.getJSONArray(guild.getId());
                            if (array.toList().contains(contentArr[2])){
                                for (int i=0; i<array.length(); i++){
                                    if (array.getString(i).equals(contentArr[2])){
                                        array.remove(i);
                                        break;
                                    }
                                }
                                object.put(guild.getId(), array);
                                object.write(writer);
                                writer.close();
                            } else {
                                channel.sendMessage("Роль <@&" + contentArr[2] + "> уже отсутствует в вайтлисте верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                            }
                        }
                        channel.sendMessage("Роль <@&" + contentArr[2] + "> успешно удалена из вайтлиста верефицированных ролей.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                    } else {
                        channel.sendMessage("Неправильное использование команды! Проверьте синтаксис команды (правильный синтаксис: `!whitelistRole add/remove <role_id>`) и повторите попытку. Код ошибки: R-1\n-# Запрошено пользвателем: " + message.getAuthor().getName() + ", " + getTime()).queue();
                    }
                } catch (Exception e){
                    logger.error(e.getMessage());
                    throw new RuntimeException(e);
                }
            } else {
                channel.sendMessage("<@" + message.getAuthor().getId() + ", неправильное использование команды. Проверьте синтаксис командой !help или !usage.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        }
    }

    public void clearMessages(MessageChannel channel, Message message, String content){
        if (TextVerification.allowedExecAdminCommands(message, channel)){
            String[] contentArr = content.split(" ");
            if (contentArr.length == 2){
                int length = Integer.parseInt(contentArr[1]);
                if (length<=49){
                    channel.getIterableHistory().takeAsync(length+1).thenAccept(messages -> {
                        channel.purgeMessages(messages);
                        channel.sendMessage("Успешно очищено " + length + " сообщений.").queue(msg -> {msg.delete().queueAfter(3, TimeUnit.SECONDS);});
                    }).exceptionally(error -> {logger.error("Произошла ошибка при удалении сообщений."); logger.warn("Caught and error while processing clearMessages command: {}", error.getMessage()); channel.sendMessage("Произошла ошибка при удалении сообщений.").queue(); return null;});
                } else {
                    channel.sendMessage("<@" + message.getAuthor().getId() + ">, вы не можете удалить больше 50 сообщений за раз.\n-# Запрошено пользователем: " + message.getAuthor().getName() + getTime()).queue();
                }
            }
        }
    }

    public void kickCommand(MessageChannel channel, Message message, String content){
        if (TextVerification.allowedExecAdminCommands(message, channel)){
            String[] messageArr = content.split(" ");
            Guild guild = message.getGuild();
            UserSnowflake snowflake = UserSnowflake.fromId(messageArr[1]);
            if (messageArr.length == 2){
                guild.kick(snowflake).queue(
                        (v) -> channel.sendMessage("<@" + messageArr[1] + "> был успешно кикнут с сервера <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                        (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось кикнуть <@" + snowflake.getId() + "> по причине: этого участника нельзя кикнуть либо же он уже кикнут.\n-# " + getTime()).queue()
                );
            } else if (messageArr.length == 3) {
                guild.kick(snowflake).queue(
                        (v) -> channel.sendMessage("<@" + messageArr[1] + "> был успешно кикнут с сервера <@" + message.getAuthor().getId() + "> по причине: " + messageArr[2] + ".\n-# " + getTime()).queue(),
                        (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось кикнуть <@" + snowflake.getId() + "> по причине: этого участника нельзя кикнуть либо же он уже кикнут.\n-# " + getTime()).queue()
                );
            } else {
                channel.sendMessage("<@" + message.getAuthor().getId() + ", неправильное использование команды. Проверьте синтаксис командой !help или !usage.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        }
    }

    public void timeoutCommand(MessageChannel channel, Message message, String content){
        if (TextVerification.allowedExecAdminCommands(message, channel)){
            String[] messageArr = content.split(" ");
            if (messageArr.length == 3){
                Guild guild = message.getGuild();
                UserSnowflake snowflake = UserSnowflake.fromId(messageArr[2]);
                if (messageArr[1].equals("60s") || messageArr[1].equals("5m") || messageArr[1].equals("10m") || messageArr[1].equals("1h") || messageArr[1].equals("1d") || messageArr[1].equals("1w")){
                    switch (messageArr[1]) {
                        case "60s" -> guild.timeoutFor(snowflake, 1, TimeUnit.MINUTES).queue(
                                (v) -> channel.sendMessage("<@" + messageArr[2] + "> был успешно отправлен в таймаут на время: 1 минута <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                                (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось отправить в таймаут <@" + snowflake.getId() + "> по причине: этого участника нельзя отправить в таймаут\n-# " + getTime()).queue()
                        );
                        case "5m" -> guild.timeoutFor(snowflake, 5, TimeUnit.MINUTES).queue(
                                (v) -> channel.sendMessage("<@" + messageArr[2] + "> был успешно отправлен в таймаут на время: 5 минут <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                                (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось отправить в таймаут <@" + snowflake.getId() + "> по причине: этого участника нельзя отправить в таймаут.\n-# " + getTime()).queue()
                        );
                        case "10m" -> guild.timeoutFor(snowflake, 10, TimeUnit.MINUTES).queue(
                                (v) -> channel.sendMessage("<@" + messageArr[2] + "> был успешно отправлен в таймаут на время: 10 минут <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                                (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось отправить в таймаут <@" + snowflake.getId() + "> по причине: этого участника нельзя отправить в таймаут.\n-# " + getTime()).queue()
                        );
                        case "1h" -> guild.timeoutFor(snowflake, 1, TimeUnit.HOURS).queue(
                                (v) -> channel.sendMessage("<@" + messageArr[2] + "> был успешно отправлен в таймаут на время: 1 час <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                                (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось отправить в таймаут <@" + snowflake.getId() + "> по причине: этого участника нельзя отправить в таймаут.\n-# " + getTime()).queue()
                        );
                        case "1d" -> guild.timeoutFor(snowflake, 1, TimeUnit.DAYS).queue(
                                (v) -> channel.sendMessage("<@" + messageArr[2] + "> был успешно отправлен в таймаут на время: 1 день <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                                (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось отправить в таймаут <@" + snowflake.getId() + "> по причине: этого участника нельзя отправить в таймаут.\n-# " + getTime()).queue()
                        );
                        case "1w" -> guild.timeoutFor(snowflake, 7, TimeUnit.DAYS).queue(
                                (v) -> channel.sendMessage("<@" + messageArr[2] + "> был успешно отправлен в таймаут на время: 1 неделя <@" + message.getAuthor().getId() + "> по причине: не указано.\n-# " + getTime()).queue(),
                                (error) -> channel.sendMessage("<@" + message.getAuthor().getId() + ">, не удалось отправить в таймаут <@" + snowflake.getId() + "> по причине: этого участника нельзя отправить в таймаут.\n-# " + getTime()).queue()
                        );
                    }
                }
            } else {
                channel.sendMessage("<@" + message.getAuthor().getId() + ", неправильное использование команды. Проверьте синтаксис командой !help или !usage.\n-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
            }
        }
    }

    public void clearCommands(MessageChannel channel, MessageReceivedEvent event){
        if (event.getAuthor().getId().equals("897054945889644564")){
            JDA bot = event.getJDA();
            bot.retrieveCommands().queue(commands -> {
                for (Command command : commands){
                    command.delete().queue();
                }
            });
            channel.sendMessage("Успешно очищены все слеш-команды для этого сервера.").queue();
        } else {
            channel.sendMessage("Только создатель бота может выполнять эти команды.").queue();
        }
    }

    public void registerCommands(MessageChannel channel, MessageReceivedEvent event){
        if (event.getAuthor().getId().equals("897054945889644564")){
            JDA bot = event.getJDA();
            bot.updateCommands()
                    .addCommands(Commands
                            .slash("help", "выводит список доступных команд.")
                            .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                            .setContexts(InteractionContextType.ALL))
                    .addCommands(Commands
                            .slash("tenzra-embed-gen", "отправляет в чат embed-сообщение основываясь на введенном тексте и загруженных файлах")
                            .addOption(OptionType.ATTACHMENT, "main-embed-pic", "просто загрузи заебал")
                            .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                            .setContexts(InteractionContextType.ALL))
                    .addCommands(Commands
                            .slash("save-embed-template", "сохраняет шаблон embed-сообщения в память бота")
                            .addOption(OptionType.STRING, "имя-шаблона", "имя шаблона", true)

                            .addOption(OptionType.STRING, "главный-текст", "главный текст", false)
                            .addOption(OptionType.STRING, "основной-текст", "что ты хочешь видеть в embed-сообщении (опационально)", false)
                            .addOption(OptionType.ATTACHMENT, "файл-embed", "какой файл будет отправлен вместе с embed-сообщением (опционально)", false)

                            .addOption(OptionType.STRING, "вложенное-имя-бота", "от какого имени будет отправлено embed-сообщение (опционально)", false)
                            .addOption(OptionType.STRING, "вложенный-главный-текст", "главный текст внутри embed сообщения", false)
                            .addOption(OptionType.STRING, "вложенный-основной-текст", "что ты хочешь видеть в embed-сообщении (опационально)", false)
                            .addOption(OptionType.ATTACHMENT, "вложенный-файл-embed", "какой файл будет отправлен вместе с embed-сообщением (опционально)", false)
                            .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                            .setContexts(InteractionContextType.ALL)
                    )
                    .addCommands(Commands
                            .slash("send-embed-template", "отправляет в чат ваше embed сообщение")
                            .addOption(OptionType.STRING, "имя-шаблона", "имя шаблона", true)
                            .addOption(OptionType.STRING, "айди-канала", "айди канала, куда нужно отправить сообщение", false)
                            .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                            .setContexts(InteractionContextType.ALL))
                    .addCommands(Commands
                            .slash("delete-embed-template", "удаляет отправленное вами embed сообщение")
                            .addOption(OptionType.STRING, "айди-сообщения", "айди сообщения", true)
                            .addOption(OptionType.STRING, "айди-канала", "айди канала, откуда нужно удалить сообщение", false)
                            .setIntegrationTypes(IntegrationType.GUILD_INSTALL)
                            .setContexts(InteractionContextType.ALL))
                    .addCommands(Commands
                            .slash("say", "отправляет в чат ваше сообщение")
                            .addOption(OptionType.STRING, "текст", "текст сообщения", true)
                            .setIntegrationTypes(IntegrationType.GUILD_INSTALL, IntegrationType.USER_INSTALL)
                            .setContexts(InteractionContextType.ALL))
                    .addCommands(Commands
                            .slash("base64encode", "кодирует любой ваш файл в строку Base64 и отправляет обратно текстовым файлом")
                            .addOption(OptionType.ATTACHMENT, "attachment", "желаемый-файл", true))
                    .addCommands(Commands
                            .slash("base64decode", "декодирует вашу строку Base64 в любой из поддерживаемых файлов")
                            .addOption(OptionType.ATTACHMENT, "attachment", "строка", true)
                            .addOption(OptionType.STRING, "filetype", "тип-выходного-файла", true)
                            .addOption(OptionType.BOOLEAN, "withfixed", "делает-ваше-видео-проигрываемым", true))
                    .addCommands(Commands
                            .slash("binaryencode", "кодирует любой ваш файл в бинарную строку и отправляет вам обратно")
                            .addOption(OptionType.ATTACHMENT, "attachment", "желаемый-файл", true)
                            .addOption(OptionType.BOOLEAN, "withspaces", "наличие-пробелов-между-бинарными-блоками", true))
                    .addCommands(Commands
                            .slash("binarydecode", "декодирует вашу строку binary в любой из поддерживаемых файлов")
                            .addOption(OptionType.ATTACHMENT, "attachment", "строка", true)
                            .addOption(OptionType.STRING, "filetype", "тип-выходного-файла", true)
                            .addOption(OptionType.BOOLEAN, "withspaces", "наличие-пробелов-между-бинарными-блоками", true))
                    .queue();
            channel.sendMessage("Глобальные команды зарегестрированы успешно").queue();
        } else {
            channel.sendMessage("Только создатель бота может выполнять эти команды.").queue();
        }
    }

    public void HelpCommand (MessageChannel channel, Message message){
        try {
            Path jarDir = Paths.get(
                    BotLauncher.class.getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
            ).getParent();
            Path filePath = jarDir.resolve("help-menu.txt");
            //Path filePath = Path.of("help-menu.txt");

            String aboutText = Files.readString(filePath);
            List<String> messages = SplitUtil.split(aboutText, 1986);
            for (String part : messages){
                channel.sendMessage(part).queue();
            }
            channel.sendMessage("-# Запрошено пользователем: " + message.getAuthor().getName() + ", " + getTime()).queue();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}

class TextVerification{
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