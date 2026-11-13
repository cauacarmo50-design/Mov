import java.io.*;
import java.nio.file.*;
import java.util.regex.*;
import java.util.stream.*;

public class ExtractCSS {
    public static void main(String[] args) throws Exception {
        Path templatesDir = Paths.get("src/main/resources/templates");
        Path staticCssDir = Paths.get("src/main/resources/static/css");
        Files.createDirectories(staticCssDir);
        Path globalCssFile = staticCssDir.resolve("global.css");

        StringBuilder globalCss = new StringBuilder();
        Pattern stylePattern = Pattern.compile("(?s)<style>(.*?)</style>");

        try (Stream<Path> paths = Files.walk(templatesDir)) {
            paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".html")).forEach(file -> {
                try {
                    String content = new String(Files.readAllBytes(file));
                    Matcher matcher = stylePattern.matcher(content);
                    if (matcher.find()) {
                        String css = matcher.group(1);
                        globalCss.append("\n/* Styles from ").append(file.getFileName()).append(" */\n");
                        globalCss.append(css);
                        
                        String newContent = matcher.replaceAll("<link rel=\"stylesheet\" th:href=\"@{/css/global.css}\">");
                        Files.write(file, newContent.getBytes());
                        System.out.println("Processed " + file.getFileName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Files.write(globalCssFile, globalCss.toString().getBytes());
        System.out.println("Global CSS created at " + globalCssFile);
    }
}
