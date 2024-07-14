package com.example.nebula.activity


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nebula.adapter.HistoryAdapter
import com.example.nebula.databinding.ActivityHistoryBinding
import com.example.nebula.model.HistoryItem
import com.example.nebula.utils.HistoryManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
class HistoryActivity : AppCompatActivity(), HistoryAdapter.HistoryAdapterInterface {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var historyManager: HistoryManager
    private var historyItemList: MutableList<HistoryItem> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = FirebaseDatabase.getInstance().reference
        historyManager = HistoryManager(database)

        setupRecyclerView()
        getHistoryFromFirebase()
    }

    private fun setupRecyclerView() {
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(historyItemList)
        adapter.setListener(this)
        binding.historyRecyclerView.adapter = adapter
    }

    private fun getHistoryFromFirebase() {
        historyManager.addRealtimeHistoryListener { historyItems ->
            runOnUiThread {
                historyItemList.clear()
                historyItemList.addAll(historyItems)
                adapter.notifyDataSetChanged()

                if (historyItemList.isEmpty()) {
                    binding.emptyHistoryText.visibility = View.VISIBLE
                } else {
                    binding.emptyHistoryText.visibility = View.GONE
                }
            }
        }
    }

    override fun onDeleteItemClicked(historyItem: HistoryItem, position: Int) {
        historyManager.deleteHistoryItem(historyItem.id) { success ->
            if (success) {
                runOnUiThread {
                    historyItemList.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    Toast.makeText(this, "History item deleted", Toast.LENGTH_SHORT).show()

                    if (historyItemList.isEmpty()) {
                        binding.emptyHistoryText.visibility = View.VISIBLE
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Failed to delete history item", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onItemClicked(historyItem: HistoryItem, position: Int) {
        openWebPage(historyItem.url)
    }

    private fun openWebPage(url: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("url", url)
        startActivity(intent)
        finish()
    }
}