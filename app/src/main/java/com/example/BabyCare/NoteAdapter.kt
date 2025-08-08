package com.example.babycare

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class NoteAdapter(
    private val familyId: String,
    private val babyId: String,
    private val notes: ArrayList<Note>,
    private val onCheckedChange: ((Note, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title
        holder.description.text = note.description
        holder.datetime.text = note.datetime

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = note.isCompleted

        val paintFlags = if (note.isCompleted) Paint.STRIKE_THRU_TEXT_FLAG else 0
        holder.title.paintFlags = paintFlags
        holder.description.paintFlags = paintFlags

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            note.isCompleted = isChecked
            onCheckedChange?.invoke(note, isChecked)


            if (note.id.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("families").document(familyId)
                    .collection("notes").document(note.id)
                    .update("isCompleted", isChecked)
            }

            val newFlags = if (isChecked) Paint.STRIKE_THRU_TEXT_FLAG else 0
            holder.title.paintFlags = newFlags
            holder.description.paintFlags = newFlags
        }
    }

    override fun getItemCount(): Int = notes.size

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textNoteTitle)
        val description: TextView = itemView.findViewById(R.id.textNoteDescription)
        val datetime: TextView = itemView.findViewById(R.id.textNoteTime)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkNoteDone)
    }
}
