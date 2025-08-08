package com.example.babycare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class AllChecklistsAdapter(
    private val items: MutableList<ChecklistWithBabyName>,
    private val onCheckedChange: (ChecklistItem, Boolean) -> Unit,
    private val onDelete: (ChecklistItem) -> Unit
) : RecyclerView.Adapter<AllChecklistsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textTitle)
        val time: TextView = view.findViewById(R.id.textTime)
        val babyName: TextView = view.findViewById(R.id.textBabyName)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_all_checklist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.checklist.title
        holder.time.text = if (item.checklist.time.isNotBlank()) {
            "🕒 ${item.checklist.time}"
        } else {
            ""
        }

        holder.babyName.text = "👶 ${item.babyName}"
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.checklist.isCompleted

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            onCheckedChange(item.checklist, isChecked)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(item.checklist)
        }

        holder.itemView.setOnClickListener {
            holder.checkBox.performClick()
        }
    }

    override fun getItemCount(): Int = items.size
}
