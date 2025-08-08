package com.example.babycare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteWithBabyAdapter(
    private val notes: List<NoteWithBaby>
) : RecyclerView.Adapter<NoteWithBabyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textNoteTitle)
        val textDescription: TextView = view.findViewById(R.id.textNoteContent)
        val textBabyName: TextView = view.findViewById(R.id.textNoteBabyName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_with_baby, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notes[position]
        holder.textTitle.text = item.note.title
        holder.textDescription.text = item.note.description
        holder.textBabyName.text = "👶 ${item.babyName}"
    }

    override fun getItemCount(): Int = notes.size
}
