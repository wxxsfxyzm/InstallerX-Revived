package com.rosan.installer.data.installer.model.entity

import com.rosan.installer.data.app.model.entity.AppEntity

data class SelectInstallEntity(
    val app: AppEntity,
    val selected: Boolean
) {
    companion object {
        /**
         * Finds and returns the single "latest" entity from a list of items in the same group.
         * The latest version is determined by the highest versionCode, with versionName as a tie-breaker.
         * This function contains the core sorting logic.
         *
         * @param items The list of SelectInstallEntity to process.
         * @return The single latest SelectInstallEntity, or null if the list is empty.
         */
        fun getLatestInGroup(items: List<SelectInstallEntity>): SelectInstallEntity? {
            // If the list is empty, there is no latest item.
            if (items.isEmpty()) {
                return null
            }
            // If there's only one, it's intrinsically the "latest".
            if (items.size == 1) {
                return items.first()
            }

            // Find the latest item by sorting and return it.
            return items.sortedWith(
                compareByDescending<SelectInstallEntity> {
                    (it.app as? AppEntity.BaseEntity)?.versionCode ?: 0L
                }.thenByDescending {
                    (it.app as? AppEntity.BaseEntity)?.versionName ?: ""
                }
            ).first() // The first one after sorting is the latest.
        }

        /**
         * Takes a list of entities from the same group and returns a new list
         * with only the "latest" version selected, as determined by getLatestInGroup().
         *
         * @param items The list of SelectInstallEntity to process.
         * @return A new list with the correct item selected.
         */
        fun selectLatestInGroup(items: List<SelectInstallEntity>): List<SelectInstallEntity> {
            // REUSE the core logic to find the single latest item.
            val latestItem = getLatestInGroup(items)

            // Return a new list where only the latest item is selected.
            // If latestItem is null (meaning the input list was empty), this correctly produces an empty list.
            return items.map { it.copy(selected = it == latestItem) }
        }
    }
}