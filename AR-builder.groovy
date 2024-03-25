/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

String shellQuote(String s) {
    // Quote a string so it's suitable to pass to the shell
    return "'" + s.replaceAll("'", "'\"'\"'") + "'"
}

def postGithubComment(String changeId, String body) {
    def authHeader = shellQuote("Authorization: token ${env.GITHUB_API_TOKEN}")
    def apiUrl = shellQuote("https://api.github.com/repos/wireapp/wire-android/issues/${changeId}/comments")

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

def defineTrackName(String branchName) {
    def overwrite = env.CUSTOM_TRACK

    if (overwrite != null) {
        return overwrite
    } else if (branchName == "main") {
        return 'internal'
    }
    return 'None'
}

pipeline {
    agent {
        docker {
            args '-u ${GUID_AGENT} --network build-machine -v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=unix:///var/run/docker.sock'
            label 'android-reloaded-builder'
            image 'android-reloaded-agent:latest'
        }
    }
    parameters {
        string(name: 'SOURCE_BRANCH', description: 'Branch or PR name to')
        string(name: 'CHANGE_BRANCH', description: 'Change branch name to build only used to checkout the correct branch if  you need the branch name use SOURCE_BRANCH')
        choice(name: 'BUILD_TYPE', choices: ['Compatrelease', 'Debug', 'Release', 'Compat'], description: 'Build Type for the Client')
        choice(name: 'FLAVOR', choices: ['Prod', 'Fdroid', 'Dev', 'Staging', 'Internal', 'Beta'], description: 'Product Flavor to build')
        booleanParam(name: 'UPLOAD_TO_S3', defaultValue: false, description: 'Boolean Flag to define if the build should be uploaded to S3')
        booleanParam(name: 'UPLOAD_TO_PLAYSTORE_ENABLED', defaultValue: false, description: 'Boolean Flag to define if the build should be uploaded to Playstore')
        booleanParam(name: 'RUN_UNIT_TEST', defaultValue: true, description: 'Boolean Flag to define if the unit tests should be run')
        booleanParam(name: 'RUN_ACCEPTANCE_TESTS', defaultValue: true, description: 'Boolean Flag to define if the acceptance tests should be run')
        booleanParam(name: 'RUN_STATIC_CODE_ANALYSIS', defaultValue: true, description: 'Boolean Flag to define if the static code analysis should be run')
        string(name: 'GITHUB_CHANGE_ID', description: 'Change ID of the PR used to post comments to GH')
    }


    environment {
        propertiesFile = 'local.properties'
        adbPort = '5555'
        emulatorPrefix = "${SOURCE_BRANCH.replaceAll('/', '_')}"
        trackName = defineTrackName(params.SOURCE_BRANCH)
        ENABLE_SIGNING = "TRUE"
    }

    stages {
        stage('Precondition Checks') {
            parallel {
                stage('Check SDK/NDK') {
                    steps {
                        script {
                            last_started = env.STAGE_NAME
                            sh '''echo ANDROID_HOME: $ANDROID_HOME \n''' +
                                    '''echo NDK_HOME: $NDK_HOME \n''' +
                                    "echo Flavor: ${params.FLAVOR}\n" +
                                    "echo BuildType: ${params.BUILD_TYPE}\n" +
                                    '''echo AdbPort: $adbPort \n''' +
                                    '''echo EmulatorPrefix: $emulatorPrefix \n''' +
                                    '''echo TrackName: $trackName \n''' +
                                    '''echo ChangeId: $CHANGE_ID \n'''
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

        stage('Fetch submodules') {
            steps {
                sh 'git submodule update --init --recursive'
            }
        }

        stage('Configure workspace and Emulators') {
            parallel {

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
                        expression { params.RUN_ACCEPTANCE_TESTS }
                    }
                    steps {
                        sh '''docker rm ${emulatorPrefix}_9 || true
                  docker run --privileged --network build-machine -d -e DEVICE="Nexus 5" -e DATAPARTITION="2g" --name ${emulatorPrefix}-${BUILD_NUMBER}_9 budtmo/docker-android-x86-9.0'''
                    }
                }

                stage('Spawn Emulator 10.0') {
                    when {
                        expression { params.RUN_ACCEPTANCE_TESTS }
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

        stage('Compile') {
            steps {
                script {
                    last_started = env.STAGE_NAME
                }

                withGradle() {
                    sh './gradlew compileApp'
                }

            }
        }

        stage('Static Code Analysis') {
            when {
                expression { params.RUN_STATIC_CODE_ANALYSIS }
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

        stage('Unit Tests') {
            when {
                expression { params.RUN_UNIT_TEST }
            }
            steps {
                script {
                    last_started = env.STAGE_NAME
                }

                withGradle() {
                    sh './gradlew runUnitTests'
                }
                publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "app/build/reports/tests/test${params.FLAVOR}${params.BUILD_TYPE}UnitTest/", reportFiles: 'index.html', reportName: 'Unit Test Report', reportTitles: 'Unit Test')

                script {
                    commitHash = sh(
                            script: 'git rev-parse HEAD | xargs echo -n',
                            returnStdout: true
                    )
                }
                zip archive: true, defaultExcludes: false, dir: "app/build/reports/tests/test${params.FLAVOR}${params.BUILD_TYPE}UnitTest/", overwrite: true, glob: "", zipFile: "unit-tests-android_${commitHash}.zip"
            }
        }

        stage('Connect Emulators') {
            parallel {
                stage('Emulator 10.0') {
                    when {
                        expression { params.RUN_ACCEPTANCE_TESTS }
                    }
                    steps {
                        sh 'adb connect ${emulatorPrefix}-${BUILD_NUMBER}_10:${adbPort}'
                    }
                }

                stage('Emulator 9.0') {
                    when {
                        expression { params.RUN_ACCEPTANCE_TESTS }
                    }
                    steps {
                        sh 'adb connect ${emulatorPrefix}-${BUILD_NUMBER}_9:${adbPort}'
                    }
                }
            }
        }

        stage('Uninstall App') {
            when {
                expression { params.RUN_ACCEPTANCE_TESTS }
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

        stage('Acceptance Tests') {
            when {
                expression { params.RUN_ACCEPTANCE_TESTS }
            }
            steps {
                script {
                    last_started = env.STAGE_NAME
                }

                withGradle() {
                    sh './gradlew runAcceptanceTests'
                }
                publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "app/build/reports/androidTests/connected/debug/flavors/${params.FLAVOR.toLowerCase()}", reportFiles: 'index.html', reportName: 'Acceptance Test Report', reportTitles: 'Acceptance Test')

                script {
                    commitHash = sh(
                            script: 'git rev-parse HEAD | xargs echo -n',
                            returnStdout: true
                    )
                }
                zip archive: true, defaultExcludes: false, dir: "app/build/reports/androidTests/connected/debug/flavors/${params.FLAVOR.toLowerCase()}", overwrite: true, glob: "", zipFile: "integration-tests-android_${commitHash}.zip"
            }
        }

        stage('Assemble APK') {
            steps {
                script {
                    last_started = env.STAGE_NAME
                    String assembleCommand = "./gradlew assemble${params.FLAVOR}${params.BUILD_TYPE}"
                    withGradle() {
                        sh assembleCommand
                    }
                }
            }
        }

        stage('Bundle AAB') {
            when {
                expression {
                    params.UPLOAD_TO_PLAYSTORE_ENABLED &&
                            ((params.FLAVOR == 'Prod' && params.BUILD_TYPE == 'Compatrelease') ||
                                    (params.FLAVOR == 'Beta' && params.BUILD_TYPE == 'Release') ||
                                    (params.FLAVOR == 'Internal' && params.BUILD_TYPE == 'Compat'))
                }
            }
            steps {
                script {
                    last_started = env.STAGE_NAME
                    String bundleCommand = "./gradlew bundle${params.FLAVOR}${params.BUILD_TYPE}"
                    withGradle() {
                        sh bundleCommand
                    }
                }
            }
        }

        stage('Archive') {
            parallel {
                stage('AAB') {
                    when {
                        expression { params.BUILD_TYPE == 'Release' }
                    }
                    steps {
                        sh "ls -la app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/"
                        archiveArtifacts(artifacts: "app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/com.wire.android-*.aab", allowEmptyArchive: true, onlyIfSuccessful: true)
                    }
                }

                stage('APK') {
                    steps {
                        sh "ls -la app/build/outputs/apk/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/"
                        archiveArtifacts(artifacts: "app/build/outputs/apk/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/com.wire.android-*.apk, app/build/**/mapping/**/*.txt, app/build/**/logs/**/*.txt", allowEmptyArchive: true, onlyIfSuccessful: true)
                    }
                }
            }
        }

        stage("Upload") {
            parallel {
                stage('S3 Bucket') {
                    when {
                        expression { params.UPLOAD_TO_S3 }
                    }
                    steps {
                        echo 'Checking folder before S3 Bucket upload'
                        sh "ls -la app/build/outputs/apk/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/"
                        echo 'Uploading file to S3 Bucket'
                        s3Upload(
                                acl: 'Private',
                                workingDir: "app/build/outputs/apk/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/",
                                includePathPattern: 'com.wire.android-*.apk',
                                bucket: 'z-lohika',
                                path: "megazord/android/reloaded/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/"
                        )
                        script {
                            if (params.SOURCE_BRANCH.startsWith("PR-") || params.SOURCE_BRANCH == "develop" || params.SOURCE_BRANCH == "release/candidate") {
                                s3Upload(
                                        acl: 'Private',
                                        workingDir: "app/build/outputs/apk/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/",
                                        includePathPattern: 'com.wire.android-*.apk',
                                        bucket: 'z-lohika',
                                        path: "megazord/android/reloaded/by-branch/${params.SOURCE_BRANCH}/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/"
                                )
                            }
                        }
                    }
                }
                stage('Upload to Wire Beta') {
                    when {
                        expression {
                            params.UPLOAD_TO_PLAYSTORE_ENABLED &&
                                    params.RUN_ACCEPTANCE_TESTS &&
                                    params.RUN_UNIT_TEST &&
                                    params.RUN_STATIC_CODE_ANALYSIS &&
                                    params.UPLOAD_TO_S3 &&
                                    params.FLAVOR == 'Beta' &&
                                    params.SOURCE_BRANCH == 'main' &&
                                    params.BUILD_TYPE == 'Release' &&
                                    params.CHANGE_ID == null
                        }
                    }
                    steps {
                        script {
                            def trackName = "internal"
                            echo 'Checking folder before playstore upload'
                            sh "ls -la app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/"
                            echo 'Uploading file to Playstore track internal'
                            androidApkUpload(
                                    googleCredentialsId: "${env.GOOGLE_PLAY_CREDS}",
                                    filesPattern: "app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/com.wire.android-*.aab",
                                    trackName: trackName,
                                    rolloutPercentage: '100',
                                    releaseName: "${trackName} Release"
                            )
                        }
                    }
                }

                stage('Upload to Wire Prod') {
                    when {
                        expression {
                            params.UPLOAD_TO_PLAYSTORE_ENABLED &&
                                    params.RUN_ACCEPTANCE_TESTS &&
                                    params.RUN_UNIT_TEST &&
                                    params.RUN_STATIC_CODE_ANALYSIS &&
                                    params.UPLOAD_TO_S3 &&
                                    params.FLAVOR == 'Prod' &&
                                    params.SOURCE_BRANCH == 'prod' &&
                                    params.BUILD_TYPE == 'Compatrelease' &&
                                    params.CHANGE_ID == null
                        }
                    }
                    steps {
                        script {
                            def trackName = env.WIRE_ANDROID_PROD_TRACK_NAME
                            echo 'Checking folder before prod playstore upload'
                            sh "ls -la app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/"
                            echo "Uploading file to prod Playstore track ${trackName}"
                            androidApkUpload(
                                    googleCredentialsId: "${env.GOOGLE_PLAY_CREDS}",
                                    filesPattern: "app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/com.wire.android-*.aab",
                                    trackName: trackName,
                                    rolloutPercentage: '100',
                                    releaseName: "${trackName} Release"
                            )
                        }
                    }
                }

                stage('Upload to Wire Internal') {
                    when {
                        expression {
                            params.UPLOAD_TO_PLAYSTORE_ENABLED &&
                                    params.RUN_ACCEPTANCE_TESTS &&
                                    params.RUN_UNIT_TEST &&
                                    params.RUN_STATIC_CODE_ANALYSIS &&
                                    params.UPLOAD_TO_S3 &&
                                    params.FLAVOR == 'Internal' &&
                                    params.SOURCE_BRANCH == 'internal' &&
                                    params.BUILD_TYPE == 'Compat' &&
                                    params.CHANGE_ID == null
                        }
                    }
                    steps {
                        script {
                            def trackName = env.WIRE_ANDROID_INTERNAL_TRACK_NAME
                            echo 'Checking folder before prod playstore upload'
                            sh "ls -la app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/"
                            echo 'Uploading file to prod Playstore track ${trackName}'
                            androidApkUpload(
                                    googleCredentialsId: "${env.GOOGLE_PLAY_CREDS}",
                                    filesPattern: "app/build/outputs/bundle/${params.FLAVOR.toLowerCase()}${params.BUILD_TYPE.capitalize()}/com.wire.android-*.aab",
                                    trackName: trackName,
                                    rolloutPercentage: '100',
                                    releaseName: "${trackName} Release"
                            )
                        }
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                if (env.SOURCE_BRANCH.startsWith('PR-')) {
                    def payload = "Build [${env.BUILD_NUMBER}](${env.BUILD_URL}) **failed**."
                    postGithubComment(params.GITHUB_CHANGE_ID, payload)
                }
            }

            wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${SOURCE_BRANCH}] - ❌ FAILED ($last_started) 👎")
        }

        success {
            script {
                lastCommits = sh(
                        script: "git log -5 --pretty=\"%h [%an] %s\" | sed \"s/^/    /\"",
                        returnStdout: true
                )

                if (env.SOURCE_BRANCH.startsWith('PR-')) {
                    def payload = "Build [${BUILD_NUMBER}](${env.BUILD_URL}) **succeeded**.\n\n"

                    def apks = findFiles(glob: "app/build/outputs/apk/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/com.wire.android-*.apk")

                    if (apks.size() == 0) {
                        payload += "The build did not produce any APK artifacts."
                    } else {
                        payload += "The build produced the following APK's:\n"

                        for (a in apks) {
                            payload += "- [${a.name}](https://z-lohika.s3-eu-west-1.amazonaws.com/megazord/android/reloaded/${params.FLAVOR.toLowerCase()}/${params.BUILD_TYPE.toLowerCase()}/${a.name})\n"
                        }
                    }

                    postGithubComment(params.GITHUB_CHANGE_ID, payload)
                }
            }

            wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${SOURCE_BRANCH}] - ✅ SUCCESS 🎉" + "\nLast 5 commits:\n```text\n$lastCommits\n```")
        }

        aborted {
            wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${SOURCE_BRANCH}] - ❌ ABORTED ($last_started) ")
        }

        always {
            sh 'docker stop ${emulatorPrefix}-${BUILD_NUMBER}_9 ${emulatorPrefix}-${BUILD_NUMBER}_10 || true'
            sh 'docker rm ${emulatorPrefix}-${BUILD_NUMBER}_9 ${emulatorPrefix}-${BUILD_NUMBER}_10 || true'
        }
    }
}
