package com.example.cdplaya.data

enum class FolderSelectionMode {
    ALL,
    CUSTOM
}

data class FolderSelection(
    val mode: FolderSelectionMode = FolderSelectionMode.ALL,
    val customFolders: Set<String> = emptySet()
) {
    fun includes(folderPath: String): Boolean =
        mode == FolderSelectionMode.ALL || folderPath in customFolders

    fun effectiveFolders(availableFolders: Collection<String>): Set<String> =
        if (mode == FolderSelectionMode.ALL) {
            availableFolders.toSet()
        } else {
            customFolders.toSet()
        }

    fun toggle(folderPath: String, availableFolders: Collection<String>): FolderSelection =
        when (mode) {
            FolderSelectionMode.ALL -> FolderSelection(
                mode = FolderSelectionMode.CUSTOM,
                customFolders = availableFolders.toSet() - folderPath
            )
            FolderSelectionMode.CUSTOM -> copy(
                customFolders = if (folderPath in customFolders) {
                    customFolders - folderPath
                } else {
                    customFolders + folderPath
                }
            )
        }

    companion object {
        val All = FolderSelection()

        fun fromStored(
            storedMode: String?,
            storedFolders: Set<String>
        ): FolderSelection {
            val explicitMode = storedMode?.let { value ->
                FolderSelectionMode.entries.firstOrNull { it.name == value }
            }
            val mode = explicitMode ?: if (storedFolders.isEmpty()) {
                FolderSelectionMode.ALL
            } else {
                FolderSelectionMode.CUSTOM
            }
            return FolderSelection(mode, storedFolders.toSet())
        }
    }
}
