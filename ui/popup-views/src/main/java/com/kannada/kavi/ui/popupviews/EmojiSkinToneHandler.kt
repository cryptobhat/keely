package com.kannada.kavi.ui.popupviews

/**
 * EmojiSkinToneHandler - Manages emoji skin tone variants
 *
 * Maps base emojis to their skin tone variants using Unicode modifiers.
 * Skin tone modifiers:
 * - 🏻 LIGHT_SKIN_TONE (\u1F3FB)
 * - 🏼 MEDIUM-LIGHT_SKIN_TONE (\u1F3FC)
 * - 🏽 MEDIUM_SKIN_TONE (\u1F3FD)
 * - 🏾 MEDIUM-DARK_SKIN_TONE (\u1F3FE)
 * - 🏿 DARK_SKIN_TONE (\u1F3FF)
 */
class EmojiSkinToneHandler {

    companion object {
        // Skin tone modifiers
        const val LIGHT = "\uD83C\uDFFB"
        const val MEDIUM_LIGHT = "\uD83C\uDFFC"
        const val MEDIUM = "\uD83C\uDFFD"
        const val MEDIUM_DARK = "\uD83C\uDFFE"
        const val DARK = "\uD83C\uDFFF"

        private val SKIN_TONES = listOf(
            SkinTone("Default", ""),
            SkinTone("🏻 Light", LIGHT),
            SkinTone("🏼 Medium-Light", MEDIUM_LIGHT),
            SkinTone("🏽 Medium", MEDIUM),
            SkinTone("🏾 Medium-Dark", MEDIUM_DARK),
            SkinTone("🏿 Dark", DARK)
        )

        // Map of base emoji to whether they support skin tones
        private val SKIN_TONE_SUPPORTED_EMOJIS = setOf(
            "👋", "🤚", "🖐", "✋", "🖖", "👌", "🤌", "🤏", "✌", "🤞",
            "🫰", "🤟", "🤘", "🤙", "👍", "👎", "✊", "👊", "🤛", "🤜",
            "👏", "🙌", "👐", "🤲", "🤝", "🤜", "🤛", "🙏", "💅", "🦵",
            "🦶", "👂", "👃", "🧠", "🦷", "🦴", "👀", "👁", "👅", "👄",
            "🍼", "🏋", "⛹", "🏌", "🏇", "🧘", "🏄", "🏊", "🤽", "🏌",
            "🚣", "🏩", "🧗", "🚴", "🚵", "🤸", "⛹", "🤺", "🤼", "🤸",
            "🏌", "🏄", "🤿", "👨", "👩", "👧", "👦", "👶", "👴", "👵",
            "💪", "🦾", "🦿", "👣", "🧑", "👨", "👩"
        )

        /**
         * Check if an emoji supports skin tones
         */
        fun supportsSkinTones(emoji: String): Boolean {
            return emoji in SKIN_TONE_SUPPORTED_EMOJIS
        }

        /**
         * Get all skin tone variants of an emoji
         * Returns list of (display_label, emoji_with_tone)
         */
        fun getSkinToneVariants(baseEmoji: String): List<SkinTone> {
            if (!supportsSkinTones(baseEmoji)) {
                return emptyList()
            }

            return SKIN_TONES.map { tone ->
                if (tone.modifier.isEmpty()) {
                    SkinTone(tone.label, baseEmoji)
                } else {
                    SkinTone(tone.label, baseEmoji + tone.modifier)
                }
            }
        }

        /**
         * Get skin tone variants with proper display format
         */
        fun getVariantOptions(baseEmoji: String): List<Pair<String, String>> {
            return getSkinToneVariants(baseEmoji).map { it.label to it.emoji }
        }
    }

    data class SkinTone(
        val label: String,      // e.g., "🏻 Light", "🏼 Medium-Light"
        val modifier: String    // e.g., "\uD83C\uDFFB"
    ) {
        val emoji: String
            get() = modifier
    }
}
