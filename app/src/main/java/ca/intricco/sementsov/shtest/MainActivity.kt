package ca.intricco.sementsov.shtest

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ca.intricco.sementsov.shtest.ui.theme.SHTestTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : ComponentActivity() {
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mDriveService: Drive
    private lateinit var mSheetsService: Sheets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SHTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                    Button(onClick = {
                        createSheet()
                    }) {
                        Text("Create Spreadsheet")
                    }
                }
            }
        }

        createGoogleSignInClient()

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                if (task.isSuccessful) {
                    val account = task.result
                    initializeDriveService(account)
                } else {
                    Log.e("SignIn", "Sign-in Failed")
                }
            }
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            val signInIntent = mGoogleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        } else {
            initializeDriveService(account)
        }
    }

    private fun createGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE), Scope(SheetsScopes.SPREADSHEETS))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun initializeDriveService(account: GoogleSignInAccount?) {
        account?.let {
            val credential = GoogleAccountCredential.usingOAuth2(
                this,
                listOf(DriveScopes.DRIVE, SheetsScopes.SPREADSHEETS))
            credential.selectedAccount = it.account

            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            mDriveService = Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("SHTest")
                .build()

            mSheetsService = Sheets.Builder(transport, jsonFactory, credential)
                .setApplicationName("SHTest")
                .build()

            account.email?.let { it1 -> Log.i("niko", it1) }
            Log.i("niko", "Drive and Sheets services initialized successfully.")
        }
    }

    private fun createSheet() {
        CoroutineScope(Dispatchers.IO).launch {
            val spreadsheet = Spreadsheet().setProperties(SpreadsheetProperties().setTitle("New Sheet"))

            try {
                val result = mSheetsService.spreadsheets().create(spreadsheet).execute()
                val sheetID = result.spreadsheetId
                Log.i("Niko", "Created Sheet with ID: $sheetID")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SHTestTheme {
        Greeting("Android")
    }
}