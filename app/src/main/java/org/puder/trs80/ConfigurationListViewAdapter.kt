/*
 * Copyright 2012-2013, Arno Puder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// AsyncTask is deprecated, but replacing it here would change when the screenshots are loaded
// relative to the rest of the binding.
@file:Suppress("DEPRECATION")

package org.puder.trs80

import android.graphics.Bitmap
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.tekle.oss.android.animation.AnimationFactory
import org.puder.trs80.configuration.Configuration
import org.puder.trs80.configuration.ConfigurationManager
import org.puder.trs80.configuration.EmulatorState
import org.puder.trs80.configuration.KeyboardLayout
import org.puder.trs80.drag.ItemTouchHelperAdapter
import java.io.IOException

private const val TAG = "ConfListViewAdapter"

/** Shown wherever a configuration has no value to show. */
private const val NOT_SET_LABEL = "-"

/** The number of disk drives a configuration can hold images for. */
private const val NUM_DISKS = 4

/**
 * Adapter for the list of configurations shown on the home screen.
 *
 * Besides one card per configuration it appends [numColumns] footer spacers, so that the last
 * row of cards can be scrolled clear of the bottom of the screen.
 *
 * @param configurationManager the configurations to show, in the order they are shown.
 * @param listener notified about the actions the user triggers on a card.
 * @param numColumns the number of columns the list is laid out in.
 */
class ConfigurationListViewAdapter(
    private val configurationManager: ConfigurationManager,
    private val listener: ConfigurationItemListener,
    private val numColumns: Int
) : RecyclerView.Adapter<ConfigurationListViewAdapter.Holder>(), ItemTouchHelperAdapter {

    override fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        configurationManager.moveConfiguration(fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onItemDismiss(position: Int) {
        // Cards cannot be swiped away, see ConfigurationItemTouchHelperCallback.
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): Holder =
        // The view type is the layout to inflate, see getItemViewType().
        Holder(LayoutInflater.from(viewGroup.context).inflate(viewType, viewGroup, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (position >= configurationManager.configCount) {
            return
        }
        val card = holder.card ?: return
        val conf = configurationManager.getConfig(position)
        if (holder.adapterPosition != position && card.viewFlipper.displayedChild != 0) {
            // The recycled card is showing its back. Turn it around without animating.
            val context = TRS80Application.getAppContext()
            card.viewFlipper.setInAnimation(context, R.anim.no_animation)
            card.viewFlipper.setOutAnimation(context, R.anim.no_animation)
            card.viewFlipper.displayedChild = 0
        }
        val emulatorState = try {
            configurationManager.getEmulatorState(conf.id)
        } catch (e: IOException) {
            Log.e(TAG, "Cannot create emulator state.", e)
            return
        }

        card.stopButton.visibility = if (emulatorState.hasState()) View.VISIBLE else View.GONE
        card.shareButton.visibility = if (emulatorState.hasXrayState()) View.VISIBLE else View.GONE

        holder.configuration = conf

        // Name
        conf.name.orNull()?.let { name ->
            card.nameFront.text = name
            card.nameBack.text = name
        }

        // Hardware
        card.model.text = when (conf.model) {
            Hardware.MODEL1 -> "Model I"
            Hardware.MODEL3 -> "Model III"
            Hardware.MODEL4 -> "Model 4"
            Hardware.MODEL4P -> "Model 4P"
            else -> NOT_SET_LABEL
        }

        // Disks
        card.disks.text = (0 until NUM_DISKS)
            .count { !conf.getDiskPath(it).orNull().isNullOrEmpty() }
            .toString()

        // Cassette
        card.cassette.setText(
            if (conf.cassettePosition > 0) R.string.cassette_not_rewound
            else R.string.cassette_rewound
        )

        // Sound
        card.sound.setText(
            if (conf.isSoundMuted) R.string.sound_disabled else R.string.sound_enabled
        )

        // Keyboards
        card.keyboardPortrait.text = keyboardLabel(conf.keyboardLayoutPortrait.orNull())
        card.keyboardLandscape.text = keyboardLabel(conf.keyboardLayoutLandscape.orNull())

        // Screenshot
        card.screenshot.setScreenshotBitmap(null)
        loadScreenshotInto(emulatorState, card.screenshot)
    }

    override fun getItemViewType(position: Int): Int =
        if (position >= configurationManager.configCount) R.layout.configuration_footer
        else R.layout.configuration_item

    /**
     * The number of configurations plus the number of columns in the current GridLayout. This
     * guarantees that at least the last configuration_footer will spill over to the next row.
     */
    override fun getItemCount(): Int = configurationManager.configCount + numColumns

    /** Loads the screenshot of [emulatorState] in the background and shows it in [view]. */
    private fun loadScreenshotInto(emulatorState: EmulatorState, view: ScreenshotView) {
        object : AsyncTask<EmulatorState, Void, Bitmap?>() {
            override fun doInBackground(vararg params: EmulatorState): Bitmap? =
                params[0].loadScreenshot()

            override fun onPostExecute(result: Bitmap?) {
                view.setScreenshotBitmap(result)
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, emulatorState)
    }

    /** @return The abbreviated label of [layout], or [NOT_SET_LABEL] if it has none. */
    private fun keyboardLabel(layout: KeyboardLayout?): String {
        val label = when (layout) {
            KeyboardLayout.KEYBOARD_LAYOUT_ORIGINAL -> R.string.keyboard_abbrev_original
            KeyboardLayout.KEYBOARD_LAYOUT_COMPACT -> R.string.keyboard_abbrev_compact
            KeyboardLayout.KEYBOARD_LAYOUT_JOYSTICK -> R.string.keyboard_abbrev_joystick
            KeyboardLayout.KEYBOARD_GAME_CONTROLLER -> R.string.keyboard_abbrev_game_controller
            KeyboardLayout.KEYBOARD_TILT -> R.string.keyboard_abbrev_tilt
            // An external keyboard has no label of its own.
            KeyboardLayout.KEYBOARD_EXTERNAL, null -> return NOT_SET_LABEL
        }
        return TRS80Application.getAppContext().getString(label)
    }

    /**
     * Holds either a configuration card or one of the footer spacers. The spacers have none of
     * the card's views, which is why [card] is null for them.
     */
    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /** The views of the card, or null if this holder is a footer spacer. */
        val card: Card? = (itemView as? RelativeLayout)?.let { Card(it) }

        /** Footers are not draggable. */
        val draggable: Boolean
            get() = card != null

        /** The configuration this holder currently shows. */
        var configuration: Configuration? = null

        init {
            card?.let { card ->
                card.viewFlipper.displayedChild = 0
                val clickListener = View.OnClickListener { view -> onClick(view) }
                itemView.setOnClickListener(clickListener)
                card.infoButton.setOnClickListener(clickListener)
                card.backButton.setOnClickListener(clickListener)
                card.editButton.setOnClickListener(clickListener)
                card.deleteButton.setOnClickListener(clickListener)
                card.runButton.setOnClickListener(clickListener)
                card.stopButton.setOnClickListener(clickListener)
                card.shareButton.setOnClickListener(clickListener)
            }
        }

        private fun onClick(view: View) {
            val position = adapterPosition
            val card = card
            val configuration = configuration
            if (position == NO_POSITION || card == null || configuration == null) {
                return
            }
            // A `when` over conditions rather than over `view.id`: the info button deliberately
            // shares the flip with the back button, but only it shows the one-time hint.
            val id = view.id
            when {
                id == R.id.configuration_info || id == R.id.configuration_back -> {
                    // Showing the hint consumes the tap on the info button.
                    if (id == R.id.configuration_info && listener.showHint()) {
                        return
                    }
                    AnimationFactory.flipTransition(
                        card.viewFlipper, AnimationFactory.FlipDirection.LEFT_RIGHT
                    )
                }
                id == R.id.configuration_edit ->
                    listener.onConfigurationEdit(configuration, position)
                id == R.id.configuration_stop ->
                    listener.onConfigurationStop(configuration, position)
                id == R.id.configuration_delete ->
                    listener.onConfigurationDelete(configuration, position)
                id == R.id.configuration_run ->
                    listener.onConfigurationRun(configuration, position)
                id == R.id.configuration_share ->
                    listener.onConfigurationShare(configuration, position)
                // A tap on the card itself runs the configuration, unless the card is flipped
                // over or the one-time hint consumed the tap.
                else -> if (!listener.showHint() && card.viewFlipper.displayedChild == 0) {
                    listener.onConfigurationRun(configuration, position)
                }
            }
        }
    }

    /** The views of a single configuration card. */
    class Card(itemView: View) {
        val nameFront: TextView = itemView.findViewById(R.id.configuration_name_front)
        val nameBack: TextView = itemView.findViewById(R.id.configuration_name_back)
        val model: TextView = itemView.findViewById(R.id.configuration_model)
        val disks: TextView = itemView.findViewById(R.id.configuration_disks)
        val cassette: TextView = itemView.findViewById(R.id.configuration_cassette)
        val sound: TextView = itemView.findViewById(R.id.configuration_sound)
        val keyboardPortrait: TextView =
            itemView.findViewById(R.id.configuration_keyboard_portrait)
        val keyboardLandscape: TextView =
            itemView.findViewById(R.id.configuration_keyboard_landscape)
        val screenshot: ScreenshotView = itemView.findViewById(R.id.configuration_screenshot)
        val viewFlipper: ViewFlipper = itemView.findViewById(R.id.configuration_view_flipper)
        val infoButton: View = itemView.findViewById(R.id.configuration_info)
        val backButton: View = itemView.findViewById(R.id.configuration_back)
        val editButton: View = itemView.findViewById(R.id.configuration_edit)
        val deleteButton: View = itemView.findViewById(R.id.configuration_delete)
        val runButton: View = itemView.findViewById(R.id.configuration_run)
        val stopButton: View = itemView.findViewById(R.id.configuration_stop)
        val shareButton: View = itemView.findViewById(R.id.configuration_share)
    }
}
