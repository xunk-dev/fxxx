package com.frameout.core.pro

import com.frameout.core.R
import com.frameout.core.edit.FilterPresets
import com.frameout.core.util.EditStrings
import com.frameout.core.util.StringSource

enum class ShowcaseKind { Accent, Filter, Shapes, Format, Smart, Export, Tool, Collage, Batch, Compare, Icons, Insights }

object ProShowcase {
    private const val SEPARATOR = "|"
    private const val HIDDEN_SHAPES = 8

    fun kindOf(feature: ProFeature): ShowcaseKind? = when (feature) {
        ProFeature.Accents -> ShowcaseKind.Accent
        ProFeature.Filters -> ShowcaseKind.Filter
        ProFeature.Shapes -> ShowcaseKind.Shapes
        ProFeature.ExportFormats -> ShowcaseKind.Format
        ProFeature.SmartExtract -> ShowcaseKind.Smart
        ProFeature.AdvancedExport -> ShowcaseKind.Export
        ProFeature.EditTools -> ShowcaseKind.Tool
        ProFeature.Collage -> ShowcaseKind.Collage
        ProFeature.Batch -> ShowcaseKind.Batch
        ProFeature.Compare -> ShowcaseKind.Compare
        ProFeature.AppIcons -> ShowcaseKind.Icons
        ProFeature.Insights -> ShowcaseKind.Insights
        else -> null
    }

    fun hiddenCount(kind: ShowcaseKind): Int = if (kind == ShowcaseKind.Shapes) HIDDEN_SHAPES else 0

    fun parse(raw: String): List<String> = raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }

    fun proFilterIds(): List<String> = ProCatalog.orderedFilters(FilterPresets.All.map { it.id }).filter { !ProCatalog.filterIsFree(it) }

    fun includedNames(kind: ShowcaseKind): List<String> = when (kind) {
        ShowcaseKind.Filter -> proFilterIds().map { EditStrings.filterName(it) }
        ShowcaseKind.Accent -> raw(R.string.pro_inc_accents)
        ShowcaseKind.Shapes -> raw(R.string.pro_inc_shapes)
        ShowcaseKind.Format -> raw(R.string.pro_inc_formats)
        ShowcaseKind.Smart -> raw(R.string.pro_inc_smart)
        ShowcaseKind.Export -> raw(R.string.pro_inc_adv_export)
        ShowcaseKind.Tool -> raw(R.string.pro_inc_edit_tools)
        ShowcaseKind.Collage -> names(
            R.string.collage_tpl_film_strip, R.string.collage_tpl_hero, R.string.collage_tpl_mosaic, R.string.collage_tpl_contact_sheet,
            R.string.collage_spacing, R.string.collage_corners, R.string.collage_aspect
        )
        ShowcaseKind.Batch -> names(R.string.batch_queue, R.string.batch_settings_title, R.string.batch_edit, R.string.batch_start)
        ShowcaseKind.Compare -> raw(R.string.pro_inc_compare)
        ShowcaseKind.Icons -> raw(R.string.pro_inc_icons)
        ShowcaseKind.Insights -> raw(R.string.pro_inc_insights)
    }

    fun smartNames(): List<String> =
        raw(R.string.pro_inc_smart).toMutableList().also { it.add(minOf(2, it.size), StringSource.get(R.string.setting_people_only)) }

    fun smartSharp(): String = StringSource.get(R.string.smart_tag_sharp)

    fun smartBlurry(): String = StringSource.get(R.string.smart_tag_blurry)

    fun smartDup(count: Int): String = StringSource.get(R.string.smart_tag_dup, count)

    fun smartBest(): String = StringSource.get(R.string.smart_tag_best)

    fun smartKeepFirst(): String = StringSource.get(R.string.smart_keep_first)

    fun title(): String = StringSource.get(R.string.pro_included)

    fun more(count: Int): String = StringSource.get(R.string.pro_included_more, count)

    fun caption(): String = StringSource.get(R.string.pro_demo_caption)

    fun tryHint(): String = StringSource.get(R.string.pro_try_hint)

    fun smartCount(): String = StringSource.get(R.string.pro_smart_count)

    fun saveSize(): String = StringSource.get(R.string.pro_save_size)

    fun batchTitle(): String = StringSource.get(R.string.batch_queue)

    fun faceGood(): String = StringSource.get(R.string.face_good)

    fun liveText(): String = StringSource.get(R.string.live_text_label)

    private fun raw(id: Int): List<String> = parse(StringSource.get(id))

    private fun names(vararg ids: Int): List<String> = ids.map { StringSource.get(it) }
}
