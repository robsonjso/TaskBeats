package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var categoriesEntity = listOf<CategoryEntity>()
    private var categories = listOf<CategoryUiData>()
    private var tasks = listOf<TaskUiData>()

    private lateinit var rvCategory: RecyclerView
    private lateinit var ctnEmptyView: LinearLayout
    private lateinit var fabCreateTask: FloatingActionButton

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

        rvCategory = findViewById(R.id.rv_categories)
        ctnEmptyView = findViewById(R.id.ll_empty_view)
        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks)
        val btnCreateEmpty = findViewById<Button>(R.id.btn_create_empty)
        fabCreateTask = findViewById(R.id.fab_create_task)

        btnCreateEmpty.setOnClickListener {
            showCreateCategoryBottomSheet()

        }

        fabCreateTask.setOnClickListener {
            showCreateTaskBottomSheet()
        }
        taskAdapter.setOnClickListener { task ->
            showCreateTaskBottomSheet(task)
        }
        categoryAdapter.setOnLongClickListener { categoryToBeDeleted ->
            if (categoryToBeDeleted.name != "+" && categoryToBeDeleted.name != "ALL") {
                val title: String = this.getString(R.string.category_delete_title)
                val description: String = this.getString(R.string.category_delete_description)
                val btnText: String = this.getString(R.string.delete)
                showInfoDialog(title, description, btnText) {
                    val categoryEntityToBeDelete = CategoryEntity(
                        name = categoryToBeDeleted.name,
                        isSelected = categoryToBeDeleted.isSelected
                    )
                    DeleteCategory(categoryEntityToBeDelete)
                }
            }
        }
        categoryAdapter.setOnClickListener { selected ->
            if (selected.name == "+") {
                showCreateCategoryBottomSheet()

            } else {
                val categoryTemp = categories.map { item ->
                    when {
                        item.name == selected.name && !item.isSelected -> item.copy(isSelected = true)
                        item.name != selected.name && item.isSelected -> item.copy(isSelected = false)
                        else -> item
                    }
                }
                    if (selected.name != "ALL") {
                        filterTaskByCategoryName(selected.name)
                    } else {
                        GlobalScope.launch(Dispatchers.IO) {
                            getTasksFromDatabase() }
                    }
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
    private fun showInfoDialog(title: String, description: String, btnText: String, onClick: () -> Unit) {
        val infoBottomSheet = InfoBottomSheet(
            title = title,
            description = description,
            btnText = btnText,
            onBtnClicked = onClick
        )
        infoBottomSheet.show(supportFragmentManager, "info_bottom_sheet")
    }
    private fun getCategoriesFromDatabase() {
        val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
        categoriesEntity = categoriesFromDb
        GlobalScope.launch(Dispatchers.Main) {

            if (categoriesFromDb.isEmpty()) {
                rvCategory.isVisible = false
                ctnEmptyView.isVisible = true
                fabCreateTask.isVisible = false
            } else {
                fabCreateTask.isVisible = true
                rvCategory.isVisible = true
                ctnEmptyView.isVisible = false
            }
        }

        // Verificar se há tarefas no banco de dados antes de adicionar a categoria "ALL"
        GlobalScope.launch(Dispatchers.IO) {
            val tasksFromDb: List<TaskEntity> = taskDao.getAll()

            val categoriesUiData = categoriesFromDb.map {
                CategoryUiData(
                    name = it.name,
                    isSelected = it.isSelected
                )
            }.toMutableList()

            // Adicionar categoria "+" independentemente
            categoriesUiData.add(
                CategoryUiData(
                    name = "+",
                    isSelected = false

                )
            )

            // Adicionar categoria "ALL" somente se houver tarefas no banco de dados
            if (tasksFromDb.isNotEmpty()) {
                categoriesUiData.add(
                    0, // Adicionar no início da lista
                    CategoryUiData(
                        name = "ALL",
                        isSelected = true,
                    )
                )
            }

            GlobalScope.launch(Dispatchers.Main) {
                categories = categoriesUiData
                categoryAdapter.submitList(categories)
            }
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
    private fun updateTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.update(taskEntity)
            getTasksFromDatabase()
        }
    }
    private fun DeleteTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskDao.delete(taskEntity)
            getTasksFromDatabase()
        }
    }
    private fun DeleteCategory(categoryEntity: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksToBeDeleted = taskDao.getAllByCategoryName(categoryEntity.name)
            taskDao.deleteAll(tasksToBeDeleted)
            categoryDao.delete(categoryEntity)
            getCategoriesFromDatabase()
            getTasksFromDatabase()
        }
    }
    private fun filterTaskByCategoryName(categoryName: String){
        GlobalScope.launch(Dispatchers.IO) {
            val taskFromDb: List<TaskEntity> = taskDao.getAllByCategoryName(categoryName)
            val taskUiData: List<TaskUiData> = taskFromDb.map {
                TaskUiData(
                    id = it.id,
                    name = it.name,
                    category = it.category
                )
            }
            GlobalScope.launch(Dispatchers.IO) {
                taskAdapter.submitList(taskUiData)
            }
        }
    }
    private fun showCreateTaskBottomSheet(taskUiData: TaskUiData? = null) {
        val createTaskBottomSheet = CreateOrUpdateTaskBottomSheet(
            task = taskUiData, // Certifica-se de passar o `TaskUiData` como o segundo argumento
            onCreateClicked = { taskToBeCreated ->
                // Inserir nova tarefa no banco de dados
                val taskEntityBeInsert = TaskEntity(
                    name = taskToBeCreated.name,
                    category = taskToBeCreated.category
                )
                insertTask(taskEntityBeInsert)
            },
            categoryList = categoriesEntity,
            onUpdateClicked = { taskToBeUpdated ->
                val taskEntityBeUpdate = TaskEntity(
                    id = taskToBeUpdated.id,
                    name = taskToBeUpdated.name,
                    category = taskToBeUpdated.category
                )
                updateTask(taskEntityBeUpdate)
            },
            onDeleteClicked = { taskToBeDeleted ->
                // Deletar tarefa do banco de dados
                val taskEntityToBeDelete = TaskEntity(
                    id = taskToBeDeleted.id,
                    name = taskToBeDeleted.name,
                    category = taskToBeDeleted.category
                )
                DeleteTask(taskEntityToBeDelete)
            }

        // Lista de categorias no terceiro argumento
        )
        createTaskBottomSheet.show(supportFragmentManager, "create_task_bottom_sheet")
    }
    private fun showCreateCategoryBottomSheet() {
        val bottomSheet = CreateCategoryBottomSheet { categoryName ->
            val categoryEntity = CategoryEntity(
                name = categoryName.uppercase(),
                isSelected = false
            )
            insertCategory(categoryEntity)
        }
        bottomSheet.show(supportFragmentManager, "bottom_sheet")
    }
}