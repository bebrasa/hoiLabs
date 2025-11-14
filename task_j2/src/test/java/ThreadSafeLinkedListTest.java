import org.example.datastructures.ThreadSafeLinkedList;
import org.example.datastructures.SortingWorker;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThreadSafeLinkedListTest {

    private static final String INPUT_FILE = "src/test/resources/words.txt";

    @Test
    void testThreadSafeListSortingThreeRuns() throws Exception {
        List<String> words = readWords(INPUT_FILE);
        int[] swapAttempts = new int[10];
        long[] executionTimes = new long[10];
        String[] resultFiles = new String[10];

        for (int run = 1; run <= 10; run++) {
            long startTime = System.currentTimeMillis();
            ThreadSafeLinkedList list = new ThreadSafeLinkedList();
            ThreadSafeLinkedList.resetSwapAttempts();

            // Добавляем слова в список
            for (String w : words) list.addAtFront(w);

            // Запускаем сортировщики
            for (int i = 0; i < 4; i++) {
                new SortingWorker(list, 0, 0).start();
            }

            // Ждём, пока реально отсортируется (до 5 минут)
            boolean sorted = waitUntilSorted(list, 30_000);
            assertTrue(sorted, "Список должен быть отсортирован после работы потоков");

            long endTime = System.currentTimeMillis();
            executionTimes[run - 1] = endTime - startTime;
            swapAttempts[run - 1] = ThreadSafeLinkedList.getSwapAttempts();

            // Записываем результат в файл
            String filename = "ThreadSafeLinkedList_Run" + run + ".txt";
            resultFiles[run - 1] = filename;
            writeListToFile(list, filename);

            System.out.println("Run " + run + ": swap attempts = " + swapAttempts[run - 1] +
                    ", время = " + executionTimes[run - 1] + "мс");
        }

        compareResultFiles(resultFiles);

        // Статистика
        int avgSwapAttempts = (swapAttempts[0] + swapAttempts[1] + swapAttempts[2]) / 3;
        long avgExecutionTime = (executionTimes[0] + executionTimes[1] + executionTimes[2]) / 3;

        System.out.println("ThreadSafeLinkedList - среднее: " + avgSwapAttempts + " попыток, " + avgExecutionTime + "мс");
    }

    private List<String> readWords(String path) throws IOException {
        List<String> result = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null)
                if (!line.isBlank()) result.add(line.trim());
        }
        return result;
    }

    private void writeListToFile(ThreadSafeLinkedList list, String filename) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            for (String s : list) bw.write(s + "\n");
        }
    }

    private boolean waitUntilSorted(ThreadSafeLinkedList list, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (!list.isSorted() && (System.currentTimeMillis() - start < timeoutMs)) {
            Thread.sleep(100);
        }
        return list.isSorted();
    }

    private List<String> readFile(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null)
                lines.add(line);
        }
        return lines;
    }

    private void compareResultFiles(String[] filenames) throws IOException {
        List<String> base = readFile(filenames[0]);
        for (int i = 1; i < filenames.length; i++) {
            List<String> compare = readFile(filenames[i]);
            assertEquals(base.size(), compare.size(),
                    "Размеры файлов " + filenames[0] + " и " + filenames[i] + " различаются");
            for (int j = 0; j < base.size(); j++) {
                assertEquals(base.get(j), compare.get(j),
                        "Несовпадение строк в позициях " + j + " между файлами " + filenames[0] + " и " + filenames[i]);
            }
        }
        System.out.println(" Все три результата идентичны");
    }
}
