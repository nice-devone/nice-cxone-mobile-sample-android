package com.nice.cxonechat.sample

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nice.cxonechat.sample.databinding.ThreadItemBinding
import com.nice.cxonechat.sample.model.Thread

class ThreadAdapter(
    private var threads: MutableList<Thread>,
    private var context: Context,
    private var threadSelectedListener: ThreadSelectedListener,
): RecyclerView.Adapter<ThreadAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, p1: Int): ViewHolder {
        val binding = ThreadItemBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return threads.size
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val option = threads[position]

        viewHolder.threadNumberTextView.text = option.name
        viewHolder.threadLastMessageTextView.text = option.lastMessage

        if (option.agentImage.isNotEmpty()) {
            Glide.with(context)
                .load(option.agentImage)
                .into(viewHolder.agentImageView)
        }

        viewHolder.threadItemCardView.setOnClickListener {
            threadSelectedListener.threadSelected(option)
        }
    }

    fun setThreads(options: List<Thread>) {
        val callback = ThreadDiffCallback(threads.toList(), options)
        val result = DiffUtil.calculateDiff(callback, true)
        threads = ArrayList(options)
        result.dispatchUpdatesTo(this)
    }

    fun updateThread(thread: Thread) {
        val index = threads.indexOf(thread)

        if (index >= 0) {
            threads.removeAt(index)
            threads.add(index, thread)
            notifyItemChanged(index)
        }
    }

    fun removeAt(position: Int) {
        threads.removeAt(position)
        notifyItemRemoved(position)
    }

    fun getThreads(): MutableList<Thread> {
        return threads
    }

    class ViewHolder(binding: ThreadItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val threadNumberTextView: TextView = binding.threadNumberTextView
        val threadItemCardView: CardView = binding.threadItemCardView
        val threadLastMessageTextView: TextView = binding.threadLastMessageTextView
        val agentImageView: ImageView = binding.agentImageView
    }

    private class ThreadDiffCallback(
        private val oldList: List<Thread>,
        private val newList: List<Thread>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.name == newItem.name
                    && oldItem.lastMessage == newItem.lastMessage
                    && oldItem.agentImage == newItem.agentImage
        }

    }

}

fun interface ThreadSelectedListener {
    fun threadSelected(thread: Thread)
}
