pipeline {
    agent {
        node {
            label 'spawner'
        }
    }

    options { disableConcurrentBuilds(abortPrevious: true) }

    environment { 
        CREDENTIALS = credentials('GITHUB_TOKEN_ANDROID') 
        PR_NUMBER = BRANCH_NAME =~ /[0-9]+$/
    }

    stages {
        stage("Wait for GitHub action to finish") {
            when {
                expression { BRANCH_NAME ==~ /PR-[0-9]+/ }
            }
            steps {
                // Check: Send pending
                script {
                    echo("Wait for github actions to start for ${BRANCH_NAME}")
                    timeout(time: 3, unit: 'MINUTES') {
                       waitUntil {
                           def output = sh label: 'Get runs', returnStdout: true, script: 'curl -u ${CREDENTIALS} https://api.github.com/repos/wireapp/wire-android/actions/workflows/98603098/runs'
                           def json = readJSON text: output
                           if (json['message']) {
                               echo("Output: " + output)
                               error("**Trigger script failed:** " + json['message'])
                           }
                           def runs = json['workflow_runs']
                           echo("Looking for PR-" + PR_NUMBER)
                           for (run in runs) {
                               def pull_requests = run['pull_requests']
                               for (pull_request in pull_requests) {
                                   if (pull_request['number'] ==~ /PR-${PR_NUMBER}/) {
                                       def run_id = run['id']
                                       echo("Found PR-" + pull_request['number'])
                                       echo("run: " + run_id)
                                       echo("status: " + run['status'])
                                       // status can be queued, in_progress, or completed
                                       if (run['status'] == 'queued' || run['status'] == 'in_progress' || run['status'] == 'completed') {
                                           return true
                                       }
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
                           def output = sh label: 'Get runs', returnStdout: true, script: 'curl -u ${CREDENTIALS} https://api.github.com/repos/wireapp/wire-android/actions/workflows/98603098/runs'
                           def json = readJSON text: output
                           def runs = json['workflow_runs']
                           echo("Looking for hash " + commit_hash)
                           for (run in runs) {
                               if (run['id'] == run_id) {
                                   echo("Found run " + run['id'])
                                   echo("conclusion: " + run['conclusion'])
                                   // conclusion can be: success, failure, neutral, cancelled, skipped, timed_out, or action_required
                                   if (run['conclusion'] == 'success') {
                                       return true
                                   } else if (run['conclusion'] == 'failure') {
                                       error("❌ **Build failed for branch '${GIT_BRANCH_WEBAPP}'** See [Github Actions](" + run['url'] + ")")
                                   } else if (run['conclusion'] == 'cancelled') {
                                       error("⚠️ **Build aborted for branch '${GIT_BRANCH_WEBAPP}'** See [Github Actions](" + run['url'] + ")")
                                   }
                               }
                           }
                           sleep(20)
                           return false;
                       }
                   }
                }
            }

        stage("Run smoke tests") {
            steps {
                // Check: Send in_progress
                build job: 'android_reloaded_smoke', parameters: [string(name: 'AppBuildNumber', value: "/artifacts/megazord/android/reloaded/staging/release/wire-android-staging-release-pr-${PR_NUMBER}.apk"), string(name: 'TAGS', value: '@smoke'), string(name: 'Branch', value: 'main')]
            }
        }

    }

    post {
        success {
            // wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ FAILED ($last_started) 👎")
            script {
                def sha = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                def payload = [
                    name: "QA-Jenkins Smoke Run",
                    status: "",
                    conclusion: 'success', // Can be one of: action_required, cancelled, failure, neutral, success, skipped, stale, timed_out 
                    details_url: env.BUILD_URL,
                    external_id: env.BUILD_ID,
                    head_sha: sha,
                    output: [
                       title: "Smoke run successful",
                       summary: "All test finished successfully",
                    ]
                ]
                def jsonPayload = groovy.json.JsonOutput.toJson(payload)
                sh """
                    curl -X POST \
                    -H "Authorization: token ${GITHUB_TOKEN}" \
                    -H "X-GitHub-Api-Version: 2022-11-28" \
                    -H "Accept: application/vnd.github+json" \
                    https://api.github.com/repos/wireapp/wire-android/check-runs \
                    -d '${jsonPayload}'
                """
            }
        }

        unsuccessful {
            // Check: Send failure
            script {
                echo("Unsuccesful")
            }
            // wireSend(secret: env.WIRE_BOT_SECRET, message: "**[#${BUILD_NUMBER} Link](${BUILD_URL})** [${BRANCH_NAME}] - ❌ ABORTED ($last_started) ")
        }
    }
}
