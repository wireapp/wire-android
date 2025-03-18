node('built-in') {

    withCredentials([string(credentialsId: params.JENKINSBOT_SECRET, variable: 'JENKINSBOT_SECRET')]) {

        stage('Get version from webapp master') {
            checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'wire-webapp']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/wireapp/wire-webapp.git']]]

            json = readFile("wire-webapp/package.json")

            webpackage = json.split('\n').find { it.contains('@wireapp/avs') }

            version = ( webpackage =~ /[\d.]+/ )[0]

            versions = "AVS (web master): " + version
        }

        stage('Get version from webapp dev') {
            checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/dev']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'wire-webapp']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/wireapp/wire-webapp.git']]]

            json = readFile("wire-webapp/package.json")

            webpackage = json.split('\n').find { it.contains('@wireapp/avs') }

            version = ( webpackage =~ /[\d.]+/ )[0]

            versions = versions + "\nAVS (web dev): " + version
        }

        stage('Get version from ios develop') {
            checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/develop']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'wire-ios-mono']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/wireapp/wire-ios-mono.git']]]

            cartfile = readFile("wire-ios-mono/wire-avs.json")

            avsLine = cartfile.split('\n').find { it.contains('avs.xcframework') }

            version = ( avsLine =~ /"([\d.]+)"/ ) [0][1]

            versions = versions + "\nAVS (ios develop): " + version
        }

        stage('Get version from kalium develop branch') {
            versions = versions + "\nAVS (kalium develop): "
            try {
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/develop']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'kalium']], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/wireapp/kalium.git']]]

                toml = readFile("kalium/gradle/libs.versions.toml")

                line = toml.split('\n').find { it.contains('avs = ') }

                version = ( line =~ /[\d.]+/ )[0]

                versions = versions + version
            } catch (e) {
                versions = versions + "Could not get version"
            }
        }

        stage('Get version from zcall current version') {
            versions = versions + "\nAVS (zcall current version): "
            try {
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '*/main']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'zautomation']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'zautomation', url: 'git@github.com:zinfra/zautomation.git']]]

                json = readFile("zautomation/tests/common/src/main/java/com/wearezeta/auto/common/CallingManager.java")

                line = json.split('\n').find { it.contains('ZCALL_CURRENT_VERSION = ') }

                version = ( line =~ /[\d.]+/ )[0]

                versions = versions + version
            } catch (e) {
                versions = versions + "Could not get version"
            }
        }

        echo(versions)

        wireSend secret: env.JENKINSBOT_SECRET, message: "**Versions:**\n" + versions
    }
}