package com.vkard.pro.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.vkard.pro.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Fade-in (300ms)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300)
        )
        
        // 2. Hold (1000ms)
        delay(1000)
        
        // 3. Fade-out (250ms)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 250)
        )
        
        // 4. Trigger Navigation
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "VKARD Logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(170.dp)
                .alpha(alpha.value)
        )
    }
}
