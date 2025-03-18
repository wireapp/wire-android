properties([
        parameters([
                choice(choices: ['staging'], description: 'Select the backend', name: 'BackendType'),
        ])
])

node('Job_distributor') {

    env.BackendType = params.BackendType

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/ios/pom.xml'], [path: 'tests/ios-tablet/pom.xml'], [path: 'tests/android/pom.xml'], [path: 'tests/android-reloaded/pom.xml'], [path: 'tests/android-tablet/pom.xml'], [path: 'tests/desktop/pom.xml'], [path: 'tests/webapp/pom.xml'], [path: 'tests/picklejar/pom.xml'], [path: 'tests/picklejar-engine/'], [path: 'tests/pwa/pom.xml'], [path: 'tests/digital-signature-sms-otp-aws/pom.xml'], [path: 'tests/maintenance'], [path: 'tests/pom.xml']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
    }

    withMaven(jdk: 'JDK17', maven: 'M3') {

        stage('Install requirements') {
            echo("Check 1Password Installation")
            sh "sh $WORKSPACE/tests/common/src/main/resources/install1PasswordCLIOnNode.sh"
        }

        stage('Build maintenance tool') {
            sh 'cd $WORKSPACE/tests/ && mvn --also-make --projects maintenance clean package -DskipTests=true -DbackendType=${BackendType}'
        }

        stage('Run maintenance tool') {
            echo("Configure 1Password integration")
            def config = [
                    serviceAccountCredentialId: '1PasswordServiceAccountToken',
                    opCLIPath                 : "/usr/bin/"
            ]
            def secrets = [
                    [envVar: 'OKTA_API_KEY', secretRef: 'op://QA automation/OKTA_API_KEY/password'],
                    [envVar: 'STRIPE_API_KEY', secretRef: 'op://QA automation/STRIPE_API_KEY/password'],
            ]

            // Use 1Password secrets
            withSecrets(config: config, secrets: secrets) {
                // Use Jenkins credentials
                withCredentials([
                        string(credentialsId: '1PasswordServiceAccountToken', variable: 'OP_SERVICE_ACCOUNT_TOKEN'),
                ]) {
                    ansiColor('xterm') {
                        sh 'cd $WORKSPACE/tests/maintenance && java -jar target/maintenance-cli-jar-with-dependencies.jar createSSOTeam ${BackendType}'
                    }
                }
            }
        }
    }

    archiveArtifacts 'tests/maintenance/target/*.txt'
}
