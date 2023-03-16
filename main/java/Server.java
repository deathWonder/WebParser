import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    final ExecutorService threadPool = Executors.newFixedThreadPool(64);
    Map<Map<String, String>, Handler> handlerList = new HashMap<>();
    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server started...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(connection(clientSocket));
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private Thread connection(Socket socket){
        return new Thread(()-> {
            try {
                final BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());

                // лимит на request line + заголовки
                final int limit = 4096;

                in.mark(limit);
                final byte[] buffer = new byte[limit];
                final int read = in.read(buffer);

                // ищем request line
                final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
                final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    badRequest(out);
                    return;
                }

                // читаем request line
                final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                if (requestLine.length != 3) {
                    badRequest(out);
                    return;
                }

                Request request = new Request();//создаем запрос
                request.setMethod(requestLine[0]);//добавляем в запрос метод

                final String message = requestLine[1];
                if (!message.startsWith("/")) {
                    badRequest(out);
                    return;
                }
                //отделяем path and queries
                List<NameValuePair> pairs = URLEncodedUtils.parse(message, StandardCharsets.UTF_8, '&', '?');
                request.setPath(pairs.get(0).getName());//добавляем в запрос путь

                if(pairs.size()>1){
                    pairs.remove(0);
                    request.setQueries(pairs);//если queries есть, то добавляем в запрос
                }

                // ищем заголовки
                final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                final int headersStart = requestLineEnd + requestLineDelimiter.length;
                final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    badRequest(out);
                    return;
                }

                // отматываем на начало буфера
                in.reset();
                // пропускаем requestLine
                in.skip(headersStart);

                final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
                final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));

                request.setHeaders(headers);//добавляем заголовки
                request.setScheme(requestLine[2]);//добавляем протокол

                if (!request.getMethod().equals("GET")) {
                    in.skip(headersDelimiter.length);
                    // вычитываем Content-Length, чтобы прочитать body
                    Optional<String> contentLength = request.getHead("Content-Length");
                    if (contentLength.isPresent()) {
                        final int length = Integer.parseInt(contentLength.get());
                        final byte[] bodyBytes = in.readNBytes(length);

                        final String body = new String(bodyBytes);

                        final Optional<String> contentType = request.getHead("Content-Type");
                        if(contentType.isPresent()) {
                            if (contentType.get().equals("application/x-www-form-urlencoded")) {
                                List<NameValuePair> bodyValues = URLEncodedUtils.parse(body, StandardCharsets.UTF_8, '&');
                                request.setBodies(bodyValues);
                            }
                        }
                    }
                }

                for(Map<String, String> key: handlerList.keySet()){//пробегаемся по ключам списка обработчиков
                    for(String method: key.keySet()){//Пробегаюсь по методам
                        if(method.equals(request.getMethod())){//если метод равняется методу запроса
                            if(key.get(method).equals(request.getPath())){//проверяю сообщение
                                handlerList.get(key).handle(request, out);//если нашли, то вызываю метод handle
                                break;
                            }
                        }
                    }
                }
                badRequest(out);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }


    public void addHandler(String method, String message, Handler handler){
        Map<String, String> map= new HashMap<>();
        map.put(method, message);
        handlerList.put(map, handler);
    }
    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}
