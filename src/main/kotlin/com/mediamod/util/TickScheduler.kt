/*
 *     MediaMod is a mod for Minecraft which displays information about your current track in-game
 *     Copyright (C) 2021 Conor Byrne
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */


package com.mediamod.util

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * A class for scheduling code to run after a certain amount of ticks
 * @author Conor Byrne (dreamhopping)
 */
object TickScheduler {
    private val tasks = mutableListOf<TickTask>()

    /**
     * Schedules a [Unit] to run after a certain amount of ticks
     *
     * @param ticks The amount of ticks to wait
     * @param unit The code to run
     */
    fun schedule(ticks: Int, unit: () -> Unit) {
        tasks.add(TickTask(ticks, unit))
    }

    @SubscribeEvent
    fun onClientTick(e: TickEvent.ClientTickEvent) {
        if (e.phase != TickEvent.Phase.END) return
        tasks.removeIf { it.attemptToExecute() }
    }

    /**
     * A class which handles the execution of a [Unit] after the specified number of ticks
     */
    class TickTask(var remainingTicks: Int, val unit: () -> Unit) {
        /**
         * Attempts to execute the [unit] supplied in the constructor
         * If there is still some ticks remaining, the [unit] will not be run
         *
         * @return true if the task has been executed, false if we still have to wait
         */
        fun attemptToExecute(): Boolean {
            return if (remainingTicks <= 0) {
                unit()
                true
            } else {
                remainingTicks--
                false
            }
        }
    }

    init {
        MinecraftForge.EVENT_BUS.register(this)
    }
}