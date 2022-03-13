package dev.mediamod.gui.screen.editor.component

import dev.mediamod.theme.Theme
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.ChildBasedMaxSizeConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.animate
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.dsl.constraint
import gg.essential.elementa.state.BasicState
import gg.essential.universal.ChatColor
import java.awt.Color

class ThemeListItem(private val theme: Theme) : UIContainer() {
    private var action: (Theme.() -> Unit)? = null

    private val textState = BasicState(theme.name)
    private val text = UIText()
        .bindText(textState)
        .constrain {
            color = Color.white.darker().constraint
        } childOf this

    init {
        constrain {
            height = ChildBasedMaxSizeConstraint()
        }

        onMouseClick {
            action?.invoke(theme)

            textState.set("${ChatColor.BOLD}${theme.name}")
            text.animate {
                setColorAnimation(Animations.IN_OUT_QUAD, 0.1f, Color.white.constraint)
            }
        }
    }

    fun unselect() = apply {
        textState.set(theme.name)
        text.animate {
            setColorAnimation(Animations.IN_OUT_QUAD, 0.1f, Color.white.darker().constraint)
        }
    }

    fun onClick(block: Theme.() -> Unit) = apply { this.action = block }
}