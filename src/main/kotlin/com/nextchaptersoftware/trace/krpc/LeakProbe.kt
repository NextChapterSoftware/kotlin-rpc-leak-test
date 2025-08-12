package com.nextchaptersoftware.trace.krpc

import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.Collections

/**
 * Tracks objects via WeakReference to assert they get GC'd after requests/connections.
 * Instantiate per test for isolation.
 */
class LeakProbe {
    private val _queue = ReferenceQueue<Any>()
    private val _refs = Collections.synchronizedList(mutableListOf<WeakReference<Any>>())

    fun track(vararg objs: Any) {
        objs.forEach { _refs += WeakReference(it, _queue) }
    }

    fun reset() {
        _refs.clear()
        while (_queue.poll() != null) { /* drain */ }
    }

    fun refsSnapshot(): List<WeakReference<Any>> = _refs.toList()
    fun queue(): ReferenceQueue<Any> = _queue
}
