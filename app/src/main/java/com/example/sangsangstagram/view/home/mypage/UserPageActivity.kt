package com.example.sangsangstagram.view.home.mypage

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.sangsangstagram.R
import com.example.sangsangstagram.databinding.ActivityUserPageBinding
import com.example.sangsangstagram.view.login.LoginActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class UserPageActivity : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context, userUuid: String): Intent {
            return Intent(context, UserPageActivity::class.java).apply {
                putExtra("userUuid", userUuid)
            }
        }
    }

    private fun getUserUuid(): String {
        return intent.getStringExtra("userUuid")!!
    }

    private val viewModel: UserPageViewModel by viewModels()
    private lateinit var binding: ActivityUserPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.profileUpdate(getUserUuid())

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::updateUi)
            }
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateUi(uiState: UserPageUiState) {
        val storage: FirebaseStorage =
            FirebaseStorage.getInstance("gs://sangsangstagram.appspot.com/")
        val storageReference = storage.reference
        val userDetail = uiState.userDetail
        val pathReference = userDetail?.profileImageUrl?.let { storageReference.child(it) }

        if (userDetail != null) {
            binding.apply {
                pathReference?.downloadUrl?.addOnSuccessListener { uri ->
                    Glide.with(this@UserPageActivity)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .fallback(R.drawable.ic_baseline_person_pin_24)
                        .centerCrop()
                        .into(accountProfileImageView)
                }
                accountName.text = userDetail.name

                if (userDetail.isMe) {
                    accountProfileButton.text = getString(R.string.update)
                    accountProfileButton.setOnClickListener {
                        startInfoUpdateUi()
                    }

                    sendMessageButton.isVisible = false
                }

            }
        }
    }

    private fun startInfoUpdateUi() {
        val intent = InfoUpdateActivity.getIntent(this)
        startActivity(intent)
    }
}