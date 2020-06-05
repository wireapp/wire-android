package scripts

tasks.register("clean").configure {
    delete("build")
}

tasks.create("fernandoCejas") {
    description = "This is Fernando"
    println("fernandito")
}

tasks.create("HelloWorld") {
    description = "HelloWorld"
    print("Hello World Fernando! -> ${BuildPlugins.androidApplication} - :)")
}