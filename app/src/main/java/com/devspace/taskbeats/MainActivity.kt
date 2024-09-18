package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskBeatDataBase::class.java, "database-taskbeat"
        ).build()
    }
    private val categoryDao: CategoryDao by lazy {
        db.getCategoryDao()

    }
    private val taskDao: TaskDao by lazy {
        db.getTaskDao()

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        insertDefaultCategory()
        val rvCategory = findViewById<RecyclerView>(R.id.rv_categories)
        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks)

        val taskAdapter = TaskListAdapter()
        val categoryAdapter = CategoryListAdapter()

        categoryAdapter.setOnClickListener { selected ->
            val categoryTemp = categories.map { item ->
                when {
                    item.name == selected.name && !item.isSelected -> item.copy(isSelected = true)
                    item.name == selected.name && item.isSelected -> item.copy(isSelected = false)
                    else -> item
                }
            }

            val taskTemp =
                if (selected.name != "ALL") {
                    tasks.filter { it.category == selected.name }
                } else {
                    tasks
                }
            taskAdapter.submitList(taskTemp)

            categoryAdapter.submitList(categoryTemp)
        }

        rvCategory.adapter = categoryAdapter
        getCategoriesFromDatabase(categoryAdapter)


        rvTask.adapter = taskAdapter
        taskAdapter.submitList(tasks)
    }
    private fun insertDefaultCategory(){
      val categoriesEntity = categories.map {
          CategoryEntity(
              name = it.name,
              isSelected = it.isSelected
          )
      }
        GlobalScope.launch(Dispatchers.IO) {
            categoryDao.insertAll(categoriesEntity)
        }
    }
    private fun insertDefaultTask(){
        val tasksEntity = tasks.map {
            TaskEntity(
                name = it.name,
                category = it.category
            )
        }
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.insertAll(tasksEntity)
        }
    }

    private fun getCategoriesFromDatabase(adapter: CategoryListAdapter) {
        GlobalScope.launch(Dispatchers.IO) {
            val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
            val categarieUiData = categoriesFromDb.map {
                CategoryUiData(
                    name = it.name,
                    isSelected = it.isSelected
                )
            }
            adapter.submitList(categarieUiData)
        }
    }
}

val categories = listOf(
    CategoryUiData(
        name = "ALL",
        isSelected = false
    ),
    CategoryUiData(
        name = "STUDY",
        isSelected = false
    ),
    CategoryUiData(
        name = "WORK",
        isSelected = false
    ),
    CategoryUiData(
        name = "WELLNESS",
        isSelected = false
    ),
    CategoryUiData(
        name = "HOME",
        isSelected = false
    ),
    CategoryUiData(
        name = "HEALTH",
        isSelected = false
    ),
)

val tasks = listOf(
    TaskUiData(
        "Estudar espanhol 1 hora por dia após aula",
        "STUDY"
    ),
    TaskUiData(
        "45 min de treino na academia",
        "HEALTH"
    ),
    TaskUiData(
        "Correr 5km",
        "HEALTH"
    ),
    TaskUiData(
        "Meditar por 10 min",
        "WELLNESS"
    ),
    TaskUiData(
        "Flexões e cardio por 20 min",
        "WELLNESS"
    ),
    TaskUiData(
        "Assistir uma Serie no netflix",
        "HOME"
    ),
    TaskUiData(
        "Dar comida para gata as 8 horas e as 18 horas",
        "HOME"
    ),
    TaskUiData(
        "arrumar a casa",
        "HOME"
    ),
    TaskUiData(
        "Terminar App taskbeats",
        "WORK"
    ),
    TaskUiData(
        "Criar Splash e logo para o App taskbeats",
        "WORK"
    ),
    TaskUiData(
        "Soltar um post no linkedin",
        "WORK"
    ),
)