package com.helal.accomplishmentplan.nativeapp

import android.content.Context
import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseConfigStatus
import com.helal.accomplishmentplan.nativeapp.core.firebase.FirebaseRuntimeConfig
import com.helal.accomplishmentplan.nativeapp.core.session.SessionCleanupCoordinator
import com.helal.accomplishmentplan.nativeapp.data.auth.AuthRepository
import com.helal.accomplishmentplan.nativeapp.data.auth.FirebaseAuthRepository
import com.helal.accomplishmentplan.nativeapp.data.auth.GoogleCredentialHelper
import com.helal.accomplishmentplan.nativeapp.data.preferences.UserMonthPreferencesStore
import com.helal.accomplishmentplan.nativeapp.data.repository.FirestoreMasterDataRepository
import com.helal.accomplishmentplan.nativeapp.data.repository.FirestoreTaskRepository
import com.helal.accomplishmentplan.nativeapp.data.repository.MasterDataRepository
import com.helal.accomplishmentplan.nativeapp.data.repository.TaskRepository

/**
 * Lightweight constructor-injection application container wiring Phase 1 Auth
 * and Phase 2 Firestore Repositories & UID-scoped DataStore preferences.
 */
class AppContainer(appContext: Context) {

    val firebaseConfigStatus: FirebaseConfigStatus = FirebaseRuntimeConfig.inspect(appContext)
    val sessionCleanupCoordinator: SessionCleanupCoordinator = SessionCleanupCoordinator()
    val googleCredentialHelper: GoogleCredentialHelper = GoogleCredentialHelper()
    val userMonthPreferencesStore: UserMonthPreferencesStore = UserMonthPreferencesStore(appContext)

    val authRepository: AuthRepository = FirebaseAuthRepository(
        configStatus = firebaseConfigStatus,
        sessionCleanupCoordinator = sessionCleanupCoordinator
    )

    val taskRepository: TaskRepository = FirestoreTaskRepository(
        configStatus = firebaseConfigStatus,
        sessionCleanupCoordinator = sessionCleanupCoordinator
    )

    val masterDataRepository: MasterDataRepository = FirestoreMasterDataRepository(
        configStatus = firebaseConfigStatus,
        sessionCleanupCoordinator = sessionCleanupCoordinator
    )
}
