package com.example.sangsangstagram.view.login

import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.sangsangstagram.R
import com.example.sangsangstagram.databinding.ActivityUserInfoBinding
import com.example.sangsangstagram.view.home.HomeActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class UserInfoActivity : AppCompatActivity() {

    private val viewModel: UserInfoViewModel by viewModels()

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, UserInfoActivity::class.java)
        }
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { imageUri ->
            if (imageUri != null) {
                @Suppress("DEPRECATION")
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            contentResolver,
                            imageUri
                        )
                    )
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                }
                viewModel.selectedImage = bitmap
                binding.profileImage.setImageBitmap(bitmap)
            }
        }

    lateinit var binding: ActivityUserInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.userNameEditText.setText(viewModel.name)
        viewModel.selectedImage?.let { binding.profileImage.setImageBitmap(it) }

        binding.profileImage.setOnClickListener {
            onClickImage()
        }

        binding.userNameEditText.addTextChangedListener {
            if (it != null) {
                viewModel.name = it.toString()
                updateDoneButton()
            }
        }

        binding.doneButton.setOnClickListener {
            viewModel.sendInfo()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUi)
            }
        }
    }

    private fun updateUi(uiState: UserInfoUiState) {
        updateDoneButton()
        when (uiState) {
            UserInfoUiState.SuccessToSave -> {
                val sharedPreferences = getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
                )
                sharedPreferences.edit()
                    .putBoolean(getString(R.string.prefs_has_user_info), true)
                    .apply()

                navigateToHomeView()
            }
            is UserInfoUiState.FailedToSave -> {
                showSnackBar(getString(R.string.failed))
            }
            else -> {}
        }
    }

    private fun updateDoneButton() {
        val isLoading = viewModel.uiState.value is UserInfoUiState.Loading
        val hasName = binding.userNameEditText.text.toString().isNotEmpty()

        binding.doneButton.apply {
            isEnabled = hasName && !isLoading
            text = getString(if (isLoading) R.string.loading else R.string.start)
        }
    }

    private fun onClickImage() {
        if (viewModel.selectedImage != null) {
            MaterialAlertDialogBuilder(this)
                .setItems(R.array.image_options) { _, which ->
                    when (which) {
                        0 -> {
                            showImagePicker()
                        }
                        1 -> {
                            viewModel.selectedImage = null
                            binding.profileImage.setImageDrawable(
                                AppCompatResources.getDrawable(
                                    this,
                                    R.drawable.ic_baseline_person_pin_24
                                )
                            )
                        }
                        else -> throw IllegalArgumentException()
                    }
                }.create()
                .show()
        } else {
            showImagePicker()
        }
    }

    private fun showImagePicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun navigateToHomeView() {
        val intent = HomeActivity.getIntent(this).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_TASK_ON_HOME
        }
        startActivity(intent)
        finish()
    }
}
