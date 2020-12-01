pipeline {
  agent {
    docker {
      image 'android-agent:latest'
      args '-u 1000:133 --network build-machine -v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=unix:///var/run/docker.sock'
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
                  echo $NDK_HOME'''
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
                        fi
                    '''
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
      parallel {
        stage('Acceptance Tests') {
          steps {
            script {
              last_started = env.STAGE_NAME
            }

            withGradle() {
              sh './gradlew runAcceptanceTests'
            }

          }
        }

        stage('Publish Unit Report') {
          steps {
            echo 'Publish JUnit report'
            publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "app/build/reports/tests/test${flavor}DebugUnitTest/", reportFiles: 'index.html', reportName: 'Unit Test Report', reportTitles: 'Unit Test')
          }
        }

      }
    }

    stage('Assemble') {
      parallel {
        stage('Assemble') {
          steps {
            script {
              last_started = env.STAGE_NAME
            }

            withGradle() {
              sh './gradlew assembleApp'
            }

          }
        }

        stage('Publish Acceptance Test') {
          steps {
            echo 'Publish Acceptance Test'
            publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: "app/build/reports/androidTests/connected/flavors/${flavor.toUpperCase()}/", reportFiles: 'index.html', reportName: 'Acceptance Test Report', reportTitles: 'Acceptance Test')
          }
        }

      }
    }

    stage('Archive APK') {
      steps {
        archiveArtifacts(artifacts: "app/build/outputs/apk/${flavor.toLowerCase()}/debug/app*.apk", allowEmptyArchive: true, onlyIfSuccessful: true)
      }
    }

  }
  environment {
    propertiesFile = 'local.properties'
    flavor = 'Dev'
    adbPort = '5555'
    emulatorPrefix = "${BRANCH_NAME.replaceAll('/','_')}"
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
      sh 'curl -s https://codecov.io/bash > codecov.sh'
      sh "bash codecov.sh -t ${env.CODECOV_TOKEN}"
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