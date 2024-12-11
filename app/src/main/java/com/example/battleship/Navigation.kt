package com.example.battleship

import androidx.compose.foundation.Image
import android.widget.Toast
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material3.TextFieldDefaults

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
fun Background(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.pxfuel),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    Background {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Semi-transparent overlay
        ) {
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
                    color = Color.White, // White text for better contrast
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Enter Username", color = Color.White) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        if (username.text.isNotBlank()) {
                            checkAndAddPlayer(
                                username = username.text.trim(),
                                firestore = firestore,
                                onPlayerExists = {
                                    Toast.makeText(
                                        context,
                                        "Welcome back, ${username.text.trim()}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate("LobbyScreen/${username.text.trim()}")
                                },
                                onPlayerAdded = {
                                    Toast.makeText(
                                        context,
                                        "Player added successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate("LobbyScreen/${username.text.trim()}")
                                },
                                onError = { errorMessage ->
                                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            Toast.makeText(context, "Please enter a valid Username", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Join Lobby", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
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
                    .addOnSuccessListener {
                    }
                    .addOnFailureListener { error ->
                        Log.e("FirestoreError", "Error updating player status: ${error.message}")
                    }
            } else {
                Log.d("Firestore", "Player document not found for username: $username")
            }
        }
        .addOnFailureListener { error ->
            Log.e("FirestoreError", "Error fetching player document: ${error.message}")
        }
}

//Helper function
@Composable
fun PlayerRow(player: String, onChallengeClick: () -> Unit) {
    // Split the player string into name and status
    val parts = player.split(" - ")
    val playerName = parts.getOrNull(0) ?: ""
    val status = parts.getOrNull(1) ?: ""

    val statusColor = if (status.equals("online", ignoreCase = true)) Color.Green else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            // Add a semi-transparent overlay for the row
            .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.small)
            .border(1.dp, Color.White, shape = MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = playerName, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text = status, fontSize = 14.sp, color = statusColor)
        }
        Button(
            onClick = {
                Log.d("Challenge", "Challenge button clicked for $playerName")
                onChallengeClick()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Challenge")
        }
    }
}


@Composable
fun GameInvitation(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Game Invitation Alert")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Decline")
            }
        }
    )
}

@Composable
fun LobbyScreen(navController: NavController, loggedInUsername: String) {
    val players = remember { mutableStateListOf<String>() }
    val firestore = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var opponent by remember { mutableStateOf<String?>(null) } // Holds the opponent being challenged
    var incomingChallenge by remember { mutableStateOf<Triple<String, String, String>?>(null) } // Holds (id, fromPlayer, toPlayer)

    // Observer to handle online/offline status
    LaunchedEffect(lifecycleOwner) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        setPlayerStatus(loggedInUsername, "online", firestore)
                    }

                    Lifecycle.Event.ON_STOP -> {
                        setPlayerStatus(loggedInUsername, "offline", firestore)
                    }

                    else -> Unit
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // Fetch live updates for player statuses
    LaunchedEffect(Unit) {
        firestore.collection("players")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        context,
                        "Error fetching players: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }
                snapshot?.documents?.mapNotNull {
                    val name = it.getString("name")
                    val status = it.getString("status")
                    if (name != null && status != null && name != loggedInUsername) "$name - $status" else null
                }?.let {
                    players.clear()
                    players.addAll(it)
                }
            }
    }

    // Send a challenge when `opponent` is set
    LaunchedEffect(opponent) {
        opponent?.let { currentOpponent ->
            sendChallenge(
                fromPlayer = loggedInUsername,
                toPlayer = currentOpponent,
                firestore = firestore,
                onSuccess = {
                    Log.d("Challenge", "Challenge sent to $currentOpponent")
                    opponent = null // Reset opponent after sending
                },
                onFailure = { exception ->
                    Log.e("ChallengeError", "Failed to send challenge: ${exception.message}")
                }
            )
        }
    }

    // Listen for challenges targeted at the current user
    LaunchedEffect(Unit) {
        listenForChallenges(
            loggedInUsername = loggedInUsername,
            firestore = firestore,
            onIncomingChallenge = { id, fromPlayer, toPlayer ->
                incomingChallenge = Triple(id, fromPlayer, toPlayer)
            }
        )
    }

    // Handle Game Invitation Dialog
    incomingChallenge?.let { (challengeId, challenger, _) ->
        GameInvitation(
            onDismissRequest = {
                declineChallenge(challengeId, firestore) // Decline challenge
                incomingChallenge = null
            },
            onConfirmation = {
                acceptChallenge(
                    challengeId = challengeId,
                    fromPlayer = challenger,
                    toPlayer = loggedInUsername,
                    firestore = firestore,
                    navController = navController
                )
                incomingChallenge = null
            },
            dialogTitle = "Game Invitation",
            dialogText = "$challenger has challenged you to a game. Do you accept?",
            icon = Icons.Default.Notifications
        )
    }

    Background {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lobby",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                text = "Welcome, $loggedInUsername!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = "Online Players",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // A card-like background for player list for better readability
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(players) { player ->
                        PlayerRow(player = player) {
                            val selectedOpponent = player.substringBefore(" - ")
                            opponent = selectedOpponent
                        }
                    }
                }
            }
        }

        // GameInvitation (Dialog)
        incomingChallenge?.let { (challengeId, challenger, _) ->
            GameInvitation(
                onDismissRequest = {
                    declineChallenge(challengeId, firestore)
                    incomingChallenge = null
                },
                onConfirmation = {
                    acceptChallenge(
                        challengeId = challengeId,
                        fromPlayer = challenger,
                        toPlayer = loggedInUsername,
                        firestore = firestore,
                        navController = navController
                    )
                    incomingChallenge = null
                },
                dialogTitle = "Game Invitation",
                dialogText = "$challenger has challenged you to a game. Do you accept?",
                icon = Icons.Default.Notifications
            )
        }
    }
}

//Send challenge
fun sendChallenge(
    fromPlayer: String,
    toPlayer: String,
    firestore: FirebaseFirestore,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val challenge = mapOf(
        "fromPlayer" to fromPlayer,
        "toPlayer" to toPlayer,
        "status" to "pending"
    )

    firestore.collection("challenges")
        .add(challenge)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { exception -> onFailure(exception) }
}

//Listen for Challenges
fun listenForChallenges(
    loggedInUsername: String,
    firestore: FirebaseFirestore,
    onIncomingChallenge: (String, String, String) -> Unit
) {
    firestore.collection("challenges")
        .whereEqualTo("toPlayer", loggedInUsername) // Filter for challenges targeting the current player
        .whereEqualTo("status", "pending") // Only listen for pending challenges
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("ChallengeError", "Error listening to challenges: ${error.message}")
                return@addSnapshotListener
            }

            // Map Firestore documents to challenge data
            snapshot?.documents?.mapNotNull { doc ->
                val id = doc.id
                val fromPlayer = doc.getString("fromPlayer")
                val toPlayer = doc.getString("toPlayer")
                val status = doc.getString("status")

                if (fromPlayer != null && toPlayer != null && status == "pending") {
                    Triple(id, fromPlayer, toPlayer)
                } else null
            }?.firstOrNull()?.let { (id, fromPlayer, toPlayer) ->
                // Pass the first pending challenge to the callback
                onIncomingChallenge(id, fromPlayer, toPlayer)
            }
        }
}


//Accept Challenge
fun acceptChallenge(
    challengeId: String,
    fromPlayer: String,
    toPlayer: String,
    firestore: FirebaseFirestore,
    navController: NavController
) {
    firestore.collection("challenges").document(challengeId)
        .update("status", "accepted")
        .addOnSuccessListener {
            navController.navigate("GameBoardScreen?playerName=$toPlayer&opponentName=$fromPlayer")
        }
        .addOnFailureListener { exception ->
            Log.e("ChallengeError", "Error accepting challenge: ${exception.message}")
        }
}

//Decline Challenge
fun declineChallenge(challengeId: String, firestore: FirebaseFirestore) {
    firestore.collection("challenges").document(challengeId)
        .update("status", "declined")
        .addOnSuccessListener {
            Log.d("Challenge", "Challenge declined successfully")
        }
        .addOnFailureListener { exception ->
            Log.e("ChallengeError", "Error declining challenge: ${exception.message}")
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

