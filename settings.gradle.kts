pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// This is the correct, modern way to declare repositories for your project's dependencies.
// The 'allprojects' block has been removed to avoid redundancy and potential errors.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "EchoFlow"
include(":app")

// The incorrect 'fun allprojects(function: Any) {}' has also been removed.
