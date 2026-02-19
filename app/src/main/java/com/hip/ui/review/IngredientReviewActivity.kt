package com.hip.ui.review

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hip.HIPApplication
import com.hip.R
import com.hip.databinding.ActivityIngredientReviewBinding
import com.hip.model.DetectedIngredient
import com.hip.model.IngredientState
import com.hip.ui.recipes.RecipeListActivity
import com.hip.ui.scan.ScanSession
import com.hip.vision.toDetectedIngredient
import kotlinx.coroutines.launch

/**
 * Pantalla de validación humana de ingredientes detectados.
 *
 * El usuario puede:
 * ✓ Confirmar ingredientes detectados
 * ✗ Eliminar falsos positivos
 * ✏ Cambiar el nombre de un ingrediente
 * 🔄 Cambiar el estado (crudo/cocido/congelado)
 * ➕ Agregar manualmente lo que no fue detectado
 */
class IngredientReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIngredientReviewBinding
    private lateinit var adapter: IngredientReviewAdapter
    private val ingredientList = mutableListOf<DetectedIngredient>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIngredientReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadDetectedIngredients()
        setupButtons()
    }

    private fun setupRecyclerView() {
        adapter = IngredientReviewAdapter(
            ingredientList,
            onDelete = { position ->
                ingredientList.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateConfirmButton()
            },
            onStateChange = { position, newState ->
                ingredientList[position] = ingredientList[position].copy(state = newState)
                adapter.notifyItemChanged(position)
            },
            onConfirmToggle = { position ->
                val current = ingredientList[position]
                ingredientList[position] = current.copy(isConfirmedByUser = !current.isConfirmedByUser)
                adapter.notifyItemChanged(position)
                updateConfirmButton()
            }
        )

        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(this@IngredientReviewActivity)
            adapter = this@IngredientReviewActivity.adapter
        }
    }

    private fun loadDetectedIngredients() {
        val detected = ScanSession.current.detectedResults
        if (detected.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvIngredients.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvIngredients.visibility = View.VISIBLE
            ingredientList.addAll(detected.map { it.toDetectedIngredient() })
            adapter.notifyDataSetChanged()
        }
        updateConfirmButton()
    }

    private fun setupButtons() {
        binding.btnAddIngredient.setOnClickListener {
            showAddIngredientDialog()
        }

        binding.btnConfirmAll.setOnClickListener {
            confirmAll()
        }

        binding.btnFindRecipes.setOnClickListener {
            proceedToRecipes()
        }

        binding.btnScanAgain.setOnClickListener {
            finish()  // Vuelve a ScanActivity
        }
    }

    private fun showAddIngredientDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_ingredient, null)
        val etName = view.findViewById<EditText>(R.id.etIngredientName)
        val etQuantity = view.findViewById<EditText>(R.id.etQuantity)
        val spinnerState = view.findViewById<Spinner>(R.id.spinnerState)

        val states = IngredientState.values().map { it.name.replace('_', ' ').lowercase() }
        spinnerState.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, states)

        AlertDialog.Builder(this)
            .setTitle("Agregar ingrediente")
            .setView(view)
            .setPositiveButton("Agregar") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotBlank()) {
                    val newIngredient = DetectedIngredient(
                        name = name,
                        quantity = etQuantity.text.toString().trim(),
                        state = IngredientState.values()[spinnerState.selectedItemPosition],
                        confidence = 1f,
                        isManuallyAdded = true,
                        isConfirmedByUser = true
                    )
                    ingredientList.add(newIngredient)
                    adapter.notifyItemInserted(ingredientList.size - 1)
                    updateConfirmButton()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmAll() {
        ingredientList.replaceAll { it.copy(isConfirmedByUser = true) }
        adapter.notifyDataSetChanged()
        updateConfirmButton()
    }

    private fun updateConfirmButton() {
        val confirmedCount = ingredientList.count { it.isConfirmedByUser }
        binding.btnFindRecipes.isEnabled = confirmedCount > 0
        binding.tvConfirmedCount.text = "$confirmedCount ingrediente(s) confirmado(s)"
    }

    private fun proceedToRecipes() {
        // Guardar en sesión
        ScanSession.current.confirmedIngredients.clear()
        ScanSession.current.confirmedIngredients.addAll(
            ingredientList.filter { it.isConfirmedByUser }
        )

        val intent = Intent(this, RecipeListActivity::class.java)
        startActivity(intent)
    }
}

// ─────────────────────────────────────────────────────
//  ADAPTER
// ─────────────────────────────────────────────────────

class IngredientReviewAdapter(
    private val ingredients: MutableList<DetectedIngredient>,
    private val onDelete: (Int) -> Unit,
    private val onStateChange: (Int, IngredientState) -> Unit,
    private val onConfirmToggle: (Int) -> Unit
) : RecyclerView.Adapter<IngredientReviewAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvIngredientName)
        val tvConfidence: TextView = view.findViewById(R.id.tvConfidence)
        val tvManualBadge: TextView = view.findViewById(R.id.tvManualBadge)
        val spinnerState: Spinner = view.findViewById(R.id.spinnerState)
        val checkConfirm: CheckBox = view.findViewById(R.id.checkConfirm)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val cardView: View = view.findViewById(R.id.cardIngredient)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ingredient = ingredients[position]

        holder.tvName.text = ingredient.name
        holder.tvConfidence.text = if (ingredient.isManuallyAdded) "" else {
            "%.0f%% confianza".format(ingredient.confidence * 100)
        }
        holder.tvManualBadge.visibility = if (ingredient.isManuallyAdded) View.VISIBLE else View.GONE
        holder.checkConfirm.isChecked = ingredient.isConfirmedByUser

        // Configurar spinner de estado
        val states = IngredientState.values()
        val stateNames = states.map { it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() } }
        holder.spinnerState.adapter = ArrayAdapter(
            holder.itemView.context,
            android.R.layout.simple_spinner_item,
            stateNames
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        val currentStateIdx = states.indexOf(ingredient.state).takeIf { it >= 0 } ?: 0
        holder.spinnerState.setSelection(currentStateIdx, false)

        holder.spinnerState.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (pos != currentStateIdx) onStateChange(holder.adapterPosition, states[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.checkConfirm.setOnClickListener {
            onConfirmToggle(holder.adapterPosition)
        }

        holder.btnDelete.setOnClickListener {
            onDelete(holder.adapterPosition)
        }

        // Color según confirmación
        holder.cardView.alpha = if (ingredient.isConfirmedByUser) 1.0f else 0.5f
    }

    override fun getItemCount() = ingredients.size
}
