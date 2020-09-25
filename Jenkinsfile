pipeline {
  agent {
    dockerfile {
      filename 'docker-agent/AndroidAgent'
      args '-u 1000:133 --network docker-compose-files_build-machine -v /var/run/docker.sock:/var/run/docker.sock -e DOCKER_HOST=unix:///var/run/docker.sock'
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

        stage('Connect Android Emulators') {
          steps {
            sh '''for i in $(docker inspect -f \'{{.Name}} - {{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}\' $(docker ps -aq) |grep \'docker-compose-files_nexus\' |grep -Eo \'1[0-9]{2}.*\')
do
        echo  "found emulator with ip $i:${ADB_PORT}"
        adb connect $i:${ADB_PORT}
done
'''
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

    stage('Run Detekt') {
      steps {
        script {
          last_started = env.STAGE_NAME
        }

        withGradle() {
          sh './gradlew detektAll'
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

    stage('Acceptance Tests') {
      parallel {
        stage('Acceptance Tests') {
          when {
            expression {
              params.AcceptanceTests
            }

          }
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
            publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'app/build/reports/tests/testDevDebugUnitTest/', reportFiles: 'index.html', reportName: 'Unit Test Report', reportTitles: 'Unit Test')
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
            publishHTML(allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: 'app/build/reports/androidTest/connected/flavours/DEV/', reportFiles: 'index.html', reportName: 'Acceptance Test Report', reportTitles: 'Acceptance Test')
          }
        }

      }
    }

    stage('Archive APK') {
      steps {
        archiveArtifacts(artifacts: 'app/build/outputs/apk/dev/debug/app*.apk', allowEmptyArchive: true, onlyIfSuccessful: true)
      }
    }

    stage('Report to CodCov') {
      when {
        expression {
          allOf {
            environment name: 'CHANGE_ID', value: ''
            branch 'master'
          }
        }

      }
      steps {
        script {
          last_started = env.STAGE_NAME
        }

        sh './gradlew jacocoReport'
        sh 'curl -s https://codecov.io/bash > codecov.sh'
        sh "bash codecov.sh -t ${env.CODECOV_TOKEN}"
      }
    }

  }
  environment {
    propertiesFile = 'local.properties'
    ADB_PORT = '5555'
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

      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ✅ SUCCESS 🎉"+"\nLast 5 commits:\n```\n$lastCommits\n```")
    }

    aborted {
      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ ABORTED ($last_started) ")
    }

  }
}