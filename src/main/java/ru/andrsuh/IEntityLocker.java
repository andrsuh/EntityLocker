package ru.andrsuh;

import java.util.concurrent.TimeoutException;


/**
 * Interface represents mechanism allowing designate the boundaries of
 * code we want to execute with exclusive access (protected code) for
 * entity with specified id.
 * <p>
 * EntityLocker guarantees that at most one thread executes
 * protected code on the same entity and allows concurrent
 * execution of protected code on different entities.
 *
 * @param <T> the type of entity ID we want to work with
 */
public interface IEntityLocker<T> {

    /**
     * Defines entry point of entity's protected code
     *
     * @param entityID value of entity's ID we want to exclusive access with
     */
    void lockEntity(T entityID);

    /**
     * Defines entry point of entity's protected code.
     * Allows specify the maximum time in milliseconds to wait for the lock
     *
     * @param entityID        value of entity's ID we want to exclusive access with
     * @param lockWaitingTime the maximum time in milliseconds to wait for the lock
     * @throws TimeoutException     if the wait timed out
     * @throws InterruptedException if the current thread was interrupted
     *                              while waiting
     */
    void lockEntity(T entityID, long lockWaitingTime) throws TimeoutException, InterruptedException;

    /**
     * Defines the endpoint of code with exclusive access to the entity.
     * Release lock if it has been blocked in current thread
     *
     * @param entityID value of locked entity's ID
     */
    void unlockEntity(T entityID);

    /**
     * Defines entry point of protected code, which will not
     * execute concurrently with any other protected code.
     */
    void acquireGlobalLock();

    /**
     * Defines the endpoint of protected code, which will not
     * execute concurrently with any other protected code.
     * Release lock if it has been blocked in current thread
     */
    void releaseGlobalLock();
}
