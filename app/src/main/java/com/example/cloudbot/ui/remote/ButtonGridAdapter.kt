package com.example.cloudbot.ui.remote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.cloudbot.R
import com.example.cloudbot.data.db.entities.ButtonEntity
import com.example.cloudbot.databinding.ItemButtonBinding

class ButtonGridAdapter(
    private val onSend: (ButtonEntity) -> Unit,
    private val onLearn: (ButtonEntity) -> Unit,
    private val onSaveToDb: (ButtonEntity) -> Unit,
    private val onRename: (ButtonEntity) -> Unit,
    private val onDelete: (ButtonEntity) -> Unit
) : RecyclerView.Adapter<ButtonGridAdapter.VH>() {

    private val items = mutableListOf<ButtonEntity>()

    fun submit(list: List<ButtonEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(val b: ItemButtonBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemButtonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val btn = items[position]
        holder.b.txtLabel.text = "${btn.label} - ${btn.signalType}"

        holder.b.btnSend.setOnClickListener { onSend(btn) }
        holder.b.btnLearn.setOnClickListener { onLearn(btn) }

        val hasLearnedSignal = btn.rawJson.isNotBlank() && btn.rawJson.trim() != "[]"
        holder.b.btnSaveDb.visibility = if (hasLearnedSignal) View.VISIBLE else View.GONE
        holder.b.btnSaveDb.setOnClickListener { onSaveToDb(btn) }

        holder.b.btnMenu.setOnClickListener {
            val pm = PopupMenu(holder.b.root.context, holder.b.btnMenu)
            pm.menuInflater.inflate(R.menu.menu_button_item, pm.menu)
            pm.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename_btn -> {
                        onRename(btn)
                        true
                    }
                    R.id.action_delete_btn -> {
                        onDelete(btn)
                        true
                    }
                    else -> false
                }
            }
            pm.show()
        }
    }

    override fun getItemCount() = items.size
}
