package com.example.babycare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BabyAdapter(
    private val babyList: List<Baby>,
    private val onBabyClick: ((Baby) -> Unit)? = null,
    private val onBabyLongClick: ((Baby) -> Unit)? = null
) : RecyclerView.Adapter<BabyAdapter.BabyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BabyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_baby, parent, false)
        return BabyViewHolder(view)
    }

    override fun onBindViewHolder(holder: BabyViewHolder, position: Int) {
        holder.bind(babyList[position])
    }

    override fun getItemCount(): Int = babyList.size

    inner class BabyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewName: TextView = itemView.findViewById(R.id.textName)
        private val textViewDetails: TextView = itemView.findViewById(R.id.textDetails)
        private val imageView: ImageView = itemView.findViewById(R.id.imageViewBaby)

        fun bind(baby: Baby) {
            textViewName.text = baby.name
            textViewDetails.text = "משקל: ${baby.weight} ק\"ג | גובה: ${baby.height} ס\"מ"

            if (!baby.imageUrl.isNullOrEmpty()) {
                Glide.with(imageView.context)
                    .load(baby.imageUrl)
                    .placeholder(R.drawable.ic_person_outline_24)
                    .error(R.drawable.ic_person_outline_24)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_person_outline_24)
            }

            itemView.setOnClickListener { onBabyClick?.invoke(baby) }
            itemView.setOnLongClickListener {
                onBabyLongClick?.invoke(baby)
                true
            }
        }
    }
}
