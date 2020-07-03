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
        withGradle() {
          sh './gradlew runUnitTests'
        }

      }
    }

    stage('Build') {
      steps {
        withGradle() {
          sh './gradlew compileApp'
        }

      }
    }

    stage('Report') {
      steps {
        wireSend(secret: 'de714d86-181d-402f-bc46-bd1b338da4d0', message: 'Reloaded Build Successful')
      }
    }

  }
}