package com.kannada.kavi.core.common.extensions

/**
 * Collection Extensions - Making Lists and Sets Super Easy!
 *
 * Collections are like containers - lists, sets, maps, etc.
 * These extensions make working with collections much easier.
 *
 * Think of a list like a shopping list - sometimes you need to add items,
 * remove items, find specific items, or check if it's empty.
 */

/**
 * Check if list is null or empty
 *
 * Example:
 * null.isNullOrEmpty() -> true
 * emptyList().isNullOrEmpty() -> true
 * listOf("apple").isNullOrEmpty() -> false
 */
fun <T> List<T>?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

/**
 * Check if list has items (opposite of isNullOrEmpty)
 *
 * Example:
 * listOf("apple", "banana").isNotNullOrEmpty() -> true
 * emptyList().isNotNullOrEmpty() -> false
 */
fun <T> List<T>?.isNotNullOrEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

/**
 * Get item at index safely (returns null if index doesn't exist)
 * This prevents crashes when trying to access items that don't exist!
 *
 * Example:
 * listOf("a", "b", "c").getOrNull(1) -> "b"
 * listOf("a", "b").getOrNull(5) -> null (doesn't crash!)
 */
fun <T> List<T>.getOrNull(index: Int): T? {
    return if (index in 0 until this.size) this[index] else null
}

/**
 * Get the second item in the list safely
 *
 * Example:
 * listOf("first", "second", "third").secondOrNull() -> "second"
 * listOf("only").secondOrNull() -> null
 */
fun <T> List<T>.secondOrNull(): T? = this.getOrNull(1)

/**
 * Get the third item in the list safely
 */
fun <T> List<T>.thirdOrNull(): T? = this.getOrNull(2)

/**
 * Get the last item safely (returns null if list is empty)
 *
 * Example:
 * listOf("a", "b", "c").lastOrNull() -> "c"
 * emptyList().lastOrNull() -> null
 */
fun <T> List<T>.lastOrNull(): T? {
    return if (this.isNotEmpty()) this[this.size - 1] else null
}

/**
 * Take first N items or less if list is shorter
 * Like regular take() but never crashes
 *
 * Example:
 * listOf("a", "b", "c").takeSafe(2) -> ["a", "b"]
 * listOf("a").takeSafe(5) -> ["a"] (doesn't crash!)
 */
fun <T> List<T>.takeSafe(count: Int): List<T> {
    return this.take(count.coerceAtMost(this.size))
}

/**
 * Remove duplicates from list while preserving order
 *
 * Example:
 * listOf("a", "b", "a", "c").removeDuplicates() -> ["a", "b", "c"]
 */
fun <T> List<T>.removeDuplicates(): List<T> {
    return this.distinct()
}

/**
 * Split list into chunks of specified size
 * Like cutting a pizza into equal slices!
 *
 * Example:
 * listOf(1, 2, 3, 4, 5).chunked(2) -> [[1,2], [3,4], [5]]
 */
fun <T> List<T>.chunkedSafe(size: Int): List<List<T>> {
    if (size <= 0) return listOf(this)
    return this.chunked(size)
}

/**
 * Find all items that match a condition and limit results
 *
 * Example:
 * listOf(1,2,3,4,5,6).filterLimit({ it > 2 }, 2) -> [3, 4]
 * (finds numbers > 2, but only returns first 2 results)
 */
fun <T> List<T>.filterLimit(predicate: (T) -> Boolean, limit: Int): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (result.size >= limit) break
        if (predicate(item)) result.add(item)
    }
    return result
}

/**
 * Shuffle list and return new shuffled copy (original list unchanged)
 * Like shuffling a deck of cards!
 *
 * Example:
 * listOf(1,2,3).shuffled() -> [3,1,2] (random order)
 */
fun <T> List<T>.shuffled(): List<T> {
    return this.shuffled()
}

/**
 * Convert list to comma-separated string
 *
 * Example:
 * listOf("apple", "banana", "orange").toCommaSeparated()
 * -> "apple, banana, orange"
 */
fun <T> List<T>.toCommaSeparated(): String {
    return this.joinToString(", ")
}

/**
 * Count items that match a condition
 *
 * Example:
 * listOf(1,2,3,4,5).countWhere { it > 3 } -> 2 (because 4 and 5 are > 3)
 */
fun <T> List<T>.countWhere(predicate: (T) -> Boolean): Int {
    return this.count(predicate)
}

/**
 * Check if all items are the same
 *
 * Example:
 * listOf("a", "a", "a").allSame() -> true
 * listOf("a", "b", "a").allSame() -> false
 */
fun <T> List<T>.allSame(): Boolean {
    if (this.isEmpty()) return true
    val first = this.first()
    return this.all { it == first }
}

/**
 * Partition list into two lists based on condition
 * Returns Pair(matching items, non-matching items)
 *
 * Example:
 * listOf(1,2,3,4,5).partitionBy { it > 3 }
 * -> Pair([4,5], [1,2,3])
 */
fun <T> List<T>.partitionBy(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    return this.partition(predicate)
}

/**
 * Map each item to a new value, but remove nulls from result
 *
 * Example:
 * listOf("1", "abc", "2").mapNotNull { it.toIntOrNull() } -> [1, 2]
 * ("abc" can't convert to Int, so it's removed)
 */
fun <T, R> List<T>.mapNotNull(transform: (T) -> R?): List<R> {
    return this.mapNotNull(transform)
}

/**
 * Create a frequency map showing how many times each item appears
 *
 * Example:
 * listOf("a", "b", "a", "c", "a").frequencies()
 * -> {"a": 3, "b": 1, "c": 1}
 */
fun <T> List<T>.frequencies(): Map<T, Int> {
    return this.groupingBy { it }.eachCount()
}

/**
 * Get the most common item in the list
 *
 * Example:
 * listOf("a", "b", "a", "c", "a").mostCommon() -> "a"
 */
fun <T> List<T>.mostCommon(): T? {
    if (this.isEmpty()) return null
    return this.groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
}

/**
 * Swap two elements at given indices
 * Returns new list with swapped elements
 *
 * Example:
 * listOf("a", "b", "c").swap(0, 2) -> ["c", "b", "a"]
 */
fun <T> List<T>.swap(index1: Int, index2: Int): List<T> {
    if (index1 !in 0 until size || index2 !in 0 until size) return this
    val mutableList = this.toMutableList()
    val temp = mutableList[index1]
    mutableList[index1] = mutableList[index2]
    mutableList[index2] = temp
    return mutableList
}

/**
 * Replace item at index with new value
 * Returns new list with replaced item
 *
 * Example:
 * listOf("a", "b", "c").replaceAt(1, "NEW") -> ["a", "NEW", "c"]
 */
fun <T> List<T>.replaceAt(index: Int, newValue: T): List<T> {
    if (index !in 0 until size) return this
    val mutableList = this.toMutableList()
    mutableList[index] = newValue
    return mutableList
}

/**
 * Add item at specific position
 * Returns new list with item inserted
 *
 * Example:
 * listOf("a", "c").insertAt(1, "b") -> ["a", "b", "c"]
 */
fun <T> List<T>.insertAt(index: Int, item: T): List<T> {
    val mutableList = this.toMutableList()
    mutableList.add(index.coerceIn(0, size), item)
    return mutableList
}

/**
 * Remove item at specific position
 * Returns new list without that item
 *
 * Example:
 * listOf("a", "b", "c").removeAt(1) -> ["a", "c"]
 */
fun <T> List<T>.removeAt(index: Int): List<T> {
    if (index !in 0 until size) return this
    val mutableList = this.toMutableList()
    mutableList.removeAt(index)
    return mutableList
}
