package org.example.datastructures;

public class SortingWorker extends Thread {
    private final ThreadSafeLinkedList list;
    private final int stepDelay;
    private final int cycleDelay;

    public SortingWorker(ThreadSafeLinkedList list, int stepDelay, int cycleDelay) {
        this.list = list;
        this.stepDelay = stepDelay;
        this.cycleDelay = cycleDelay;
    }

    private void pause(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        while (true) {
            ThreadSafeLinkedList.ElementNode current = list.getHead();
            while (current != null) {
                list.attemptSwap(current);
                pause(stepDelay);
                current = current.next;
            }
            pause(cycleDelay);
        }
    }
}
