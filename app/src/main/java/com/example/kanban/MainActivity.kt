package com.example.kanban

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.kanban.ui.kanban.KanbanFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "Starting MainActivity...")

        try {
            // Use simple layout without toolbar
            setContentView(R.layout.activity_main_simple)
            Log.d("MainActivity", "Layout set successfully")

            if (savedInstanceState == null) {
                supportFragmentManager.commit {
                    replace(R.id.fragment_container, KanbanFragment())
                }
                Log.d("MainActivity", "Fragment added successfully")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            throw e
        }
    }
}