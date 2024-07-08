pipeline {
    agent {
        node {
            label 'built-in'
        }
    }

    options { disableConcurrentBuilds(abortPrevious: true) }

    environment { 
        CREDENTIALS = credentials('GITHUB_TOKEN_ANDROID')
        WIRE_BOT_SECRET = credentials('JENKINSBOT_ANDROID_SMOKE')
    }

    stages {
        stage("Wait for GitHub action to finish") {
            when {
                expression { BRANCH_NAME ==~ /PR-[0-9]+/ }
            }
            steps {
                script {
                    def commit_hash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                    def pr_number = BRANCH_NAME.replaceAll(/\D/, '')
                    echo("Wait for github actions to start for ${BRANCH_NAME}")
                    timeout(time: 30, unit: 'MINUTES') {
                       waitUntil {
                           def output = sh label: 'Get runs', returnStdout: true, script: 'curl -s -L -H "Accept: application/vnd.github+json" -H "Authorization: Bearer ${CREDENTIALS}" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/wireapp/wire-android/actions/workflows/98603098/runs'
                           def json = readJSON text: output
                           if (json['message']) {
                               echo("Output: " + output)
                               error("**Trigger script failed:** " + json['message'])
                           }
                           def runs = json['workflow_runs']
                           echo("Looking for PR-" + pr_number + " with hash" + commit_hash)
                           for (run in runs) {
                               if (run['head_sha'] == commit_hash) {
                                   echo("Found " + commit_hash)
                                   echo("status: " + run['status'])
                                   // status can be queued, in_progress, or completed
                                   if (run['status'] == 'queued' || run['status'] == 'in_progress' || run['status'] == 'completed') {
                                       return true
                                   }
                               }
                           }
                           sleep(20)
                           return false
                       }
                    }

                    echo("Wait for apk to be build for ${BRANCH_NAME}")
                    timeout(time: 70, unit: 'MINUTES') {
                       waitUntil {
                           def output = sh label: 'Get runs', returnStdout: true, script: 'curl -s -L -H "Accept: application/vnd.github+json" -H "Authorization: Bearer ${CREDENTIALS}" -H "X-GitHub-Api-Version: 2022-11-28" https://api.github.com/repos/wireapp/wire-android/actions/workflows/98603098/runs'
                           def json = readJSON text: output
                           def runs = json['workflow_runs']
                           echo("Looking for hash " + commit_hash)
                           for (run in runs) {
                               if (run['head_sha'] == commit_hash) {
                                   echo("conclusion: " + run['conclusion'])
                                   env.GITHUB_ACTION_URL = run['url'].replace('api.github.com/repos', 'github.com/')
                                   // conclusion can be: success, failure, neutral, cancelled, skipped, timed_out, or action_required
                                   if (run['conclusion'] == 'success') {
                                       return true
                                   } else if (run['conclusion'] == 'failure') {
                                       error("❌ **Build failed for branch '${BRANCH_NAME}'** See Github Actions: " + env.GITHUB_ACTION_URL)
                                   } else if (run['conclusion'] == 'cancelled') {
                                       error("⚠️ **Build aborted for branch '${BRANCH_NAME}'** See Github Actions: " + env.GITHUB_ACTION_URL)
                                   }
                               }
                           }
                           sleep(20)
                           return false;
                       }
                   }
                }
            }
            post {
                unsuccessful {
                    script {
                        wireSend(secret: env.WIRE_BOT_SECRET, message: "❌ **$BRANCH_NAME**\n[$CHANGE_TITLE](${CHANGE_URL})\nBuild aborted or failed! See [Github Actions](" + env.GITHUB_ACTION_URL + ")")
                    }
                }
            }
        }

        stage("Smoke Tests") {
            when {
                expression { BRANCH_NAME ==~ /PR-[0-9]+/ }
            }
            steps {
                script {
                    def files = []
                    withAWS(region: 'eu-west-1', credentials: "S3_CREDENTIALS") {
                        files = s3FindFiles bucket: "z-lohika", path: "artifacts/megazord/android/reloaded/staging/compat/$BRANCH_NAME/", onlyFiles: true, glob: '*.apk'
                    }
                    files.sort { a, b -> a.lastModified <=> b.lastModified }
                    if (files.size() < 1) {
                        error("Could not find any apk at provided location!")
                    } else {
                        def lastModifiedFileName = files[-1].name
                        def childJob = build job: 'android_reloaded_smoke', parameters: [string(name: 'AppBuildNumber', value: "artifacts/megazord/android/reloaded/staging/compat/$BRANCH_NAME/${lastModifiedFileName}"), string(name: 'TAGS', value: '@smoke'), string(name: 'Branch', value: 'main')]
                        env.CHILD_JOB_URL = childJob.getAbsoluteUrl()
                    }
                }
            }
            post {
                unsuccessful {
                    script {
                        wireSend(secret: env.WIRE_BOT_SECRET, message: "❌ **$BRANCH_NAME**\n[$CHANGE_TITLE](${CHANGE_URL})\nQA-Jenkins - Smoke Tests failed! [Details](" + env.CHILD_JOB_URL + ")")
                    }
                }
            }
        }

    }

    post {
        success {
            script {
                if (env.BRANCH_NAME ==~ /PR-[0-9]+/) {
                    wireSend(secret: env.WIRE_BOT_SECRET, message: "✅ **$BRANCH_NAME**\n[$CHANGE_TITLE](${CHANGE_URL})\nQA-Jenkins - Smoke Tests [Details](${BUILD_URL})")
                }
            }
        }
    }
}
