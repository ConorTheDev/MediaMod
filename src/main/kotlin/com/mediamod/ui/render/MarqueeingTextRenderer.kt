package com.mediamod.ui.render

import com.mediamod.ui.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

/**
 * A text renderer that will render the text as a marquee when required
 *
 * @param textX The x position of the text
 * @param textY The y position of the text
 * @param maximumWidth The maximum length in pixels that will be displayed of the string
 * @param maximumHeight The maximum height in pixels that will be displayed of the string
 * @param textColor The colour of the text
 * @param textProgressIncrement The speed of the text scrolling
 * @param text The text to display
 */
class MarqueeingTextRenderer(
    private val textX: Int,
    private val textY: Int,
    private val maximumWidth: Int,
    private val maximumHeight: Int,
    private val textColor: Color = Color.WHITE,
    private val textProgressIncrement: Double = 0.005,
    var text: String = ""
) {
    private val fontRenderer = Minecraft.getMinecraft().fontRendererObj
    private var textProgressPercent = 0.00

    /**
     * Renders the text as a marquee if required, otherwise just draw a static string
     * @param partialTicks The number of "partial ticks" between this tick and the previous tick
     */
    fun render(partialTicks: Float) {
        if (fontRenderer.getStringWidth(text) > 90) {
            val textString = "$text     ${fontRenderer.trimStringToWidth(text, maximumWidth)}"

            val textWidth = fontRenderer.getStringWidth(textString)
            val textProgressPartialTicks = min((textProgressPercent + partialTicks * textProgressIncrement), 1.0)

            RenderUtils.drawScissor(textX, textY, maximumWidth, maximumHeight) {
                RenderUtils.drawText(
                    textString,
                    ((textX - (textProgressPartialTicks * max(0, textWidth - 90)))).toFloat(),
                    textY.toFloat(),
                    textColor
                )
            }
        } else {
            RenderUtils.drawText(text, textX.toFloat(), textY.toFloat(), textColor)
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

        textProgressPercent += textProgressIncrement
        if (textProgressPercent > 1.0 + textProgressIncrement)
            textProgressPercent = 0.0
    }

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }
}