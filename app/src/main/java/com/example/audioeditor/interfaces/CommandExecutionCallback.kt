package com.example.audioeditor.interfaces

interface CommandExecutionCallback {
    fun onCommandExecutionSuccess()
    fun onCommandExecutionFailure(errorMessage: String)
}