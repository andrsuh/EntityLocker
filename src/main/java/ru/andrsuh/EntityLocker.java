package ru.andrsuh;

import com.sun.istack.internal.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocker<T> implements IEntityLocker<T> {

    private final ReentrantLock globalLock = new ReentrantLock(true);
    private final Map<T, Lock> storage = new ConcurrentHashMap<>();
    private volatile boolean isGlobalLock;

    @Override
    public void lockEntity(@Nullable T entityID) {
        Lock lock = getLockByID(entityID);
        checkIfGlobalLock();
        lock.lock();
    }

    @Override
    public void lockEntity(@Nullable T entityID, long lockWaitingTime) throws TimeoutException, InterruptedException {
        Lock lock = getLockByID(entityID);

        checkIfGlobalLock();
        if (!lock.tryLock(lockWaitingTime, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException();
        }
    }

    private void checkIfGlobalLock() {
        if (isGlobalLock) {
            globalLock.lock();
            try {
            } finally {
                globalLock.unlock();
            }
        }
    }

    @Override
    public void acquireGlobalLock() {
        globalLock.lock();
        isGlobalLock = true;
        for (T id : storage.keySet()) {
            storage.get(id).lock();
        }
    }

    @Override
    public void releaseGlobalLock() {
        if (globalLock.isHeldByCurrentThread()) {
            isGlobalLock = false;
            for (Lock lock : storage.values()) {
                if (((ReentrantLock) lock).isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
            // I'm not sure whether It would useful round previous code with try and release lock in finally
            globalLock.unlock();
        }
    }

    private Lock getLockByID(@Nullable T entityID) {
        if (entityID == null) {
            throw new IllegalArgumentException("Entity id can't be a null");
        }

        storage.putIfAbsent(entityID, new ReentrantLock(true));
        return storage.get(entityID);
    }

    @Override
    public void unlockEntity(@Nullable T entityID) {
        if (entityID == null) {
            throw new IllegalArgumentException("Entity id can't be a null");
        }

        ReentrantLock lock = (ReentrantLock) storage.get(entityID);
        if (lock == null) {
            throw new IllegalArgumentException("Entity with the specified key does not exist");
        }

        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
