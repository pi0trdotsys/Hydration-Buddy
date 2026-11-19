package com.kropi.hydration.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.kropi.hydration.R

/** „3 dni", „1 dzień", „5 dni" — polska odmiana (i angielska) przez zasoby plurals. */
fun Context.daysText(n: Int): String = resources.getQuantityString(R.plurals.n_days, n, n)
fun Context.timesText(n: Int): String = resources.getQuantityString(R.plurals.n_times, n, n)
fun Context.entriesText(n: Int): String = resources.getQuantityString(R.plurals.n_entries, n, n)

@Composable fun daysText(n: Int): String = LocalContext.current.daysText(n)
@Composable fun timesText(n: Int): String = LocalContext.current.timesText(n)
@Composable fun entriesText(n: Int): String = LocalContext.current.entriesText(n)
