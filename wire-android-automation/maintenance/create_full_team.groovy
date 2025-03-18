/*properties([
        parameters([
                string(name: 'Branch', defaultValue: 'main', description: 'Select the branch to run from', trim: false),
                choice(name: 'BackendType', choices: ['staging', 'bund-qa-column-1', 'bund-qa-column-2', 'bund-qa-column-3', 'bund-next-column-1', 'bund-next-column-2', 'bund-next-column-3', 'anta', 'bella', 'chala', 'diya', 'elna', 'foma'], description: 'Select either staging or edge backend'),
                string(name: 'TeamSize', defaultValue: '10', description: 'The size of the team (including the owner)', trim: false),
                booleanParam(name: 'MLS', defaultValue: false, description: 'Enable MLS for team'),
                booleanParam(name: 'E2EI', defaultValue: false, description: 'Enable E2EI for team'),
                [$class: 'DynamicReferenceParameter',
                 choiceType: 'ET_FORMATTED_HTML',
                 description: 'Name of Owner',
                 omitValueField: true,
                 name: 'OwnerName',
                 referencedParameters: 'OwnerName',
                 script: [$class: 'GroovyScript',
                          fallbackScript: [
                                  classpath: [],
                                  sandbox: true,
                                  script: 'return "error"'
                          ],
                          script: [
                                  classpath: [],
                                  sandbox: false,
                                  script: """
			return "<input name='value' value='" + (java.util.UUID.randomUUID() as String)[0..7] + "' class='setting-input' type='text'>"
		    """
                          ]
                 ]
                ],
                string(defaultValue: '0', description: 'the number of devices for each generated user NOTE: can not be higher than 7', name: 'NumberOfDevices', trim: false),
                choice(choices: ['http://192.168.2.50:8080'], description: 'The URL to the ETS Rest service', name: 'ETS_SERVICE_URL'),
                booleanParam(name: 'UserPictures', defaultValue: true, description: 'Upload profile pictures for each user'),
                booleanParam(name: 'CreateFullConversation', defaultValue: false, description: 'Create one big conversation with all users (not possible with MLS enabled)'),
                booleanParam(name: 'IgnoreExceptions', defaultValue: false, description: 'Enable this if you want to ignore exceptions thrown by the backend while adding members/devices')
        ])
])*/

node('Job_distributor') {

    env.BackendType = params.Branch
    env.BackendType = params.BackendType
    env.TeamSize = params.TeamSize
    env.CreateFullConversation = params.CreateFullConversation
    env.OwnerName = params.OwnerName
    env.NumberOfDevices = params.NumberOfDevices
    env.ETS_SERVICE_URL = params.ETS_SERVICE_URL
    env.UserPictures = params.UserPictures
    env.IgnoreExceptions = params.IgnoreExceptions

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: ' */${Branch}']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/ios/pom.xml'], [path: 'tests/ios-tablet/pom.xml'], [path: 'tests/android/pom.xml'], [path: 'tests/android-reloaded/pom.xml'], [path: 'tests/android-tablet/pom.xml'], [path: 'tests/desktop/pom.xml'], [path: 'tests/webapp/pom.xml'], [path: 'tests/picklejar/pom.xml'], [path: 'tests/picklejar-engine/'], [path: 'tests/pwa/pom.xml'], [path: 'tests/maintenance'], [path: 'tests/pom.xml']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
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
                [envVar: 'KEYCLOAK_PASSWORD', secretRef: 'op://QA automation/KEYCLOAK_PASSWORD/password'],
                [envVar: 'STRIPE_API_KEY', secretRef: 'op://QA automation/STRIPE_API_KEY/password'],
        ]

        // Use 1Password secrets
        withSecrets(config: config, secrets: secrets) {
            // Use Jenkins credentials
            withCredentials([
                    string(credentialsId: '1PasswordServiceAccountToken', variable: 'OP_SERVICE_ACCOUNT_TOKEN'),
            ]) {
                stage('Run maintenance tool') {
                    ansiColor('xterm') {
                        sh 'cd $WORKSPACE/tests/maintenance && java -jar target/maintenance-cli-jar-with-dependencies.jar createFullTeam ${TeamSize} ${MLS} ${E2EI} ${OwnerName} ${NumberOfDevices} ${UserPictures} ${CreateFullConversation} ${IgnoreExceptions} ${BackendType} ${ETS_SERVICE_URL}'
                    }
                }
            }
        }
    }

    archiveArtifacts 'tests/maintenance/target/*.txt'

}
