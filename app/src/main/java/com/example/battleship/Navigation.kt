package com.example.battleship

//Imports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
        composable("ResultScreen") { ResultScreen(navController) }
    }
}

@Composable
fun MainScreen(navController: NavController) {
    var username by remember { mutableStateOf(TextFieldValue("")) }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Battleship",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Enter Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )

        if (showError) {
            Text(
                text = "Username must be at least 3 characters",
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (username.text.length >= 3) {
                    showError = false
                    navController.navigate("LobbyScreen")
                } else {
                    showError = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            Text("JOIN LOBBY")
        }
    }
}

@Composable
fun LobbyScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        //verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lobby Screen",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}