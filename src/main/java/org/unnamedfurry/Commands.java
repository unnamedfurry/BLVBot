package org.unnamedfurry;

import net.dv8tion.jda.api.entities.Message;
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
                    .header("Authorization", "Bot MTQzMzQ3ODA2MTYwMDUzODg0Ng.G9JVSn.zaKp8THIxZd6JabrLs-9noyzyJ-6b1nRI2c9kk")
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
        }
    }
}