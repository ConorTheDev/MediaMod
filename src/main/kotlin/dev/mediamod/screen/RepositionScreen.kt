package dev.mediamod.screen

import dev.mediamod.config.Configuration
import dev.mediamod.hud.PlayerComponent
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.pixels
import gg.essential.elementa.utils.withAlpha
import java.awt.Color

class RepositionScreen : WindowScreen(ElementaVersion.V1) {
    private val xState = BasicState(Configuration.playerX)
    private val yState = BasicState(Configuration.playerY)

    private val container by UIBlock(Color.black.withAlpha(0.5f))
        .constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf window

    init {
        xState.onSetValue { Configuration.playerX = it }
        yState.onSetValue { Configuration.playerY = it }

        val textContainer = UIContainer()
            .constrain {
                y = CenterConstraint()
                width = 100.percent()
                height = ChildBasedMaxSizeConstraint()
            } childOf container

        UIText("Reposition Player")
            .constrain {
                x = CenterConstraint()
                textScale = 1.5f.pixels()
            } childOf textContainer

        UIText("Drag the player around to change its position. Double click it to reset.")
            .constrain {
                x = CenterConstraint()
                y = SiblingConstraint(5f)

                color = Color.gray.toConstraint()
                textScale = 0.75f.pixels()
            } childOf textContainer

        var dragOffset = 0f to 0f
        var isDragging = false
        PlayerComponent()
            .constrain {
                x = xState.pixels()
                y = yState.pixels()

                width = 150.pixels()
                height = 50.pixels()
            }
            .onMouseClick { event ->
                if (event.clickCount == 2) {
                    // Reset on double click
                    xState.set(5f)
                    yState.set(5f)

                    return@onMouseClick
                }

                isDragging = true
                dragOffset = event.absoluteX to event.absoluteY
            }
            .onMouseRelease {
                isDragging = false
            }
            .onMouseDrag { mouseX, mouseY, mouseButton ->
                if (mouseButton != 0 || !isDragging)
                    return@onMouseDrag

                val absoluteX = mouseX + getLeft()
                val absoluteY = mouseY + getTop()
                val deltaX = absoluteX - dragOffset.first
                val deltaY = absoluteY - dragOffset.second

                dragOffset = absoluteX to absoluteY

                val newX = getLeft() + deltaX
                val newY = getTop() + deltaY

                if (newX >= 0 && newX <= (this@RepositionScreen.width - this.getWidth())) {
                    xState.set(newX)
                }

                if (newY >= 0 && newY <= (this@RepositionScreen.height - this.getHeight())) {
                    yState.set(newY)
                }
            } childOf container
    }

    override fun onScreenClose() {
        Configuration.markDirty()
        super.onScreenClose()
    }
}