def defineFlavour() {
    //check if the pipeline has the custom flavour env variable set
    echo "define Flavour"
    def overwrite = "${env.CUSTOM_FLAVOUR}"
    echo "overwrite is set to [${overwrite}]"
    if(overwrite != "") {
        return overwrite
    }

    echo "checking branchname. Branchname is set to [${env.BRANCH_NAME}]"
    def branchName = "${env.BRANCH_NAME}"
    if (branchName == "main") {
        return 'Internal'
    } else if(branchName == "develop") {
        return 'Dev'
    } else if(branchName == "release") {
        return 'Public'
    }
    echo "returning Dev"
    return 'Dev'
}

def defineBuildType() {
    echo "checking build type: [${env.CUSTOM_BUILD_TYPE}]"
    def overwrite = "${env.CUSTOM_BUILD_TYPE}"
    if(overwrite != "") {
        return overwrite
    }
    echo "returning Release"
    return "Release"
}

def defineTrackName() {
    echo "checking trackname. Branch name is [${env.BRANCH_NAME}]"
    def branchName = "${env.BRANCH_NAME}"
    if (branchName == "main") {
        return 'internal-testing'
    } else if(branchName == "develop") {
        return 'Alpha'
    } else if(branchName == "release") {
        return 'production'
    }
    echo "Returning Alpha"
    return 'Alpha'
}

pipeline {
  agent {
    docker {
      args '-u 1000:133 --network build-machine -v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=unix:///var/run/docker.sock'
      label 'android-reloaded-builder'
      image 'android-reloaded-agent:latest'
    }

  }
  stages {
    stage('Precondition Checks') {
      parallel {
        stage('Check SDK/NDK') {
          steps {
            script {
              last_started = env.STAGE_NAME
            }

            sh '''echo $ANDROID_HOME
                  echo $NDK_HOME
                  echo $flavor
                  echo $buildType
                  echo $adbPort
                  echo $emulatorPrefix
                  echo $trackName
               '''
          }
        }

        stage('Create properties file') {
          steps {
            sh '''FILE=/${propertiesFile}
                        if test -f "$FILE"; then
                            echo "${propertiesFile} exists already"
                        else
                            echo "sdk.dir="$ANDROID_HOME >> ${propertiesFile}
                            echo "ndk.dir="$NDK_HOME >> ${propertiesFile}
                            echo "nexus.url=http://10.10.124.134:8081/repository/public/" >> ${propertiesFile}
                        fi
                    '''
          }
        }

        stage('Fetch Signing Files') {
          steps {
            configFileProvider([
                                                                                                                                                                                                                                                                                                                                                                                                          configFile(fileId: '00246e05-bb93-45f5-b1e6-0ff2d4ff9453', targetLocation: 'app/reloaded-debug-key.keystore.asc'),
                                                                                                                                                                                                                                                                                                                                                                                                          configFile(fileId: '97ce3674-1ed5-42a0-9185-00a93896b364', targetLocation: 'app/reloaded-release-key.keystore.asc'),
                                                                                                                                                                                                                                                                                                                                                                                                          configFile(fileId: '10414dfa-5450-4c18-84fb-970fc9c6ae90', variable: 'GROOVY_FILE_THAT_SETS_VARIABLES')
                                                                                                                                                                                                                                                                                                                                                                                                      ]) {
                sh '''
                    base64 --decode app/reloaded-debug-key.keystore.asc > app/reloaded-debug-key.keystore
                    base64 --decode app/reloaded-release-key.keystore.asc > app/reloaded-release-key.keystore
                '''
                load env.GROOVY_FILE_THAT_SETS_VARIABLES
              }

            }
          }

        }
      }

      stage('Spawn Wrapper and Emulators') {
        parallel {
          stage('Spawn Gradle Wrapper') {
            steps {
              withGradle() {
                sh './gradlew -Porg.gradle.jvmargs=-Xmx16g wrapper'
              }

            }
          }

          stage('Spawn Emulator 9.0') {
            steps {
              sh '''docker rm ${emulatorPrefix}_9 || true
docker run --privileged --network build-machine -d -e DEVICE="Nexus 5" --name ${emulatorPrefix}-${BUILD_NUMBER}_9 budtmo/docker-android-x86-9.0'''
            }
          }

          stage('Spawn Emulator 10.0') {
            steps {
              sh '''docker rm ${emulatorPrefix}_10 || true
docker run --privileged --network build-machine -d -e DEVICE="Nexus 5" --name ${emulatorPrefix}-${BUILD_NUMBER}_10 budtmo/docker-android-x86-10.0'''
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
        steps {
          script {
            last_started = env.STAGE_NAME
          }

          withGradle() {
            sh './gradlew runUnitTests'
          }

          publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "app/build/reports/tests/test${flavor}${buildType}UnitTest/", reportFiles: 'index.html', reportName: 'Unit Test Report', reportTitles: 'Unit Test')
        }
      }

      stage('Connect Emulators') {
        parallel {
          stage('Emulator 10.0') {
            steps {
              sh 'adb connect ${emulatorPrefix}-${BUILD_NUMBER}_10:${adbPort}'
            }
          }

          stage('Emulator 9.0') {
            steps {
              sh 'adb connect ${emulatorPrefix}-${BUILD_NUMBER}_9:${adbPort}'
            }
          }

        }
      }

      stage('Prepare Emulators') {
        parallel {
          stage('Uninstall App') {
            steps {
              script {
                last_started = env.STAGE_NAME
              }

              withGradle() {
                sh './gradlew :app:uninstallAll'
              }

            }
          }

        }
      }

      stage('Acceptance Tests') {
        steps {
          script {
            last_started = env.STAGE_NAME
          }

          withGradle() {
            sh './gradlew runAcceptanceTests'
          }

          publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "app/build/reports/androidTests/connected/flavors/${flavor.toUpperCase()}/", reportFiles: 'index.html', reportName: 'Acceptance Test Report', reportTitles: 'Acceptance Test')
        }
      }


      stage('Assemble APK') {
        steps {
          script {
            last_started = env.STAGE_NAME
          }

          withGradle() {
            sh './gradlew assemble${flavour}${buildType}'
          }

        }
      }

      stage('Bundle AAB') {
        steps {
          script {
            last_started = env.STAGE_NAME
          }

          withGradle() {
            sh './gradlew :app:bundle${flavour}${buildType}'
          }
        }
      }

      stage("Upload") {
        parallel {
            stage('S3 Bucket') {
              steps {
                s3Upload(acl: 'Private', file: "app/build/outputs/apk/com.wire.android-*.aa", bucket: 'z-lohika', path: "megazord/android/reloaded/${params.Flavor.toLowerCase()}/${params.BuildType.toLowerCase()}/")
              }
            }
            stage('Playstore') {
              steps {
                androidApkUpload(apkFilesPattern: 'app/build/outputs/bundle/${flavor.toLowerCase()}${buildType.capitalize()}/bcom.wire.android-*.aa', trackName: '${trackName}', rolloutPercent: 100, rolloutPercentage: '100', releaseName: '${trackName} Release')
              }
            }
        }
      }


      stage('Archive') {
        parallel {
          stage('AAB') {
            steps {
              archiveArtifacts(artifacts: "app/build/outputs/bundle/${flavor.toLowerCase()}${buildType.capitalize()}/com.wire.android-*.aab", allowEmptyArchive: true, onlyIfSuccessful: true)
            }
          }

          stage('APK') {
            steps {
              archiveArtifacts(allowEmptyArchive: true, artifacts: 'app/build/outputs/apk/${flavor.toLowerCase()}/${buildType.toLowerCase()}/com.wire.android-*.apk, app/build/**/mapping/**/*.txt, app/build/**/logs/**/*.txt')
            }
          }

        }
      }
    }
    environment {
      propertiesFile = 'local.properties'
      flavor = 'Dev'
      buildType = 'Release'
      adbPort = '5555'
      emulatorPrefix = "${BRANCH_NAME.replaceAll('/','_')}"
      trackName = defineTrackName()
    }
    post {
      failure {
        wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ FAILED ($last_started) 👎")
      }

      success {
        script {
          lastCommits = sh(
            script: "git log -5 --pretty=\"%h [%an] %s\" | sed \"s/^/    /\"",
            returnStdout: true
          )
        }

        sh './gradlew jacocoReport'
        wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ✅ SUCCESS 🎉"+"\nLast 5 commits:\n```\n$lastCommits\n```")
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