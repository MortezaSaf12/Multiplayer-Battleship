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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "MainScreen") {
        composable("MainScreen") { MainScreen(navController) }
        composable("LobbyScreen/{username}") { backStackEntry ->
            val loggedInUsername = backStackEntry.arguments?.getString("username").orEmpty()
            if (loggedInUsername.isBlank()) {
                Toast.makeText(LocalContext.current, "Invalid username", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            } else {
                LobbyScreen(navController, loggedInUsername)
            }
        }
        composable("GameBoardScreen?playerName={playerName}&opponentName={opponentName}") { backStackEntry ->
            val playerName = backStackEntry.arguments?.getString("playerName").orEmpty()
            val opponentName = backStackEntry.arguments?.getString("opponentName").orEmpty()
            GameBoardScreen(navController, playerName, opponentName)
        }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Handle player status based on lifecycle events
    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (username.text.isNotBlank()) {
                        setPlayerStatus(username.text.trim(), "online", firestore)
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    if (username.text.isNotBlank()) {
                        setPlayerStatus(username.text.trim(), "offline", firestore)
                    }
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }

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
                .padding(8.dp),
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            singleLine = true
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

//Helper function
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

//Helper function
private fun setPlayerStatus(username: String, status: String, firestore: FirebaseFirestore) {
    firestore.collection("players")
        .whereEqualTo("name", username)
        .get()
        .addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                snapshot.documents[0].reference.update("status", status)
            }
        }
}

@Composable
fun LobbyScreen(navController: NavController, loggedInUsername: String) {
    val players = remember { mutableStateListOf<String>() }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // Fetching "online users
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
                    if (name != null && status != null) "$name - $status" else null
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
            text = "Welcome, $loggedInUsername!",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            text = "Online Players",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(players) { player ->
                PlayerRow(player = player) {
                    val opponent = player.substringBefore(" - ")
                    navController.navigate("GameBoardScreen?playerName=$loggedInUsername&opponentName=$opponent")
                }
            }
        }
    }
    }

@Composable
fun GameBoardScreen(
    navController: NavController,
    playerName: String,
    opponentName: String
) {
    val boardSize = 10
    val player1Grid = remember { MutableList(boardSize) { MutableList(boardSize) { "W" } } } // Player 1's grid
    val player2Grid = remember { MutableList(boardSize) { MutableList(boardSize) { "W" } } } // Player 2's grid
    val player1View = remember { MutableList(boardSize) { MutableList(boardSize) { "W" } } } // Player 1's view of Player 2's grid
    val player2View = remember { MutableList(boardSize) { MutableList(boardSize) { "W" } } } // Player 2's view of Player 1's grid
    var turn by remember { mutableStateOf("Player 1") }

    fun translateCoordinates(row: Int, col: Int): Pair<Int, Int> {
        // Simple translation logic (identity mapping for demonstration)
        return row to col
    }

    fun handleCellClick(
        playerGrid: MutableList<MutableList<String>>,
        opponentView: MutableList<MutableList<String>>,
        row: Int,
        col: Int
    ) {
        if (turn == "Player 1") {
            val (translatedRow, translatedCol) = translateCoordinates(row, col)
            if (opponentView[translatedRow][translatedCol] == "W") {
                opponentView[translatedRow][translatedCol] = if (playerGrid[translatedRow][translatedCol] == "S") "H" else "M"
                turn = "Player 2"
            }
        } else {
            val (translatedRow, translatedCol) = translateCoordinates(row, col)
            if (opponentView[translatedRow][translatedCol] == "W") {
                opponentView[translatedRow][translatedCol] = if (playerGrid[translatedRow][translatedCol] == "S") "H" else "M"
                turn = "Player 1"
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
            text = "$playerName vs $opponentName",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Current Turn: ${if (turn == "Player 1") playerName else opponentName}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Opponent's Grid
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$opponentName's Board",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GridView(
                grid = if (turn == "Player 1") player1View else player2View,
                onCellClick = { row, col ->
                    if (turn == "Player 1") {
                        handleCellClick(player2Grid, player1View, row, col)
                    } else {
                        handleCellClick(player1Grid, player2View, row, col)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Player's Own Grid
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$playerName's Ships",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GridView(grid = if (turn == "Player 1") player1Grid else player2Grid)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Quit Game")
        }
    }
}

@Composable
fun GridView(
    grid: List<List<String>>,
    onCellClick: ((Int, Int) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .size(300.dp)
            .aspectRatio(1f)
    ) {
        for (i in grid.indices) {
            Row(
                modifier = Modifier.weight(1f)
            ) {
                for (j in grid[i].indices) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .border(1.dp, Color.Black)
                            .background(
                                when (grid[i][j]) {
                                    "H" -> Color.Red // Hit
                                    "M" -> Color.Gray // Miss
                                    "S" -> Color.Black // Ship
                                    else -> Color.Cyan // Water
                                }
                            )
                            .clickable(enabled = onCellClick != null) {
                                onCellClick?.invoke(i, j)
                            }
                    )
                }
            }
        }
    }
}


