import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args){
        final Server server = new Server(9999);


        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/index.html", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                final Path filePath = Path.of(".", "public", request.getPath());
                try {
                    final String mimeType = Files.probeContentType(filePath);

                    final long length = Files.size(filePath);
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    Files.copy(filePath, responseStream);
                    responseStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });//не стал добавлять хендлер с методом PUT потому что не знаю, как его сделать такой запрос
        server.addHandler("GET", "/classic.html", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) {
                final Path filePath = Path.of(".", "public", request.getPath());
                try {
                    final String mimeType = Files.probeContentType(filePath);

                    final var template = Files.readString(filePath);
                    final var content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    responseStream.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    responseStream.write(content);
                    responseStream.flush();
                }catch (Exception e){
                    e.printStackTrace();}
            }
        });

        server.start();
    }
}