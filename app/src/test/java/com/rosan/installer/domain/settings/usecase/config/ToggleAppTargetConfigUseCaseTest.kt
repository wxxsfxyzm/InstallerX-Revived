// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 InstallerX Revived contributors
package com.rosan.installer.domain.settings.usecase.config

import com.rosan.installer.domain.settings.model.app.AppModel
import com.rosan.installer.domain.settings.repository.AppRepository
import com.rosan.installer.domain.settings.util.AppOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class ToggleAppTargetConfigUseCaseTest {
    @Test
    fun `assigning unknown scope creates a nullable package row`() = runTest {
        val repository = RecordingAppRepository()

        ToggleAppTargetConfigUseCase(repository)(packageName = null, configId = 4L, applied = true)

        assertEquals(null, repository.model?.packageName)
        assertEquals(4L, repository.model?.configId)
    }

    @Test
    fun `assigning unknown scope moves the existing row between profiles`() = runTest {
        val repository = RecordingAppRepository(
            AppModel(
                id = 7L,
                packageName = null,
                configId = 2L,
                createdAt = 1L,
                modifiedAt = 1L,
            ),
        )

        ToggleAppTargetConfigUseCase(repository)(packageName = null, configId = 4L, applied = true)

        assertEquals(7L, repository.model?.id)
        assertEquals(null, repository.model?.packageName)
        assertEquals(4L, repository.model?.configId)
    }

    @Test
    fun `removing unknown scope deletes the existing nullable package row`() = runTest {
        val repository = RecordingAppRepository(
            AppModel(
                id = 7L,
                packageName = null,
                configId = 4L,
                createdAt = 1L,
                modifiedAt = 1L,
            ),
        )

        ToggleAppTargetConfigUseCase(repository)(packageName = null, configId = 4L, applied = false)

        assertNull(repository.model)
    }
}

private class RecordingAppRepository(initial: AppModel? = null) : AppRepository {
    var model: AppModel? = initial
        private set

    override suspend fun all(order: AppOrder): List<AppModel> = listOfNotNull(model)

    override fun flowAll(order: AppOrder): Flow<List<AppModel>> = emptyFlow()

    override suspend fun find(id: Long): AppModel? = model?.takeIf { it.id == id }

    override fun flowFind(id: Long): Flow<AppModel?> = emptyFlow()

    override suspend fun findByPackageName(packageName: String?): AppModel? = model?.takeIf {
        it.packageName == packageName
    }

    override fun flowFindByPackageName(packageName: String?): Flow<AppModel?> = emptyFlow()

    override suspend fun update(model: AppModel) {
        this.model = model
    }

    override suspend fun insert(model: AppModel) {
        this.model = model.copy(id = if (model.id == 0L) 1L else model.id)
    }

    override suspend fun delete(model: AppModel) {
        if (this.model?.id == model.id) this.model = null
    }
}
