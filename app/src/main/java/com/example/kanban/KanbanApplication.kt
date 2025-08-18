package com.example.kanban

import android.app.Application
import com.example.kanban.data.SimpleRepository

class KanbanApplication : Application() {
    
    val repository by lazy { SimpleRepository(this) }
    
    override fun onCreate() {
        super.onCreate()
    }
}