package com.example.lab1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lab1.data.api.GitHubApi
import com.example.lab1.data.repository.GitHubRepository
import com.example.lab1.domain.DeceptionDetector
import com.example.lab1.ui.MainScreen
import com.example.lab1.ui.MainViewModel
import com.example.lab1.ui.theme.Lab1Theme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(GitHubApi::class.java)
        val repository = GitHubRepository(
            api = api,
            token = BuildConfig.GITHUB_TOKEN,
            owner = BuildConfig.GITHUB_OWNER,
            repo = BuildConfig.GITHUB_REPO
        )
        val detector = DeceptionDetector()

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository, detector) as T
            }
        }

        enableEdgeToEdge()
        setContent {
            Lab1Theme {
                val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                MainScreen(viewModel = viewModel)
            }
        }
    }
}
