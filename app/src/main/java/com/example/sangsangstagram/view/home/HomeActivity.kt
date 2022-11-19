package com.example.sangsangstagram.view.home

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import com.example.sangsangstagram.R
import com.example.sangsangstagram.databinding.ActivityHomeBinding
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.sangsangstagram.view.home.mypage.UserPageActivity
import com.example.sangsangstagram.view.home.post.postcreate.PostCreateActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch


class HomeActivity : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, HomeActivity::class.java)
        }
    }

    private val viewModel: HomeViewModel by viewModels()
    lateinit var binding: ActivityHomeBinding
    private val user = Firebase.auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val userUuid = user?.uid.toString()
        viewModel.updateCurrentUserProfileImage(userUuid)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::homeProfileSet)
            }
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        findViewById<BottomNavigationView>(R.id.bottom_nav)
            .setupWithNavController(navController)
        binding.bottomNav.setupWithNavController(navController)

        binding.postCreateButton.setOnClickListener {
            startActivity(Intent(this, PostCreateActivity::class.java))
        }

        binding.profileImage.setOnClickListener {
            val intent = UserPageActivity.getIntent(this, Firebase.auth.currentUser?.uid.toString())
            startActivity(intent)
        }
    }

    private fun homeProfileSet(uiState: HomeUiState) {
        val storageReference = Firebase.storage.reference
        val userDetail = uiState.userDetail

        if (userDetail != null) {
            binding.apply {
                userDetail.profileImageUrl?.let {
                    Glide.with(this@HomeActivity)
                        .load(storageReference.child(it))
                        .into(profileImage)
                }
            }
        }
    }
}