Compass app first built in traditional activity/xml artchitecture and then converted to use jetpack compose. Added permisions to ask user for location permission and device's hardware components.
App displays a compass image, rotation degrees and location. Uses sensor event listener class to continuously update rotation value inside its callback method. To obtain device location, callback method first returns
longitude and latitude values which gets passed on to another method which uses Geocoder to get the city and country name using the 2 values.

First time using Gradle Kotlin DSL (build.gradle.kts) file

Libraries Used-

Jetpack Compose  
Location  
Coroutines
