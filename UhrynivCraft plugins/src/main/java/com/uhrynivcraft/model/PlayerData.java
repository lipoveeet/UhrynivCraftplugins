package com.uhrynivcraft.model;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory representation of a player's economy state.
 * Balance changes happen here first (fast, thread-safe) and are
 * periodically flushed to MySQL by the EconomyManager save task.
 */
public class PlayerData {

    private final UUID uuid;
    private volatile String name;
    private volatile double balance;
    private volatile double totalEarned;

    /** True if this record has unsaved changes that need to be flushed to MySQL. */
    private volatile boolean dirty;

    /**
     * Rolling log of (timestampMillis, amount) pairs earned in the last hour.
     * Used to compute "earned in the last hour" for /up and for the anti-farm tiers.
     * A deque is used so old entries can be trimmed cheaply from the front.
     */
    private final ConcurrentLinkedDeque<HourEntry> hourLog = new ConcurrentLinkedDeque<>();

    public PlayerData(UUID uuid, String name, double balance, double totalEarned) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
        this.totalEarned = totalEarned;
    }

    public record HourEntry(long timestamp, double amount) {}

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public double getTotalEarned() {
        return totalEarned;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        this.dirty = false;
    }

    /** Adds (or subtracts, if negative) UP from the balance. Thread-safe via synchronized block. */
    public synchronized void addBalance(double amount, boolean countAsEarned) {
        this.balance += amount;
        if (countAsEarned && amount > 0) {
            this.totalEarned += amount;
            hourLog.addLast(new HourEntry(System.currentTimeMillis(), amount));
        }
        this.dirty = true;
    }

    public synchronized boolean subtractBalance(double amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        dirty = true;
        return true;
    }

    /** Removes entries older than one hour and returns the sum earned within the last hour. */
    public synchronized double getHourIncome() {
        long cutoff = System.currentTimeMillis() - 3_600_000L;
        while (!hourLog.isEmpty() && hourLog.peekFirst().timestamp() < cutoff) {
            hourLog.pollFirst();
        }
        double sum = 0.0;
        for (HourEntry e : hourLog) {
            sum += e.amount();
        }
        return sum;
    }
}
