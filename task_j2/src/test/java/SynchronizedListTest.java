import org.example.collections.SynchronizedArrayListWorker;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SynchronizedListTest {

    private static final String INPUT_FILE = "src/test/resources/words.txt";

    @Test
    void testSynchronizedListSortingThreeRuns() throws Exception {
        List<String> words = readWords(INPUT_FILE);
        int[] swapSteps = new int[3];
        long[] executionTimes = new long[3];

        for (int run = 1; run <= 3; run++) {
            long startTime = System.currentTimeMillis();
            List<String> list = Collections.synchronizedList(new ArrayList<>());
            SynchronizedArrayListWorker.resetSwapSteps();

            list.addAll(words);

            for (int i = 0; i < 4; i++) {
                new SynchronizedArrayListWorker(list, 0, 0).start();
            }

            long maxWait = 30_000;
            while (true) {
                boolean sorted;
                synchronized (list) {
                    sorted = true;
                    for (int i = 0; i < list.size() - 1; i++) {
                        if (list.get(i).compareTo(list.get(i + 1)) > 0) {
                            sorted = false;
                            break;
                        }
                    }
                }
                long now = System.currentTimeMillis();
                if (sorted || now - startTime > maxWait) break;
                Thread.sleep(50);
            }

            for (int check = 0; check < 3; check++) {
                Thread.sleep(100);
                synchronized (list) {
                    for (int i = 0; i < list.size() - 1; i++) {
                        assertTrue(list.get(i).compareTo(list.get(i + 1)) <= 0, 
                            "Список не отсортирован (проверка " + (check+1) + "/3)");
                    }
                }
            }

            long endTime = System.currentTimeMillis();
            executionTimes[run - 1] = endTime - startTime;
            swapSteps[run - 1] = SynchronizedArrayListWorker.getSwapSteps();

            writeListToFile(list, "SynchronizedList_Run" + run + ".txt");
            System.out.println("Run " + run + ": swap steps = " + swapSteps[run - 1] + 
                             ", время = " + executionTimes[run - 1] + "мс");
        }

        // Статистика
        int avgSwapSteps = (swapSteps[0] + swapSteps[1] + swapSteps[2]) / 3;
        long avgExecutionTime = (executionTimes[0] + executionTimes[1] + executionTimes[2]) / 3;
        
        System.out.println("SynchronizedArrayList - среднее: " + avgSwapSteps + " шагов, " + avgExecutionTime + "мс");
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

    private void writeListToFile(List<String> list, String filename) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            synchronized (list) {
                for (String s : list) bw.write(s + "\n");
            }
        }
    }

}

