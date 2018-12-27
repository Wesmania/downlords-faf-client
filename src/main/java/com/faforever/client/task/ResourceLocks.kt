package com.faforever.client.task

import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock

object ResourceLocks {

    private val NETWORK_LOCK = ReentrantReadWriteLock()
    private val DISK_LOCK = ReentrantLock()

    fun acquireDownloadLock() {
        NETWORK_LOCK.readLock().lock()
    }

    fun freeDownloadLock() {
        NETWORK_LOCK.readLock().unlock()
    }

    fun acquireUploadLock() {
        NETWORK_LOCK.writeLock().lock()
    }

    fun freeUploadLock() {
        NETWORK_LOCK.writeLock().unlock()
    }

    fun acquireDiskLock() {
        DISK_LOCK.lock()
    }

    fun freeDiskLock() {
        DISK_LOCK.unlock()
    }
}
