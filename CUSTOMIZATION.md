# Customization

Some customers want a custom APK, with their own logo and other build-time customizations.

In order to do keep these customizations private, we do not store customer-specific customizations
in this repository.

We support fetching a "customization" directory from another git repository during build time.

In order to fetch from this repository, the following values must be set as environment variables or
in the `local.properties` file during Gradle configuration:

| Variable          | Description                                                                                                        |
|-------------------|--------------------------------------------------------------------------------------------------------------------|
| CUSTOM_REPOSITORY | URL to the Git repository that contains all customizations                                                         |
| GRGIT_USER        | Git credentials: username for checking out the repository                                                          |
| GRGIT_PASSWORD    | Git credentials: password for checking out the repository                                                          |
| CUSTOM_FOLDER     | Name/path of the "customization root" directory. Represented below as "customizationRoot", but it can be whatever. |
| CLIENT_FOLDER     | Name of the custom build directory within the "CUSTOM_FOLDER"                                                      |

> [!NOTE]
> This CUSTOM_FOLDER and CLIENT_FOLDER were made to keep compatibility with the old Android app.
> It would make more sense to combine them in a single variable with the full path to the custom build.
> If someone is looking into refactoring stuff, this is one I'd change at some point.

# The Customization Files

When customizing a build, it is expected that a directory with the following structure exists:

```
-customizationRoot/
    |-myCustomBuildA/
        |-custom-reloaded.json
        |-resources/
            |-[optional - custom resources to overwrite src/res]
```

## Build-time flags and values

Everything in the [default.json](./default.json) file can be
overwritten by the values in the `custom-reloaded.json`.
Both files must have the same exact format, and the `custom-reloaded.json` will have the
final say in which value will be bundled with the application.

Consider that the same property (`"app_name"`) is defined in all possible places (_i.e._ both in
`default` and `custom-reloaded` files, and both within a flavor and for all flavors).

`default.json`:

```json
{
    "flavors": {
        "prod": {
            "app_name": "Wire - Prod Flavor"
        }
    },
    "app_name": "Wire"
}
```

`custom-reloaded.json`

```json
{
    "flavors": {
        "prod": {
            "app_name": "My Custom App - Prod Flavor"
        }
    },
    "app_name": "My Custom App"
}
```

When building the `prod` flavor, the value will be chosen based on the following priority order (
highest priority first):

| Priority    | Value                       | Explanation                      |
|-------------|-----------------------------|----------------------------------|
| 1 - Highest | My Custom App - Prod Flavor | Custom build and specific flavor |
| 2           | My Custom App               | Custom build                     |
| 3           | Wire - Prod Flavor          | Specific flavor                  |
| 4 - Lowest  | Wire                        | Generic                          |

## Resources

These files will overwrite the Android Resources present in `src/prod/res` (or any other flavor).

During build time, these files will be copied into `src/prod/res`, `src/beta/res`, `src/dev/res`,
etc.
and will overwrite files in case of conflict.

This means that unlike the build flags and values, it is not possible to overwrite resources from a
specific flavor. This use case was never needed, but it _could_ be adapted for it.

Because of
[Resource Merging](https://developer.android.com/studio/write/add-resources#resource_merging),
this can be used to overwrite specific strings and other resources. For example, consider the
following structure:

```
-app/src/main/
    |-res/
        |-mipmap/
        |    |-file1.png (picture of a cat)
        |-values/
        |    |-all-strings.xml
-app/src/prod/
    |-res/
        |-mipmap/
        |    |-file1.png (picture of a monkey)

-custom/
    |-resources/
        |-mipmap/
        |    |-file1.png (picture of a dog)
        |-values/
        |    |-custom-string-replacements.xml
```

### Drawables and Mipmaps

Without the customization, the app would have a picture of a monkey as `file1.png` when building the
`prod` flavor, and a picture of a cat for the other flavors.
With the customization, the `file1.png` will be overwritten with the picture of a dog for all
flavors.

### Strings

Consider that the `all-strings.xml` file contains two strings:

```xml

<resources>
    <string name="buildType">Wire</string>
    <string name="color">Red</string>
</resources>
```

Consider that the `custom-string-replacements.xml` contains one string:

```xml

<resources>
    <string name="buildType">Customized!</string>
</resources>
```

Without Customization: the app displays "Wire" for the `buildType` string.
With Customization: the app displays "Customized!" for the `buildType`.

Again, thanks
to [Resource Merging](https://developer.android.com/studio/write/add-resources#resource_merging),
the `color` string doesn't need to be overwritten. Because the `custom-string-replacements` file
will be copied into the `prod` directory, the `buildType` will be chosen based on
the flavor over the generic one put in `main`.

# Implementation details

Take a look into the [customization files within buildSrc](buildSrc/src/main/kotlin/customization)
for more details on how it works.
