package ru.andrsuh;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class EntityLockerTest {
    private static final int NUM_OF_INCREMENTS = 500_000;
    private List<Thread> threads;
    private EntityLocker<Integer> entityLocker;
    private TestClass testObject;

    @Before
    public void initializeTestEnvironment() {
        threads = new ArrayList<>();
        entityLocker = new EntityLocker<>();
        testObject = new TestClass(42); // any
    }

    @Test
    public void checkProtectedCodeExecute() throws InterruptedException {
        int value = 42;

        entityLocker.lockEntity(testObject.getId());
        try {
            testObject.setValue(value);
        } finally {
            entityLocker.unlockEntity(testObject.getId());
        }

        assertEquals(value, testObject.getValue());
    }

    @Test
    public void checkProtectedCodeExecuteSequantiallyOnSameEntity() throws InterruptedException {
        for (int i = 0; i < NUM_OF_INCREMENTS; ++i) {
            Thread thread = new Thread(() -> {
                entityLocker.lockEntity(testObject.getId());
                try {
                    testObject.incrementValue();
                } finally {
                    entityLocker.unlockEntity(testObject.getId());
                }
            });
            thread.start();
            threads.add(thread);
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(NUM_OF_INCREMENTS, testObject.getValue());
    }

    @Test
    public void checkTimedWaiting() throws InterruptedException {
        final long numOfIncrements = 1;

        Thread thread = new Thread(() -> {
            entityLocker.lockEntity(testObject.getId());
            try {
                Thread.sleep(2000);
                testObject.incrementValue();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                entityLocker.unlockEntity(testObject.getId());
            }
        });
        thread.start();
        threads.add(thread);

        Thread anotherThread = new Thread(() -> {
            try {
                entityLocker.lockEntity(testObject.getId(), 1L);
                try {
                    testObject.incrementValue();
                } finally {
                    entityLocker.unlockEntity(testObject.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
                // time is out - do nothing
            }
        });
        anotherThread.start();
        threads.add(anotherThread);

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(numOfIncrements, testObject.getValue());
    }

    @Test
    public void checkGlobalLockProtoctedCodeExecuteSequantiallyWithOthers() throws InterruptedException {
        final EntityLocker<Integer> entityLocker = new EntityLocker<>();

        for (int i = 0; i < NUM_OF_INCREMENTS; ++i) {
            Thread thread = new Thread(() -> {
                entityLocker.lockEntity(testObject.getId());
                try {
                    testObject.incrementValue();
                } finally {
                    entityLocker.unlockEntity(testObject.getId());
                }
            });
            thread.start();
            threads.add(thread);
        }

        Thread thread = new Thread(() -> {
            entityLocker.acquireGlobalLock();

            try {
                for (int i = 0; i < NUM_OF_INCREMENTS; i++) {
                    testObject.incrementValue();
                }
            } finally {
                entityLocker.releaseGlobalLock();
            }
        });
        thread.start();
        threads.add(thread);

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(NUM_OF_INCREMENTS * 2, testObject.getValue());
    }

    private static class TestClass {
        private int id;

        private int value;

        public TestClass(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void incrementValue() {
            this.value += 1; // Non-atomic operation
        }
    }
}