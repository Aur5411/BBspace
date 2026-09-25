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

// Optional local checkout for deterministic development when GitHub Packages credentials are not
// available. Production/default resolution still uses the exact declared package coordinates.
providers.gradleProperty("bili.miuix.source").orNull?.let { sourcePath ->
    includeBuild(sourcePath) {
        dependencySubstitution {
            substitute(module("top.yukonga.miuix.kmp:miuix-core-android"))
                .using(project(":miuix-core"))
            substitute(module("top.yukonga.miuix.kmp:miuix-ui-android"))
                .using(project(":miuix-ui"))
            substitute(module("top.yukonga.miuix.kmp:miuix-shader-android"))
                .using(project(":miuix-shader"))
            substitute(module("top.yukonga.miuix.kmp:miuix-blur-android"))
                .using(project(":miuix-blur"))
            substitute(module("top.yukonga.miuix.kmp:miuix-preference-android"))
                .using(project(":miuix-preference"))
            substitute(module("top.yukonga.miuix.kmp:miuix-squircle-android"))
                .using(project(":miuix-squircle"))
            substitute(module("top.yukonga.miuix.kmp:miuix-icons-android"))
                .using(project(":miuix-icons"))
            substitute(module("top.yukonga.miuix.kmp:miuix-nav-android"))
                .using(project(":miuix-nav"))
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // miuix 使用 Maven Central 上的正式版（0.9.4），无需 GitHub Packages 认证。
        // Cling Repo
        maven { 
            url = uri("http://4thline.org/m2") 
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "BBspace"
include(":app")
include(":baselineprofile")
include(":settings-core")
include(":network-core")
include(":plugin-sdk")
include(":design-system")
include(":dolby-ffmpeg-decoder")
include(":danmaku-engine")

 
