package com.gamehub.app

interface Pausable {
    fun pauseGame()
    fun resumeGame()
}

const val MODE_FRIEND = 0
const val MODE_BOT = 1
