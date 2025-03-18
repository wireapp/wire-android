import jenkins.model.*
import hudson.model.*
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.jenkinsci.plugins.workflow.support.steps.StageStepExecution

if (params.JENKINSBOT_SECRET == null || params.KILL_JOBS == null || params.PURGE_JOB_HISTORY == null) {
    properties([
            parameters([
                    string(name: 'LOG_ROTATION_IGNORE_LIST', defaultValue: 'jenkins_backup, master, png, webapp', description: 'Comma separated list of job names (name can be partial) to ignore log rotation configuration', trim: false),
                    credentials(name: 'JENKINSBOT_SECRET', credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl', defaultValue: 'JENKINSBOT_QA_MAINTENANCE', required: true),
                    booleanParam(name: 'KILL_JOBS', defaultValue: false, description: 'Kill all other running jobs'),
                    booleanParam(name: 'PURGE_JOB_HISTORY', defaultValue: false, description: 'Set defaults for how long to keep job history & artifacts and purge all jobs with longer history'),
            ])
    ])
}

@NonCPS
def printBlockingJobs() {
    final jobs = Jenkins.instance.items
    final blockingJobs = jobs.findAll { it.getAllJobs() && it.isBuilding() && !it.name.contains("backup") }

    if (blockingJobs) {
        echo("Blocking jobs: ")
        blockingJobs.each {
            echo(it.name)
        }
    }
}

def jenkinsbot_secret = ""

withCredentials([string(credentialsId: params.JENKINSBOT_SECRET, variable: 'JENKINSBOT_SECRET')]) {
    jenkinsbot_secret = env.JENKINSBOT_SECRET
}

def ignoreJobs = LOG_ROTATION_IGNORE_LIST.split(',')
print("Ignoring following jobs: " + ignoreJobs)

node('built-in') {

    env.PATH = "$PATH:/usr/local/bin"

    env.LOG_ROTATION_IGNORE_LIST = params.LOG_ROTATION_IGNORE_LIST
    env.LOG_ROTATION_DAYS_DEFAULT = 14
    env.LOG_ROTATION_NUM_DEFAULT = 25
    env.LOG_ROTATION_DAYS_MAX = 28
    env.LOG_ROTATION_NUM_MAX = 50

    env.LOG_ROTATION_ARTIFACTS_DAYS_DEFAULT = 14
    env.LOG_ROTATION_ARTIFACTS_NUM_DEFAULT = 25
    env.LOG_ROTATION_ARTIFACTS_DAYS_MAX = 14
    env.LOG_ROTATION_ARTIFACTS_NUM_MAX = 25

    try {

        if (params.KILL_JOBS) {
            stage('Kill blocking jobs') {
                def jobs = Jenkins.instance.items
                final blockingJobs = jobs.findAll { it.isBuilding() && !it.name.toLowerCase().contains("backup") }

                if (blockingJobs) {
                    println "Killing blocking jobs: ${blockingJobs}"
                    blockingJobs.collect { Job job ->
                        job.builds.findAll { Run run ->
                            run.isBuilding()
                        } ?: []
                    }.sum().each { Run item ->
                        println "item: " + item
                        if (item in WorkflowRun) {
                            WorkflowRun run = (WorkflowRun) item
                            //hard kill
                            run.doKill()
                            //release pipeline concurrency locks
                            StageStepExecution.exit(run)
                            println "Killed WorkflowRun: ${run}"
                        } else if (item in FreeStyleBuild) {
                            FreeStyleBuild run = (FreeStyleBuild) item
                            run.executor.interrupt(Result.ABORTED)
                            println "Killed FreeStyleBuild: ${run}"
                        } else {
                            println "WARNING: Don't know how to handle ${item.class}"
                        }
                    }
                } else {
                    println "    No blocking jobs were found"
                }
            }
        }

        if (params.PURGE_JOB_HISTORY) {
            stage('Set default settings for jobs with no rotation') {
                println "=========================================================="
                println "Set default settings for jobs with no rotation \n==>"

                def jobs = Jenkins.instance.items
                jobs.each { job ->
                    ignoreJobs.each { filter ->
                        if (job.name.contains(filter.trim())) {
                            jobs.remove(job)
                            println "    Ignoring job " + job.name + " due to filter: " + filter
                        }
                    }
                }

                jobs.each { job ->
                    job.logRotator = new hudson.tasks.LogRotator(LOG_ROTATION_DAYS_DEFAULT, LOG_ROTATION_NUM_DEFAULT, LOG_ROTATION_ARTIFACTS_DAYS_DEFAULT, LOG_ROTATION_ARTIFACTS_NUM_DEFAULT)
                }


                println "Job Completed."
                println "=========================================================="
            }

            stage('Purge jobs history and logs') {
                def jobs = Jenkins.instance.items
                println "Purge jobs history and logs \n==>"
                jobs.findAll { it.logRotator && !it.disabled }.each { job ->
                    job.logRotate()
                    println "    '$job.name' done"
                }
                println "Job Completed."
                println "=========================================================="
            }
        }

    } catch (e) {
        print e
        wireSend secret: "$jenkinsbot_secret", message: "**Jenkins-Backup preparation failed:** " + e.message
        error('Backup preparation failed')
    }

    try {
        timeout(activity: true, time: 60) {

            env.BACKUP_DATE = sh returnStdout: true, script: 'date +%Y_%h-%d'
            env.MINIMAL_FREE_SPACE_GB = 50
            env.DAYS_TO_KEEP_LOCAL_BACKUPS = 30

            env.JENKINS_HD_FREE = sh returnStdout: true, script: 'df -g / | awk \'NR==2 {print $4}\' | tr -d \'\n\''

            env.BACKUP_ARCHIVE = sh returnStdout: true, script: 'echo JENKINS_BACKUP_$(date "+%Y-%m-%d-%H-%M").tar'
            env.BACKUP_ARCHIVE = env.BACKUP_ARCHIVE.trim()

            echo "=============================================================="
            echo "Jenkins 'Root' (HD) has ${JENKINS_HD_FREE} GB of free space."
            echo "=============================================================="

            if (env.JENKINS_HD_FREE.toInteger() < env.MINIMAL_FREE_SPACE_GB.toInteger()) {
                error("Not enough free space on Jenknis HD. Canceling backup...")
            }

            printBlockingJobs()

            env.BACKUP_ROOT = "$WORKSPACE/Backup/"

            sh 'mkdir -p $BACKUP_ROOT'

            stage('Backup general configurations, nodes and secrets') {
                sh 'find $JENKINS_HOME -maxdepth 1 -type f -name "*.xml" -o -type d -name nodes -o -type d -name secrets | xargs tar cpPf $BACKUP_ROOT/$BACKUP_ARCHIVE'
            }

            stage('Backup job configurations') {
                sh 'find -L $JENKINS_HOME/jobs -maxdepth 2 -type f -exec tar rpPf $BACKUP_ROOT/$BACKUP_ARCHIVE {} \\;'
            }

            stage('Backup job builds for last 2 days') {
                sh 'find -L $JENKINS_HOME/jobs -mindepth 3 -maxdepth 3 -type d -mtime 2 -exec tar rpPf $BACKUP_ROOT/$BACKUP_ARCHIVE {} \\;'
            }

            stage('Backup list of installed plugins') {
                sh 'find $JENKINS_HOME/plugins -name "*.jpi" | xargs -n 1 basename | grep -v "flaky\\|wire" | sed "s/.jpi//g" > plugins.txt'
                sh 'tar rpPf $BACKUP_ROOT/$BACKUP_ARCHIVE plugins.txt'
            }

            stage('Backup custom plugins') {
                sh 'tar rpPf $BACKUP_ROOT/$BACKUP_ARCHIVE $JENKINS_HOME/plugins/flaky-test-handler*'
                sh 'tar rpPf $BACKUP_ROOT/$BACKUP_ARCHIVE $JENKINS_HOME/plugins/jenkins-wire-notification-plugin*'
            }

            stage('Remove old local backups') {
                sh 'find "$BACKUP_ROOT" -type f -mtime +${DAYS_TO_KEEP_LOCAL_BACKUPS}d -delete || true'
            }
        }

    } catch (e) {
        print e
        wireSend secret: jenkinsbot_secret, message: "❌ **Backup failed:** " + e.message
        error('Backup failed')
    }

    try {

        timeout(activity: true, time: 60) {

            def qaJenkins = true
            if (env.JENKINS_URL.contains('10.10.124.17')) {
                qaJenkins = false
            }

            if (qaJenkins) {

                def wdmycloud = [:]
                wdmycloud.name = "WD My Cloud"
                wdmycloud.host = "192.168.2.200"
                wdmycloud.allowAnyHosts = true

                stage('Upload backups to WD My Cloud') {
                    withCredentials([usernamePassword(credentialsId: 'WD My Cloud', usernameVariable: 'userName', passwordVariable: 'password')]) {
                        wdmycloud.user = userName
                        wdmycloud.password = password

                        // Print current directory content
                        sshCommand remote: wdmycloud, command: 'ls -la /DataVolume/shares/jenkins_backups'

                        // Delete old backups
                        sshCommand remote: wdmycloud, command: 'find /DataVolume/shares/jenkins_backups -type f -mtime +' + env.DAYS_TO_KEEP_LOCAL_BACKUPS + ' -delete || true'

                        // Upload new backup via SSH
                        sshPut remote: wdmycloud, from: env.BACKUP_ROOT + '/' + env.BACKUP_ARCHIVE, into: '/DataVolume/shares/jenkins_backups/'

                        // Print current directory content again
                        sshCommand remote: wdmycloud, command: 'ls -la /DataVolume/shares/jenkins_backups'
                    }
                }

                def remote = [:]
                remote.name = "node040"
                remote.host = "192.168.2.40"
                remote.allowAnyHosts = true

                stage('Upload backups to node040') {
                    withCredentials([usernamePassword(credentialsId: 'e47a2f3d-4d8f-4e8e-9e7b-274be3a5576c', usernameVariable: 'userName', passwordVariable: 'password')]) {
                        remote.user = userName
                        remote.password = password

                        // Print current directory content
                        sshCommand remote: remote, command: 'ls -la /Users/jenkins/jenkins_backups'

                        // Delete old backups
                        sshCommand remote: remote, command: 'find /Users/jenkins/jenkins_backups -type f -mtime +' + env.DAYS_TO_KEEP_LOCAL_BACKUPS + ' -delete || true'

                        // Upload new backup via SSH
                        sshPut remote: remote, from: env.BACKUP_ROOT + '/' + env.BACKUP_ARCHIVE, into: '/Users/jenkins/jenkins_backups/'

                        // Print current directory content again
                        sshCommand remote: remote, command: 'ls -la /Users/jenkins/jenkins_backups'
                    }
                }

            } else {

                def linux_build_machine = [:]
                linux_build_machine.name = "linux build machine"
                linux_build_machine.host = "10.10.124.16"
                linux_build_machine.allowAnyHosts = true

                stage('Upload backups to ' + linux_build_machine.name) {
                    withCredentials([sshUserPrivateKey(credentialsId: '4f69084b-d82d-470c-a28f-6e8fde230302', keyFileVariable: 'identity', usernameVariable: 'userName')]) {
                        linux_build_machine.user = userName
                        linux_build_machine.identityFile = identity

                        // Print current directory content
                        sshCommand remote: linux_build_machine, command: 'ls -la /home/jenkins/Backup/'

                        // Delete old backups
                        sshCommand remote: linux_build_machine, command: 'find /home/jenkins/Backup/ -type f -mtime +' + env.DAYS_TO_KEEP_LOCAL_BACKUPS + ' -delete || true'

                        // Upload new backup via SSH
                        sshPut remote: linux_build_machine, from: env.BACKUP_ROOT + '/' + env.BACKUP_ARCHIVE, into: '/home/jenkins/Backup/'

                        // Print current directory content again
                        sshCommand remote: linux_build_machine, command: 'ls -la /home/jenkins/Backup/'
                    }
                }
            }

            wireSend secret: jenkinsbot_secret, message: "✅ $BACKUP_ARCHIVE uploaded **successfully**"
        }
    } catch (e) {
        print e
        wireSend secret: jenkinsbot_secret, message: "❌ **$BACKUP_ARCHIVE upload failed:** " + e.message
        error('Uploading backup failed')
    }
}
