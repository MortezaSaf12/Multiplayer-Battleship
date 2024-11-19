package com.example.battleship

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "MainScreen") {
        composable("MainScreen") { MainScreen(navController) }
        composable("LobbyScreen/{username}") { backStackEntry ->
            val loggedInUsername = backStackEntry.arguments?.getString("username").orEmpty()
            LobbyScreen(navController, loggedInUsername)
        }
        composable("GameBoardScreen") { GameBoardScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Battleship",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Enter Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black,
                cursorColor = Color.Black
            )
        )

        Button(
            onClick = {
                if (username.text.isNotBlank()) {
                    checkAndAddPlayer(
                        username = username.text.trim(),
                        firestore = firestore,
                        onPlayerExists = {
                            Toast.makeText(context, "Welcome back, ${username.text.trim()}", Toast.LENGTH_SHORT).show()
                            navController.navigate("LobbyScreen/${username.text.trim()}")
                        },
                        onPlayerAdded = {
                            Toast.makeText(context, "Player added successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigate("LobbyScreen/${username.text.trim()}")
                        },
                        onError = { errorMessage ->
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Please enter a valid Username", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Join Lobby", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun checkAndAddPlayer(
    username: String,
    firestore: FirebaseFirestore,
    onPlayerExists: () -> Unit,
    onPlayerAdded: () -> Unit,
    onError: (String) -> Unit
) {
    val playersCollection = firestore.collection("players")

    playersCollection
        .whereEqualTo("name", username)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                playersCollection
                    .document(snapshot.documents[0].id)
                    .update("status", "online")
                    .addOnSuccessListener { onPlayerExists() }
                    .addOnFailureListener { onError("Error updating player status") }
            } else {
                playersCollection
                    .add(
                        mapOf(
                            "name" to username,
                            "status" to "online",
                            "playerId" to UUID.randomUUID().toString()
                        )
                    )
                    .addOnSuccessListener { onPlayerAdded() }
                    .addOnFailureListener { exception ->
                        onError("Error adding player: ${exception.message}")
                    }
            }
        }
        .addOnFailureListener { exception ->
            onError("Error checking player: ${exception.message}")
        }
}

@Composable
fun LobbyScreen(navController: NavController, loggedInUsername: String) {
    val players = remember { mutableStateListOf<String>() }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val listenerRegistration = firestore.collection("players")
            .whereEqualTo("status", "online")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(context, "Error fetching players: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                snapshot?.documents?.mapNotNull {
                    val name = it.getString("name")
                    val status = it.getString("status")
                    if (name != null && status != null && name) "$name - $status" else null
                    //if (name != null && status != null && name != loggedInUsername) "$name - $status" else null
                }?.let {
                    players.clear()
                    players.addAll(it)
                }
            }
        onDispose {
            listenerRegistration.remove()
        }
    }

    @Composable
    fun PlayerRow(player: String, onChallengeClick: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color.Black, shape = MaterialTheme.shapes.small)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = player, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Button(
                onClick = onChallengeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) {
                Text("Challenge")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lobby",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Online Players",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Text(
            text = "Welcome, $loggedInUsername!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(players) { player ->
                PlayerRow(player = player) {
                    navController.navigate("GameBoardScreen")
                }
            }
        }
    }
}

@Composable
fun GameBoardScreen(navController: NavController) {
    val boardSize = 10
    val grid = remember { MutableList(boardSize) { MutableList(boardSize) { "W" } } }
    var turn by remember { mutableStateOf("Player 1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Player 1 vs Player 2",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Current Turn: $turn",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Blue)
        ) {
            Column {
                for (i in 0 until boardSize) {
                    Row {
                        for (j in 0 until boardSize) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .border(1.dp, Color.Black)
                                    .background(
                                        when (grid[i][j]) {
                                            "H" -> Color.Red
                                            "S" -> Color.Black
                                            else -> Color.Cyan
                                        }
                                    )
                                    .clickable {
                                        if (turn == "Player 1") {
                                            grid[i][j] = if (grid[i][j] == "W") "H" else grid[i][j]
                                            turn = "Player 2"
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { turn = if (turn == "Player 1") "Player 2" else "Player 1" },
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("End Turn")
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Quit Game")
        }
    }
}
