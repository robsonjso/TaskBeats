package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var categories = listOf<CategoryUiData>()
    private var tasks = listOf<TaskUiData>()

    private val categoryAdapter = CategoryListAdapter()
    private val taskAdapter by lazy {
        TaskListAdapter()
    }

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

        val rvCategory = findViewById<RecyclerView>(R.id.rv_categories)
        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks)
        val fabCreateTask = findViewById<FloatingActionButton>(R.id.fab_create_task)


        fabCreateTask.setOnClickListener {
            showCreateTaskBottomSheet()
        }
        taskAdapter.setOnClickListener {
            showCreateTaskBottomSheet()

        }
         categoryAdapter.setOnClickListener { selected ->
             if(selected.name == "+"){
                 val bottomSheet = CreateCategoryBottomSheet {
                     categoryName ->
                     val categoryEntity = CategoryEntity(
                         name = categoryName,
                         isSelected = false
                     )
                     insertCategory(categoryEntity)

                 }
                 bottomSheet.show(supportFragmentManager, "bottom_sheet")

             }else{
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
         }

        rvCategory.adapter = categoryAdapter
        GlobalScope.launch(Dispatchers.IO) {
            getCategoriesFromDatabase()
        }

        rvTask.adapter = taskAdapter

        GlobalScope.launch(Dispatchers.IO) {
            getTasksFromDatabase()
        }
    }
     /*private fun insertDefaultCategory(){
       val categoriesEntity = categories.map {
           CategoryEntity(
               name = it.name,
               isSelected = it.isSelected
           )
       }
         GlobalScope.launch(Dispatchers.IO) {
             categoryDao.insertAll(categoriesEntity)
         }
     }*/
     /* private fun insertDefaultTask(){
         val tasksEntity = tasks.map {
             TaskEntity(
                 name = it.name,
                 category = it.category
             )
         }
         GlobalScope.launch(Dispatchers.IO) {
             taskDao.insertAll(tasksEntity)
         }
     }*/
    private fun getCategoriesFromDatabase() {

            val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
            val categoriesUiData= categoriesFromDb.map {
                CategoryUiData(
                    name = it.name,
                    isSelected = it.isSelected
                )
            }
                .toMutableList()

            //add fake + category
            categoriesUiData.add(
                CategoryUiData(
                    name = "+",
                    isSelected = false
                )
            )
            GlobalScope.launch(Dispatchers.Main) {
                categories = categoriesUiData
                categoryAdapter.submitList(categoriesUiData)
            }

    }
    private fun getTasksFromDatabase() {

            val tasksFromDb: List<TaskEntity> = taskDao.getAll()
            val tasksUiData: List<TaskUiData> = tasksFromDb.map {
                TaskUiData(
                    id = it.id,
                    name = it.name,
                    category = it.category
                )

            }
            GlobalScope.launch(Dispatchers.Main) {
                tasks = tasksUiData
                taskAdapter.submitList(tasksUiData)
            }
    }
    private fun insertCategory(category: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            categoryDao.insert(category)
            getCategoriesFromDatabase()
        }
    }

    private fun insertTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.insert(taskEntity)
            getTasksFromDatabase()
        }
    }
    private fun showCreateTaskBottomSheet() {
        val createTaskBottomSheet = CreateTaskBottomSheet(
            { taskToBeCreated ->
                // Inserir nova tarefa no banco de dados
                val taskEntityBeInsert = TaskEntity(
                    name = taskToBeCreated.name,
                    category = taskToBeCreated.category
                )
                insertTask(taskEntityBeInsert)
            },
            categories // Passando lista de categorias para o BottomSheet
        )
        createTaskBottomSheet.show(supportFragmentManager, "create_task_bottom_sheet")
    }
}