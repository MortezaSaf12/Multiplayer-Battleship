package com.example.battleship

//Imports
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "MainScreen") {
        composable("MainScreen") { MainScreen(navController) }
        composable("LobbyScreen") { LobbyScreen(navController) }
        composable("GameBoardScreen") { GameBoardScreen(navController) }
        //composable("ResultScreen") { ResultScreen(navController) }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    var username by remember { mutableStateOf(TextFieldValue("")) }

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
            color = Color.Black,
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
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.Transparent,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Black,
                cursorColor = Color.Black
            )
        )

        Button(
            onClick = {
                navController.navigate("LobbyScreen")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Join Lobby", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun LobbyScreen(navController: NavController) {
    val playersList = listOf("Player 1", "Player 2", "Player 3", "Player 4", "Player 5", "Player 6") // Replace with Firebase data later

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lobby",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Text(
            text = "Online Players",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(playersList) { player ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .border(
                            width = 1.dp,
                            color = Color.Black,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = player,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        // Navigate to the GameBoardScreen when challenging a player
                        navController.navigate("GameBoardScreen")
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ){
                        Text("Challenge")
                    }
                }
            }
        }
    }
}

@Composable
fun GameBoardScreen(navController: NavController) {
    val boardSize = 10
    val grid = remember { mutableStateListOf<MutableList<String>>() }
    val playerName = "Player 1"
    val opponentName = "Player 2"
    var turn by remember { mutableStateOf(playerName) }
    val statusMessage by remember { mutableStateOf("Game in Progress...") }

    // Initialize grid with "W" for water
    LaunchedEffect(Unit) {
        if (grid.isEmpty()) {
            for (i in 0 until boardSize) {
                grid.add(MutableList(boardSize) { "W" })
            }
        }
    }

    // Handle grid not being initialized
    if (grid.isEmpty()) {
        Text(
            text = "Loading game...",
            modifier = Modifier.fillMaxSize(),
            fontSize = 20.sp,
            color = Color.Black
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display game state
        Text(
            text = "$playerName vs $opponentName",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Current Turn: $turn",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = statusMessage,
            fontSize = 14.sp,
            color = Color.Blue,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Game board grid
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
                                        if (turn == playerName) {
                                            grid[i][j] = if (grid[i][j] == "W") "H" else "S"
                                            turn = opponentName
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // End turn button
        Button(
            onClick = {
                turn = if (turn == playerName) opponentName else playerName
            },
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("End Turn")
        }

        //Quit game
        Button(
            onClick = {
                navController.popBackStack() // Navigate to previous screen (Lobby screen)
            },
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            )
        ) {
            Text("Quit game")
        }
    }
}

