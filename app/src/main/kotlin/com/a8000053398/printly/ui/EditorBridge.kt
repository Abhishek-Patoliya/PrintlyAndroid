package com.a8000053398.printly.ui

import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.viewmodel.EditorInitialAction

/** Bridges a freshly-created (not-yet-persisted) [PrintProject] — and an
 * optional initial sheet to open — into the "editor/new" nav route, since
 * Navigation Compose can't carry a whole object graph as a route argument.
 * Also where a launcher App Shortcut deep link (this app's equivalent of
 * iOS's Siri Shortcuts `AppIntentCoordinator`) stashes the project it wants
 * opened once the app is in the foreground. */
object EditorBridge {
    var pendingProject: PrintProject? = null
    var pendingInitialAction: EditorInitialAction? = null

    fun stage(project: PrintProject, initialAction: EditorInitialAction? = null) {
        pendingProject = project
        pendingInitialAction = initialAction
    }

    fun consume(): Pair<PrintProject, EditorInitialAction?>? {
        val project = pendingProject ?: return null
        val action = pendingInitialAction
        pendingProject = null
        pendingInitialAction = null
        return project to action
    }
}
