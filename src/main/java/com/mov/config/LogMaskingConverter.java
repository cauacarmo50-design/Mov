package com.mov.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogMaskingConverter extends MessageConverter {

    // Mascara qualquer string como "senha=123" ou "token=abc" ou JSON "senha":"123"
    private static final Pattern PATTERN = Pattern.compile("(?i)(senha|password|token)[\\s]*[=:][\\s]*[\"']?([^\\s\"'&]+)[\"']?");

    @Override
    public String convert(ILoggingEvent event) {
        String message = super.convert(event);
        try {
            Matcher matcher = PATTERN.matcher(message);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1) + "=***MASCARADO***");
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            return message;
        }
    }
}
