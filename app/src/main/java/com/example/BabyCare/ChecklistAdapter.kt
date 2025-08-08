package com.example.babycare

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ChecklistAdapter(
    private val items: MutableList<ChecklistItem>,
    private val familyId: String,
    private val onItemDeleted: () -> Unit = {}
) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_all_checklist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.title.text = item.title
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.isCompleted

        holder.time.visibility = if (item.time.isNotBlank()) {
            holder.time.text = "🕒 שעה: ${item.time}"
            View.VISIBLE
        } else View.GONE

        holder.babyName.visibility = View.GONE

        val paintFlags = if (item.isCompleted) Paint.STRIKE_THRU_TEXT_FLAG else 0
        holder.title.paintFlags = paintFlags

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isCompleted = isChecked

            db.collection("families").document(familyId)
                .collection("checklist").document(item.id)
                .update("isCompleted", isChecked)
        }

        holder.checkBox.contentDescription = "סמן אם ${item.title} בוצעה"

        holder.itemView.setOnClickListener {
            holder.checkBox.performClick()
        }

        holder.btnDelete.setOnClickListener {
            db.collection("families").document(familyId)
                .collection("checklist").document(item.id)
                .delete()
                .addOnSuccessListener {
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    onItemDeleted()
                }
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textTitle)
        val time: TextView = view.findViewById(R.id.textTime)
        val babyName: TextView = view.findViewById(R.id.textBabyName)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
    }
}
