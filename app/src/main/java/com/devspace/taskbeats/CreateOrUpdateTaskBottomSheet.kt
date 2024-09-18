package com.devspace.taskbeats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class CreateOrUpdateTaskBottomSheet(
    private val onCreateClicked: (TaskUiData) -> Unit,
    private val onUpdateClicked: (TaskUiData) -> Unit,
    private val onDeleteClicked: (TaskUiData) -> Unit,
    private val task: TaskUiData? = null,
    private val categoryList: List<CategoryEntity>
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.create_or_update_task_bottom_sheet, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tv_title)  // Mantido como Button
        val btnCreateOrUpdate = view.findViewById<Button>(R.id.btn_task_create_or_update)
        val btnDelete = view.findViewById<Button>(R.id.btn_task_delete)
        val tieTaskName = view.findViewById<TextInputEditText>(R.id.tie_task_name)

        val spinner: Spinner = view.findViewById(R.id.category_list)
        var taskCategory: String? = null  // Categoria selecionada
        // Prepara as categorias no Spinner
        val categoryListTemp = mutableListOf("Select")
        categoryListTemp.addAll(
            categoryList.map { it.name }
        )
        val categoryStr: List<String> = categoryListTemp

        ArrayAdapter(
            requireActivity().baseContext,
            android.R.layout.simple_spinner_item,
            categoryStr.toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }
        // Listener para capturar a categoria selecionada
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                taskCategory = categoryStr[position]  // Define a categoria selecionada
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                taskCategory = null  // Se nada for selecionado, mantém null
            }
        }
        // Se for atualizar uma tarefa existente
        if (task == null) {
            btnDelete.isVisible = false
            tvTitle.setText(R.string.create_task_title)
            btnCreateOrUpdate.setText(R.string.create)
        } else {
            // Para criar nova tarefa
            btnDelete.isVisible = true
            tvTitle.setText(R.string.update_task_title)
            btnCreateOrUpdate.setText(R.string.update)

            tieTaskName.setText(task.name)
        }
        btnDelete.setOnClickListener {
            if (task != null) {
                onDeleteClicked.invoke(task)
            }else{
                Log.d("CreateOrUpdateTaskBottomSheet", "Task is not found")
            }
            dismiss()
        }
        btnCreateOrUpdate.setOnClickListener {
            val name = tieTaskName.text.toString().trim()
            if (taskCategory != "Select" && name.isNotEmpty()) {
                if (task == null) {
                    // Chama a função passada para criar/atualizar a tarefa
                    onCreateClicked.invoke(
                        TaskUiData(
                            id = 0,
                            name = name,
                            category = requireNotNull(taskCategory)  // Garante que a categoria está selecionada
                        )
                    )
                }else {
                    onUpdateClicked.invoke(
                        TaskUiData(
                            id = task.id,
                            name = name,
                            category = requireNotNull(taskCategory)  // Garante que a categoria está selecionada
                        )
                    )
                }
                dismiss()  // Fecha o bottom sheet
            } else {
                // Mostra erro se o nome ou a categoria não estiverem definidos
                Snackbar.make(btnCreateOrUpdate, "Por favor, insira um nome e selecione uma categoria.", Snackbar.LENGTH_SHORT).show()
            }
        }
        return view
    }
}