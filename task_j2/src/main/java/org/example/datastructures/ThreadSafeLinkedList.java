package org.example.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeLinkedList implements Iterable<String> {

    private ElementNode head = null;
    private static int swapAttempts = 0;

    public static void resetSwapAttempts() { swapAttempts = 0; }
    public static int getSwapAttempts() { return swapAttempts; }

    public static class ElementNode {
        String value;
        ElementNode next;
        ElementNode prev;
        final ReentrantLock lock = new ReentrantLock();

        ElementNode(String value) { this.value = value; }
    }

    public void addAtFront(String val) {
        ElementNode newNode = new ElementNode(val);
        if (head == null) {
            head = newNode;
            return;
        }

        head.lock.lock();
        ElementNode oldHead = head;
        try {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        } finally {
            oldHead.lock.unlock();
        }
    }

    public ElementNode getHead() { return head; }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<>() {
            ElementNode current = head;

            @Override
            public boolean hasNext() { return current != null; }

            @Override
            public String next() {
                if (current == null) throw new NoSuchElementException();
                current.lock.lock();
                ElementNode old = current;
                try {
                    String val = current.value;
                    current = current.next;
                    return val;
                } finally {
                    old.lock.unlock();
                }
            }
        };
    }

    public void attemptSwap(ElementNode node) {
        if (node == null)
            return;

        node.lock.lock();
        ElementNode prevNode = node.prev;
        node.lock.unlock();

        if (prevNode != null)
            prevNode.lock.lock();

        try {
            if (prevNode != null)
                node = prevNode.next;

            node.lock.lock();
            try {
                ElementNode nextNode = node.next;
                if (nextNode == null) return;

                nextNode.lock.lock();
                try {
                    if (node.value.compareTo(nextNode.value) > 0) {
                        ElementNode nextNext = nextNode.next;
                        if (nextNext != null)
                            nextNext.lock.lock();
                        try {
                            // Меняем ссылки для swap
                            if (prevNode != null)
                                prevNode.next = nextNode;
                            nextNode.prev = prevNode;

                            nextNode.next = node;
                            node.prev = nextNode;

                            node.next = nextNext;
                            if (nextNext != null)
                                nextNext.prev = node;

                            if (head == node)
                                head = nextNode;

                            swapAttempts++;
                        } finally {
                            if (nextNext != null)
                                nextNext.lock.unlock();
                        }
                    }
                } finally {
                    nextNode.lock.unlock();
                }
            } finally {
                node.lock.unlock();
            }
        } finally {
            if (prevNode != null)
                prevNode.lock.unlock();
        }
    }


    public boolean isSorted() {
        ElementNode current = head;
        while (current != null && current.next != null) {
            current.lock.lock();
            ElementNode next = current.next;
            next.lock.lock();
            try {
                if (current.value.compareTo(next.value) > 0) return false;
            } finally {
                next.lock.unlock();
                current.lock.unlock();
            }
            current = next;
        }
        return true;
    }
}

