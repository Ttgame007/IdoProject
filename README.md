# Thwakz AI app

# System requierments:
[minimum]
RAM: 6gb RAM.
CPU: Snapdragon 7 gen 1/Dimensity 8000/Exynos 1280.
Storage: 4gb of free storage.
*with each phone within the CPU/RAM theres variability YMMV.
Will run small models (1-4B) fine.

[Recommended]
RAM: 8gb RAM.
CPU: Snapdragon 8 gen 1/Dimensity 9200/Exynos 2100.
Storage: 10+gb of free storage.
*with each phone within the CPU/RAM theres variability YMMV.
Will run bigger models (5-10B) fine. faster tokens with more RAM.

Android Studio Version Narwhal 3

[versions]
agp = "8.13.0"
junit = "4.13.2"
junitVersion = "1.1.5"
espressoCore = "3.5.1"
appcompat = "1.6.1"
material = "1.10.0"
activity = "1.8.0"
constraintlayout = "2.1.4"


dependencies 
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

[developed by Ido]
