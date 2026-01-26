package com.panko.brewmate

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panko.brewmate.viewmodel.AuthUiState
import com.panko.brewmate.viewmodel.AuthViewModel

@Composable
fun AuthScreen(
    authViewModel: AuthViewModel = viewModel(),
    onAuthSuccess: () -> Unit
) {
    // Collect states from the ViewModel
    val uiState by authViewModel.uiState.collectAsState()
    val inputState by authViewModel.inputState.collectAsState()

    // Effect for Success
    LaunchedEffect(uiState) {
        if (uiState == AuthUiState.Success) {
            onAuthSuccess()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            val isSignIn = inputState.isSignIn

            Text(
                text = if (isSignIn) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Email Field
            OutlinedTextField(
                value = inputState.email,
                onValueChange = authViewModel::onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = inputState.password,
                onValueChange = authViewModel::onPasswordChange,
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Auth Button
            Button(
                onClick = authViewModel::authenticate,
                enabled = uiState != AuthUiState.Loading,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (uiState == AuthUiState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (isSignIn) "Sign In" else "Sign Up")
                }
            }

            // Error Message
            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Toggle Button
            TextButton(onClick = authViewModel::toggleAuthMode) {
                Text(
                    text = if (isSignIn) "Need an account? Sign Up" else "Already have an account? Sign In"
                )
            }
        }
    }
}