package tools.vlab.kberry.server.log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedStack<T> {

    private final int maxSize;
    private final Deque<T> deque;
    private final ReentrantLock lock = new ReentrantLock();

    public BoundedStack(int maxSize) {
        this.maxSize = maxSize;
        this.deque = new ArrayDeque<>(maxSize);
    }

    public void push(T element) {
        lock.lock();
        try {
            if (deque.size() == maxSize) {
                deque.removeLast(); // ältestes Element raus
            }
            deque.addFirst(element); // Stack-Push
        } finally {
            lock.unlock();
        }
    }

    public T pop() {
        lock.lock();
        try {
            return deque.pollFirst();
        } finally {
            lock.unlock();
        }
    }

    public T peek() {
        lock.lock();
        try {
            return deque.peekFirst();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return deque.size();
        } finally {
            lock.unlock();
        }
    }

    public List<T> toList() {
        lock.lock();
        try {
            return new ArrayList<>(deque);
        } finally {
            lock.unlock();
        }
    }
}
