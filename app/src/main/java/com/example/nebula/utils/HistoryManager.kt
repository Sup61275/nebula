package com.example.nebula.utils

import android.util.Log
import com.example.nebula.model.HistoryItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
class HistoryManager(private val database: DatabaseReference) {
    private val historyRef = database.child("history")

    fun saveHistoryItem(url: String, title: String) {
        val timestamp = System.currentTimeMillis()
        val id = historyRef.push().key ?: return
        val historyItem = HistoryItem(id, url, title, timestamp)

        historyRef.child(id).setValue(historyItem)
            .addOnSuccessListener {
                Log.d("HistoryManager", "History item saved successfully: $historyItem")
            }
            .addOnFailureListener { err ->
                Log.e("HistoryManager", "Error saving history item: ${err.message}")
            }
    }

    fun addRealtimeHistoryListener(callback: (List<HistoryItem>) -> Unit) {
        historyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyItems = mutableListOf<HistoryItem>()
                for (childSnapshot in snapshot.children) {
                    val historyItem = childSnapshot.getValue(HistoryItem::class.java)
                    historyItem?.let { historyItems.add(it) }
                }
                callback(historyItems.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistoryManager", "Error fetching history: ${error.message}")
                callback(emptyList())
            }
        })
    }

    fun deleteHistoryItem(id: String, callback: (Boolean) -> Unit) {
        historyRef.child(id).removeValue()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }
}