#!/usr/bin/env groovy

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

        stage('Create local.properties') {
          steps {
            sh '''FILE=/local.properties
                        if test -f "$FILE"; then
                            echo "local.properties exists already"
                        else
                            echo "sdk.dir="$ANDROID_HOME >> local.properties
                            echo "ndk.dir="$NDK_HOME >> local.properties
                        fi
                    '''
          }
        }

        stage('ls') {
          steps {
            sh '''ls -la
                        cd app
                        ls -la'''
          }
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

    stage('Build') {
      steps {
        script {
          last_started = env.STAGE_NAME
        }

        withGradle() {
          sh './gradlew compileApp'
        }

      }
    }

  }
  post {
      failure {
        wireSend(secret: env.WIRE_BOT_SECRET, message: "[${BRANCH_NAME}]**[${BUILD_NUMBER}](${BUILD_URL})** - ❌ FAILED ($last_started) 👎")
      }

      success {
        script {
          lastCommits = sh(
            script: "git log -5 --pretty=\"%h [%an] %s\" | sed \"s/^/    /\"",
            returnStdout: true
          )
        }

        wireSend(secret: env.WIRE_BOT_SECRET, message: "${BRANCH_NAME}]**[${BUILD_NUMBER}](${BUILD_URL})** - ✅ SUCCESS 🎉"+"\nLast 5 commits:\n```\n$lastCommits\n```")
      }

      aborted {
        wireSend(secret: env.WIRE_BOT_SECRET, message: "${BRANCH_NAME}]**[${BUILD_NUMBER}](${BUILD_URL})** - ❌ ABORTED ($last_started) ")
      }
    }
}