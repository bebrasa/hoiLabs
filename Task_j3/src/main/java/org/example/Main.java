package org.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {

    static final String BASE_URL = "http://localhost:8080";
    static final HttpClient client = HttpClient.newHttpClient();
    static final Set<String> visited = Collections.synchronizedSet(new HashSet<>());
    static final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    static final Phaser phaser = new Phaser();

    public static void main(String[] args) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            visited.add("/");
            phaser.register();
            executor.submit(() -> {
                crawl(executor, "/");
            });

            while (phaser.getRegisteredParties() != 0) {
                Thread.sleep(100);
            }

            Collections.sort(messages);
            messages.forEach(System.out::println);
            System.out.println(messages.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void crawl(ExecutorService executor, String path) {
        String url = BASE_URL + path;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                String msg = parseMessage(body);

                messages.add(msg);

                for (String next : parsePaths(body)) {
                    synchronized (visited) {

                        if (visited.add(next)) {
                            phaser.register();
                            executor.submit(() -> {
                                crawl(executor, "/" + next);
                            });
                        }
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error crawling " + url + ": " + e.getMessage());
        } finally {
            phaser.arriveAndDeregister();
        }
    }

    static String parseMessage(String body) {
        int key = body.indexOf("\"message\"");
        if (key == -1) return "";
        int firstQuote = body.indexOf('"', key + 9);
        int secondQuote = body.indexOf('"', firstQuote + 1);
        if (firstQuote == -1 || secondQuote == -1) return "";
        return body.substring(firstQuote + 1, secondQuote);
    }

    static List<String> parsePaths(String body) {
        List<String> list = new ArrayList<>();
        int key = body.indexOf("\"successors\"");
        if (key == -1) return list;
        int start = body.indexOf('[', key);
        int end = body.indexOf(']', start);
        if (start == -1 || end == -1) return list;

        String content = body.substring(start + 1, end);
        for (String token : content.split(",")) {
            token = token.trim();
            if (token.startsWith("\"") && token.endsWith("\"")) {
                list.add(token.substring(1, token.length() - 1));
            }
        }
        return list;
    }
}