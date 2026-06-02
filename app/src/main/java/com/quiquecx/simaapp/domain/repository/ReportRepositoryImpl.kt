package com.quiquecx.simaapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.quiquecx.simaapp.domain.entity.ActivityEntity
import com.quiquecx.simaapp.domain.entity.ReportConfig
import com.quiquecx.simaapp.domain.entity.ReportFormat
import com.quiquecx.simaapp.domain.repository.ReportRepository
import com.quiquecx.simaapp.utils.CsvGenerator
import com.quiquecx.simaapp.utils.FileShareHelper
import com.quiquecx.simaapp.utils.HtmlGenerator
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val htmlGenerator: HtmlGenerator,
    private val csvGenerator: CsvGenerator,
    private val fileShareHelper: FileShareHelper
) : ReportRepository {

    override suspend fun getFilteredActivities(config: ReportConfig): List<ActivityEntity> {
        // 1. Obtener todos los proyectos de la empresa
        val projectsSnapshot = firestore.collection("Proyectos")
            .whereEqualTo("empresaId", config.companyId)
            .get()
            .await()
        val projectIds = projectsSnapshot.documents.mapNotNull { it.id }

        // 2. Si se especificó un proyecto, filtrar solo ese
        val targetProjectIds = if (config.projectId != null) {
            listOf(config.projectId)
        } else {
            projectIds
        }

        // 3. Para cada proyecto, obtener sus actividades
        val allActivities = mutableListOf<ActivityEntity>()
        for (projectId in targetProjectIds) {
            var query = firestore.collection("activities")
                .whereEqualTo("projectId", projectId)

            config.activityId?.let {
                query = query.whereEqualTo("id", it)
            }
            config.startDate?.let {
                query = query.whereGreaterThanOrEqualTo("fechaInicio", it.toDate())
            }
            config.endDate?.let {
                query = query.whereLessThanOrEqualTo("fechaInicio", it.toDate())
            }

            val snapshot = query.get().await()
            val activities = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ActivityEntity::class.java)?.copy(id = doc.id)
            }
            allActivities.addAll(activities)
        }

        // 4. Filtrar por productividad mínima (en memoria)
        return allActivities.filter { activity ->
            val productivity = calculateProductivity(activity)
            productivity >= config.minProductivity
        }
    }

    override suspend fun generateReport(activities: List<ActivityEntity>, config: ReportConfig): File {
        return when (config.format) {
            ReportFormat.PDF -> htmlGenerator.generate(activities, config)
            ReportFormat.EXCEL -> csvGenerator.generate(activities, config)
            ReportFormat.CSV -> csvGenerator.generate(activities, config)
        }
    }

    override suspend fun shareReport(file: File, format: ReportFormat, context: Context) {
        fileShareHelper.share(file, format, context)
    }

    private fun calculateProductivity(activity: ActivityEntity): Int {
        val estimado = activity.estimadoHoras.toDoubleOrNull() ?: 0.0
        val real = activity.horasAcumuladas
        return if (estimado > 0) ((real / estimado) * 100).toInt().coerceIn(0, 100) else 0
    }
}