package com.example.babycare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class AllNotesGroupedAdapter(
    private val groups: List<NotesGroup>,
    private val onDelete: (Note, NotesGroup) -> Unit
) : RecyclerView.Adapter<AllNotesGroupedAdapter.GroupedNoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupedNoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_grouped, parent, false)
        return GroupedNoteViewHolder(view)
    }

    override fun getItemCount() = groups.size

    override fun onBindViewHolder(holder: GroupedNoteViewHolder, position: Int) {
        holder.bind(groups[position])
    }

    inner class GroupedNoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val header = view.findViewById<TextView>(R.id.txtBabyHeader)
        private val container = view.findViewById<LinearLayout>(R.id.noteContainer)

        fun bind(group: NotesGroup) {
            header.text = group.babyName
            container.removeAllViews()
            for (note in group.notes) {
                val noteView = LayoutInflater.from(container.context)
                    .inflate(R.layout.item_note_single_line, container, false)
                noteView.findViewById<TextView>(R.id.tvNoteTitle).text = note.title
                noteView.findViewById<TextView>(R.id.tvNoteDesc).text = note.description
                noteView.findViewById<TextView>(R.id.tvNoteTime).text = note.datetime
                noteView.findViewById<Button>(R.id.btnDeleteNote).setOnClickListener {
                    onDelete(note, group)
                }
                container.addView(noteView)
            }
        }
    }
}
