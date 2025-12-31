package org.unnamedfurry;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class GameBot {
    private static final Logger log = LoggerFactory.getLogger(GameBot.class);
    public static String getTime(){
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String date = LocalDate.now().format(dateFormatter);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String time = LocalTime.now().format(timeFormatter);
        return "-" + date + "-" + time;
    }

    public void base64encode(SlashCommandInteractionEvent event){
        event.deferReply().queue();
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("attachment")).getAsAttachment();
        String inputName = "/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64e."+getTime()+"."+attachment.getFileExtension();
        File input = new File(inputName);
        attachment.getProxy().downloadToFile(input).join();
        try {
            byte[] src = Files.readAllBytes(input.toPath());
            String base64encoded = Base64.getEncoder().encodeToString(src);
            String outputName = "/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64o."+getTime()+".txt";
            File output = new File(outputName);
            Files.writeString(output.toPath(), base64encoded);
            event.getHook().editOriginal("Success!").setAttachments(FileUpload.fromData(output, "output.txt")).queue(
                    success -> {
                        if (input.delete() && output.delete()){}
                        else {log.error("Cannot delete file while encoding to base64!!! File: /root/DiscordBot/tempFiles/{} or /root/DiscordBot/tempFiles/{}", inputName, outputName);}
                    }
            );
        } catch (Exception e) {
            event.getHook().editOriginal("Произошла ошибка.").queue();
            log.error("Caught an unexpected error while encoding to base64: \n{}\n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public void base64decode(SlashCommandInteractionEvent event){
        event.deferReply().queue();
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("attachment")).getAsAttachment();
        String inputName = "/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64d."+getTime()+".txt";
        File input = new File(inputName);
        attachment.getProxy().downloadToFile(input).join();
        String requestingType = Objects.requireNonNull(event.getOption("filetype")).getAsString();
        List<String> FILE_EXTENSIONS = List.of("txt", "doc", "png", "jpeg", "jpg", "mp4", "mkv", "mov", "webm", "gif", "wav", "mp3", "m4a", "aac");
        if (!FILE_EXTENSIONS.contains(requestingType)){
            event.getHook().editOriginal("Неподдерживаемый формат файла. Текущие поддерживаемые форматы: txt, doc, png, jpeg, jpg, mp4, mkv, mov, webm, gif, wav, mp3, m4a, aac.").queue();
            return;
        }
        try {
            byte[] src = Base64.getDecoder().decode(Files.readString(input.toPath()));
            String outputName = "/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-b64o."+getTime()+"."+requestingType;
            File output = new File(outputName);
            Files.write(output.toPath(), src);
            event.getHook().editOriginal("Success!").setAttachments(FileUpload.fromData(output, "output."+requestingType)).queue(
                    success -> {
                        if (input.delete() && output.delete()){}
                        else {log.error("Cannot delete file while decoding from base64!!! File: /root/DiscordBot/tempFiles/{} or /root/DiscordBot/tempFiles/{}", inputName, outputName);}
                    }
            );
        } catch (Exception e) {
            event.getHook().editOriginal("Файл не содержит либо содержит битую строку Base64.").queue();
            log.error("Caught an unexpected error while decoding user's file: \n{}, \n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public void binaryEncode(SlashCommandInteractionEvent event){
        event.deferReply().queue();
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("attachment")).getAsAttachment();
        String inputName = "/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-binE."+getTime()+"."+attachment.getFileExtension();
        File input = new File(inputName);
        attachment.getProxy().downloadToFile(input).join();
        try {
            byte[] src = Files.readAllBytes(input.toPath());
            StringBuilder sb = new StringBuilder(src.length * (Objects.requireNonNull(event.getOption("withspaces")).getAsBoolean() ? 9 : 8));
            if (Objects.requireNonNull(event.getOption("withspaces")).getAsBoolean()){
                for (byte b : src){
                    String bin = Integer.toBinaryString(Byte.toUnsignedInt(b));
                    sb.append("00000000".substring(bin.length())).append(bin).append(" ");
                }
            } else {
                for (byte b : src){
                    String bin = Integer.toBinaryString(Byte.toUnsignedInt(b));
                    sb.append("00000000".substring(bin.length())).append(bin);
                }
            }
            String bytes = sb.toString().trim();
            String outputName = "/root/DiscordBot/tempFiles/"+event.getUser().getId()+"-binD."+getTime()+".txt";
            File output = new File(outputName);
            Files.writeString(output.toPath(), bytes);
            event.getHook().editOriginal("Success!").setAttachments(FileUpload.fromData(output, "output.txt")).queue(
                    success -> {
                        if (input.delete() && output.delete()){}
                        else {log.error("Cannot delete file while encoding to binary!!! File: /root/DiscordBot/tempFiles/{} or /root/DiscordBot/tempFiles/{}", inputName, outputName);}
                    }
            );
        } catch (Exception e) {
            event.getHook().editOriginal("Произошла ошибка.").queue();
            log.error("Caught an unexpected error while encoding to binary: \n{}\n{}", e.getMessage(), e.getStackTrace());
        }
    }

    public void binaryDecode(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Message.Attachment attachment = Objects.requireNonNull(event.getOption("attachment")).getAsAttachment();
        String inputName = "/root/DiscordBot/tempFiles/" + event.getUser().getId() + "-binD." + getTime() + "." + attachment.getFileExtension();
        File input = new File(inputName);
        attachment.getProxy().downloadToFile(input).join();
        try {
            String encoded = Files.readString(input.toPath());
            String requestingType = Objects.requireNonNull(event.getOption("filetype")).getAsString();
            List<String> FILE_EXTENSIONS = List.of("txt", "doc", "png", "jpeg", "jpg", "mp4", "mkv", "mov", "webm", "gif", "wav", "mp3", "m4a", "aac");
            if (!FILE_EXTENSIONS.contains(requestingType)) {
                event.reply("Неподдерживаемый формат файла. Текущие поддерживаемые форматы: txt, doc, png, jpeg, jpg, mp4, mkv, mov, webm, gif, wav, mp3, m4a, aac.").setEphemeral(true).queue();
                return;
            }

            byte[] result;
            if (Objects.requireNonNull(event.getOption("withspaces")).getAsBoolean()) {
                String[] parts = encoded.trim().split("\\s+");
                result = new byte[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String bits = parts[i];
                    if (bits.length() != 8) {
                        event.getHook().editOriginal("Каждый блок должен быть кратным 8!").queue();
                        return;
                    }
                    result[i] = (byte) Integer.parseInt(bits, 2);
                }
            } else {
                if (encoded.length() % 8 != 0) {
                    event.getHook().editOriginal("Каждый блок должен быть кратным 8!").queue();
                    return;
                }
                result = new byte[encoded.length() / 8];
                for (int i = 0; i < result.length; i++) {
                    String byteStr = encoded.substring(i * 8, (i + 1) * 8);
                    result[i] = (byte) Integer.parseInt(byteStr, 2);
                }
            }
            String outputName = "/root/DiscordBot/tempFiles/" + event.getUser().getId() + "-binE." + getTime() + "." + requestingType;
            File output = new File(outputName);
            Files.write(output.toPath(), result);
            event.getHook().editOriginal("Success!").setAttachments(FileUpload.fromData(output, "output." + requestingType)).queue(
                    success -> {
                        if (input.delete() && output.delete()) {
                        } else {
                            log.error("Cannot delete file while decoding from binary!!! File: /root/DiscordBot/tempFiles/{} or /root/DiscordBot/tempFiles/{}", inputName, outputName);
                        }
                    }
            );
        } catch (IOException e) {
            event.getHook().editOriginal("Произошла ошибка.").queue();
            log.error("Caught an unexpected error while decoding from binary: \n{}\n{}", e.getMessage(), e.getStackTrace());
        } catch (NumberFormatException e) {
            event.getHook().editOriginal("В строке используются недопустимые сивмолы (можно только 0, 1 и пробел).").queue();
        }
    }
}
