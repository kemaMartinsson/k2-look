package com.kema.k2look.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kema.k2look.R
import com.kema.k2look.viewmodel.LayoutBuilderViewModel
import com.kema.k2look.viewmodel.MainViewModel

@Composable
fun MainScreen(
        viewModel: MainViewModel = viewModel(),
        layoutBuilderViewModel: LayoutBuilderViewModel = viewModel(),
        onBack: () -> Unit = {},
        openUpdateDialog: Boolean = false,
        onUpdateDialogHandled: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Wire LayoutBuilderViewModel at startup so gesture screen cycling works
    // regardless of whether the user has ever visited the Fields tab.
    LaunchedEffect(Unit) {
        layoutBuilderViewModel.setBridge(viewModel.getBridge())
        viewModel.setLayoutBuilderViewModel(layoutBuilderViewModel)
    }

    // Navigate to About tab and open update dialog when launched from notification
    LaunchedEffect(openUpdateDialog) {
        if (openUpdateDialog) {
            selectedTabIndex = 3 // About tab
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo and Title Section
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "K2Look Logo",
                    modifier = Modifier.height(26.dp)
            )
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            Text(
                    text = "K2Look",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
            )
        }

        // 2x2 Tab Grid
        TwoRowTabGrid(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                tabs = listOf("Status", "Fields", "Gestures", "About")
        )

        // Content based on selected tab
        when (selectedTabIndex) {
            0 -> StatusTab(viewModel, uiState)
            1 -> DataFieldBuilderTab(mainViewModel = viewModel, viewModel = layoutBuilderViewModel)
            2 -> GesturesTab(viewModel, uiState)
            3 ->
                    AboutTab(
                            viewModel = viewModel,
                            uiState = uiState,
                            openUpdateDialogOnStart = openUpdateDialog,
                            onUpdateDialogHandled = onUpdateDialogHandled
                    )
        }
    }
}

@Composable
private fun TwoRowTabGrid(selectedTabIndex: Int, onTabSelected: (Int) -> Unit, tabs: List<String>) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: tabs 0 and 1
        Row(modifier = Modifier.fillMaxWidth()) {
            for (index in 0..1) {
                val selected = selectedTabIndex == index
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .background(if (selected) primary else surface)
                                        .clickable { onTabSelected(index) }
                                        .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = tabs[index],
                            color = if (selected) onPrimary else onSurface,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        // Row 2: tabs 2 and 3
        Row(modifier = Modifier.fillMaxWidth()) {
            for (index in 2..3) {
                val selected = selectedTabIndex == index
                Box(
                        modifier =
                                Modifier.weight(1f)
                                        .background(if (selected) primary else surface)
                                        .clickable { onTabSelected(index) }
                                        .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Text(
                            text = tabs[index],
                            color = if (selected) onPrimary else onSurface,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                    )
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
