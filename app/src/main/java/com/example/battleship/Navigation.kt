package com.example.battleship

import androidx.compose.runtime.Composable
import androidx.navigation.NavHost

@Composable
fun Navigation(){
        NavHost(navController = navController, startDestination = "MainPage") {
            composable("MainPage") { MainScreen(navController, todoList) }
            composable("Lobby") { AddNoteScreen(navController, todoList) }
            composable("Battleground") { AddNoteScreen(navController, todoList) }
            }
        }