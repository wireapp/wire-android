properties([
        parameters([
                string(name: 'OwnerName', defaultValue: 'Mr. Quinn Sipes', description: 'Username of team owner', trim: false),
        ])
])

node('Job_distributor') {

        env.OwnerName = params.OwnerName

        stage('Checkout') {
                checkout([$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/ios/pom.xml'], [path: 'tests/ios-tablet/pom.xml'], [path: 'tests/android/pom.xml'], [path: 'tests/android-reloaded/pom.xml'], [path: 'tests/android-tablet/pom.xml'], [path: 'tests/desktop/pom.xml'], [path: 'tests/webapp/pom.xml'], [path: 'tests/picklejar/pom.xml'], [path: 'tests/picklejar-engine/'], [path: 'tests/pwa/pom.xml'], [path: 'tests/maintenance'], [path: 'tests/pom.xml']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
        }

        withMaven(jdk: 'JDK17', maven: 'M3') {

                stage('Build maintenance tool') {
                        sh 'cd $WORKSPACE/tests/ && mvn --also-make --projects maintenance clean package -DskipTests=true -DbackendType=staging'
                }

                stage('Run maintenance tool') {
                        withCredentials([string(credentialsId: "OKTA_API_KEY", variable: 'OKTA_API_KEY')]) {
                                ansiColor('xterm') {
                                        sh 'cd $WORKSPACE/tests/maintenance && java -jar target/maintenance-cli-jar-with-dependencies.jar createFixedSSOTeam ${OwnerName}'
                                }
                        }
                }
        }

        archiveArtifacts 'tests/maintenance/target/*.txt'
}