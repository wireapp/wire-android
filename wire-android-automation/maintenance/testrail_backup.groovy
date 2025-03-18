if (params.JENKINSBOT_SECRET == null) {
    properties([
            parameters([
                    credentials(name: 'JENKINSBOT_SECRET', credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl', defaultValue: 'JENKINSBOT_QA_MAINTENANCE', required: true),
            ])
    ])
}

def jenkinsbot_secret = ""

withCredentials([string(credentialsId: params.JENKINSBOT_SECRET, variable: 'JENKINSBOT_SECRET')]) {
    jenkinsbot_secret = env.JENKINSBOT_SECRET
}

node('testrail_node034') {

    env.DATE = sh returnStdout: true, script: 'date +%F-%H-%M'
    env.DATE = env.DATE.trim()

    try {

        timeout(activity: true, time: 60) {

            env.DAYS_TO_KEEP_LOCAL_BACKUPS = 30
            env.BACKUP_ARCHIVE = "testrail-backup-${DATE}.tar.gz"

            stage('Compress config, attachments and reports') {
                sh 'tar -vzcf "/home/jenkins/testrail-backup-$DATE.tar.gz" /home/jenkins/testrail/_config/config.php /home/jenkins/testrail/_opt/attachments/ /home/jenkins/testrail/_opt/reports/ /home/jenkins/testrail/_ssl/'
            }

            stage('Upload backups to node037') {
                sshagent(credentials: ['jenkins_slaves']) {
                    // Print current directory content
                    sh 'ssh -o StrictHostKeyChecking=no jenkins@192.168.2.37 ls -la /mnt/backups/testrail/'

                    // Delete old backups
                    sh 'ssh -o StrictHostKeyChecking=no jenkins@192.168.2.37 find /mnt/backups/testrail/ -type f -mtime +' + env.DAYS_TO_KEEP_LOCAL_BACKUPS + ' -delete || true'

                    // Upload new backup via SSH
                    sh 'scp -o StrictHostKeyChecking=no /home/jenkins/testrail-backup-' + env.DATE + '.tar.gz jenkins@192.168.2.37:/mnt/backups/testrail/'

                    // Print current directory content again
                    sh 'ssh -o StrictHostKeyChecking=no jenkins@192.168.2.37 ls -la /mnt/backups/testrail/'
                }
            }

            wireSend secret: jenkinsbot_secret, message: "✅ $BACKUP_ARCHIVE uploaded **successfully**"
        }

        stage('Cleanup old backups locally') {
            sh 'find /home/jenkins/testrail-backup-*.tar.gz -type f -mtime +' + env.DAYS_TO_KEEP_LOCAL_BACKUPS + ' -delete || true'
        }
    } catch (e) {
        print e
        wireSend secret: jenkinsbot_secret, message: "❌ **$BACKUP_ARCHIVE upload failed:** " + e.message

        error('Uploading backup failed')
    }
}
