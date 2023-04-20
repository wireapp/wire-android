List<String> defineFlavor() {
    //check if the pipeline has the custom flavor env variable set
    def overwrite = env.CUSTOM_FLAVOR
    if(overwrite != null) {
        return overwrite
    }

    def branchName = env.BRANCH_NAME

    if (branchName == "main") {
        return ['Beta']
    } else if(branchName == "develop") {
        return ['Staging', 'Dev']
    } else if(branchName == "prod") {
        return ['Prod']
    } else if(branchName == "internal") {
        return ['Internal']
    }
    return ['Staging', 'Dev']
}

String defineBuildType(String flavor) {
    def overwrite = env.CUSTOM_BUILD_TYPE
    if(overwrite != null) {
        return overwrite
    }

    // internal is used for wire beta builds
    if (flavor == 'Beta') {
        return 'Release'
    } else if (flavor == 'Prod') {
        return "Compatrelease"
    }
    // use the scala client signing keys for testing upgrades.
    return "Compat"
}

// build the assemble command for the given flavors
// if there is more than one flavor, we need to build in parallel
// example:
// ./gradlew assembleStagingDebug assembleDevDebug --parallel
// ./gradlew assembleStagingDebug
String buildAssembleCommand(List<String> flavors) {
    def command = "./gradlew"
    flavors.each { flavor->
      def buildType = defineBuildType(flavor)
      command += (" assemble"+flavor+buildType)
    }
    if (flavors.size() > 1) {
      command += " --parallel"
    }

    return command
}

String buildBundleCommand(List<String> flavors) {
    def command = './gradlew'
    flavors.each { flavor ->
      def buildType = defineBuildType(flavor)
      if(shouldCreateBundle(buildType)) {
        command += (" bundle"+flavor+buildType)
      }
    }
    if (flavors.size() > 1) {
      command += " --parallel"
    }
    if(command.size() > 10) {
      return command
    }
    return null
}

def shouldCreateBundle(String buildType) {
    if (buildType == "Release" || buildType == "Compatrelease") {
      return true
    }
    return false
}

def shouldPublishToStore(String flavor, String buildType) {
    if (buildType == "Release" && flavor == "Beta") {
      return true
    }
    return false
}

def defineTrackName() {
    def overwrite = env.CUSTOM_TRACK
    def branchName = env.BRANCH_NAME

    if(overwrite != null) {
        return overwrite
    } else if (branchName == "main") {
        return 'internal'
    }
    return 'None'
}

String shellQuote(String s) {
    // Quote a string so it's suitable to pass to the shell
    return "'" + s.replaceAll("'", "'\"'\"'") + "'"
}

def postGithubComment(String changeId, String body) {
    def authHeader = shellQuote("Authorization: token ${env.GITHUB_API_TOKEN}")
    def apiUrl = shellQuote("https://api.github.com/repos/wireapp/wire-android-reloaded/issues/${changeId}/comments")

    // The comment body must be quoted for embedding into a JSON string,
    // and the JSON string must be quoted for embedding into the shell command
    // line. Note well: the backslash character has a special meaning in
    // both the first argument (regular expression pattern) and the second
    // (Matcher.replaceAll() escaping character) of String.replaceAll(); hence,
    // yet another level of escaping is required here!

    def payload = body.replaceAll('\\\\', '\\\\\\\\').replaceAll('"', '\\\\"').replaceAll('\n', '\\\\n')
    def json = shellQuote('{"body":"' + payload + '"}')

    // Note the interpolated variables here come from Groovy -- the command
    // line which the shell interpreter executes is fully rendered, and contains
    // no unsubstituted variables
    sh "curl -s -H ${authHeader} -X POST -d ${json} ${apiUrl}"

}

pipeline {
  agent {
    docker {
      args '-u ${GUID_AGENT} --network build-machine -v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=unix:///var/run/docker.sock'
      label 'android-reloaded-builder'
      image 'android-reloaded-agent:latest'
    }
  }

  options { disableConcurrentBuilds(abortPrevious: true) }

  stages {
    stage('Precondition Checks') {
      parallel {
        stage('Check SDK/NDK') {
          steps {
            script {
              last_started = env.STAGE_NAME

             // TODO: make sure the flavor in the echo is not empty
              sh '''echo ANDROID_HOME: $ANDROID_HOME
                    echo NDK_HOME: $NDK_HOME
                    echo FLAVOR: $flavorText
                    echo AdbPort: $adbPort
                    echo EmulatorPrefix: $emulatorPrefix
                    echo TrackName: $trackName
                    echo ChangeId: $CHANGE_ID
                    '''
            }
          }
        }

        stage('Create properties file') {
          steps {
           withCredentials([
                    string(credentialsId: 'GITHUB_PACKAGES_USER', variable: 'GITHUB_USER'),
                    string(credentialsId: 'GITHUB_PACKAGES_TOKEN', variable: 'GITHUB_TOKEN')
                  ]) {
                    sh '''FILE=/${propertiesFile}
                          if test -f "$FILE"; then
                              echo "${propertiesFile} exists already, deleting"
                              rm {$propertiesFile}
                          fi
                          echo "sdk.dir="$ANDROID_HOME >> ${propertiesFile}
                          echo "ndk.dir="$NDK_HOME >> ${propertiesFile}
                          echo "github.package_registry.user="$GITHUB_USER >> ${propertiesFile}
                          echo "github.package_registry.token="$GITHUB_TOKEN >> ${propertiesFile}
                       '''
             }
          }
        }
      }
    }

    stage('Load Env Variables') {
      steps {
        configFileProvider([
          configFile(fileId: env.GROOVY_ENV_VARS_REFERENCE_FILE_NAME, variable: 'GROOVY_FILE_THAT_SETS_VARIABLES')
        ]) {
          load env.GROOVY_FILE_THAT_SETS_VARIABLES
        }
      }
    }

    stage('Fetch Signing Files') {
      steps {
        configFileProvider([
         configFile(fileId: env.COMPAT_KEYSTORE_FILE_ID, targetLocation: env.COMPAT_ENC_TARGET_LOCATION),
         configFile(fileId: env.COMPAT_RELEASE_KEYSTORE_FILE_ID, targetLocation: env.COMPAT_RELEASE_ENC_TARGET_LOCATION),
         configFile(fileId: env.DEBUG_KEYSTORE_FILE_ID, targetLocation: env.DEBUG_ENC_TARGET_LOCATION),
         configFile(fileId: env.RELEASE_KEYSTORE_FILE_ID, targetLocation: env.RELEASE_ENC_TARGET_LOCATION),
        ]) {
          sh '''
            base64 --decode $COMPAT_ENC_TARGET_LOCATION > $COMPAT_DEC_TARGET_LOCATION
            base64 --decode $COMPAT_RELEASE_ENC_TARGET_LOCATION > $COMPAT_RELEASE_DEC_TARGET_LOCATION
            base64 --decode $DEBUG_ENC_TARGET_LOCATION > $DEBUG_DEC_TARGET_LOCATION
            base64 --decode $RELEASE_ENC_TARGET_LOCATION > $RELEASE_DEC_TARGET_LOCATION
          '''
        }
      }
    }

    stage('Spawn Wrapper and Emulators') {
      parallel {
        stage('Fetch submodules') {
          steps {
            sh 'git submodule update --init --recursive'
          }
        }

        stage('Copy local.properties to Kalium') {
          steps {
            sh '\\cp -f ${propertiesFile} kalium/${propertiesFile}'
          }
        }

        stage('Spawn Gradle Wrapper') {
          steps {
            withGradle() {
              sh './gradlew -Porg.gradle.jvmargs=-Xmx16g wrapper'
            }
          }
        }

        stage('Spawn Emulator 9.0') {
          when {
            expression { env.runAcceptanceTests.toBoolean() }
          }
          steps {
            sh '''docker rm ${emulatorPrefix}_9 || true
                  docker run --privileged --network build-machine -d -e DEVICE="Nexus 5" -e DATAPARTITION="2g" --name ${emulatorPrefix}-${BUILD_NUMBER}_9 budtmo/docker-android-x86-9.0'''
          }
        }

        stage('Spawn Emulator 10.0') {
          when {
            expression { env.runAcceptanceTests.toBoolean() }
          }
          steps {
            sh '''docker rm ${emulatorPrefix}_10 || true
                  docker run --privileged --network build-machine -d -e DEVICE="Nexus 5" -e DATAPARTITION="2g" --name ${emulatorPrefix}-${BUILD_NUMBER}_10 budtmo/docker-android-x86-10.0'''
          }
        }
      }
    }

    stage('Clean') {
      steps {
        script {
          last_started = env.STAGE_NAME
        }
        withGradle() {
          sh './gradlew clean'
        }
      }
    }

    stage('Static Code Analysis') {
      when {
        expression { env.runStaticCodeAnalysis.toBoolean() }
      }
      steps {
        script {
          last_started = env.STAGE_NAME
        }

        withGradle() {
          sh './gradlew staticCodeAnalysis'
        }

      }
    }

    stage('Connect Emulators') {
      parallel {
        stage('Emulator 10.0') {
          when {
            expression { env.runAcceptanceTests.toBoolean() }
          }
          steps {
            sh 'adb connect ${emulatorPrefix}-${BUILD_NUMBER}_10:${adbPort}'
          }
        }

        stage('Emulator 9.0') {
          when {
            expression { env.runAcceptanceTests.toBoolean() }
          }
          steps {
            sh 'adb connect ${emulatorPrefix}-${BUILD_NUMBER}_9:${adbPort}'
          }
        }
      }
    }

    stage('Uninstall App') {
      when {
        expression { env.runAcceptanceTests.toBoolean() }
      }
      steps {
        script {
          last_started = env.STAGE_NAME
        }

        withGradle() {
          sh './gradlew uninstallAll'
        }

      }
    }

    stage('Assemble APK') {
      steps {
        script {
          last_started = env.STAGE_NAME
          def list = defineFlavor()
          def assembleCommand = buildAssembleCommand(list)
          withGradle() {
            sh assembleCommand
          }
        }
      }
    }

    stage('Bundle AAB') {
      steps {
        script {
          last_started = env.STAGE_NAME
          def list = defineFlavor()
          def bundleCommand = buildBundleCommand(list)
          if (bundleCommand != null) {
            withGradle() {
              sh bundleCommand
            }
          }
        }
      }
    }

    stage('Archive') {
        parallel {
          stage('AAB') {
            steps {
              script {
                def list = defineFlavor()
                list.each { flavor ->
                  def buildType = defineBuildType(flavor)
                  if (shouldCreateBundle(buildType)) {
                    def lsBundleCommand = "ls -la app/build/outputs/bundle/${flavor.toLowerCase()}${buildType.capitalize()}/"
                    def archiveCommand = "app/build/outputs/bundle/${flavor.toLowerCase()}${buildType.capitalize()}/com.wire.android-*.aab"
                    sh lsBundleCommand
                    archiveArtifacts(artifacts: archiveCommand, allowEmptyArchive: true, onlyIfSuccessful: true)
                  }
                }
              }
            }
          }

          stage('APK') {
            steps {
              script {
                def list = defineFlavor()
                list.each { flavor ->
                  def buildType = defineBuildType(flavor)
                  def lsApkCommand = "ls -la app/build/outputs/apk/${flavor.toLowerCase()}/${buildType.toLowerCase()}/"
                  def archiveCommand = "app/build/outputs/apk/${flavor.toLowerCase()}/${buildType.toLowerCase()}/com.wire.android-*.apk, app/build/**/mapping/**/*.txt, app/build/**/logs/**/*.txt"
                  sh lsApkCommand
                  archiveArtifacts(artifacts: archiveCommand, allowEmptyArchive: true, onlyIfSuccessful: true)
                }
              }
            }
          }
        }
      }

    stage("Upload") {
      parallel {
          stage('S3 Bucket') {
            steps {
              script {
                def list = defineFlavor()
                list.each { flavor ->
                  def buildType = defineBuildType(flavor)
                  def checkFolderCommand = "ls -la app/build/outputs/apk/${flavor.toLowerCase()}/${buildType.toLowerCase()}/"
                  def workingDir = "app/build/outputs/apk/${flavor.toLowerCase()}/${buildType.toLowerCase()}/"
                  def megazordPath = "megazord/android/reloaded/${flavor.toLowerCase()}/${buildType.toLowerCase()}/"
                  echo 'Checking folder before S3 Bucket upload ${flavor} ${buildType}'
                  sh checkFolderCommand
                  echo 'Uploading file to S3 Bucket ${flavor} ${buildType}'
                  s3Upload(acl:'Private', workingDir: workingDir, includePathPattern:'com.wire.android-*.apk', bucket: 'z-lohika', path: megazordPath)
                  if (env.BRANCH_NAME.startsWith("PR-") || env.BRANCH_NAME == "develop") {
                    s3Upload(acl:'Private', workingDir: workingDir, includePathPattern:'com.wire.android-*.apk', bucket: 'z-lohika', path: "megazord/android/reloaded/by-branch/${env.BRANCH_NAME}/")
                  }
                }
              }
            }
          }

          stage('Playstore') {
            when {
              expression { env.trackName != 'None' && env.CHANGE_ID == null }
            }
            steps {
              script {
                def flavors = defineFlavor()
                flavors.each { flavor ->
                  def buildType = defineBuildType(flavor)
                  if (shouldCreateBundle(buildType)) {
                    def lsBundleCommand = "ls -la app/build/outputs/bundle/${flavor.toLowerCase()}${buildType.capitalize()}/"
                    def filePath = "app/build/outputs/bundle/${flavor.toLowerCase()}${buildType.capitalize()}/com.wire.android-*.aab"
                    echo 'Checking folder before playstore upload'
                    sh lsBundleCommand
                    echo 'Uploading file to Playstore track ${trackName}'
                    androidApkUpload(googleCredentialsId: 'google play access', filesPattern: filePath, trackName: "${trackName}", rolloutPercentage: '100', releaseName: "${trackName} Release")
                  }
                }
              }
            }
          }
      }
    }
  }

  environment {
    propertiesFile = 'local.properties'
    unitTestFlavor = 'Dev'
    unitTestBuildType = 'Debug'
    adbPort = '5555'
    emulatorPrefix = "${BRANCH_NAME.replaceAll('/','_')}"
    trackName = defineTrackName()
    runAcceptanceTests = true
    runUnitTests = true
    runStaticCodeAnalysis = true
    ENABLE_SIGNING = "TRUE"
  }

  post {
    failure {
      script {
        if (env.BRANCH_NAME.startsWith('PR-')) {
          def payload = "Build [${env.BUILD_NUMBER}](${env.BUILD_URL}) **failed**."
          postGithubComment(env.CHANGE_ID, payload)
        }
      }

      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ FAILED ($last_started) 👎")
    }

    success {
      script {
        lastCommits = sh(
          script: "git log -5 --pretty=\"%h [%an] %s\" | sed \"s/^/    /\"",
          returnStdout: true
        )

        if (env.BRANCH_NAME.startsWith('PR-')) {
          def payload = "Build [${BUILD_NUMBER}](${env.BUILD_URL}) **succeeded**.\n\n"

          def flavorList = defineFlavor()
          flavorList.each { flavor ->
            def buildType = defineBuildType(flavor)
            def apks = findFiles(glob: "app/build/outputs/apk/${flavor.toLowerCase()}/${buildType.toLowerCase()}/com.wire.android-*.apk")

            if (apks.size() > 0) {
              payload += "$flavor$buildType produced the following APK's:\n"

              for (a in apks) {
                payload += "- [${a.name}](https://z-lohika.s3-eu-west-1.amazonaws.com/megazord/android/reloaded/${flavor.toLowerCase()}/${buildType.toLowerCase()}/${a.name})\n"
              }
            } else {
              payload += "$flavor$buildType did not produce any APK artifacts.\n"
            }
          }
          postGithubComment(env.CHANGE_ID, payload)
        }
      }

      sh './gradlew jacocoReport'
      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ✅ SUCCESS 🎉"+"\nLast 5 commits:\n```text\n$lastCommits\n```")
    }

    aborted {
      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ ABORTED ($last_started) ")
    }

    always {
      sh 'docker stop ${emulatorPrefix}-${BUILD_NUMBER}_9 ${emulatorPrefix}-${BUILD_NUMBER}_10 || true'
      sh 'docker rm ${emulatorPrefix}-${BUILD_NUMBER}_9 ${emulatorPrefix}-${BUILD_NUMBER}_10 || true'
    }

  }
}
