pipeline {
  agent any
  stages {
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

    stage('Wire Notification') {
      steps {
        wireSend(secret: 'de714d86-181d-402f-bc46-bd1b338da4d0', message: 'Wire Reloaded has been build')
      }
    }

  }
}