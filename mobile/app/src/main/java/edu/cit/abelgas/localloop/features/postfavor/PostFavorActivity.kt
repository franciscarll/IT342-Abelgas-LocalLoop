package edu.cit.abelgas.localloop.features.postfavor

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import edu.cit.abelgas.localloop.R
import edu.cit.abelgas.localloop.databinding.ActivityPostFavorBinding
import edu.cit.abelgas.localloop.shared.api.ApiClient
import edu.cit.abelgas.localloop.shared.util.SharedPreferencesHelper
import kotlinx.coroutines.launch
import java.util.Calendar

class PostFavorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostFavorBinding
    private val viewModel: PostFavorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostFavorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure ApiClient has prefs (idempotent — safe to call multiple times)
        ApiClient.init(SharedPreferencesHelper(this))

        setupBackButton()
        setupTitleField()
        setupCategoryGrid()
        setupDescriptionField()
        setupDateField()
        setupPostButton()
        observeState()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Back arrow — "← Favor Feed" header button
    // Uses finish() + overridePendingTransition(0,0) matching project pattern
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Title field — live character counter + validation
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupTitleField() {
        binding.etFavorTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                viewModel.onTitleChanged(text)
                // Live counter: "47/200"
                binding.tvTitleCounter.text = "${text.length}/200"
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Category grid — exclusive single-selection chip buttons
    // Built programmatically from ViewModel.categories to stay in sync
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupCategoryGrid() {
        // Map category label → icon drawable resource
        // Add entries here when new categories are added to the backend
        val iconMap = mapOf(
            "Errand"         to R.drawable.ic_category_errand,
            "Pet Care"       to R.drawable.ic_category_pet,
            "Plant Watering" to R.drawable.ic_category_plant,
            "Tool Borrowing" to R.drawable.ic_category_tool,
            "Grocery Help"   to R.drawable.ic_category_grocery
        )

        viewModel.categories.forEach { category ->
            // Inflate chip view from reusable layout
            val chip = layoutInflater.inflate(
                R.layout.item_category_chip_grid,
                binding.categoryGrid,
                false
            ) as android.widget.LinearLayout

            chip.findViewById<android.widget.ImageView>(R.id.ivCategoryIcon)
                .setImageResource(iconMap[category] ?: R.drawable.ic_category_errand)

            chip.findViewById<android.widget.TextView>(R.id.tvCategoryLabel).text = category

            chip.setOnClickListener {
                viewModel.onCategorySelected(category)
                hideKeyboard()
            }

            binding.categoryGrid.addView(chip)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Description field
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupDescriptionField() {
        binding.etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onDescriptionChanged(s?.toString() ?: "")
            }
        })
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Date field — standard DatePickerDialog, no custom calendar component
    // minDate set to today so past dates cannot be selected
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupDateField() {
        binding.dateFieldContainer.setOnClickListener {
            hideKeyboard()
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    // ISO-8601 format — matches dateNeeded field in FavorDetailDto
                    val iso = "%04d-%02d-%02d".format(year, month + 1, day)
                    viewModel.onDateSelected(iso)
                    // Display format shown to user: "May 23, 2026"
                    val display = android.text.format.DateFormat.format("MMM dd, yyyy",
                        cal.apply { set(year, month, day) }.time).toString()
                    binding.tvSelectedDate.text = display
                    binding.tvSelectedDate.setTextColor(
                        ContextCompat.getColor(this, R.color.text_primary)
                    )
                    // Show clear X button
                    binding.btnClearDate.visibility = View.VISIBLE
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).also { dialog ->
                dialog.datePicker.minDate = cal.timeInMillis
            }.show()
        }

        binding.btnClearDate.setOnClickListener {
            viewModel.onDateCleared()
            binding.tvSelectedDate.text = getString(R.string.date_placeholder)
            binding.tvSelectedDate.setTextColor(
                ContextCompat.getColor(this, R.color.text_hint)
            )
            binding.btnClearDate.visibility = View.GONE
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Post button
    // ─────────────────────────────────────────────────────────────────────────
    private fun setupPostButton() {
        binding.btnPostFavor.setOnClickListener {
            hideKeyboard()
            viewModel.onPostFavorClicked()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observe ViewModel state — single collect, all UI reactions here
    // ─────────────────────────────────────────────────────────────────────────
    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    // ── Title counter color: red when over limit ──────────────
                    val counterColor = if (state.titleError is TitleError.ExceedsLimit)
                        R.color.text_error else R.color.text_secondary
                    binding.tvTitleCounter.setTextColor(
                        ContextCompat.getColor(this@PostFavorActivity, counterColor)
                    )

                    // ── Title warning visibility ──────────────────────────────
                    binding.tvTitleWarning.visibility =
                        if (state.titleError is TitleError.ExceedsLimit) View.VISIBLE
                        else View.GONE

                    // ── Category chip selection visual state ──────────────────
                    updateChipSelectionStates(state.selectedCategory)

                    // ── Post button enabled/disabled ──────────────────────────
                    binding.btnPostFavor.isEnabled = state.canSubmit
                    binding.btnPostFavor.alpha = if (state.canSubmit) 1.0f else 0.5f

                    // ── Loading overlay ───────────────────────────────────────
                    binding.loadingOverlay.visibility =
                        if (state.isSubmitting) View.VISIBLE else View.GONE

                    // ── Success → finish and return to previous screen ────────
                    if (state.submitSuccess) {
                        Snackbar.make(
                            binding.root,
                            "Favor posted successfully!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                        // Small delay so user sees the success Snackbar
                        binding.root.postDelayed({
                            finish()
                            overridePendingTransition(0, 0)
                        }, 800)
                    }

                    // ── Error Snackbar (non-blocking) ─────────────────────────
                    state.submitError?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearSubmitError()
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update chip background/stroke to reflect selected state
    // Selected: orange stroke + very light orange background
    // Unselected: default border + white background
    // ─────────────────────────────────────────────────────────────────────────
    private fun updateChipSelectionStates(selectedCategory: String?) {
        for (i in 0 until binding.categoryGrid.childCount) {
            val chip = binding.categoryGrid.getChildAt(i) as? android.widget.LinearLayout
                ?: continue
            val label = chip.findViewById<android.widget.TextView>(R.id.tvCategoryLabel)
                ?.text?.toString() ?: continue

            val isSelected = label == selectedCategory

            // Background drawable swap — selected vs default
            chip.setBackgroundResource(
                if (isSelected) R.drawable.bg_chip_selected
                else R.drawable.bg_chip_default
            )

            // Label color
            label.let {
                chip.findViewById<android.widget.TextView>(R.id.tvCategoryLabel)
                    .setTextColor(
                        ContextCompat.getColor(
                            this,
                            if (isSelected) R.color.primary else R.color.text_primary
                        )
                    )
            }

            // Icon tint
            chip.findViewById<android.widget.ImageView>(R.id.ivCategoryIcon)
                ?.setColorFilter(
                    ContextCompat.getColor(
                        this,
                        if (isSelected) R.color.primary else R.color.text_secondary
                    )
                )
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}