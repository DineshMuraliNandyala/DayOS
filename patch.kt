    // Export Room schema JSON for migration auditing
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

ktlint {
