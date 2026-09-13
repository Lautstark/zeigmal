package de.lautstark.zeigmal.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import de.lautstark.zeigmal.R

/** The family's speech bubble in zeigmal's teal. Never recoloured, never animated; things happen around it. */
@Composable
fun Mark(modifier: Modifier = Modifier) {
    Image(painter = painterResource(R.drawable.ic_mark), contentDescription = null, modifier = modifier)
}
