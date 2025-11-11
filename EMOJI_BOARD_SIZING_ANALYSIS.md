# Emoji Board Height & Sizing Analysis - Kavi Keyboard

## Summary
Found the emoji board height definition and identified sizing dimensions. The emoji board is 200dp height with FlorisBoard-style design.

## 1. PRIMARY HEIGHT DEFINITION

File: KaviInputMethodService.kt (Line 569)
```
(200 * resources.displayMetrics.density).toInt() // 200dp height
```

Fixed height = 200dp applied to entire emoji board view in LinearLayout.

## 2. EMOJI BOARD CLASS - EmojiBoardView.kt

### Category Bar (at TOP - FlorisBoard style)
- Height: 52dp (52 * density)
- Position: Top of board
- Content: Category icons + scrollable tabs

### Emoji Grid Layout
- Emojis per row: 9 (FlorisBoard standard)
- Emoji size: (width / 9) * 0.75
- Padding: (width / 9) * 0.125 per side
- Row height: emojiSize + (2 * padding)

### Example (360dp width phone):
- Cell width: 360 / 9 = 40dp
- Emoji size: 40 * 0.75 = 30dp
- Row height: 40dp
- Visible rows in 200dp: ~3-4 rows max

## 3. CONTAINER LAYOUT STRUCTURE

Vertical order:
1. Suggestion strip (wrap_content)
2. 4dp separator
3. Clipboard strip (gone by default)
4. Emoji board (200dp FIXED)
5. Keyboard (remaining space)

Total spacing: 8dp container padding + 8dp strip margin + 4dp separator

## 4. POTENTIAL SIZING ISSUES

Issue 1: Fixed height 200dp
- Category bar: 52dp
- Emoji area: 148dp
- Only ~3 full rows visible without scrolling

Issue 2: Category bar is 26% of total height
- Large compared to emoji grid

Issue 3: No dynamic height
- Doesn't change for orientation or screen size
- Hardcoded value

Issue 4: Keyboard visibility toggle
- Emoji board shown = keyboard hidden
- Requires 200dp of space as replacement

## 5. EMOJI SIZING CALCULATIONS

onMeasure() calculations (line 211-230):
- emojiSize = (width / 9) * 0.75
- emojiPadding = (width / 9) * 0.125
- categoryBarHeight = 52 * density
- categoryItemWidth = 90 * density

Drawing (line 345-389):
- Emoji text size: emojiSize * 0.75
- Cell width: width / 9
- Row height: emojiSize + (padding * 2)

## 6. FLORISBOARD COMPARISON

Implemented features:
- Category tabs at TOP
- 9 emojis per row
- Recent emoji tracking
- Smooth scrolling with fling
- Material Design 3 colors

Differences:
- Kavi: 200dp fixed height
- FlorisBoard: Dynamic/configurable height
- Kavi: Category bar 52dp
- FlorisBoard: Variable height

## 7. KEY FILES

1. EmojiBoardView.kt (480 lines)
   - Main implementation
   - Sizing in onMeasure()
   - Touch/scroll handling

2. EmojiData.kt (149 lines)
   - 9 categories
   - Curated emoji lists

3. KaviInputMethodService.kt (2078 lines)
   - Creates emoji board with 200dp
   - Visibility management
   - Line 569: 200dp height

4. ClipboardStripView.kt
   - Related FlorisBoard component

## 8. SIZING SUMMARY

Dimension             | Value           | Type
---------------------|-----------------|------
Total height          | 200dp           | Fixed
Category bar          | 52dp            | Fixed
Emoji grid area       | ~148dp          | Calculated
Emojis per row        | 9               | Fixed
Cell width            | width / 9       | Dynamic
Emoji size            | cellWidth * 0.75| Dynamic
Emoji padding         | cellWidth * 0.125| Dynamic
Row height            | emojiSize + pad*2| Dynamic
Visible rows          | ~3-4            | Variable
Category tab width    | 90dp            | Fixed
Emoji text size       | emojiSize * 0.75| Dynamic

## 9. VISIBILITY MECHANICS

showEmojiBoard() (line 1709):
- emojiBoardView visible
- keyboardView GONE
- suggestionStripView GONE

hideEmojiBoard() (line 1721):
- emojiBoardView GONE
- keyboardView visible
- suggestionStripView visible

## 10. TOUCH HANDLING

findEmojiAt() (line 515):
- Checks if touch in category bar first
- Calculates emoji position from touch
- Accounts for scroll offset
- Returns emoji, row, col

calculateMaxScroll() (line 232):
- Content height: total height - category bar height
- Total emoji height: totalRows * rowHeight
- Max scroll: total - content

