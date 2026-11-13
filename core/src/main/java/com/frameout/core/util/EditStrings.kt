package com.frameout.core.util

import com.frameout.core.R

object EditStrings {
    private fun s(id: Int, vararg args: Any): String = StringSource.get(id, *args)
    fun tools() = s(R.string.edit_tools)
    fun discardTitle() = s(R.string.edit_discard_title)
    fun discardBody() = s(R.string.edit_discard_body)
    fun discard() = s(R.string.edit_discard)
    fun keepEditing() = s(R.string.edit_keep_editing)
    fun filters() = s(R.string.edit_filters)
    fun filterStrength() = s(R.string.edit_filter_strength)
    fun resetFilter() = s(R.string.edit_reset_filter)
    fun filterName(id: String) = s(
        when (id) {
            "mono" -> R.string.filter_mono
            "noir" -> R.string.filter_noir
            "silver" -> R.string.filter_silver
            "sepia" -> R.string.filter_sepia
            "vintage" -> R.string.filter_vintage
            "fade" -> R.string.filter_fade
            "warm" -> R.string.filter_warm
            "cool" -> R.string.filter_cool
            "vivid" -> R.string.filter_vivid
            "dramatic" -> R.string.filter_dramatic
            "cinema" -> R.string.filter_cinema
            "sunset" -> R.string.filter_sunset
            "chrome" -> R.string.filter_chrome
            "kodak" -> R.string.filter_kodak
            "polaroid" -> R.string.filter_polaroid
            "matte" -> R.string.filter_matte
            "lomo" -> R.string.filter_lomo
            "tealorange" -> R.string.filter_tealorange
            "golden" -> R.string.filter_golden
            "arctic" -> R.string.filter_arctic
            "neon" -> R.string.filter_neon
            "emerald" -> R.string.filter_emerald
            "infrared" -> R.string.filter_infrared
            "duo_ocean" -> R.string.filter_duo_ocean
            "duo_sunset" -> R.string.filter_duo_sunset
            "duo_rose" -> R.string.filter_duo_rose
            "portra" -> R.string.filter_portra
            "velvia" -> R.string.filter_velvia
            "ektar" -> R.string.filter_ektar
            "cinestill" -> R.string.filter_cinestill
            "bleach" -> R.string.filter_bleach
            "cross" -> R.string.filter_cross
            "moody" -> R.string.filter_moody
            "matcha" -> R.string.filter_matcha
            "pastel" -> R.string.filter_pastel
            "trix" -> R.string.filter_trix
            else -> R.string.filter_none
        }
    )
    fun draw() = s(R.string.edit_draw)
    fun blur() = s(R.string.edit_blur)
    fun pixelate() = s(R.string.edit_pixelate)
    fun solid() = s(R.string.edit_solid)
    fun red() = s(R.string.edit_color_red)
    fun yellow() = s(R.string.edit_color_yellow)
    fun green() = s(R.string.edit_color_green)
    fun blue() = s(R.string.edit_color_blue)
    fun white() = s(R.string.edit_color_white)
    fun black() = s(R.string.edit_color_black)
    fun undoMark() = s(R.string.edit_undo_mark)
    fun redo() = s(R.string.edit_redo)
    fun reset() = s(R.string.edit_reset)
    fun rotateLeft() = s(R.string.edit_rotate_left)
    fun rotateRight() = s(R.string.edit_rotate_right)
    fun resetCrop() = s(R.string.edit_reset_crop)
    fun resetRotate() = s(R.string.edit_reset_rotate)
    fun cropRotate() = s(R.string.edit_crop_rotate)
    fun resetCropRotate() = s(R.string.edit_reset_crop_rotate)
    fun drawArea() = s(R.string.edit_draw_area)
    fun coverArea() = s(R.string.edit_cover_area)
    fun pen() = s(R.string.edit_pen)
    fun rectangle() = s(R.string.edit_rectangle)
    fun strength() = s(R.string.edit_strength)
    fun size() = s(R.string.edit_size)
    fun color() = s(R.string.edit_color)
    fun customColor() = s(R.string.edit_custom_color)
    fun zoomIn() = s(R.string.edit_zoom_in)
    fun zoomOut() = s(R.string.edit_zoom_out)
    fun zoomFit() = s(R.string.edit_zoom_fit)
    fun zoomActual() = s(R.string.edit_zoom_actual)
    fun pixelGrid() = s(R.string.edit_pixel_grid)
    fun decrease() = s(R.string.edit_decrease)
    fun increase() = s(R.string.edit_increase)
    fun resetAdjust() = s(R.string.edit_reset_adjust)
    fun text() = s(R.string.edit_text)
    fun textDefault() = s(R.string.edit_text_default)
    fun textHint() = s(R.string.edit_text_hint)
    fun textAdd() = s(R.string.edit_text_add)
    fun textDelete() = s(R.string.edit_text_delete)
    fun textArea() = s(R.string.edit_text_area)
    fun bold() = s(R.string.edit_text_bold)
    fun italic() = s(R.string.edit_text_italic)
    fun font() = s(R.string.edit_text_font)
    fun fontSans() = s(R.string.edit_font_sans)
    fun fontSerif() = s(R.string.edit_font_serif)
    fun fontMono() = s(R.string.edit_font_mono)
    fun fontCursive() = s(R.string.edit_font_cursive)
    fun opacity() = s(R.string.edit_opacity)
    fun shadow() = s(R.string.edit_shadow)
    fun outline() = s(R.string.edit_outline)
    fun background() = s(R.string.edit_background)
    fun backgroundNone() = s(R.string.edit_background_none)
    fun backgroundDark() = s(R.string.edit_background_dark)
    fun backgroundLight() = s(R.string.edit_background_light)
    fun backgroundCustom() = s(R.string.edit_background_custom)
    fun tabStyle() = s(R.string.edit_tab_style)
    fun adjust() = s(R.string.edit_adjust)
    fun cover() = s(R.string.edit_cover)
    fun brightness() = s(R.string.edit_brightness)
    fun contrast() = s(R.string.edit_contrast)
    fun saturation() = s(R.string.edit_saturation)
}
