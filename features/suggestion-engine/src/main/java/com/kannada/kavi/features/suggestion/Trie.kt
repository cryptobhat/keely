package com.kannada.kavi.features.suggestion

/**
 * Trie - Super Fast Word Search Data Structure
 *
 * A Trie (pronounced "try") is like a tree for storing words.
 * It makes word lookups INCREDIBLY fast - perfect for autocomplete!
 *
 * WHAT IS A TRIE?
 * ===============
 * Imagine an upside-down tree where each path from root to leaf spells a word.
 *
 * Visual example for English words: "cat", "car", "dog"
 *
 *         ROOT
 *        /    \
 *       c      d
 *       |      |
 *       a      o
 *      / \     |
 *     t   r    g
 *    (*)  (*)  (*)
 *
 * (*) = end of word marker
 *
 * WHY USE A TRIE?
 * ===============
 * **Speed Comparison:**
 * - Linear search in array: O(n × m) where n = words, m = word length
 * - Binary search: O(log n × m)
 * - Hash table: O(m) but can't do prefix search
 * - **Trie: O(m)** - fastest for prefix searches!
 *
 * **Example Performance:**
 * - Dictionary: 100,000 words
 * - Array search: 100,000 comparisons
 * - Trie search: ~10 comparisons (average word length)
 * - **10,000× faster!**
 *
 * PERFECT FOR:
 * ============
 * - Autocomplete (type "hel" → find "hello", "help", "helmet")
 * - Spell checking
 * - Word games
 * - Search engines
 * - Dictionaries
 *
 * HOW IT WORKS:
 * =============
 * 1. **Insert "cat":**
 *    - Start at root
 *    - Add child 'c'
 *    - Add child 'a' to 'c'
 *    - Add child 't' to 'a'
 *    - Mark 't' as end-of-word
 *
 * 2. **Insert "car":**
 *    - Start at root
 *    - 'c' already exists → use it
 *    - 'a' already exists → use it
 *    - Add child 'r' to 'a'
 *    - Mark 'r' as end-of-word
 *
 * 3. **Search "ca":**
 *    - Start at root
 *    - Follow 'c' → found
 *    - Follow 'a' → found
 *    - Return all children: ['t', 'r'] = words "cat", "car"
 *
 * MEMORY USAGE:
 * =============
 * - Each node: ~200 bytes (in Kotlin/Java)
 * - 100,000 words × 6 letters avg = 600,000 nodes
 * - Total: ~120 MB
 * - **Optimized**: Can reduce to ~50 MB with compression
 *
 * TIME COMPLEXITY:
 * ================
 * - Insert word: O(m) where m = word length
 * - Search word: O(m)
 * - Find prefix: O(m)
 * - Find all words with prefix: O(m + n) where n = results
 */
class Trie {

    /**
     * Root node - starting point of the tree
     *
     * Like the trunk of a tree, everything grows from here
     */
    private val root = TrieNode()

    /**
     * Total words stored in this Trie
     *
     * Useful for statistics and debugging
     */
    private var wordCount = 0

    /**
     * Insert a word into the Trie
     *
     * @param word The word to insert (e.g., "ನಮಸ್ತೆ", "hello")
     * @param frequency How often this word is used (for ranking)
     */
    fun insert(word: String, frequency: Int = 1) {
        if (word.isEmpty()) return

        var currentNode = root
        val normalizedWord = word.lowercase() // Case-insensitive

        // Walk through each character
        for (char in normalizedWord) {
            // If character node doesn't exist, create it
            if (!currentNode.children.containsKey(char)) {
                currentNode.children[char] = TrieNode()
            }

            // Move to the child node
            currentNode = currentNode.children[char]!!
        }

        // Mark end of word
        if (!currentNode.isEndOfWord) {
            wordCount++
        }
        currentNode.isEndOfWord = true
        currentNode.word = word // Store original casing
        currentNode.frequency = frequency
    }

    /**
     * Search for exact word match
     *
     * @param word The word to search for
     * @return true if word exists, false otherwise
     */
    fun search(word: String): Boolean {
        val node = searchNode(word)
        return node != null && node.isEndOfWord
    }

    /**
     * Check if any word starts with this prefix
     *
     * @param prefix The prefix to search (e.g., "hel")
     * @return true if prefix exists
     *
     * Example:
     * - startsWith("hel") → true (if "hello", "help" exist)
     * - startsWith("xyz") → false (no words start with xyz)
     */
    fun startsWith(prefix: String): Boolean {
        return searchNode(prefix) != null
    }

    /**
     * Find all words that start with the given prefix
     *
     * This is the MAGIC function for autocomplete!
     *
     * @param prefix The prefix to search (e.g., "nam")
     * @param maxResults Maximum number of results to return
     * @return List of matching words sorted by frequency
     *
     * Example:
     * - findWordsWithPrefix("nam") → ["ನಮಸ್ತೆ", "ನಾನು", "ನಮ್ಮ"]
     * - findWordsWithPrefix("ka") → ["ಕನ್ನಡ", "ಕರ್ನಾಟಕ", "ಕಾಫಿ"]
     */
    fun findWordsWithPrefix(prefix: String, maxResults: Int = 10): List<Pair<String, Int>> {
        if (prefix.isEmpty()) return emptyList()

        val normalizedPrefix = prefix.lowercase()
        val prefixNode = searchNode(normalizedPrefix) ?: return emptyList()

        // Collect all words under this prefix node
        val results = mutableListOf<Pair<String, Int>>()
        collectWords(prefixNode, results)

        // Sort by frequency (most common first) and limit results
        return results
            .sortedByDescending { it.second } // Sort by frequency
            .take(maxResults)
    }

    /**
     * Get word frequency
     *
     * @param word The word to check
     * @return Frequency count, or 0 if word doesn't exist
     */
    fun getFrequency(word: String): Int {
        val node = searchNode(word)
        return if (node != null && node.isEndOfWord) {
            node.frequency
        } else {
            0
        }
    }

    /**
     * Update word frequency
     *
     * Call this when user types a word to increase its rank.
     *
     * @param word The word to update
     * @param increment How much to increase frequency (default 1)
     */
    fun incrementFrequency(word: String, increment: Int = 1) {
        val node = searchNode(word)
        if (node != null && node.isEndOfWord) {
            node.frequency += increment
        } else {
            // Word doesn't exist, insert it
            insert(word, increment)
        }
    }

    /**
     * Delete a word from the Trie
     *
     * @param word The word to delete
     * @return true if deleted, false if word didn't exist
     */
    fun delete(word: String): Boolean {
        if (word.isEmpty()) return false

        val normalizedWord = word.lowercase()
        return deleteHelper(root, normalizedWord, 0)
    }

    /**
     * Clear all words from the Trie
     *
     * Resets to empty state
     */
    fun clear() {
        root.children.clear()
        wordCount = 0
    }

    /**
     * Get total number of words
     *
     * @return Word count
     */
    fun size(): Int = wordCount

    /**
     * Check if Trie is empty
     *
     * @return true if no words stored
     */
    fun isEmpty(): Boolean = wordCount == 0

    // ==================== Private Helper Functions ====================

    /**
     * Search for a node representing the last character of the word/prefix
     *
     * @param word The word or prefix to search
     * @return The node if found, null otherwise
     */
    private fun searchNode(word: String): TrieNode? {
        if (word.isEmpty()) return null

        var currentNode = root
        val normalizedWord = word.lowercase()

        for (char in normalizedWord) {
            val childNode = currentNode.children[char] ?: return null
            currentNode = childNode
        }

        return currentNode
    }

    /**
     * Recursively collect all words from a node
     *
     * @param node Starting node
     * @param results List to collect results into
     */
    private fun collectWords(node: TrieNode, results: MutableList<Pair<String, Int>>) {
        // If this node marks end of word, add it
        if (node.isEndOfWord && node.word != null) {
            results.add(Pair(node.word!!, node.frequency))
        }

        // Recursively collect from all children
        for ((_, childNode) in node.children) {
            collectWords(childNode, results)
        }
    }

    /**
     * Recursive helper to delete a word
     *
     * @param node Current node
     * @param word Word to delete
     * @param index Current character index
     * @return true if current node should be deleted
     */
    private fun deleteHelper(node: TrieNode, word: String, index: Int): Boolean {
        if (index == word.length) {
            // Reached end of word
            if (!node.isEndOfWord) {
                return false // Word doesn't exist
            }

            node.isEndOfWord = false
            node.word = null
            wordCount--

            // If node has no children, it can be deleted
            return node.children.isEmpty()
        }

        val char = word[index]
        val childNode = node.children[char] ?: return false

        val shouldDeleteChild = deleteHelper(childNode, word, index + 1)

        if (shouldDeleteChild) {
            node.children.remove(char)
            // Return true if current node can also be deleted
            return node.children.isEmpty() && !node.isEndOfWord
        }

        return false
    }

    /**
     * TrieNode - Individual Node in the Trie
     *
     * Each node represents one character in a word path.
     */
    private class TrieNode {
        /**
         * Child nodes - maps character to next node
         *
         * Example: 'c' → TrieNode for 'a' → TrieNode for 't'
         */
        val children = mutableMapOf<Char, TrieNode>()

        /**
         * Is this the end of a valid word?
         *
         * Example: In "cat", the 't' node has isEndOfWord = true
         */
        var isEndOfWord = false

        /**
         * The complete word (with original casing)
         *
         * Stored only at end-of-word nodes
         */
        var word: String? = null

        /**
         * How often this word appears/is used
         *
         * Higher frequency = show first in suggestions
         */
        var frequency: Int = 0
    }
}

/**
 * USAGE EXAMPLE:
 * ==============
 *
 * Building a dictionary:
 * ```kotlin
 * val trie = Trie()
 *
 * // Insert Kannada words
 * trie.insert("ನಮಸ್ತೆ", frequency = 100) // Very common
 * trie.insert("ನಾನು", frequency = 50)
 * trie.insert("ನಮ್ಮ", frequency = 30)
 * trie.insert("ಕನ್ನಡ", frequency = 80)
 * trie.insert("ಕರ್ನಾಟಕ", frequency = 40)
 *
 * // Search exact word
 * val exists = trie.search("ನಮಸ್ತೆ") // true
 *
 * // Check prefix
 * val hasPrefix = trie.startsWith("ನಮ") // true
 *
 * // Get autocomplete suggestions
 * val suggestions = trie.findWordsWithPrefix("ನಮ", maxResults = 5)
 * // Result: [("ನಮಸ್ತೆ", 100), ("ನಮ್ಮ", 30)]
 *
 * // User typed a word - increase its frequency
 * trie.incrementFrequency("ನಾನು")
 * // Now frequency is 51
 * ```
 *
 * PERFORMANCE TIPS:
 * =================
 * 1. **Load dictionary once** at startup (not on every search)
 * 2. **Use background thread** for building Trie (can take 1-2 seconds for 100k words)
 * 3. **Cache frequent searches** - many users type similar words
 * 4. **Limit results** to 5-10 suggestions (user won't read more anyway)
 * 5. **Persist user frequencies** to database (learn from user behavior)
 */
