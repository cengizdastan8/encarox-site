package com.encarox.kodo.data.gmail

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.gmail.GmailScopes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Google Sign-In consent for the read-only Gmail scope and builds a
 * per-account [GoogleAccountCredential]. Users add each of their Gmail accounts
 * by signing in once; Kodo then reads only verification emails from them.
 */
@Singleton
class GmailAuthManager @Inject constructor(
    private val appContext: Context
) {
    private val gmailScope = Scope(GmailScopes.GMAIL_READONLY)

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(gmailScope)
            .build()

    val signInClient: GoogleSignInClient by lazy {
        GoogleSignIn.getClient(appContext, signInOptions)
    }

    /** Intent that opens the account picker + Gmail consent screen. */
    fun newSignInIntent(): Intent = signInClient.signInIntent

    /** True once the given account has granted the read-only Gmail scope. */
    fun hasGmailAccess(email: String): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(appContext) ?: return false
        return account.email == email &&
            GoogleSignIn.hasPermissions(account, gmailScope)
    }

    /** Builds an authenticated credential for a specific account email. */
    fun credentialFor(email: String): GoogleAccountCredential {
        return GoogleAccountCredential
            .usingOAuth2(appContext, listOf(GmailScopes.GMAIL_READONLY))
            .apply { selectedAccount = Account(email, "com.google") }
    }
}
