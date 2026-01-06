package com.example.puydufouexperience.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.puydufouexperience.R
import com.example.puydufouexperience.data.db.DatabaseProvider
import com.example.puydufouexperience.data.repository.UserRepository
import com.example.puydufouexperience.databinding.ActivityLoginBinding
import com.example.puydufouexperience.ui.main.MainActivity
import com.example.puydufouexperience.viewmodel.login.LoginViewModel

/**
 * LoginActivity (UI pura, MVVM):
 * - No accede a Room
 * - No usa Coroutines
 * - No aplica idioma/tema
 * - Solo delega en el ViewModel y navega
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    /**
     * Creamos el ViewModel con Factory manual.
     * (Sin DI raro; usando tus clases existentes.)
     */
    private val viewModel: LoginViewModel by viewModels {
        val db = DatabaseProvider.get(applicationContext)
        val repo = UserRepository(db.usuarioDao(), db.ajustesUsuarioDao())
        LoginViewModel.Factory(repo, applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1) Observamos el estado del VM
        viewModel.state.observe(this) { state ->
            when (state) {
                LoginViewModel.State.Idle -> Unit

                LoginViewModel.State.Loading -> {
                    // Sin progress bar: bloqueamos botones para evitar dobles clicks
                    setButtonsEnabled(false)
                }

                LoginViewModel.State.Ready -> {
                    // Ya se puede interactuar
                    setButtonsEnabled(true)
                }

                is LoginViewModel.State.Error -> {
                    setButtonsEnabled(true)
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }

                is LoginViewModel.State.Logged -> {
                    setButtonsEnabled(true)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }

                is LoginViewModel.State.Registered -> {
                    setButtonsEnabled(true)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_account_created),
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }

        // 2) Init: seed wait + autologin
        viewModel.init()

        // 3) Acceder
        binding.btnAcceder.setOnClickListener {
            viewModel.login(
                nombre = binding.etUsuario.text.toString(),
                passwordPlain = binding.etContrasena.text.toString()
            )
        }

        // 4) Crear cuenta
        binding.btnRegistrar.setOnClickListener {
            viewModel.register(
                nombre = binding.etUsuario.text.toString(),
                passwordPlain = binding.etContrasena.text.toString()
            )
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnAcceder.isEnabled = enabled
        binding.btnRegistrar.isEnabled = enabled
    }
}
