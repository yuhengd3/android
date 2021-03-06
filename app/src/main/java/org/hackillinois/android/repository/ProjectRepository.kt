package org.hackillinois.android.repository

import androidx.lifecycle.LiveData
import org.hackillinois.android.App
import org.hackillinois.android.database.entity.Project
import org.hackillinois.android.model.projects.ProjectsList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class ProjectRepository {
    private val projectsDao = App.database.projectsDao()

    fun fetchProjectsinCategory(category: String): LiveData<List<Project>> {
        refreshAll()
        return projectsDao.getProjectsWithTag(category)
    }

    fun fetchProject(projectId: String): LiveData<Project> {
        refreshAll()
        return projectsDao.getProject(projectId)
    }

    private fun refreshAll() {
        App.getAPI().allProjects().enqueue(object : Callback<ProjectsList> {
            override fun onResponse(call: Call<ProjectsList>, response: Response<ProjectsList>) {
                if (response.isSuccessful) {
                    val projectsList: List<Project> = response.body()?.projects ?: return
                    thread { projectsDao.clearTableAndInsertProjects(projectsList) }
                }
            }

            override fun onFailure(call: Call<ProjectsList>, t: Throwable) {}
        })
    }

    companion object {
        val instance: ProjectRepository by lazy { ProjectRepository() }
    }
}
