List<String> defineFlavor() {
    //check if the pipeline has the custom flavor env variable set
    def overwrite = env.CUSTOM_FLAVOR
    if (overwrite != null) {
        return overwrite
    }

    def branchName = env.BRANCH_NAME

    if (branchName == "main") {
        return ['Beta']
    } else if (branchName == "develop") {
        return ['Staging', 'Dev']
    } else if (branchName == "prod") {
        return ['Prod']
    } else if (branchName == "internal") {
        return ['Internal']
    }
    return ['Dev', 'Staging']
}

String defineBuildType(String flavor) {
    def overwrite = env.CUSTOM_BUILD_TYPE
    if (overwrite != null) {
        return overwrite
    }

    // internal is used for wire beta builds
    if (flavor == 'Beta') {
        return 'Release'
    } else if (flavor == 'Prod') {
        return "Compatrelease"
    }
    // use the scala client signing keys for testing upgrades.
    return "Compat"
}

def defineTrackName() {
    def overwrite = env.CUSTOM_TRACK
    def branchName = env.BRANCH_NAME

    if (overwrite != null) {
        return overwrite
    } else if (branchName == "main") {
        return 'internal'
    }
    return 'None'
}

String shellQuote(String s) {
    // Quote a string so it's suitable to pass to the shell
    return "'" + s.replaceAll("'", "'\"'\"'") + "'"
}

def postGithubComment(String changeId, String body) {
    def authHeader = shellQuote("Authorization: token ${env.GITHUB_API_TOKEN}")
    def apiUrl = shellQuote("https://api.github.com/repos/wireapp/wire-android-reloaded/issues/${changeId}/comments")

    // The comment body must be quoted for embedding into a JSON string,
    // and the JSON string must be quoted for embedding into the shell command
    // line. Note well: the backslash character has a special meaning in
    // both the first argument (regular expression pattern) and the second
    // (Matcher.replaceAll() escaping character) of String.replaceAll(); hence,
    // yet another level of escaping is required here!

    def payload = body.replaceAll('\\\\', '\\\\\\\\').replaceAll('"', '\\\\"').replaceAll('\n', '\\\\n')
    def json = shellQuote('{"body":"' + payload + '"}')

    // Note the interpolated variables here come from Groovy -- the command
    // line which the shell interpreter executes is fully rendered, and contains
    // no unsubstituted variables
    sh "curl -s -H ${authHeader} -X POST -d ${json} ${apiUrl}"

}

pipeline {
    agent {
        node {
            label 'spawner'
        }
    }

    options { disableConcurrentBuilds(abortPrevious: true) }

    stages {

        stage('Load Env Variables') {
            steps {
                configFileProvider([
                        configFile(fileId: env.GROOVY_ENV_VARS_REFERENCE_FILE_NAME, variable: 'GROOVY_FILE_THAT_SETS_VARIABLES')
                ]) {
                    load env.GROOVY_FILE_THAT_SETS_VARIABLES
                }
            }
        }

        stage("run pipeline") {
            steps {
                script {
                    def dynamicStages = [:]
                    for (flavor in flavorList) {
                        String buildType = defineBuildType(flavor)
                        String stageName = "Build $flavor"
                        dynamicStages[stageName] = {
                            stage(stageName) {
                                steps {
                                    build job: 'AR-builder', parameters: [
                                            [$class: 'StringParameterValue', name: 'SOURCE_BRANCH', value: env.BRANCH_NAME],
                                            [$class: 'StringParameterValue', name: 'BUILD_TYPE', value: buildType],
                                            [$class: 'StringParameterValue', name: 'FLAVOR', value: flavor],
                                            [$class: 'BooleanParameterValue', name: 'UPLOAD_TO_S3', value: false],
                                            [$class: 'BooleanParameterValue', name: 'TRY_UPLOAD_TO_PLAYSTORE', value: false],
                                            [$class: 'BooleanParameterValue', name: 'RUN_UNIT_TEST', value: true],
                                            [$class: 'BooleanParameterValue', name: 'RUN_ACCEPTANCE_TESTS', value: false],
                                            [$class: 'BooleanParameterValue', name: 'RUN_STATIC_CODE_ANALYSIS', value: false]
                                    ]
                                }
                            }
                        }
                    }
                    parallel dynamicStages
                }
            }
        }
    }

    environment {
        flavorList = defineFlavor()
        trackName = defineTrackName()
    }

    post {
        failure {
            script {
                if (env.BRANCH_NAME.startsWith('PR-')) {
                    def payload = "Build [${env.BUILD_NUMBER}](${env.BUILD_URL}) **failed**."
                    postGithubComment(env.CHANGE_ID, payload)
                }
            }

            wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ FAILED ($last_started) 👎")
        }

        success {
            script {
            }
        }

        aborted {
            wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ ABORTED ($last_started) ")
        }

    }
}
