/*
properties([
        parameters([
                string(defaultValue: '21', description: 'The number of participants waiting to pick up your call (1-40)', name: 'WaitingSize', trim: true),
                booleanParam(defaultValue: false, description: 'Check this if instances should mute themselves', name: 'JoinMuted', trim: true),
                string(defaultValue: '0', description: 'The number of participants with switched on video', name: 'VideoSize', trim: true),
                string(defaultValue: 'ios', description: 'Each chapter got a different set of predefined users to not interfere with other chapter while testing', name: 'Chapter', trim: true),
        ])
])
*/

node('Job_distributor') {

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/ios/pom.xml'], [path: 'tests/ios-tablet/pom.xml'], [path: 'tests/android/pom.xml'], [path: 'tests/android-reloaded/pom.xml'], [path: 'tests/android-tablet/pom.xml'], [path: 'tests/desktop/pom.xml'], [path: 'tests/webapp/pom.xml'], [path: 'tests/picklejar/pom.xml'], [path: 'tests/picklejar-engine/'], [path: 'tests/pwa/pom.xml'], [path: 'tests/maintenance'], [path: 'tests/pom.xml']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
    }

    withMaven(jdk: 'JDK17', maven: 'M3') {

        stage('Start call instances and wait') {
            sh 'cd $WORKSPACE/tests/ && mvn --also-make --projects maintenance clean package -DskipTests=true -DbackendType=staging -DcallingServiceUrl=loadbalanced'

            ansiColor('xterm') {
                sh 'cd $WORKSPACE/tests/maintenance && java -jar target/maintenance-cli-jar-with-dependencies.jar prepareConferenceCall ${WaitingSize} ${JoinMuted} ${VideoSize} ${Chapter}'
            }
        }

        input 'Cleanup?'

        stage('Destroy all used call instances') {
            ansiColor('xterm') {
                sh 'cd $WORKSPACE/tests/maintenance && java -jar target/maintenance-cli-jar-with-dependencies.jar cleanupConferenceCallInstances ${Chapter}'
            }
        }
    }
}