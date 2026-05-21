package com.mountaincrab.logrhythm.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth: FirebaseAuth = Firebase.auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val currentUser: StateFlow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser)

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun signInWithGoogle(idToken: String): FirebaseUser =
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
            .await().user!!

    suspend fun signInAnonymously(): FirebaseUser =
        auth.signInAnonymously().await().user!!

    fun signOut() = auth.signOut()
}
