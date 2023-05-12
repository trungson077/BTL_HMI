package com.example.nextface_android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.nextface_android.model.StaffInfo


class StaffListViewAdapter: RecyclerView.Adapter<StaffListViewAdapter.ViewHolder>() {
    private val data: MutableList<StaffInfo> = mutableListOf()

    class ViewHolder(item: View): RecyclerView.ViewHolder(item) {
        val avatar: ImageView = item.findViewById(R.id.staffAvatar)
        val name: TextView = item.findViewById(R.id.staffName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.staff_item, parent, false)
            .let { ViewHolder(it) }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(data[position]) {
            holder.avatar.setImageBitmap(avatar)
            holder.name.text = "Bệnh nhân $name"
        }
    }

    override fun getItemCount(): Int = data.size

    fun getItem(position: Int) = if (position < data.size && position >= 0)
        data[position] else null

    fun add(staff: StaffInfo) {
        data.withIndex().firstOrNull { it.value.code == staff.code }
            ?.let {
                data[it.index] = staff
                notifyItemChanged(it.index)
            } ?: kotlin.run {
            with(data) {
                add(staff)
                notifyItemInserted(indexOf(staff))
            }
        }
    }

    fun clear() {
        data.clear()
    }
}