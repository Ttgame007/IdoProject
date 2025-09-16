# Thwakz AI App

A mobile app designed to run lightweight and larger AI models efficiently, depending on the device specs.

---

## System Requirements

### Minimum

- **RAM**: 6 GB
- **CPU**: Snapdragon 7 Gen 1 / Dimensity 8000 / Exynos 1280
- **Storage**: 4 GB of free storage

> **Note:** There may be variability depending on the phone’s CPU and RAM. Your mileage may vary.  
> This setup will run small models (1-4B parameters) smoothly.

### Recommended

- **RAM**: 8 GB
- **CPU**: Snapdragon 8 Gen 1 / Dimensity 9200 / Exynos 2100
- **Storage**: 10+ GB of free storage

> **Note:** There may be variability depending on the phone’s CPU and RAM. Your mileage may vary.  
> This setup will run larger models (5-10B parameters) more efficiently and with faster token processing.

### Android Studio Version

- **Version**: Narwhal 3

---

## Versions

- **AGP**: 8.13.0
- **JUnit**: 4.13.2
- **JUnit Version**: 1.1.5
- **Espresso Core**: 3.5.1
- **AppCompat**: 1.6.1
- **Material**: 1.10.0
- **Activity**: 1.8.0
- **ConstraintLayout**: 2.1.4

---

## Dependencies

```gradle
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Unit Test Dependencies
    testImplementation(libs.junit)
    
    // Android Test Dependencies
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
