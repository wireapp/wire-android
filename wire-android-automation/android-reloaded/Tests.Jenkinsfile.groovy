/*
TAGS as String parameter
Flavor as Choice parameter: dev, internal, candidate, experimental, production, fdroid
BuildType as Choice parameter: release, debug
AppBuildNumber as String parameter
Branch as String parameter main
Platform as Choice parameter
backendType as Choice parameter master
TESTINY_RUN_NAME as String parameter
OldBuildNumber as String parameter
browserName as String parameter
surefire.rerunFailingTestsCount as String parameter 1
test as String parameter
isCountlyAvailable as String parameter
 */
import hudson.tasks.test.AbstractTestResultAction

@NonCPS
def testStatuses() {
    AbstractTestResultAction testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    if (testResultAction != null) {
        def total = testResultAction.totalCount
        def failed = testResultAction.failCount
        def skipped = testResultAction.skipCount
        def passed = total - failed - skipped
        int percent = (total > 0) ? (passed * 100) / (total - skipped) : 0
        testStatus = "Tests passed: ${passed}, Failed: ${failed} ${testResultAction.failureDiffString}, Skipped: ${skipped}, Total: ${total} (${percent}%)"
    } else {
        testStatus = "Could not find test results!"
    }
    return testStatus
}

@NonCPS
def isTestSuccessful() {
    AbstractTestResultAction testResultAction = currentBuild.rawBuild.getAction(AbstractTestResultAction.class)
    if (testResultAction != null && testResultAction.failCount > 0) {
        return false
    }
    return true
}

node("Job_distributor") {

    // Select grid
    if ("${GRID}" == "Android-SQCALL-phones-node040") {
        nodeLabel = "${GRID}"
        hubUrl = "http://192.168.2.40:4444/wd/hub"
        tablet = false
        label = "android"
        platformVersion = "60"
    } else if ("${GRID}" == "Android-phones-node140") {
        nodeLabel = "${GRID}"
        hubUrl = "http://192.168.2.140:4444/wd/hub"
        tablet = false
        label = "android"
        platformVersion = "60"
    } else if ("${GRID}" == "Android-tablets-node080") {
        nodeLabel = "${GRID}"
        hubUrl = "http://192.168.2.80:4447/wd/hub"
        tablet = true
        label = "android_tablet"
        platformVersion = "60"
    }

    if (params.Flavor == "staging compat") {
        S3_FOLDER = "artifacts/megazord/android/reloaded/staging/compat/"
    } else if (params.Flavor == "internal beta") {
        S3_FOLDER = "artifacts/megazord/android/reloaded/beta/release/"
    } else if (params.Flavor == "internal release candidate") {
        S3_FOLDER = "artifacts/megazord/android/reloaded/internal/compat/"
    } else if (params.Flavor == "experimental") {
        // ToDo: This will not work. PR builds are in separate folder under this directory currently. Fix as soon as possible.
        S3_FOLDER = "artifacts/megazord/android/reloaded/staging/compat/"
    } else if (params.Flavor == "column-1") {
        S3_FOLDER = "android/custom/bund/column1/prod/compatrelease/"
    } else if (params.Flavor == "column-2") {
        S3_FOLDER = "android/custom/bund/column2/prod/compatrelease/"
    } else if (params.Flavor == "column-3") {
        S3_FOLDER = "android/custom/bund/column3/prod/compatrelease/"
    } else if (params.Flavor == "debug") {
        S3_FOLDER = "artifacts/megazord/android/reloaded/dev/debug/"
    } else if (params.Flavor == "fdroid") {
        S3_FOLDER = "artifacts/megazord/android/reloaded/fdroid/compatrelease/"
    } else if (params.Flavor == "production") {
        S3_FOLDER = "artifacts/megazord/android/reloaded/prod/compatrelease/"
    }

    if (params.Flavor == "staging compat") {
        env.APP_ID = "com.waz.zclient.dev"
    } else if (params.Flavor == "internal beta") {
        env.APP_ID = "com.wire.android.internal"
    } else if (params.Flavor == "internal release candidate") {
        env.APP_ID = "com.wire.internal"
    } else if (params.Flavor == "experimental") {
        env.APP_ID = "com.waz.zclient.dev"
    } else if (params.Flavor == "column-1") {
        env.APP_ID = "com.wire.android.bund"
    } else if (params.Flavor == "column-2") {
            env.APP_ID = "com.wire.android.bund.column2"
    } else if (params.Flavor == "column-3") {
        env.APP_ID = "com.wire.android.bund.column3"
    } else if (params.Flavor == "debug") {
        env.APP_ID = "com.waz.zclient.dev.debug"
    } else if (params.Flavor == "fdroid") {
        env.APP_ID = "com.wire"
    } else if (params.Flavor == "production") {
        env.APP_ID = "com.wire"
    } else {
        error("Unknown flavour: ${params.Flavor}")
    }

    if (params.BuildType == null && params.BuildType == "") {
        error("Missing buildType parameter")
    }

    def aborted = false
    def TAGS = "${params.TAGS}"

    // Keep build forever when RC is made
    if (params.TESTINY_RUN_NAME != "") {
        currentBuild.description = params.TESTINY_RUN_NAME
        currentBuild.keepLog = true
    }

    // Select bot credentials for specific Wire conversation based on job name
    if ("${JOB_NAME}" =~ /_BUND_/) {
        credentialsId = "JENKINSBOT_BUND"
    } else if ("${JOB_NAME}" =~ /_regression_/) {
        credentialsId = "JENKINSBOT_ANDROID_REGRESSION"
    } else if ("${JOB_NAME}" =~ /_smoke/) {
        credentialsId = "JENKINSBOT_ANDROID_SMOKE"
    } else if ("${JOB_NAME}" =~ /_critical_flows/) {
        credentialsId = "JENKINSBOT_ANDROID_CRITICAL_FLOWS"
    } else {
        credentialsId = "JENKINSBOT_ANDROID_QA"
    }

    echo("Configure 1Password integration")
    def config = [
            serviceAccountCredentialId: '1PasswordServiceAccountToken',
            opCLIPath: "/usr/bin/"
    ]
    def secrets = [
            [envVar: 'OKTA_API_KEY', secretRef: 'op://Test Automation/OKTA_API_KEY/password'],
            [envVar: 'KEYCLOAK_PASSWORD', secretRef: 'op://Test Automation/KEYCLOAK_PASSWORD/password'],
            [envVar: 'LH_SERVICE_AUTH_TOKEN', secretRef: 'op://Test Automation/LH_SERVICE_AUTH_TOKEN/password'],
            [envVar: 'STRIPE_API_KEY', secretRef: 'op://Test Automation/STRIPE_API_KEY/password'],
            [envVar: 'SOCKS_PROXY_PASSWORD', secretRef: 'op://Test Automation/SOCKS_PROXY_PASSWORD/password'],
            [envVar: 'MS_EMAIL', secretRef: 'op://Test Automation/MS_CREDENTIALS/username'],
            [envVar: 'MS_PASSWORD', secretRef: 'op://Test Automation/MS_CREDENTIALS/password'],
            [envVar: 'BLACKLIST_S3_SECRET', secretRef: 'op://Test Automation/BLACKLIST_S3_SECRET/password'],
            [envVar: 'TESTINY_API_KEY', secretRef: 'op://Test Automation/TESTINY_API_KEY_ANDROID/password'],
            [envVar: 'CALLINGSERVICE_BASIC_AUTH', secretRef: 'op://Test Automation/CALLINGSERVICE_BASIC_AUTH/password'],
    ]

    // Use 1Password secrets
    withSecrets(config: config, secrets: secrets) {
        // Use Jenkins credentials
        withCredentials([
                string(credentialsId: '1PasswordServiceAccountToken', variable: 'OP_SERVICE_ACCOUNT_TOKEN'),
                file(credentialsId: 'KUBECONFIG_anta', variable: 'KUBECONFIG_anta'),
                file(credentialsId: 'KUBECONFIG_bella', variable: 'KUBECONFIG_bella'),
                file(credentialsId: 'KUBECONFIG_chala', variable: 'KUBECONFIG_chala'),
                file(credentialsId: 'KUBECONFIG_foma', variable: 'KUBECONFIG_foma'),
                file(credentialsId: 'KUBECONFIG_gudja_offline_android', variable: 'KUBECONFIG_gudja_offline_android'),
                file(credentialsId: 'KUBECONFIG_bund_next_column_1', variable: 'KUBECONFIG_bund_next_column_1'),
                file(credentialsId: 'KUBECONFIG_bund_next_column_offline_android', variable: 'KUBECONFIG_bund_next_column_offline_android'),
                file(credentialsId: 'KUBECONFIG_bund_qa_column_offline_android', variable: 'KUBECONFIG_bund_qa_column_offline_android'),
                file(credentialsId: 'KUBECONFIG_bund_qa_column_1', variable: 'KUBECONFIG_bund_qa_column_1'),
                string(credentialsId: "${credentialsId}", variable: 'JENKINSBOT_SECRET'),
        ]) {

            // Checkout
            stage('Checkout & Clean') {
                if (tablet) {
                    checkout([$class: 'GitSCM', branches: [[name: '*/${Branch}']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'zautomation'], [$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/android'], [path: 'tests/android-tablet'], [path: 'tests/pom.xml'], [path: 'tests/tools']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
                } else {
                    checkout([$class: 'GitSCM', branches: [[name: '*/${Branch}']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'zautomation'], [$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'tests/common'], [path: 'tests/android-reloaded'], [path: 'tests/pom.xml'], [path: 'tests/tools']]], [$class: 'CheckoutOption', timeout: 30], [$class: 'CloneOption', depth: 0, noTags: true, reference: '', shallow: true, timeout: 30], [$class: 'BuildChooserSetting', buildChooser: [$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]])
                }

                echo("Installing kubectl if not already installed")
                kubeCtlSetup = readFile("${WORKSPACE}/zautomation/tests/common/kubectlSetup.sh")
                sh kubeCtlSetup
                if (params.TESTINY_RUN_NAME != "") {
                    if (params.test == "") {
                        echo("Tag git branch with Testiny run name")
                        def tagname = params.TESTINY_RUN_NAME.replaceAll("\\s", "").replaceAll("\\(", "_").replaceAll("\\)", "_")
                        sshagent(credentials: ['zautomation-writeable']) {
                            sh returnStatus: true, script: "cd zautomation && git tag Android-${tagname}"
                            sh returnStatus: true, script: "cd zautomation && git push origin Android-${tagname}"
                        }
                    }
                }
            }

            stage('Download build from S3') {

                OldS3AppPath = ""
                S3AppPath = ""

                // Get list of files on S3
                def files = []
                withAWS(region: 'eu-west-1', credentials: "S3_CREDENTIALS") {
                    files = s3FindFiles bucket: "z-lohika", path: S3_FOLDER, onlyFiles: true, glob: '*.apk'
                }

                // Get latest version via sorting by build number

                def buildNumbers = []
                files.each {
                    if (params.Flavor == "fdroid") {
                        print("Downloading Fdroid Build...")
                        def match = (it =~ /-v([0-9]+\.[0-9]+\.[0-9]+)-fdroid-compatrelease\.apk/)
                        buildNumbers.add(match[0][1][2]);
                    } else {
                        def match = (it =~ /[-\(]{1}[0-9]+[-\)]{1}/)
                        buildNumbers.add(match[0].replace("(", "").replace(")", "").replace("-", "").toInteger())
                    }
                }
                buildNumbers.sort()

                if ("${AppBuildNumber}" == "latest") {
                    env.REAL_BUILD_NUMBER = buildNumbers[buildNumbers.size() - 1]
                    if ("${OldBuildNumber}" == "") {
                        // Get Older version via sorting by build number
                        print("Old build number is not specified.")
                        env.OLD_BUILD_NUMBER = buildNumbers[buildNumbers.size() - 2]
                    } else if ("${OldBuildNumber}" != "") {
                        env.OLD_BUILD_NUMBER = "${OldBuildNumber}"
                    }
                } else if ("${AppBuildNumber}" != "latest") {
                    env.REAL_BUILD_NUMBER = "${AppBuildNumber}"
                    if ("${OldBuildNumber}" == "") {
                        // ToDo: Fix method to grab one build underneath the provided one.
                        print("Old build number is not specified. Please provide the build number you want to test with.")
                        env.OLD_BUILD_NUMBER = "${AppBuildNumber}"
                        print env.OLD_BUILD_NUMBER
                    } else if ("${OldBuildNumber}" != "") {
                        env.OLD_BUILD_NUMBER = "${OldBuildNumber}"
                    }
                }

                // Download apk directly from s3 if AppBuildNumber contains what looks like a s3 path
                if ("${AppBuildNumber}" ==~ /.*\.apk$/) {
                    echo("Download apk directly from ${AppBuildNumber}...")
                    S3AppPath = "${AppBuildNumber}"
                    apkNameOnS3 = S3AppPath - ~/^.*\//
                    echo("${apkNameOnS3}")

                    files.each {
                        if (it.name.contains("${OLD_BUILD_NUMBER}")) {
                            OldS3AppPath = S3_FOLDER + it
                            return true
                        }
                    }
                } else {
                    files.each {
                        if (it.name.contains("${REAL_BUILD_NUMBER}")) {
                            S3AppPath = S3_FOLDER + it
                            apkNameOnS3 = it
                        }
                        if (it.name.contains("${OLD_BUILD_NUMBER}")) {
                            OldS3AppPath = S3_FOLDER + it
                        }
                        if (S3AppPath != "" && OldS3AppPath != "") {
                            //both builds are found, exit loop
                            return true
                        }
                    }
                }

                // Stash testing gallery on job distributor
                stash name: "testing-gallery", includes: "zautomation/tests/tools/android/*.apk"

                // Go to grid machine
                nodes = nodesByLabel(nodeLabel)
                nodes.each {
                    node(it) {
                        env.APP_PATH = "$WORKSPACE/Wire.apk"
                        env.OLD_APP_PATH = "$WORKSPACE/Wire.old.apk"

                        // Download builds to machine that contains the grid
                        withAWS(region: 'eu-west-1', credentials: "S3_CREDENTIALS") {
                            try {
                                s3Download(file: "$APP_PATH", bucket: 'z-lohika', path: "${S3AppPath}", force: true)

                                if (OldS3AppPath != "") {
                                    // Copy original apk as orig.apk to workaround issue that appium cache is used on reinstall
                                    sh "cp -f ${APP_PATH} ${APP_PATH}.orig.apk"

                                    // Example for OldBuildNumber = wire-candidate-release-3.53.3337.apk,wire-candidate-release-3.54.3343.apk
                                    s3Download(file: env.OLD_APP_PATH, bucket: 'z-lohika', path: "${OldS3AppPath}", force: true)
                                }
                            } catch (e) {
                                sleep 10
                                // When multiple atomic jobs download at the same time, we could hit SocketTimeoutException
                                s3Download(file: "$APP_PATH", bucket: 'z-lohika', path: "${S3AppPath}", force: true)

                                if (OldS3AppPath != "") {
                                    // Copy original apk as orig.apk to workaround issue that appium cache is used on reinstall
                                    sh "cp -f ${APP_PATH} ${APP_PATH}.orig.apk"
                                    s3Download(file: env.OLD_APP_PATH, bucket: 'z-lohika', path: "${OldS3AppPath}", force: true)
                                }
                            }
                        }

                        // Unstash testing gallery on the grid machine
                        unstash name: "testing-gallery"
                        testingGalleryPath = "$WORKSPACE/zautomation/tests/tools/android/"

                        // Check how many phones are in the grid currently
                        if (tablet) {
                            env.PHONESONGRID = sh returnStdout: true, script: '/usr/local/bin/adb devices | grep "device$" | cut -f1 | xargs -I {}  /usr/local/bin/adb -s {} shell getprop ro.build.characteristics | grep tablet | wc -l | tr -d " "'
                        } else {
                            env.PHONESONGRID = sh returnStdout: true, script: '/usr/local/bin/adb devices | grep "device$" | cut -f1 | xargs -I {}  /usr/local/bin/adb -s {} shell getprop ro.build.characteristics | grep -v tablet | wc -l | tr -d " "'
                        }

                        // Delete all previous versions of wire on all devices
                        sh returnStatus: true, script: '''
                        DEVICES=`/usr/local/bin/adb devices | grep "device$" | cut -f1`
                        PACKAGES='com.wire.internal com.wire com.waz.zclient.dev com.wire.android.dev com.wire.android.internal com.wire.android.bund com.wire.android.bund.column2 com.wire.android.bund.column3 com.waz.zclient.dev.debug'

                        for DEVICE in $DEVICES; do
                            for PACKAGE in $PACKAGES; do
                                /usr/local/bin/adb -s $DEVICE uninstall $PACKAGE || true
                            done
                        done

                        echo "All packages deleted"
                    '''
                    }
                }
            }

            echo("Setting the build description")

            def tags = TAGS.split(',')
            if (tags.size() > 4) {
                tags = tags[0] + "," + tags[1] + ", ..."
            }
            if (tags[0] != "" && tags.size() <= 4) {
                tags = TAGS.trim()
            }

            def description = tags + "\n" + apkNameOnS3 + "\nzautomation branch: ${env.Branch}"
            if (env["TESTINY_RUN_NAME"]) {
                description = description + "\n${env.TESTINY_RUN_NAME}"
            }
            if (currentBuild.description == null) {
                currentBuild.description = description
            } else {
                currentBuild.description = currentBuild.description + ": " + description
            }

            withMaven(jdk: 'JDK17', maven: 'M3', mavenOpts: '-Xmx1024m', mavenLocalRepo: '.repository', options: [junitPublisher(disabled: true), jacocoPublisher(disabled: true)]) {

                stage('Build common') {
                    echo("Check 1Password Installation")
                    sh "sh $WORKSPACE/zautomation/tests/common/src/main/resources/install1PasswordCLIOnNode.sh"

                    echo("Get backend connections...")
                    sh "sh $WORKSPACE/zautomation/tests/common/src/main/resources/backendConnections.sh"

                    sh """
mvn clean install \\
-f $WORKSPACE/zautomation/tests/common/pom.xml \\
-DbackendType="${backendType}" \\
-DbackendConnections="${WORKSPACE}/backendConnections.json" \\
-DtestinyProjectName="Wire Android Reloaded" \\
-DtestinyRunName="$TESTINY_RUN_NAME" \\
-DcucumberReportUrl="${JOB_NAME} ${BUILD_DISPLAY_NAME} - Cucumber Report: ${BUILD_URL}cucumber-html-reports/" \\
-DcallingServiceUrl='loadbalanced' \\
-Dcom.wire.calling.env='${CALLING_SERVICE_ENV}' \\
-DsyncIsAutomated=true
"""
                }

                // This is needed because of https://issues.jenkins-ci.org/browse/JENKINS-7180
                def RERUN = currentBuild.getRawBuild().actions.find { it instanceof ParametersAction }?.parameters?.find {
                    it.name == 'surefire.rerunFailingTestsCount'
                }?.value
                env.DEFLAKETESTS = params.test

                if (!browserName.isEmpty()) {
                    env.PICKLEJAR_PARALLEL_MAX = 1
                } else {
                    env.PICKLEJAR_PARALLEL_MAX = phonesOnGrid.toInteger()
                }

                def isCountlyAvailable = params.isCountlyAvailable ?: ""

                if (tablet) {

                    stage('Build android') {
                        sh "mvn -DskipTests=true clean install -f $WORKSPACE/zautomation/tests/android-reloaded/pom.xml"
                    }

                    stage('Run tests') {
                        try {
                            lock("${GRID}") {
                                realtimeJUnit(keepLongStdio: true, testDataPublishers: [[$class: 'JUnitFlakyTestDataPublisher']], testResults: 'zautomation/tests/android-tablet/target/xml-reports/TEST*.xml') {
                                    sh """
mvn clean integration-test \\
-f $WORKSPACE/zautomation/tests/android-tablet/pom.xml \\
-P isOnGrid \\
-Durl='${hubUrl}' \\
-Dpicklejar.parallelism=${PICKLEJAR_PARALLEL_MAX} \\
-DskipTests=false \\
-Dpicklejar.tags='$TAGS' \\
-DappPath=${APP_PATH} \\
-DtestingGalleryPath="${testingGalleryPath}" \\
-DandroidVersion='${platformVersion}' \\
-Dsurefire.rerunFailingTestsCount=${RERUN} \\
-Dtest="${DEFLAKETESTS}" \\
-Dpackage=$APP_ID \\
-DbrowserName="${browserName}" \\
-DcurrentApkVersion=${REAL_BUILD_NUMBER}
"""
                                }
                            }
                        } catch (e) {
                            print e
                            if (e instanceof hudson.AbortException) {
                                aborted = true
                            }
                        }
                    }
                } else {
                    stage('Run tests') {
                        try {
                            lock("${GRID}") {
                                realtimeJUnit(keepLongStdio: true, testDataPublishers: [[$class: 'JUnitFlakyTestDataPublisher']], testResults: 'zautomation/tests/android-reloaded/target/xml-reports/TEST*.xml') {
                                    sh """
mvn clean integration-test \\
-f $WORKSPACE/zautomation/tests/android-reloaded/pom.xml \\
-P isOnGrid \\
-Durl='${hubUrl}' \\
-Dpicklejar.parallelism=${PICKLEJAR_PARALLEL_MAX} \\
-DskipTests=false \\
-Dpicklejar.tags="${TAGS}" \\
-DappPath="${APP_PATH}" \\
-DoldAppPath="${OLD_APP_PATH}" \\
-DtestingGalleryPath="${testingGalleryPath}" \\
-DandroidVersion='${platformVersion}' \\
-Dtest="${DEFLAKETESTS}" \\
-Dpackage="${APP_ID}" \\
-Dsurefire.rerunFailingTestsCount=${RERUN} \\
-DbrowserName="${browserName}" \\
-DcurrentApkVersion=${REAL_BUILD_NUMBER} \\
-DisCountlyAvailable="${isCountlyAvailable}"
"""
                                }
                            }
                        } catch (e) {
                            print e
                            if (e instanceof hudson.AbortException) {
                                aborted = true
                            }
                        }
                    }
                }

            }

            stage('Generate test results') {

                try {
                    // Generate Jenkins cucumber HTML reports and archive JSON
                    archiveArtifacts artifacts: '**/target/*report*.json', followSymlinks: false
                    if (tablet) {
                        cucumber failedFeaturesNumber: -1, failedScenariosNumber: -1, failedStepsNumber: -1, fileIncludePattern: '**/target/*report*.json', jsonReportDirectory: "${WORKSPACE}/zautomation/tests/android-tablet/", mergeFeaturesById: true, pendingStepsNumber: -1, skippedStepsNumber: -1, sortingMethod: 'ALPHABETICAL', undefinedStepsNumber: -1
                    } else {
                        cucumber failedFeaturesNumber: -1, failedScenariosNumber: -1, failedStepsNumber: -1, fileIncludePattern: '**/target/*report*.json', jsonReportDirectory: "${WORKSPACE}/zautomation/tests/android-reloaded/", mergeFeaturesById: true, pendingStepsNumber: -1, skippedStepsNumber: -1, sortingMethod: 'ALPHABETICAL', undefinedStepsNumber: -1
                    }
                } catch (e) {
                    print e
                    if (e instanceof hudson.AbortException) {
                        aborted = true
                    }
                }
                try {
                    if (!tablet) {
                        // Zip and archive cucumber HTML reports
                        node("built-in") {
                            sh returnStatus: true, script: 'rm -rf cucumber-report*.zip'
                            zip archive: true, defaultExcludes: false, dir: "../../jobs/${JOB_NAME}/builds/${BUILD_NUMBER}/cucumber-html-reports/", overwrite: true, zipFile: "cucumber-report_Android_build_${REAL_BUILD_NUMBER}.zip"
                        }
                    }
                } catch (e) {
                    print e
                    if (e instanceof hudson.AbortException) {
                        aborted = true
                    }
                }
            }

            stage('Report test results') {
                def testResult = testStatuses()
                if (isTestSuccessful()) {
                    if (!aborted) {
                        wireSend secret: env.JENKINSBOT_SECRET, message: "✅ **${JOB_NAME} ${BUILD_DISPLAY_NAME}**\n${description}\nSee [JUnit Reports](${BUILD_URL}testReport/) or [Cucumber Reports](${BUILD_URL}cucumber-html-reports)\n${testResult}"
                    } else {
                        wireSend secret: env.JENKINSBOT_SECRET, message: "⚠️ **${JOB_NAME} ${BUILD_DISPLAY_NAME} was aborted**\n${description}\nSee [Console log](${BUILD_URL}console)\n${testResult}"
                    }
                } else {
                    wireSend secret: env.JENKINSBOT_SECRET, message: "❌ **${JOB_NAME} ${BUILD_DISPLAY_NAME}**\n${description}\nSee [JUnit Reports](${BUILD_URL}testReport/) or [Cucumber Reports](${BUILD_URL}cucumber-html-reports)\n${testResult}"
                }
            }
        }
    }
}
