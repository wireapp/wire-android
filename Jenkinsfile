#!/usr/bin/env groovy

pipeline {
  agent {
    dockerfile {
      filename 'docker-agent/AndroidAgent'
    }

  }

  environment {
    propertiesFile='local.properties'
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

        stage('Create ${env.propertiesFile}') {
          steps {
            sh '''FILE=/${env.propertiesFile}
                        if test -f "$FILE"; then
                            echo "${env.propertiesFile} exists already"
                        else
                            echo "sdk.dir="$ANDROID_HOME >> ${env.propertiesFile}
                            echo "ndk.dir="$NDK_HOME >> ${env.propertiesFile}
                        fi
                    '''
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

        wireSend(secret: env.WIRE_BOT_SECRET, message: "[${BRANCH_NAME}]**[${BUILD_NUMBER}](${BUILD_URL})** - ✅ SUCCESS 🎉"+"\nLast 5 commits:\n```\n$lastCommits\n```")
      }

      aborted {
        wireSend(secret: env.WIRE_BOT_SECRET, message: "[${BRANCH_NAME}]**[${BUILD_NUMBER}](${BUILD_URL})** - ❌ ABORTED ($last_started) ")
      }
    }
}
