package app.qwgram.next.td

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot

/**
 * Map with one snapshot state per key. Reading `map[id]` in a composable only subscribes to that key,
 * so a file-download tick or a status change recomposes the one row that shows it — not every row
 * (SnapshotStateMap invalidates every reader on any write, which made scrolling stutter).
 * Main thread only.
 */
class KeyedMap<K, V : Any> {
    private val slots = HashMap<K, MutableState<V?>>()
    private val keysVersion = mutableIntStateOf(0)

    /**
     * Never creates state while reading (reads can happen inside read-only snapshots, e.g. snapshotFlow,
     * where a freshly created state would be invisible afterwards). A missing key subscribes to key-set
     * changes instead, so the reader recomposes when the value first arrives.
     */
    operator fun get(k: K): V? {
        val s = slots[k]
        if (s == null) { keysVersion.intValue; return null }
        return s.value
    }

    operator fun set(k: K, v: V) {
        val s = slots[k]
        if (s == null) {
            slots[k] = mutableStateOf(v); keysVersion.intValue++
        } else {
            if (s.value == null) keysVersion.intValue++
            s.value = v
        }
    }

    fun remove(k: K) {
        slots[k]?.let { if (it.value != null) { it.value = null; keysVersion.intValue++ } }
    }

    /** current values; subscribes only to key-set changes, not to every value */
    val values: List<V>
        get() {
            keysVersion.intValue
            return Snapshot.withoutReadObservation { slots.values.mapNotNull { it.value } }
        }

    fun peek(k: K): V? = Snapshot.withoutReadObservation { slots[k]?.value }

    fun clear() {
        slots.values.forEach { it.value = null }
        keysVersion.intValue++
    }
}
