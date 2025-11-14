package org.example.collections;

import java.util.Collections;
import java.util.List;

public class SynchronizedArrayListWorker extends Thread {
    private final List<String> list;
    private final int stepDelay;
    private final int cycleDelay;
    private static int swapSteps = 0;

    public SynchronizedArrayListWorker(List<String> list, int stepDelay, int cycleDelay) {
        this.list = list;
        this.stepDelay = stepDelay;
        this.cycleDelay = cycleDelay;
    }

    public static int getSwapSteps() {
        return swapSteps;
    }

    public static void resetSwapSteps() {
        swapSteps = 0;
    }

    private void pause(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        while (true) {
            int size;
            synchronized (list) {
                size = list.size();
            }

            for (int i = 0; i < Math.max(0, size - 1); i++) {
                pause(stepDelay);

                synchronized (list) {
                    if (i + 1 >= list.size()) break;
                    String a = list.get(i);
                    String b = list.get(i + 1);
                    if (a.compareTo(b) > 0) {
                        Collections.swap(list, i, i + 1);
                        swapSteps++;
                    }
                }
            }
            pause(cycleDelay);
        }
    }
}
