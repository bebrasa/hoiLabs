package org.example.notcustom;

import org.example.collections.SynchronizedArrayListWorker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class MainSyncList {
    public static void main(String[] args) {
        List<String> list = Collections.synchronizedList(new ArrayList<>());
        Scanner scanner = new Scanner(System.in);

        for (int i = 0; i < 4; i++) {
            new SynchronizedArrayListWorker(list, 250, 1000).start();
        }

        while (true) {
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                System.out.println("Current list status:");
                synchronized (list) {
                    for (String s : list) {
                        System.out.println(s);
                        try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                    }
                }
            } else {
                // Разбиваем длинные строки на куски по 80 символов и добавляем в начало
                for (int i = 0; i < line.length(); i += 80) {
                    int end = Math.min(i + 80, line.length());
                    list.add(0, line.substring(i, end));
                }
            }
        }
    }
}


