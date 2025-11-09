package com.kannada.kavi.core.layout

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.kannada.kavi.core.common.Result
import com.kannada.kavi.core.common.resultError
import com.kannada.kavi.core.common.resultSuccess
import com.kannada.kavi.core.layout.models.*
import java.io.InputStreamReader

/**
 * LayoutLoader - Loads Keyboard Layouts from JSON Files
 *
 * This class is like a translator that reads JSON files and converts them
 * into KeyboardLayout objects that our app can understand.
 *
 * Think of it like reading a recipe (JSON) and preparing the actual dish (KeyboardLayout)!
 *
 * JSON files are stored in: assets/layouts/
 * - phonetic.json
 * - kavi.json
 * - qwerty.json
 */
class LayoutLoader(private val context: Context) {

    private val gson = Gson()

    /**
     * Load a layout from JSON file
     *
     * @param fileName Name of the JSON file (e.g., "phonetic.json")
     * @return Result containing the loaded KeyboardLayout or an error
     *
     * Example:
     * ```
     * val result = layoutLoader.loadLayout("phonetic.json")
     * when (result) {
     *     is Result.Success -> useLayout(result.data)
     *     is Result.Error -> showError(result.exception)
     * }
     * ```
     */
    fun loadLayout(fileName: String): Result<KeyboardLayout> {
        return try {
            // Step 1: Read the JSON file from assets
            val jsonString = readJsonFromAssets(fileName)

            // Step 2: Parse JSON to KeyboardLayout object
            val layout = parseLayout(jsonString, fileName)

            // Step 3: Validate the layout
            validateLayout(layout)

            resultSuccess(layout)
        } catch (e: Exception) {
            resultError("Failed to load layout '$fileName': ${e.message}")
        }
    }

    /**
     * Load multiple layouts at once
     *
     * @param fileNames List of JSON file names
     * @return Result containing list of loaded layouts
     */
    fun loadLayouts(fileNames: List<String>): Result<List<KeyboardLayout>> {
        return try {
            val layouts = fileNames.mapNotNull { fileName ->
                when (val result = loadLayout(fileName)) {
                    is Result.Success -> result.data
                    is Result.Error -> {
                        // Log error but continue loading other layouts
                        println("Error loading $fileName: ${result.exception.message}")
                        null
                    }
                }
            }

            if (layouts.isEmpty()) {
                resultError("Failed to load any layouts")
            } else {
                resultSuccess(layouts)
            }
        } catch (e: Exception) {
            resultError("Failed to load layouts: ${e.message}")
        }
    }

    /**
     * Load all available layouts from assets/layouts/ folder
     */
    fun loadAllLayouts(): Result<List<KeyboardLayout>> {
        return try {
            // List all JSON files in layouts folder
            val layoutFiles = context.assets.list("layouts")
                ?.filter { it.endsWith(".json") }
                ?: emptyList()

            if (layoutFiles.isEmpty()) {
                return resultError("No layout files found in assets/layouts/")
            }

            // Load each layout
            loadLayouts(layoutFiles.map { "layouts/$it" })
        } catch (e: Exception) {
            resultError("Failed to list layout files: ${e.message}")
        }
    }

    /**
     * Read JSON file from assets folder
     */
    private fun readJsonFromAssets(fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val reader = InputStreamReader(inputStream)
        return reader.readText().also {
            reader.close()
            inputStream.close()
        }
    }

    /**
     * Parse JSON string to KeyboardLayout object
     *
     * This is where the magic happens! We convert JSON text into Kotlin objects.
     */
    private fun parseLayout(jsonString: String, fileName: String): KeyboardLayout {
        // Parse JSON to JsonObject first (gives us more control)
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject

        // Extract basic information
        val id = extractId(fileName)
        val name = jsonObject.get("name")?.asString ?: "Unknown Layout"
        val description = jsonObject.get("description")?.asString ?: ""
        val version = jsonObject.get("version")?.asString ?: "1.0"
        val language = jsonObject.get("language")?.asString ?: "en"
        val transliterationEnabled = jsonObject.get("transliteration_enabled")?.asBoolean ?: false

        // Extract layers (the heart of the layout!)
        val layers = parseLayersWithInheritance(jsonObject, fileName)
        if (layers.isEmpty()) {
            throw IllegalStateException("Layout '$fileName' must define layers or inherit them using 'layers_from'")
        }

        // Extract transliteration rules (if enabled)
        val transliterationRules = if (transliterationEnabled) {
            when {
                jsonObject.has("transliteration_rules") ->
                    parseTransliterationRules(jsonObject.getAsJsonObject("transliteration_rules"))
                jsonObject.has("transliteration_rules_file") ->
                    loadTransliterationRulesFromFile(
                        resolveAssetPath(fileName, jsonObject.get("transliteration_rules_file").asString)
                    )
                else -> null
            }
        } else null

        // Create and return the KeyboardLayout
        return KeyboardLayout(
            id = id,
            name = name,
            description = description,
            version = version,
            language = language,
            layers = layers,
            transliterationEnabled = transliterationEnabled,
            transliterationRules = transliterationRules
        )
    }

    /**
     * Extract layout ID from filename
     * Example: "phonetic.json" → "phonetic"
     */
    private fun extractId(fileName: String): String {
        return fileName
            .substringAfterLast("/")  // Remove path
            .substringBeforeLast(".") // Remove extension
    }

    /**
     * Parse all layers from JSON
     *
     * Layers JSON structure:
     * {
     *   "default": { "rows": [...] },
     *   "shift": { "rows": [...] },
     *   "symbols": { "rows": [...] }
     * }
     */
    private fun parseLayers(layersJson: JsonObject): Map<String, List<KeyboardRow>> {
        val layers = mutableMapOf<String, List<KeyboardRow>>()

        // Iterate through each layer (default, shift, symbols, etc.)
        for ((layerName, layerData) in layersJson.entrySet()) {
            val layerObject = layerData.asJsonObject
            val rowsArray = layerObject.getAsJsonArray("rows")

            // Parse all rows in this layer
            val rows = rowsArray.map { rowElement ->
                val rowObject = rowElement.asJsonObject
                val keysArray = rowObject.getAsJsonArray("keys")

                // Parse all keys in this row
                val keys = keysArray.map { keyElement ->
                    parseKey(keyElement.asJsonObject)
                }

                // Create KeyboardRow
                KeyboardRow(keys = keys)
            }

            layers[layerName] = rows
        }

        return layers
    }

    /**
     * Parse layers from current layout, optionally inheriting from another layout file.
     */
    private fun parseLayersWithInheritance(
        jsonObject: JsonObject,
        fileName: String,
        visited: MutableSet<String> = mutableSetOf()
    ): Map<String, List<KeyboardRow>> {
        if (visited.contains(fileName)) {
            throw IllegalStateException("Circular layer inheritance detected at '$fileName'")
        }

        val layers = mutableMapOf<String, List<KeyboardRow>>()
        visited.add(fileName)

        val inheritKey = when {
            jsonObject.has("layers_from") -> "layers_from"
            jsonObject.has("base_layout") -> "base_layout"
            else -> null
        }

        if (inheritKey != null) {
            val basePath = resolveAssetPath(fileName, jsonObject.get(inheritKey).asString)
            val baseJson = JsonParser.parseString(readJsonFromAssets(basePath)).asJsonObject
            layers.putAll(parseLayersWithInheritance(baseJson, basePath, visited))
        }

        if (jsonObject.has("layers")) {
            layers.putAll(parseLayers(jsonObject.getAsJsonObject("layers")))
        }

        visited.remove(fileName)
        return layers
    }

    /**
     * Parse a single key from JSON
     *
     * Key JSON structure:
     * {
     *   "label": "ಅ",
     *   "output": "ಅ",
     *   "type": "character",
     *   "width": 1.0
     * }
     */
    private fun parseKey(keyJson: JsonObject): Key {
        val label = keyJson.get("label")?.asString ?: ""
        val output = keyJson.get("output")?.asString ?: ""
        val typeString = keyJson.get("type")?.asString ?: "character"
        val width = keyJson.get("width")?.asFloat ?: 1.0f

        // Convert type string to KeyType enum
        val type = parseKeyType(typeString)

        // Parse long-press keys if available
        val longPressKeys = if (keyJson.has("longPress")) {
            val longPressArray = keyJson.getAsJsonArray("longPress")
            longPressArray.map { it.asString }
        } else {
            null
        }

        return Key(
            label = label,
            output = output,
            type = type,
            width = width,
            longPressKeys = longPressKeys
        )
    }

    /**
     * Convert string to KeyType enum
     * Example: "shift" → KeyType.SHIFT
     */
    private fun parseKeyType(typeString: String): KeyType {
        return when (typeString.lowercase()) {
            "character" -> KeyType.CHARACTER
            "shift" -> KeyType.SHIFT
            "delete" -> KeyType.DELETE
            "enter" -> KeyType.ENTER
            "space" -> KeyType.SPACE
            "symbols" -> KeyType.SYMBOLS
            "symbols_extra" -> KeyType.SYMBOLS_EXTRA
            "symbols_alt" -> KeyType.SYMBOLS_ALT
            "default" -> KeyType.DEFAULT
            "language" -> KeyType.LANGUAGE
            "emoji" -> KeyType.EMOJI
            "voice" -> KeyType.VOICE
            "settings" -> KeyType.SETTINGS
            else -> KeyType.CHARACTER
        }
    }

    /**
     * Parse transliteration rules from JSON
     *
     * Rules JSON structure:
     * {
     *   "ka": "ಕ",
     *   "namaste": "ನಮಸ್ತೆ",
     *   "kannada": "ಕನ್ನಡ"
     * }
     */
    private fun parseTransliterationRules(rulesJson: JsonObject): Map<String, String> {
        val rules = mutableMapOf<String, String>()

        for ((key, value) in rulesJson.entrySet()) {
            rules[key.lowercase()] = value.asString
        }

        return rules
    }

    /**
     * Load transliteration rules from an external file (can contain nested objects).
     */
    private fun loadTransliterationRulesFromFile(path: String): Map<String, String>? {
        return try {
            val jsonElement = JsonParser.parseString(readJsonFromAssets(path))
            val rules = mutableMapOf<String, String>()
            collectTransliterationRules(jsonElement, rules)
            if (rules.isEmpty()) null else rules
        } catch (e: Exception) {
            println("Failed to load transliteration rules from '$path': ${e.message}")
            null
        }
    }

    private fun collectTransliterationRules(element: JsonElement, out: MutableMap<String, String>) {
        when {
            element.isJsonObject -> {
                element.asJsonObject.entrySet().forEach { (key, value) ->
                    if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        out[key.lowercase()] = value.asString
                    } else {
                        collectTransliterationRules(value, out)
                    }
                }
            }
        }
    }

    /**
     * Resolve a relative asset path based on the current file location.
     */
    private fun resolveAssetPath(currentFile: String, relativePath: String): String {
        val cleanedRelativePath = relativePath.trim().trimStart('/')
        if (cleanedRelativePath.contains("/")) {
            return cleanedRelativePath
        }

        val baseDir = currentFile.substringBeforeLast("/", missingDelimiterValue = "")
        return if (baseDir.isEmpty()) cleanedRelativePath else "$baseDir/$cleanedRelativePath"
    }

    /**
     * Validate that the layout is usable
     * Throws exception if validation fails
     */
    private fun validateLayout(layout: KeyboardLayout) {
        // Check that layout has at least one layer
        if (layout.layers.isEmpty()) {
            throw IllegalStateException("Layout '${layout.name}' has no layers")
        }

        // Check that default layer exists
        if (layout.getDefaultLayer() == null) {
            throw IllegalStateException("Layout '${layout.name}' has no default layer")
        }

        // Check that layers have rows
        layout.layers.forEach { (layerName, rows) ->
            if (rows.isEmpty()) {
                throw IllegalStateException("Layer '$layerName' has no rows")
            }
        }

        // Check that rows have keys
        layout.layers.forEach { (layerName, rows) ->
            rows.forEachIndexed { index, row ->
                if (row.keys.isEmpty()) {
                    throw IllegalStateException("Layer '$layerName', row $index has no keys")
                }
            }
        }
    }
}
