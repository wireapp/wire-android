pipeline {
  agent {
    dockerfile {
      filename 'docker-agent/AndroidAgent'
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

    stage('Archive') {
      steps {
        archiveArtifacts(artifacts: 'app/build/outputs/apk/dev/debug/app*.apk', allowEmptyArchive: true, onlyIfSuccessful: true)
      }
    }

  }
  environment {
    propertiesFile = 'local.properties'
  }
  post {
    failure {
      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME} - ${GIT_COMMITTER_NAME}] - ❌ FAILED ($last_started) 👎")
    }

    success {
      script {
        lastCommits = sh(
          script: "git log -5 --pretty=\"%h [%an] %s\" | sed \"s/^/    /\"",
          returnStdout: true
        )
      }

      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME} - ${GIT_COMMITTER_NAME}] - ✅ SUCCESS 🎉"+"\nLast 5 commits:\n```\n$lastCommits\n```")
    }

    aborted {
      wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME} - ${GIT_COMMITTER_NAME}] - ❌ ABORTED ($last_started) ")
    }

  }
}