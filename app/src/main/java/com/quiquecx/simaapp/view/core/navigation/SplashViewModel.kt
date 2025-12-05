// Archivo: SplashViewModel.kt

package com.quiquecx.simaapp.view.core

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    // Devuelve true si hay un usuario logueado actualmente.
    fun isUserAuthenticated(): Boolean {
        return auth.currentUser != null
    }
}