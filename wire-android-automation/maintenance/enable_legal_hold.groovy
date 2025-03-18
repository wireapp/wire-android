properties([
        parameters([
                choice(choices: ['staging'], description: 'Select the backend', name: 'BackendType'),
                string(description: 'The id of the team where legal hold should be enabled', name: 'TeamID', trim: true),
                booleanParam(defaultValue: true, description: 'Check this to enable legal hold for the given team instead', name: 'EnableLegalHold')
        ])
])

node('Job_distributor') {

    env.BackendType = params.BackendType
    env.TeamID = params.TeamID
    env.EnableLegalHold = params.EnableLegalHold

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/ios/pom.xml'], [path: 'tests/ios-tablet/pom.xml'], [path: 'tests/android/pom.xml'], [path: 'tests/android-reloaded/pom.xml'], [path: 'tests/android-tablet/pom.xml'], [path: 'tests/desktop/pom.xml'], [path: 'tests/webapp/pom.xml'], [path: 'tests/picklejar/pom.xml'], [path: 'tests/picklejar-engine/'], [path: 'tests/pwa/pom.xml'], [path: 'tests/maintenance'], [path: 'tests/pom.xml']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
    }

    withMaven(jdk: 'JDK17', maven: 'M3') {

        stage('Install requirements') {
            echo("Check 1Password Installation")
            sh "sh $WORKSPACE/tests/common/src/main/resources/install1PasswordCLIOnNode.sh"
        }

        stage('Build maintenance tool') {
            sh 'cd $WORKSPACE/tests/ && mvn --also-make --projects maintenance clean package -DskipTests=true -DbackendType=${BackendType}'
        }

        def config = [
                serviceAccountCredentialId: '1PasswordServiceAccountToken',
                opCLIPath                 : "/usr/bin/"
        ]
        def secrets = [
                [envVar: 'LH_SERVICE_AUTH_TOKEN', secretRef: 'op://QA automation/LH_SERVICE_AUTH_TOKEN/password'],
        ]

        // Use 1Password secrets
        withSecrets(config: config, secrets: secrets) {
            // Use Jenkins credentials
            withCredentials([
                    string(credentialsId: '1PasswordServiceAccountToken', variable: 'OP_SERVICE_ACCOUNT_TOKEN'),
            ]) {

                stage('Run maintenance tool') {
                    ansiColor('xterm') {
                        sh 'cd $WORKSPACE/tests/maintenance && java -jar target/maintenance-cli-jar-with-dependencies.jar enableLegalHold ${BackendType} ${TeamID} ${EnableLegalHold}'
                    }
                }
            }
        }
    }
}