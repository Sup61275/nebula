package com.example.nebula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nebula.databinding.ItemHistoryBinding
import com.example.nebula.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(private val list: MutableList<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    private var listener: HistoryAdapterInterface? = null

    fun setListener(listener: HistoryAdapterInterface) {
        this.listener = listener
    }

    class HistoryViewHolder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        with(holder) {
            with(list[position]) {
                binding.titleTextView.text = this.title
                binding.urlTextView.text = this.url
                binding.root.setOnClickListener {
                    listener?.onItemClicked(this, position)
                }
                binding.deleteButton.setOnClickListener {
                    listener?.onDeleteItemClicked(this, position)
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size

    interface HistoryAdapterInterface {
        fun onDeleteItemClicked(historyItem: HistoryItem, position: Int)
        fun onItemClicked(historyItem: HistoryItem, position: Int)
    }
}