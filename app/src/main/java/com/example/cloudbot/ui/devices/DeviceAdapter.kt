package com.example.cloudbot.ui.devices

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.cloudbot.R
import com.example.cloudbot.data.db.entities.DeviceEntity
import com.example.cloudbot.databinding.ItemDeviceBinding

class DeviceAdapter(
    private val onOpen: (DeviceEntity) -> Unit,
    private val onQuickDownload: (DeviceEntity) -> Unit,
    private val onQuickLearn: (DeviceEntity) -> Unit,
    private val onRename: (DeviceEntity) -> Unit,
    private val onEditIp: (DeviceEntity) -> Unit,
    private val onDelete: (DeviceEntity) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.VH>() {

    private val items = mutableListOf<DeviceEntity>()

    fun submit(list: List<DeviceEntity>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(val b: ItemDeviceBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.b.txtName.text = d.name
        holder.b.txtIp.text = d.ip
        holder.b.txtMeta.text = "Hub: ${d.hubId}"

        holder.b.root.setOnClickListener { onOpen(d) }
        holder.b.btnOpen.setOnClickListener { onOpen(d) }
        holder.b.btnDownload.setOnClickListener { onQuickDownload(d) }
        holder.b.btnLearn.setOnClickListener { onQuickLearn(d) }

        holder.b.btnMenu.setOnClickListener {
            val pm = PopupMenu(holder.b.root.context, holder.b.btnMenu)
            pm.menuInflater.inflate(R.menu.menu_device_item, pm.menu)
            pm.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rename -> { onRename(d); true }
                    R.id.action_edit_ip -> { onEditIp(d); true }
                    R.id.action_delete -> { onDelete(d); true }
                    else -> false
                }
            }
            pm.show()
        }
    }

    override fun getItemCount() = items.size
}
