package com.example.babycare

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class ScheduleWithBabyAdapter(
    private val schedules: MutableList<ScheduleWithBaby>,
    private val onDeleteClick: (ScheduleWithBaby) -> Unit,
    private val onItemClick: ((ScheduleWithBaby) -> Unit)? = null
) : RecyclerView.Adapter<ScheduleWithBabyAdapter.ScheduleViewHolder>() {

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvScheduleTitle)
        val tvDesc: TextView = itemView.findViewById(R.id.tvScheduleDesc)
        val tvTime: TextView = itemView.findViewById(R.id.tvScheduleTime)
        val tvFrequency: TextView = itemView.findViewById(R.id.tvScheduleFrequency)
        val tvBaby: TextView = itemView.findViewById(R.id.tvScheduleBaby)
        val checkBox: CheckBox = itemView.findViewById(R.id.cbDone)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION && onItemClick != null) {
                    onItemClick.invoke(schedules[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule_with_baby, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val item = schedules[position]
        val context = holder.itemView.context

        holder.tvTitle.text = item.title
        holder.tvDesc.text = item.description
        holder.tvTime.text = "🕒 ${item.time}"
        holder.tvFrequency.text = "⏳ תדירות: ${item.frequency}"
        holder.tvBaby.text = "👶 ${item.babyName}"

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = item.isDone
        holder.itemView.alpha = if (item.isDone) 0.5f else 1f

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            holder.itemView.alpha = if (isChecked) 0.5f else 1f

            val sharedPref = context.getSharedPreferences("BabyCare", Context.MODE_PRIVATE)
            val familyId = sharedPref.getString("familyId", null)
            if (!familyId.isNullOrEmpty() && item.id.isNotBlank()) {
                val db = FirebaseFirestore.getInstance()
                db.collection("families").document(familyId)
                    .collection("schedule")
                    .document(item.id)
                    .update("isDone", isChecked)
                    .addOnFailureListener {
                        Toast.makeText(context, "שגיאה בעדכון סטטוס", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    override fun getItemCount(): Int = schedules.size
}
