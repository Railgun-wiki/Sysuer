package com.sysu.edu.api;

import androidx.annotation.NonNull;

import java.util.LinkedList;

public class RequestQueue {
    LinkedList<Runnable> queue = new LinkedList<>();
    public void add(@NonNull Runnable runnable) {
        queue.add(runnable);
    }
    public void next() {
        if (queue.poll() != null && !queue.isEmpty()) queue.getFirst().run();
    }
}
