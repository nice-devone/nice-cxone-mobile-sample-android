package com.nice.cxonechat.sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nice.cxonechat.sample.model.Thread

class ThreadAdapter(
    private var threads: ArrayList<Thread>?,
    private var context: Context,
    private var threadSelectedListener: ThreadSelectedListener
): RecyclerView.Adapter<ThreadAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.thread_item, viewGroup, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return threads?.size!!
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val option = threads!![position]

        viewHolder.threadNumberTextView.text = option.name
        viewHolder.threadLastMessageTextView.text = option.message

        Glide.with(context)
            .load(option.agentImage)
            .into(viewHolder.agentImageView)

        viewHolder.threadItemCardView.setOnClickListener {
            threadSelectedListener.threadSelected(option)
        }
    }

    fun addThreads(options: ArrayList<Thread>) {
        threads = ArrayList()
        threads?.addAll(options)
        notifyDataSetChanged()
    }

    fun updateThread(thread: Thread) {
        val index = threads!!.indexOf(thread)

        if (index >= 0) {
            threads!!.removeAt(index)
            threads!!.add(index, thread)
            notifyItemChanged(index)
        }
    }

    fun removeAt(position: Int) {
        threads?.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getThreads(): ArrayList<Thread> {
        return threads!!
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var threadNumberTextView: TextView = itemView.findViewById(R.id.thread_number_text_view)
        var threadItemCardView: CardView = itemView.findViewById(R.id.thread_item_card_view)
        var threadLastMessageTextView: TextView = itemView.findViewById(R.id.thread_last_message_text_view)
        var agentImageView: ImageView = itemView.findViewById(R.id.agent_image_view)
    }

}

interface ThreadSelectedListener {
    fun threadSelected(thread: Thread)
}
