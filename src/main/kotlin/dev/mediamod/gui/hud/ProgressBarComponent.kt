package dev.mediamod.gui.hud

import dev.mediamod.MediaMod
import dev.mediamod.theme.Theme
import dev.mediamod.utils.setColorAnimated
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.universal.UMatrixStack
import org.apache.commons.lang3.time.DurationFormatUtils
import kotlin.math.min

class ProgressBarComponent : UIBlock(MediaMod.themeManager.currentTheme.colors.progressBarBackground) {
    private val elapsedTextState = BasicState("0:00")
    private val durationTextState = BasicState("0:00")

    private var lastUpdate = 0L

    private val progressBlock = UIBlock(MediaMod.themeManager.currentTheme.colors.progressBar)
        .constrain {
            width = 1.pixels()
            height = 100.percent()
        } childOf this

    private val elapsedText = UIText("0:00", false)
        .constrain {
            x = 2.pixels()
            y = CenterConstraint()
            textScale = 0.7f.pixels()
            color = MediaMod.themeManager.currentTheme.colors.progressBarText.constraint
        } childOf this

    private val durationText = UIText("0:00", false)
        .constrain {
            x = 2.pixels(alignOpposite = true)
            y = CenterConstraint()
            textScale = 0.7f.pixels()
            color = MediaMod.themeManager.currentTheme.colors.progressBarText.constraint
        } childOf this

    init {
        elapsedText.bindText(elapsedTextState)
        durationText.bindText(durationTextState)

        MediaMod.serviceManager.currentTrack.onSetValue {
            it?.let {
                updateProgress(it.elapsed, it.duration)
                lastUpdate = System.currentTimeMillis()
            } ?: progressBlock.setWidth(0.pixels())
        }

        MediaMod.themeManager.onUpdate(this::updateTheme)
        MediaMod.themeManager.onChange(this::updateTheme)

        MediaMod.serviceManager.currentTrack.get()?.let {
            updateProgress(it.elapsed, it.duration)
            lastUpdate = System.currentTimeMillis()
        }
    }

    override fun draw(matrixStack: UMatrixStack) {
        MediaMod.serviceManager.currentTrack.get()?.let {
            if (lastUpdate != 0L && !it.paused) {
                val change = System.currentTimeMillis() - lastUpdate
                val elapsed = it.elapsed + change
                updateProgress(min(elapsed, it.duration), it.duration)
            }
        }

        super.draw(matrixStack)
    }

    private fun updateTheme(theme: Theme) {
        setColorAnimated(theme.colors.progressBarBackground.constraint)
        progressBlock.setColorAnimated(theme.colors.progressBar.constraint)
        elapsedText.setColorAnimated(theme.colors.progressBarText.constraint)
        durationText.setColorAnimated(theme.colors.progressBarText.constraint)
    }

    private fun updateProgress(elapsed: Long, duration: Long) {
        val progress = elapsed / duration.toFloat()
        progressBlock.setWidth((progress * 100).percent())

        updateProgressText(elapsed, duration)
    }

    private fun updateProgressText(elapsed: Long, duration: Long) {
        elapsedTextState.set(DurationFormatUtils.formatDuration(elapsed, "mm:ss"))
        durationTextState.set(DurationFormatUtils.formatDuration(duration, "mm:ss"))
    }
}