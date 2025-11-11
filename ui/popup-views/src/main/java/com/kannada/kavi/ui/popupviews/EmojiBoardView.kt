package com.kannada.kavi.ui.popupviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.animation.ValueAnimator
import androidx.core.content.ContextCompat
import com.kannada.kavi.features.themes.KeyboardDesignSystem
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import android.content.SharedPreferences

/**
 * EmojiBoardView - FlorisBoard-style Emoji Picker
 *
 * FlorisBoard Design Features:
 * - Category tabs at TOP (horizontal scrollable)
 * - Clean emoji grid below
 * - Recent emojis tracking
 * - Smooth scrolling with fling
 * - Material Design 3 styling
 * - 9 emojis per row for optimal layout
 */
class EmojiBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val density = resources.displayMetrics.density
    private val prefs: SharedPreferences = context.getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)
    
    // Emoji data
    private val allCategories = EmojiData.getCategories().toMutableList()
    private var currentCategoryIndex = 0
    private var searchQuery = ""
    private var filteredEmojis: List<String> = emptyList()
    private var isSearchMode = false
    
    // Recent emojis (max 32)
    private val recentEmojis = mutableListOf<String>()
    
    // Layout
    private var emojiSize = 0f
    private var categoryBarHeight = 0f
    private var emojiPadding = 0f
    private var categoryItemWidth = 0f
    private var emojisPerRow = 9
    private var categoryScrollX = 0f
    private var maxCategoryScrollX = 0f
    
    // Scrolling
    private var scrollY = 0f
    private var maxScrollY = 0f
    private var lastTouchY = 0f
    private var isScrolling = false
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private var flingAnimator: ValueAnimator? = null
    
    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC
    }

    private val categoryBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
    }

    private val categorySelectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
    }

    private val categoryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        color = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        textSize = 12f * density
        isFakeBoldText = true
        isSubpixelText = true
    }

    private val emojiTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        color = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        isSubpixelText = true
    }

    private val emojiPressedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
    }
    
    // Touch feedback
    private var pressedEmojiIndex = -1
    private var pressedEmojiRow = -1
    private var pressedEmojiCol = -1
    
    // Listener
    private var onEmojiSelectedListener: ((String) -> Unit)? = null
    
    fun setOnEmojiSelectedListener(listener: (String) -> Unit) {
        onEmojiSelectedListener = listener
    }
    
    init {
        setWillNotDraw(false)
        setBackgroundColor(KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC)
        loadRecentEmojis()
        updateFilteredEmojis()
    }
    
    /**
     * Load recent emojis from SharedPreferences
     */
    private fun loadRecentEmojis() {
        val recentString = prefs.getString("recent_emojis", "") ?: ""
        recentEmojis.clear()
        recentEmojis.addAll(recentString.split(",").filter { it.isNotEmpty() }.take(32))
        
        // Update recents category
        if (recentEmojis.isNotEmpty() && allCategories.isNotEmpty()) {
            allCategories[0] = EmojiData.EmojiCategory("Recently Used", recentEmojis)
        }
    }
    
    /**
     * Save emoji to recent list
     */
    private fun saveEmojiToRecent(emoji: String) {
        // Remove if already exists
        recentEmojis.remove(emoji)
        // Add to front
        recentEmojis.add(0, emoji)
        // Keep only 32 most recent
        if (recentEmojis.size > 32) {
            recentEmojis.removeAt(recentEmojis.size - 1)
        }
        
        // Save to preferences
        prefs.edit().putString("recent_emojis", recentEmojis.joinToString(",")).apply()
        
        // Update recents category
        if (allCategories.isNotEmpty()) {
            allCategories[0] = EmojiData.EmojiCategory("Recently Used", recentEmojis)
        }
        
        // If we're on recents, refresh
        if (currentCategoryIndex == 0 && !isSearchMode) {
            updateFilteredEmojis()
            invalidate()
        }
    }
    
    /**
     * Update filtered emojis based on search or category
     */
    private fun updateFilteredEmojis() {
        if (isSearchMode && searchQuery.isNotEmpty()) {
            // Search mode - filter all emojis
            filteredEmojis = allCategories.flatMap { it.emojis }
                .filter { emoji ->
                    // Simple search - check if emoji contains search query
                    // In a real implementation, you'd use emoji names/keywords
                    searchQuery.lowercase() in getEmojiKeywords(emoji).lowercase()
                }
                .distinct()
        } else {
            // Category mode
            val category = allCategories.getOrNull(currentCategoryIndex)
            filteredEmojis = category?.emojis ?: emptyList()
        }
        
        // Reset scroll
        scrollY = 0f
        calculateMaxScroll()
    }
    
    /**
     * Get keywords for emoji (simplified - in real app, use emoji database)
     */
    private fun getEmojiKeywords(emoji: String): String {
        // This is a simplified version - in a real app, use emoji database
        // For now, return common keywords based on category
        return when {
            emoji in allCategories[1].emojis -> "smiley face happy sad laugh cry"
            emoji in allCategories[2].emojis -> "animal nature dog cat bird"
            emoji in allCategories[3].emojis -> "food drink eat apple pizza"
            emoji in allCategories[4].emojis -> "activity sport game play"
            emoji in allCategories[5].emojis -> "travel place car plane"
            emoji in allCategories[6].emojis -> "object thing item"
            emoji in allCategories[7].emojis -> "symbol heart star"
            emoji in allCategories[8].emojis -> "flag country"
            else -> ""
        }
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        // Calculate emoji size (9 emojis per row - FlorisBoard style)
        // Optimized sizing: use 85% of cell width for emoji, 7.5% padding on each side
        val cellWidth = width / emojisPerRow.toFloat()
        emojiSize = cellWidth * 0.85f
        emojiPadding = cellWidth * 0.075f
        categoryBarHeight = 52f * density  // Taller tabs like FlorisBoard
        categoryItemWidth = 90f * density  // Fixed width for horizontal scrolling

        // Set emoji text size
        emojiTextPaint.textSize = emojiSize * 0.75f

        // Calculate max category scroll
        val totalCategoryWidth = allCategories.size * categoryItemWidth
        maxCategoryScrollX = (totalCategoryWidth - width).coerceAtLeast(0f)

        setMeasuredDimension(width, height)
        calculateMaxScroll()
    }
    
    private fun calculateMaxScroll() {
        val contentHeight = height - categoryBarHeight
        val emojiRowHeight = emojiSize + emojiPadding * 2
        val totalRows = (filteredEmojis.size / emojisPerRow) + if (filteredEmojis.size % emojisPerRow > 0) 1 else 0
        val totalHeight = totalRows * emojiRowHeight
        maxScrollY = (totalHeight - contentHeight).coerceAtLeast(0f)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw category bar at TOP (FlorisBoard style)
        drawCategoryBar(canvas)

        // Draw emojis
        drawEmojis(canvas)
    }
    
    /**
     * Draw category bar at top (FlorisBoard style)
     */
    private fun drawCategoryBar(canvas: Canvas) {
        val barTop = 0f
        val barBottom = categoryBarHeight

        // Draw category bar background
        canvas.drawRect(0f, barTop, width.toFloat(), barBottom, categoryBarPaint)

        // Save canvas for horizontal scrolling
        canvas.save()
        canvas.clipRect(0f, barTop, width.toFloat(), barBottom)

        // Draw category tabs (horizontally scrollable)
        allCategories.forEachIndexed { index, category ->
            val left = index * categoryItemWidth - categoryScrollX
            val right = left + categoryItemWidth

            // Only draw if visible
            if (right > 0 && left < width) {
                val centerX = (left + right) / 2f
                val centerY = barTop + categoryBarHeight / 2f

                // Draw selected indicator (bottom underline - FlorisBoard style)
                if (index == currentCategoryIndex && !isSearchMode) {
                    val indicatorHeight = 3f * density
                    canvas.drawRect(
                        left + 8f * density,
                        barBottom - indicatorHeight,
                        right - 8f * density,
                        barBottom,
                        categorySelectedPaint
                    )
                }

                // Draw category icon (emoji)
                val categoryEmoji = getCategoryIcon(index)
                val iconSize = 24f * density
                emojiTextPaint.textSize = iconSize
                val iconY = centerY - 8f * density - ((emojiTextPaint.descent() + emojiTextPaint.ascent()) / 2)
                canvas.drawText(categoryEmoji, centerX, iconY, emojiTextPaint)

                // Draw category label
                val categoryLabel = getCategoryLabel(category.name)
                val labelY = centerY + 12f * density - ((categoryTextPaint.descent() + categoryTextPaint.ascent()) / 2)
                canvas.drawText(categoryLabel, centerX, labelY, categoryTextPaint)
            }
        }

        canvas.restore()
    }

    /**
     * Get category icon emoji
     */
    private fun getCategoryIcon(index: Int): String {
        return when (index) {
            0 -> "🕒"  // Recent
            1 -> "😀"  // Smileys
            2 -> "🐶"  // Animals
            3 -> "🍕"  // Food
            4 -> "⚽"  // Activities
            5 -> "✈️"  // Travel
            6 -> "💡"  // Objects
            7 -> "❤️"  // Symbols
            8 -> "🏁"  // Flags
            else -> "😀"
        }
    }

    /**
     * Get category short label
     */
    private fun getCategoryLabel(name: String): String {
        return when {
            name.contains("Recent") -> "Recent"
            name.contains("Smileys") -> "Smileys"
            name.contains("Animals") -> "Animals"
            name.contains("Food") -> "Food"
            name.contains("Activities") -> "Activity"
            name.contains("Travel") -> "Travel"
            name.contains("Objects") -> "Objects"
            name.contains("Symbols") -> "Symbols"
            name.contains("Flags") -> "Flags"
            else -> name.take(8)
        }
    }
    
    /**
     * Draw emojis in grid (below category bar)
     */
    private fun drawEmojis(canvas: Canvas) {
        val startY = categoryBarHeight
        val endY = height.toFloat()
        val emojiRowHeight = emojiSize + emojiPadding * 2

        // Reset emoji text size
        emojiTextPaint.textSize = emojiSize * 0.75f

        // Calculate visible range
        val visibleStart = ((scrollY / emojiRowHeight).toInt()).coerceAtLeast(0)
        val visibleEnd = min(
            visibleStart + ((endY - startY) / emojiRowHeight).toInt() + 2,
            (filteredEmojis.size / emojisPerRow) + 1
        )

        // Draw visible emojis
        for (row in visibleStart until visibleEnd) {
            for (col in 0 until emojisPerRow) {
                val index = row * emojisPerRow + col
                if (index >= filteredEmojis.size) break

                val emoji = filteredEmojis[index]
                val cellWidth = width / emojisPerRow.toFloat()
                val x = col * cellWidth + cellWidth / 2f
                val y = startY + row * emojiRowHeight + emojiRowHeight / 2f - scrollY

                // Only draw if visible
                if (y > startY && y < endY) {
                    // Draw pressed background
                    if (row == pressedEmojiRow && col == pressedEmojiCol) {
                        val rect = RectF(
                            col * cellWidth,
                            y - emojiRowHeight / 2f,
                            (col + 1) * cellWidth,
                            y + emojiRowHeight / 2f
                        )
                        canvas.drawRect(rect, emojiPressedPaint)
                    }

                    val emojiY = y - ((emojiTextPaint.descent() + emojiTextPaint.ascent()) / 2)
                    canvas.drawText(emoji, x, emojiY, emojiTextPaint)
                }
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                isScrolling = false
                pressedEmojiIndex = -1
                pressedEmojiRow = -1
                pressedEmojiCol = -1
                
                // Cancel any ongoing fling
                flingAnimator?.cancel()
                
                // Initialize velocity tracker
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(event)
                
                // Check if category bar was tapped (now at top)
                if (event.y <= categoryBarHeight) {
                    val adjustedX = event.x + categoryScrollX
                    val categoryIndex = (adjustedX / categoryItemWidth).toInt()
                    if (categoryIndex in allCategories.indices) {
                        isSearchMode = false
                        searchQuery = ""
                        currentCategoryIndex = categoryIndex
                        updateFilteredEmojis()

                        // Auto-scroll to make selected category visible
                        val targetScrollX = categoryIndex * categoryItemWidth - width / 2f + categoryItemWidth / 2f
                        categoryScrollX = targetScrollX.coerceIn(0f, maxCategoryScrollX)

                        invalidate()
                        return true
                    }
                }
                
                // Check if emoji was tapped
                val emojiInfo = findEmojiAt(event.x, event.y)
                if (emojiInfo != null) {
                    pressedEmojiRow = emojiInfo.second
                    pressedEmojiCol = emojiInfo.third
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)

                // Only scroll emojis if not in category bar
                if (event.y > categoryBarHeight) {
                    val deltaY = lastTouchY - event.y

                    if (!isScrolling && abs(deltaY) > touchSlop) {
                        isScrolling = true
                        pressedEmojiRow = -1
                        pressedEmojiCol = -1
                        invalidate()
                    }

                    if (isScrolling) {
                        scrollY = (scrollY + deltaY).coerceIn(0f, maxScrollY)
                        lastTouchY = event.y
                        invalidate()
                    }
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.let { tracker ->
                    tracker.computeCurrentVelocity(1000)
                    val velocityY = tracker.yVelocity
                    
                    if (isScrolling && abs(velocityY) > minFlingVelocity) {
                        // Fling
                        startFling(-velocityY)
                    } else if (!isScrolling) {
                        // Check if emoji was tapped
                        val emojiInfo = findEmojiAt(event.x, event.y)
                        if (emojiInfo != null) {
                            val emoji = emojiInfo.first
                            saveEmojiToRecent(emoji)
                            onEmojiSelectedListener?.invoke(emoji)
                        }
                    }
                    
                    tracker.recycle()
                }
                velocityTracker = null
                
                pressedEmojiRow = -1
                pressedEmojiCol = -1
                isScrolling = false
                invalidate()
            }
        }
        
        return true
    }
    
    /**
     * Start fling animation
     */
    private fun startFling(velocity: Float) {
        flingAnimator?.cancel()
        
        val startScroll = scrollY
        val distance = velocity * 0.3f // Damping factor
        
        flingAnimator = ValueAnimator.ofFloat(0f, distance).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val delta = animator.animatedValue as Float
                scrollY = (startScroll + delta).coerceIn(0f, maxScrollY)
                invalidate()
            }
            start()
        }
    }
    
    /**
     * Find emoji at touch coordinates
     * Returns: (emoji, row, col) or null
     */
    private fun findEmojiAt(x: Float, y: Float): Triple<String, Int, Int>? {
        if (y <= categoryBarHeight) return null

        val startY = categoryBarHeight
        val emojiRowHeight = emojiSize + emojiPadding * 2
        val cellWidth = width / emojisPerRow.toFloat()

        val adjustedY = y + scrollY - startY
        val row = (adjustedY / emojiRowHeight).toInt()
        val col = (x / cellWidth).toInt()

        val index = row * emojisPerRow + col
        if (index in filteredEmojis.indices) {
            return Triple(filteredEmojis[index], row, col)
        }

        return null
    }
    
    /**
     * Refresh colors when theme changes
     */
    fun refreshColors() {
        backgroundPaint.color = KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC
        categoryBarPaint.color = KeyboardDesignSystem.Colors.SPECIAL_KEY_BACKGROUND_DYNAMIC
        categorySelectedPaint.color = KeyboardDesignSystem.Colors.ACTION_KEY_BACKGROUND_DYNAMIC
        categoryTextPaint.color = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        emojiTextPaint.color = KeyboardDesignSystem.Colors.KEY_TEXT_DYNAMIC
        emojiPressedPaint.color = KeyboardDesignSystem.Colors.KEY_PRESSED_DYNAMIC
        setBackgroundColor(KeyboardDesignSystem.Colors.KEYBOARD_BACKGROUND_DYNAMIC)
        invalidate()
    }
}
