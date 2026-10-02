package com.example.ui.screens.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentCategories
import com.example.ui.components.DocumentCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultAmber
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultRed
import com.example.ui.theme.VaultRedContainer
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.SortBy
import com.example.ui.viewmodel.VaultViewModel

@Composable
fun DocumentsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val filter by viewModel.documentFilter.collectAsStateWithLifecycle()
    val currentTheme by viewModel.appTheme.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedDocIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val categories = listOf("All") + DocumentCategories.DEFAULT_NAMES

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(currentTheme.bgColor)
            .testTag("documents_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Row / Selection Mode Top Row
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(currentTheme.surfaceElevatedColor)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedDocIds = emptySet()
                            },
                            modifier = Modifier.testTag("close_doc_selection_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Selection",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedDocIds.size} Selected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentTheme.primaryColor
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val allSelected = documents.isNotEmpty() && selectedDocIds.size == documents.size
                        Button(
                            onClick = {
                                selectedDocIds = if (allSelected) emptySet() else documents.map { it.id }.toSet()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (allSelected) currentTheme.primaryColor.copy(alpha = 0.2f) else currentTheme.surfaceColor,
                                contentColor = currentTheme.primaryColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("select_all_docs_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allSelected) "Deselect" else "Select All (${documents.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (selectedDocIds.isNotEmpty()) {
                                    showDeleteConfirm = true
                                }
                            },
                            enabled = selectedDocIds.isNotEmpty(),
                            modifier = Modifier.testTag("delete_selected_docs_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = if (selectedDocIds.isNotEmpty()) VaultRed else TextMuted
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Document Vault",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${documents.size} items protected",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (documents.isNotEmpty()) {
                            Button(
                                onClick = { isSelectionMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentTheme.surfaceElevatedColor,
                                    contentColor = currentTheme.primaryColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp).testTag("select_docs_mode_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = currentTheme.primaryColor
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(VaultSurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Newest First", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSortBy(SortBy.DATE_DESC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Oldest First", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSortBy(SortBy.DATE_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Name (A to Z)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSortBy(SortBy.NAME_ASC)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Type", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSortBy(SortBy.FILE_TYPE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Size (Largest)", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSortBy(SortBy.FILE_SIZE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Expiry Date", color = TextPrimary) },
                                    onClick = {
                                        viewModel.setSortBy(SortBy.EXPIRY_SOON)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Category Scroll Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { categoryName ->
                    val isSelected = if (categoryName == "All") filter.category == null else filter.category == categoryName
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.setDocumentCategory(if (categoryName == "All") null else categoryName)
                        },
                        label = { Text(categoryName, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color(0xFF003822),
                            containerColor = VaultSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("category_chip_$categoryName")
                    )
                }
            }

            // Expiry Quick Toggles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter.isExpiringSoonOnly,
                    onClick = { viewModel.setFilterExpiringSoon(!filter.isExpiringSoonOnly) },
                    label = { Text("Expiring Soon (30d)", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VaultAmber,
                        selectedLabelColor = Color(0xFF451A03),
                        containerColor = VaultSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("filter_expiring_soon")
                )

                FilterChip(
                    selected = filter.isExpiredOnly,
                    onClick = { viewModel.setFilterExpired(!filter.isExpiredOnly) },
                    label = { Text("Expired", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VaultRed,
                        selectedLabelColor = Color.White,
                        containerColor = VaultSurfaceElevated,
                        labelColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("filter_expired")
                )
            }

            // Documents List
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "📂", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No documents found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to add or scan a document into this category.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val isSelected = selectedDocIds.contains(doc.id)
                        DocumentCard(
                            document = doc,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedDocIds = if (isSelected) selectedDocIds - doc.id else selectedDocIds + doc.id
                                } else {
                                    viewModel.selectDocument(doc)
                                }
                            },
                            onFavoriteToggle = { viewModel.toggleDocumentFavorite(doc) },
                            modifier = if (isSelected) Modifier.border(2.dp, currentTheme.primaryColor, RoundedCornerShape(14.dp)) else Modifier
                        )
                    }
                }
            }
        }

        // Floating Bottom Action Bar for Multi-Selection
        if (isSelectionMode && selectedDocIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(currentTheme.surfaceElevatedColor)
                    .border(1.dp, currentTheme.borderColor, RoundedCornerShape(30.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultRedContainer,
                            contentColor = VaultRed
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("floating_delete_docs_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete (${selectedDocIds.size})", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            isSelectionMode = false
                            selectedDocIds = emptySet()
                        }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }

        // Floating Action Button (hidden in selection mode)
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.ADD_DOCUMENT) },
                containerColor = currentTheme.primaryColor,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_document_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Document",
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = VaultSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
                title = {
                    Text(
                        text = "Delete ${selectedDocIds.size} Document(s)?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete ${selectedDocIds.size} selected document(s)? They will be safely moved to Trash.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.batchDeleteDocuments(selectedDocIds.toList())
                            selectedDocIds = emptySet()
                            isSelectionMode = false
                            showDeleteConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VaultRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_batch_delete_docs_btn")
                    ) {
                        Text("Delete ${selectedDocIds.size} Documents", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
